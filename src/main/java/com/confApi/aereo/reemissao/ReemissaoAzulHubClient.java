package com.confApi.aereo.reemissao;

import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.*;

@Component
public class ReemissaoAzulHubClient {
    private final ConfAppService auth;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
    public ReemissaoAzulHubClient(ConfAppService auth, ObjectMapper mapper) { this.auth = auth; this.mapper = mapper; }
    public JsonNode executar(String acao, JsonNode pedido) {
        try {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(UrlConfig.URL_CONFIANCA_HUB.replaceAll("/+$", "") + "/api/aereo/reemissao-azul/" + acao))
                .timeout(Duration.ofSeconds(300)).header("Content-Type", "application/json").header("Authorization", "Bearer " + auth.token().getToken())
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(pedido)));
        if ("concluir".equals(acao)) {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes s) {
                HttpServletRequest origem = s.getRequest();
                for (String key : List.of("X-Usuario-Token", "X-Antifraude-Operacao", "X-Antifraude-IP", "X-Antifraude-Contexto", "X-Antifraude-Sessao")) {
                    String value = origem.getHeader(key); if (value != null && !value.isBlank()) request.header(key, value);
                }
            }
        }
        // Transporte sem log de corpos/cartao e sem repeticao de mutacoes.
        HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("O Hub nao confirmou a operacao.");
        return mapper.readTree(response.body());
        } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException("Espera pela operacao interrompida."); }
        catch (java.io.IOException ex) { throw new IllegalStateException("Nao foi possivel confirmar a resposta do Hub."); }
    }
}
