package com.confApi.chatconfianca.service;

import com.confApi.aereo.AereoClient;
import com.confApi.aereo.dto.ConsultarLocalizadorResponse;
import com.confApi.aereo.dto.Reserva;
import com.confApi.chatconfianca.client.ChatConfiancaManagerClient;
import com.confApi.chatconfianca.dto.model.Conversa;
import com.confApi.chatconfianca.dto.model.ConversaEvento;
import com.confApi.chatconfianca.dto.model.RefAgencia;
import com.confApi.chatconfianca.dto.model.SimulacaoRemarcacao;
import com.confApi.chatconfianca.dto.remarcacao.RemarcacaoRequest;
import com.confApi.chatconfianca.dto.remarcacao.RemarcacaoSimulacaoResponse;
import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.db.confManager.aeroporto.AeroportoService;
import com.confApi.db.confManager.regraAereaAlteracao.RegraAereaAlteracaoManagerService;
import com.confApi.exception.RegraDeNegocioException;
import com.confApi.hub.aereo.dto.Bilhete;
import com.confApi.hub.aereo.dto.Passageiro;
import com.confApi.hub.aereo.dto.TrechoReserva;
import com.confApi.hub.enumerador.TipoLimite;
import com.confApi.hub.limites.LimitesService;
import com.confApi.hub.limites.dto.Disponibilidade;
import com.confApi.hub.limites.dto.LimiteCredito;
import com.confApi.hub.limites.dto.StatusResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
class ChatConfiancaRemarcacaoServiceTest {
    private static final Long SIMULACAO_ID = 70L;
    private static final Long CONVERSA_ID = 20L;
    private static final Integer USUARIO_ID = 101;

    private final ChatConfiancaManagerClient manager = mock(ChatConfiancaManagerClient.class);
    private final ChatConfiancaService chatService = mock(ChatConfiancaService.class);
    private final AereoClient aereoClient = mock(AereoClient.class);
    private final AeroportoService aeroportoService = mock(AeroportoService.class);
    private final RegraAereaAlteracaoManagerService regraService =
            mock(RegraAereaAlteracaoManagerService.class);
    private final LimitesService limitesService = mock(LimitesService.class);
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    private ChatConfiancaRemarcacaoService service;
    private SimulacaoRemarcacao simulacao;
    private SessaoChatResponse sessao;

    @BeforeEach
    void setUp() {
        service = new ChatConfiancaRemarcacaoService(
                manager,
                chatService,
                aereoClient,
                aeroportoService,
                regraService,
                limitesService,
                mapper);

        simulacao = new SimulacaoRemarcacao();
        simulacao.setId(SIMULACAO_ID);
        simulacao.setConversaId(CONVERSA_ID);
        simulacao.setCodgUsuario(USUARIO_ID);
        simulacao.setLocalizador("ABC123");
        simulacao.setTrechoIndice(0);
        simulacao.setStatus("AGUARDANDO_PASSAGEIROS");
        simulacao.setExpiraEm(LocalDateTime.now().plusMinutes(20));

        Conversa conversa = new Conversa();
        conversa.setId(CONVERSA_ID);
        conversa.setSolicitanteCodgUsuario(USUARIO_ID);
        when(chatService.buscarConversa(CONVERSA_ID)).thenReturn(conversa);
        sessao = new SessaoChatResponse();
        RefAgencia agencia = new RefAgencia();
        agencia.setCodgSistemaBackoffice("987");
        sessao.setAgencia(agencia);
        when(chatService.montarSessao(USUARIO_ID, null)).thenReturn(sessao);
        when(manager.get(anyString(), any(Class.class))).thenReturn(simulacao);
        when(manager.post(anyString(), any(), any(Class.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
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
        reserva.setViagens(List.of(new TrechoReserva()));
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
