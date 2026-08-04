package com.confApi.service;

import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.endPoints.usuario.UsuarioApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class UsuarioLogadoService {

    @Autowired
    private UsuarioApi usuarioApi;

    public UserDetails getUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof UserDetails) {
            return (UserDetails) principal;
        }

        return null;
    }

    public Usuario getLoginUsuarioLogado() {
        return usuarioApi.consultaUsuarioByLogin(getUsuarioLogado().getUsername());
    }

}
