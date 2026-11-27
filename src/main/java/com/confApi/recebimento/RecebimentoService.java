package com.confApi.recebimento;

import com.confApi.carros.dto.CarroCompraModel;
import com.confApi.carros.dto.SistemaCarroHub;
import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.seguros.dto.SeguroCompraModel;
import com.confApi.operacao.RastreioReserva;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Service
public class RecebimentoService {

    private static final Logger LOG = LoggerFactory.getLogger(RecebimentoService.class);

    @Autowired
    private ConfAppService confAppService;

    @Autowired
    @Qualifier("recebimentoRedeRestTemplate")
    private RestTemplate restTemplate;

    public Recebimento criarRecebimento(SeguroCompraModel req) {
        if (req == null || req.getRecebimento() == null) return null;
        String sistemaNome = req.getSistema() != null ? req.getSistema().getNomeSistema() : "Hero";
        return criarRecebimento(new Recebimento(req.getRecebimento()), sistemaNome);
    }

    public Recebimento criarRecebimento(CarroCompraModel req) {
        if (req == null || req.getRecebimento() == null) {
            return null;
        }
        return criarRecebimento(new Recebimento(req.getRecebimento()), resolverSistemaNomeCarro(req));
    }

    private Recebimento criarRecebimento(Recebimento recebimento, String sistemaNome) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        RastreioReserva.propagar(headers);
        try {
            ConfAppResp token = confAppService.token();
            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            }
        } catch (Exception ex) {
            LOG.warn("Falha antes de enviar recebimento operacao={} tipo={}",
                    RastreioReserva.id(), ex.getClass().getSimpleName());
            return null;
        }

        Recebimento resposta;
        try {
            resposta = restTemplate.exchange(UrlConfig.URL_CONFIANCA_MANAGER + "recebimento/" + sistemaNome + "?autorizacao=false",
                    HttpMethod.POST, new HttpEntity<>(recebimento, headers), Recebimento.class).getBody();
        } catch (HttpStatusCodeException ex) {
            int status = ex.getRawStatusCode();
            LOG.warn("Manager não confirmou recebimento operacao={} status={}", RastreioReserva.id(), status);
            if (status == 409) throw PagamentoRedeEmConferencia.doConflito(ex);
            if (status >= 500) throw PagamentoRedeEmConferencia.doRecebimento(null);
            return null;
        } catch (RuntimeException ex) {
            LOG.warn("Falha após iniciar recebimento operacao={} tipo={}", RastreioReserva.id(), ex.getClass().getSimpleName());
            throw PagamentoRedeEmConferencia.doRecebimento(null);
        }
        if (resposta == null || resposta.getStatus() == null || Integer.valueOf(3).equals(resposta.getStatus())
                || Integer.valueOf(6).equals(resposta.getStatus())
                || Integer.valueOf(1).equals(resposta.getStatus()) && resposta.getCodgRecebimento() == null) {
            throw PagamentoRedeEmConferencia.doRecebimento(resposta);
        }
        return resposta;
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
        RastreioReserva.propagar(headers);

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
            LOG.warn("Falha ao cancelar recebimento operacao={} tipo={}",
                    RastreioReserva.id(), ex.getClass().getSimpleName());
            return null;

        }
    }
}
