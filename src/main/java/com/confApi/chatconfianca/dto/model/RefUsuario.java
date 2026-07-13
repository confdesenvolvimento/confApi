package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class RefUsuario {
    private Integer codgUsuario;
    private Integer codgAgencia;
    private Integer codgUnidade;
    private String nomeCompleto;
    private String loginUsuario;
    private String email;
    private String cpf;
    private String telefone;
    private String celular;
    private String tipoUsuario;
    private String administradorAgencia;
    private Integer status;
    private Boolean ativoChat;
    private LocalDateTime sincronizadoEm;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}