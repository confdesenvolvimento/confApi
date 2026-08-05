package com.confApi.clientapp.integration.enrollment;

public class ClientAppEnrollmentException extends RuntimeException {
    private final int status;
    private final String code;
    private final boolean retryable;

    public ClientAppEnrollmentException(int status, String code, boolean retryable) {
        super(code);
        this.status = status;
        this.code = code;
        this.retryable = retryable;
    }

    public int getStatus() { return status; }
    public String getCode() { return code; }
    public boolean isRetryable() { return retryable; }
}
