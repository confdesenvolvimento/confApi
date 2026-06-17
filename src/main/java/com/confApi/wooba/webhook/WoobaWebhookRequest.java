package com.confApi.wooba.webhook;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WoobaWebhookRequest {

    @JsonProperty("Api")
    private String api;

    @JsonProperty("TransactionType")
    private Integer transactionType;

    @JsonProperty("TransactionTypeDescription")
    private String transactionTypeDescription;

    @JsonProperty("Id")
    private Long id;

    @JsonProperty("UniqueId")
    private String uniqueId;

    @JsonProperty("Locator")
    private String locator;

    @JsonProperty("Ticket")
    private String ticket;

    @JsonProperty("LastUpdate")
    private String lastUpdate;

    private final Map<String, Object> additionalFields = new LinkedHashMap<>();

    @JsonAnySetter
    public void addAdditionalField(String key, Object value) {
        additionalFields.put(key, value);
    }

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public Integer getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(Integer transactionType) {
        this.transactionType = transactionType;
    }

    public String getTransactionTypeDescription() {
        return transactionTypeDescription;
    }

    public void setTransactionTypeDescription(String transactionTypeDescription) {
        this.transactionTypeDescription = transactionTypeDescription;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getLocator() {
        return locator;
    }

    public void setLocator(String locator) {
        this.locator = locator;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Map<String, Object> getAdditionalFields() {
        return Collections.unmodifiableMap(additionalFields);
    }

    public String resumo() {
        return "Api=" + safe(api)
                + ", TransactionType=" + safe(transactionType)
                + ", TransactionTypeDescription=" + safe(transactionTypeDescription)
                + ", Id=" + safe(id)
                + ", UniqueId=" + safe(uniqueId)
                + ", Locator=" + safe(locator)
                + ", Ticket=" + safe(ticket)
                + ", LastUpdate=" + safe(lastUpdate);
    }

    private String safe(Object value) {
        if (value == null) {
            return "null";
        }
        String text = value.toString();
        return text.trim().isEmpty() ? "vazio" : text;
    }

    @Override
    public String toString() {
        return "WoobaWebhookRequest{" +
                "api='" + api + '\'' +
                ", transactionType=" + transactionType +
                ", transactionTypeDescription='" + transactionTypeDescription + '\'' +
                ", id=" + id +
                ", uniqueId='" + uniqueId + '\'' +
                ", locator='" + locator + '\'' +
                ", ticket='" + ticket + '\'' +
                ", lastUpdate='" + lastUpdate + '\'' +
                ", additionalFields=" + additionalFields +
                '}';
    }
}
