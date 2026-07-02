package com.confApi.db.clube.campanha.dto;

import com.confApi.db.clube.arquivoAnexo.ArquivoAnexo;
import com.confApi.db.clube.campanha.RankingEntry;
import com.confApi.db.clube.tipoProduto.TipoProduto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampanhaRankingDTO {
    private Integer codgCampanha;
    private String nomeCampanha;
    private String tituloCampanha;
    private String descricaoCampanha;
    private String descricaoCampanhaExibicao;
    private String regrasCampanha;
    private Date validadeInicio;
    private Date validadeFinal;
    private Integer flagTipoPublico;
    private Integer flagPremium;
    private String descricaoTipoProdutoCampanha;
    private Integer flagTipoContabilizaVendas;
    private Integer flagStatusCampanha;
    private String CompanhiaAerea;
    private String iataCia;
    private String numrCia;
    private TipoProduto tipoProduto;
    private ArquivoAnexo arquivoAnexo;
    private Integer flagContabilTarifa;
    private Integer flagContabilBilhete;
    private Integer flagContabilAgencia;
    private Integer flagContabilEmissor;
    private Integer quantidadeTopResultado;
    private Double valorPago;
    private Integer flagTipoContabilValorPago;
    private Integer flagTipoMercado;
    private String linkVideo;
    private Integer flagTipoMensal;
    private String descUnidade;
    private List<RankingEntryDTO> rankingCampanha;
}
