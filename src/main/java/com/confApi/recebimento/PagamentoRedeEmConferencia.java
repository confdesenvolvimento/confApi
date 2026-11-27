package com.confApi.recebimento;

import com.confApi.db.confManager.recebimento.Recebimento;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.HttpStatusCodeException;

import java.io.IOException;
import java.util.regex.Pattern;

public final class PagamentoRedeEmConferencia extends RuntimeException {
    public static final String CODIGO = "PAGAMENTO_REDE_EM_CONFERENCIA";
    public static final String MENSAGEM =
            "O pagamento está em conferência. Consulte a tentativa original antes de fazer uma nova cobrança.";
    private static final Pattern TID = Pattern.compile("[A-Za-z0-9_-]{1,80}");
    private static final ObjectMapper JSON = new ObjectMapper();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Resposta(String codigo, String mensagem, Integer codgRecebimento, String tid) { }

    private final Resposta resposta;

    private PagamentoRedeEmConferencia(Integer codgRecebimento, String tid) {
        super(MENSAGEM);
        resposta = new Resposta(CODIGO, MENSAGEM, codgRecebimento != null && codgRecebimento > 0 ? codgRecebimento : null,
                tid != null && TID.matcher(tid).matches() ? tid : null);
    }

    public Resposta resposta() {
        return resposta;
    }

    public static PagamentoRedeEmConferencia doRecebimento(Recebimento recebimento) {
        return new PagamentoRedeEmConferencia(recebimento == null ? null : recebimento.getCodgRecebimento(),
                recebimento == null ? null : recebimento.getCodgTransacao());
    }

    static PagamentoRedeEmConferencia doConflito(HttpStatusCodeException erro) {
        byte[] bytes = erro.getResponseBodyAsByteArray();
        if (bytes.length > 8192) return doRecebimento(null);
        try {
            var corpo = JSON.readTree(bytes);
            if (corpo != null && CODIGO.equals(corpo.path("codigo").asText())) {
                var id = corpo.path("codgRecebimento");
                return new PagamentoRedeEmConferencia(id.isIntegralNumber() && id.canConvertToInt() ? id.intValue() : null,
                        corpo.path("tid").textValue());
            }
        } catch (IOException | RuntimeException ignorado) {
            // A resposta do Manager pode conter dados sensíveis.
        }
        return doRecebimento(null);
    }
}
