package com.confApi.corporate.mapper;
import com.confApi.corporate.dto.CompanhiaFamiliaDTO;
import com.confApi.corporate.dto.FamiliaDTO;
import com.confApi.corporate.dto.InformacoesFamiliaDTO;
import com.confApi.db.confManager.companhiaAerea.CompanhiaAerea;
import com.confApi.db.confManager.familia.dto.FamiliaCompanhia;
import com.confApi.db.confManager.familia.dto.FamiliaInformacoes;

import java.util.*;
import java.util.stream.Collectors;

public class FamiliaMapper {

    public static List<CompanhiaFamiliaDTO> toCompanhiaFamiliaDTOList(List<FamiliaCompanhia> origem) {
        if (origem == null || origem.isEmpty()) return Collections.emptyList();

        // Agrupa por companhia (IATA é uma chave boa; pode trocar para codgCompanhiaAerea se preferir)
        Map<String, List<FamiliaCompanhia>> porCia =
                origem.stream()
                        .filter(Objects::nonNull)
                        .filter(fc -> fc.getCompanhiaAerea() != null)
                        .collect(Collectors.groupingBy(fc -> nvl(fc.getCompanhiaAerea().getIataCia())));

        List<CompanhiaFamiliaDTO> saida = new ArrayList<>();

        for (Map.Entry<String, List<FamiliaCompanhia>> e : porCia.entrySet()) {
            List<FamiliaCompanhia> itensCia = e.getValue();
            if (itensCia.isEmpty()) continue;

            CompanhiaAerea cia = itensCia.get(0).getCompanhiaAerea();

            CompanhiaFamiliaDTO dto = new CompanhiaFamiliaDTO();
            dto.setNome(nvl(cia.getNomeCia()));
            dto.setIata(nvl(cia.getIataCia()));
            dto.setNumrCia(nvl(cia.getNumrCia()));

            // Monta as famílias
            List<FamiliaDTO> familias =
                    itensCia.stream()
                            .map(FamiliaMapper::toFamiliaDTO)
                            // se quiser ordenar pela "posicao" do JSON, descomente:
                            // .sorted(Comparator.comparingInt(fc -> nz(fc.getPosicao())))
                            .collect(Collectors.toList());

            dto.setFamiliaDTOList(familias);
            saida.add(dto);
        }

        // opcional: ordenar companhias por nome
        saida.sort(Comparator.comparing(CompanhiaFamiliaDTO::getNome, Comparator.nullsLast(String::compareToIgnoreCase)));

        return saida;
    }

    private static FamiliaDTO toFamiliaDTO(FamiliaCompanhia fc) {
        FamiliaDTO f = new FamiliaDTO();
        f.setNomeFamiliaCompanhia(nvl(fc.getNomeFamiliaCompanhia()));
        f.setNomeFamiliaCompanhiaDescricao(nvl(fc.getNomeFamiliaCompanhiaDescricao()));
        f.setCodSigla(nvl(fc.getCodSigla()));
        f.setCorFamilia(nvl(fc.getCorFamilia()));

        // tipoRota: no seu JSON é number (ex: 2). Você quer String "rota".
        // Sugestão: mapear 1=NACIONAL, 2=INTERNACIONAL (ajuste conforme tua regra)
        f.setRota(mapRota(fc.getTipoRota()));

        List<InformacoesFamiliaDTO> infos =
                Optional.ofNullable(fc.getFamiliaInformacoes())
                        .orElse(Collections.emptyList())
                        .stream()
                        .filter(Objects::nonNull)
                        // opcional: ordenar por posicao
                        .sorted(Comparator.comparingInt(i -> nz(i.getPosicao())))
                        .map(FamiliaMapper::toInformacaoDTO)
                        .collect(Collectors.toList());

        // no seu DTO o nome está "familiaDTOList", mas ele guarda InformacoesFamiliaDTO.
        // Mantive como você escreveu.
        f.setFamiliaDTOList(infos);

        return f;
    }

    private static InformacoesFamiliaDTO toInformacaoDTO(FamiliaInformacoes i) {
        InformacoesFamiliaDTO dto = new InformacoesFamiliaDTO();
        dto.setDescricao(nvl(i.getDescricao()));
        // no JSON vem int (0/1). Você definiu String no DTO:
        dto.setFlagContempla(mapContempla(i.getFlagContempla()));
        return dto;
    }

    private static String mapContempla(Integer map) {
        int v = nz(map);
        if (v == 1) return "SIM";
        if (v == 2) return "OPCIONAL";
        if (v == 0) return "NAO";
        return String.valueOf(v); // fallback
    }

    private static String mapRota(Integer tipoRota) {
        int v = nz(tipoRota);
        if (v == 1) return "NACIONAL";
        if (v == 2) return "INTERNACIONAL";
        if (v == 0) return "AMBOS";
        return String.valueOf(v); // fallback
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static int nz(Integer i) {
        return i == null ? 0 : i;
    }
}
