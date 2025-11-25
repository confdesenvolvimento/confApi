package com.confApi.db.confManager.reservaPacote;

import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.db.confManager.usuario.Usuario;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReservaPacote implements Serializable {

    private Integer codgPacote;
    private String idenPacote;
    private Timestamp dataCriacao;
    private Timestamp dataLimiteEmissao;
    private Timestamp dataEmissao;
    private Timestamp dataCancelamento;
    private Integer status;
    private Agencia codgAgencia;
    private String descricaoMotivoCancelamento;
    private Usuario codgUsuarioCriacao;
    private Usuario codgUsuarioCancelamento;
    private Integer statusPagamento;
    private Timestamp prazoPagamentoCliente;

    private List<Recebimento> recebimentosDB;


    public ReservaPacote() {
    }

    public ReservaPacote(Integer codgPacote, String idenPacote, Timestamp dataCriacao, Timestamp dataLimiteEmissao, Timestamp dataEmissao, Timestamp dataCancelamento, Integer status, Agencia codgAgencia, String descricaoMotivoCancelamento, Usuario codgUsuarioCriacao, Usuario codgUsuarioCancelamento, Integer statusPagamento, Timestamp prazoPagamentoCliente) {
        this.codgPacote = codgPacote;
        this.idenPacote = idenPacote;
        this.dataCriacao = dataCriacao;
        this.dataLimiteEmissao = dataLimiteEmissao;
        this.dataEmissao = dataEmissao;
        this.dataCancelamento = dataCancelamento;
        this.status = status;
        this.codgAgencia = codgAgencia;
        this.descricaoMotivoCancelamento = descricaoMotivoCancelamento;
        this.codgUsuarioCriacao = codgUsuarioCriacao;
        this.codgUsuarioCancelamento = codgUsuarioCancelamento;
        this.statusPagamento = statusPagamento;
        this.prazoPagamentoCliente = prazoPagamentoCliente;
    }

    public ReservaPacote(Integer codgPacote, String idenPacote, Timestamp dataCriacao,
                         Timestamp dataLimiteEmissao, Timestamp dataEmissao, Timestamp dataCancelamento,
                         Integer status, Agencia codgAgencia, String descricaoMotivoCancelamento,
                         Usuario codgUsuarioCriacao, Usuario codgUsuarioCancelamento, Integer statusPagamento,
                         Timestamp prazoPagamentoCliente, List<Recebimento> recebimentosDB) {
        this.codgPacote = codgPacote;
        this.idenPacote = idenPacote;
        this.dataCriacao = dataCriacao;
        this.dataLimiteEmissao = dataLimiteEmissao;
        this.dataEmissao = dataEmissao;
        this.dataCancelamento = dataCancelamento;
        this.status = status;
        this.codgAgencia = codgAgencia;
        this.descricaoMotivoCancelamento = descricaoMotivoCancelamento;
        this.codgUsuarioCriacao = codgUsuarioCriacao;
        this.codgUsuarioCancelamento = codgUsuarioCancelamento;
        this.statusPagamento = statusPagamento;
        this.prazoPagamentoCliente = prazoPagamentoCliente;
        this.recebimentosDB = recebimentosDB;
    }


    public ReservaPacote(Integer codgReservaPacote) {
        this.codgPacote = codgReservaPacote;
    }
}

