package com.confApi.wooba.sales.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WoobaSalesDetailsRequest {

    @JsonProperty("TransactionUniqueId")
    private String transactionUniqueId;

    @JsonProperty("OffSet")
    private String offset;

    @JsonProperty("AccessCredentials")
    private WoobaSalesAccessCredentials accessCredentials;

    public WoobaSalesDetailsRequest() {
    }

    public WoobaSalesDetailsRequest(String transactionUniqueId, String offset,
                                    WoobaSalesAccessCredentials accessCredentials) {
        this.transactionUniqueId = transactionUniqueId;
        this.offset = offset;
        this.accessCredentials = accessCredentials;
    }

    public String getTransactionUniqueId() {
        return transactionUniqueId;
    }

    public void setTransactionUniqueId(String transactionUniqueId) {
        this.transactionUniqueId = transactionUniqueId;
    }

    public String getOffset() {
        return offset;
    }

    public void setOffset(String offset) {
        this.offset = offset;
    }

    public WoobaSalesAccessCredentials getAccessCredentials() {
        return accessCredentials;
    }

    public void setAccessCredentials(WoobaSalesAccessCredentials accessCredentials) {
        this.accessCredentials = accessCredentials;
    }
}
