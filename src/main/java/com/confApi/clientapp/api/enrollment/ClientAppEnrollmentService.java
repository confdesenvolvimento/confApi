package com.confApi.clientapp.api.enrollment;

import com.confApi.clientapp.config.ClientAppEnrollmentProperties;
import com.confApi.clientapp.config.MViagensBackendProperties;
import com.confApi.clientapp.integration.enrollment.ClientAppEnrollmentException;
import com.confApi.clientapp.integration.enrollment.ManagerPassengerDiscoveryClient;
import com.confApi.clientapp.integration.enrollment.MViagensEnrollmentClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;

import java.util.*;
import java.util.regex.Pattern;

public class ClientAppEnrollmentService {
    private static final Pattern CPF = Pattern.compile("\\d{11}");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]{1,120}@[A-Za-z0-9.-]{1,120}\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^\\+[1-9]\\d{7,14}$");
    private final ClientAppEnrollmentProperties enrollmentProperties;
    private final MViagensBackendProperties backendProperties;
    private final ObjectProvider<MViagensEnrollmentClient> backend;
    private final ObjectProvider<ManagerPassengerDiscoveryClient> manager;
    private final ObjectMapper objectMapper;

    public ClientAppEnrollmentService(ClientAppEnrollmentProperties enrollmentProperties,
                                      MViagensBackendProperties backendProperties,
                                      ObjectProvider<MViagensEnrollmentClient> backend,
                                      ObjectProvider<ManagerPassengerDiscoveryClient> manager,
                                      ObjectMapper objectMapper) {
        this.enrollmentProperties = enrollmentProperties; this.backendProperties = backendProperties;
        this.backend = backend; this.manager = manager; this.objectMapper = objectMapper;
    }

    public JsonNode start(StartRequest request, String idem, String requestId, String correlationId) {
        ensureReady();
        String cpf = normalizeCpf(request.cpf());
        JsonNode flow = backend.getObject().createFlow(Map.of("cpf", cpf, "device", device(request)), idem, requestId, correlationId);
        String flowId = text(flow, "flowId", "id");
        if (flowId == null) throw unavailable();
        List<ManagerPassengerDiscoveryClient.Match> matches = manager.getObject().findByCpf(cpf);
        if (matches.isEmpty()) {
            var response = objectMapper.createObjectNode();
            response.put("outcome", "AGENCY_NOT_FOUND");
            response.put("flowId", flowId);
            response.put("flowVersion", longValue(flow, "flowVersion", 0));
            if (flow.has("expiresAt")) response.set("expiresAt", flow.get("expiresAt"));
            return response;
        }
        List<Map<String, Object>> agencies = new ArrayList<>();
        for (ManagerPassengerDiscoveryClient.Match match : matches) {
            Map<String, Object> candidate = new LinkedHashMap<>();
            candidate.put("agencyId", match.agencyId()); candidate.put("displayName", safeName(match.agencyName(), "Agência"));
            candidate.put("logoUrl", match.logoUrl()); candidate.put("passengerBaseIds", match.passengerIds());
            boolean agencyActive = match.agencyStatus() > 0;
            String destination = preferredDestination(match);
            boolean eligible = agencyActive && destination != null;
            candidate.put("eligible", eligible);
            if (!eligible) candidate.put("ineligibilityCode", agencyActive ? "CONTACT_UNAVAILABLE" : "AGENCY_INACTIVE");
            if (eligible) candidate.put("otpDelivery", Map.of("passengerBaseId", match.passengerIds().get(0), "channel", enrollmentProperties.getOtpPreferredChannel(), "destination", destination));
            agencies.add(candidate);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("expectedFlowVersion", longValue(flow, "flowVersion", 0));
        payload.put("customerDisplayName", matches.stream().flatMap(m -> m.names().stream()).findFirst().orElse("Cliente"));
        payload.put("agencies", agencies);
        return backend.getObject().submitCandidates(flowId, payload, derivedKey(idem), requestId, correlationId);
    }

    public JsonNode select(String flowId, SelectAgencyRequest request, String idem, String requestId, String correlationId) {
        ensureReady(); return backend.getObject().selectAgency(flowId, Map.of("agencyId", request.agencyId(), "expectedFlowVersion", request.expectedFlowVersion()), idem, requestId, correlationId);
    }
    public JsonNode verify(String challengeId, VerifyOtpRequest request, String idem, String requestId, String correlationId) {
        ensureReady(); return backend.getObject().verify(challengeId, Map.of("code", request.code()), idem, requestId, correlationId);
    }
    public JsonNode resend(String challengeId, String idem, String requestId, String correlationId) {
        ensureReady(); return backend.getObject().resend(challengeId, idem, requestId, correlationId);
    }
    public JsonNode refresh(RefreshSessionRequest request, String idem, String requestId, String correlationId) {
        ensureReady(); return backend.getObject().refresh(Map.of("refreshToken", request.refreshToken(), "installationId", request.installationId()), idem, requestId, correlationId);
    }

    private void ensureReady() {
        if (!enrollmentProperties.isEnabled() || !backendProperties.isEnabled()
                || backend.getIfAvailable() == null || manager.getIfAvailable() == null) throw unavailable();
    }
    private String normalizeCpf(String value) {
        String cpf = value == null ? "" : value.replaceAll("\\D", "");
        if (!CPF.matcher(cpf).matches() || cpf.chars().distinct().count() == 1 || !validCpf(cpf)) throw new ClientAppEnrollmentException(400, "INVALID_CPF", false);
        return cpf;
    }
    private boolean validCpf(String cpf) { int sum = 0; for (int i=0;i<9;i++) sum += (cpf.charAt(i)-48)*(10-i); int d1=(sum*10)%11; if(d1==10)d1=0; if(d1 != cpf.charAt(9)-48)return false; sum=0; for(int i=0;i<10;i++)sum+=(cpf.charAt(i)-48)*(11-i); int d2=(sum*10)%11; if(d2==10)d2=0; return d2==cpf.charAt(10)-48; }
    private Map<String,Object> device(StartRequest r) { Map<String,Object> d = new LinkedHashMap<>(); d.put("installationId", r.installationId()); d.put("platform", r.platform()); d.put("appVersion", r.appVersion()); d.put("osVersion", r.osVersion()); d.put("displayName", r.displayName()); return d; }
    private String preferredDestination(ManagerPassengerDiscoveryClient.Match match) { for(String e:match.emails()) if(EMAIL.matcher(e).matches()) return e.toLowerCase(Locale.ROOT); for(String p:match.phones()) { String n=p.replaceAll("[^0-9+]", ""); if(PHONE.matcher(n).matches()) return n; } return null; }
    private String safeName(String v, String fallback) { return v == null || v.isBlank() ? fallback : v.substring(0, Math.min(160, v.length())); }
    private String text(JsonNode n, String... names) { for(String name:names) if(n.has(name)&&!n.get(name).isNull()) return n.get(name).asText(); return null; }
    private long longValue(JsonNode n, String name, long fallback) { return n.has(name)&&n.get(name).canConvertToLong()?n.get(name).asLong():fallback; }
    private String derivedKey(String key) { return (key + ".candidates").substring(0, Math.min(128, key.length()+11)); }
    private ClientAppEnrollmentException unavailable() { return new ClientAppEnrollmentException(503, "ENROLLMENT_SERVICE_UNAVAILABLE", true); }

    public record StartRequest(String cpf, String installationId, String platform, String appVersion, String osVersion, String displayName) {}
    public record SelectAgencyRequest(int agencyId, long expectedFlowVersion) {}
    public record VerifyOtpRequest(String code) {}
    public record RefreshSessionRequest(String refreshToken, String installationId) {}
}
