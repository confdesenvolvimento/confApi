package com.confApi.db.confManager.usuario;

import com.confApi.db.confManager.agencia.Agencia;
import com.confApi.db.confManager.unidade.Unidade;
import lombok.Data;

import java.io.Serializable;

@Data
public class UsuarioConfDto implements Serializable {

    private Integer codgUsuario;
    private String nomeUsuario;
    private String tipoUsuario;
    private Unidade unidade;
    private Agencia agencia;
    private Integer codigoWooba;
    private Integer ruleUsuario;
    private String verificacaoTipoUsuario;
    private String emails;
}
