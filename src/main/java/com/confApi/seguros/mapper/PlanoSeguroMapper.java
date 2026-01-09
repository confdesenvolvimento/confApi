package com.confApi.seguros.mapper;

import com.confApi.db.confManager.seguro.cobertura.SeguroCobertura;
import com.confApi.db.confManager.seguro.coberturaDetalhada.SeguroCoberturaDetalhada;
import com.confApi.seguros.dto.CoberturaSeguroDTO;
import com.confApi.seguros.dto.PlanoSeguroDTO;

import java.util.Comparator;
import java.util.List;

public final class PlanoSeguroMapper {

    private PlanoSeguroMapper() {}

    public static PlanoSeguroDTO toDTO(
            SeguroCobertura cobertura,
            List<SeguroCoberturaDetalhada> detalhes
    ) {
        if (cobertura == null) return null;

        PlanoSeguroDTO dto = new PlanoSeguroDTO();

        // Identificação do plano
        dto.setIdPlano(cobertura.getIdCobertura());
        dto.setNomePlano(cobertura.getNomeCobertura());

        // Fornecedor: você pode fixar ou buscar de outro lugar
        dto.setFornecedor("HERO");
        dto.setCodgFornecedor("HERO");

        dto.setMoedaCobertura(cobertura.getMoeda());

        // Preços
        dto.setPrecoBaseBRL(cobertura.getValorSeguroNetBrl());
        dto.setPrecoFinalBRL(
                safe(cobertura.getValorSeguroNetBrl()) +
                        safe(cobertura.getValorSeguroMkpBrl())
        );

        dto.setScore(0); // não existe no banco

        // Coberturas detalhadas
        if (detalhes != null) {
            detalhes.stream()
                    .sorted(Comparator.comparing(SeguroCoberturaDetalhada::getIdOrdemExibicao))
                    .forEach(d -> dto.getCoberturasDetalhes().add(toCoberturaDTO(d)));
        }

        // Campos de exibição (opcional)
       /* dto.setPrecoTotal(formatBRL(dto.getPrecoFinalBRL()));
        dto.setParcelamento(null);*/

        return dto;
    }

    private static CoberturaSeguroDTO toCoberturaDTO(SeguroCoberturaDetalhada d) {
        CoberturaSeguroDTO c = new CoberturaSeguroDTO();

        c.setOrderDisplay(d.getIdOrdemExibicao());
        c.setNome(d.getTituloCobertura());
        c.setDescricao(d.getDescricaoCobertura());
        c.setValor(d.getValorCobertura() == null ? null : d.getValorCobertura().doubleValue());
        c.setMoeda(d.getMoedaCobertura());

        c.setIcone(mapIcon(d.getTituloCobertura()));
        c.setObs(null);

        return c;
    }

    private static String mapIcon(String titulo) {
        if (titulo == null) return "pi pi-shield";
        String t = titulo.toLowerCase();

        if (t.contains("médic")) return "pi pi-heart-fill";
        if (t.contains("odont")) return "pi pi-smile";
        if (t.contains("bagag")) return "pi pi-briefcase";
        if (t.contains("cancel")) return "pi pi-calendar-times";
        if (t.contains("covid")) return "pi pi-shield";
        return "pi pi-check-circle";
    }

    private static Double safe(Double v) {
        return v == null ? 0.0 : v;
    }

    private static String formatBRL(Double v) {
        if (v == null) return null;
        return String.format("R$ %.2f", v).replace('.', ',');
    }
}

