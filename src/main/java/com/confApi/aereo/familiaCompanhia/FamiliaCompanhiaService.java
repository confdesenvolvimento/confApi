package com.confApi.aereo.familiaCompanhia;

import com.confApi.aereo.AereoClientV2;
import com.confApi.aereo.dto.*;
import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.familia.dto.FamiliaCompanhia;
import com.confApi.db.confManager.familia.dto.FamiliaInformacoes;
import com.confApi.util.JsonLogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import javax.transaction.Transactional;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class FamiliaCompanhiaService {

    private static final Logger LOG = Logger.getLogger(AereoClientV2.class.getName());

    private final RestTemplate restTemplate;

    @Autowired
    private ConfAppService confAppService;
    @Autowired
    private UrlConfig urlConfig;

    public FamiliaCompanhiaService(@Qualifier("hubRestTemplate") RestTemplate hubRestTemplate) {
        this.restTemplate = hubRestTemplate;
    }

    public List<CompanhiaFamiliaModel> findByTipoRota(Integer idTipoRota) {

        List<FamiliaCompanhia> familiasCompanhias = get(
                "Aéreo - Recuperar Famílias por Tipo Rota",
                UrlConfig.URL_CONFIANCA_MANAGER + "/familiaCompanhia/findByTipoRota/" + idTipoRota,
                new ParameterizedTypeReference<List<FamiliaCompanhia>>() {},
                new ArrayList<>()
        );

        Map<String, CompanhiaFamiliaModel> map = new LinkedHashMap<>();

        for (FamiliaCompanhia fc : familiasCompanhias) {

            if (fc == null || fc.getCompanhiaAerea() == null) {
                continue;
            }

            String iata = fc.getCompanhiaAerea().getIataCia();

            CompanhiaFamiliaModel companhia = map.computeIfAbsent(iata, sigla -> {
                CompanhiaFamiliaModel cf = new CompanhiaFamiliaModel(sigla);
                cf.setNomeCompanhia(fc.getCompanhiaAerea().getNomeCia());
                return cf;
            });

            FamiliaModel fm = new FamiliaModel();
            fm.setCodgFamilia(fc.getCodSigla());
            fm.setColor(fc.getCorFamilia());
            fm.setDescFamilia(fc.getNomeFamiliaCompanhiaDescricao());
            fm.setNomeFamilia(fc.getNomeFamiliaCompanhia());
            fm.setSiglaCompanhia(iata);
            fm.setTipoRota(fc.getTipoRota());

            fm.setIdOrdenacao(
                    fc.getPosicao() != null
                            ? fc.getPosicao()
                            : fc.getCodgFamiliaCompanhia()
            );

            if (fc.getFamiliaInformacoes() != null) {
                for (FamiliaInformacoes info : fc.getFamiliaInformacoes()) {
                    Integer posicao = info.getPosicao() != null ? info.getPosicao() : 0;

                    fm.getFamiliaDetalhes().add(
                            new FamiliaDetalheModel(
                                    posicao,
                                    info.getDescricao(),
                                    info.getFlagContempla()
                            )
                    );
                }

                fm.getFamiliaDetalhes()
                        .sort(Comparator.comparing(FamiliaDetalheModel::getPosicao));
            }
            companhia.getFamilias().add(fm);
        }
        List<CompanhiaFamiliaModel> response = new ArrayList<>(map.values());

        for (CompanhiaFamiliaModel cf : response) {
            cf.getFamilias().sort(
                    Comparator.comparing(FamiliaModel::getIdOrdenacao)
            );
        }

        return response;
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

    private <REQ, RES> RES get(
            String operacao,
            String endpoint,
            ParameterizedTypeReference<RES> responseType,
            RES retornoPadrao
    ) {
        long inicio = System.currentTimeMillis();

        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<REQ> entity = new HttpEntity<>(null, headers);

            JsonLogUtil.logRequest(operacao, null);

            ResponseEntity<RES> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.GET,
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
                    new Object[]{operacao, response.getStatusCode(), endpoint}
            );

        } catch (Exception e) {
            tratarErro(operacao, endpoint, inicio, e);
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
