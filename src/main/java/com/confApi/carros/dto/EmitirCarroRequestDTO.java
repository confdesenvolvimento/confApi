package com.confApi.carros.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class EmitirCarroRequestDTO {
    private String token;
    private String localizador;
    private List<PagamentoCarroHub> pagamentoList;
    private String ip;
    private SistemaCarroHub sistema;

    public EmitirCarroRequestDTO(ReservarCarroResponseDTO obj, ReservarCarroRequestDTO req) {
        this.token = obj.getTokenSession();
        this.localizador = obj.getReservaCarro().getBookingID();
        this.pagamentoList = new ArrayList<>();
        this.pagamentoList.add(new PagamentoCarroHub("InvoicedPayment"));
        this.ip = req.getIpEmissao();
        this.sistema = req.getSistema();
    }
}
