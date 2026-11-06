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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class AereoClient {

    private static final Logger LOG = Logger.getLogger(AereoClient.class.getName());

    private static final String API_AEREO = "/api/aereo";

    private final RestTemplate restTemplate;

    @Autowired
    private ConfAppService confAppService;

    private final RestTemplate operadoraRestTemplate;

    public AereoClient(RestTemplate hubRestTemplate) { this(hubRestTemplate, hubRestTemplate); }

    @Autowired
    public AereoClient(@Qualifier("hubRestTemplate") RestTemplate hubRestTemplate,
            @Qualifier("hubOperadoraRestTemplate") RestTemplate operadoraRestTemplate) {
        this.restTemplate = hubRestTemplate;
        this.operadoraRestTemplate = operadoraRestTemplate;
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

    public ConsultarEticketResponse cancelarBilhete(CancelarBilheteRequest request) { return cancelarBilhete(request, null); }

    public ConsultarEticketResponse cancelarBilhete(CancelarBilheteRequest request, String usuario) {
        if (modalidadeOperadora(request == null ? null : request.getTipoTarifa())) {
            validarRegistroOperadora(request.getCodgReservaAereo(), request.getLocalizador());
            return operadora("/cancelarbilhete", request, usuario, ConsultarEticketResponse.class);
        }
        return post(
                "Aéreo - Cancelar Bilhete",
                API_AEREO + "/cancelarbilhete",
                request,
                ConsultarEticketResponse.class,
                new ConsultarEticketResponse()
        );
    }

    public CancelarReservaResponse cancelarReserva(CancelarReservaRequest request) { return cancelarReserva(request, null); }

    public CancelarReservaResponse cancelarReserva(CancelarReservaRequest request, String usuario) {
        if (modalidadeOperadora(request == null ? null : request.getTipoTarifa())) {
            validarRegistroOperadora(request.getCodgReservaAereo(), request.getLocalizador());
            return operadora("/cancelar", request, usuario, CancelarReservaResponse.class);
        }
        return post(
                "Aéreo - Cancelar Reserva",
                API_AEREO + "/cancelar",
                request,
                CancelarReservaResponse.class,
                new CancelarReservaResponse()
        );
    }

    public EmitirResponse emitir(EmitirRequest request) {
        return emitir(request, new HttpHeaders());
    }

    public EmitirResponse emitir(EmitirRequest request, HttpHeaders antifraude) {
        String url = montarUrl(API_AEREO + "/emitir");
        try {
            ConfAppResp token = confAppService.token();
            HttpHeaders headers = defaultHeaders(token.getToken());
            for (String name : List.of("X-Usuario-Token", "X-Antifraude-Operacao", "X-Antifraude-IP", "X-Antifraude-Contexto", "X-Antifraude-Sessao")) {
                String value = antifraude == null ? null : antifraude.getFirst(name);
                if (value != null && !value.isBlank()) headers.set(name, value);
            }
            ResponseEntity<EmitirResponse> response = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(request, headers), EmitirResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) return response.getBody();
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "EMISSAO_AEREA_FALHA tipo={0}", ex.getClass().getSimpleName());
        }
        throw new IllegalStateException("Não foi possível confirmar o resultado da emissão. Consulte a reserva antes de tentar novamente.");
    }

    public ConsultarLocalizadorResponse carregarReserva(ConsultarLocalizadorRequest request) { return carregarReserva(request, null); }

    public ConsultarLocalizadorResponse carregarReserva(ConsultarLocalizadorRequest consultarRequest, String usuario) {
        if (modalidadeOperadora(consultarRequest == null ? null : consultarRequest.getTipoTarifa())) {
            if (consultarRequest.getCodgReservaAereo() == null || consultarRequest.getCodgReservaAereo() <= 0
                    || consultarRequest.getLocalizador() == null || !consultarRequest.getLocalizador().matches("[A-Za-z0-9._-]{3,45}"))
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o registro da reserva OPER para consultar.");
            return operadora("/consultar", consultarRequest, usuario, ConsultarLocalizadorResponse.class);
        }
        return post(
                "Aéreo - Consultar Localizador",
                API_AEREO + "/consultar",
                consultarRequest,
                ConsultarLocalizadorResponse.class,
                new ConsultarLocalizadorResponse()
        );
    }

    /** Read-only lookup that never reports an integration failure as an empty reservation list. */
    public ConsultarLocalizadorResponse carregarReservaEstrita(ConsultarLocalizadorRequest consultarRequest) {
        if (modalidadeOperadora(consultarRequest == null ? null : consultarRequest.getTipoTarifa())) return carregarReserva(consultarRequest, null);
        return post(
                "Aéreo - Validar Localizador",
                API_AEREO + "/consultar",
                consultarRequest,
                ConsultarLocalizadorResponse.class,
                null,
                true);
    }

    /** Retorna as formas de pagamento permitidas pelo HUB para o localizador. */
    public Map<String, Object> iniciarEmissao(ConsultarLocalizadorRequest consultarRequest) {
        if (modalidadeOperadora(consultarRequest == null ? null : consultarRequest.getTipoTarifa()))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT, "O pagamento e a emissão OPER devem seguir pelo pacote.");
        return post(
                "Aéreo - Iniciar Emissão",
                API_AEREO + "/iniciaremissao",
                consultarRequest,
                new ParameterizedTypeReference<Map<String, Object>>() {},
                Collections.emptyMap()
        );
    }

    public ReservarResponse reserva(ReservarRequest reservarRequest) { return reserva(reservarRequest, null); }

    public ReservarResponse reserva(ReservarRequest reservarRequest, String usuario) {
        if (modalidadeOperadora(reservarRequest == null ? null : reservarRequest.getTipoTarifa())) {
            validarSelecaoOperadora(reservarRequest.getCriteriosOperadora(), reservarRequest.getChaveSelecao());
            return operadora("/reservar", reservarRequest, usuario, ReservarResponse.class);
        }
        return post(
                "Aéreo - Reservar",
                API_AEREO + "/reservar",
                reservarRequest,
                ReservarResponse.class,
                new ReservarResponse()
        );
    }

    public TarifarResponse tarifar(TarifarRequest tarifarRequest) { return tarifar(tarifarRequest, null); }

    public TarifarResponse tarifar(TarifarRequest tarifarRequest, String usuario) {
        if (modalidadeOperadora(tarifarRequest == null ? null : tarifarRequest.getTipoTarifa())) {
            validarSelecaoOperadora(tarifarRequest.getCriteriosOperadora(), tarifarRequest.getChaveSelecao());
            return operadora("/tarifar", tarifarRequest, usuario, TarifarResponse.class);
        }
        return post(
                "Aéreo - Tarifar",
                API_AEREO + "/tarifar",
                tarifarRequest,
                TarifarResponse.class,
                new TarifarResponse()
        );
    }

    private static boolean modalidadeOperadora(String tipo) {
        if (tipo == null || tipo.isBlank() || "CONS".equals(tipo)) return false;
        if ("OPER".equals(tipo)) return true;
        throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de tarifa inválido.");
    }
    private static void validarRegistroOperadora(Integer id, String localizador) {
        if (id == null || id <= 0 || localizador == null || !localizador.matches("[A-Za-z0-9._-]{3,45}"))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o registro da reserva OPER para cancelar.");
    }
    private static void validarSelecaoOperadora(com.fasterxml.jackson.databind.JsonNode criterios, String chave) {
        if (criterios == null || !criterios.isObject() || chave == null || !chave.matches("[a-f0-9]{64}"))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione novamente a família da tarifa OPER.");
    }
    private <T> T operadora(String rota, Object request, String usuario, Class<T> tipo) {
        if (usuario == null || usuario.isBlank() || usuario.length() > 16000 || usuario.chars().anyMatch(Character::isISOControl))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Renove sua sessão para continuar a tarifa OPER.");
        try {
            var token = confAppService.token();
            if (token == null || token.getToken() == null || token.getToken().isBlank()) throw new IllegalStateException();
            var headers = defaultHeaders(token.getToken());
            headers.set("X-Usuario-Token", usuario);
            var response = operadoraRestTemplate.exchange(montarUrl(API_AEREO + rota), HttpMethod.POST,
                    new HttpEntity<>(new CorpoOperadora(request), headers), tipo);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) throw new IllegalStateException();
            return response.getBody();
        } catch (RestClientResponseException ex) {
            HttpStatus status = HttpStatus.resolve(ex.getRawStatusCode());
            if (status == null || !status.is4xxClientError()) status = HttpStatus.BAD_GATEWAY;
            throw new org.springframework.web.server.ResponseStatusException(status,
                    "A condição OPER não foi confirmada. Confira a tarifa e o andamento da reserva antes de continuar.");
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "AEREO_OPER_FALHA operacao={0} tipo={1}", new Object[]{rota, ex.getClass().getSimpleName()});
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível confirmar o retorno OPER. Consulte o andamento antes de tentar reservar novamente.");
        }
    }
    private static final class CorpoOperadora {
        private final Object dados;
        CorpoOperadora(Object dados) { this.dados = dados; }
        @com.fasterxml.jackson.annotation.JsonValue public Object dados() { return dados; }
        @Override public String toString() { return "AereoOPER[dados protegidos]"; }
    }

    /** Read-only repricing for chat: never converts integration errors into an empty DTO. */
    public TarifarResponse tarifarEstrita(TarifarRequest request) {
        long inicio = System.currentTimeMillis();
        try {
            ConfAppResp token = confAppService.token();
            ResponseEntity<TarifarResponse> response = restTemplate.exchange(
                    montarUrl(API_AEREO + "/tarifar"), HttpMethod.POST,
                    new HttpEntity<>(request, defaultHeaders(token.getToken())), TarifarResponse.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new TarifacaoException("HTTP_ERRO");
            }
            if (response.getBody() == null) throw new TarifacaoException("RESPOSTA_VAZIA");
            return response.getBody();
        } catch (TarifacaoException ex) {
            throw ex;
        } catch (Exception ex) {
            String codigo = isTimeout(ex) ? "TIMEOUT"
                    : ex instanceof RestClientResponseException ? "HTTP_ERRO" : "ERRO_INTEGRACAO";
            // No exception message/cause/body: these may contain credentials or customer data.
            LOG.log(Level.WARNING, "REMARCACAO_TARIFACAO_FALHA codigo={0} tipo={1}",
                    new Object[]{codigo, ex.getClass().getSimpleName()});
            throw new TarifacaoException(codigo);
        } finally {
            logTempoExecucao("Aereo - Tarifar Remarcacao", inicio);
        }
    }

    public static class TarifacaoException extends IllegalStateException {
        private final String codigo;

        public TarifacaoException(String codigo) {
            super("Tarifacao nao concluida.");
            this.codigo = codigo;
        }

        public String getCodigo() { return codigo; }
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

    /** A remarcacao precisa distinguir uma consulta vazia de uma falha tecnica. */
    public List<PesquisaResponse> pesquisarDisponibilidadeEstrita(PesquisaRequestDTO pesquisaRequestDTO) {
        return post(
                "Aereo - Pesquisar Disponibilidade Remarcacao",
                API_AEREO + "/pesquisa",
                pesquisaRequestDTO,
                new ParameterizedTypeReference<List<PesquisaResponse>>() {},
                Collections.emptyList(),
                true
        );
    }

    public static class ConsultaDisponibilidadeException extends IllegalStateException {
        private final String codigo;

        public ConsultaDisponibilidadeException(String codigo) {
            super("Consulta de disponibilidade nao concluida.");
            this.codigo = codigo;
        }

        public String getCodigo() {
            return codigo;
        }
    }

    public List<PesquisaResponse> pesquisarDisponibilidadeV2(PesquisaRequestDTOV2 pesquisaRequestDTO) {
        return post(
                "Aéreo - Pesquisar Disponibilidade V2",
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
        return post(operacao, endpoint, request, responseClass, retornoPadrao, false);
    }

    private <REQ, RES> RES post(
            String operacao,
            String endpoint,
            REQ request,
            Class<RES> responseClass,
            RES retornoPadrao,
            boolean resultadoEstrito
    ) {
        validarPesquisaTradicional(request);
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
            if (resultadoEstrito) {
                throw new IllegalStateException("Nao foi possivel consultar a reserva no HUB.", e);
            }
        }

        if (resultadoEstrito) {
            throw new IllegalStateException("A consulta da reserva retornou uma resposta invalida.");
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
        return post(operacao, endpoint, request, responseType, retornoPadrao, false);
    }

    private <REQ, RES> RES post(
            String operacao,
            String endpoint,
            REQ request,
            ParameterizedTypeReference<RES> responseType,
            RES retornoPadrao,
            boolean resultadoEstrito
    ) {
        validarPesquisaTradicional(request);
        String url = montarUrl(endpoint);
        long inicio = System.currentTimeMillis();

        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<REQ> entity = new HttpEntity<>(request, headers);

            if (!resultadoEstrito) JsonLogUtil.logRequest(operacao, request);

            ResponseEntity<RES> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    responseType
            );

            if (!resultadoEstrito) JsonLogUtil.logResponse(operacao, response.getBody());

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
            if (resultadoEstrito) {
                String codigo = isTimeout(e) ? "TIMEOUT" : "ERRO_CONSULTA";
                LOG.log(Level.WARNING, "REMARCACAO_DISPONIBILIDADE_FALHA codigo={0} tipo={1}",
                        new Object[]{codigo, e.getClass().getSimpleName()});
                throw new ConsultaDisponibilidadeException(codigo);
            }
            tratarErro(operacao, url, inicio, e);
        }

        if (resultadoEstrito) {
            throw new ConsultaDisponibilidadeException("RESPOSTA_INVALIDA");
        }
        return retornoPadrao;
    }

    private void validarPesquisaTradicional(Object request) {
        if (request instanceof PesquisaModalidadeRequest pesquisa) {
            pesquisa.exigirPesquisaTradicional();
        }
    }

    private String montarUrl(String endpoint) {
        String baseUrl = UrlConfig.URL_CONFIANCA_HUB;
        if (baseUrl.endsWith("/") && endpoint.startsWith("/")) {
            return baseUrl + endpoint.substring(1);
        }
        if (!baseUrl.endsWith("/") && !endpoint.startsWith("/")) {
            return baseUrl + "/" + endpoint;
        }
        return baseUrl + endpoint;
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
