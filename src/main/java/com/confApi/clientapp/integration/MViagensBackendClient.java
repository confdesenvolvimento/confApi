package com.confApi.clientapp.integration;

import com.confApi.clientapp.config.MViagensBackendProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.confApi.clientapp.integration.MViagensBackendUnavailableException.Reason.CONNECTIVITY_OR_TIMEOUT;
import static com.confApi.clientapp.integration.MViagensBackendUnavailableException.Reason.MALFORMED_RESPONSE;
import static com.confApi.clientapp.integration.MViagensBackendUnavailableException.Reason.TECHNICAL_AUTHENTICATION;
import static com.confApi.clientapp.integration.MViagensBackendUnavailableException.Reason.UPSTREAM_HTTP;

public class MViagensBackendClient {

    public static final String SERVICE_TOKEN_HEADER = "X-MViagens-Service-Token";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private static final Pattern JWT_PATTERN = Pattern.compile(
            "^[A-Za-z0-9_-]{1,4096}\\.[A-Za-z0-9_-]{1,4096}\\.[A-Za-z0-9_-]{1,4096}$"
    );
    private static final Pattern CORRELATION_ID_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,100}$");
    private static final Pattern SCOPE_PATTERN = Pattern.compile("^[a-z][a-z0-9:-]{0,99}$");
    private static final long UNSIGNED_INT_MAX = 4_294_967_295L;

    private final RestTemplate restTemplate;
    private final MViagensBackendProperties properties;
    private final URI introspectionUri;

    public MViagensBackendClient(RestTemplate restTemplate, MViagensBackendProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.introspectionUri = UriComponentsBuilder
                .fromUri(properties.getBaseUrl())
                .pathSegment("internal", "v1", "auth", "introspect")
                .build()
                .toUri();
    }

    public MViagensIntrospectionResponse introspect(String rawAccessToken, String correlationId) {
        if (!isSafeJwt(rawAccessToken)) {
            return inactive();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(SERVICE_TOKEN_HEADER, properties.getServiceToken());
        headers.setBearerAuth(rawAccessToken);
        headers.set(CORRELATION_ID_HEADER, normalizeCorrelationId(correlationId));
        headers.setCacheControl("no-store");
        headers.setPragma("no-cache");

        try {
            ResponseEntity<MViagensIntrospectionResponse> response = restTemplate.exchange(
                    introspectionUri,
                    HttpMethod.POST,
                    new HttpEntity<Void>(headers),
                    MViagensIntrospectionResponse.class
            );
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw unavailable(MALFORMED_RESPONSE);
            }

            validateSemantics(response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException.Unauthorized exception) {
            throw unavailable(TECHNICAL_AUTHENTICATION);
        } catch (HttpStatusCodeException exception) {
            throw unavailable(UPSTREAM_HTTP);
        } catch (ResourceAccessException exception) {
            throw unavailable(CONNECTIVITY_OR_TIMEOUT);
        } catch (RestClientException exception) {
            throw unavailable(MALFORMED_RESPONSE);
        }
    }

    private void validateSemantics(MViagensIntrospectionResponse response) {
        if (response.active() == null) {
            throw unavailable(MALFORMED_RESPONSE);
        }
        if (!response.active()) {
            if (response.customerPublicId() != null
                    || response.sessionPublicId() != null
                    || response.devicePublicId() != null
                    || response.agencyLinkPublicId() != null
                    || response.activeAgencyId() != null
                    || response.sessionVersion() != null
                    || response.contextVersion() != null
                    || !response.scopes().isEmpty()) {
                throw unavailable(MALFORMED_RESPONSE);
            }
            return;
        }

        if (!isCanonicalUuid(response.customerPublicId())
                || !isCanonicalUuid(response.sessionPublicId())
                || !isCanonicalUuid(response.devicePublicId())
                || !isCanonicalUuid(response.agencyLinkPublicId())
                || response.activeAgencyId() == null
                || response.activeAgencyId() <= 0
                || !isUnsignedInt(response.sessionVersion())
                || !isUnsignedInt(response.contextVersion())
                || response.scopes().isEmpty()
                || response.scopes().size() > 50
                || !response.scopes().contains("customer:authenticated")
                || response.scopes().stream().anyMatch(scope -> scope == null || !SCOPE_PATTERN.matcher(scope).matches())) {
            throw unavailable(MALFORMED_RESPONSE);
        }
    }

    private boolean isSafeJwt(String token) {
        return token != null && token.length() <= 8192 && JWT_PATTERN.matcher(token).matches();
    }

    private boolean isCanonicalUuid(String value) {
        if (value == null) {
            return false;
        }
        try {
            return UUID.fromString(value).toString().equals(value.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean isUnsignedInt(Long value) {
        return value != null && value >= 0 && value <= UNSIGNED_INT_MAX;
    }

    private String normalizeCorrelationId(String value) {
        if (value != null && CORRELATION_ID_PATTERN.matcher(value).matches()) {
            return value;
        }
        return UUID.randomUUID().toString();
    }

    private MViagensIntrospectionResponse inactive() {
        return new MViagensIntrospectionResponse(false, null, null, null, null, null, null, null, List.of());
    }

    private MViagensBackendUnavailableException unavailable(MViagensBackendUnavailableException.Reason reason) {
        return new MViagensBackendUnavailableException(reason);
    }
}
