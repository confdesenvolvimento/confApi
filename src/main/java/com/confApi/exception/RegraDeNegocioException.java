package com.confApi.exception;

public class RegraDeNegocioException extends RuntimeException {
    private final int status;
    public RegraDeNegocioException(int status, String mensagem) {
        super(mensagem);
        this.status = status;
    }
    public int getStatus() { return status; }
}
