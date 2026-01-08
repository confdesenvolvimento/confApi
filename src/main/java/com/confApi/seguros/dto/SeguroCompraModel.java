package com.confApi.seguros.dto;

import com.confApi.model.FormaPagamentoModel;
import com.confApi.model.IdentificacaoAgenciaModel;
import com.confApi.model.RecebimentoModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
@Data
public class SeguroCompraModel {
    private IdentificacaoAgenciaModel identificacaoAgenciaModel ;
    private ContatoEmergenciaDTO contatoEmergencia ;
    private List<SeguradoDTO> segurados = new ArrayList<>();
    private PlanoSeguroDTO plano;
    private FormaPagamentoModel formaPagamentoSelecionada;
    private RecebimentoModel recebimento;

    public SeguroCompraModel() {
    }
}
