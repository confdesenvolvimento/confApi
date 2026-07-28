package com.confApi.chatconfianca.service;

import com.confApi.chatconfianca.dto.enums.PapelAtendente;
import com.confApi.chatconfianca.dto.enums.PrioridadeConversa;
import com.confApi.chatconfianca.dto.model.Departamento;
import com.confApi.chatconfianca.dto.model.DepartamentoAtendente;
import com.confApi.chatconfianca.dto.model.DepartamentoUnidade;
import com.confApi.chatconfianca.dto.model.ChatPerfil;
import com.confApi.chatconfianca.dto.model.ChatUsuarioPerfil;
import com.confApi.chatconfianca.dto.model.RefUnidade;
import com.confApi.chatconfianca.dto.model.RefUsuario;
import com.confApi.chatconfianca.dto.model.SlaPolitica;
import com.confApi.chatconfianca.dto.request.DepartamentoAtendenteSincronizacaoRequest;
import com.confApi.chatconfianca.dto.request.DepartamentoUnidadeSincronizacaoRequest;
import com.confApi.chatconfianca.dto.request.SlaPoliticaSincronizacaoRequest;
import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatConfiancaGestaoUnidadeServiceTest {

    @Mock
    private ChatConfiancaConfigService configService;
    @Mock
    private ChatConfiancaService chatService;

    @InjectMocks
    private ChatConfiancaGestaoUnidadeService service;

    @Test
    void permiteSincronizacaoParaAdministradorGlobal() {
        SessaoChatResponse sessao = new SessaoChatResponse();
        sessao.setAdmin(true);
        RefUsuario usuario = new RefUsuario();
        usuario.setCodgUnidade(1);
        usuario.setCodgAgencia(501);
        usuario.setTipoUsuario("Administrativo");
        sessao.setUsuario(usuario);
        when(chatService.montarSessao(4389)).thenReturn(sessao);

        Departamento departamento = new Departamento();
        departamento.setId(4L);
        departamento.setNome("Tecnologia Geral");
        when(configService.buscarDepartamento(4L)).thenReturn(departamento);
        when(configService.listarDepartamentos()).thenReturn(List.of(departamento));

        RefUnidade unidade = new RefUnidade();
        unidade.setCodgUnidade(1);
        unidade.setNomeUnidade("CGB");
        unidade.setAtivoChat(true);
        when(configService.listarUnidadesReferencia()).thenReturn(List.of(unidade));

        DepartamentoUnidade vinculo = new DepartamentoUnidade();
        vinculo.setDepartamentoId(4L);
        vinculo.setCodgUnidade(1);
        DepartamentoUnidadeSincronizacaoRequest request =
                new DepartamentoUnidadeSincronizacaoRequest();
        request.setDepartamentoId(4L);
        request.setCodigosUnidade(List.of(1));
        when(configService.sincronizarDepartamentoUnidades(request))
                .thenReturn(List.of(vinculo));

        List<DepartamentoUnidade> resultado =
                service.sincronizarDepartamentoUnidades(4389, request);

        assertEquals(1, resultado.size());
        verify(configService).sincronizarDepartamentoUnidades(request);
    }

    @Test
    void bloqueiaSincronizacaoEmMassaParaGestorDeUnidade() {
        RefUsuario usuario = new RefUsuario();
        usuario.setCodgUnidade(1);
        SessaoChatResponse sessao = new SessaoChatResponse();
        sessao.setGestor(true);
        sessao.setUsuario(usuario);
        when(chatService.montarSessao(5000)).thenReturn(sessao);

        DepartamentoUnidadeSincronizacaoRequest request =
                new DepartamentoUnidadeSincronizacaoRequest();
        request.setDepartamentoId(4L);
        request.setCodigosUnidade(List.of(1, 2));

        assertThrows(
                RegraDeNegocioException.class,
                () -> service.sincronizarDepartamentoUnidades(5000, request));
    }

    @Test
    void administradorSincronizaAtendenteEmVariasUnidadesDoDepartamento() {
        SessaoChatResponse sessao = new SessaoChatResponse();
        sessao.setAdmin(true);
        sessao.setUsuario(usuario(4389, 1, 501));
        when(chatService.montarSessao(4389)).thenReturn(sessao);

        Departamento departamento = new Departamento();
        departamento.setId(4L);
        departamento.setNome("Tecnologia");
        departamento.setAtivo(true);
        when(configService.buscarDepartamento(4L)).thenReturn(departamento);
        when(configService.listarDepartamentos()).thenReturn(List.of(departamento));

        RefUsuario atendente = usuario(6000, 1, null);
        when(configService.listarUsuariosReferencia()).thenReturn(List.of(atendente));
        DepartamentoUnidade unidade1 = departamentoUnidade(11L, 1);
        DepartamentoUnidade unidade2 = departamentoUnidade(12L, 2);
        when(configService.listarDepartamentoUnidades())
                .thenReturn(List.of(unidade1, unidade2));

        ChatPerfil perfil = perfil(2L, "SUPERVISOR");
        when(configService.listarPerfis()).thenReturn(List.of(perfil));
        when(configService.listarUsuarioPerfis()).thenReturn(List.of());
        when(configService.listarDepartamentoAtendentes()).thenReturn(List.of(
                departamentoAtendente(21L, 11L, 6000, PapelAtendente.SUPERVISOR),
                departamentoAtendente(22L, 12L, 6000, PapelAtendente.SUPERVISOR)));
        when(configService.salvarUsuarioPerfil(any(ChatUsuarioPerfil.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(configService.sincronizarDepartamentoAtendente(
                any(DepartamentoAtendenteSincronizacaoRequest.class)))
                .thenReturn(List.of());

        DepartamentoAtendenteSincronizacaoRequest request =
                new DepartamentoAtendenteSincronizacaoRequest();
        request.setDepartamentoId(4L);
        request.setCodgUsuario(6000);
        request.setDepartamentoUnidadeIds(List.of(11L, 12L));
        request.setPapel(PapelAtendente.SUPERVISOR);

        service.sincronizarAtendente(4389, request);

        ArgumentCaptor<DepartamentoAtendenteSincronizacaoRequest> captor =
                ArgumentCaptor.forClass(DepartamentoAtendenteSincronizacaoRequest.class);
        verify(configService).sincronizarDepartamentoAtendente(captor.capture());
        assertEquals(List.of(11L, 12L), captor.getValue().getDepartamentoUnidadeIds());
        assertEquals(List.of(11L, 12L), captor.getValue().getDepartamentoUnidadeIdsEscopo());
        verify(configService, times(2)).salvarUsuarioPerfil(any(ChatUsuarioPerfil.class));
    }

    @Test
    void gestorNaoVinculaAtendenteAUnidadeForaDoSeuEscopo() {
        SessaoChatResponse sessao = new SessaoChatResponse();
        sessao.setGestor(true);
        sessao.setUsuario(usuario(5000, 1, null));
        when(chatService.montarSessao(5000)).thenReturn(sessao);

        Departamento departamento = new Departamento();
        departamento.setId(4L);
        departamento.setNome("Tecnologia");
        departamento.setAtivo(true);
        when(configService.buscarDepartamento(4L)).thenReturn(departamento);
        when(configService.listarDepartamentos()).thenReturn(List.of(departamento));
        when(configService.listarUsuariosReferencia())
                .thenReturn(List.of(usuario(6000, 1, null)));
        when(configService.listarDepartamentoUnidadesPorUnidade(1))
                .thenReturn(List.of(departamentoUnidade(11L, 1)));

        DepartamentoAtendenteSincronizacaoRequest request =
                new DepartamentoAtendenteSincronizacaoRequest();
        request.setDepartamentoId(4L);
        request.setCodgUsuario(6000);
        request.setDepartamentoUnidadeIds(List.of(11L, 12L));

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.sincronizarAtendente(5000, request));

        assertEquals(403, erro.getStatus());
        verify(configService, never()).sincronizarDepartamentoAtendente(any());
    }

    @Test
    void administradorGlobalPodeConcederAdminChatSemEscopoDeUnidade() {
        RefUsuario administrador = usuario(4389, 1, 501);
        SessaoChatResponse sessao = new SessaoChatResponse();
        sessao.setAdmin(true);
        sessao.setUsuario(administrador);
        when(chatService.montarSessao(4389)).thenReturn(sessao);

        ChatPerfil perfilAdmin = perfil(99L, "ADMIN_CHAT");
        when(configService.buscarPerfil(99L)).thenReturn(perfilAdmin);
        RefUsuario novoAdministrador = usuario(6000, 2, 700);
        when(configService.listarUsuariosReferencia()).thenReturn(List.of(novoAdministrador));
        when(configService.listarUsuarioPerfis()).thenReturn(List.of());
        when(configService.salvarUsuarioPerfil(any(ChatUsuarioPerfil.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChatUsuarioPerfil vinculo = new ChatUsuarioPerfil();
        vinculo.setCodgUsuario(6000);
        vinculo.setPerfilId(99L);

        ChatUsuarioPerfil salvo = service.salvarUsuarioPerfil(4389, vinculo);

        assertNull(salvo.getCodgUnidade());
        assertTrue(salvo.getAtivo());
        verify(configService).salvarUsuarioPerfil(vinculo);
    }

    @Test
    void administradorAplicaSlaEmTodasAsUnidadesAtivasDoDepartamento() {
        SessaoChatResponse sessao = new SessaoChatResponse();
        sessao.setAdmin(true);
        sessao.setUsuario(usuario(4389, 1, 501));
        when(chatService.montarSessao(4389)).thenReturn(sessao);

        Departamento departamento = new Departamento();
        departamento.setId(4L);
        departamento.setNome("Tecnologia");
        departamento.setAtivo(true);
        when(configService.buscarDepartamento(4L)).thenReturn(departamento);
        when(configService.listarDepartamentos()).thenReturn(List.of(departamento));

        DepartamentoUnidade unidade1 = departamentoUnidade(11L, 1);
        DepartamentoUnidade unidade2 = departamentoUnidade(12L, 2);
        DepartamentoUnidade inativa = departamentoUnidade(13L, 3);
        inativa.setAtivo(false);
        when(configService.listarDepartamentoUnidades())
                .thenReturn(List.of(unidade1, unidade2, inativa));

        SlaPolitica politica = new SlaPolitica();
        politica.setDepartamentoUnidadeId(11L);
        politica.setPrioridade(PrioridadeConversa.NORMAL);
        when(configService.sincronizarSlaPoliticas(
                any(SlaPoliticaSincronizacaoRequest.class)))
                .thenReturn(List.of(politica));

        SlaPoliticaSincronizacaoRequest request =
                new SlaPoliticaSincronizacaoRequest();
        request.setDepartamentoId(4L);
        request.setPrioridade(PrioridadeConversa.NORMAL);
        request.setPrimeiraRespostaMinutos(20);
        request.setResolucaoMinutos(180);
        request.setAlertaAntesMinutos(15);

        service.sincronizarSlaPoliticas(4389, request);

        ArgumentCaptor<SlaPoliticaSincronizacaoRequest> captor =
                ArgumentCaptor.forClass(SlaPoliticaSincronizacaoRequest.class);
        verify(configService).sincronizarSlaPoliticas(captor.capture());
        assertEquals(List.of(11L, 12L), captor.getValue().getDepartamentoUnidadeIdsEscopo());
        assertTrue(captor.getValue().getAtivo());
    }

    @Test
    void gestorDeUnidadeNaoPodeConcederAdminChat() {
        RefUsuario gestor = usuario(5000, 1, null);
        SessaoChatResponse sessao = new SessaoChatResponse();
        sessao.setGestor(true);
        sessao.setUsuario(gestor);
        when(chatService.montarSessao(5000)).thenReturn(sessao);
        when(configService.buscarPerfil(99L)).thenReturn(perfil(99L, "ADMIN_CHAT"));

        ChatUsuarioPerfil vinculo = new ChatUsuarioPerfil();
        vinculo.setCodgUsuario(6000);
        vinculo.setPerfilId(99L);

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.salvarUsuarioPerfil(5000, vinculo));

        assertEquals(403, erro.getStatus());
    }

    @Test
    void perfilAdminChatApareceSomenteParaAdministradorGlobal() {
        SessaoChatResponse sessaoAdmin = new SessaoChatResponse();
        sessaoAdmin.setAdmin(true);
        sessaoAdmin.setUsuario(usuario(4389, 1, 501));
        when(chatService.montarSessao(4389)).thenReturn(sessaoAdmin);

        SessaoChatResponse sessaoGestor = new SessaoChatResponse();
        sessaoGestor.setGestor(true);
        sessaoGestor.setUsuario(usuario(5000, 1, null));
        when(chatService.montarSessao(5000)).thenReturn(sessaoGestor);

        List<ChatPerfil> perfis = List.of(
                perfil(1L, "ATENDENTE"),
                perfil(2L, "SUPERVISOR"),
                perfil(3L, "GESTOR"),
                perfil(4L, "GESTOR_UNIDADE"),
                perfil(99L, "ADMIN_CHAT"));
        when(configService.listarPerfis()).thenReturn(perfis);

        List<ChatPerfil> perfisAdmin = service.listarPerfisGerenciaveis(4389);
        List<ChatPerfil> perfisGestor = service.listarPerfisGerenciaveis(5000);

        assertTrue(perfisAdmin.stream().anyMatch(item -> "ADMIN_CHAT".equals(item.getCodigo())));
        assertTrue(perfisGestor.stream().noneMatch(item -> "ADMIN_CHAT".equals(item.getCodigo())));
    }

    @Test
    void naoPermiteInativarUltimoAdministradorGlobal() {
        SessaoChatResponse sessao = new SessaoChatResponse();
        sessao.setAdmin(true);
        sessao.setUsuario(usuario(4389, 1, 501));
        when(chatService.montarSessao(4389)).thenReturn(sessao);

        ChatPerfil perfilAdmin = perfil(99L, "ADMIN_CHAT");
        when(configService.buscarPerfil(99L)).thenReturn(perfilAdmin);
        RefUsuario administrador = usuario(4389, 1, 501);
        when(configService.listarUsuariosReferencia()).thenReturn(List.of(administrador));

        ChatUsuarioPerfil existente = new ChatUsuarioPerfil();
        existente.setId(10L);
        existente.setCodgUsuario(4389);
        existente.setPerfilId(99L);
        existente.setAtivo(true);
        when(configService.buscarUsuarioPerfil(10L)).thenReturn(existente);
        when(configService.listarPerfis()).thenReturn(List.of(perfilAdmin));
        when(configService.listarUsuarioPerfis()).thenReturn(List.of(existente));

        ChatUsuarioPerfil alteracao = new ChatUsuarioPerfil();
        alteracao.setId(10L);
        alteracao.setCodgUsuario(4389);
        alteracao.setPerfilId(99L);
        alteracao.setAtivo(false);

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.salvarUsuarioPerfil(4389, alteracao));

        assertEquals(409, erro.getStatus());
    }

    @Test
    void excluirUltimoVinculoRevogaPerfilAutomaticoDaUnidade() {
        SessaoChatResponse sessao = new SessaoChatResponse();
        sessao.setAdmin(true);
        sessao.setUsuario(usuario(4389, 1, 501));
        when(chatService.montarSessao(4389)).thenReturn(sessao);

        DepartamentoUnidade unidade = departamentoUnidade(11L, 1);
        DepartamentoAtendente vinculo =
                departamentoAtendente(21L, 11L, 6000, PapelAtendente.ATENDENTE);
        when(configService.buscarDepartamentoAtendente(21L)).thenReturn(vinculo);
        when(configService.buscarDepartamentoUnidade(11L)).thenReturn(unidade);
        when(configService.listarDepartamentoUnidades()).thenReturn(List.of(unidade));
        when(configService.listarDepartamentoAtendentes()).thenReturn(List.of());

        ChatPerfil perfilAtendente = perfil(1L, "ATENDENTE");
        ChatUsuarioPerfil automatico = usuarioPerfil(
                31L, 6000, perfilAtendente.getId(), 1, true, true);
        when(configService.listarPerfis()).thenReturn(List.of(perfilAtendente));
        when(configService.listarUsuarioPerfis()).thenReturn(List.of(automatico));
        when(configService.salvarUsuarioPerfil(any(ChatUsuarioPerfil.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.excluirAtendente(4389, 21L);

        ArgumentCaptor<ChatUsuarioPerfil> captor =
                ArgumentCaptor.forClass(ChatUsuarioPerfil.class);
        verify(configService).salvarUsuarioPerfil(captor.capture());
        assertEquals(31L, captor.getValue().getId());
        assertEquals(false, captor.getValue().getAtivo());
        assertTrue(captor.getValue().getAutomatico());
    }

    @Test
    void excluirVinculoNaoRevogaPerfilConcedidoManualmente() {
        SessaoChatResponse sessao = new SessaoChatResponse();
        sessao.setAdmin(true);
        sessao.setUsuario(usuario(4389, 1, 501));
        when(chatService.montarSessao(4389)).thenReturn(sessao);

        DepartamentoUnidade unidade = departamentoUnidade(11L, 1);
        DepartamentoAtendente vinculo =
                departamentoAtendente(21L, 11L, 6000, PapelAtendente.ATENDENTE);
        when(configService.buscarDepartamentoAtendente(21L)).thenReturn(vinculo);
        when(configService.buscarDepartamentoUnidade(11L)).thenReturn(unidade);
        when(configService.listarDepartamentoUnidades()).thenReturn(List.of(unidade));
        when(configService.listarDepartamentoAtendentes()).thenReturn(List.of());

        ChatPerfil perfilAtendente = perfil(1L, "ATENDENTE");
        ChatUsuarioPerfil manual = usuarioPerfil(
                31L, 6000, perfilAtendente.getId(), 1, true, false);
        when(configService.listarPerfis()).thenReturn(List.of(perfilAtendente));
        when(configService.listarUsuarioPerfis()).thenReturn(List.of(manual));

        service.excluirAtendente(4389, 21L);

        verify(configService, never()).salvarUsuarioPerfil(any(ChatUsuarioPerfil.class));
    }

    @Test
    void perfilAutomaticoNaoPodeSerExcluidoPelaAbaDeAcessos() {
        SessaoChatResponse sessao = new SessaoChatResponse();
        sessao.setAdmin(true);
        sessao.setUsuario(usuario(4389, 1, 501));
        when(chatService.montarSessao(4389)).thenReturn(sessao);

        ChatUsuarioPerfil automatico =
                usuarioPerfil(31L, 6000, 1L, 1, true, true);
        when(configService.buscarUsuarioPerfil(31L)).thenReturn(automatico);

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.excluirUsuarioPerfil(4389, 31L));

        assertEquals(409, erro.getStatus());
        verify(configService, never()).excluirUsuarioPerfil(31L);
    }

    private RefUsuario usuario(Integer codigo, Integer unidade, Integer agencia) {
        RefUsuario usuario = new RefUsuario();
        usuario.setCodgUsuario(codigo);
        usuario.setCodgUnidade(unidade);
        usuario.setCodgAgencia(agencia);
        usuario.setAtivoChat(true);
        return usuario;
    }

    private ChatPerfil perfil(Long id, String codigo) {
        ChatPerfil perfil = new ChatPerfil();
        perfil.setId(id);
        perfil.setCodigo(codigo);
        perfil.setNome(codigo);
        perfil.setAtivo(true);
        return perfil;
    }

    private DepartamentoUnidade departamentoUnidade(Long id, Integer unidade) {
        DepartamentoUnidade vinculo = new DepartamentoUnidade();
        vinculo.setId(id);
        vinculo.setDepartamentoId(4L);
        vinculo.setCodgUnidade(unidade);
        vinculo.setAtivo(true);
        return vinculo;
    }

    private DepartamentoAtendente departamentoAtendente(Long id,
                                                         Long departamentoUnidadeId,
                                                         Integer codgUsuario,
                                                         PapelAtendente papel) {
        DepartamentoAtendente vinculo = new DepartamentoAtendente();
        vinculo.setId(id);
        vinculo.setDepartamentoUnidadeId(departamentoUnidadeId);
        vinculo.setCodgUsuario(codgUsuario);
        vinculo.setPapel(papel);
        vinculo.setAtivo(true);
        return vinculo;
    }

    private ChatUsuarioPerfil usuarioPerfil(Long id,
                                             Integer codgUsuario,
                                             Long perfilId,
                                             Integer codgUnidade,
                                             boolean ativo,
                                             boolean automatico) {
        ChatUsuarioPerfil vinculo = new ChatUsuarioPerfil();
        vinculo.setId(id);
        vinculo.setCodgUsuario(codgUsuario);
        vinculo.setPerfilId(perfilId);
        vinculo.setCodgUnidade(codgUnidade);
        vinculo.setAtivo(ativo);
        vinculo.setAutomatico(automatico);
        return vinculo;
    }
}
