package com.confApi.aereo.dto;

import com.confApi.hub.aereo.dto.Contato;
import com.confApi.hub.aereo.dto.Passageiro;
import com.confApi.model.IdentificacaoAgenciaModel;
import lombok.Data;

import java.util.List;
@Data
public class ReservarRequest {
    private IdentificacaoAgenciaModel identificacaoAgenciaModel;
    private Agencia agencia;
    private List<ClasseSelecionada> classesSelecionadas;
    private List<Contato> contatos;
    private String identificacaoDaViagem;
    private String identificacaoDaViagemVolta;
    private List<String> identificacaoViagemMultipla;
    private List<Passageiro> passageiros;
    private String sistema;
    private String promocode ="";

}
