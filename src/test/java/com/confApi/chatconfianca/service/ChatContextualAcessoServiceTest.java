package com.confApi.chatconfianca.service;

import com.confApi.chatconfianca.client.ChatConfiancaManagerClient;
import com.confApi.chatconfianca.dto.model.RefAgencia;
import com.confApi.chatconfianca.dto.model.RefUnidade;
import com.confApi.chatconfianca.dto.model.RefUsuario;
import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.exception.RegraDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatContextualAcessoServiceTest {
    private final ChatConfiancaManagerClient manager = mock(ChatConfiancaManagerClient.class);
    private final ChatConfiancaConfigService config = mock(ChatConfiancaConfigService.class);
    private final ChatContextualAcessoService service = new ChatContextualAcessoService(manager, config);
    private final RefUsuario usuario = new RefUsuario();
    private final RefAgencia agencia = new RefAgencia();
    private final RefUnidade unidade = new RefUnidade();
    private final SessaoChatResponse sessao = new SessaoChatResponse();

    @BeforeEach
    void setUp() {
        usuario.setCodgUsuario(7);
        usuario.setTipoUsuario("Administrativo");
        usuario.setAtivoChat(true);
        agencia.setCodgAgencia(59);
        agencia.setCodgUnidade(2);
        agencia.setAtivoChat(true);
        unidade.setCodgUnidade(2);
        unidade.setAtivoChat(true);
        sessao.setUsuario(usuario);
        sessao.setAgencia(agencia);
        sessao.setUnidade(unidade);
        when(config.buscarUsuarioReferencia(7)).thenReturn(usuario);
        when(manager.get("chat-confianca/consultas/agencias/59", RefAgencia.class)).thenReturn(agencia);
        when(manager.get("chat-confianca/consultas/unidades/2", RefUnidade.class)).thenReturn(unidade);
    }

    @Test
    void administrativoGlobalPodeUsarAgenciaSelecionadaSemPerfilAdminChat() {
        assertDoesNotThrow(() -> service.validarSessao(sessao, 7, 59));
        assertFalse(sessao.isAdmin());
    }

    @Test
    void administrativoDaUnidadeNaoPodeUsarOutraUnidade() {
        usuario.setCodgUnidade(1);
        assertNegado();
    }

    @Test
    void administrativoDaAgenciaNaoPodeUsarOutraAgencia() {
        usuario.setCodgAgencia(58);
        assertNegado();
    }

    @Test
    void perfilDoCadastroPrevaleceSobrePerfilInformadoNaSessao() {
        RefUsuario cadastro = new RefUsuario();
        cadastro.setCodgUsuario(7);
        cadastro.setTipoUsuario("Agente");
        cadastro.setAtivoChat(true);
        when(config.buscarUsuarioReferencia(7)).thenReturn(cadastro);
        assertNegado();
    }

    @Test
    void agenciaInativaContinuaBloqueada() {
        agencia.setAtivoChat(false);
        assertNegado();
    }

    @Test
    void unidadeInativaContinuaBloqueada() {
        unidade.setAtivoChat(false);
        assertNegado();
    }

    @Test
    void usuarioInativoContinuaBloqueado() {
        usuario.setAtivoChat(false);
        assertNegado();
    }

    @Test
    void agenciaSolicitadaDeveCorresponderASessao() {
        assertEquals(403, assertThrows(RegraDeNegocioException.class,
                () -> service.validarSessao(sessao, 7, 58)).getStatus());
    }

    @Test
    void agenciaDevePertencerAUnidadeDaSessao() {
        agencia.setCodgUnidade(3);
        assertNegado();
    }

    private void assertNegado() {
        assertEquals(403, assertThrows(RegraDeNegocioException.class,
                () -> service.validarSessao(sessao, 7, 59)).getStatus());
    }
}
