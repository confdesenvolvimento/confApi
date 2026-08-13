package com.confApi.chatconfianca.service;

import com.confApi.aereo.AereoClient;
import com.confApi.aereo.dto.ConsultarLocalizadorRequest;
import com.confApi.aereo.dto.ConsultarLocalizadorResponse;
import com.confApi.aereo.dto.Reserva;
import com.confApi.chatconfianca.client.ChatConfiancaManagerClient;
import com.confApi.chatconfianca.dto.enums.StatusConversa;
import com.confApi.chatconfianca.dto.model.Conversa;
import com.confApi.chatconfianca.dto.model.ConversaEvento;
import com.confApi.chatconfianca.dto.model.RefAgencia;
import com.confApi.chatconfianca.dto.model.SimulacaoRemarcacao;
import com.confApi.chatconfianca.dto.remarcacao.RemarcacaoRequest;
import com.confApi.chatconfianca.dto.remarcacao.RemarcacaoSimulacaoResponse;
import com.confApi.chatconfianca.dto.remarcacao.ReservasEmitidasRemarcacaoResponse;
import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.db.confManager.aeroporto.AeroportoService;
import com.confApi.db.confManager.regraAereaAlteracao.RegraAereaAlteracaoManagerService;
import com.confApi.endPoints.reservaAereo.ReservaAereoApi;
import com.confApi.exception.RegraDeNegocioException;
import com.confApi.hub.aereo.dto.Bilhete;
import com.confApi.hub.aereo.dto.Passageiro;
import com.confApi.hub.aereo.dto.TrechoReserva;
import com.confApi.hub.aereo.dto.Voo;
import com.confApi.hub.enumerador.TipoLimite;
import com.confApi.hub.limites.LimitesService;
import com.confApi.hub.limites.dto.Disponibilidade;
import com.confApi.hub.limites.dto.LimiteCredito;
import com.confApi.hub.limites.dto.StatusResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
class ChatConfiancaRemarcacaoServiceTest {
    private static final Long SIMULACAO_ID = 70L;
    private static final Long CONVERSA_ID = 20L;
    private static final Integer USUARIO_ID = 101;
    private static final Integer AGENCIA_ID = 321;

    private final ChatConfiancaManagerClient manager = mock(ChatConfiancaManagerClient.class);
    private final ChatConfiancaService chatService = mock(ChatConfiancaService.class);
    private final AereoClient aereoClient = mock(AereoClient.class);
    private final AeroportoService aeroportoService = mock(AeroportoService.class);
    private final RegraAereaAlteracaoManagerService regraService =
            mock(RegraAereaAlteracaoManagerService.class);
    private final LimitesService limitesService = mock(LimitesService.class);
    private final ReservaAereoApi reservaAereoApi = mock(ReservaAereoApi.class);
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    private ChatConfiancaRemarcacaoService service;
    private SimulacaoRemarcacao simulacao;
    private SessaoChatResponse sessao;
    private Conversa conversa;

