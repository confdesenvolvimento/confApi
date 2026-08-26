package com.confApi.hoteis;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.cacheHotel.cidade.CacheHotelCidade;
import com.confApi.cacheHotel.cidade.CacheHotelCidadeAPI;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.text.Normalizer;

@Service
public class HotelCityService {

    private final RestTemplate restTemplate;
    private final ConfAppService confAppService;
    private final CacheHotelCidadeAPI cacheHotelCidadeAPI;

    public HotelCityService(
            @Qualifier("restTemplate") RestTemplate restTemplate,
            ConfAppService confAppService,
            CacheHotelCidadeAPI cacheHotelCidadeAPI) {
        this.restTemplate = restTemplate;
        this.confAppService = confAppService;
        this.cacheHotelCidadeAPI = cacheHotelCidadeAPI;
    }

    public List<CidadeHotelQuantidadeDTO> buscarCidadesComQuantidadeHoteis(String query) {
        String termo = query == null ? "" : query.trim();
        ConfAppResp token = confAppService.token();
        String url = UriComponentsBuilder
                .fromHttpUrl(UrlConfig.URL_CONFIANCA_MANAGER)
                .path("hotel/buscarCidadesComQuantidadeHoteis")
                .queryParam("nome", termo)
                .queryParam("query", termo)
                .toUriString();

        ResponseEntity<List<CidadeHotelQuantidadeDTO>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(defaultHeaders(token.getToken())),
                new ParameterizedTypeReference<List<CidadeHotelQuantidadeDTO>>() {
                }
        );

        List<CidadeHotelQuantidadeDTO> quantidades = response.getBody() == null
                ? Collections.emptyList()
                : response.getBody();

        Map<String, Long> quantidadePorCodigo = new HashMap<>();
        for (CidadeHotelQuantidadeDTO cidade : quantidades) {
            String codigo = cidade.getCodeCidade();
            if (codigo != null) {
                quantidadePorCodigo.put(codigo, cidade.getQuantidadeHoteis() == null ? 0L : cidade.getQuantidadeHoteis());
            }
        }

        // A lista do CacheHotel preserva os nomes e os campos que a tela já apresentava.
        List<CacheHotelCidade> cidadesAnteriores = cacheHotelCidadeAPI.searchCidades(termo);
        return cidadesAnteriores.stream()
                .filter(cidade -> normalizar(cidade.getNomeCidade()).startsWith(normalizar(termo)))
                .map(cidade -> converter(cidade, quantidadePorCodigo))
                .collect(Collectors.toList());
    }

    private String normalizar(String valor) {
        return Normalizer.normalize(valor == null ? "" : valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }

    private CidadeHotelQuantidadeDTO converter(CacheHotelCidade cidade, Map<String, Long> quantidadePorCodigo) {
        CidadeHotelQuantidadeDTO dto = new CidadeHotelQuantidadeDTO();
        dto.setId(cidade.getId());
        dto.setCodeCidade(cidade.getCodeCidade());
        dto.setNomeCidade(cidade.getNomeCidade());
        dto.setNomeEstado(cidade.getNomeEstado());
        dto.setNomePais(cidade.getNomePais());
        dto.setCodePais(parseInteger(cidade.getCodePais(), null));
        dto.setCodeEstado(parseInteger(cidade.getCodeEstado(), null));
        dto.setZonaCode(cidade.getZonaCode());
        dto.setCodgEz(cidade.getCodgEz());
        dto.setQuantidadeHoteis(quantidadePorCodigo.getOrDefault(
                String.valueOf(cidade.getId()),
                quantidadePorCodigo.getOrDefault(cidade.getCodeCidade(), 0L)));
        return dto;
    }

    private Integer parseInteger(String value, Integer fallback) {
        try {
            return value == null || value.trim().isEmpty() ? fallback : Integer.valueOf(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private HttpHeaders defaultHeaders(String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(bearerToken);
        return headers;
    }
}
