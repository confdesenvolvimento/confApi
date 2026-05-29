package com.confApi.aereo.dto;

import com.confApi.aereo.eNums.TipoConsulta;
import com.confApi.aereo.eNums.TipoPesquisa;
import com.confApi.hub.aereo.dto.Aeroporto;
import com.confApi.hub.aereo.dto.Companhia;
import com.confApi.model.IdentificacaoAgenciaModel;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class PesquisaRequestDTO {
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
    private ParametrosPesquisa pesquisa;
    private List<Sistema> sistemas;
    private List<Companhia> companhias;
    private IdentificacaoAgenciaModel identificacaoAgenciaModel;

    public PesquisaRequestDTO() {
    }

    public PesquisaRequestDTO(TipoPesquisa tipoPesquisa, TipoConsulta tipoConsulta,
                           Aeroporto aeroportoOrigem, Aeroporto aeroportoDestino, Date dataIda,
                           Date dataVolta, Integer qtdADT, Integer qtdCHD, Integer qtdINF,
                           ParametrosPesquisa pesquisa, List<Sistema> sistemas, List<Companhia> companhias) {

        this.tipoPesquisa = tipoPesquisa;
        this.tipoConsulta = tipoConsulta;
        this.aeroportoOrigem = aeroportoOrigem;
        this.aeroportoDestino = aeroportoDestino;
        this.dataIda = dataIda;
        this.dataVolta = dataVolta;
        this.qtdADT = qtdADT;
        this.qtdCHD = qtdCHD;
        this.qtdINF = qtdINF;
        this.pesquisa = pesquisa;
        this.sistemas = sistemas;
        this.companhias = companhias;
    }

    public PesquisaRequestDTO(TipoPesquisa tipoPesquisa, TipoConsulta tipoConsulta,
                           Aeroporto aeroportoOrigem, Aeroporto aeroportoDestino, Date dataIda,
                           Date dataVolta, Integer qtdADT, Integer qtdCHD, Integer qtdINF,
                           ParametrosPesquisa pesquisa) {

        this.tipoPesquisa = tipoPesquisa;
        this.tipoConsulta = tipoConsulta;
        this.aeroportoOrigem = aeroportoOrigem;
        this.aeroportoDestino = aeroportoDestino;
        this.dataIda = dataIda;
        this.dataVolta = dataVolta;
        this.qtdADT = qtdADT;
        this.qtdCHD = qtdCHD;
        this.qtdINF = qtdINF;
        this.pesquisa = pesquisa;
    }
}
