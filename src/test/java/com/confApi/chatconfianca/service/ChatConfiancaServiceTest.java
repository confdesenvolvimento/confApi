package com.confApi.chatconfianca.service;

import com.confApi.chatconfianca.client.ChatConfiancaManagerClient;
import com.confApi.chatconfianca.dto.enums.DistribuicaoDepartamento;
import com.confApi.chatconfianca.dto.enums.DisponibilidadeAtendimentoHumano;
import com.confApi.chatconfianca.dto.enums.PapelAtendente;
import com.confApi.chatconfianca.dto.enums.PrioridadeConversa;
import com.confApi.chatconfianca.dto.enums.StatusAtendente;
import com.confApi.chatconfianca.dto.enums.StatusConversa;
import com.confApi.chatconfianca.dto.enums.StatusFila;
import com.confApi.chatconfianca.dto.enums.StatusMensagem;
import com.confApi.chatconfianca.dto.enums.RemetenteTipo;
import com.confApi.chatconfianca.dto.enums.VisibilidadeMensagem;
import com.confApi.chatconfianca.dto.model.AtendimentoAvaliacao;
import com.confApi.chatconfianca.dto.model.AtendenteStatus;
import com.confApi.chatconfianca.dto.model.Conversa;
import com.confApi.chatconfianca.dto.model.ConversaParticipante;
import com.confApi.chatconfianca.dto.model.ConversaTransferencia;
import com.confApi.chatconfianca.dto.model.DepartamentoAtendente;
import com.confApi.chatconfianca.dto.model.DepartamentoUnidade;
import com.confApi.chatconfianca.dto.model.FilaAtendimento;
import com.confApi.chatconfianca.dto.model.Mensagem;
import com.confApi.chatconfianca.dto.model.MensagensNaoLidasResumo;
import com.confApi.chatconfianca.dto.model.RefAgencia;
import com.confApi.chatconfianca.dto.model.RespostaRapida;
import com.confApi.chatconfianca.dto.model.RefUnidade;
import com.confApi.chatconfianca.dto.model.RefUsuario;
import com.confApi.chatconfianca.dto.model.VwFilaAtendimento;
import com.confApi.chatconfianca.dto.request.AbrirConversaRequest;
import com.confApi.chatconfianca.dto.request.AssumirAtendimentoRequest;
import com.confApi.chatconfianca.dto.request.AvaliarAtendimentoRequest;
import com.confApi.chatconfianca.dto.request.EncerrarConversaRequest;
import com.confApi.chatconfianca.dto.request.EnviarMensagemRequest;
import com.confApi.chatconfianca.dto.request.RegistrarLeituraRequest;
import com.confApi.chatconfianca.dto.response.ChatNotificacaoResumoResponse;
import com.confApi.chatconfianca.dto.response.DepartamentoAtendimentoOpcao;
import com.confApi.exception.RegraDeNegocioException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.core.ParameterizedTypeReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
class ChatConfiancaServiceTest {

    private static final Integer SOLICITANTE = 101;
    private static final Integer ATENDENTE = 202;
    private static final Integer CODG_AGENCIA = 501;
    private static final Integer CODG_UNIDADE = 1;
    private static final Long DEPARTAMENTO_UNIDADE_ID = 100L;
    private static final Long DEPARTAMENTO_REMARCACAO_ID = 200L;
    private static final Long CONVERSA_ID = 10L;
    private static final Long FILA_ID = 20L;
    private static final String LEITURA_BULK_PATH =
            "chat-confianca/persistencia/mensagem-leituras/conversas/"
                    + CONVERSA_ID + "/usuarios/";

    private final ChatConfiancaManagerClient manager = mock(ChatConfiancaManagerClient.class);
    private final ChatConfiancaConfigService configService = mock(ChatConfiancaConfigService.class);
    private ChatConfiancaService service;
    private Fixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new Fixture();
        service = new ChatConfiancaService(manager, configService);

