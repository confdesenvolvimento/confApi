package com.confApi.hub.hotel.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class HotelPesquisaQuarto {
    private Integer id;
    private String nomeQuartoPesquisa;
    private Integer qtdQuartos = 1;
    private Integer qtdAdultos = 1;
    private Integer qtdCriancas = 0;
    private List<HotelPesquisaIdadeCrianca> idadeCriancas = new ArrayList<HotelPesquisaIdadeCrianca>();
}
