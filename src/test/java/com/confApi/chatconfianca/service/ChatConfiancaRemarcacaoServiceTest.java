package com.confApi.chatconfianca.service;

import com.confApi.aereo.AereoClient;
import com.confApi.aereo.dto.ConsultarLocalizadorRequest;
import com.confApi.aereo.dto.ConsultarLocalizadorResponse;
import com.confApi.aereo.dto.FamiliaPreco;
import com.confApi.aereo.dto.PesquisaRequestDTO;
import com.confApi.aereo.dto.PesquisaResponse;
import com.confApi.aereo.dto.Preco;
import com.confApi.aereo.dto.PrecoTipo;
import com.confApi.aereo.dto.Reserva;
import com.confApi.aereo.dto.TarifarRequest;
import com.confApi.aereo.dto.TarifarResponse;
import com.confApi.aereo.dto.Trecho;
import com.confApi.aereo.dto.ValorBase;
import com.confApi.aereo.dto.ValorPassageiro;
import com.confApi.aereo.dto.ValorReserva;
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
import com.confApi.db.confManager.regraAereaAlteracao.dto.RegraAereaAlteracaoCalculoResponse;
import com.confApi.db.confManager.regraAereaAlteracao.dto.RegraAereaAlteracaoConsultaRequest;
import com.confApi.db.confManager.regraAereaAlteracao.dto.RegraAereaAlteracaoConsultaResponse;
import com.confApi.db.confManager.regraAereaAlteracao.dto.RegraAereaAlteracaoRegraResponse;
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
    void deveTratarLaEJjComoMesmaCompanhiaAoValidarCodeshare() {
        prepararInicioLatam("LA", "JJ", "LA", false);

        RemarcacaoSimulacaoResponse response = service.iniciar(iniciar(501, "ABC123"));

        assertEquals("AGUARDANDO_CRITERIOS", response.getStatus());
        assertNull(response.getMotivoBloqueio());
        verify(regraService).simular(any());
    }

    @Test
    void deveManterBloqueioDeCodeshareComOperadoraDeOutraCompanhia() {
        prepararInicioLatam("LA", "LA", "AA", false);

        RemarcacaoSimulacaoResponse response = service.iniciar(iniciar(501, "ABC123"));

        assertEquals("NAO_ELEGIVEL", response.getStatus());
        assertEquals(
                "Nao encontrei trecho futuro nacional, ativo e sem codeshare que possa ser simulado automaticamente.",
                response.getMotivoBloqueio());
        verify(regraService, never()).simular(any());
    }

    @Test
    void deveManterBloqueioQuandoHubMarcaCodeshareExplicitamente() {
        prepararInicioLatam("LA", "LA", "LA", true);

        RemarcacaoSimulacaoResponse response = service.iniciar(iniciar(501, "ABC123"));

        assertEquals("NAO_ELEGIVEL", response.getStatus());
        verify(regraService, never()).simular(any());
    }

    @Test
    void devePesquisarLatamComoLaEAceitarDisponibilidadeLaParaSimulacaoJj() throws Exception {
        simulacao.setStatus("AGUARDANDO_CRITERIOS");
        simulacao.setOrigem("CGB");
        simulacao.setDestino("BSB");
        simulacao.setCompanhiaIata("JJ");
        simulacao.setPassageirosJson(
                "{\"escopo\":\"INDIVIDUAL\",\"indices\":[0],\"passageiros\":[]}");
        when(aereoClient.carregarReserva(any())).thenReturn(
                reservaLatamElegivel("JJ", "LA", "LA", false));

        Trecho opcao = opcaoPesquisa("LATAM-ID", "CGB", "BSB", "3895");
        opcao.setCompanhia(new com.confApi.hub.aereo.dto.Companhia(1, "LA", "LATAM"));
        opcao.getVoos().get(0).setCiaMandatoria(
                new com.confApi.hub.aereo.dto.Companhia(1, "LA", "LATAM"));
        PesquisaResponse disponibilidade = new PesquisaResponse();
        disponibilidade.setTrechos1(List.of(opcao));
        when(aereoClient.pesquisarDisponibilidade(any())).thenReturn(List.of(disponibilidade));

        RemarcacaoRequest.Pesquisar request = new RemarcacaoRequest.Pesquisar();
        request.setCodgUsuario(USUARIO_ID);
        request.setData(LocalDate.now().plusDays(10));
        request.setPeriodo("QUALQUER");
        request.setSomenteDireto(false);

        RemarcacaoSimulacaoResponse response = service.pesquisar(SIMULACAO_ID, request);

        assertEquals("AGUARDANDO_OPCAO", response.getStatus());
        assertEquals(1, response.getOpcoes().size());
        ArgumentCaptor<PesquisaRequestDTO> pesquisa =
                ArgumentCaptor.forClass(PesquisaRequestDTO.class);
        verify(aereoClient).pesquisarDisponibilidade(pesquisa.capture());
        assertEquals("LA", pesquisa.getValue().getCompanhias().get(0).getCodigoIata());
    }

    @Test
    void deveExibirSomenteFamiliasComTarifaIgualOuMaiorQueOriginal() throws Exception {
        Trecho opcao = opcaoPesquisa("LATAM-ID", "CGB", "BSB", "3895");
        opcao.setFamilias(List.of(
                familiaPesquisa("Menor", 250.0, 400.0),
                familiaPesquisa("Igual", 300.0, 360.0),
                familiaPesquisa("Maior", 350.0, 410.0)));
        prepararPesquisaLatamComTarifaMinima(
                opcao, "IGUAL_OU_MAIOR", 300.0);

        RemarcacaoSimulacaoResponse response = service.pesquisar(
                SIMULACAO_ID, pesquisaRemarcacao());

        assertEquals("AGUARDANDO_OPCAO", response.getStatus());
        assertEquals(1, response.getOpcoes().size());
        assertEquals(List.of("Igual", "Maior"), response.getOpcoes().get(0).getFamilias()
                .stream().map(RemarcacaoSimulacaoResponse.Familia::getNome).toList());
    }

    @Test
    void deveManterTarifaMenorQuandoConfiguracaoPermite() throws Exception {
        Trecho opcao = opcaoPesquisa("LATAM-ID", "CGB", "BSB", "3895");
        opcao.setFamilias(List.of(familiaPesquisa("Menor", 250.0, 400.0)));
        prepararPesquisaLatamComTarifaMinima(
                opcao, "PODE_SER_MENOR", 300.0);

        RemarcacaoSimulacaoResponse response = service.pesquisar(
                SIMULACAO_ID, pesquisaRemarcacao());

        assertEquals("AGUARDANDO_OPCAO", response.getStatus());
        assertEquals("Menor", response.getOpcoes().get(0).getFamilias().get(0).getNome());
    }

    @Test
    void deveBloquearQuandoRetarifacaoFinalFicaAbaixoDaTarifaOriginal() throws Exception {
        Trecho opcao = opcaoPesquisa("LATAM-ID", "CGB", "BSB", "3895");
        opcao.setFamilias(List.of(familiaPesquisa("Igual", 300.0, 360.0)));
        prepararPesquisaLatamComTarifaMinima(
                opcao, "IGUAL_OU_MAIOR", 300.0);
        simulacao.setStatus("AGUARDANDO_OPCAO");
        simulacao.setResultadosJson(mapper.writeValueAsString(List.of(opcao)));

        TarifarResponse tarifa = new TarifarResponse();
        Preco preco = new Preco();
        preco.setMoeda("BRL");
        PrecoTipo adulto = new PrecoTipo();
        adulto.setValorTarifa(250.0);
        adulto.setValorTaxaEmbarque(60.0);
        preco.setPrecoAdulto(adulto);
        tarifa.setPreco(preco);
        when(aereoClient.tarifar(any())).thenReturn(tarifa);
        when(regraService.simular(any())).thenReturn(
                regraPermitida(false, "IGUAL_OU_MAIOR"));

        RemarcacaoSimulacaoResponse response = service.simular(
                SIMULACAO_ID, simular(0, 0));

        assertEquals("NAO_ELEGIVEL", response.getStatus());
        assertTrue(response.getMotivoBloqueio().contains(
                "tarifa igual ou superior a tarifa original"));
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
    void deveOferecerIdaVoltaSomenteParaParInversoElegivelComCronologiaConhecida() {
        Reserva reserva = prepararReservaIdaVolta();
        simulacao.setStatus("AGUARDANDO_TRECHO");
        simulacao.setVersao(8);
        RemarcacaoSimulacaoResponse response = service.consultar(SIMULACAO_ID, USUARIO_ID);
        assertTrue(response.isPermiteSelecionarIdaVolta());
        assertEquals(List.of(0, 1), response.getIndicesTrechosIdaVolta());
        assertEquals(8, response.getVersao());

        reserva.getViagens().get(1).setDestino(new com.confApi.hub.aereo.dto.Aeroporto("GRU", "GRU"));
        assertFalse(service.consultar(SIMULACAO_ID, USUARIO_ID).isPermiteSelecionarIdaVolta());
        reserva.getViagens().get(1).setDestino(new com.confApi.hub.aereo.dto.Aeroporto("CGB", "CGB"));
        reserva.getViagens().get(1).getVoos().get(0).setDataPartida(null);
        assertFalse(service.consultar(SIMULACAO_ID, USUARIO_ID).isPermiteSelecionarIdaVolta());
    }

    @Test
    void deveSelecionarIdaVoltaOpcionalEOrdenarOsIndicesAntesDosPassageiros() {
        prepararReservaIdaVolta();
        when(regraService.simular(any())).thenReturn(regraPermitida(false));
        RemarcacaoRequest.SelecionarTrecho request = selecionarIdaVolta(List.of(1, 0));
        RemarcacaoSimulacaoResponse response = service.selecionarTrecho(SIMULACAO_ID, request);
        assertEquals("AGUARDANDO_CRITERIOS", response.getStatus());
        assertTrue(response.isRemarcacaoConjunta());
        assertFalse(response.isRemarcacaoConjuntaObrigatoria());
        assertEquals(1, response.getOrdemTrechoAtual());
        assertEquals(2, response.getQuantidadeTrechos());
        assertEquals("[0,1]", simulacao.getTrechosIndicesJson());
        assertEquals(0, simulacao.getTrechoIndice());
        assertTrue(response.getTrechosSelecionados().isEmpty());
        verify(aereoClient, never()).tarifar(any());
    }

    @Test
    void deveUsarCronologiaMesmoQuandoReservaTrazVoltaAntesDaIda() {
        Reserva reserva = prepararReservaIdaVolta();
        reserva.setViagens(List.of(reserva.getViagens().get(1), reserva.getViagens().get(0)));
        simulacao.setStatus("AGUARDANDO_TRECHO");
        assertEquals(List.of(1, 0), service.consultar(SIMULACAO_ID, USUARIO_ID).getIndicesTrechosIdaVolta());
        when(regraService.simular(any())).thenReturn(regraPermitida(false));
        RemarcacaoSimulacaoResponse response = service.selecionarTrecho(
                SIMULACAO_ID, selecionarIdaVolta(List.of(0, 1)));
        assertEquals("[1,0]", simulacao.getTrechosIndicesJson());
        assertEquals(1, simulacao.getTrechoIndice());
        assertEquals("CGB", response.getCriterios().getOrigem());
    }

    @Test
    void deveRejeitarIndicesRepetidosAusentesOuForaDaReserva() {
        prepararReservaIdaVolta();
        for (List<Integer> indices : List.of(List.<Integer>of(), List.of(0), List.of(0, 0),
                List.of(0, 2), List.of(-1, 0), java.util.Arrays.asList(0, null))) {
            assertThrows(RegraDeNegocioException.class,
                    () -> service.selecionarTrecho(SIMULACAO_ID, selecionarIdaVolta(indices)));
        }
        assertNull(simulacao.getTrechosIndicesJson());
        verify(regraService, never()).simular(any());
    }

    @Test
    void deveRejeitarIdaVoltaDeCompanhiasDiferentes() {
        Reserva reserva = prepararReservaIdaVolta();
        reserva.getViagens().get(1).setCompanhia(new com.confApi.hub.aereo.dto.Companhia(2, "AD", "AZUL"));
        assertThrows(RegraDeNegocioException.class,
                () -> service.selecionarTrecho(SIMULACAO_ID, selecionarIdaVolta(List.of(0, 1))));
        verify(regraService, never()).simular(any());
    }

    @Test
    void deveExigirRegraHomologadaCompativelNosDoisTrechos() {
        prepararReservaIdaVolta();
        RegraAereaAlteracaoConsultaResponse outraRegra = regraPermitida(false);
        outraRegra.getRegra().setId(11L);
        when(regraService.simular(any())).thenReturn(regraPermitida(false), outraRegra);
        RemarcacaoSimulacaoResponse response = service.selecionarTrecho(
                SIMULACAO_ID, selecionarIdaVolta(List.of(0, 1)));
        assertEquals("NAO_ELEGIVEL", response.getStatus());
        assertTrue(response.getMensagem().contains("regras diferentes"));
        verify(aereoClient, never()).tarifar(any());
    }

    @Test
    void devePreservarExigenciaConjuntaAoEscolherSomenteVolta() {
        prepararReservaIdaVolta();
        when(regraService.simular(any())).thenReturn(regraPermitida(true));
        RemarcacaoRequest.SelecionarTrecho request = new RemarcacaoRequest.SelecionarTrecho();
        request.setCodgUsuario(USUARIO_ID);
        request.setTrechoIndice(1);
        RemarcacaoSimulacaoResponse response = service.selecionarTrecho(SIMULACAO_ID, request);
        assertTrue(response.isRemarcacaoConjuntaObrigatoria());
        assertEquals("[0,1]", simulacao.getTrechosIndicesJson());
        assertEquals(0, simulacao.getTrechoIndice());
    }

    @Test
    void deveAtivarFluxoConjuntoPelaConfiguracaoDaRegra() {
        simulacao.setStatus("AGUARDANDO_TRECHO");
        Passageiro passageiro = passageiro("Maria", "ADT", "001", "ATIVO");
        Reserva reserva = new Reserva();
        reserva.setLocalizador("ABC123");
        reserva.setPassageiros(List.of(passageiro));
        reserva.setViagens(List.of(
                trechoReserva("CGB", "BSB", "1234"),
                trechoReserva("BSB", "CGB", "5678")));
        ConsultarLocalizadorResponse hub = new ConsultarLocalizadorResponse();
        hub.setReservas(List.of(reserva));
        when(aereoClient.carregarReserva(any())).thenReturn(hub);
        when(aeroportoService.findIatasAeroportosNacionais())
                .thenReturn(java.util.Set.of("CGB", "BSB"));
        when(regraService.simular(any())).thenReturn(regraPermitida(true));

        RemarcacaoRequest.SelecionarTrecho request = new RemarcacaoRequest.SelecionarTrecho();
        request.setCodgUsuario(USUARIO_ID);
        request.setTrechoIndice(0);
        RemarcacaoSimulacaoResponse response =
                service.selecionarTrecho(SIMULACAO_ID, request);

        assertTrue(response.isRemarcacaoConjunta());
        assertTrue(response.isRemarcacaoConjuntaObrigatoria());
        assertEquals(2, response.getQuantidadeTrechos());
        assertEquals("[0,1]", simulacao.getTrechosIndicesJson());
        assertTrue(simulacao.getTrechosOriginaisJson().contains("5678"));
    }

    @Test
    void deveExigirSelecaoDaVoltaAntesDeTarifarRemarcacaoConjunta() throws Exception {
        prepararSimulacaoConjunta();

        RemarcacaoSimulacaoResponse response =
                service.simular(SIMULACAO_ID, simular(0, 0));

        assertEquals("AGUARDANDO_CRITERIOS", response.getStatus());
        assertTrue(response.isRemarcacaoConjunta());
        assertEquals(2, response.getQuantidadeTrechos());
        assertEquals(2, response.getOrdemTrechoAtual());
        assertEquals(Integer.valueOf(1), simulacao.getTrechoIndice());
        assertTrue(simulacao.getOfertaSelecionadaJson().contains("\"trechoIndice\":0"));
        assertEquals(1, response.getTrechosSelecionados().size());
        assertEquals(0, response.getTrechosSelecionados().get(0).getTrechoIndice());
        assertEquals(LocalDate.now().plusDays(30), response.getCriterios().getDataMinima());
        verify(aereoClient, never()).tarifar(any());
    }

    @Test
    void devePreservarIdaSelecionadaAoPesquisarEConsultarVolta() throws Exception {
        prepararSimulacaoConjunta();
        service.simular(SIMULACAO_ID, simular(0, 0));
        String ofertaIda = simulacao.getOfertaSelecionadaJson();
        PesquisaResponse pesquisa = new PesquisaResponse();
        pesquisa.setTrechos1(List.of(opcaoPesquisa("VOLTA-ID", "BSB", "CGB", "5678")));
        when(aereoClient.pesquisarDisponibilidade(any())).thenReturn(List.of(pesquisa));
        RemarcacaoRequest.Pesquisar request = pesquisaRemarcacao();
        request.setData(LocalDate.now().plusDays(33));

        RemarcacaoSimulacaoResponse response = service.pesquisar(SIMULACAO_ID, request);
        assertEquals("AGUARDANDO_OPCAO", response.getStatus());
        assertEquals(ofertaIda, simulacao.getOfertaSelecionadaJson());
        assertEquals(1, response.getTrechosSelecionados().size());
        assertEquals("CGB", response.getTrechosSelecionados().get(0).getVoo().getOrigem());
        assertEquals(1, service.consultar(SIMULACAO_ID, USUARIO_ID).getTrechosSelecionados().size());
        verify(aereoClient, never()).tarifar(any());
    }

    @Test
    void deveRejeitarDataDaVoltaAnteriorAChegadaDaIdaSemApagarSelecao() throws Exception {
        prepararSimulacaoConjunta();
        service.simular(SIMULACAO_ID, simular(0, 0));
        String ofertaIda = simulacao.getOfertaSelecionadaJson();
        RemarcacaoRequest.Pesquisar request = pesquisaRemarcacao();
        request.setData(LocalDate.now().plusDays(29));
        assertThrows(RegraDeNegocioException.class, () -> service.pesquisar(SIMULACAO_ID, request));
        assertEquals(ofertaIda, simulacao.getOfertaSelecionadaJson());
        verify(aereoClient, never()).pesquisarDisponibilidade(any());
    }

    @Test
    void deveValidarHorarioDaVoltaNoMesmoDiaAntesDeTarifar() throws Exception {
        prepararSimulacaoConjunta();
        service.simular(SIMULACAO_ID, simular(0, 0));
        Trecho volta = opcaoPesquisa("VOLTA-ID", "BSB", "CGB", "5678");
        Voo voo = volta.getVoos().get(0);
        definirHorario(voo, LocalDate.now().plusDays(30), "12:30", "14:30");
        simulacao.setResultadosJson(mapper.writeValueAsString(List.of(volta)));
        String ofertaIda = simulacao.getOfertaSelecionadaJson();
        assertThrows(RegraDeNegocioException.class, () -> service.simular(SIMULACAO_ID, simular(0, 0)));
        assertEquals(ofertaIda, simulacao.getOfertaSelecionadaJson());
        verify(aereoClient, never()).tarifar(any());

        definirHorario(voo, LocalDate.now().plusDays(30), "13:30", "15:30");
        simulacao.setResultadosJson(mapper.writeValueAsString(List.of(volta)));
        prepararTarifacaoPermitida();
        assertEquals("PREVIA_DISPONIVEL", service.simular(SIMULACAO_ID, simular(0, 0)).getStatus());
    }

    @Test
    void deveAceitarHorarioLocalDeChegadaIgualAPartidaEmAeroportosComFusosDistintos() throws Exception {
        Reserva reserva = prepararReservaIdaVolta();
        reserva.getViagens().get(0).getVoos().get(0).setHoraChegada("10:30");
        simulacao.setStatus("AGUARDANDO_TRECHO");
        assertTrue(service.consultar(SIMULACAO_ID, USUARIO_ID).isPermiteSelecionarIdaVolta());

        prepararSimulacaoConjunta();
        Trecho ida = opcaoPesquisa("IDA-ID", "CGB", "BSB", "1234");
        ida.getVoos().get(0).setHoraChegada("10:30");
        simulacao.setResultadosJson(mapper.writeValueAsString(List.of(ida)));
        assertEquals("AGUARDANDO_CRITERIOS", service.simular(SIMULACAO_ID, simular(0, 0)).getStatus());
        simulacao.setResultadosJson(mapper.writeValueAsString(
                List.of(opcaoPesquisa("VOLTA-ID", "BSB", "CGB", "5678"))));
        prepararTarifacaoPermitida();
        assertEquals("PREVIA_DISPONIVEL", service.simular(SIMULACAO_ID, simular(0, 0)).getStatus());
    }

    @Test
    void deveNaoFiltrarTarifaMinimaIndividualNoFluxoConjunto() throws Exception {
        Trecho opcao = opcaoPesquisa("IDA-ID", "CGB", "BSB", "1234");
        opcao.setFamilias(List.of(familiaPesquisa("Light", 250.0, 300.0)));
        prepararPesquisaLatamComTarifaMinima(opcao, "IGUAL_OU_MAIOR", 500.0);
        simulacao.setTrechosIndicesJson("[0,1]");
        RemarcacaoSimulacaoResponse response = service.pesquisar(SIMULACAO_ID, pesquisaRemarcacao());
        assertEquals("AGUARDANDO_OPCAO", response.getStatus());
        assertEquals(1, response.getOpcoes().size());
        assertEquals(1, response.getOpcoes().get(0).getFamilias().size());
    }

    @Test
    void deveAplicarMinimoNoTotalTarifadoDaIdaEVolta() throws Exception {
        prepararSimulacaoConjunta();
        service.simular(SIMULACAO_ID, simular(0, 0));
        Reserva reserva = prepararReservaIdaVolta();
        definirTarifaOriginal(reserva, 500.0);
        simulacao.setResultadosJson(mapper.writeValueAsString(
                List.of(opcaoPesquisa("VOLTA-ID", "BSB", "CGB", "5678"))));
        prepararTarifacaoPermitida();
        when(regraService.simular(any())).thenReturn(regraPermitida(false, "IGUAL_OU_MAIOR"));

        RemarcacaoSimulacaoResponse response = service.simular(SIMULACAO_ID, simular(0, 0));
        assertEquals("NAO_ELEGIVEL", response.getStatus());
        assertTrue(response.getMensagem().contains("tarifa igual ou superior"));
        verify(aereoClient).tarifar(any());
    }

    @Test
    void deveCobrarUmUnicoTotalConsolidadoSemDuplicarMultaPorTrecho() throws Exception {
        prepararSimulacaoConjunta();
        service.simular(SIMULACAO_ID, simular(0, 0));
        simulacao.setResultadosJson(mapper.writeValueAsString(
                List.of(opcaoPesquisa("VOLTA-ID", "BSB", "CGB", "5678"))));
        prepararTarifacaoPermitida();
        RegraAereaAlteracaoConsultaResponse regra = regraPermitida(false);
        regra.getCalculo().setValorMulta(new BigDecimal("50.00"));
        regra.getCalculo().setDiferencaTarifaria(new BigDecimal("30.00"));
        regra.getCalculo().setTaxaServico(new BigDecimal("10.00"));
        regra.getCalculo().setTotalPrevisto(new BigDecimal("90.00"));
        when(regraService.simular(any())).thenReturn(regra);

        RemarcacaoSimulacaoResponse response = service.simular(SIMULACAO_ID, simular(0, 0));
        assertEquals("PREVIA_DISPONIVEL", response.getStatus());
        assertEquals(2, response.getPrevia().getTrechosSelecionados().size());
        assertEquals(new BigDecimal("50.00"), response.getPrevia().getMulta());
        assertEquals(new BigDecimal("90.00"), response.getPrevia().getTotalEstimado());
        assertEquals(response.getPrevia().getTotalEstimado(), response.getPrevia().getTotalSelecionado());
        verify(aereoClient).tarifar(any());
        verify(regraService).simular(any());
    }

    @Test
    void deveTarifarIdaEVoltaEAplicarRegraEmDoisTrechos() throws Exception {
        prepararSimulacaoConjunta();
        service.simular(SIMULACAO_ID, simular(0, 0));
        simulacao.setResultadosJson(mapper.writeValueAsString(
                List.of(opcaoPesquisa("VOLTA-ID", "BSB", "CGB", "5678"))));

        prepararTarifacaoPermitida();

        RemarcacaoSimulacaoResponse response =
                service.simular(SIMULACAO_ID, simular(0, 0));

        assertEquals("PREVIA_DISPONIVEL", response.getStatus());
        assertEquals(2, response.getPrevia().getTrechosSelecionados().size());
        assertEquals(List.of(0, 1), response.getTrechosSelecionados().stream()
                .map(RemarcacaoSimulacaoResponse.TrechoSelecionado::getTrechoIndice).toList());
        ArgumentCaptor<TarifarRequest> tarifaRequest = ArgumentCaptor.forClass(TarifarRequest.class);
        verify(aereoClient).tarifar(tarifaRequest.capture());
        assertEquals("IDA-ID", tarifaRequest.getValue().getIdentificacaoViagem());
        assertEquals("VOLTA-ID", tarifaRequest.getValue().getIdentificacaoDaViagemVolta());
        assertEquals(List.of(1, 2), tarifaRequest.getValue().getClasses().stream()
                .map(item -> item.getTrecho()).distinct().collect(java.util.stream.Collectors.toList()));

        ArgumentCaptor<RegraAereaAlteracaoConsultaRequest> regraRequest =
                ArgumentCaptor.forClass(RegraAereaAlteracaoConsultaRequest.class);
        verify(regraService).simular(regraRequest.capture());
        assertEquals(2, regraRequest.getValue().getQuantidadeTrechos());
        assertFalse(regraRequest.getValue().getFamiliaTarifariaAlterada());
    }

    @Test
    void deveInformarAoManagerQuandoAlgumTrechoMudaDeFamilia() throws Exception {
        prepararSimulacaoConjunta();
        service.simular(SIMULACAO_ID, simular(0, 0));
        simulacao.setResultadosJson(mapper.writeValueAsString(
                List.of(opcaoPesquisa(
                        "VOLTA-ID", "BSB", "CGB", "5678", "PLUS", "Plus"))));
        prepararTarifacaoPermitida();

        service.simular(SIMULACAO_ID, simular(0, 0));

        ArgumentCaptor<RegraAereaAlteracaoConsultaRequest> regraRequest =
                ArgumentCaptor.forClass(RegraAereaAlteracaoConsultaRequest.class);
        verify(regraService).simular(regraRequest.capture());
        assertTrue(regraRequest.getValue().getFamiliaTarifariaAlterada());
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

    @Test
    void voltarDosPassageirosDeveLimparSelecaoEDarNovaEscolhaDosTrechos() throws Exception {
        prepararNavegacaoVoltar();
        simulacao.setStatus("AGUARDANDO_PASSAGEIROS");
        marcarCalculoEPagamentoObsoletos();

        RemarcacaoSimulacaoResponse response = service.voltar(SIMULACAO_ID, voltar());

        assertEquals("AGUARDANDO_TRECHO", response.getStatus());
        assertNull(simulacao.getTrechoIndice());
        assertNull(simulacao.getTrechosIndicesJson());
        assertNull(simulacao.getCriteriosJson());
        assertNull(simulacao.getResultadosJson());
        assertNull(simulacao.getOfertaSelecionadaJson());
        assertNull(simulacao.getPassageirosJson());
        assertCalculoEPagamentoLimpos(response);
        assertEquals(2, response.getTrechos().size());
        assertTrue(response.isPermiteSelecionarIdaVolta());
        assertFalse(response.isPermiteVoltar());
        assertEquals(SIMULACAO_ID, response.getId());
        assertEquals(CONVERSA_ID, response.getConversaId());
    }

    @Test
    void voltarDosCriteriosDaIdaDevePermitirRevisarMesmoPassageiroUnico() throws Exception {
        prepararNavegacaoVoltar();
        simulacao.setStatus("AGUARDANDO_CRITERIOS");
        marcarCalculoEPagamentoObsoletos();

        RemarcacaoSimulacaoResponse response = service.voltar(SIMULACAO_ID, voltar());

        assertEquals("AGUARDANDO_PASSAGEIROS", response.getStatus());
        assertEquals(1, response.getPassageiros().size());
        assertFalse(response.getPassageiros().get(0).isSelecionado());
        assertEquals(0, mapper.readTree(simulacao.getPassageirosJson()).path("indices").size());
        assertEquals("[0,1]", simulacao.getTrechosIndicesJson());
        assertEquals(0, simulacao.getTrechoIndice());
        assertNull(simulacao.getCriteriosJson());
        assertNull(simulacao.getResultadosJson());
        assertNull(simulacao.getOfertaSelecionadaJson());
        assertCalculoEPagamentoLimpos(response);
        assertTrue(response.isPermiteVoltar());
        verify(aereoClient, never()).tarifar(any());
    }

    @Test
    void voltarDasOpcoesDaVoltaDeveRestaurarPreferenciasEPreservarIda() throws Exception {
        prepararNavegacaoVoltar();
        service.simular(SIMULACAO_ID, simular(0, 0));
        RemarcacaoRequest.Pesquisar criterios = pesquisaVoltar(33, "TARDE", true);
        simulacao.setCriteriosJson(mapper.writeValueAsString(criterios));
        simulacao.setResultadosJson(mapper.writeValueAsString(
                List.of(opcaoPesquisa("VOLTA-ID", "BSB", "CGB", "5678"))));
        simulacao.setStatus("AGUARDANDO_OPCAO");
        String passageiros = simulacao.getPassageirosJson();
        marcarCalculoEPagamentoObsoletos();

        RemarcacaoSimulacaoResponse response = service.voltar(SIMULACAO_ID, voltar());

        assertEquals("AGUARDANDO_CRITERIOS", response.getStatus());
        assertEquals(1, simulacao.getTrechoIndice());
        assertEquals(1, response.getTrechosSelecionados().size());
        assertEquals(0, response.getTrechosSelecionados().get(0).getTrechoIndice());
        assertEquals(passageiros, simulacao.getPassageirosJson());
        assertNull(simulacao.getResultadosJson());
        assertTrue(response.isPreferenciasRestauradas());
        assertEquals(criterios.getData(), response.getCriterios().getDataSugerida());
        assertEquals("TARDE", response.getCriterios().getPeriodo());
        assertTrue(response.getCriterios().isSomenteDireto());
        assertCalculoEPagamentoLimpos(response);
        verify(aereoClient, never()).tarifar(any());
    }

    @Test
    void voltarDosCriteriosDaVoltaDeveRestaurarPesquisaDaIdaSemTarifar() throws Exception {
        prepararNavegacaoVoltar();
        String resultadosIda = simulacao.getResultadosJson();
        String criteriosIda = simulacao.getCriteriosJson();
        String passageiros = simulacao.getPassageirosJson();
        service.simular(SIMULACAO_ID, simular(0, 0));

        RemarcacaoSimulacaoResponse response = service.voltar(SIMULACAO_ID, voltar());

        assertEquals("AGUARDANDO_OPCAO", response.getStatus());
        assertEquals(0, simulacao.getTrechoIndice());
        assertEquals("CGB", simulacao.getOrigem());
        assertEquals("BSB", simulacao.getDestino());
        assertEquals(mapper.readTree(resultadosIda), mapper.readTree(simulacao.getResultadosJson()));
        assertEquals(mapper.readTree(criteriosIda), mapper.readTree(simulacao.getCriteriosJson()));
        assertEquals(passageiros, simulacao.getPassageirosJson());
        assertTrue(response.getTrechosSelecionados().isEmpty());
        assertEquals(1, response.getOpcoes().size());
        assertTrue(response.isPreferenciasRestauradas());
        assertEquals(LocalDate.now().plusDays(30), response.getCriterios().getDataSugerida());
        assertCalculoEPagamentoLimpos(response);
        verify(aereoClient, never()).tarifar(any());

        RemarcacaoSimulacaoResponse consulta = service.consultar(SIMULACAO_ID, USUARIO_ID);
        assertTrue(consulta.isPermiteVoltar());
        assertNotNull(consulta.getLabelVoltar());
        assertTrue(consulta.isPreferenciasRestauradas());
        assertEquals(response.getCriterios().getDataSugerida(), consulta.getCriterios().getDataSugerida());
        verify(aereoClient, never()).pesquisarDisponibilidade(any());
    }

    @Test
    void voltarDaVoltaLegadaSemSnapshotDeveReabrirCriteriosDaIda() throws Exception {
        prepararNavegacaoVoltar();
        service.simular(SIMULACAO_ID, simular(0, 0));
        com.fasterxml.jackson.databind.node.ObjectNode selecao =
                (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(simulacao.getOfertaSelecionadaJson());
        for (com.fasterxml.jackson.databind.JsonNode item : selecao.path("trechos")) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) item).remove("criteriosPesquisa");
            ((com.fasterxml.jackson.databind.node.ObjectNode) item).remove("resultadosPesquisa");
        }
        simulacao.setOfertaSelecionadaJson(mapper.writeValueAsString(selecao));

        RemarcacaoSimulacaoResponse response = service.voltar(SIMULACAO_ID, voltar());

        assertEquals("AGUARDANDO_CRITERIOS", response.getStatus());
        assertEquals(0, simulacao.getTrechoIndice());
        assertNull(simulacao.getResultadosJson());
        assertTrue(response.getTrechosSelecionados().isEmpty());
        assertNotNull(response.getCriterios());
        assertFalse(response.isPreferenciasRestauradas());
        verify(aereoClient, never()).tarifar(any());
    }

    @Test
    void voltarDaPreviaConjuntaDeveReabrirVoltaEInvalidarPrecoEPagamento() throws Exception {
        prepararNavegacaoVoltar();
        service.simular(SIMULACAO_ID, simular(0, 0));
        simulacao.setCriteriosJson(mapper.writeValueAsString(pesquisaVoltar(33, "TARDE", true)));
        simulacao.setResultadosJson(mapper.writeValueAsString(
                List.of(opcaoPesquisa("VOLTA-ID", "BSB", "CGB", "5678"))));
        String resultadosVolta = simulacao.getResultadosJson();
        prepararTarifacaoPermitida();
        service.simular(SIMULACAO_ID, simular(0, 0));
        simulacao.setFormaPagamentoCodigo(2);
        simulacao.setFormaPagamentoDescricao("Cartao");
        simulacao.setPagamentoStatus("PREFERENCIA_REGISTRADA");
        simulacao.setPagamentoSelecionadoEm(LocalDateTime.now());

        RemarcacaoSimulacaoResponse response = service.voltar(SIMULACAO_ID, voltar());

        assertEquals("AGUARDANDO_OPCAO", response.getStatus());
        assertEquals(1, simulacao.getTrechoIndice());
        assertEquals("BSB", response.getCriterios().getOrigem());
        assertEquals(mapper.readTree(resultadosVolta), mapper.readTree(simulacao.getResultadosJson()));
        assertEquals(1, response.getTrechosSelecionados().size());
        assertEquals(0, response.getTrechosSelecionados().get(0).getTrechoIndice());
        assertCalculoEPagamentoLimpos(response);
        assertTrue(response.isPreferenciasRestauradas());
        verify(aereoClient).tarifar(any());
        assertThrows(RegraDeNegocioException.class,
                () -> service.encaminhar(SIMULACAO_ID, encaminhar()));
    }

    @Test
    void voltarParaCorrigirIdaDevePermitirConcluirERecalcularNovaPreviaConjunta() throws Exception {
        prepararNavegacaoVoltar();
        simulacao.setResultadosJson(mapper.writeValueAsString(List.of(
                opcaoPesquisa("IDA-ORIGINAL", "CGB", "BSB", "1234"),
                opcaoPesquisa("IDA-ALTERNATIVA", "CGB", "BSB", "9876"))));
        service.simular(SIMULACAO_ID, simular(0, 0));
        service.voltar(SIMULACAO_ID, voltar());

        RemarcacaoSimulacaoResponse novaIda = service.simular(SIMULACAO_ID, simular(1, 0));
        assertEquals("AGUARDANDO_CRITERIOS", novaIda.getStatus());
        assertEquals(2, novaIda.getOrdemTrechoAtual());
        assertEquals("9876", novaIda.getTrechosSelecionados().get(0).getVoo().getNumerosVoos());
        simulacao.setCriteriosJson(mapper.writeValueAsString(pesquisaVoltar(33, "TARDE", true)));
        simulacao.setResultadosJson(mapper.writeValueAsString(
                List.of(opcaoPesquisa("VOLTA-ID", "BSB", "CGB", "5678"))));
        prepararTarifacaoPermitida();

        RemarcacaoSimulacaoResponse previa = service.simular(SIMULACAO_ID, simular(0, 0));
        assertEquals("PREVIA_DISPONIVEL", previa.getStatus());
        assertEquals(2, previa.getPrevia().getTrechosSelecionados().size());
        assertEquals("9876", previa.getPrevia().getTrechosSelecionados().get(0).getVoo().getNumerosVoos());
        service.voltar(SIMULACAO_ID, voltar());
        RemarcacaoSimulacaoResponse recalculada = service.simular(SIMULACAO_ID, simular(0, 0));
        assertEquals("PREVIA_DISPONIVEL", recalculada.getStatus());
        assertEquals(2, recalculada.getPrevia().getTrechosSelecionados().size());
        verify(aereoClient, org.mockito.Mockito.times(2)).tarifar(any());
    }

    @Test
    void voltarDaPreviaLegadaSemResultadosDeveReabrirCriterios() throws Exception {
        prepararNavegacaoVoltar();
        simulacao.setTrechosIndicesJson("[0]");
        prepararPrevia("80.00");
        simulacao.setResultadosJson(null);
        marcarCalculoEPagamentoObsoletos();

        RemarcacaoSimulacaoResponse response = service.voltar(SIMULACAO_ID, voltar());

        assertEquals("AGUARDANDO_CRITERIOS", response.getStatus());
        assertEquals(0, simulacao.getTrechoIndice());
        assertNotNull(response.getCriterios());
        assertCalculoEPagamentoLimpos(response);
        verify(aereoClient, never()).tarifar(any());
    }

    @Test
    void voltarDeveRejeitarVersaoAusenteOuAntigaSemMutarEstado() {
        simulacao.setVersao(7);
        for (Integer versao : java.util.Arrays.asList(null, 6)) {
            RemarcacaoRequest.Voltar request = voltar();
            request.setVersaoEsperada(versao);
            RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                    () -> service.voltar(SIMULACAO_ID, request));
            assertEquals(409, erro.getStatus());
            assertEquals("AGUARDANDO_PASSAGEIROS", simulacao.getStatus());
            assertEquals(7, simulacao.getVersao());
        }
        verify(manager, never()).post(anyString(), any(), any(Class.class));
        verify(aereoClient, never()).carregarReserva(any());
    }

    @Test
    void voltarDeveRejeitarSegundoCliqueDaMesmaVersao() throws Exception {
        prepararNavegacaoVoltar();
        simulacao.setStatus("AGUARDANDO_CRITERIOS");
        RemarcacaoRequest.Voltar request = voltar();
        RemarcacaoSimulacaoResponse primeiro = service.voltar(SIMULACAO_ID, request);
        assertEquals("AGUARDANDO_PASSAGEIROS", primeiro.getStatus());
        assertTrue(primeiro.getVersao() > request.getVersaoEsperada());
        Integer versaoDepois = simulacao.getVersao();

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.voltar(SIMULACAO_ID, request));

        assertEquals(409, erro.getStatus());
        assertEquals("AGUARDANDO_PASSAGEIROS", simulacao.getStatus());
        assertEquals(versaoDepois, simulacao.getVersao());
    }

    @Test
    void voltarDevePreservarCompatibilidadeComSimulacaoLegadaSemVersao() throws Exception {
        prepararNavegacaoVoltar();
        simulacao.setVersao(null);
        simulacao.setStatus("AGUARDANDO_CRITERIOS");
        when(manager.post(anyString(), any(SimulacaoRemarcacao.class), eq(SimulacaoRemarcacao.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        RemarcacaoSimulacaoResponse response = service.voltar(SIMULACAO_ID, voltar());

        assertEquals("AGUARDANDO_PASSAGEIROS", response.getStatus());
        assertNull(response.getVersao());
    }

    @Test
    void voltarDeveBloquearEtapasFinaisIniciaisEOcupadas() {
        simulacao.setVersao(5);
        for (String status : List.of("ENCAMINHADO", "NAO_ELEGIVEL", "ERRO", "EXPIRADO",
                "CANCELADO", "AGUARDANDO_TRECHO", "PESQUISANDO", "CALCULANDO")) {
            simulacao.setStatus(status);
            RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                    () -> service.voltar(SIMULACAO_ID, voltar()));
            assertEquals(409, erro.getStatus());
            assertEquals(status, simulacao.getStatus());
        }
        verify(manager, never()).post(anyString(), any(), any(Class.class));
    }

    @Test
    void voltarDeveRespeitarExpiracaoUsuarioEConversaHumana() {
        RemarcacaoRequest.Voltar outroUsuario = voltar();
        outroUsuario.setCodgUsuario(999);
        assertEquals(403, assertThrows(RegraDeNegocioException.class,
                () -> service.voltar(SIMULACAO_ID, outroUsuario)).getStatus());
        simulacao.setExpiraEm(LocalDateTime.now().minusMinutes(1));
        assertEquals(409, assertThrows(RegraDeNegocioException.class,
                () -> service.voltar(SIMULACAO_ID, voltar())).getStatus());
        simulacao.setExpiraEm(LocalDateTime.now().plusMinutes(20));
        conversa.setStatus(StatusConversa.AGUARDANDO_ATENDENTE);
        conversa.setAtendenteResponsavelCodgUsuario(700);
        assertEquals(409, assertThrows(RegraDeNegocioException.class,
                () -> service.voltar(SIMULACAO_ID, voltar())).getStatus());
        verify(manager, never()).post(anyString(), any(), any(Class.class));
    }

    @Test
    void consultaDeveEsconderVoltarAposEncaminhamento() {
        simulacao.setStatus("ENCAMINHADO");
        conversa.setStatus(StatusConversa.AGUARDANDO_ATENDENTE);
        conversa.setAtendenteResponsavelCodgUsuario(700);
        assertFalse(service.consultar(SIMULACAO_ID, USUARIO_ID).isPermiteVoltar());
    }

    private void prepararNavegacaoVoltar() throws Exception {
        prepararSimulacaoConjunta();
        Reserva reserva = prepararReservaIdaVolta();
        simulacao.setTrechoOriginalJson(mapper.writeValueAsString(reserva.getViagens().get(0)));
        simulacao.setTrechosOriginaisJson(mapper.writeValueAsString(reserva.getViagens()));
        simulacao.setCriteriosJson(mapper.writeValueAsString(pesquisaVoltar(30, "MANHA", false)));
        simulacao.setVersao(5);
        when(manager.post(anyString(), any(SimulacaoRemarcacao.class), eq(SimulacaoRemarcacao.class)))
                .thenAnswer(invocation -> {
                    SimulacaoRemarcacao salva = invocation.getArgument(1);
                    salva.setVersao(salva.getVersao() + 1);
                    return salva;
                });
    }

    private RemarcacaoRequest.Pesquisar pesquisaVoltar(int dias, String periodo, boolean direto) {
        RemarcacaoRequest.Pesquisar request = pesquisaRemarcacao();
        request.setData(LocalDate.now().plusDays(dias));
        request.setPeriodo(periodo);
        request.setSomenteDireto(direto);
        return request;
    }

    private RemarcacaoRequest.Voltar voltar() {
        RemarcacaoRequest.Voltar request = new RemarcacaoRequest.Voltar();
        request.setCodgUsuario(USUARIO_ID);
        request.setVersaoEsperada(simulacao.getVersao());
        return request;
    }

    private void marcarCalculoEPagamentoObsoletos() {
        simulacao.setCalculoJson("{\"totalEstimado\":90.00}");
        simulacao.setFormaPagamentoCodigo(2);
        simulacao.setFormaPagamentoDescricao("Cartao");
        simulacao.setPagamentoStatus("PREFERENCIA_REGISTRADA");
        simulacao.setPagamentoSelecionadoEm(LocalDateTime.now());
    }

    private void assertCalculoEPagamentoLimpos(RemarcacaoSimulacaoResponse response) {
        assertNull(simulacao.getCalculoJson());
        assertNull(simulacao.getFormaPagamentoCodigo());
        assertNull(simulacao.getFormaPagamentoDescricao());
        assertNull(simulacao.getPagamentoSelecionadoEm());
        assertNull(response.getPrevia());
        assertNull(response.getFormaPagamentoSelecionada());
        assertFalse(response.isPermiteEncaminhar());
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

    private RemarcacaoRequest.Simular simular(int opcao, int familia) {
        RemarcacaoRequest.Simular request = new RemarcacaoRequest.Simular();
        request.setCodgUsuario(USUARIO_ID);
        request.setOpcaoIndice(opcao);
        request.setFamiliaIndice(familia);
        return request;
    }

    private RemarcacaoRequest.Pesquisar pesquisaRemarcacao() {
        RemarcacaoRequest.Pesquisar request = new RemarcacaoRequest.Pesquisar();
        request.setCodgUsuario(USUARIO_ID);
        request.setData(LocalDate.now().plusDays(10));
        request.setPeriodo("QUALQUER");
        request.setSomenteDireto(false);
        return request;
    }

    private void prepararPesquisaLatamComTarifaMinima(
            Trecho opcao,
            String novoValorMinimo,
            double tarifaOriginal) throws Exception {
        simulacao.setStatus("AGUARDANDO_CRITERIOS");
        simulacao.setOrigem("CGB");
        simulacao.setDestino("BSB");
        simulacao.setCompanhiaIata("LA");
        simulacao.setPassageirosJson(
                "{\"escopo\":\"INDIVIDUAL\",\"indices\":[0],\"passageiros\":[]}");
        simulacao.setRegraSnapshotJson(mapper.writeValueAsString(
                regraPermitida(false, novoValorMinimo)));

        ConsultarLocalizadorResponse reserva = reservaLatamElegivel(
                "LA", "LA", "LA", false);
        definirTarifaOriginal(reserva.getReservas().get(0), tarifaOriginal);
        when(aereoClient.carregarReserva(any())).thenReturn(reserva);

        opcao.setCompanhia(new com.confApi.hub.aereo.dto.Companhia(1, "LA", "LATAM"));
        opcao.getVoos().get(0).setCiaMandatoria(
                new com.confApi.hub.aereo.dto.Companhia(1, "LA", "LATAM"));
        PesquisaResponse disponibilidade = new PesquisaResponse();
        disponibilidade.setTrechos1(List.of(opcao));
        when(aereoClient.pesquisarDisponibilidade(any())).thenReturn(List.of(disponibilidade));
    }

    private void definirTarifaOriginal(Reserva reserva, double tarifa) {
        ValorPassageiro passageiro = new ValorPassageiro();
        passageiro.setNomePassageiro("Maria");
        passageiro.setTarifa(tarifa);
        passageiro.setTotal(tarifa);

        ValorBase base = new ValorBase();
        base.setTarifa(tarifa);
        base.setTotal(tarifa);
        base.setValorPassageiroList(List.of(passageiro));

        ValorReserva valorReserva = new ValorReserva();
        valorReserva.setValor(tarifa);
        valorReserva.setValorBase(base);
        reserva.setValorReserva(valorReserva);
    }

    private void prepararSimulacaoConjunta() throws Exception {
        simulacao.setStatus("AGUARDANDO_OPCAO");
        simulacao.setTrechoIndice(0);
        simulacao.setOrigem("CGB");
        simulacao.setDestino("BSB");
        simulacao.setCompanhiaIata("G3");
        simulacao.setTrechosIndicesJson("[0,1]");
        simulacao.setPassageirosJson(
                "{\"escopo\":\"INDIVIDUAL\",\"indices\":[0],\"passageiros\":[]}");
        simulacao.setResultadosJson(mapper.writeValueAsString(
                List.of(opcaoPesquisa("IDA-ID", "CGB", "BSB", "1234"))));

        Passageiro passageiro = passageiro("Maria", "ADT", "001", "ATIVO");
        Reserva reserva = new Reserva();
        reserva.setLocalizador("ABC123");
        reserva.setPassageiros(List.of(passageiro));
        reserva.setViagens(List.of(
                trechoReserva("CGB", "BSB", "1234"),
                trechoReserva("BSB", "CGB", "5678")));
        ConsultarLocalizadorResponse hub = new ConsultarLocalizadorResponse();
        hub.setReservas(List.of(reserva));
        when(aereoClient.carregarReserva(any())).thenReturn(hub);
    }

    private Reserva prepararReservaIdaVolta() {
        Reserva reserva = new Reserva();
        reserva.setLocalizador("ABC123");
        reserva.setPassageiros(List.of(passageiro("Maria", "ADT", "001", "ATIVO")));
        reserva.setViagens(List.of(trechoReserva("CGB", "BSB", "1234"),
                trechoReserva("BSB", "CGB", "5678")));
        ConsultarLocalizadorResponse response = new ConsultarLocalizadorResponse();
        response.setReservas(List.of(reserva));
        when(aereoClient.carregarReserva(any())).thenReturn(response);
        when(aeroportoService.findIatasAeroportosNacionais()).thenReturn(java.util.Set.of("CGB", "BSB", "GRU"));
        return reserva;
    }

    private RemarcacaoRequest.SelecionarTrecho selecionarIdaVolta(List<Integer> indices) {
        RemarcacaoRequest.SelecionarTrecho request = new RemarcacaoRequest.SelecionarTrecho();
        request.setCodgUsuario(USUARIO_ID);
        request.setTrechosIndices(indices);
        return request;
    }

    private void definirHorario(Voo voo, LocalDate data, String partida, String chegada) {
        java.util.Date dia = java.util.Date.from(data.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        voo.setDataPartida(dia);
        voo.setDataChegada(dia);
        voo.setHoraPartida(partida);
        voo.setHoraChegada(chegada);
    }

    private Trecho opcaoPesquisa(String identificacao, String origem, String destino, String numero) {
        return opcaoPesquisa(identificacao, origem, destino, numero, "LIGHT", "Light");
    }

    private Trecho opcaoPesquisa(
            String identificacao,
            String origem,
            String destino,
            String numero,
            String familiaCodigo,
            String familiaNome) {
        Trecho trecho = new Trecho();
        trecho.setSistema("Wooba");
        trecho.setIdentificacaoDaViagem(identificacao);
        trecho.setCompanhia(new com.confApi.hub.aereo.dto.Companhia(1, "G3", "GOL"));
        trecho.setOrigem(new com.confApi.hub.aereo.dto.Aeroporto(origem, origem));
        trecho.setDestino(new com.confApi.hub.aereo.dto.Aeroporto(destino, destino));
        Voo voo = voo(origem, destino, numero);
        voo.setIdentificacaoDoVoo("VOO-" + numero);
        trecho.setVoos(List.of(voo));
        FamiliaPreco familia = new FamiliaPreco();
        familia.setClasse("Y");
        familia.setBaseTarifaria("YBR");
        familia.setTipo(familiaNome);
        familia.setFamilia(new com.confApi.aereo.dto.Familia(
                familiaNome, familiaCodigo));
        Preco preco = new Preco();
        preco.setTotalGeral(360.0);
        familia.setPreco(preco);
        trecho.setFamilias(List.of(familia));
        return trecho;
    }

    private FamiliaPreco familiaPesquisa(
            String nome,
            double tarifa,
            double totalGeral) {
        FamiliaPreco familia = new FamiliaPreco();
        familia.setClasse("Y");
        familia.setBaseTarifaria("YBR");
        familia.setTipo(nome);
        familia.setFamilia(new com.confApi.aereo.dto.Familia(
                nome, nome.toUpperCase()));
        Preco preco = new Preco();
        preco.setTotalTarifa(tarifa);
        preco.setTotalGeral(totalGeral);
        PrecoTipo adulto = new PrecoTipo();
        adulto.setValorTarifa(tarifa);
        preco.setPrecoAdulto(adulto);
        familia.setPreco(preco);
        return familia;
    }

    private TrechoReserva trechoReserva(String origem, String destino, String numero) {
        TrechoReserva trecho = new TrechoReserva();
        trecho.setCompanhia(new com.confApi.hub.aereo.dto.Companhia(1, "G3", "GOL"));
        trecho.setOrigem(new com.confApi.hub.aereo.dto.Aeroporto(origem, origem));
        trecho.setDestino(new com.confApi.hub.aereo.dto.Aeroporto(destino, destino));
        trecho.setVoos(List.of(voo(origem, destino, numero)));
        return trecho;
    }

    private Voo voo(String origem, String destino, String numero) {
        Voo voo = new Voo();
        voo.setCiaMandatoria(new com.confApi.hub.aereo.dto.Companhia(1, "G3", "GOL"));
        voo.setNumeroVoo(numero);
        voo.setFamilia("Light");
        voo.setFamiliaCodigo("LIGHT");
        voo.setOrigem(new com.confApi.hub.aereo.dto.Aeroporto(origem, origem));
        voo.setDestino(new com.confApi.hub.aereo.dto.Aeroporto(destino, destino));
        int dias = "BSB".equals(origem) && "CGB".equals(destino) ? 33 : 30;
        definirHorario(voo, LocalDate.now().plusDays(dias), "10:30", "12:30");
        return voo;
    }

    private void prepararTarifacaoPermitida() {
        TarifarResponse tarifa = new TarifarResponse();
        Preco preco = new Preco();
        preco.setMoeda("BRL");
        PrecoTipo adulto = new PrecoTipo();
        adulto.setValorTarifa(300.0);
        adulto.setValorTaxaEmbarque(60.0);
        preco.setPrecoAdulto(adulto);
        tarifa.setPreco(preco);
        when(aereoClient.tarifar(any())).thenReturn(tarifa);
        when(regraService.simular(any())).thenReturn(regraPermitida(false));
    }

    private RegraAereaAlteracaoConsultaResponse regraPermitida(boolean remarcacaoConjunta) {
        return regraPermitida(remarcacaoConjunta, null);
    }

    private RegraAereaAlteracaoConsultaResponse regraPermitida(
            boolean remarcacaoConjunta,
            String novoValorMinimo) {
        RegraAereaAlteracaoRegraResponse regra = new RegraAereaAlteracaoRegraResponse();
        regra.setId(10L);
        regra.setPermiteAlteracao(true);
        regra.setExigeRemarcacaoConjunta(remarcacaoConjunta);
        regra.setNovoValorMinimo(novoValorMinimo);
        RegraAereaAlteracaoCalculoResponse calculo = new RegraAereaAlteracaoCalculoResponse();
        calculo.setCalculoCompleto(true);
        calculo.setValorTarifaBase(BigDecimal.ZERO);
        calculo.setValorNovaTarifa(new BigDecimal("300.00"));
        calculo.setValorTaxasBase(BigDecimal.ZERO);
        calculo.setValorNovasTaxas(new BigDecimal("60.00"));
        calculo.setValorMulta(BigDecimal.ZERO);
        calculo.setDiferencaTarifaria(BigDecimal.ZERO);
        calculo.setTaxaServico(BigDecimal.ZERO);
        calculo.setTotalPrevisto(BigDecimal.ZERO);
        RegraAereaAlteracaoConsultaResponse response = new RegraAereaAlteracaoConsultaResponse();
        response.setStatus("PERMITIDA");
        response.setRegra(regra);
        response.setCalculo(calculo);
        return response;
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

    private void prepararInicioLatam(
            String companhiaTrecho,
            String companhiaMandatoria,
            String companhiaOperadora,
            boolean codeshare) {
        when(manager.get(
                contains("chat-confianca/consultas/remarcacoes/reservas-emitidas"),
                eq(ReservasEmitidasRemarcacaoResponse.class)))
                .thenReturn(respostaSelecao(reservaEmitida(501, "ABC123", "LA", "Wooba")));
        when(aereoClient.carregarReserva(any())).thenReturn(reservaLatamElegivel(
                companhiaTrecho, companhiaMandatoria, companhiaOperadora, codeshare));
        when(aeroportoService.findIatasAeroportosNacionais())
                .thenReturn(java.util.Set.of("CGB", "BSB"));
        when(regraService.simular(any())).thenReturn(regraPermitida(false));
    }

    private ConsultarLocalizadorResponse reservaLatamElegivel(
            String companhiaTrecho,
            String companhiaMandatoria,
            String companhiaOperadora,
            boolean codeshare) {
        Reserva reserva = new Reserva();
        reserva.setLocalizador("ABC123");
        reserva.setSistema("Wooba");
        reserva.setDataEmissao(new java.util.Date());
        reserva.setPassageiros(List.of(passageiro("Maria", "ADT", "001", "ATIVO")));

        TrechoReserva trecho = new TrechoReserva();
        trecho.setCompanhia(new com.confApi.hub.aereo.dto.Companhia(
                1, companhiaTrecho, companhiaTrecho));
        trecho.setOrigem(new com.confApi.hub.aereo.dto.Aeroporto("CGB", "Cuiaba"));
        trecho.setDestino(new com.confApi.hub.aereo.dto.Aeroporto("BSB", "Brasilia"));

        Voo voo = voo("CGB", "BSB", "1234");
        voo.setCiaMandatoria(new com.confApi.hub.aereo.dto.Companhia(
                1, companhiaMandatoria, companhiaMandatoria));
        voo.setCiaOperadora(new com.confApi.hub.aereo.dto.Companhia(
                1, companhiaOperadora, companhiaOperadora));
        voo.setIsCodeShare(codeshare);
        trecho.setVoos(List.of(voo));
        reserva.setViagens(List.of(trecho));

        ConsultarLocalizadorResponse response = new ConsultarLocalizadorResponse();
        response.setReservas(List.of(reserva));
        return response;
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
