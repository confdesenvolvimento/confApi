package com.confApi.db.confManager.faturas;


import com.confApi.confApp.ConfAppService;
import com.confApi.confApp.dto.SandBoxResp;
import com.confApi.config.UrlConfig;

import com.confApi.db.confManager.faturas.dto.DetalharFaturaRS;
import com.confApi.db.confManager.faturas.dto.FaturaRQ;
import com.confApi.db.confManager.faturas.dto.FaturaSicaRQ;
import com.confApi.db.confManager.faturas.dto.FaturaSicaRS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


import java.net.URI;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
@Service
public class FaturasService  {

    private static final Logger LOG = Logger.getLogger(FaturasService.class.getName());

    private final RestTemplate restTemplate;

    @Autowired
    private ConfAppService confAppService;

    // Endpoints baseados no seu UtilApi
    private static final String ENDPOINT = UrlConfig.URL_CONFIANCA_MANAGER;
    private static final String URL_FATURAS          =  "faturaSica/listarFaturas";
    private static final String URL_DETALHAR_FATURA  = ENDPOINT + "faturaSica/detalharFatura";
    private static final String URL_GET_PDF_BOLETO   = ENDPOINT + "faturaSica/getBoleto";
    private static final String URL_GET_PDF_FATURA   = ENDPOINT + "faturaSica/getPdf";
    private static final String URL_GET_CSV_FATURA   = ENDPOINT + "faturaSica/getCsv";



    // Injeção do RestTemplate (recomendado registrar um @Bean em AppConfig)
    public FaturasService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /* -------------------------
       Helpers
    -------------------------- */
    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        SandBoxResp token = confAppService.token();
        headers.setBearerAuth(token.getToken());
        return headers;
    }

    private <T> ResponseEntity<T> postJson(URI uri, Object body, Class<T> responseType) {
        HttpEntity<Object> entity = new HttpEntity<>(body, defaultHeaders());
        return restTemplate.exchange(uri, HttpMethod.POST, entity, responseType);
    }

    private <T> ResponseEntity<T> postJson(URI uri, Object body, ParameterizedTypeReference<T> typeRef) {
        HttpEntity<Object> entity = new HttpEntity<>(body, defaultHeaders());
        return restTemplate.exchange(uri, HttpMethod.POST, entity, typeRef);
    }

    private URI buildUri(String baseUrl) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl).build(true).toUri();
    }

    /* -------------------------
       Listar faturas
    -------------------------- */
    public List<FaturaSicaRS> faturaSica(FaturaSicaRQ faturaSicaRQ) {
        try {
           String  ENDPOINT = UrlConfig.URL_CONFIANCA_MANAGER;
            System.out.println("URL FATURA: "+ENDPOINT+URL_FATURAS);
            URI uri = buildUri(ENDPOINT+URL_FATURAS);
            ResponseEntity<List<FaturaSicaRS>> resp = postJson(
                    uri,
                    faturaSicaRQ,
                    new ParameterizedTypeReference<List<FaturaSicaRS>>() {}
            );

            List<FaturaSicaRS> lista = resp.getBody();
            if (resp.getStatusCode().is2xxSuccessful() && lista != null) {
                if (lista.isEmpty()) {
                    // sua mensagem de “sem faturas”
                    // Util.mensageAlert(EnumIconesMensagem.Ideia.getValor(),"No momento, não há faturas em aberto.");
                }
                return lista;
            }
            LOG.log(Level.WARNING, "faturaSica: status={0} body=null", resp.getStatusCode());
            return Collections.emptyList();

        } catch (HttpStatusCodeException e) {
            LOG.log(Level.SEVERE, "faturaSica HTTP " + e.getStatusCode() + " -> " + e.getResponseBodyAsString(), e);
            return Collections.emptyList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao listar faturas", e);
            return Collections.emptyList();
        }
    }

    /* -------------------------
       Detalhar fatura
    -------------------------- */
    public DetalharFaturaRS detalharFaturaSica(FaturaRQ faturaRQ) {
        try {
            URI uri = buildUri(URL_DETALHAR_FATURA);
            ResponseEntity<DetalharFaturaRS> resp = postJson(uri, faturaRQ, DetalharFaturaRS.class);

            DetalharFaturaRS body = resp.getBody();
            if (resp.getStatusCode().is2xxSuccessful() && body != null) {
                return body;
            }
            LOG.log(Level.WARNING, "detalharFaturaSica: status={0} body=null", resp.getStatusCode());
            return null;

        } catch (HttpStatusCodeException e) {
            LOG.log(Level.SEVERE, "detalharFaturaSica HTTP " + e.getStatusCode() + " -> " + e.getResponseBodyAsString(), e);
            return null;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao detalhar fatura", e);
            return null;
        }
    }

    /* -------------------------
       Downloads (CSV / PDF)
    -------------------------- */
    public byte[] downloadCsvFatura(FaturaRQ body) {
        return postBytes(URL_GET_CSV_FATURA, body);
    }

    public byte[] downloadPdfFatura(FaturaRQ body) {
        return postBytes(URL_GET_PDF_FATURA, body);
    }

    public byte[] downloadPdfFaturaBoleto(FaturaRQ body) {
        return postBytes(URL_GET_PDF_BOLETO, body);
    }

    private byte[] postBytes(String url, Object body) {
        try {
            URI uri = buildUri(url);
            HttpEntity<Object> entity = new HttpEntity<>(body, defaultHeaders());
            ResponseEntity<byte[]> resp = restTemplate.exchange(uri, HttpMethod.POST, entity, byte[].class);

            if (resp.getStatusCode().is2xxSuccessful()) {
                return resp.getBody();
            }

            if (resp.getStatusCode().is5xxServerError()) {
               // message = new FacesMessage(FacesMessage.SEVERITY_WARN, "Alerta", "Documento não disponível.");
               System.out.println("Alerta - Documento não disponível.");

            } else {

                System.out.println("Falha ao baixar o documento. Status: "+ resp.getStatusCode());

            }
            return null;

        } catch (HttpStatusCodeException e) {
            System.out.println("Download HTTP " + e.getStatusCode() + " -> " + e.getResponseBodyAsString());

            if (e.getStatusCode().is5xxServerError()) {
                System.out.println("Alerta - Falha ao baixar o documento.");

            }
            return null;

        } catch (Exception e) {
            System.out.println("Erro ao baixar documento: " + e.getMessage());
            return null;
        }
    }

    /* -------------------------
       Verificação simples de URL (opcional)
       Se preferir, pode trocar por um HEAD do RestTemplate
    -------------------------- */
    public boolean isUrlActive(String urlString) {
        try {
            URI uri = buildUri(urlString);
            HttpEntity<Void> entity = new HttpEntity<>(defaultHeaders());
            ResponseEntity<Void> resp = restTemplate.exchange(uri, HttpMethod.HEAD, entity, Void.class);
            System.out.println("Código de resposta HEAD: " + resp.getStatusCode());

            return true; // se respondeu, consideramos ativa
        } catch (Exception e) {
            System.out.println("Erro ao acessar a URL: " + e.getMessage());
            return false;
        }
    }
}
