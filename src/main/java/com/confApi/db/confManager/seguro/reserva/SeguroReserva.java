package com.confApi.db.confManager.seguro.reserva;

import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.db.confManager.sistema.Sistema;
import com.confApi.db.confManager.usuario.Usuario;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class SeguroReserva {

    private Integer codgReservaSeguro;
    private Usuario usuario;
    private String localizador;
    private Sistema sistema;
    private Agencia agencia;
    private Integer status;
    private Integer statusPagamentoCliente;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataEmissao;
    private LocalDateTime prazoEmissaoCliente;
    private LocalDateTime dataCancelamento;
    private Usuario usuarioCancelamento;
    private Double valorTotalReservaNet;
    private Double valorTotalReservaMarkup;
    private LocalDateTime prazoCancelamento;
    private String observacaoInterna;
    private String observacaoPublica;
    private String descricaoMotivoCancelamento;
    private Date dataInicioCobertura;
    private Date dataFinalCobertura;
}
