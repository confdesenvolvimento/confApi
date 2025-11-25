package com.confApi.hub.hotel.dto;

import java.io.Serializable;

import com.confApi.db.confManager.hotelHospede.HotelHospede;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class Hospedes implements Serializable{
    private Integer idade;
    private String tipo;
    private String nomePassageiro;
    private String sobrenomePassageiro;
    private String sexo;
    private Integer possicao;
    private Integer rph;
    private String dataNascimento;
    // Inicio - Dados para o Pacote
    private String cpf;
    private String email;
    private String telefone;
    // Final - Dados para o Pacote

    public Hospedes(HotelHospede hospede) {
        this.idade = hospede.getHospedeIdade();
        this.tipo = hospede.getTipoHospede();
        this.nomePassageiro = hospede.getHospedeNome();
        this.sobrenomePassageiro = hospede.getHospedeSobrenome();
        this.sexo = null;
        this.possicao = null;
        this.rph = null;
        this.dataNascimento = null;
        this.cpf = null;
        this.email = null;
        this.telefone = null;
    }
}