        when(configService.sincronizarUsuarioReferencia(anyInt()))
                .thenAnswer(invocation -> usuario((Integer) invocation.getArgument(0)));
        when(configService.listarAtendentesDepartamento(DEPARTAMENTO_UNIDADE_ID))
                .thenReturn(Collections.singletonList(fixture.vinculoAtendente));
        when(configService.listarDepartamentoUnidadesPorUnidade(CODG_UNIDADE))
                .thenReturn(Collections.singletonList(fixture.departamentoUnidade));
        when(configService.listarDepartamentos()).thenReturn(Collections.emptyList());
        when(configService.buscarAtendenteStatus(ATENDENTE)).thenReturn(statusOnline());
        when(configService.salvarAtendenteStatus(any(AtendenteStatus.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(manager.get(anyString(), any(Class.class))).thenAnswer(this::responderGet);
        when(manager.getList(anyString(), any(ParameterizedTypeReference.class))).thenAnswer(this::responderGetList);
        when(manager.post(anyString(), any(), any(Class.class))).thenAnswer(this::responderPost);
    }

    @Test
    void respostasRapidasDevemUsarDepartamentoDoVinculoDaConversa() {
        RespostaRapida resposta = new RespostaRapida();
        resposta.setId(301L);
        resposta.setDepartamentoId(fixture.departamentoUnidade.getDepartamentoId());
        resposta.setCodgUnidade(CODG_UNIDADE);
        resposta.setTitulo("Solicitar localizador");
        resposta.setTexto("Por favor, informe o localizador da reserva.");
        resposta.setAtivo(true);
        when(configService.buscarDepartamentoUnidade(DEPARTAMENTO_UNIDADE_ID))
                .thenReturn(fixture.departamentoUnidade);
        when(configService.listarRespostasRapidas(
                fixture.departamentoUnidade.getDepartamentoId(), CODG_UNIDADE, true))
                .thenReturn(List.of(resposta));

        List<RespostaRapida> respostas = service.listarRespostasRapidasAtendente(
                ATENDENTE, null, CODG_UNIDADE, DEPARTAMENTO_UNIDADE_ID);

        assertEquals(List.of(resposta), respostas);
        verify(configService).buscarDepartamentoUnidade(DEPARTAMENTO_UNIDADE_ID);
        verify(configService).listarRespostasRapidas(
                fixture.departamentoUnidade.getDepartamentoId(), CODG_UNIDADE, true);
    }

    @Test
    void fluxoPrincipalDeveAbrirAssumirEnviarEncerrarEAvaliar() {
        AbrirConversaRequest abrir = new AbrirConversaRequest();
        abrir.setCodgUsuario(SOLICITANTE);
        abrir.setDepartamentoUnidadeId(DEPARTAMENTO_UNIDADE_ID);
        abrir.setAssunto("Problema com tarifa");
        abrir.setDescricaoInicial("Preciso de ajuda.");
        abrir.setPrioridade(PrioridadeConversa.NORMAL);

        Conversa aberta = service.abrirConversa(abrir);

        assertEquals(CONVERSA_ID, aberta.getId());
        assertEquals(StatusConversa.AGUARDANDO_ATENDENTE, aberta.getStatus());
        assertEquals(SOLICITANTE, aberta.getSolicitanteCodgUsuario());

        AssumirAtendimentoRequest assumir = new AssumirAtendimentoRequest();
        assumir.setFilaId(FILA_ID);
        assumir.setCodgAtendente(ATENDENTE);

        Conversa assumida = service.assumirAtendimento(assumir);

        assertEquals(StatusConversa.EM_ATENDIMENTO, assumida.getStatus());
        assertEquals(ATENDENTE, assumida.getAtendenteResponsavelCodgUsuario());
        assertEquals(StatusFila.EM_ATENDIMENTO, fixture.fila.getStatus());

        EnviarMensagemRequest mensagemRequest = new EnviarMensagemRequest();
        mensagemRequest.setConversaId(CONVERSA_ID);
        mensagemRequest.setCodgUsuario(ATENDENTE);
        mensagemRequest.setConteudo("Ola, estou verificando.");

        Mensagem mensagem = service.enviarMensagem(mensagemRequest);

        assertEquals(CONVERSA_ID, mensagem.getConversaId());
        assertEquals(ATENDENTE, mensagem.getRemetenteCodgUsuario());
        assertEquals(StatusMensagem.ENVIADA, mensagem.getStatus());
        assertEquals(VisibilidadeMensagem.PUBLICA, mensagem.getVisibilidade());

        EncerrarConversaRequest encerrar = new EncerrarConversaRequest();
        encerrar.setConversaId(CONVERSA_ID);
        encerrar.setCodgUsuario(ATENDENTE);
        encerrar.setCategoria("Resolvido");
        encerrar.setMotivo("Tarifa explicada ao cliente.");

        Conversa encerrada = service.encerrarConversa(encerrar);

        assertEquals(StatusConversa.ENCERRADA, encerrada.getStatus());
        assertEquals(ATENDENTE, encerrada.getEncerradoPorCodgUsuario());
        assertEquals("Resolvido - Tarifa explicada ao cliente.", encerrada.getMotivoEncerramento());
        Mensagem avisoEncerramento = fixture.mensagens.get(fixture.mensagens.size() - 1);
        assertEquals(RemetenteTipo.SISTEMA, avisoEncerramento.getRemetenteTipo());
        assertEquals(VisibilidadeMensagem.PUBLICA, avisoEncerramento.getVisibilidade());
        assertTrue(avisoEncerramento.getConteudo().contains("O atendente encerrou esta conversa."));
        assertTrue(avisoEncerramento.getConteudo().contains("Tarifa explicada ao cliente."));

        AvaliarAtendimentoRequest avaliar = new AvaliarAtendimentoRequest();
        avaliar.setConversaId(CONVERSA_ID);
        avaliar.setCodgUsuarioAvaliador(SOLICITANTE);
        avaliar.setNota(5);
        avaliar.setComentario("Atendimento muito bom.");

        AtendimentoAvaliacao avaliacao = service.avaliarAtendimento(avaliar);

        assertNotNull(avaliacao.getId());
        assertEquals(CONVERSA_ID, avaliacao.getConversaId());
        assertEquals(SOLICITANTE, avaliacao.getCodgUsuarioAvaliador());
        assertEquals(5, avaliacao.getNota());
        assertEquals("Atendimento muito bom.", avaliacao.getComentario());
    }

    @Test
    void conversaAssistidaDeveIniciarForaDoHorarioHumanoSemCriarFila() {
        fixture.departamentoUnidade.setHorarioAtendimentoJson("{}");
        fixture.departamentoUnidade.setMensagemForaHorario("Atendimento humano indisponivel.");

        Conversa conversa = service.iniciarConversaAssistida(
                SOLICITANTE,
                DEPARTAMENTO_UNIDADE_ID,
                "Orientacao pela ConfIA",
                "Preciso de ajuda.",
                PrioridadeConversa.NORMAL,
                "{}",
                CODG_AGENCIA);

        assertEquals(CONVERSA_ID, conversa.getId());
        assertEquals(StatusConversa.AGUARDANDO_SOLICITANTE, conversa.getStatus());
        verify(manager, never()).post(
                eq("chat-confianca/persistencia/filas"),
                any(FilaAtendimento.class),
                eq(FilaAtendimento.class));
    }

    @Test
    void conversaHumanaDeveContinuarBloqueadaForaDoHorario() {
        fixture.departamentoUnidade.setHorarioAtendimentoJson("{}");
        fixture.departamentoUnidade.setMensagemForaHorario("Atendimento humano indisponivel.");
        AbrirConversaRequest request = new AbrirConversaRequest();
        request.setCodgUsuario(SOLICITANTE);
        request.setDepartamentoUnidadeId(DEPARTAMENTO_UNIDADE_ID);
        request.setAssunto("Atendimento humano");

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.abrirConversa(request));

        assertEquals(400, erro.getStatus());
        assertEquals("Atendimento humano indisponivel.", erro.getMessage());
        verify(manager, never()).post(
                eq("chat-confianca/persistencia/conversas"),
                any(Conversa.class),
                eq(Conversa.class));
    }

    @Test
    void opcoesDevemManterEquipeVisivelForaDoHorario() {
        fixture.departamentoUnidade.setHorarioAtendimentoJson("{}");
        fixture.departamentoUnidade.setMensagemForaHorario("Equipe retorna no proximo expediente.");

        List<DepartamentoAtendimentoOpcao> opcoes =
                service.listarOpcoesAtendimentoUsuario(SOLICITANTE, CODG_AGENCIA);

        assertEquals(1, opcoes.size());
        DepartamentoAtendimentoOpcao opcao = opcoes.get(0);
        assertEquals(DisponibilidadeAtendimentoHumano.FORA_HORARIO,
                opcao.getDisponibilidadeHumano());
        assertFalse(Boolean.TRUE.equals(opcao.getPermiteHumano()));
        assertEquals("Equipe retorna no proximo expediente.",
                opcao.getMensagemDisponibilidade());
    }

    @Test
    void opcoesDevemInformarDisponibilidadeRealDaEquipe() {
        List<DepartamentoAtendimentoOpcao> opcoes =
                service.listarOpcoesAtendimentoUsuario(SOLICITANTE, CODG_AGENCIA);

        assertEquals(1, opcoes.size());
        DepartamentoAtendimentoOpcao opcao = opcoes.get(0);
        assertEquals(DisponibilidadeAtendimentoHumano.DISPONIVEL,
                opcao.getDisponibilidadeHumano());
        assertTrue(Boolean.TRUE.equals(opcao.getPermiteHumano()));
        assertTrue(Boolean.TRUE.equals(opcao.getAtendenteLivre()));
    }

    @Test
    void opcoesDevemDistinguirEquipeSemAtendente() {
        when(configService.listarAtendentesDepartamento(DEPARTAMENTO_UNIDADE_ID))
                .thenReturn(Collections.emptyList());

        List<DepartamentoAtendimentoOpcao> opcoes =
                service.listarOpcoesAtendimentoUsuario(SOLICITANTE, CODG_AGENCIA);

        assertEquals(1, opcoes.size());
        DepartamentoAtendimentoOpcao opcao = opcoes.get(0);
        assertEquals(DisponibilidadeAtendimentoHumano.SEM_ATENDENTE,
                opcao.getDisponibilidadeHumano());
        assertFalse(Boolean.TRUE.equals(opcao.getPermiteHumano()));
    }

    @Test
    void encerramentoPeloUsuarioRegistraAvisoParaOAtendente() {
        fixture.conversa = conversaParaRemarcacao(DEPARTAMENTO_UNIDADE_ID, StatusConversa.EM_ATENDIMENTO);
        fixture.conversa.setAtendenteResponsavelCodgUsuario(ATENDENTE);
        fixture.solicitanteParticipante = true;
        fixture.atendenteParticipante = true;

        EncerrarConversaRequest encerrar = new EncerrarConversaRequest();
        encerrar.setConversaId(CONVERSA_ID);
        encerrar.setCodgUsuario(SOLICITANTE);
        encerrar.setMotivo("Cliente encerrou a solicitacao.");

        Conversa encerrada = service.encerrarConversa(encerrar);

        assertEquals(StatusConversa.ENCERRADA, encerrada.getStatus());
        Mensagem avisoEncerramento = fixture.mensagens.get(fixture.mensagens.size() - 1);
        assertEquals(RemetenteTipo.SISTEMA, avisoEncerramento.getRemetenteTipo());
        assertTrue(avisoEncerramento.getConteudo().contains("O usuário encerrou esta conversa."));
    }

    @Test
    void avaliarAtendimentoNaoPermiteDuplicidade() {
        fixture.conversa = conversaEncerrada();
        fixture.solicitanteParticipante = true;
        fixture.avaliacao = new AtendimentoAvaliacao();
        fixture.avaliacao.setId(99L);
        fixture.avaliacao.setConversaId(CONVERSA_ID);
        fixture.avaliacao.setCodgUsuarioAvaliador(SOLICITANTE);
        fixture.avaliacao.setNota(4);

        AvaliarAtendimentoRequest request = new AvaliarAtendimentoRequest();
        request.setConversaId(CONVERSA_ID);
        request.setCodgUsuarioAvaliador(SOLICITANTE);
        request.setNota(5);

        RegraDeNegocioException ex = assertThrows(RegraDeNegocioException.class,
                () -> service.avaliarAtendimento(request));

        assertEquals(409, ex.getStatus());
        assertEquals("Esta conversa ja foi avaliada por este usuario.", ex.getMessage());
    }

    @Test
    void perfilAdminChatPermaneceGlobalMesmoVinculadoAUnidadeEAgencia() {
        fixture.atendente.setCodgAgencia(CODG_AGENCIA);
        fixture.perfisAtendente.add("ADMIN_CHAT");

        var sessao = service.montarSessao(ATENDENTE);

        assertTrue(sessao.isAdmin());
        assertTrue(sessao.getPerfis().contains("ADMIN_CHAT"));
        assertEquals(CODG_UNIDADE, sessao.getUnidade().getCodgUnidade());
        assertEquals(CODG_AGENCIA, sessao.getAgencia().getCodgAgencia());
    }

    @Test
    void gestorAdministrativoSemPerfilAdminContinuaSemAcessoGlobal() {
        fixture.atendente.setCodgAgencia(CODG_AGENCIA);
        fixture.atendente.setTipoUsuario("Administrativo");
        fixture.vinculoAtendente.setPapel(PapelAtendente.GESTOR);

        var sessao = service.montarSessao(ATENDENTE);

        assertTrue(sessao.isGestor());
        assertFalse(sessao.isAdmin());
    }

    @Test
    void vinculoGestorDeOutraUnidadeNaoLiberaGestaoDaUnidadeAtual() {
        fixture.vinculoAtendente.setPapel(PapelAtendente.GESTOR);
        fixture.vinculoAtendente.setDepartamentoUnidadeId(999L);

        var sessao = service.montarSessao(ATENDENTE);

        assertTrue(sessao.isAtendente());
        assertFalse(sessao.isGestor());
        assertFalse(sessao.isAdmin());
    }

    @Test
    void gestorDeUnidadeNaoConsultaHistoricoDeOutraUnidade() {
        fixture.vinculoAtendente.setPapel(PapelAtendente.GESTOR);

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.listarHistoricoUnidade(2, ATENDENTE));

        assertEquals(403, erro.getStatus());
    }

