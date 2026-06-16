package com.confApi.wooba.sales.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WoobaSalesCompanyCredentials {

    @JsonProperty("Identifier")
    private String identifier;

    @JsonProperty("Password")
    private String password;

    public WoobaSalesCompanyCredentials() {
    }

    public WoobaSalesCompanyCredentials(String identifier, String password) {
        this.identifier = identifier;
        this.password = password;
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
}
