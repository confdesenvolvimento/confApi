package com.confApi.wooba.sales;

import com.confApi.db.confManager.recebimento.Recebimento;

import java.util.Locale;
import java.util.List;

/** Compara referencias de pagamento, sem usar bilhete, valor ou status como identidade. */
final class WoobaPagamentoIdentidade {
    private WoobaPagamentoIdentidade() {}

    static void exigirIdentidadeForte(Recebimento recebimento) {
        if (!temIdentidadeForte(recebimento)) {
            throw new IllegalStateException("Pagamento Wooba sem identidade forte. Conferencia manual necessaria.");
        }
    }

    static boolean temIdentidadeForte(Recebimento recebimento) {
        String[] identidade = referencias(recebimento);
        return identidade[0] != null || identidade[1] != null || identidade[2] != null;
    }

    static void validarCriacao(List<Recebimento> atuais, Recebimento entrada) {
        String origem = scope(entrada);
        if (origem == null) {
            throw new IllegalStateException("Origem AIR/TKT do pagamento Wooba nao identificada. Conferencia manual necessaria antes de criar.");
        }
        for (Recebimento atual : atuais) {
            if (!temIdentidadeForte(atual)) {
                throw new IllegalStateException("Recebimento legado sem identidade forte na reserva. Conferencia manual necessaria antes de criar outro.");
            }
            String origemAtual = scope(atual);
            if (origemAtual == null || !origem.equals(origemAtual)) {
                throw new IllegalStateException("Recebimentos AIR/TKT misturados ou origem legada desconhecida. Conferencia manual necessaria antes de criar outro.");
            }
        }
    }

    private static String scope(Recebimento recebimento) {
        String origem = campo(recebimento.getMensagem(), "WoobaPaymentScope");
        return "AIR".equals(origem) || "TKT".equals(origem) ? origem : null;
    }

    static boolean mesmoPagamento(Recebimento salvo, Recebimento entrada) {
        String[] esquerda = referencias(salvo);
        String[] direita = referencias(entrada);
        boolean referenciaIgual = false;
        boolean referenciaDivergente = false;
        for (int i = 0; i < esquerda.length; i++) {
            if (esquerda[i] == null || direita[i] == null) continue;
            if (esquerda[i].equals(direita[i])) referenciaIgual = true;
            else referenciaDivergente = true;
        }
        if (referenciaIgual && referenciaDivergente) {
            throw new IllegalStateException("Identidades fortes do pagamento Wooba conflitantes. Conferencia manual necessaria.");
        }
        return referenciaIgual;
    }

    private static String[] referencias(Recebimento recebimento) {
        if (recebimento == null) throw new IllegalStateException("Recebimento Wooba nao informado.");
        return new String[] {
                campo(recebimento.getMensagem(), "WoobaPaymentId"),
                normalizar(recebimento.getCodgTransacao()),
                normalizar(recebimento.getOrderGatewayCartao())
        };
    }

    private static String campo(String mensagem, String nome) {
        String id = null;
        if (mensagem == null) return null;
        for (String parte : mensagem.split(";")) {
            String[] campo = parte.trim().split("=", 2);
            if (campo.length != 2 || !nome.equals(campo[0].trim())) continue;
            String atual = normalizar(campo[1]);
            if (id != null && atual != null && !id.equals(atual)) {
                throw new IllegalStateException("Mais de um valor para " + nome + " no recebimento Wooba. Conferencia manual necessaria.");
            }
            if (atual != null) id = atual;
        }
        return id;
    }

    static String normalizar(String valor) {
        if (valor == null) return null;
        String normalizado = valor.trim().toUpperCase(Locale.ROOT);
        if (normalizado.isEmpty() || "NULL".equals(normalizado) || "UNDEFINED".equals(normalizado)
                || normalizado.matches("0+")
                || "00000000-0000-0000-0000-000000000000".equals(normalizado)) return null;
        return normalizado;
    }
}
