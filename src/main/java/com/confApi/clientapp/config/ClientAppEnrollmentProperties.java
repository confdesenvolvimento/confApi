package com.confApi.clientapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.AssertTrue;
import java.net.URI;
import java.util.Locale;

@Validated
@ConfigurationProperties(prefix = "mviagens.enrollment")
public class ClientAppEnrollmentProperties {
    private boolean enabled;
    private URI managerBaseUrl = URI.create("http://127.0.0.1:8082/");
    private String passengerBaseByCpfPath = "";
    private String agencyByCodePath = "/agencia";
    private String otpPreferredChannel = "EMAIL";

    @AssertTrue(message = "A configuracao de cadastro do cliente final e invalida")
    public boolean isConfigurationValid() {
        if (!enabled) return true;
        if (managerBaseUrl == null || !managerBaseUrl.isAbsolute() || managerBaseUrl.getHost() == null
                || managerBaseUrl.getUserInfo() != null || managerBaseUrl.getQuery() != null
                || managerBaseUrl.getFragment() != null) return false;
        String scheme = managerBaseUrl.getScheme().toLowerCase(Locale.ROOT);
        String host = managerBaseUrl.getHost().toLowerCase(Locale.ROOT);
        if (!("https".equals(scheme) || ("http".equals(scheme)
                && ("localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host))))) return false;
        return passengerBaseByCpfPath != null && passengerBaseByCpfPath.startsWith("/")
                && agencyByCodePath != null && agencyByCodePath.startsWith("/")
                && ("EMAIL".equals(otpPreferredChannel) || "SMS".equals(otpPreferredChannel)
                || "WHATSAPP".equals(otpPreferredChannel));
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public URI getManagerBaseUrl() { return managerBaseUrl; }
    public void setManagerBaseUrl(URI managerBaseUrl) { this.managerBaseUrl = managerBaseUrl; }
    public String getPassengerBaseByCpfPath() { return passengerBaseByCpfPath; }
    public void setPassengerBaseByCpfPath(String passengerBaseByCpfPath) { this.passengerBaseByCpfPath = passengerBaseByCpfPath; }
    public String getAgencyByCodePath() { return agencyByCodePath; }
    public void setAgencyByCodePath(String agencyByCodePath) { this.agencyByCodePath = agencyByCodePath; }
    public String getOtpPreferredChannel() { return otpPreferredChannel; }
    public void setOtpPreferredChannel(String otpPreferredChannel) { this.otpPreferredChannel = otpPreferredChannel; }
}