    @Test
    void registrarLeituraDeveUsarUmaUnicaChamadaBulkERetornarQuantidade() {
        fixture.conversa = conversaEncerrada();
        fixture.solicitanteParticipante = true;
        when(manager.post(
                eq(LEITURA_BULK_PATH + SOLICITANTE + "?incluirInternas=false"),
                isNull(),
                eq(Integer.class)))
                .thenReturn(4);

        int atualizadas = service.registrarLeitura(leitura(SOLICITANTE, false));

        assertEquals(4, atualizadas);
        verify(manager, times(1))
                .post(
                        eq(LEITURA_BULK_PATH + SOLICITANTE + "?incluirInternas=false"),
                        isNull(),
                        eq(Integer.class));
        verify(manager, never()).getList(
                eq("chat-confianca/consultas/conversas/" + CONVERSA_ID + "/mensagens"),
                any(ParameterizedTypeReference.class));
        verify(manager, never()).get(anyString(), eq(com.confApi.chatconfianca.dto.model.MensagemLeitura.class));
        verify(manager, never()).post(
                eq("chat-confianca/persistencia/mensagem-leituras"),
                any(),
                eq(com.confApi.chatconfianca.dto.model.MensagemLeitura.class));
        verify(manager, never()).post(
                eq("chat-confianca/persistencia/mensagens"),
                any(Mensagem.class),
                eq(Mensagem.class));
    }

