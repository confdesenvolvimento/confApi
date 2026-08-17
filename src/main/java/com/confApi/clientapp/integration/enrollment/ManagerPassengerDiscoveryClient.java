package com.confApi.clientapp.integration.enrollment;

import com.confApi.clientapp.config.ClientAppEnrollmentProperties;
import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;

public class ManagerPassengerDiscoveryClient {
    private static final Logger LOG = LoggerFactory.getLogger(ManagerPassengerDiscoveryClient.class);
    private final RestTemplate restTemplate;
    private final ClientAppEnrollmentProperties properties;
    private final ConfAppService confAppService;
    private final ObjectMapper objectMapper;

    public ManagerPassengerDiscoveryClient(@Qualifier("mviagensRestTemplate") RestTemplate restTemplate,
                                           ClientAppEnrollmentProperties properties,
                                           ConfAppService confAppService,
                                           ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.confAppService = confAppService;
        this.objectMapper = objectMapper;
    }

    public List<Match> findByCpf(String cpf) {
        LOG.info("Manager passenger discovery start cpfSuffix={}", cpfSuffix(cpf));
        if (properties.getPassengerBaseByCpfPath() == null || properties.getPassengerBaseByCpfPath().isBlank()) {
            throw new ClientAppEnrollmentException(503, "PASSENGER_LOOKUP_NOT_CONFIGURED", true);
        }
        String token = managerToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (token != null && !token.isBlank()) headers.setBearerAuth(token);
        URI uri = UriComponentsBuilder.fromUri(properties.getManagerBaseUrl())
                .path(properties.getPassengerBaseByCpfPath())
                .queryParam("cpf", cpf).build().toUri();
        JsonNode response = get(uri, headers);
        List<JsonNode> passengers = nodes(response);
        LOG.info("Manager passenger discovery response passengers={}", passengers.size());
        Map<Integer, MatchBuilder> byAgency = new LinkedHashMap<>();
        for (JsonNode passenger : passengers) {
            int agencyId = intValue(passenger, "agencyId", "codgAgencia", "codg_agencia");
            int passengerId = intValue(passenger, "passengerBaseId", "codgPassageiroBase", "codg_passageiro_base");
            if (agencyId <= 0 || passengerId <= 0) continue;
            MatchBuilder builder = byAgency.computeIfAbsent(agencyId, id -> new MatchBuilder(id));
            builder.passengerIds.add(passengerId);
            String name = text(passenger, "passengerName", "nomePassageiro", "nome_passageiro");
            if (name != null) builder.names.add(name.trim());
            String email = text(passenger, "email");
            String phone = text(passenger, "celular", "telefone");
            if (email != null && !email.isBlank()) builder.emails.add(email.trim());
            if (phone != null && !phone.isBlank()) builder.phones.add(phone.trim());
        }
        for (MatchBuilder builder : byAgency.values()) {
            JsonNode agency = get(UriComponentsBuilder.fromUri(properties.getManagerBaseUrl())
                    .path(properties.getAgencyByCodePath()).queryParam("codgAgencia", builder.agencyId).build().toUri(), headers);
            JsonNode value = nodes(agency).stream().findFirst().orElse(agency);
            builder.name = text(value, "agencyName", "nomeAgencia", "nome_agencia");
            builder.logo = text(value, "agencyLogoUrl", "logomarca", "logoUrl", "logo_url");
            builder.status = intValue(value, "agencyStatus", "status");
        }
        List<Match> result = new ArrayList<>();
        byAgency.values().forEach(builder -> result.add(builder.build()));
        LOG.info("Manager passenger discovery completed agencies={}", result.size());
        return result;
    }

    private String managerToken() {
        try {
            ConfAppResp response = confAppService.token();
            return response == null ? null : response.getToken();
        } catch (RuntimeException exception) {
            LOG.error("Manager technical authentication failed error={}", exception.getClass().getSimpleName());
            throw new ClientAppEnrollmentException(503, "PASSENGER_LOOKUP_UNAVAILABLE", true);
        }
    }

    private JsonNode get(URI uri, HttpHeaders headers) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new ClientAppEnrollmentException(503, "PASSENGER_LOOKUP_UNAVAILABLE", true);
            }
            return objectMapper.readTree(response.getBody());
        } catch (ClientAppEnrollmentException exception) { throw exception;
        } catch (HttpStatusCodeException exception) {
            LOG.error("Manager request rejected uri={} status={}", uri.getPath(), exception.getStatusCode().value());
            String code = exception.getStatusCode().value() == 401 || exception.getStatusCode().value() == 403
                    ? "PASSENGER_LOOKUP_FORBIDDEN"
                    : "PASSENGER_LOOKUP_UNAVAILABLE";
            throw new ClientAppEnrollmentException(503, code, true);
        } catch (RestClientException | java.io.IOException exception) {
            LOG.error("Manager request failed uri={} error={}", uri.getPath(), exception.getClass().getSimpleName());
            throw new ClientAppEnrollmentException(503, "PASSENGER_LOOKUP_UNAVAILABLE", true);
        }
    }

    private List<JsonNode> nodes(JsonNode node) {
        if (node == null || node.isNull()) return List.of();
        if (node.isArray()) { List<JsonNode> list = new ArrayList<>(); node.forEach(list::add); return list; }
        if (node.has("content") && node.get("content").isArray()) return nodes(node.get("content"));
        return List.of(node);
    }

    private int intValue(JsonNode node, String... names) { for (String name : names) if (node.has(name) && node.get(name).canConvertToInt()) return node.get(name).asInt(); return 0; }
    private String text(JsonNode node, String... names) { for (String name : names) if (node.has(name) && !node.get(name).isNull()) return node.get(name).asText(); return null; }
    private String cpfSuffix(String value) { String digits = value == null ? "" : value.replaceAll("\\D", ""); return digits.length() <= 4 ? "****" : "****" + digits.substring(digits.length() - 4); }

    public record Match(int agencyId, String agencyName, String logoUrl, int agencyStatus,
                        List<Integer> passengerIds, List<String> names, List<String> emails, List<String> phones) {}

    private static final class MatchBuilder {
        final int agencyId; String name; String logo; int status;
        final Set<Integer> passengerIds = new LinkedHashSet<>();
        final Set<String> names = new LinkedHashSet<>();
        final Set<String> emails = new LinkedHashSet<>();
        final Set<String> phones = new LinkedHashSet<>();
        MatchBuilder(int agencyId) { this.agencyId = agencyId; }
        Match build() { return new Match(agencyId, name, logo, status, List.copyOf(passengerIds), List.copyOf(names), List.copyOf(emails), List.copyOf(phones)); }
    }
}
