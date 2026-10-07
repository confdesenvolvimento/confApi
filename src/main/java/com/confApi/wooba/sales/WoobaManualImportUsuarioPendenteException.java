package com.confApi.wooba.sales;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class WoobaManualImportUsuarioPendenteException extends ResponseStatusException {
    public WoobaManualImportUsuarioPendenteException() {
        super(HttpStatus.CONFLICT, "Usuario nao encontrado no Manager. Busque e selecione um login para continuar a importacao.");
    }
}