    @Test
    void registrarLeituraDeveManterBloqueioParaUsuarioSemParticipacao() {
        fixture.conversa = conversaEncerrada();

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.registrarLeitura(leitura(SOLICITANTE, false)));

        assertEquals(403, erro.getStatus());
        assertEquals("Usuario nao participa da conversa.", erro.getMessage());
        verify(manager, never())
                .post(
                        eq(LEITURA_BULK_PATH + SOLICITANTE + "?incluirInternas=false"),
                        isNull(),
                        eq(Integer.class));
    }

    @Test
    void registrarLeituraDeveManterAcessoDoGestorDaUnidade() {
        fixture.conversa = conversaEncerrada();
        fixture.vinculoAtendente.setPapel(PapelAtendente.GESTOR);
        when(manager.post(
                eq(LEITURA_BULK_PATH + ATENDENTE + "?incluirInternas=true"),
                isNull(),
                eq(Integer.class)))
                .thenReturn(2);

        int atualizadas = service.registrarLeitura(leitura(ATENDENTE, true));

        assertEquals(2, atualizadas);
        verify(manager, never()).get(
                "chat-confianca/consultas/conversas/" + CONVERSA_ID
                        + "/participantes/" + ATENDENTE + "/exists",
                Boolean.class);
        verify(manager, times(1))
                .post(
                        eq(LEITURA_BULK_PATH + ATENDENTE + "?incluirInternas=true"),
                        isNull(),
                        eq(Integer.class));
    }

    @Test
    void registrarLeituraDoAtendenteResponsavelDeveIncluirNotasInternas() {
        fixture.conversa = conversaEncerrada();
        fixture.atendenteParticipante = true;
        when(manager.post(
                eq(LEITURA_BULK_PATH + ATENDENTE + "?incluirInternas=true"),
                isNull(),
                eq(Integer.class)))
                .thenReturn(1);

        int atualizadas = service.registrarLeitura(leitura(ATENDENTE, false));

        assertEquals(1, atualizadas);
        verify(manager, times(1))
                .post(
                        eq(LEITURA_BULK_PATH + ATENDENTE + "?incluirInternas=true"),
                        isNull(),
                        eq(Integer.class));
    }

    @Test
    void administradorGlobalConsultaHistoricoDeQualquerUnidadeMesmoVinculado() {
        fixture.atendente.setCodgAgencia(CODG_AGENCIA);
        fixture.perfisAtendente.add("ADMIN_CHAT");

        List<?> historico = service.listarHistoricoUnidade(2, ATENDENTE);

        assertNotNull(historico);
        assertTrue(historico.isEmpty());
    }

    @Test
    void atendenteNaoConsultaStatusDeOutroUsuario() {
        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.buscarAtendenteStatus(ATENDENTE, SOLICITANTE));

        assertEquals(403, erro.getStatus());
    }

    @Test
    void resumoNotificacoesDoClienteRetornaConversasNaoLidas() {
        fixture.resumoSolicitante.setConversasNaoLidas(2);
        fixture.resumoSolicitante.setMensagensNaoLidas(4);
        fixture.resumoSolicitante.setConversaDestaqueId(CONVERSA_ID);

        ChatNotificacaoResumoResponse resumo = service.resumirNotificacoes(SOLICITANTE);

        assertEquals(2, resumo.getConversasUsuarioNaoLidas());
        assertEquals(4, resumo.getMensagensUsuarioNaoLidas());
        assertEquals(CONVERSA_ID, resumo.getConversaUsuarioDestaqueId());
        assertEquals(2, resumo.getTotalPendencias());
        assertFalse(resumo.isAtendente());
    }

    @Test
    void resumoNotificacoesDoAtendenteIncluiFilaPermitidaENaoLidas() {
        fixture.resumoAtendente.setConversasNaoLidas(1);
        fixture.resumoAtendente.setMensagensNaoLidas(3);
        fixture.resumoAtendente.setConversaDestaqueId(CONVERSA_ID);
        FilaAtendimento fila = new FilaAtendimento();
        fila.setId(FILA_ID);
        fila.setConversaId(CONVERSA_ID);
        fila.setDepartamentoUnidadeId(DEPARTAMENTO_UNIDADE_ID);
        fila.setStatus(StatusFila.AGUARDANDO);
        fixture.filasDepartamento.add(fila);

        VwFilaAtendimento itemFila = new VwFilaAtendimento();
        itemFila.setId(FILA_ID);
        itemFila.setConversaId(CONVERSA_ID);
        itemFila.setStatus(StatusFila.AGUARDANDO);
        fixture.filaAguardando.add(itemFila);

        ChatNotificacaoResumoResponse resumo = service.resumirNotificacoes(ATENDENTE);

        assertTrue(resumo.isAtendente());
        assertEquals(1, resumo.getFilasAguardando());
        assertEquals(1, resumo.getConversasAtendenteNaoLidas());
        assertEquals(3, resumo.getMensagensAtendenteNaoLidas());
        assertEquals(CONVERSA_ID, resumo.getConversaAtendenteDestaqueId());
        assertEquals(2, resumo.getTotalPendencias());
    }

    @Test
    void deveDirecionarRemarcacaoAoUnicoDepartamentoConfiguradoDaUnidade() {
        fixture.conversa = conversaParaRemarcacao(DEPARTAMENTO_UNIDADE_ID, StatusConversa.NOVA);
        fixture.solicitanteParticipante = true;
        DepartamentoUnidade destino = departamentoRemarcacao(DEPARTAMENTO_REMARCACAO_ID);
        DepartamentoAtendente atendenteDestino =
                vinculoAtendente(DEPARTAMENTO_REMARCACAO_ID);
        when(configService.listarDepartamentoUnidadesPorUnidade(CODG_UNIDADE))
                .thenReturn(List.of(fixture.departamentoUnidade, destino));
        when(configService.listarAtendentesDepartamento(DEPARTAMENTO_REMARCACAO_ID))
                .thenReturn(List.of(atendenteDestino));

        Conversa encaminhada = service.encaminharConversaParaDepartamentoRemarcacao(
                CONVERSA_ID, SOLICITANTE, "Conclusao da simulacao de remarcacao.");

        assertEquals(DEPARTAMENTO_REMARCACAO_ID, encaminhada.getDepartamentoUnidadeId());
        assertEquals(StatusConversa.AGUARDANDO_ATENDENTE, encaminhada.getStatus());
        assertEquals(DEPARTAMENTO_REMARCACAO_ID, fixture.fila.getDepartamentoUnidadeId());
        assertEquals(StatusFila.AGUARDANDO, fixture.fila.getStatus());
        verify(manager).post(
                eq("chat-confianca/persistencia/conversa-transferencias"),
                any(ConversaTransferencia.class),
                eq(ConversaTransferencia.class));
    }

    @Test
    void devePreservarEncaminhamentoHumanoComumNoDepartamentoAtual() {
        fixture.conversa = conversaParaRemarcacao(DEPARTAMENTO_UNIDADE_ID, StatusConversa.NOVA);
        fixture.solicitanteParticipante = true;
        when(manager.get(
                "chat-confianca/persistencia/departamento-unidades/" + DEPARTAMENTO_UNIDADE_ID,
                DepartamentoUnidade.class))
                .thenReturn(fixture.departamentoUnidade);

        Conversa encaminhada = service.encaminharConversaParaAtendente(
                CONVERSA_ID, SOLICITANTE, "Atendimento humano solicitado.");

        assertEquals(DEPARTAMENTO_UNIDADE_ID, encaminhada.getDepartamentoUnidadeId());
        assertEquals(StatusConversa.AGUARDANDO_ATENDENTE, encaminhada.getStatus());
        assertEquals(DEPARTAMENTO_UNIDADE_ID, fixture.fila.getDepartamentoUnidadeId());
        verify(manager, never()).post(
                eq("chat-confianca/persistencia/conversa-transferencias"),
                any(ConversaTransferencia.class),
                eq(ConversaTransferencia.class));
    }

    @Test
    void naoDeveAlterarConversaQuandoNaoHaDestinoDeRemarcacaoConfigurado() {
        fixture.conversa = conversaParaRemarcacao(DEPARTAMENTO_UNIDADE_ID, StatusConversa.NOVA);
        fixture.solicitanteParticipante = true;
        when(configService.listarDepartamentoUnidadesPorUnidade(CODG_UNIDADE))
                .thenReturn(List.of(fixture.departamentoUnidade));

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.encaminharConversaParaDepartamentoRemarcacao(
                        CONVERSA_ID, SOLICITANTE, "Remarcacao."));

        assertEquals(409, erro.getStatus());
        assertTrue(erro.getMessage().contains("nao possui departamento ativo configurado"));
        verificarQueRoteamentoNaoFoiPersistido();
    }

    @Test
    void naoDeveAlterarConversaQuandoHaMaisDeUmDestinoDeRemarcacao() {
        fixture.conversa = conversaParaRemarcacao(DEPARTAMENTO_UNIDADE_ID, StatusConversa.NOVA);
        fixture.solicitanteParticipante = true;
        DepartamentoUnidade primeiro = departamentoRemarcacao(DEPARTAMENTO_REMARCACAO_ID);
        DepartamentoUnidade segundo = departamentoRemarcacao(201L);
        when(configService.listarDepartamentoUnidadesPorUnidade(CODG_UNIDADE))
                .thenReturn(List.of(primeiro, segundo));

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.encaminharConversaParaDepartamentoRemarcacao(
                        CONVERSA_ID, SOLICITANTE, "Remarcacao."));

        assertEquals(409, erro.getStatus());
        assertTrue(erro.getMessage().contains("mais de um departamento ativo configurado"));
        verificarQueRoteamentoNaoFoiPersistido();
    }

    @Test
    void deveManterConversaQueJaAguardaNoDepartamentoDeRemarcacao() {
        fixture.conversa = conversaParaRemarcacao(
                DEPARTAMENTO_REMARCACAO_ID, StatusConversa.AGUARDANDO_ATENDENTE);
        fixture.solicitanteParticipante = true;
        DepartamentoUnidade destino = departamentoRemarcacao(DEPARTAMENTO_REMARCACAO_ID);
        when(configService.listarDepartamentoUnidadesPorUnidade(CODG_UNIDADE))
                .thenReturn(List.of(destino));
        when(configService.listarAtendentesDepartamento(DEPARTAMENTO_REMARCACAO_ID))
                .thenReturn(List.of(vinculoAtendente(DEPARTAMENTO_REMARCACAO_ID)));

        Conversa encaminhada = service.encaminharConversaParaDepartamentoRemarcacao(
                CONVERSA_ID, SOLICITANTE, "Remarcacao.");

        assertEquals(DEPARTAMENTO_REMARCACAO_ID, encaminhada.getDepartamentoUnidadeId());
        assertEquals(StatusConversa.AGUARDANDO_ATENDENTE, encaminhada.getStatus());
        verificarQueRoteamentoNaoFoiPersistido();
    }

    @Test
    void naoDeveDirecionarRemarcacaoParaDepartamentoSemAtendenteHumano() {
        fixture.conversa = conversaParaRemarcacao(DEPARTAMENTO_UNIDADE_ID, StatusConversa.NOVA);
        fixture.solicitanteParticipante = true;
        DepartamentoUnidade destino = departamentoRemarcacao(DEPARTAMENTO_REMARCACAO_ID);
        when(configService.listarDepartamentoUnidadesPorUnidade(CODG_UNIDADE))
                .thenReturn(List.of(destino));
        when(configService.listarAtendentesDepartamento(DEPARTAMENTO_REMARCACAO_ID))
                .thenReturn(Collections.emptyList());

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.encaminharConversaParaDepartamentoRemarcacao(
                        CONVERSA_ID, SOLICITANTE, "Remarcacao."));

        assertEquals(409, erro.getStatus());
        assertTrue(erro.getMessage().contains("nao possui atendente humano ativo"));
        verificarQueRoteamentoNaoFoiPersistido();
    }

    private void verificarQueRoteamentoNaoFoiPersistido() {
        verify(manager, never()).post(
                eq("chat-confianca/persistencia/filas"),
                any(FilaAtendimento.class),
                eq(FilaAtendimento.class));
        verify(manager, never()).post(
                eq("chat-confianca/persistencia/conversas"),
                any(Conversa.class),
                eq(Conversa.class));
        verify(manager, never()).post(
                eq("chat-confianca/persistencia/conversa-transferencias"),
                any(ConversaTransferencia.class),
                eq(ConversaTransferencia.class));
    }

    private RegistrarLeituraRequest leitura(Integer codgUsuario, boolean gestor) {
        RegistrarLeituraRequest request = new RegistrarLeituraRequest();
        request.setConversaId(CONVERSA_ID);
        request.setCodgUsuario(codgUsuario);
        request.setGestor(gestor);
        return request;
    }

    private Object responderGet(InvocationOnMock invocation) {
        String path = invocation.getArgument(0);
        if (path.equals("chat-confianca/consultas/usuarios/" + SOLICITANTE)) {
            return fixture.solicitante;
        }
        if (path.equals("chat-confianca/consultas/usuarios/" + ATENDENTE)) {
            return fixture.atendente;
        }
        if (path.equals("chat-confianca/consultas/agencias/" + CODG_AGENCIA)) {
            return fixture.agencia;
        }
        if (path.equals("chat-confianca/consultas/unidades/" + CODG_UNIDADE)) {
            return fixture.unidade;
        }
        if (path.equals("chat-confianca/consultas/conversas/" + CONVERSA_ID)) {
            return fixture.conversa;
        }
        if (path.equals("chat-confianca/persistencia/filas/" + FILA_ID)) {
            return fixture.fila;
        }
        if (path.equals("chat-confianca/consultas/conversas/" + CONVERSA_ID + "/fila")) {
            return fixture.fila;
        }
        if (path.equals("chat-confianca/consultas/conversas/" + CONVERSA_ID + "/avaliacoes/" + SOLICITANTE)) {
            return fixture.avaliacao;
        }
        if (path.equals("chat-confianca/consultas/conversas/" + CONVERSA_ID + "/participantes/" + SOLICITANTE + "/exists")) {
            return fixture.solicitanteParticipante;
        }
        if (path.equals("chat-confianca/consultas/conversas/" + CONVERSA_ID + "/participantes/" + ATENDENTE + "/exists")) {
            return fixture.atendenteParticipante;
        }
        if (path.equals("chat-confianca/consultas/resumos/nao-lidas/solicitante/" + SOLICITANTE)) {
            return fixture.resumoSolicitante;
        }
        if (path.equals("chat-confianca/consultas/resumos/nao-lidas/solicitante/" + ATENDENTE)) {
            return fixture.resumoSolicitanteAtendente;
        }
        if (path.equals("chat-confianca/consultas/resumos/nao-lidas/atendente/" + ATENDENTE)) {
            return fixture.resumoAtendente;
        }
        return null;
    }

    private Object responderGetList(InvocationOnMock invocation) {
        String path = invocation.getArgument(0);
        if (path.equals("chat-confianca/consultas/departamentos-disponiveis/agencia/" + CODG_AGENCIA)) {
            return Collections.singletonList(fixture.departamentoUnidade);
        }
        if (path.equals("chat-confianca/consultas/departamento-unidades/" + DEPARTAMENTO_UNIDADE_ID + "/filas")) {
            return fixture.filasDepartamento;
        }
        if (path.equals("chat-confianca/consultas/atendentes/" + ATENDENTE + "/departamentos")) {
            return Collections.singletonList(fixture.vinculoAtendente);
        }
        if (path.startsWith("chat-confianca/consultas/usuarios/" + ATENDENTE + "/perfis")) {
            return fixture.perfisAtendente;
        }
        if (path.equals("chat-confianca/consultas/fila/status/AGUARDANDO")) {
            return fixture.filaAguardando;
        }
        if (path.equals("chat-confianca/consultas/fila/status/CHAMANDO")) {
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    private Object responderPost(InvocationOnMock invocation) {
        String path = invocation.getArgument(0);
        Object body = invocation.getArgument(1);

        if (path.equals("chat-confianca/persistencia/conversas")) {
            Conversa conversa = (Conversa) body;
            if (conversa.getId() == null) {
                conversa.setId(CONVERSA_ID);
            }
            fixture.conversa = conversa;
            return conversa;
        }
        if (path.equals("chat-confianca/persistencia/filas")) {
            FilaAtendimento fila = (FilaAtendimento) body;
            if (fila.getId() == null) {
                fila.setId(FILA_ID);
            }
            fixture.fila = fila;
            return fila;
        }
        if (path.equals("chat-confianca/persistencia/conversa-participantes")) {
            ConversaParticipante participante = (ConversaParticipante) body;
            if (ATENDENTE.equals(participante.getCodgUsuario())) {
                fixture.atendenteParticipante = true;
            }
            if (SOLICITANTE.equals(participante.getCodgUsuario())) {
                fixture.solicitanteParticipante = true;
            }
            return participante;
        }
        if (path.equals("chat-confianca/persistencia/mensagens")) {
            Mensagem mensagem = (Mensagem) body;
            if (mensagem.getId() == null) {
                mensagem.setId(fixture.proximaMensagemId++);
            }
            fixture.mensagens.add(mensagem);
            return mensagem;
        }
        if (path.equals("chat-confianca/persistencia/atendimento-avaliacoes")) {
            AtendimentoAvaliacao avaliacao = (AtendimentoAvaliacao) body;
            avaliacao.setId(30L);
            fixture.avaliacao = avaliacao;
            return avaliacao;
        }
        return body;
    }

    private RefUsuario usuario(Integer codgUsuario) {
        if (SOLICITANTE.equals(codgUsuario)) {
            return fixture.solicitante;
        }
        if (ATENDENTE.equals(codgUsuario)) {
            return fixture.atendente;
        }
        return null;
    }

    private Conversa conversaEncerrada() {
        Conversa conversa = new Conversa();
        conversa.setId(CONVERSA_ID);
        conversa.setDepartamentoUnidadeId(DEPARTAMENTO_UNIDADE_ID);
        conversa.setCodgUnidade(CODG_UNIDADE);
        conversa.setCodgAgencia(CODG_AGENCIA);
        conversa.setSolicitanteCodgUsuario(SOLICITANTE);
        conversa.setAtendenteResponsavelCodgUsuario(ATENDENTE);
        conversa.setAssunto("Problema com tarifa");
        conversa.setStatus(StatusConversa.ENCERRADA);
        conversa.setPrioridade(PrioridadeConversa.NORMAL);
        return conversa;
    }

    private Conversa conversaParaRemarcacao(
            Long departamentoUnidadeId,
            StatusConversa status) {
        Conversa conversa = new Conversa();
        conversa.setId(CONVERSA_ID);
        conversa.setDepartamentoUnidadeId(departamentoUnidadeId);
        conversa.setCodgUnidade(CODG_UNIDADE);
        conversa.setCodgAgencia(CODG_AGENCIA);
        conversa.setSolicitanteCodgUsuario(SOLICITANTE);
        conversa.setAssunto("Remarcacao aerea");
        conversa.setStatus(status);
        conversa.setPrioridade(PrioridadeConversa.NORMAL);
        return conversa;
    }

    private AtendenteStatus statusOnline() {
        AtendenteStatus status = new AtendenteStatus();
        status.setCodgUsuario(ATENDENTE);
        status.setStatus(StatusAtendente.ONLINE);
        status.setAtendimentosAtivos(0);
        return status;
    }

    private static final class Fixture {
        private final RefUsuario solicitante = solicitante();
        private final RefUsuario atendente = atendente();
        private final RefAgencia agencia = agencia();
        private final RefUnidade unidade = unidade();
        private final DepartamentoUnidade departamentoUnidade = departamentoUnidade();
        private final DepartamentoAtendente vinculoAtendente = vinculoAtendente();
        private Conversa conversa;
        private FilaAtendimento fila;
        private AtendimentoAvaliacao avaliacao;
        private boolean solicitanteParticipante;
        private boolean atendenteParticipante;
        private long proximaMensagemId = 1000L;
        private final List<Mensagem> mensagens = new ArrayList<>();
        private final List<String> perfisAtendente = new ArrayList<>(List.of("ATENDENTE"));
        private final MensagensNaoLidasResumo resumoSolicitante = new MensagensNaoLidasResumo();
        private final MensagensNaoLidasResumo resumoSolicitanteAtendente = new MensagensNaoLidasResumo();
        private final MensagensNaoLidasResumo resumoAtendente = new MensagensNaoLidasResumo();
        private final List<FilaAtendimento> filasDepartamento = new ArrayList<>();
        private final List<VwFilaAtendimento> filaAguardando = new ArrayList<>();
    }

    private static RefUsuario solicitante() {
        RefUsuario usuario = new RefUsuario();
        usuario.setCodgUsuario(SOLICITANTE);
        usuario.setCodgAgencia(CODG_AGENCIA);
        usuario.setNomeCompleto("Cliente Teste");
        usuario.setAtivoChat(true);
        usuario.setStatus(1);
        return usuario;
    }

    private static RefUsuario atendente() {
        RefUsuario usuario = new RefUsuario();
        usuario.setCodgUsuario(ATENDENTE);
        usuario.setCodgUnidade(CODG_UNIDADE);
        usuario.setNomeCompleto("Atendente Teste");
        usuario.setAtivoChat(true);
        usuario.setStatus(1);
        return usuario;
    }

    private static RefAgencia agencia() {
        RefAgencia agencia = new RefAgencia();
        agencia.setCodgAgencia(CODG_AGENCIA);
        agencia.setCodgUnidade(CODG_UNIDADE);
        agencia.setNomeAgencia("Agencia Teste");
        agencia.setAtivoChat(true);
        agencia.setStatus(1);
        return agencia;
    }

    private static RefUnidade unidade() {
        RefUnidade unidade = new RefUnidade();
        unidade.setCodgUnidade(CODG_UNIDADE);
        unidade.setNomeUnidade("Unidade Teste");
        unidade.setAtivoChat(true);
        unidade.setStatus(1);
        return unidade;
    }

    private static DepartamentoUnidade departamentoUnidade() {
        DepartamentoUnidade departamento = new DepartamentoUnidade();
        departamento.setId(DEPARTAMENTO_UNIDADE_ID);
        departamento.setDepartamentoId(1L);
        departamento.setCodgUnidade(CODG_UNIDADE);
        departamento.setNomeExibicao("Suporte");
        departamento.setPermiteChamadoAgencia(true);
        departamento.setPermiteChamadoInterno(false);
        departamento.setRecebeRemarcacaoAerea(false);
        departamento.setExigeAssunto(true);
        departamento.setDistribuicao(DistribuicaoDepartamento.MANUAL);
        departamento.setLimiteChatsPorAtendente(3);
        departamento.setAtivo(true);
        return departamento;
    }

    private static DepartamentoUnidade departamentoRemarcacao(Long id) {
        DepartamentoUnidade departamento = departamentoUnidade();
        departamento.setId(id);
        departamento.setNomeExibicao("Aereo / Remarcacao");
        departamento.setRecebeRemarcacaoAerea(true);
        return departamento;
    }

    private static DepartamentoAtendente vinculoAtendente() {
        return vinculoAtendente(DEPARTAMENTO_UNIDADE_ID);
    }

    private static DepartamentoAtendente vinculoAtendente(Long departamentoUnidadeId) {
        DepartamentoAtendente vinculo = new DepartamentoAtendente();
        vinculo.setId(1L);
        vinculo.setDepartamentoUnidadeId(departamentoUnidadeId);
        vinculo.setCodgUsuario(ATENDENTE);
        vinculo.setPapel(PapelAtendente.ATENDENTE);
        vinculo.setRecebeChamados(true);
        vinculo.setPrioridadeDistribuicao(1);
        vinculo.setLimiteChatsSimultaneos(3);
        vinculo.setAtivo(true);
        return vinculo;
    }
}
