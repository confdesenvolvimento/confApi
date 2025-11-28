package com.confApi.endPoints.usuario;

import com.confApi.db.confManager.usuario.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioApi usuarioApi;

    public UsuarioResponse consultarUsuario(String login) {
        Usuario usuario = usuarioApi.consultaUsuarioByLogin(login);
        return new UsuarioResponse(usuario);
    }
}
