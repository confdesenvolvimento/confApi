package com.confApi.hoteis;

import com.confApi.hub.hotel.dto.ReservarRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import java.io.IOException;
import java.util.Date;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

final class ReservaOmnibeesFalha extends RuntimeException {

    private static final ObjectReader JSON = new ObjectMapper().reader().with(
            DeserializationFeature.FAIL_ON_TRAILING_TOKENS, DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    private static final int MAXIMO_BYTES = 4096;
    private static final Pattern LOCALIZADOR = Pattern.compile("[A-Za-z0-9_-]{3,85}");
    private static final Pattern CODIGO_FORNECEDOR = Pattern.compile("[A-Za-z0-9_-]{1,24}");

    private final Resposta resposta;

    private ReservaOmnibeesFalha(Resultado resultado, String localizador, String codigoFornecedor) {
        this(new Resposta(resultado.name(), resultado.etapa, resultado.mensagem, localizador, codigoFornecedor,
                null, null, null), null);
    }

    private ReservaOmnibeesFalha(Resposta resposta, Throwable causa) {
        super(resposta.mensagem(), causa);
        this.resposta = resposta;
    }

    static ReservaOmnibeesFalha gravacaoNaoConfirmada(String localizador, Date entrada, Date saida,
                                                         String observacao, RuntimeException causa) {
        String referencia = localizador != null && LOCALIZADOR.matcher(localizador).matches() ? localizador : null;
        return new ReservaOmnibeesFalha(new Resposta("OMNIBEES_MANAGER_NAO_CONFIRMADO", "MANAGER",
                "A Omnibees confirmou a reserva, mas a gravação no Manager ficou pendente. Confira antes de reservar novamente.",
                referencia, null, observacao, epochMillis(entrada), epochMillis(saida)), causa);
    }

    private static Long epochMillis(Date data) {
        return data == null ? null : data.getTime();
    }

    Resposta resposta() {
        return resposta;
    }

    static ReservaOmnibeesFalha interpretar(ReservarRequest pedido, HttpStatusCodeException erro) {
        if (erro.getRawStatusCode() != HttpStatus.BAD_GATEWAY.value() || !exclusivamenteOmnibees(pedido)) return null;
        byte[] corpo = erro.getResponseBodyAsByteArray();
        if (corpo.length == 0 || corpo.length > MAXIMO_BYTES) return null;
        try {
            JsonNode dados = JSON.readTree(corpo);
            if (dados == null || !dados.isObject()) return null;
            Resultado resultado = Resultado.reconhecer(dados.path("codigo").asText(), dados.path("etapa").asText());
            if (resultado == null) return null;

            String localizador = null;
            if (resultado == Resultado.OMNIBEES_DETALHES_INDISPONIVEIS) {
                localizador = textoValido(dados.path("localizador"), LOCALIZADOR);
                if (localizador == null) return null;
            } else if (resultado.descartaCriacao && dados.hasNonNull("localizador")) {
                return null;
            }
            return new ReservaOmnibeesFalha(resultado, localizador, textoValido(dados.path("codigoFornecedor"), CODIGO_FORNECEDOR));
        } catch (IOException e) {
            return null;
        }
    }

    private static boolean exclusivamenteOmnibees(ReservarRequest pedido) {
        return pedido != null && pedido.getAcomodacoes() != null && !pedido.getAcomodacoes().isEmpty()
                && pedido.getAcomodacoes().stream().allMatch(a -> a != null && "Omnibees".equalsIgnoreCase(a.getSistema()));
    }

    private static String textoValido(JsonNode valor, Pattern padrao) {
        return valor.isTextual() && padrao.matcher(valor.textValue()).matches() ? valor.textValue() : null;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Resposta(String codigo, String etapa, String mensagem, String localizador, String codigoFornecedor,
                    String observacaoOmnibees, Long dataEntrada, Long dataSaida) { }

    private enum Resultado {
        OMNIBEES_NAO_ENVIADA("INITIATE", "O envio da confirmação da reserva à Omnibees não foi realizado.", true),
        OMNIBEES_COMMIT_RECUSADO("COMMIT", "A Omnibees recusou a confirmação da reserva.", true),
        OMNIBEES_COMMIT_INCERTO("COMMIT", "A Omnibees não confirmou o resultado da reserva. Confira antes de tentar novamente.", false),
        OMNIBEES_DETALHES_INDISPONIVEIS("CONSULTA", "A Omnibees confirmou o localizador. Os detalhes da reserva precisam de conferência.", false);

        private final String etapa;
        private final String mensagem;
        private final boolean descartaCriacao;

        Resultado(String etapa, String mensagem, boolean descartaCriacao) {
            this.etapa = etapa;
            this.mensagem = mensagem;
            this.descartaCriacao = descartaCriacao;
        }

        private static Resultado reconhecer(String codigo, String etapa) {
            for (Resultado resultado : values()) {
                if (resultado.name().equals(codigo) && resultado.etapa.equals(etapa)) return resultado;
            }
            return null;
        }
    }
}
