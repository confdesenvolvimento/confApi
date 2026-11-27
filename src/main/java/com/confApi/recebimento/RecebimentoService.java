package com.confApi.recebimento;

import com.confApi.carros.dto.CarroCompraModel;
import com.confApi.carros.dto.SistemaCarroHub;
import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.rodoviario.dto.RodoviarioCompraModel;
import com.confApi.seguros.dto.SeguroCompraModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

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

    public Recebimento criarRecebimento(CarroCompraModel req) {
        if (req == null || req.getRecebimento() == null) {
            return null;
        }

        Recebimento recebimento = new Recebimento(req.getRecebimento());
        System.out.println("recebimento.getCodgReservaCarro():: " + recebimento.getCodgReservaCarro());

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        String sistemaNome = resolverSistemaNomeCarro(req);

        try {
            ConfAppResp token = confAppService.token();

            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            }

            HttpEntity<Recebimento> requestEntity =
                    new HttpEntity<>(recebimento, headers);

            ResponseEntity<Recebimento> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_MANAGER
                            + "recebimento/"
                            + sistemaNome
                            + "?autorizacao=false",
                            HttpMethod.POST,
                            requestEntity,
                            Recebimento.class
                    );

            return response.getBody();

        } catch (Exception ex) {
            System.out.println("ERRO AO CRIAR RECEBIMENTO CARRO: " + ex.getMessage());
            return null;
        }
    }

    /**
     * O Manager registra o faturado ou processa o cartão na Rede.
     * A falha é propagada para que o orquestrador confira a cobrança
     * antes de permitir outra tentativa.
     */
    public Recebimento criarRecebimento(RodoviarioCompraModel req) {
        if (req == null || req.getRecebimento() == null) {
            throw new IllegalArgumentException("Recebimento rodoviário obrigatório.");
        }

        Recebimento recebimento = new Recebimento(req.getRecebimento());
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        ConfAppResp token = confAppService.token();
        if (token == null || token.getToken() == null || token.getToken().isBlank()) {
            throw new IllegalStateException(
                    "Autenticação do Manager indisponível para o recebimento rodoviário."
            );
        }
        headers.setBearerAuth(token.getToken());

        ResponseEntity<Recebimento> response = restTemplate.exchange(
                UrlConfig.URL_CONFIANCA_MANAGER + "recebimento/RODOVIARIO?autorizacao=false",
                HttpMethod.POST,
                new HttpEntity<>(recebimento, headers),
                Recebimento.class
        );

        if (response.getBody() == null) {
            throw new IllegalStateException(
                    "O Manager não confirmou o recebimento rodoviário."
            );
        }
        return response.getBody();
    }

    private String resolverSistemaNomeCarro(CarroCompraModel req) {
        if (req == null
            || req.getReservaCarro() == null
            || req.getReservaCarro().getSistema() == null) {
            return "MB";
        }

        SistemaCarroHub sistema = req.getReservaCarro().getSistema();

        if (sistema.getNome() != null && !sistema.getNome().trim().isEmpty()) {
            return sistema.getNome().trim();
        }

        return "MB";
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