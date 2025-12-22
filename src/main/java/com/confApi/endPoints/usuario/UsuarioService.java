package com.confApi.endPoints.usuario;

import com.confApi.corporate.dto.usuarioExternoDTO.UsuarioExternoResponseDTO;
import com.confApi.corporate.mapper.UsuarioExternoMapper;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.db.confManager.usuario.dto.AuthRequestDto;
import com.confApi.db.confManager.usuario.dto.UsuarioDto;
import com.confApi.db.confManager.usuario.dto.autenticar.UsuarioResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
import java.util.Map;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioApi usuarioApi;

    public UsuarioResponse consultarUsuario(String login) {
        Usuario usuario = usuarioApi.consultaUsuarioByLogin(login);
        return new UsuarioResponse(usuario);
    }

    public UsuarioExternoResponseDTO autenficarUsuarioExterno(String login) throws IOException {

        Object retorno = usuarioApi.consultaUsuarioByLoginWooba(login);

        if (retorno == null) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> json = (Map<String, Object>) retorno;

        return UsuarioExternoMapper.toDTO(json);
    }

    public ResponseEntity<UsuarioDto> autenficarUsuarioAuth(@RequestBody AuthRequestDto requestDto) throws IOException {
        System.out.println("chamou login "+requestDto);
        return ResponseEntity.ok().body(usuarioApi.autenficarUsuarioAuth(requestDto).getBody());
    }
}
