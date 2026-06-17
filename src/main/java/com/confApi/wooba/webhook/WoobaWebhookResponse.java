package com.confApi.wooba.webhook;

public class WoobaWebhookResponse {

    private String status;
    private String message;
    private Boolean accepted;
    private Boolean airReservation;
    private String uniqueId;
    private String locator;
    private String transactionType;

    public WoobaWebhookResponse() {
    }

    public WoobaWebhookResponse(String status, String message, Boolean accepted,
                                Boolean airReservation, String uniqueId,
                                String locator, String transactionType) {
        this.status = status;
        this.message = message;
        this.accepted = accepted;
        this.airReservation = airReservation;
        this.uniqueId = uniqueId;
        this.locator = locator;
        this.transactionType = transactionType;
    }

    public static WoobaWebhookResponse receivedAirReservation(WoobaWebhookRequest request) {
        return new WoobaWebhookResponse(
                "RECEIVED",
                "Webhook de reserva aerea recebido para processamento.",
                true,
                true,
                request.getUniqueId(),
                request.getLocator(),
                request.getTransactionTypeDescription()
        );
    }

    public static WoobaWebhookResponse ignored(WoobaWebhookRequest request, String message) {
        return new WoobaWebhookResponse(
                "IGNORED",
                message,
                true,
                false,
                request != null ? request.getUniqueId() : null,
                request != null ? request.getLocator() : null,
                request != null ? request.getTransactionTypeDescription() : null
        );
    }

    public static WoobaWebhookResponse disabled(WoobaWebhookRequest request, String message) {
        return new WoobaWebhookResponse(
                "DISABLED",
                message,
                true,
                false,
                request != null ? request.getUniqueId() : null,
                request != null ? request.getLocator() : null,
                request != null ? request.getTransactionTypeDescription() : null
        );
    }

    public static WoobaWebhookResponse invalid(String message) {
        return new WoobaWebhookResponse(
                "INVALID",
                message,
                false,
                false,
                null,
                null,
                null
        );
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }

    public Boolean getAirReservation() {
        return airReservation;
    }

    public void setAirReservation(Boolean airReservation) {
        this.airReservation = airReservation;
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

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }
}
