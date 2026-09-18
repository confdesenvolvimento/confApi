package com.confApi.chatconfianca.service;

import com.confApi.chatconfianca.dto.model.RefUsuario;
import com.confApi.exception.RegraDeNegocioException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatConfiancaRequestAuthorizationServiceTest {
    private final ChatConfiancaConfigService configService = mock(ChatConfiancaConfigService.class);
    private ChatConfiancaRequestAuthorizationService service;

    @BeforeEach
    void setUp() {
        service = new ChatConfiancaRequestAuthorizationService(configService, "api.confplus");
    }

    @Test
    void deveVincularLoginAutenticadoAoCodigoDoUsuario() {
        RefUsuario usuario = usuario(101, "cliente.teste");
        when(configService.buscarUsuarioReferencia(101)).thenReturn(usuario);

        assertDoesNotThrow(() -> service.validarUsuario(auth("cliente.teste"), 101));
    }

    @Test
    void deveNegarTentativaDeUsarCodigoDeOutroUsuario() {
        when(configService.buscarUsuarioReferencia(202)).thenReturn(usuario(202, "outro.usuario"));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.validarUsuario(auth("cliente.teste"), 202));

        assertEquals(403, erro.getStatus());
        assertEquals("O usuario autenticado nao corresponde ao usuario informado no chat.",
                erro.getMessage());
    }

    @Test
    void deveNegarRequisicaoSemAutenticacao() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.validarUsuario(null, 101));

        assertEquals(403, erro.getStatus());
    }

    @Test
    void clienteTecnicoPayaraPodeRepresentarUsuarioDoPortal() {
        assertDoesNotThrow(() -> service.validarUsuario(auth("api.confplus"), 101));

        verify(configService, never()).buscarUsuarioReferencia(101);
        verify(configService, never()).sincronizarUsuarioReferencia(101);
    }

    private Authentication auth(String login) {
        return new UsernamePasswordAuthenticationToken(
                login,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private RefUsuario usuario(Integer codigo, String login) {
        RefUsuario usuario = new RefUsuario();
        usuario.setCodgUsuario(codigo);
        usuario.setLoginUsuario(login);
        usuario.setAtivoChat(true);
        return usuario;
    }
}
