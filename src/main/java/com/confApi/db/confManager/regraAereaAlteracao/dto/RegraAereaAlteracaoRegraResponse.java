package com.confApi.db.confManager.regraAereaAlteracao.dto;

import com.confApi.db.confManager.companhiaAerea.CompanhiaAerea;
import com.confApi.db.confManager.familia.dto.FamiliaCompanhia;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegraAereaAlteracaoRegraResponse {
    private Long id;
    private CompanhiaAerea companhiaAerea;
    private FamiliaCompanhia familiaCompanhia;
    private String classesReserva;
    private String classesReservaJson;
    private String tipoEvento;
    private String momento;

    private Boolean permiteAlteracao;
    private String statusAlteracao;
    private String tipoAlteracao;
    private Boolean cobraDiferencaTarifaria;

    private Boolean aplicaMulta;
    private String tipoMulta;
    private String criterioMulta;
    private String criterioMultiplosTrechos;
    private String moedaMulta;
    private BigDecimal valorMultaFixo;
    private BigDecimal valorMultaFixoEuropa;
    private BigDecimal valorMultaFixoDemaisInternacionais;
    private BigDecimal percentualMulta;
    private String baseCalculoMulta;
    private Boolean porPassageiro;
    private Boolean porTrecho;

    private Boolean permiteNoShow;
    private Boolean exigeReemissao;
    private String tipoReemissao;
    private Boolean permiteAlterarRota;
    private Boolean permiteAlterarCabine;
    private Boolean permiteAlterarOperadora;
    private Boolean permiteMudarDomInt;
    private String novoValorMinimo;

    private String tituloUsuario;
    private String descricaoUsuario;
    private String mensagemCalculo;
    private String orientacaoInterna;
    private String observacao;
    private String fonteUrl;
    private Object dataConsulta;
    private Integer prioridade;
    private String origemRegra;
    private String statusRevisao;
    private Boolean ativo;
    private Object dataCriacao;
    private Object dataAtualizacao;

    public String getCompanhia() {
        CompanhiaAerea companhia = companhiaDaRegra();
        if (companhia == null) return null;
        return primeiroValor(companhia.getIataCia(), companhia.getNomeCia());
    }

    public String getNomeCompanhia() {
        CompanhiaAerea companhia = companhiaDaRegra();
        return companhia == null ? null : companhia.getNomeCia();
    }

    public String getMercado() {
        if (familiaCompanhia == null) return null;
        return mercadoTipoRota(familiaCompanhia.getTipoRota());
    }

    public String getFamiliaTarifaria() {
        if (familiaCompanhia == null) return null;
        return primeiroValor(familiaCompanhia.getNomeFamiliaCompanhia(), familiaCompanhia.getNomeFamiliaCompanhiaDescricao());
    }

    public String getFamiliaTarifariaNormalizada() {
        return normalizarChave(getFamiliaTarifaria());
    }

    public String getCodigoTarifario() {
        return familiaCompanhia == null ? null : familiaCompanhia.getCodSigla();
    }

    private CompanhiaAerea companhiaDaRegra() {
        if (familiaCompanhia != null && familiaCompanhia.getCompanhiaAerea() != null) {
            return familiaCompanhia.getCompanhiaAerea();
        }
        return companhiaAerea;
    }

    private String mercadoTipoRota(Integer tipoRota) {
        if (tipoRota == null) return null;
        if (tipoRota == 0) return "AMBOS";
        if (tipoRota == 1) return "NACIONAL";
        if (tipoRota == 2) return "INTERNACIONAL";
        return null;
    }

    private String primeiroValor(String... valores) {
        if (valores == null) return null;
        for (String valor : valores) {
            if (temValor(valor)) return valor.trim();
        }
        return null;
    }

    private boolean temValor(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }

    private String normalizarChave(String valor) {
        if (!temValor(valor)) return null;
        String semAcento = Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String normalizado = semAcento.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        return normalizado.replaceAll("^_|_$", "");
    }
}
