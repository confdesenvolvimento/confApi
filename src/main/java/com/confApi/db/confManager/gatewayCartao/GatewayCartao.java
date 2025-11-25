package com.confApi.db.confManager.gatewayCartao;

import lombok.Data;

import java.io.Serializable;

@Data
public class GatewayCartao implements Serializable {

    private Integer codgGateway;
    private String nomeGateway;
    private Integer flagStatus;
    private String login;
    private String senha;
    private String classeNomeSistema;

    public GatewayCartao() {
    }



    public GatewayCartao(Integer codgGateway) {
        this.codgGateway = codgGateway;
    }


}
