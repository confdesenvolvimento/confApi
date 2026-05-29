package com.confApi.aereo.dto;

import com.confApi.db.confManager.agencia.dto.Agencia;
import lombok.Data;

import java.util.List;
@Data
public class TarifarRequest {
    private String sistema;
    private Agencia agencia;
    private List<ClasseSelecionada> classes;
    private String identificacaoViagem;
    private String identificacaoDaViagemVolta;
    private List<PassageiroTipoQtd> passageiroTipoQtds;
    private String promocode;
    private List<String> viagensMultiplas = null;
}
