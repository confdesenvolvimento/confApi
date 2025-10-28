package com.confApi.db.confManager.agencia;

import lombok.Data;

@Data
public class AgenciaClienteDto {
    private Integer codgAgencia;
    private String nomeAgencia;
    private String logomarca;
    private String email;
    private String telefone;

    public AgenciaClienteDto() {
    }

    public AgenciaClienteDto(Agencia agencia) {
        this.codgAgencia = agencia.getCodgAgencia();
        this.nomeAgencia = agencia.getNomeAgencia();
        this.logomarca = agencia.getLogomarca();
        this.email = agencia.getEmail();
        this.telefone = agencia.getTelefone();
    }
}
