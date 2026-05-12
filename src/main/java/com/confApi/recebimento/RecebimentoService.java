package com.confApi.recebimento;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.db.confManager.seguro.reserva.SeguroReserva;
import com.confApi.seguros.dto.SeguroCompraModel;
import com.confApi.seguros.dto.SeguroViagemPesquisaDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Optional;
import java.util.logging.Level;

@Service
public class RecebimentoService {

    @Autowired
    private ConfAppService confAppService;

    @Autowired
    private RestTemplate restTemplate;

    public Recebimento criarRecebimento(SeguroCompraModel req) {
        Recebimento recebimento = new Recebimento(req.getRecebimento());
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        String sistemaNome = req.getSistema() != null ? req.getSistema().getNomeSistema() : "Hero";

        try {
            ConfAppResp token = confAppService.token();

            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            }

            HttpEntity<Recebimento> requestEntity = new HttpEntity<>(recebimento, headers);

            ResponseEntity<Recebimento> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_MANAGER + "recebimento/" + sistemaNome + "?autorizacao=false",
                            HttpMethod.POST,
                            requestEntity,
                            Recebimento.class
                    );

            return response.getBody();
        } catch (Exception ex) {
            System.out.println("ERRO: " + ex.getMessage());
            return null;

        }
    }

    public Recebimento cancelarRecebimento(Recebimento recebimento) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        try {
            ConfAppResp token = confAppService.token();

            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            }

            HttpEntity<Recebimento> requestEntity = new HttpEntity<>(recebimento, headers);

            ResponseEntity<Recebimento> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_MANAGER + "recebimento/cancelarRecebimento/" + recebimento.getCodgRecebimento(),
                            HttpMethod.PUT,
                            requestEntity,
                            Recebimento.class
                    );

            return response.getBody();
        } catch (Exception ex) {
            System.out.println("ERRO: " + ex.getMessage());
            return null;

        }
    }
}
