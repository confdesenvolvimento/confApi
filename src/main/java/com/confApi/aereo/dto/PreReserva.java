package com.confApi.aereo.dto;

import com.confApi.db.confManager.usuario.dto.UsuarioDto;
import com.confApi.hub.aereo.PassageiroModel;
import com.confApi.hub.aereo.ReservaValoresAereo;
import com.confApi.hub.aereo.dto.Contato;
import lombok.Data;

import java.util.List;

@Data
public class PreReserva {
    private List<Trecho> trechos;
    private List<PassageiroModel> passageiros;

    private Integer qtdAdt = 0;
    private Integer qtdChd = 0;
    private Integer qtdInf = 0;
    private Integer tipoTrecho = null;
    private Integer tipoVooPesquisa = null;
    private List<String> identificacaoViagemMultipla;

    private List<ReservaValoresAereo> valoresReservaAdt;
    private List<ReservaValoresAereo> valoresReservaChd;
    private List<ReservaValoresAereo> valoresReservaInf;
    private List<Contato> contatos;
    private Double valorTotalTaxaEmbarque = 0.0;
    private Double valorTotalTaxaDu = 0.0;
    private Double valorTotalRav = 0.0;
    private Double valorTotalRc = 0.0;
    private Double valorTotalTarifa = 0.0;
    private Double valorTotalTaxas = 0.0;
    private Double valorTotalGeral = 0.0;
    private Double valorTotalMkp = 0.0;
    private Boolean isReservaGerarCotacao = false;
    private String identificadorCotacao = null;
    private Integer codgPacote = null;

    private String sistema;
    private UsuarioDto usuario;
    private Integer fonte;
}
