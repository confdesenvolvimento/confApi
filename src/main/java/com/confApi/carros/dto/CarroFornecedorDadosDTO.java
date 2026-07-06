package com.confApi.carros.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarroFornecedorDadosDTO {

    private String bookingStatus;
    private String pagamentoStatus;

    private Integer statusReservaFornecedor;
    private Integer statusPagamentoFornecedor;

    private String restricao;
    private String restricaoUS;
    private String termosECondicoes;
    private String urlVoucherPT;
    private String urlVoucherUS;
    private String urlTravelFlow;

    private String telefoneLojaRetirada;
    private String telefoneLojaDevolucao;

    private List<PolicyHub> policies = new ArrayList<>();

    private RegrasCancelamento regrasCancelamento;

}
