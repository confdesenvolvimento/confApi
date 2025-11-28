package com.confApi.hub.aereo;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class VooHub {
    private Integer id;
    private AeroportoHub destino;
    private AeroportoHub origem;
    private CompanhiaHub ciaMandatoria;
    private CompanhiaHub ciaOperadora;
    private Boolean bagagemInclusa = false;
    private Integer bagagemIndicador = null;
    private Double bagagemPeso = 0.0;
    private Integer bagagemQuantidade = 0;
    private String bagagemUnidadeDeMedida;
    private List<BagagemHub> bagagens = new ArrayList<>();
    private Date dataPartida;
    private String horaPartida;
    private Date dataChegada;
    private String horaChegada;
    private String duracao;
    private String equipamento;
    private Integer qtdEscalas = 0;
    private String classe;
    private String numeroVoo;
    private String cabine;
    private Integer tipoSegmento = 0; // I= Ida, V=Volta
    private Boolean isConexao = false;
    private Boolean isReembolsavel = false;
    private List<AssentoHub> assentos;
    private Boolean conexao = false;
    private String familia;
    private String familiaCodigo;
    private String localizadorCia;
    private String status;
    private Boolean surface;
    private String identificacaoDoVoo;
}
