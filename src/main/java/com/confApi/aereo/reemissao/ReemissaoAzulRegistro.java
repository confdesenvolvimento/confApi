package com.confApi.aereo.reemissao;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ReemissaoAzulRegistro {
    private String id, status, localizador, chaveAtiva, dadosJson;
    private Integer versao, codgUsuario, codgAgencia, reservaId;
    private LocalDateTime expiraEm, criadoEm, atualizadoEm;
}
