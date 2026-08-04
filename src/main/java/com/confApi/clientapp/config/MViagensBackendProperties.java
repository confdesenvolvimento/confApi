package com.confApi.clientapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.net.URI;
import java.util.Locale;

@Validated
@ConfigurationProperties(prefix = "mviagens.backend")
public class MViagensBackendProperties {

    private boolean enabled;
    private URI baseUrl = URI.create("http://127.0.0.1:8091");
    private String serviceToken = "";

    @Min(100)
    @Max(30000)
    private long connectTimeoutMs = 2000;

    @Min(100)
    @Max(30000)
    private long readTimeoutMs = 5000;

    @AssertTrue(message = "A configuracao habilitada do mViagensBackend e invalida")
    public boolean isEnabledConfigurationValid() {
        if (!enabled) {
            return true;
        }

        return isBaseUrlSafe(baseUrl)
                && serviceToken != null
                && serviceToken.equals(serviceToken.trim())
                && serviceToken.length() >= 32
                && serviceToken.length() <= 512;
    }

    private boolean isBaseUrlSafe(URI uri) {
        if (uri == null || !uri.isAbsolute() || uri.getHost() == null) {
            return false;
        }
        if (uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            return false;
        }

        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if ("https".equals(scheme)) {
            return true;
        }
        if (!"http".equals(scheme)) {
            return false;
        }

        String host = uri.getHost().toLowerCase(Locale.ROOT);
        return "localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public URI getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(URI baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getServiceToken() {
        return serviceToken;
    }

    public void setServiceToken(String serviceToken) {
        this.serviceToken = serviceToken;
    }

    public long getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(long connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public long getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(long readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
}
