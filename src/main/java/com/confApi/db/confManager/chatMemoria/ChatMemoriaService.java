package com.confApi.db.confManager.chatMemoria;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.confApp.dto.SandBoxResp;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.chatMemoria.dto.ChatMemoria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ChatMemoriaService {
    private final RestTemplate restTemplate;

    public ChatMemoriaService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Autowired
    private ConfAppService confAppService;

    public List<ChatMemoria> findByBase(String base) {
        try {
            // Obtém token (ajuste se usar outro service de autenticação)
            ConfAppResp token = confAppService.token();

            // Monta URL
            String urlBase = UrlConfig.URL_CONFIANCA_MANAGER;
            String url = String.format("%schatMemoria/base/%s", urlBase, base);
            System.out.println("findByBase -> " + url);

            //  Cabeçalhos da requisição
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + token.getToken());

            HttpEntity<Object> requestEntity = new HttpEntity<>(headers);

            //  Executa GET e converte automaticamente para lista
            ResponseEntity<List<ChatMemoria>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<ChatMemoria>>() {}
            );

            List<ChatMemoria> resultado = responseEntity.getBody();
            System.out.println("findByBase Result -> " + resultado);
            return resultado != null ? resultado : Collections.emptyList();

        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Erro ao buscar ChatMemoria por base", e);
            return Collections.emptyList();
        }
    }


}
