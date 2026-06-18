package com.confApi.exception;

public class ServiceIndisponivelException extends RuntimeException {
    public ServiceIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
