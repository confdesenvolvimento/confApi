package com.confApi.db.confManager.agencia;

import com.confApi.config.UrlConfig;
import com.confApi.db.AbstractTransactionServiceApi;
import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.db.confManager.bandeira.Bandeira;
import com.confApi.db.confManager.bandeira.BandeiraService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AgenciaApi extends AbstractTransactionServiceApi implements Serializable {

    private final String urlAPI = "agencia";

    public Agencia findCodgAgencia(Integer codgAgencia) {
        String url = UrlConfig.URL_CONFIANCA_MANAGER + urlAPI + "?codgAgencia=" + codgAgencia;
        String responseBody = sendHttpApiGet(url);
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // tenta como objeto
            return objectMapper.readValue(responseBody, Agencia.class);
        } catch (Exception e) {
            try {
                // tenta como array
                List<Agencia> lista = objectMapper.readValue(
                        responseBody,
                        new TypeReference<List<Agencia>>() {
                        }
                );
                if (!lista.isEmpty()) return lista.get(0);
            } catch (Exception ex) {
                Logger.getLogger(AgenciaApi.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }
}