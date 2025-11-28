package com.confApi.endPoints.pagamento;

import com.confApi.endPoints.cartao.CartaoResponse;
import lombok.Data;

@Data
public class PagamentoResponse {
    private Long codgFormaPagamento;
    private Double valorEntrada = 0.0;
    private Double valorPagamento = 0.0;
    private CartaoResponse cartao;
}
