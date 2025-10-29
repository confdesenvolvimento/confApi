package com.confApi.db.wooba.hoteis.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
@Data
public class ReservasHotelExibicao implements Serializable {

    private String unidade;
    private String agencia;
    private String localizador;
    private String identificacaoHotelSistema;
    private String codgUsuario;
    private String codgAgencia;
    private String nomeHotel;
    private Integer sistema;
    private String nomeFornecedor;
    private String prazoPagamentoCliente;
    private Integer numeroDeQuartos;
    private Integer qtdeAdultos;
    private Integer qtdeCriancas;
    private Integer qtdeBebes;
    private Integer quantidadeDiarias;
    private Integer pago;
    private Integer status;
    private String checkIn;
    private String checkOut;
    private Double valorDiaria;
    private Double totalDeDiarias;
    private Double totalDeDiariasNet;
    private Double diariaNet;
    private Double vlrMkpPercentual;
    private Double taxas;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date criadaEm;
    private String limiteParaEmissao;
    private String fonte;
    private String hospede;

}
