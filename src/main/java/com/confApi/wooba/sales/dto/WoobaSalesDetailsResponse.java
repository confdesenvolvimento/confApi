package com.confApi.wooba.sales.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public class WoobaSalesDetailsResponse {

    @JsonProperty("Request")
    private JsonNode request;

    @JsonProperty("Transaction")
    private JsonNode transaction;

    @JsonProperty("Success")
    private Boolean success;

    @JsonProperty("Errors")
    private JsonNode errors;

    @JsonProperty("RequestDate")
    private String requestDate;

    public JsonNode getRequest() {
        return request;
    }

    public void setRequest(JsonNode request) {
        this.request = request;
    }

    public JsonNode getTransaction() {
        return transaction;
    }

    public void setTransaction(JsonNode transaction) {
        this.transaction = transaction;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public JsonNode getErrors() {
        return errors;
    }

    public void setErrors(JsonNode errors) {
        this.errors = errors;
    }

    public String getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(String requestDate) {
        this.requestDate = requestDate;
    }

    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }
}