    @BeforeEach
    void setUp() {
        service = new ChatConfiancaRemarcacaoService(
                manager,
                chatService,
                aereoClient,
                aeroportoService,
                regraService,
                limitesService,
                mapper,
                reservaAereoApi);

        simulacao = new SimulacaoRemarcacao();
        simulacao.setId(SIMULACAO_ID);
        simulacao.setConversaId(CONVERSA_ID);
        simulacao.setCodgUsuario(USUARIO_ID);
        simulacao.setCodgAgencia(AGENCIA_ID);
        simulacao.setLocalizador("ABC123");
        simulacao.setTrechoIndice(0);
        simulacao.setStatus("AGUARDANDO_PASSAGEIROS");
        simulacao.setExpiraEm(LocalDateTime.now().plusMinutes(20));

        conversa = new Conversa();
        conversa.setId(CONVERSA_ID);
        conversa.setSolicitanteCodgUsuario(USUARIO_ID);
        conversa.setCodgAgencia(AGENCIA_ID);
        conversa.setCodgUnidade(12);
        conversa.setStatus(StatusConversa.AGUARDANDO_SOLICITANTE);
        conversa.setMetadadosJson("{\"origem\":\"CONFIA\"}");
        when(chatService.buscarConversa(CONVERSA_ID, USUARIO_ID, false)).thenReturn(conversa);
        sessao = new SessaoChatResponse();
        RefAgencia agencia = new RefAgencia();
        agencia.setCodgAgencia(AGENCIA_ID);
        agencia.setCodgSistemaBackoffice("987");
        sessao.setAgencia(agencia);
        when(chatService.montarSessao(USUARIO_ID, null)).thenReturn(sessao);
        when(chatService.montarSessao(USUARIO_ID, AGENCIA_ID)).thenReturn(sessao);
        when(chatService.departamentoRemarcacaoDisponivel(CONVERSA_ID)).thenReturn(true);
        when(manager.get(anyString(), any(Class.class))).thenReturn(simulacao);
        when(manager.post(anyString(), any(), any(Class.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    @Test
    void deveDerivarAgenciaDaConversaEPropagarFiltrosSemFiltrarPorCriador() {
        when(manager.get(
                contains("chat-confianca/consultas/remarcacoes/reservas-emitidas"),
                eq(ReservasEmitidasRemarcacaoResponse.class)))
                .thenReturn(respostaSelecao());

        service.listarReservasEmitidas(
                CONVERSA_ID,
                USUARIO_ID,
                "ABC 123",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                2,
                25);

        ArgumentCaptor<String> path = ArgumentCaptor.forClass(String.class);
        verify(manager).get(path.capture(), eq(ReservasEmitidasRemarcacaoResponse.class));
        assertTrue(path.getValue().contains("codgAgencia=" + AGENCIA_ID));
        assertTrue(path.getValue().contains("busca=ABC+123"));
        assertTrue(path.getValue().contains("dataEmissaoInicio=2026-07-01"));
        assertTrue(path.getValue().contains("dataEmissaoFim=2026-07-31"));
        assertTrue(path.getValue().contains("page=2"));
        assertTrue(path.getValue().contains("size=25"));
        assertFalse(path.getValue().contains("codgUsuario"));
    }

    @Test
    void deveRejeitarUsuarioDeOutraAgenciaAntesDeConsultarReservas() {
        sessao.getAgencia().setCodgAgencia(999);

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.listarReservasEmitidas(
                        CONVERSA_ID, USUARIO_ID, null, null, null, 0, 10));

        assertEquals(403, erro.getStatus());
        verify(manager, never()).get(
                contains("chat-confianca/consultas/remarcacoes/reservas-emitidas"),
                eq(ReservasEmitidasRemarcacaoResponse.class));
    }

    @Test
    void deveRejeitarConversaDeOutroUsuario() {
        Integer outroUsuario = 202;
        Conversa conversa = new Conversa();
        conversa.setId(CONVERSA_ID);
        conversa.setSolicitanteCodgUsuario(USUARIO_ID);
        conversa.setCodgAgencia(AGENCIA_ID);
        when(chatService.buscarConversa(CONVERSA_ID, outroUsuario, false)).thenReturn(conversa);

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.listarReservasEmitidas(
                        CONVERSA_ID, outroUsuario, null, null, null, 0, 10));

        assertEquals(403, erro.getStatus());
        verify(manager, never()).get(
                contains("chat-confianca/consultas/remarcacoes/reservas-emitidas"),
                eq(ReservasEmitidasRemarcacaoResponse.class));
    }

    @Test
    void naoDeveListarReservasQuandoConversaConfiaEstaEncerrada() {
        conversa.setStatus(StatusConversa.ENCERRADA);

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.listarReservasEmitidas(
                        CONVERSA_ID, USUARIO_ID, null, null, null, 0, 10));

        assertEquals(409, erro.getStatus());
        verify(manager, never()).get(
                contains("chat-confianca/consultas/remarcacoes/reservas-emitidas"),
                eq(ReservasEmitidasRemarcacaoResponse.class));
        verify(manager, never()).post(anyString(), any(), any(Class.class));
    }

    @Test
    void naoDeveIniciarSimulacaoQuandoConversaJaEstaComAtendimentoHumano() {
        conversa.setStatus(StatusConversa.EM_ATENDIMENTO);
        conversa.setAtendenteResponsavelCodgUsuario(700);

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.iniciar(iniciar(501, "ABC123")));

        assertEquals(409, erro.getStatus());
        verify(manager, never()).get(
                contains("chat-confianca/consultas/remarcacoes/reservas-emitidas"),
                eq(ReservasEmitidasRemarcacaoResponse.class));
        verify(manager, never()).post(anyString(), any(), any(Class.class));
        verify(aereoClient, never()).carregarReserva(any());
    }

    @Test
    void naoDevePermitirMutacaoDepoisQueConversaSaiuDaConfia() {
        conversa.setStatus(StatusConversa.AGUARDANDO_ATENDENTE);
        conversa.setAtendenteResponsavelCodgUsuario(700);
        RemarcacaoRequest.SelecionarPassageiros request = new RemarcacaoRequest.SelecionarPassageiros();
        request.setCodgUsuario(USUARIO_ID);
        request.setEscopo("TODOS");

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.selecionarPassageiros(SIMULACAO_ID, request));

