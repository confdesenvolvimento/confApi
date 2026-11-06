package com.confApi.aereo.dto;

import com.confApi.aereo.eNums.Classe;
import com.confApi.aereo.eNums.TipoConsulta;
import com.confApi.aereo.eNums.TipoPesquisa;
import com.confApi.aereo.eNums.ModalidadeTarifa;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.confApi.hub.aereo.dto.Aeroporto;
import com.confApi.hub.aereo.dto.Companhia;
import lombok.Data;

import java.util.Date;
import java.util.List;
@Data
public class TarifarPesquisaRequest implements PesquisaModalidadeRequest {
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JsonSetter(nulls = Nulls.SKIP)
    private ModalidadeTarifa modalidadeTarifa = ModalidadeTarifa.CONS;
    private String agencia;
    private String unidade;
    private TipoPesquisa tipoPesquisa;
    private TipoConsulta tipoConsulta;
    private Aeroporto aeroportoOrigem;
    private Aeroporto aeroportoDestino;
    private Date dataIda;
    private Date dataVolta;
    private Integer qtdADT = 0;
    private Integer qtdCHD = 0;
    private Integer qtdINF = 0;
    private Sistema sistema;
    private Companhia companhia;
    private Classe classe = null;
    private List<String> voos;
    private List<String> voosVolta;
}
