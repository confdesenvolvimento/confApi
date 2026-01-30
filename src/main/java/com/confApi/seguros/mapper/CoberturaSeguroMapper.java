package com.confApi.seguros.mapper;

import com.confApi.db.confManager.seguro.coberturaDetalhada.SeguroCoberturaDetalhada;
import com.confApi.seguros.dto.CoberturaSeguroDTO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CoberturaSeguroMapper {

    private CoberturaSeguroMapper() {}

    public static CoberturaSeguroDTO toDTO(SeguroCoberturaDetalhada d) {
        if (d == null) return null;

        CoberturaSeguroDTO dto = new CoberturaSeguroDTO();

        dto.setOrderDisplay(d.getIdOrdemExibicao());
        dto.setNome(d.getTituloCobertura());
        dto.setDescricao(d.getDescricaoCobertura());

        // Valor: no banco é Integer, no DTO é Double
        if (d.getValorCobertura() != null) {
            dto.setValor(d.getValorCobertura().doubleValue());
        }

        dto.setMoeda(d.getMoedaCobertura());

        // Ícone (regra simples por tipo)
        dto.setIcone(mapIcon(d.getTituloCobertura()));

        // Observação (não existe no banco hoje)
        dto.setObs(null);

        return dto;
    }

    public static List<CoberturaSeguroDTO> toDTOList(List<SeguroCoberturaDetalhada> list) {
        if (list == null || list.isEmpty()) return new ArrayList<>();

        return list.stream()
                .sorted(Comparator.comparing(
                        SeguroCoberturaDetalhada::getIdOrdemExibicao,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .map(CoberturaSeguroMapper::toDTO)
                .toList();
    }

    // ===== Helpers =====

    private static String mapIcon(String titulo) {
        if (titulo == null) return "pi pi-shield";

        String t = titulo.toLowerCase();

        if (t.contains("médic")) return "pi pi-heart-fill";
        if (t.contains("odont")) return "pi pi-smile";
        if (t.contains("bagag")) return "pi pi-briefcase";
        if (t.contains("cancel")) return "pi pi-calendar-times";
        if (t.contains("covid")) return "pi pi-shield";
        if (t.contains("regresso")) return "pi pi-home";
        if (t.contains("esporte")) return "pi pi-bolt";

        return "pi pi-check-circle";
    }
}