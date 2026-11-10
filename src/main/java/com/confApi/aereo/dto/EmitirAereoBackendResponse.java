package com.confApi.aereo.dto;

import com.confApi.db.confManager.recebimento.Recebimento;
import lombok.Data;

@Data
public class EmitirAereoBackendResponse {
    private boolean sucesso;
    private boolean linkPagamento;
    private String mensagem;
    private EmitirResponse emitirResponse;
    private Recebimento recebimento;

    public static EmitirAereoBackendResponse sucesso(EmitirResponse response) {
        EmitirAereoBackendResponse dto = new EmitirAereoBackendResponse();
        dto.sucesso = true;
        dto.emitirResponse = response;
        dto.mensagem = "Reserva emitida com sucesso.";
        return dto;
    }

    public static EmitirAereoBackendResponse erro(String mensagem) {
        EmitirAereoBackendResponse dto = new EmitirAereoBackendResponse();
        dto.sucesso = false;
        dto.mensagem = mensagem;
        return dto;
    }

    public static EmitirAereoBackendResponse linkPagamento(Recebimento recebimento) {
        EmitirAereoBackendResponse dto = new EmitirAereoBackendResponse();
        dto.sucesso = true;
        dto.linkPagamento = true;
        dto.recebimento = recebimento;
        dto.mensagem = "Link de pagamento gerado com sucesso.";
        return dto;
    }
}
