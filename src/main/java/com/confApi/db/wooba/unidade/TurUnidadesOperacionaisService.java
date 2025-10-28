package com.confApi.db.wooba.unidade;

import com.confApi.db.AbstractTransactionServiceApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class TurUnidadesOperacionaisService extends AbstractTransactionServiceApi implements Serializable {

    private final String urlAPI = "http://localhost:8082/turUnidadesOperacionais";

    public List<TurUnidadesOperacionais> findAll() {
        List<TurUnidadesOperacionais> turUnidadesOperacionaises = null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String requestBody = objectMapper.writeValueAsString(null);
            String responseBody = sendHttpApiGet(urlAPI);

            turUnidadesOperacionaises = objectMapper.readValue(responseBody, new TypeReference<List<TurUnidadesOperacionais>>() {
            });

        } catch (JsonProcessingException ex) {
            Logger.getLogger(TurUnidadesOperacionaisService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return turUnidadesOperacionaises;
    }

    public List<TurUnidadesOperacionais> findAllParamsCodgUnidade(Integer codgUnidade) {
        //Unidade unidade = null;
        List<TurUnidadesOperacionais> unidades = null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(urlAPI)
                    .queryParam("id", codgUnidade);
            String Url = builder.toUriString();

            String responseBody = sendHttpApiGet(Url);

            unidades = objectMapper.readValue(responseBody, new TypeReference<List<TurUnidadesOperacionais>>() {
            });
            // for (Unidade unidade1 : unidades) {
            //      unidade = unidade1;
            //  }

        } catch (JsonProcessingException ex) {
            Logger.getLogger(TurUnidadesOperacionaisService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return unidades;
    }

    public TurUnidadesOperacionais findByCodgUnidade(Integer codgUnidade) {
        TurUnidadesOperacionais unidade = null;
        List<TurUnidadesOperacionais> unidades = null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(urlAPI)
                    .queryParam("id", codgUnidade);
            String Url = builder.toUriString();

            String responseBody = sendHttpApiGet(Url);

            unidades = objectMapper.readValue(responseBody, new TypeReference<List<TurUnidadesOperacionais>>() {
            });
            for (TurUnidadesOperacionais unidade1 : unidades) {
                unidade = unidade1;
            }

        } catch (JsonProcessingException ex) {
            Logger.getLogger(TurUnidadesOperacionaisService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return unidade;
    }
}
