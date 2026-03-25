package com.confApi.db.confManager.seguro.reserva;

import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.db.confManager.seguro.cobertura.SeguroCobertura;
import com.confApi.db.confManager.seguro.segurado.SeguroSegurado;
import com.confApi.db.confManager.sistema.Sistema;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.seguros.dto.CoberturaSeguroDTO;
import com.confApi.seguros.dto.SeguradoDTO;
import com.confApi.seguros.dto.SeguroCompraModel;
import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class SeguroReserva {

    private Integer codgReservaSeguro;
    private Usuario codgUsuarioCriacao;
    private String localizador;
    private Sistema codgSistema;
    private Agencia codgAgencia;
    private Integer status;
    private Integer statusPagamentoCliente;
    private Timestamp dataCriacao;
    private Timestamp dataEmissao;
    private Timestamp prazoEmissaoCliente;
    private Timestamp dataCancelamento;
    private Usuario codgUsuarioCancelamento;
    private Double valorTotalReservaNet;
    private Double valorTotalReservaMarkup;
    private Timestamp prazoCancelamento;
    private String observacaoInterna;
    private String observacaoPublica;
    private String descricaoMotivoCancelamento;
    private Date dataInicioCobertura;
    private Date dataFinalCobertura;
    private String contatoNome;
    private String contatoEmail;
    private String contatoTelefone;
    private List<Recebimento> recebimentos = new ArrayList<>();
    private List<SeguroSegurado> seguradosList = new ArrayList<>();
    private List<SeguroCobertura> coberturaList = new ArrayList<>();

    public SeguroReserva() {
    }

    public SeguroReserva(String observacaoInterna) {
        this.observacaoInterna = observacaoInterna;
    }

    public SeguroReserva(SeguroCompraModel seguroCompraModel) {
        this.codgReservaSeguro = null;
        this.codgUsuarioCriacao = new Usuario(seguroCompraModel.getIdentificacaoAgenciaModel().getCodgUsuario());
        this.localizador = null;
        this.codgSistema = seguroCompraModel.getSistema();
        this.codgAgencia = new Agencia(seguroCompraModel.getIdentificacaoAgenciaModel().getCodgAgencia());
        this.status = 1;
        this.statusPagamentoCliente = 0;
        this.dataCriacao = new Timestamp(System.currentTimeMillis());
        this.dataEmissao = null;
        this.prazoEmissaoCliente = null;
        this.dataCancelamento = null;
        this.codgUsuarioCancelamento = null;
        this.valorTotalReservaNet = seguroCompraModel.getPlano().getPrecoBaseBRL();
        this.valorTotalReservaMarkup = seguroCompraModel.getPlano().getPrecoFinalBRL();
        this.prazoCancelamento = null;
        this.observacaoInterna = null;
        this.observacaoPublica = null;
        this.descricaoMotivoCancelamento = null;
        this.dataInicioCobertura = java.sql.Date.valueOf(seguroCompraModel.getPlano().getDataInicioCobertura());
        this.dataFinalCobertura = java.sql.Date.valueOf(seguroCompraModel.getPlano().getDataFinalCombertura());
        this.contatoNome = seguroCompraModel.getContatoEmergencia().getNome();
        this.contatoEmail = seguroCompraModel.getContatoEmergencia().getEmail();
        this.contatoTelefone = seguroCompraModel.getContatoEmergencia().getTelefone();
        List<Recebimento> recebimentoList = new ArrayList<>();
        Recebimento recebimento = new Recebimento(seguroCompraModel.getRecebimento());
        recebimentoList.add(recebimento);
        this.recebimentos = recebimentoList;
        List<SeguroSegurado> seguradosList = new ArrayList<>();
        for(SeguradoDTO seguradoDTO : seguroCompraModel.getSegurados()){
            SeguroSegurado seguroSegurado = new SeguroSegurado(seguradoDTO, seguroCompraModel.getPlano());
            seguradosList.add(seguroSegurado);
        }
        this.seguradosList = seguradosList;
        this.coberturaList.add(new SeguroCobertura(seguroCompraModel.getPlano()));
    }
}
