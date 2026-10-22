package com.confApi.hub.rodoviario;

import com.confApi.rodoviario.RodoviarioHttpClient;
import com.confApi.rodoviario.RodoviarioIntegracaoException;
import com.confApi.rodoviario.dto.PesquisaRodoviarioRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import static com.confApi.rodoviario.RodoviarioHttpClient.Servico.HUB;

@Service
@RequiredArgsConstructor
public class RodoviarioHubClient {
    private final RodoviarioHttpClient http;
    public enum Operacao {
        pesquisa(false), pesquisaconexoes(false), sugestoesconexao(false), mapadeassentos(false),
        reservar(true), consultarreserva(false), cancelarreserva(true), emitir(true),
        listarcompras(false), consultar(false), cancelarcompra(true), cancelarbilhete(true), atualizarreferencia(true);
        private final boolean efeito;
        Operacao(boolean efeito) { this.efeito = efeito; }
    }
    public JsonNode pesquisar(PesquisaRodoviarioRequestDTO request) {
        return enviar(Operacao.pesquisa, request);
    }
    public JsonNode pesquisarConexoes(PesquisaRodoviarioRequestDTO request) {
        return enviar(Operacao.pesquisaconexoes, request);
    }
    public JsonNode sugerirConexoes(PesquisaRodoviarioRequestDTO request) {
        return enviar(Operacao.sugestoesconexao, request);
    }
    public JsonNode post(Operacao operacao, JsonNode request) {
        if (request == null || !request.isObject())
            throw RodoviarioIntegracaoException.falha(400, "HUB", "REQUEST_INVALIDO", "Informe um objeto JSON.", false);
        return enviar(operacao, request);
    }
    private JsonNode enviar(Operacao operacao, Object request) {
        if (request == null)
            throw RodoviarioIntegracaoException.falha(400, "HUB", "REQUEST_INVALIDO", "Informe os dados da requisicao.", false);
        return validar(http.trocar(HUB, "api/rodoviario/" + operacao.name(), HttpMethod.POST,
                request, null, null, JsonNode.class, operacao.efeito), operacao.efeito);
    }
    public JsonNode catalogo(String recurso, String codigo, String sistema) {
        if (!java.util.Set.of("viacoes", "localidades", "chavepagamento").contains(recurso)
                || ("chavepagamento".equals(recurso) && codigo != null)
                || (codigo != null && !("viacoes".equals(recurso) ? codigo.matches("[1-9][0-9]*")
                        : codigo.matches("(?:CIT|ROD)_[1-9][0-9]*"))))
            throw RodoviarioIntegracaoException.falha(400, "HUB", "REQUEST_INVALIDO", "Recurso ou codigo invalido.", false);
        var query = new LinkedMultiValueMap<String, String>();
        if (sistema != null && !sistema.isBlank()) query.add("sistema", sistema);
        String rota = "api/rodoviario/" + recurso + (codigo == null ? "" : "/" + codigo);
        return validar(http.trocar(HUB, rota, HttpMethod.GET, null, query, null, JsonNode.class, false), false);
    }
    private JsonNode validar(JsonNode resposta, boolean efeito) {
        if (!resposta.isObject() || !resposta.path("sistema").isTextual() || !resposta.hasNonNull("dados"))
            throw RodoviarioIntegracaoException.falha(502, "HUB", "RESPOSTA_INVALIDA",
                    "Envelope rodoviario invalido. Consulte antes de repetir uma operacao.", efeito);
        return resposta;
    }
}
