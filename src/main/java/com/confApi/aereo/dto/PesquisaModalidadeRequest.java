package com.confApi.aereo.dto;

import com.confApi.aereo.eNums.ModalidadeTarifa;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Contrato de entrada das pesquisas convencionais, sem mudar o payload legado enviado ao Hub. */
public interface PesquisaModalidadeRequest {
    ModalidadeTarifa getModalidadeTarifa();

    default void exigirPesquisaTradicional() {
        if (getModalidadeTarifa() == ModalidadeTarifa.OPER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A modalidade OPER exige o fluxo especifico de tarifa operadora.");
        }
    }
}
