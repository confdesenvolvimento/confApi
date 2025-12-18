package com.confApi.db.confManager.usuario;

import com.confApi.config.UrlConfig;
import com.confApi.db.AbstractTransactionServiceApi;
import com.confApi.db.confManager.agencia.AgenciaApi;
import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.db.confManager.usuario.dto.autenticar.UsuarioResponseDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class UsuarioApi extends AbstractTransactionServiceApi implements Serializable {

    private final String urlAPI = UrlConfig.URL_CONFIANCA_MANAGER +"/wooba/turUsuarios";


    public UsuarioResponseDTO findUsuarioByDB(String login) {
        String url = "/loginDB/" + login;
        String responseBody = sendHttpApiGet(url);
        ObjectMapper objectMapper = new ObjectMapper();
        List<UsuarioResponseDTO> lista =null;
        try {
            // tenta como objeto
            return objectMapper.readValue(responseBody, UsuarioResponseDTO.class);
        } catch (Exception e) {
            try {
                // tenta como array
                lista = objectMapper.readValue(
                        responseBody,
                        new TypeReference<List<UsuarioResponseDTO>>() {
                        }
                );
                if (!lista.isEmpty()) return lista.get(0);
            } catch (Exception ex) {
                Logger.getLogger(AgenciaApi.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return lista.get(0);
    }

}
