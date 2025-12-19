package com.confApi.db.confManager.usuario;

import com.confApi.db.confManager.usuario.dto.AuthRequestDto;
import com.confApi.db.confManager.usuario.dto.UsuarioDto;
import com.confApi.db.confManager.usuario.dto.autenticar.UsuarioResponseDTO;
import com.confApi.endPoints.usuario.UsuarioApi;
import com.confApi.endPoints.usuario.UsuarioService;
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
        System.out.println("chamou login "+requestDto);
        return ResponseEntity.ok().body(usuarioService.autenficarUsuarioAuth(requestDto).getBody());
    }

}
