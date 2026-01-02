package com.confApi.hoteis.model.reserva;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class InfoGlobalFront {
    private List<String> comentarios = new ArrayList<>();
    private Double valorTotalEstadiaNet;
    private String moeda;
}
