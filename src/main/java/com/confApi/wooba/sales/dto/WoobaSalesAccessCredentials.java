package com.confApi.wooba.sales.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WoobaSalesAccessCredentials {

    @JsonProperty("Company")
    private WoobaSalesCompanyCredentials company;

    public WoobaSalesAccessCredentials() {
    }

    public WoobaSalesAccessCredentials(WoobaSalesCompanyCredentials company) {
        this.company = company;
    }

    public WoobaSalesCompanyCredentials getCompany() {
        return company;
    }

    public void setCompany(WoobaSalesCompanyCredentials company) {
        this.company = company;
    }
}
