package com.confApi.endPoints.passageiro;

import com.confApi.endPoints.bilhete.BilheteResponse;
import com.confApi.endPoints.contato.ContatoResponse;
import com.confApi.endPoints.documentoPassageiro.DocumentoPassageiroResponse;
import com.confApi.endPoints.passaporte.PassaporteResponse;
import com.confApi.endPoints.reservaValoresAereos.ReservaValoresAereoResponse;
import lombok.Data;

import java.util.List;

@Data
public class PassageiroResponse {
    private Integer codgPassageiroDb;
    private String cpf;
    private DocumentoPassageiroResponse documento = new DocumentoPassageiroResponse();
    private String email;
    private String faixaEtaria;
    private String nascimento;
    private String nome;
    private String nomeDoMeio;
    private String sobrenome;
    private PassaporteResponse passaporte = new PassaporteResponse();
    private String sexo;
    private ContatoResponse telefone = new ContatoResponse();
    private String voeBiz;
    private String idPassageiro;
    private List<ReservaValoresAereoResponse> valores;
    private List<BilheteResponse> bilhetes;
}
