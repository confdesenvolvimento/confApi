package com.confApi.aereo.dto;

import com.confApi.aereo.eNums.Classe;
import com.confApi.aereo.eNums.TipoConsulta;
import com.confApi.aereo.eNums.TipoPesquisa;
import com.confApi.hub.aereo.dto.Aeroporto;
import com.confApi.hub.aereo.dto.Companhia;
import lombok.Data;

import java.util.Date;
import java.util.List;
@Data
public class TarifarPesquisaRequest {
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
