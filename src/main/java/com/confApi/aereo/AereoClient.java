package com.confApi.aereo;

import com.confApi.aereo.dto.*;
import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.util.JsonLogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class AereoClient {

    private static final Logger LOG = Logger.getLogger(AereoClient.class.getName());

    private static final String API_AEREO = "/api/aereo";

    private final RestTemplate restTemplate;

    @Autowired
    private ConfAppService confAppService;

    public AereoClient(@Qualifier("hubRestTemplate") RestTemplate hubRestTemplate) {
        this.restTemplate = hubRestTemplate;
    }

    public BuscarFormasFinanciamentoResponse recuperarFormasFinanciamentoToken(BuscarFormasFinanciamentoRequest request) {
        return post(
                "Aéreo - Recuperar Formas de Financiamento Token",
                API_AEREO + "/recuperarformasdefinanciamentoToken",
                request,
                BuscarFormasFinanciamentoResponse.class,
                new BuscarFormasFinanciamentoResponse()
        );
    }

    public List<PesquisaResponse> tarifarPesquisa(TarifarPesquisaRequest request) {
        return post(
                "Aéreo - Tarifar Pesquisa",
                API_AEREO + "/tarifarpesquisa",
                request,
                new ParameterizedTypeReference<List<PesquisaResponse>>() {},
                new ArrayList<>()
        );
    }

    public RemoverAssentoResponse removerAssento(RemoverAssentoRequest request) {
        return post(
                "Aéreo - Remover Assento",
                API_AEREO + "/removerassento",
                request,
                RemoverAssentoResponse.class,
                new RemoverAssentoResponse()
        );
    }

    public MarcarAssentoResponse marcarAssento(MarcarAssentoRequest request) {
        return post(
                "Aéreo - Marcar Assento",
                API_AEREO + "/marcarassento",
                request,
                MarcarAssentoResponse.class,
                new MarcarAssentoResponse()
        );
    }

    public MapaAssentoResponse buscarMapaAssentos(MapaAssentoRequest request) {
        return post(
                "Aéreo - Buscar Mapa de Assentos",
                API_AEREO + "/mapadeassentos",
                request,
                MapaAssentoResponse.class,
                new MapaAssentoResponse()
        );
    }

    public BuscarFormasFinanciamentoResponse recuperarFormasFinanciamento(BuscarFormasFinanciamentoRequest request) {
        return post(
                "Aéreo - Recuperar Formas de Financiamento",
                API_AEREO + "/recuperarformasdefinanciamento",
                request,
                BuscarFormasFinanciamentoResponse.class,
                new BuscarFormasFinanciamentoResponse()
        );
    }

    public ConsultarEticketResponse cancelarBilhete(CancelarBilheteRequest request) {
        return post(
                "Aéreo - Cancelar Bilhete",
                API_AEREO + "/cancelarbilhete",
                request,
                ConsultarEticketResponse.class,
                new ConsultarEticketResponse()
        );
    }

    public CancelarReservaResponse cancelarReserva(CancelarReservaRequest request) {
        return post(
                "Aéreo - Cancelar Reserva",
                API_AEREO + "/cancelar",
                request,
                CancelarReservaResponse.class,
                new CancelarReservaResponse()
        );
    }

    public EmitirResponse emitir(EmitirRequest request) {
        return post(
                "Aéreo - Emitir",
                API_AEREO + "/emitir",
                request,
                EmitirResponse.class,
                new EmitirResponse()
        );
    }

    public ConsultarLocalizadorResponse carregarReserva(ConsultarLocalizadorRequest consultarRequest) {
        return post(
                "Aéreo - Consultar Localizador",
                API_AEREO + "/consultar",
                consultarRequest,
                ConsultarLocalizadorResponse.class,
                new ConsultarLocalizadorResponse()
        );
    }

    public ReservarResponse reserva(ReservarRequest reservarRequest) {
        return post(
                "Aéreo - Reservar",
                API_AEREO + "/reservar",
                reservarRequest,
                ReservarResponse.class,
                new ReservarResponse()
        );
    }

    public TarifarResponse tarifar(TarifarRequest tarifarRequest) {
        return post(
                "Aéreo - Tarifar",
                API_AEREO + "/tarifar",
                tarifarRequest,
                TarifarResponse.class,
                new TarifarResponse()
        );
    }

    public List<PesquisaResponse> pesquisarDisponibilidade(PesquisaRequestDTO pesquisaRequestDTO) {
        return post(
                "Aéreo - Pesquisar Disponibilidade",
                API_AEREO + "/pesquisa",
                pesquisaRequestDTO,
                new ParameterizedTypeReference<List<PesquisaResponse>>() {},
                Collections.emptyList()
        );
    }

    private <REQ, RES> RES post(
            String operacao,
            String endpoint,
            REQ request,
            Class<RES> responseClass,
            RES retornoPadrao
    ) {
        String url = montarUrl(endpoint);
        long inicio = System.currentTimeMillis();

        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<REQ> entity = new HttpEntity<>(request, headers);

            JsonLogUtil.logRequest(operacao, request);

            ResponseEntity<RES> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    responseClass
            );

            JsonLogUtil.logResponse(operacao, response.getBody());

            logTempoExecucao(operacao, inicio);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            LOG.log(
                    Level.WARNING,
                    "{0} retornou status {1} sem corpo válido. URL: {2}",
                    new Object[]{operacao, response.getStatusCode(), url}
            );

        } catch (Exception e) {
            tratarErro(operacao, url, inicio, e);
        }

        return retornoPadrao;
    }

    private <REQ, RES> RES post(
            String operacao,
            String endpoint,
            REQ request,
            ParameterizedTypeReference<RES> responseType,
            RES retornoPadrao
    ) {
        String url = montarUrl(endpoint);
        long inicio = System.currentTimeMillis();

        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<REQ> entity = new HttpEntity<>(request, headers);

            JsonLogUtil.logRequest(operacao, request);

            ResponseEntity<RES> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    responseType
            );

            JsonLogUtil.logResponse(operacao, response.getBody());

            logTempoExecucao(operacao, inicio);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            LOG.log(
                    Level.WARNING,
                    "{0} retornou status {1} sem corpo válido. URL: {2}",
                    new Object[]{operacao, response.getStatusCode(), url}
            );

        } catch (Exception e) {
            tratarErro(operacao, url, inicio, e);
        }

        return retornoPadrao;
    }

    private String montarUrl(String endpoint) {
        return UrlConfig.URL_CONFIANCA_HUB + endpoint;
    }

    private HttpHeaders defaultHeaders(String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(bearerToken);
        return headers;
    }

    private void tratarErro(String operacao, String url, long inicio, Exception e) {
        logTempoExecucao(operacao, inicio);

        if (e instanceof ResourceAccessException) {
            tratarErroAcesso(operacao, url, e);
            return;
        }

        if (e instanceof RestClientResponseException) {
            tratarErroHttp(operacao, url, (RestClientResponseException) e);
            return;
        }

        LOG.log(
                Level.SEVERE,
                operacao + " - Erro inesperado ao consumir HUB. URL: " + url,
                e
        );
    }

    private void tratarErroAcesso(String operacao, String url, Exception e) {
        if (isTimeout(e)) {
            LOG.log(
                    Level.SEVERE,
                    operacao + " - Timeout ao consumir HUB. URL: " + url
                            + ". Verifique se o HUB está ativo, se a URL está correta, "
                            + "se o endpoint está demorando demais ou se o readTimeout do RestTemplate está baixo.",
                    e
            );
            return;
        }

        LOG.log(
                Level.SEVERE,
                operacao + " - Erro de conexão/acesso ao consumir HUB. URL: " + url,
                e
        );
    }

    private void tratarErroHttp(String operacao, String url, RestClientResponseException e) {
        LOG.log(
                Level.SEVERE,
                operacao + " - Erro HTTP ao consumir HUB. URL: " + url
                        + ", Status: " + e.getRawStatusCode()
                        + ", ResponseBody: " + e.getResponseBodyAsString(),
                e
        );
    }

    private boolean isTimeout(Throwable e) {
        Throwable causa = e;

        while (causa != null) {
            if (causa instanceof SocketTimeoutException) {
                return true;
            }

            causa = causa.getCause();
        }

        return false;
    }

    private void logTempoExecucao(String operacao, long inicio) {
        long fim = System.currentTimeMillis();
        long tempoMs = fim - inicio;

        LOG.log(
                Level.INFO,
                "{0} - Tempo de execução no HUB: {1} ms",
                new Object[]{operacao, tempoMs}
        );
    }
}