package com.confApi.wooba.sales;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "wooba.sales")
public class WoobaSalesProperties {

    private String baseUrl = "https://sales.confianca.tur.br/Salesv1/api/v1/sales/";
    private String identifier ="api.sales.hub";
    private String password ="dzRV41P4r48Q";
    private String offset = "-03:00:00";
    private String developerToken ="8c9e1803-c944-4d60-89c3-ac0f00f3e489";
    private String developerAccessCode;
    private String developerTokenHeaderName = "developer-token";
    private String developerAccessCodeHeaderName = "developer-access-code";
    private String developerAccessCodePublicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDFmk9er35aE2rNG18mZiQg3OPFJi8wHPF+"
            + "gp6T1stNWwNmg6XfzLZqaGEwARUdIWt+fgIJ9vrxEORqNVdbzMqvr6a8a6J"
            + "fkre1ird4h4Bx8Sb3dyegRjyc4isMUkJp8EZSHP56j+Ja06ECBD4K38tnPWZl8hClnNhJ0avHktt5CQIDAQAB";
    private String developerAccessCodePrefix = "3BMPT7Z3699O|";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOffset() {
        return offset;
    }

    public void setOffset(String offset) {
        this.offset = offset;
    }

    public String getDeveloperToken() {
        return developerToken;
    }

    public void setDeveloperToken(String developerToken) {
        this.developerToken = developerToken;
    }

    public String getDeveloperAccessCode() {
        return developerAccessCode;
    }

    public void setDeveloperAccessCode(String developerAccessCode) {
        this.developerAccessCode = developerAccessCode;
    }

    public String getDeveloperTokenHeaderName() {
        return developerTokenHeaderName;
    }

    public void setDeveloperTokenHeaderName(String developerTokenHeaderName) {
        this.developerTokenHeaderName = developerTokenHeaderName;
    }

    public String getDeveloperAccessCodeHeaderName() {
        return developerAccessCodeHeaderName;
    }

    public void setDeveloperAccessCodeHeaderName(String developerAccessCodeHeaderName) {
        this.developerAccessCodeHeaderName = developerAccessCodeHeaderName;
    }

    public String getDeveloperAccessCodePublicKey() {
        return developerAccessCodePublicKey;
    }

    public void setDeveloperAccessCodePublicKey(String developerAccessCodePublicKey) {
        this.developerAccessCodePublicKey = developerAccessCodePublicKey;
    }

    public String getDeveloperAccessCodePrefix() {
        return developerAccessCodePrefix;
    }

    public void setDeveloperAccessCodePrefix(String developerAccessCodePrefix) {
        this.developerAccessCodePrefix = developerAccessCodePrefix;
    }
}
