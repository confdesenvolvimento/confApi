package com.confApi.hub.aereo;

import com.confApi.endPoints.agencia.Agencia;
import lombok.Data;

@Data
public class AgenciaHub {
    private String login;
    private String senha;
    private String contato;
    private String email;
    private String nome;
    private String unidade;
    private String codgSistemaBackoffice;

    public AgenciaHub(Agencia obj) {
        this.login = obj.getUsuarioApi();
        this.senha = obj.getSenhaApi();
        this.contato = obj.getTelefone();
        this.email = obj.getEmail();
        this.nome = obj.getNomeAgencia();
        this.unidade = obj.getCodgUnidade() != null ? obj.getCodgUnidade().getNomeUnidade() : null;
        this.codgSistemaBackoffice = obj.getCodgSistemaBackOffice();
    }

    public AgenciaHub(String nome) {
        this.nome = nome;
    }
}
