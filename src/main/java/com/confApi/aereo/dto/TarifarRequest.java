package com.confApi.aereo.dto;

import com.confApi.db.confManager.agencia.dto.Agencia;
import lombok.Data;

import java.util.List;
@Data
public class TarifarRequest {
    private String tipoTarifa = "CONS";
    private com.fasterxml.jackson.databind.JsonNode criteriosOperadora;
    private String chaveSelecao;
    private Integer codgPacote;
    private Long codgReservaHotel;
    private java.math.BigDecimal precoEsperadoOperadora;

    private String sistema;
    private Agencia agencia;
    private List<ClasseSelecionada> classes;
    private String identificacaoViagem;
    private String identificacaoDaViagemVolta;
    private List<PassageiroTipoQtd> passageiroTipoQtds;
    private String promocode;
    private List<String> viagensMultiplas = null;
}
