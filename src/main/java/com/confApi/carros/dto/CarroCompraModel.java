package com.confApi.carros.dto;

import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.model.FormaPagamentoModel;
import com.confApi.model.RecebimentoModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarroCompraModel {

    private ReservarCarroRequestDTO reservaCarro;
    private FormaPagamentoModel formaPagamentoSelecionada = new FormaPagamentoModel();
    private RecebimentoModel recebimento;
    private Double valorTaxaExtraBrl;
    private Usuario usuario;
    private Agencia agencia;
    private String fonte;

    private Integer qtdPortas;
}
