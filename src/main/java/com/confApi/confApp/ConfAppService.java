package com.confApi.confApp;

import com.confApi.confApp.dto.SandBoxDto;
import com.confApi.confApp.dto.SandBoxResp;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.security.auth.message.AuthException;
import java.io.IOException;
import java.util.Arrays;

@Service
public class ConfAppService {
    private final String urlSandBox = "http://192.168.171.45/SandBox/user/auth";

    public SandBoxResp token() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        SandBoxDto sandBoxDto = new SandBoxDto();
        sandBoxDto.setLogin("api.limits");
        sandBoxDto.setSenha("@Limits2023");
        HttpEntity<SandBoxDto> requestEntity = new HttpEntity<SandBoxDto>(sandBoxDto, headers);
        ResponseEntity<SandBoxResp> responseEntity = null;
        try {
            // Faz a solicitação HTTP usando RestTemplate
            responseEntity = new RestTemplate().exchange(urlSandBox, HttpMethod.POST, requestEntity, SandBoxResp.class);
        } catch (HttpClientErrorException.BadRequest ex) {
            // Captura a exceção e lança uma exceção personalizada com a mensagem de erro
            String responseBody = ex.getResponseBodyAsString();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = null;
            try {
                root = objectMapper.readTree(responseBody);
                String errorMessage = root.get("errors").get(0).asText();
                new AuthException(errorMessage);
            } catch (IOException ex1) {
                new AuthException("Erro ao processar resposta do servidor");
            }
        }

        return responseEntity.getBody();
    }
}
