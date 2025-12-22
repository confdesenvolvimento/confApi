package com.confApi.endPoints.usuario;

import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.db.confManager.usuario.dto.AuthRequestDto;
import com.confApi.db.confManager.usuario.dto.UsuarioDto;
import com.confApi.db.confManager.usuario.dto.autenticar.UsuarioResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioApi usuarioApi;

    public UsuarioResponse consultarUsuario(String login) {
        Usuario usuario = usuarioApi.consultaUsuarioByLogin(login);
        return new UsuarioResponse(usuario);
    }

    public ResponseEntity<Object> autenficarUsuarioExterno(String login) {
        Object usuario = usuarioApi.consultaUsuarioByLoginWooba(login);
        return ResponseEntity.ok((usuario));
    }

    public ResponseEntity<UsuarioDto> autenficarUsuarioAuth(@RequestBody AuthRequestDto requestDto) throws IOException {
        System.out.println("chamou login "+requestDto);
        return ResponseEntity.ok().body(usuarioApi.autenficarUsuarioAuth(requestDto).getBody());
    }
}
