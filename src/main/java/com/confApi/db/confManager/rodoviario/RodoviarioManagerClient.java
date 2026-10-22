package com.confApi.db.confManager.rodoviario;

import com.confApi.db.confManager.rodoviario.dto.*;
import com.confApi.rodoviario.RodoviarioHttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
public class RodoviarioManagerClient {
    private final RodoviarioHttpClient http;

    public enum Recurso {
        reservas("reservaRodoviario"), trechos("rodoviarioTrecho"), passageiros("rodoviarioPassageiro"),
        assentos("rodoviarioAssento"), bilhetes("rodoviarioBilhete"), valores("rodoviarioValor");
        public final String rota;
        Recurso(String rota) { this.rota = rota; }
    }

    public <T> T trocar(String rota, HttpMethod metodo, Object corpo, MultiValueMap<String, String> parametros,
                        String usuario, Class<T> tipo, boolean individual) {
        if (individual && (usuario == null || usuario.isBlank()))
            throw new ResponseStatusException(UNAUTHORIZED, "X-Usuario-Token individual obrigatorio.");
        return http.trocar(RodoviarioHttpClient.Servico.MANAGER, rota, metodo, corpo, parametros, usuario, tipo, metodo != HttpMethod.GET);
    }

    public ReservaRodoviarioDetalheDTO salvarCompleta(SalvarReservaRodoviarioRequest request, String usuario) {
        return trocar("reservaRodoviario/salvarCompleta", HttpMethod.POST, request, null, usuario, ReservaRodoviarioDetalheDTO.class, true);
    }

    public ReservaRodoviarioDetalheDTO detalhe(Integer id, String usuario) {
        return trocar("reservaRodoviario/detalhe/" + id, HttpMethod.GET, null, null, usuario, ReservaRodoviarioDetalheDTO.class, true);
    }

    public JsonNode crud(Recurso recurso, Integer id, HttpMethod metodo, Object corpo, MultiValueMap<String, String> filtros, String usuario) {
        return trocar(recurso.rota + (id == null ? "" : "/" + id), metodo, corpo, filtros, usuario, JsonNode.class, true);
    }
}