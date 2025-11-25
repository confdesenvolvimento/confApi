package com.confApi.hub.hotel.dto;

import lombok.*;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class InfoGlobal {
    private List<String> comentarios = new ArrayList<>();
    private Double valorTotalEstadiaNet;
    private String moeda;


}
