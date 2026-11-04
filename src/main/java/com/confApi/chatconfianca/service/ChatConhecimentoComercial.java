package com.confApi.chatconfianca.service;

import com.confApi.chatconfianca.intencao.ChatIntencaoRuntimeDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;

/** Conteudo editorial publicado: nao e autorizacao de credito nem comando de execucao. */
public final class ChatConhecimentoComercial {
    private ChatConhecimentoComercial() { }

    public static String vigenteCadastro(String mensagem, String base,
            List<com.confApi.db.confManager.chatMemoria.dto.ChatMemoria> registros,
            LocalDate hoje, ObjectMapper mapper) {
        if (registros == null || base == null || base.isBlank()) return null;
        var memorias = new java.util.ArrayList<ChatIntencaoRuntimeDto.Memoria>();
        for (var registro : registros) {
            if (registro == null || !Integer.valueOf(1).equals(registro.getStatus())) continue;
            String escopo = ChatPedidoOperacional.normalizar(registro.getBase());
            if (!"geral".equals(escopo) && !escopo.equals(ChatPedidoOperacional.normalizar(base))) continue;
            var memoria = new ChatIntencaoRuntimeDto.Memoria();
            memoria.setCodgMemoria(registro.getCodgMemoria()); memoria.setBase(registro.getBase());
            memoria.setTexto(registro.getText()); memorias.add(memoria);
        }
        return vigente(mensagem, base, memorias, hoje, mapper);
    }

    public static String vigente(String mensagem, String base, List<ChatIntencaoRuntimeDto.Memoria> memorias,
            LocalDate hoje, ObjectMapper mapper) {
        if (memorias == null || hoje == null) return null;
        String pedido = ChatPedidoOperacional.normalizar(mensagem);
        for (var memoria : memorias) {
            if (memoria == null || memoria.getTexto() == null || memoria.getTexto().length() > 16000) continue;
            try {
                JsonNode j = mapper.readTree(memoria.getTexto());
                if (!"chat.condicao-comercial.v1".equals(j.path("schema").asText())) continue;
                LocalDate inicio = LocalDate.parse(j.path("vigenteDe").asText());
                LocalDate fim = LocalDate.parse(j.path("vigenteAte").asText());
                if (fim.isBefore(inicio) || hoje.isBefore(inicio) || hoje.isAfter(fim)) continue;
                String companhia = ChatPedidoOperacional.normalizar(j.path("companhia").asText());
                String escopo = ChatPedidoOperacional.normalizar(j.path("base").asText());
                if (companhia.isBlank() || !java.util.regex.Pattern.compile("\\b" + java.util.regex.Pattern.quote(companhia) + "\\b").matcher(pedido).find()) continue;
                if (!"geral".equals(escopo) && !escopo.equals(ChatPedidoOperacional.normalizar(base))) continue;
                String texto = j.path("texto").asText();
                String fonte = j.path("fontePublica").asText();
                if (texto.isBlank() || texto.length() > 5000 || fonte.isBlank() || fonte.length() > 300) continue;
                return texto + "\n\nFonte: " + fonte + ". Vigência: " + inicio + " a " + fim
                        + ". A condição cadastrada não confirma autorização de crédito nem executa pagamento ou emissão.";
            } catch (Exception ignored) { /* Conteudo incompleto/legado nao e condicao vigente verificavel. */ }
        }
        return null;
    }
}
