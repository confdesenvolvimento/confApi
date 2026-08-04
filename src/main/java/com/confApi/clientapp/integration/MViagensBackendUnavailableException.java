package com.confApi.clientapp.integration;

public final class MViagensBackendUnavailableException extends RuntimeException {

    public enum Reason {
        TECHNICAL_AUTHENTICATION,
        UPSTREAM_HTTP,
        CONNECTIVITY_OR_TIMEOUT,
        MALFORMED_RESPONSE
    }

    private final Reason reason;

    public MViagensBackendUnavailableException(Reason reason) {
        super("mViagensBackend unavailable");
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
