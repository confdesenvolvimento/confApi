package com.confApi.db.confManager.historicoReserva;

import com.confApi.config.UrlConfig;
import com.confApi.db.AbstractTransactionServiceApi;
import com.confApi.db.confManager.historicoReserva.dto.HistoricoReserva;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HistoricoReservaApi extends AbstractTransactionServiceApi implements Serializable {

    private final String urlAPI ="/historicoReserva";


    public List<HistoricoReserva> findByCodgReservaHotel(Integer codgReservaHotel) {
        List<HistoricoReserva> historico = new ArrayList<>();
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String responseBody = sendHttpApiGetPublic(UrlConfig.URL_CONFIANCA_MANAGER + urlAPI + "/listarByCodgReservaHotel/" + codgReservaHotel);
            historico = objectMapper.readValue(responseBody, new TypeReference<List<HistoricoReserva>>() {
            });

        } catch (JsonProcessingException ex) {
            Logger.getLogger(HistoricoReservaApi.class.getName()).log(Level.SEVERE, null, ex);
        }
        return historico;
    }
}
