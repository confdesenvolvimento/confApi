package com.confApi.seguros.mapper;

import com.confApi.db.confManager.seguro.cobertura.SeguroCobertura;
import com.confApi.seguros.dto.CoberturaSeguroDTO;
import com.confApi.seguros.dto.PlanoSeguroDTO;

import java.util.List;

public final class SeguroCoberturaToPlanoMapper {

    private SeguroCoberturaToPlanoMapper() {}

    /**
     * Mapeia SeguroCobertura (Manager) para PlanoSeguroDTO (Front)
     */
    public static PlanoSeguroDTO toDTO(
            SeguroCobertura c,
            List<CoberturaSeguroDTO> coberturasDetalhes
    ) {
        if (c == null) return null;

        PlanoSeguroDTO dto = new PlanoSeguroDTO();

        // Identificação do plano
        dto.setIdPlano(c.getIdCobertura());          // ex: HERO60
        dto.setNomePlano(c.getNomeCobertura());      // ex: HERO 60 PLUS

        // Fornecedor (hoje fixo HERO — se virar multi, ajuste aqui)
        dto.setFornecedor("HERO");
        dto.setCodgFornecedor("HERO");

        // Moeda principal da cobertura
        dto.setMoedaCobertura(c.getMoeda());         // ex: USD

        // Preços
        dto.setPrecoBaseBRL(c.getValorSeguroNetBrl());

        Double precoFinal =
                safe(c.getValorSeguroNetBrl()) +
                        safe(c.getValorSeguroMkpBrl());

        dto.setPrecoFinalBRL(precoFinal);

        // Score (não existe no banco hoje)
        dto.setScore(0);

        // Coberturas detalhadas
        if (coberturasDetalhes != null) {
            dto.setCoberturasDetalhes(coberturasDetalhes);
        }

        // ===== Campos auxiliares de exibição (front) =====



        // Parcelamento — se você quiser evoluir depois
      //  dto.setParcelamento(null);

        // Blocos de valores individuais (não existem no banco hoje)
        /*dto.setVoo(null);
        dto.setDmh(null);
        dto.setBagagem(null);
        dto.setOdonto(null);
        dto.setFarmacia(null);*/

        return dto;
    }

    // ===== Helpers =====

    private static Double safe(Double v) {
        return v == null ? 0.0 : v;
    }

    private static String formatBRL(Double v) {
        if (v == null) return null;
        return String.format("R$ %.2f", v).replace('.', ',');
    }
}
