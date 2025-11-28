package com.confApi.endPoints.reservaPacote;

import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.db.confManager.reservaPacote.ReservaPacote;
import com.confApi.endPoints.agencia.Agencia;
import com.confApi.endPoints.recebimento.RecebimentoResponse;
import com.confApi.endPoints.usuario.UsuarioResponse;
import lombok.Data;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Data
public class ReservaPacoteResponse {
    private Integer codgPacote;
    private String idenPacote;
    private Timestamp dataCriacao;
    private Timestamp dataLimiteEmissao;
    private Timestamp dataEmissao;
    private Timestamp dataCancelamento;
    private Integer status;
    private Agencia codgAgencia;
    private String descricaoMotivoCancelamento;
    private UsuarioResponse codgUsuarioCriacao;
    private UsuarioResponse codgUsuarioCancelamento;
    private Integer statusPagamento;
    private Timestamp prazoPagamentoCliente;
    private List<RecebimentoResponse> recebimentosDB;

    public ReservaPacoteResponse(ReservaPacote reservaPacote) {
        this.codgPacote = reservaPacote.getCodgPacote();
        this.idenPacote = reservaPacote.getIdenPacote();
        this.dataCriacao = reservaPacote.getDataCriacao();
        this.dataLimiteEmissao = reservaPacote.getDataLimiteEmissao();
        this.dataEmissao = reservaPacote.getDataEmissao();
        this.dataCancelamento = reservaPacote.getDataCancelamento();
        this.status = reservaPacote.getStatus();
        this.codgAgencia = new Agencia(reservaPacote.getCodgAgencia());
        this.descricaoMotivoCancelamento = reservaPacote.getDescricaoMotivoCancelamento();
        this.codgUsuarioCriacao = new UsuarioResponse(reservaPacote.getCodgUsuarioCriacao());
        this.codgUsuarioCancelamento = new UsuarioResponse(reservaPacote.getCodgUsuarioCancelamento());
        this.statusPagamento = reservaPacote.getStatusPagamento();
        this.prazoPagamentoCliente = reservaPacote.getPrazoPagamentoCliente();
        this.recebimentosDB = new ArrayList<>();
        for(Recebimento recebimento : reservaPacote.getRecebimentosDB()){
            this.recebimentosDB.add(new RecebimentoResponse(recebimento));
        }

    }
}
