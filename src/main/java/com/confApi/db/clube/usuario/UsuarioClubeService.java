package com.confApi.db.clube.usuario;


import com.confApi.endPoints.clube.usuario.UsuarioClubeApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioClubeService {

    @Autowired
    private UsuarioClubeApi usuarioClubeApi;

    public Page<Usuario> findAllPage(int page, int size, String nome) {
        return usuarioClubeApi.findAllPage(page, size, nome);
    }

    public List<Usuario> listaParams(Usuario usuario) {
        return usuarioClubeApi.listaParams(usuario);
    }
}
