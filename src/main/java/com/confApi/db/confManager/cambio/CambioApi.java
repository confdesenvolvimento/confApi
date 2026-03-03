package com.confApi.db.confManager.cambio;

import com.confApi.config.UrlConfig;
import com.confApi.db.AbstractTransactionServiceApi;
import com.confApi.db.confManager.historicoReserva.HistoricoReservaApi;
import com.confApi.db.confManager.historicoReserva.dto.HistoricoReserva;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CambioApi  extends AbstractTransactionServiceApi implements Serializable {

    private final String urlAPI ="/cambio";


    public List<Cambio> findUltimoCambio() {
        List<Cambio> cambios = new ArrayList<>();
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String responseBody = sendHttpApiGetPublic(UrlConfig.URL_CONFIANCA_MANAGER + urlAPI + "/getUltimosRegistroCambio");
            cambios = objectMapper.readValue(responseBody, new TypeReference<List<Cambio>>() {
            });

        } catch (JsonProcessingException ex) {
            Logger.getLogger(HistoricoReservaApi.class.getName()).log(Level.SEVERE, null, ex);
        }
        return cambios;
    }
}

