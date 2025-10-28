package com.confApi.autenticador;

import com.confApi.confApp.ConfAppDto;
import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.dto.CredenciaisDTO;
import com.confApi.dto.TokenDTO;
import com.confApi.entity.Usuario;
import com.confApi.repo.UsuarioRepository;
import com.confApi.security.jwt.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.rmi.server.RemoteServer;

@RestController
@RequestMapping("/user")
public class UsuarioRest {
    @Autowired
    UsuarioRepository usuarioRepo;
    @Autowired
    JwtService jwtService;

    @PostMapping("/auth")
    public TokenDTO autenticar(@RequestBody CredenciaisDTO credencial) {

        try {
            Usuario usuario = new Usuario();
            usuario.setLogin(credencial.getLogin());
            usuario.setSenha(credencial.getSenha());
            usuario = usuarioRepo.findByLoginAndSenha(credencial.getLogin(), credencial.getSenha());
            String token = jwtService.gerarToken(usuario);

            TokenDTO tokenDTO = new TokenDTO();
            tokenDTO.setLogin(usuario.getLogin());
            tokenDTO.setToken(token);

            return tokenDTO;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não Autorizado, login ou senha invalidos.");
        }
    }

    @PostMapping("/auth2")
    public ConfAppResp autenticar2(@RequestBody CredenciaisDTO credencial) {
        System.out.println(credencial.getLogin());
        ConfAppResp tokenResp = new ConfAppService().token();
        System.out.println(tokenResp);
        return tokenResp;
    }


}
