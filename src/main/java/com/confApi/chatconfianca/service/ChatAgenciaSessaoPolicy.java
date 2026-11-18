package com.confApi.chatconfianca.service;

import com.confApi.chatconfianca.dto.model.RefUsuario;

/** Escopo comercial do cadastro no Manager; nao concede perfis de gestao do chat. */
final class ChatAgenciaSessaoPolicy {
    private ChatAgenciaSessaoPolicy() { }

    static boolean permiteSelecaoGlobal(RefUsuario usuario) {
        return usuario != null
                && Boolean.TRUE.equals(usuario.getAtivoChat())
                && usuario.getCodgAgencia() == null
                && usuario.getCodgUnidade() == null
                && "Administrativo".equalsIgnoreCase(
                        usuario.getTipoUsuario() == null ? "" : usuario.getTipoUsuario().trim());
    }
}
