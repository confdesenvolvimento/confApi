package com.confApi.db.confManager.usuario;

import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.db.confManager.unidade.dto.Unidade;
import com.confApi.endPoints.usuario.UsuarioResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
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

    public UsuarioConfDto(UsuarioResponse usuarioResponse) {
        this.codgUsuario = usuarioResponse.getCodgUsuario();
        this.nomeUsuario = usuarioResponse.getNomeCompleto();
        this.tipoUsuario = usuarioResponse.getTipoUsuario();
        this.unidade = usuarioResponse.getUnidade() != null ? new Unidade(usuarioResponse.getUnidade()) : null;
        this.agencia = usuarioResponse.getAgencia() != null ? new Agencia(usuarioResponse.getAgencia()) : null;
        this.codigoWooba = usuarioResponse.getCodigoWooba();
        this.ruleUsuario = null;
        this.verificacaoTipoUsuario = null;
        this.emails = usuarioResponse.getEmail();
    }
}
