package com.confApi.carros.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarroReservaOperacaoResponseDTO {

    private Boolean success;
    private String mensagem;
    private Object returnMessage;

    private CarroReservaDetalheDTO reserva;

    private CarroFornecedorDadosDTO fornecedor;

    private Boolean mergeExecutado;
    private Boolean statusAtualizado;

    public static CarroReservaOperacaoResponseDTO sucesso(
            CarroReservaDetalheDTO reserva
    ) {
        CarroReservaOperacaoResponseDTO response = new CarroReservaOperacaoResponseDTO();

        response.setSuccess(true);
        response.setMensagem("Reserva de carro consultada com sucesso.");
        response.setReturnMessage("Reserva de carro consultada com sucesso.");
        response.setReserva(reserva);
        response.setMergeExecutado(false);
        response.setStatusAtualizado(false);

        return response;
    }

    public static CarroReservaOperacaoResponseDTO sucesso(
            CarroReservaDetalheDTO reserva,
            CarroFornecedorDadosDTO fornecedor,
            Boolean mergeExecutado,
            Boolean statusAtualizado
    ) {
        CarroReservaOperacaoResponseDTO response = sucesso(reserva);

        response.setFornecedor(fornecedor);
        response.setMergeExecutado(Boolean.TRUE.equals(mergeExecutado));
        response.setStatusAtualizado(Boolean.TRUE.equals(statusAtualizado));

        return response;
    }

    public static CarroReservaOperacaoResponseDTO erro(String mensagem) {
        CarroReservaOperacaoResponseDTO response = new CarroReservaOperacaoResponseDTO();

        response.setSuccess(false);
        response.setMensagem(mensagem);
        response.setReturnMessage(mensagem);
        response.setReserva(null);
        response.setFornecedor(null);
        response.setMergeExecutado(false);
        response.setStatusAtualizado(false);

        return response;
    }
}
