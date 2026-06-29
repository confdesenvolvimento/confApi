package com.confApi.db.confManager.regraAereaReembolso.dto;

import com.confApi.db.confManager.familia.dto.FamiliaCompanhia;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegraAereaReembolsoRegraResponse {
    private Long id;
    private FamiliaCompanhia familiaCompanhia;
    private String companhia;
    private String nomeCompanhia;
    private String mercado;
    private String cabine;
    private String familiaTarifaria;
    private String familiaTarifariaNormalizada;
    private String codigoTarifario;
    private String classesReserva;
    private String momento;

    private Boolean permiteReembolso;
    private String statusReembolso;
    private String tipoReembolso;
    private Boolean reembolsaTarifa;
    private Boolean reembolsaTaxaEmbarque;
    private Boolean requerSimulacao;
    private Boolean requerValidacaoManual;

    private Boolean aplicaMulta;
    private String tipoMulta;
    private String moedaMulta;
    private BigDecimal valorMultaFixo;
    private BigDecimal percentualMulta;
    private BigDecimal percentualReembolso;
    private String baseCalculoMulta;
    private Boolean porPassageiro;
    private Boolean porTrecho;

    private String tituloUsuario;
    private String descricaoUsuario;
    private String mensagemCalculo;
    private String orientacaoInterna;
    private String observacao;
    private String fonteUrl;
    private Integer prioridade;
    private String origemRegra;
    private String statusRevisao;

    public String getCompanhia() {
        if (temValor(companhia)) return companhia;
        if (familiaCompanhia == null || familiaCompanhia.getCompanhiaAerea() == null) return null;
        return primeiroValor(familiaCompanhia.getCompanhiaAerea().getIataCia(), familiaCompanhia.getCompanhiaAerea().getNomeCia());
    }

    public String getNomeCompanhia() {
        if (temValor(nomeCompanhia)) return nomeCompanhia;
        if (familiaCompanhia == null || familiaCompanhia.getCompanhiaAerea() == null) return null;
        return familiaCompanhia.getCompanhiaAerea().getNomeCia();
    }

    public String getMercado() {
        if (temValor(mercado)) return mercado;
        if (familiaCompanhia == null) return null;
        return mercadoTipoRota(familiaCompanhia.getTipoRota());
    }

    public String getFamiliaTarifaria() {
        if (temValor(familiaTarifaria)) return familiaTarifaria;
        if (familiaCompanhia == null) return null;
        return primeiroValor(familiaCompanhia.getNomeFamiliaCompanhia(), familiaCompanhia.getNomeFamiliaCompanhiaDescricao());
    }

    public String getFamiliaTarifariaNormalizada() {
        if (temValor(familiaTarifariaNormalizada)) return familiaTarifariaNormalizada;
        return normalizarChave(getFamiliaTarifaria());
    }

    public String getCodigoTarifario() {
        if (temValor(codigoTarifario)) return codigoTarifario;
        if (familiaCompanhia == null) return null;
        return familiaCompanhia.getCodSigla();
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
