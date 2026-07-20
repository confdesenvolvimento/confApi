package com.confApi.db.clube.campanha.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class CampanhasAgrupadasDTO {
    private List<CampanhaRankingDTO> campanhaRankingDTOsRegionais = new ArrayList<>();
    private List<CampanhaRankingDTO> campanhaRankingDTOsMensais = new ArrayList<>();
    private List<CampanhaRankingDTO> campanhaRankingDTOsRegulares = new ArrayList<>();
}
