package com.confApi.wooba.sales.dto;

public record WoobaManualImportResponse(boolean sucesso, String mensagem, String acao,
        Integer codgReservaAereo, String localizador, String companhia, Integer statusReserva,
        String descricaoStatus, int passageiros, int bilhetes, int pagamentos) {
    public static WoobaManualImportResponse erro(String mensagem) {
        return new WoobaManualImportResponse(false, mensagem, null, null, null, null, null, null, 0, 0, 0);
    }

    public static WoobaManualImportResponse usuarioPendente(String mensagem) {
        return new WoobaManualImportResponse(false, mensagem, "SELECIONAR_USUARIO", null, null, null, null, null, 0, 0, 0);
    }
}
