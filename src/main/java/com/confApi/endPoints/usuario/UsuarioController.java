package com.confApi.endPoints.usuario;

import com.confApi.db.confManager.usuario.UsuarioService;
import com.confApi.db.confManager.usuario.dto.AuthRequestDto;
import com.confApi.db.confManager.usuario.dto.UsuarioDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/usuario")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping("/auth")
    public ResponseEntity<UsuarioDto> autenficarUsuario(@RequestBody AuthRequestDto requestDto) throws IOException {
        return ResponseEntity.ok().body(usuarioService.autenficarUsuarioAuth(requestDto).getBody());
    }

}
