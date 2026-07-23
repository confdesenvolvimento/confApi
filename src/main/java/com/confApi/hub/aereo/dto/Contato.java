package com.confApi.hub.aereo.dto;

import com.confApi.aereo.dto.PreReserva;
import com.confApi.db.confManager.usuario.dto.UsuarioDto;
import com.confApi.endPoints.contato.ContatoResponse;

import java.io.Serializable;


public class Contato implements Serializable{
    private String cidade;
    private String email;
    private String endereco;
    private String numeroDDD;
    private String numeroDDI;
    private String numeroTelefone;
    private String nome;

    public Contato(Contato contato) {
        if (contato == null) {
            return;
        }

        this.cidade = contato.getCidade();
        this.email = contato.getEmail();
        this.endereco = contato.getEndereco();
        this.numeroDDD = contato.getNumeroDDD();
        this.numeroDDI = contato.getNumeroDDI();
        this.numeroTelefone = contato.getNumeroTelefone();
        this.nome = contato.getNome();
    }

    public Contato(ContatoResponse contatoResponse) {
        this.cidade = contatoResponse.getCidade();
        this.email = contatoResponse.getEmail();
        this.endereco = contatoResponse.getEndereco();
        this.numeroDDD = contatoResponse.getNumeroDDD();
        this.numeroDDI = contatoResponse.getNumeroDDI();
        this.numeroTelefone = contatoResponse.getNumeroTelefone();
        this.nome = contatoResponse.getNome();
    }

    public Contato() {
    }

    public Contato(String cidade, String email, String endereco, String numeroDDD,
                   String numeroDDI, String numeroTelefone, String nome) {
        this.cidade = cidade;
        this.email = email;
        this.endereco = endereco;
        this.numeroDDD = numeroDDD;
        this.numeroDDI = numeroDDI;
        this.numeroTelefone = numeroTelefone;
        this.nome = nome;
    }

    public Contato(PreReserva preReserva) {
        this.numeroDDI = "55";
        this.numeroDDD = "65";
        this.numeroTelefone = "33142700";
        this.cidade = "Cuiaba";
        this.endereco = "RUA SAO SEBASTIAO";
        this.email = "confianca@confiancaturismo.com.br";
        this.nome = "Contato";

        if (preReserva == null || preReserva.getUsuario() == null) {
            return;
        }
        UsuarioDto usuario = preReserva.getUsuario();
        if (usuario.getEmail() != null && !usuario.getEmail().isBlank()) {
            this.email = usuario.getEmail();
        }

        if (usuario.getNome() != null && !usuario.getNome().isBlank()) {
            this.nome = usuario.getNome();
        }

        if (usuario.getTelefone() != null && !usuario.getTelefone().isBlank()) {
            String telefone = usuario.getTelefone().replaceAll("[^0-9]", "");

            if (telefone.length() >= 10) {
                if (telefone.startsWith("55") && telefone.length() > 11) {
                    this.numeroDDD = telefone.substring(2, 4);
                    this.numeroTelefone = telefone.substring(4);
                } else {
                    this.numeroDDD = telefone.substring(0, 2);
                    this.numeroTelefone = telefone.substring(2);
                }
                this.numeroDDI = "55";
            }
        }

        if (usuario.getAgencia() != null) {
            if (usuario.getAgencia().getEmail() != null && !usuario.getAgencia().getEmail().isBlank()) {
                this.email = usuario.getAgencia().getEmail();
            }

            if (usuario.getAgencia().getTelefone() != null && !usuario.getAgencia().getTelefone().isBlank()) {
                String telefone = usuario.getAgencia().getTelefone().replaceAll("[^0-9]", "");

                if (telefone.length() >= 10) {
                    if (telefone.startsWith("55") && telefone.length() > 11) {
                        this.numeroDDD = telefone.substring(2, 4);
                        this.numeroTelefone = telefone.substring(4);
                    } else {
                        this.numeroDDD = telefone.substring(0, 2);
                        this.numeroTelefone = telefone.substring(2);
                    }
                    this.numeroDDI = "55";
                }
            }

            if (usuario.getAgencia().getNomeAgencia() != null && !usuario.getAgencia().getNomeAgencia().isBlank()) {
                this.nome = usuario.getAgencia().getNomeAgencia();
            }

            if (usuario.getAgencia().getCidade() != null && !usuario.getAgencia().getCidade().isBlank()) {
                this.cidade = usuario.getAgencia().getCidade();
            }

            if (usuario.getAgencia().getEndereco() != null && !usuario.getAgencia().getEndereco().isBlank()) {
                this.endereco = usuario.getAgencia().getEndereco();
            }
        }
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getNumeroDDD() {
        return numeroDDD;
    }

    public void setNumeroDDD(String numeroDDD) {
        this.numeroDDD = numeroDDD;
    }

    public String getNumeroDDI() {
        return numeroDDI;
    }

    public void setNumeroDDI(String numeroDDI) {
        this.numeroDDI = numeroDDI;
    }

    public String getNumeroTelefone() {
        return numeroTelefone;
    }

    public void setNumeroTelefone(String numeroTelefone) {
        this.numeroTelefone = numeroTelefone;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
}

