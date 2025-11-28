package com.confApi.endPoints.recebimento;

import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.endPoints.cartao.CartaoResponse;
import com.confApi.endPoints.formaPagamento.FormaPagamentoResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecebimentoResponse {
    private Integer codgFormaPagamento;
    private String nomeFormaPagamento;
    private Double valorEntrada = 0.0;
    private Double valorPagamento = 0.0;
    private FormaPagamentoResponse formaDePagamento;
    private CartaoResponse cartaoSelecionado;
    private Integer statusRecebimento;
    private Date dataRecebimento;
    private Integer codgCodgRecebimento;
    private String assinatura;
    private String link;

    public RecebimentoResponse(Recebimento recebimento) {
        this.codgFormaPagamento = recebimento.getCodgFormaPagto().getCodgFormaPagto();
        this.nomeFormaPagamento = recebimento.getCodgFormaPagto().getNomeFormaPagto();
        this.valorEntrada = recebimento.getValrEntrada();
        this.valorPagamento = recebimento.getValrRecebimento();
        this.formaDePagamento = new FormaPagamentoResponse(recebimento.getCodgFormaPagto());
        this.cartaoSelecionado = new CartaoResponse(recebimento);
        this.statusRecebimento = recebimento.getStatus();
        this.dataRecebimento = recebimento.getDataRecebimento();
        this.codgCodgRecebimento = recebimento.getCodgRecebimento();
        this.assinatura = recebimento.getAssinaturaEletronica();
        this.link = recebimento.getLink();
    }
}