        assertEquals(409, erro.getStatus());
        verify(manager, never()).post(anyString(), any(), any(Class.class));
        verify(aereoClient, never()).carregarReserva(any());
    }

    @Test
    void devePermitirConsultarResultadoFinalDepoisDoHandoff() {
        conversa.setStatus(StatusConversa.AGUARDANDO_ATENDENTE);
        conversa.setAtendenteResponsavelCodgUsuario(700);
        simulacao.setStatus("ENCAMINHADO");

        RemarcacaoSimulacaoResponse response = service.consultar(SIMULACAO_ID, USUARIO_ID);

        assertEquals("ENCAMINHADO", response.getStatus());
        assertEquals("Solicitacao encaminhada", response.getTitulo());
    }

    @Test
    void deveSelecionarReservaExatamentePorIdEPersistirReferencia() {
        ReservasEmitidasRemarcacaoResponse.Item item = reservaEmitida(501, "ABC123", "G3", "Wooba");
        when(manager.get(
                contains("chat-confianca/consultas/remarcacoes/reservas-emitidas"),
                eq(ReservasEmitidasRemarcacaoResponse.class)))
                .thenReturn(respostaSelecao(item));
        when(aereoClient.carregarReserva(any())).thenReturn(new ConsultarLocalizadorResponse());

        service.iniciar(iniciar(501, "abc123"));

        verify(manager).get(
                contains("codgAgencia=" + AGENCIA_ID + "&reservaId=501"),
                eq(ReservasEmitidasRemarcacaoResponse.class));
        ArgumentCaptor<SimulacaoRemarcacao> captor = ArgumentCaptor.forClass(SimulacaoRemarcacao.class);
        verify(manager, atLeastOnce()).post(
                eq("chat-confianca/persistencia/simulacoes-remarcacao"),
                captor.capture(),
                eq(SimulacaoRemarcacao.class));
        assertTrue(captor.getAllValues().stream()
                .anyMatch(itemSalvo -> Integer.valueOf(501).equals(itemSalvo.getReservaAereoId())));
    }

    @Test
    void deveAceitarReservasDasCompanhiasSuportadasIndependentementeDoSistema() {
        when(aereoClient.carregarReserva(any())).thenReturn(new ConsultarLocalizadorResponse());

        for (String companhia : List.of("G3", "LA", "JJ", "AD")) {
            when(manager.get(
                    contains("chat-confianca/consultas/remarcacoes/reservas-emitidas"),
                    eq(ReservasEmitidasRemarcacaoResponse.class)))
                    .thenReturn(respostaSelecao(
                            reservaEmitida(501, "ABC123", companhia, "Sistema sem integracao")));

            RemarcacaoSimulacaoResponse response = service.iniciar(iniciar(501, "ABC123"));

            assertNotNull(response, "A companhia " + companhia + " deve ser aceita.");
        }
    }

    @Test
    void deveRejeitarReservaDeCompanhiaNaoSuportadaAntesDeConsultarHub() {
        when(manager.get(
                contains("chat-confianca/consultas/remarcacoes/reservas-emitidas"),
                eq(ReservasEmitidasRemarcacaoResponse.class)))
                .thenReturn(respostaSelecao(reservaEmitida(501, "ABC123", "AA", "Wooba")));

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.iniciar(iniciar(501, "ABC123")));

        assertEquals(409, erro.getStatus());
        assertTrue(erro.getMessage().contains("G3, LA, JJ e AD"));
        verify(aereoClient, never()).carregarReserva(any());
    }

    @Test
    void deveRejeitarReservaIdInexistenteOuDeOutraAgencia() {
        when(manager.get(
                contains("chat-confianca/consultas/remarcacoes/reservas-emitidas"),
                eq(ReservasEmitidasRemarcacaoResponse.class)))
                .thenReturn(respostaSelecao());

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.iniciar(iniciar(999, "ABC123")));

        assertEquals(404, erro.getStatus());
        verify(aereoClient, never()).carregarReserva(any());
    }

    @Test
    void deveRejeitarQuandoIdNaoCorrespondeAoLocalizadorExato() {
        when(manager.get(
                contains("chat-confianca/consultas/remarcacoes/reservas-emitidas"),
                eq(ReservasEmitidasRemarcacaoResponse.class)))
                .thenReturn(respostaSelecao(reservaEmitida(501, "ABC123", "G3", "Wooba")));

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.iniciar(iniciar(501, "XYZ999")));

        assertEquals(409, erro.getStatus());
        verify(aereoClient, never()).carregarReserva(any());
    }

    @Test
    void legadoPorLocalizadorDeveRejeitarColisaoAmbigua() {
        when(manager.get(
                contains("chat-confianca/consultas/remarcacoes/reservas-emitidas"),
                eq(ReservasEmitidasRemarcacaoResponse.class)))
                .thenReturn(respostaSelecao(
                        reservaEmitida(501, "ABC123", "G3", "Wooba"),
                        reservaEmitida(502, "ABC123", "LA", "Wooba")));

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.iniciar(iniciar(null, "ABC123")));

        assertEquals(409, erro.getStatus());
        verify(aereoClient, never()).carregarReserva(any());
    }

    @Test
    void naoDeveUsarPrimeiraReservaQuandoLocalizadorNaoCorresponde() {
        when(manager.get(
                contains("chat-confianca/consultas/remarcacoes/reservas-emitidas"),
                eq(ReservasEmitidasRemarcacaoResponse.class)))
                .thenReturn(respostaSelecao(reservaEmitida(501, "ABC123", "G3", "Wooba")));
        Reserva outraReserva = new Reserva();
        outraReserva.setLocalizador("XYZ999");
        outraReserva.setStatus("CANCELADA");
        ConsultarLocalizadorResponse hub = new ConsultarLocalizadorResponse();
        hub.setReservas(List.of(outraReserva));
        when(aereoClient.carregarReserva(any())).thenReturn(hub);

        RemarcacaoSimulacaoResponse response = service.iniciar(iniciar(501, "ABC123"));

        assertEquals("Nao foi possivel carregar a reserva informada.", response.getMensagem());
    }

    @Test
    void deveRejeitarCorrespondenciaHubDeOutraCompanhia() {
        when(manager.get(
                contains("chat-confianca/consultas/remarcacoes/reservas-emitidas"),
                eq(ReservasEmitidasRemarcacaoResponse.class)))
                .thenReturn(respostaSelecao(reservaEmitida(501, "ABC123", "G3", "Wooba")));
        ConsultarLocalizadorResponse hub = new ConsultarLocalizadorResponse();
        hub.setReservas(List.of(reservaHub("ABC123", "Amadeus", "LA")));
        when(aereoClient.carregarReserva(any())).thenReturn(hub);

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.iniciar(iniciar(501, "ABC123")));

        assertEquals(409, erro.getStatus());
    }

    @Test
    void deveIgnorarSistemaAoConferirReservaCarregadaEMantenRequestWooba() {
        when(manager.get(
                contains("chat-confianca/consultas/remarcacoes/reservas-emitidas"),
                eq(ReservasEmitidasRemarcacaoResponse.class)))
                .thenReturn(respostaSelecao(reservaEmitida(501, "ABC123", "JJ", "Sabre")));
        Reserva reservaCarregada = reservaHub("ABC123", "Amadeus", "LA");
        reservaCarregada.setDataEmissao(new java.util.Date());
        ConsultarLocalizadorResponse hub = new ConsultarLocalizadorResponse();
        hub.setReservas(List.of(reservaCarregada));
        when(aereoClient.carregarReserva(any())).thenReturn(hub);

        RemarcacaoSimulacaoResponse response = service.iniciar(iniciar(501, "ABC123"));

        assertEquals("A reserva nao retornou passageiros.", response.getMensagem());
        ArgumentCaptor<ConsultarLocalizadorRequest> request =
                ArgumentCaptor.forClass(ConsultarLocalizadorRequest.class);
        verify(aereoClient).carregarReserva(request.capture());
        assertEquals("Wooba", request.getValue().getSistema());
    }

    @Test
    void deveTratarLaDaReservaComoEquivalenteAJjRetornadaPeloHub() {
        when(manager.get(
                contains("chat-confianca/consultas/remarcacoes/reservas-emitidas"),
                eq(ReservasEmitidasRemarcacaoResponse.class)))
                .thenReturn(respostaSelecao(reservaEmitida(501, "ABC123", "LA", "Outro sistema")));
        Reserva reservaCarregada = reservaHub("ABC123", "Sistema diferente", "JJ");
        reservaCarregada.setDataEmissao(new java.util.Date());
        ConsultarLocalizadorResponse hub = new ConsultarLocalizadorResponse();
        hub.setReservas(List.of(reservaCarregada));
        when(aereoClient.carregarReserva(any())).thenReturn(hub);

        RemarcacaoSimulacaoResponse response = service.iniciar(iniciar(501, "ABC123"));

        assertEquals("A reserva nao retornou passageiros.", response.getMensagem());
    }

    @Test
    void deveManterLegadoSeguroQuandoLocalizadorTemUmaCorrespondenciaExata() {
        when(manager.get(
                contains("chat-confianca/consultas/remarcacoes/reservas-emitidas"),
                eq(ReservasEmitidasRemarcacaoResponse.class)))
                .thenReturn(respostaSelecao(reservaEmitida(501, "ABC123", "G3", "Wooba")));
        Reserva reserva = reservaHub("ABC123", "Wooba", "G3");
        reserva.setDataEmissao(new java.util.Date());
        ConsultarLocalizadorResponse hub = new ConsultarLocalizadorResponse();
        hub.setReservas(List.of(reserva));
        when(aereoClient.carregarReserva(any())).thenReturn(hub);

        service.iniciar(iniciar(null, "abc123"));

        verify(manager).get(
                contains("codgAgencia=" + AGENCIA_ID + "&busca=ABC123"),
                eq(ReservasEmitidasRemarcacaoResponse.class));
        ArgumentCaptor<SimulacaoRemarcacao> captor = ArgumentCaptor.forClass(SimulacaoRemarcacao.class);
        verify(manager, atLeastOnce()).post(
                eq("chat-confianca/persistencia/simulacoes-remarcacao"),
                captor.capture(),
                eq(SimulacaoRemarcacao.class));
        assertTrue(captor.getAllValues().stream()
                .anyMatch(itemSalvo -> Integer.valueOf(501).equals(itemSalvo.getReservaAereoId())));
    }

    @Test
    void deveDesambiguarHubSomenteComCompanhiaDaReservaSelecionada() {
        when(manager.get(
                contains("chat-confianca/consultas/remarcacoes/reservas-emitidas"),
                eq(ReservasEmitidasRemarcacaoResponse.class)))
                .thenReturn(respostaSelecao(reservaEmitida(501, "ABC123", "G3", "Wooba")));

        Reserva incorreta = reservaHub("ABC123", "Amadeus", "LA");
        incorreta.setStatus("CANCELADA");
        Reserva correta = reservaHub("ABC123", "Sabre", "G3");
        correta.setDataEmissao(new java.util.Date());
        ConsultarLocalizadorResponse hub = new ConsultarLocalizadorResponse();
        hub.setReservas(List.of(incorreta, correta));
        when(aereoClient.carregarReserva(any())).thenReturn(hub);

        RemarcacaoSimulacaoResponse response = service.iniciar(iniciar(501, "ABC123"));

        assertEquals("A reserva nao retornou passageiros.", response.getMensagem());
    }

    @Test
    void deveFalharQuandoCompanhiaAindaDeixaLocalizadorAmbiguoNoHub() {
        when(manager.get(
                contains("chat-confianca/consultas/remarcacoes/reservas-emitidas"),
                eq(ReservasEmitidasRemarcacaoResponse.class)))
                .thenReturn(respostaSelecao(reservaEmitida(501, "ABC123", "G3", "Wooba")));
        ConsultarLocalizadorResponse hub = new ConsultarLocalizadorResponse();
        hub.setReservas(List.of(
                reservaHub("ABC123", "Wooba", "G3"),
                reservaHub("ABC123", "Sabre", "G3")));
        when(aereoClient.carregarReserva(any())).thenReturn(hub);

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.iniciar(iniciar(501, "ABC123")));

        assertEquals(409, erro.getStatus());
    }

    @Test
    void deveSelecionarUmPassageiroSemIncluirOsDemais() {
        when(aereoClient.carregarReserva(any())).thenReturn(
                respostaReserva(
                        passageiro("Maria", "ADT", "001", "ATIVO"),
                        passageiro("Joao", "CHD", "002", "ATIVO")));

        RemarcacaoRequest.SelecionarPassageiros request =
                new RemarcacaoRequest.SelecionarPassageiros();
        request.setCodgUsuario(USUARIO_ID);
        request.setEscopo("INDIVIDUAL");
        request.setPassageiroIndice(1);

        RemarcacaoSimulacaoResponse response =
                service.selecionarPassageiros(SIMULACAO_ID, request);

        assertEquals("AGUARDANDO_CRITERIOS", response.getStatus());
        assertFalse(response.getPassageiros().get(0).isSelecionado());
        assertTrue(response.getPassageiros().get(1).isSelecionado());
        assertTrue(simulacao.getPassageirosJson().contains("\"indices\":[1]"));
    }

    @Test
    void naoDeveSelecionarTodosQuandoHaBilheteInativo() {
        when(aereoClient.carregarReserva(any())).thenReturn(
                respostaReserva(
                        passageiro("Maria", "ADT", "001", "ATIVO"),
                        passageiro("Joao", "CHD", "002", "CANCELADO")));

        RemarcacaoRequest.SelecionarPassageiros request =
                new RemarcacaoRequest.SelecionarPassageiros();
        request.setCodgUsuario(USUARIO_ID);
        request.setEscopo("TODOS");

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.selecionarPassageiros(SIMULACAO_ID, request));

        assertEquals(409, erro.getStatus());
    }

    @Test
    void bebeNaoDeveSerSelecionadoSemAdultoResponsavel() {
        when(aereoClient.carregarReserva(any())).thenReturn(
                respostaReserva(
                        passageiro("Maria", "ADT", "001", "ATIVO"),
                        passageiro("Bebe", "INF", "002", "ATIVO")));

        RemarcacaoRequest.SelecionarPassageiros request =
                new RemarcacaoRequest.SelecionarPassageiros();
        request.setCodgUsuario(USUARIO_ID);
        request.setEscopo("INDIVIDUAL");
        request.setPassageiroIndice(1);

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.selecionarPassageiros(SIMULACAO_ID, request));

        assertEquals(409, erro.getStatus());
    }

    @Test
    void deveRegistrarCartaoComoPreferenciaSemProcessarPagamento() throws Exception {
        prepararPrevia("437.17");

        RemarcacaoSimulacaoResponse response =
                service.selecionarFormaPagamento(SIMULACAO_ID, formaPagamento(2));

        assertTrue(response.isExigeFormaPagamento());
        assertTrue(response.isPermiteEncaminhar());
        assertEquals(2, response.getFormaPagamentoSelecionada().getCodigo());
        assertEquals("CARTAO", response.getFormaPagamentoSelecionada().getChave());
        assertEquals("PREFERENCIA_REGISTRADA",
                response.getFormaPagamentoSelecionada().getStatus());
        assertEquals(2, simulacao.getFormaPagamentoCodigo());
        assertEquals("PREFERENCIA_REGISTRADA", simulacao.getPagamentoStatus());
        assertNotNull(simulacao.getPagamentoSelecionadoEm());
        verify(chatService).registrarMensagemBot(
                eq(CONVERSA_ID),
                org.mockito.ArgumentMatchers.contains("Nenhuma cobranca"),
                org.mockito.ArgumentMatchers.contains("\"formaPagamentoSelecionada\""));
    }

    @Test
    void deveDisponibilizarFaturaQuandoLimiteConfirmadoCobreTotal() throws Exception {
        prepararPrevia("437.17");
        mockLimiteFaturado("500,00");

        RemarcacaoSimulacaoResponse response =
                service.selecionarFormaPagamento(SIMULACAO_ID, formaPagamento(1));

        assertEquals(1, response.getFormaPagamentoSelecionada().getCodigo());
        assertEquals("FATURA", response.getFormaPagamentoSelecionada().getChave());
        assertEquals("DISPONIVEL", response.getFormasPagamento().get(0).getStatus());
        assertTrue(response.getFormasPagamento().get(0).isDisponivel());
        verify(manager).post(
                eq("chat-confianca/persistencia/conversa-eventos"),
                any(ConversaEvento.class),
                eq(ConversaEvento.class));
    }

    @Test
    void naoDeveAceitarFaturaQuandoLimiteConfirmadoForInsuficiente() throws Exception {
        prepararPrevia("437.17");
        mockLimiteFaturado("100,00");

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.selecionarFormaPagamento(SIMULACAO_ID, formaPagamento(1)));

        assertEquals(409, erro.getStatus());
        assertNull(simulacao.getFormaPagamentoCodigo());
    }

    @Test
    void deveManterFaturaSelecionavelQuandoConsultaNaoPuderConfirmarLimite() throws Exception {
        prepararPrevia("437.17");
        when(limitesService.checkLimiteApi(any())).thenReturn(new StatusResponse(1, "Indisponivel"));

        RemarcacaoSimulacaoResponse response =
                service.selecionarFormaPagamento(SIMULACAO_ID, formaPagamento(1));

        assertEquals("SUJEITA_VALIDACAO", response.getFormasPagamento().get(0).getStatus());
        assertEquals(1, response.getFormaPagamentoSelecionada().getCodigo());
        assertEquals("PREFERENCIA_REGISTRADA_SUJEITA_VALIDACAO",
                response.getFormaPagamentoSelecionada().getStatus());
        assertEquals("PREFERENCIA_REGISTRADA_SUJEITA_VALIDACAO",
                simulacao.getPagamentoStatus());
        assertTrue(response.isPermiteEncaminhar());
    }

    @Test
    void deveTratarFalhaTecnicaNaConsultaComoSujeitaValidacao() throws Exception {
        prepararPrevia("437.17");
        when(limitesService.checkLimiteApi(any())).thenReturn(new StatusResponse(0, "OK"));
        Disponibilidade indisponibilidadeTecnica = new Disponibilidade(List.of());
        indisponibilidadeTecnica.setConsultaConfirmada(false);
        indisponibilidadeTecnica.setMensagemConsulta("Servico indisponivel");
        when(limitesService.consultaLimiteApi(any())).thenReturn(indisponibilidadeTecnica);

        RemarcacaoSimulacaoResponse response =
                service.selecionarFormaPagamento(SIMULACAO_ID, formaPagamento(1));

        assertEquals("SUJEITA_VALIDACAO", response.getFormasPagamento().get(0).getStatus());
        assertTrue(response.getFormasPagamento().get(0).isDisponivel());
    }

    @Test
    void deveIndisponibilizarFaturaQuandoConsultaConfirmadaNaoRetornarFaturado() throws Exception {
        prepararPrevia("437.17");
        when(limitesService.checkLimiteApi(any())).thenReturn(new StatusResponse(0, "OK"));
        Disponibilidade semFaturado = new Disponibilidade(List.of());
        semFaturado.setConsultaConfirmada(true);
        when(limitesService.consultaLimiteApi(any())).thenReturn(semFaturado);

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.selecionarFormaPagamento(SIMULACAO_ID, formaPagamento(1)));

        assertEquals(409, erro.getStatus());
        assertNull(simulacao.getFormaPagamentoCodigo());
    }

    @Test
    void naoDeveEncaminharDiferencaPositivaSemPreferenciaPagamento() throws Exception {
        prepararPrevia("80.00");

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.encaminhar(SIMULACAO_ID, encaminhar()));

        assertEquals(409, erro.getStatus());
        verify(chatService, never())
                .encaminharConversaParaDepartamentoRemarcacao(any(), any(), anyString());
    }

    @Test
    void deveEncaminharTotalZeroSemSolicitarFormaPagamento() throws Exception {
        prepararPrevia("0.00");

        RemarcacaoSimulacaoResponse response =
                service.encaminhar(SIMULACAO_ID, encaminhar());

        assertEquals("ENCAMINHADO", response.getStatus());
        assertFalse(response.isExigeFormaPagamento());
        assertNull(response.getFormaPagamentoSelecionada());
        verify(chatService).encaminharConversaParaDepartamentoRemarcacao(
                eq(CONVERSA_ID), eq(USUARIO_ID), anyString());
    }

    @Test
    void deveEnviarHandoffV2ComPreviaEPreferencia() throws Exception {
        prepararPrevia("80.00");
        simulacao.setTrechoOriginalJson(
                "{\"origem\":{\"codigoIata\":\"CGB\"},"
                        + "\"destino\":{\"codigoIata\":\"BSB\"},"
                        + "\"companhia\":{\"codigoIata\":\"G3\"},"
                        + "\"voos\":[{\"numeroVoo\":\"1234\",\"dataPartida\":1893456000000,"
                        + "\"horaPartida\":\"10:30\",\"dataChegada\":1893466800000,"
                        + "\"horaChegada\":\"13:30\"}]}");
        service.selecionarFormaPagamento(SIMULACAO_ID, formaPagamento(2));

        RemarcacaoSimulacaoResponse response =
                service.encaminhar(SIMULACAO_ID, encaminhar());

        assertEquals("ENCAMINHADO", response.getStatus());
        assertNotNull(response.getPrevia());
        assertEquals(2, response.getFormaPagamentoSelecionada().getCodigo());
        assertEquals(1, response.getTrechos().size());
        assertTrue(response.getTrechos().get(0).isSelecionado());
        assertEquals("CGB", response.getTrechos().get(0).getOrigem());
        assertEquals("1234", response.getTrechos().get(0).getNumeroVoos());
        verify(chatService).registrarMensagemSistema(
                eq(CONVERSA_ID),
                org.mockito.ArgumentMatchers.contains("Pagamento preferido Cartao"),
                org.mockito.ArgumentMatchers.argThat(json ->
                        json.contains("\"handoffSchema\":\"chat.reschedule.handoff.v2\"")
                                && json.contains("\"status\":\"ENCAMINHADO\"")));
    }

    @Test
    void deveManterSimulacaoRetomavelQuandoFalharRegistroDoHandoff() throws Exception {
        prepararPrevia("0.00");
        when(chatService.registrarMensagemSistema(any(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Falha ao registrar handoff"))
                .thenReturn(null);

        assertThrows(
                RuntimeException.class,
                () -> service.encaminhar(SIMULACAO_ID, encaminhar()));

        assertEquals("PREVIA_DISPONIVEL", simulacao.getStatus());

        RemarcacaoSimulacaoResponse response =
                service.encaminhar(SIMULACAO_ID, encaminhar());

        assertEquals("ENCAMINHADO", response.getStatus());
        assertEquals("ENCAMINHADO", simulacao.getStatus());
    }

    @Test
    void deveTransportarMotivoDeInelegibilidadeNoHandoff() {
        simulacao.setStatus("NAO_ELEGIVEL");
        simulacao.setMotivoBloqueio("Bilhete nao contempla o trecho selecionado.");

        RemarcacaoSimulacaoResponse response =
                service.encaminhar(SIMULACAO_ID, encaminhar());

        assertEquals("Bilhete nao contempla o trecho selecionado.",
                response.getMotivoBloqueio());
        verify(chatService).registrarMensagemSistema(
                eq(CONVERSA_ID),
                anyString(),
                org.mockito.ArgumentMatchers.contains(
                        "\"motivoBloqueio\":\"Bilhete nao contempla o trecho selecionado.\""));
    }

    @Test
    void falhaNoEventoPosHandoffNaoDeveInvalidarEncaminhamento() throws Exception {
        prepararPrevia("0.00");
        when(manager.post(
                eq("chat-confianca/persistencia/conversa-eventos"),
                any(ConversaEvento.class),
                eq(ConversaEvento.class)))
                .thenThrow(new RuntimeException("Falha no evento"));

        RemarcacaoSimulacaoResponse response =
                service.encaminhar(SIMULACAO_ID, encaminhar());

        assertEquals("ENCAMINHADO", response.getStatus());
        assertEquals("ENCAMINHADO", simulacao.getStatus());
    }

    @Test
    void naoDeveAceitarPreferenciaComPreviaExpirada() throws Exception {
        prepararPrevia("80.00");
        RemarcacaoSimulacaoResponse.Previa previa =
                mapper.readValue(simulacao.getCalculoJson(), RemarcacaoSimulacaoResponse.Previa.class);
        previa.setValidoAte(LocalDateTime.now().minusMinutes(1));
        simulacao.setCalculoJson(mapper.writeValueAsString(previa));

        RegraDeNegocioException erro = assertThrows(
                RegraDeNegocioException.class,
                () -> service.selecionarFormaPagamento(SIMULACAO_ID, formaPagamento(2)));

        assertEquals(409, erro.getStatus());
    }

    private RemarcacaoRequest.Iniciar iniciar(Integer reservaId, String localizador) {
        RemarcacaoRequest.Iniciar request = new RemarcacaoRequest.Iniciar();
        request.setConversaId(CONVERSA_ID);
        request.setCodgUsuario(USUARIO_ID);
        request.setReservaId(reservaId);
        request.setLocalizador(localizador);
        request.setCodgAgenciaSessao(999);
        return request;
    }

    private ReservasEmitidasRemarcacaoResponse respostaSelecao(
            ReservasEmitidasRemarcacaoResponse.Item... items) {
        ReservasEmitidasRemarcacaoResponse response = new ReservasEmitidasRemarcacaoResponse();
        response.setItems(List.of(items));
        response.setPage(0);
        response.setSize(50);
        response.setTotalElements((long) items.length);
        response.setTotalPages(items.length == 0 ? 0 : 1);
        response.setHasNext(false);
        return response;
    }

    private ReservasEmitidasRemarcacaoResponse.Item reservaEmitida(
            int reservaId,
            String localizador,
            String companhia,
            String sistema) {
        ReservasEmitidasRemarcacaoResponse.Item item = new ReservasEmitidasRemarcacaoResponse.Item();
        item.setReservaId(reservaId);
        item.setLocalizador(localizador);
        item.setStatus(3);
        item.setDataEmissao(LocalDateTime.now().minusDays(1));
        item.setCompanhiaIata(companhia);
        item.setSistema(sistema);
        item.setQuantidadeBilhetesAtivos(1);
        item.setDisponivelSimulacao(true);
        return item;
    }

    private Reserva reservaHub(String localizador, String sistema, String companhia) {
        Reserva reserva = new Reserva();
        reserva.setLocalizador(localizador);
        reserva.setSistema(sistema);
        TrechoReserva trecho = new TrechoReserva();
        trecho.setCompanhia(new com.confApi.hub.aereo.dto.Companhia(1, companhia, companhia));
        trecho.setVoos(List.of());
        reserva.setViagens(List.of(trecho));
        return reserva;
    }

    private void prepararPrevia(String total) throws Exception {
        RemarcacaoSimulacaoResponse.Previa previa = new RemarcacaoSimulacaoResponse.Previa();
        previa.setTotalEstimado(new BigDecimal(total));
        previa.setTotalSelecionado(new BigDecimal(total));
        previa.setCalculoCompleto(true);
        previa.setValidoAte(LocalDateTime.now().plusMinutes(10));
        simulacao.setCalculoJson(mapper.writeValueAsString(previa));
        simulacao.setStatus("PREVIA_DISPONIVEL");
    }

    private RemarcacaoRequest.SelecionarFormaPagamento formaPagamento(int codigo) {
        RemarcacaoRequest.SelecionarFormaPagamento request =
                new RemarcacaoRequest.SelecionarFormaPagamento();
        request.setCodgUsuario(USUARIO_ID);
        request.setCodigo(codigo);
        return request;
    }

    private RemarcacaoRequest.Encaminhar encaminhar() {
        RemarcacaoRequest.Encaminhar request = new RemarcacaoRequest.Encaminhar();
        request.setCodgUsuario(USUARIO_ID);
        return request;
    }

    private void mockLimiteFaturado(String totalDisponivel) {
        LimiteCredito limite = new LimiteCredito();
        limite.setTipoLimite(TipoLimite.FATURADO);
        limite.setTotalDisponivel(totalDisponivel);
        Disponibilidade disponibilidade = new Disponibilidade(List.of(limite));
        disponibilidade.setConsultaConfirmada(true);
        when(limitesService.checkLimiteApi(any())).thenReturn(new StatusResponse(0, "OK"));
        when(limitesService.consultaLimiteApi(any()))
                .thenReturn(disponibilidade);
    }

    private ConsultarLocalizadorResponse respostaReserva(Passageiro... passageiros) {
        Reserva reserva = new Reserva();
        reserva.setLocalizador("ABC123");
        reserva.setPassageiros(List.of(passageiros));

        TrechoReserva trecho = new TrechoReserva();
        trecho.setCompanhia(new com.confApi.hub.aereo.dto.Companhia(1, "G3", "GOL"));
        trecho.setOrigem(new com.confApi.hub.aereo.dto.Aeroporto("CGB", "Cuiaba"));
        trecho.setDestino(new com.confApi.hub.aereo.dto.Aeroporto("BSB", "Brasilia"));

        Voo voo = new Voo();
        voo.setCiaMandatoria(new com.confApi.hub.aereo.dto.Companhia(1, "G3", "GOL"));
        voo.setNumeroVoo("1234");
        voo.setOrigem(new com.confApi.hub.aereo.dto.Aeroporto("CGB", "Cuiaba"));
        voo.setDestino(new com.confApi.hub.aereo.dto.Aeroporto("BSB", "Brasilia"));
        voo.setDataPartida(new java.util.Date());
        voo.setHoraPartida("10:30");
        voo.setDataChegada(new java.util.Date());
        voo.setHoraChegada("12:00");
        trecho.setVoos(List.of(voo));

        reserva.setViagens(List.of(trecho));
        ConsultarLocalizadorResponse response = new ConsultarLocalizadorResponse();
        response.setReservas(List.of(reserva));
        return response;
    }

    private Passageiro passageiro(String nome, String tipo, String bilheteNumero, String status) {
        Bilhete bilhete = new Bilhete();
        bilhete.setNumero(bilheteNumero);
        bilhete.setStatus(status);
        Passageiro passageiro = new Passageiro();
        passageiro.setNome(nome);
        passageiro.setFaixaEtaria(tipo);
        passageiro.setBilhetes(List.of(bilhete));
        return passageiro;
    }
}
