package com.confApi.db.confManager.bandeira;

import com.confApi.config.UrlConfig;
import com.confApi.db.AbstractTransactionServiceApi;
import com.confApi.hub.aereo.BandeiraModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class BandeiraService extends AbstractTransactionServiceApi implements Serializable {

    private final String urlAPI = "bandeira";

    public List<BandeiraModel> findByAll() {
        List<BandeiraModel> bandeirasModel = new ArrayList<>();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String responseBody = sendHttpApiGet( UrlConfig.URL_CONFIANCA_MANAGER+urlAPI);
            List<Bandeira> bandeiras = objectMapper.readValue(responseBody, new TypeReference<List<Bandeira>>() {
            });

            for (Bandeira s : bandeiras) {
                bandeirasModel.add(new BandeiraModel(s.getCodgBandeira(), s.getNomeBandeira(), s.getSiglaBandeira()));
            }
        } catch (JsonProcessingException ex) {
            Logger.getLogger(BandeiraService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return bandeirasModel;
    }

    public Bandeira findByNome(String nome) {
        String url = urlAPI + "/" + nome;
        Bandeira bandeiraC = null;
        ObjectMapper objectMapper = new ObjectMapper();
        String responseBody = sendHttpApiGet(url);
        try {
            bandeiraC = objectMapper.readValue(responseBody, Bandeira.class);
        } catch (JsonProcessingException ex) {
            Logger.getLogger(BandeiraService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return bandeiraC;
    }
}
