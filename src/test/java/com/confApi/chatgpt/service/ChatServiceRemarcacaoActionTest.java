package com.confApi.chatgpt.service;

import com.confApi.aereo.AereoClient;
import com.confApi.aereo.AereoRegrasReservaService;
import com.confApi.aereo.dto.ConsultarLocalizadorRequest;
import com.confApi.aereo.dto.ConsultarLocalizadorResponse;
import com.confApi.aereo.dto.Reserva;
import com.confApi.chatconfianca.dto.reserva.ReservaAereaRecenteItem;
import com.confApi.chatconfianca.dto.reserva.ReservasAereasRecentesResponse;
import com.confApi.chatconfianca.service.ChatConfiancaReservaAereaService;
import com.confApi.chatgpt.config.OpenAIProperties;
import com.confApi.chatgpt.dto.ChatActionDTO;
import com.confApi.chatgpt.dto.ChatMessageDTO;
import com.confApi.chatgpt.dto.ChatResponseDTO;
import com.confApi.chatgpt.dto.ConversationRequestDTO;
import com.confApi.chatgpt.tools.ToolRouter;
import com.confApi.db.confManager.alertaTarifa.AlertaTarifaService;
import com.confApi.db.confManager.chatMemoria.ChatMemoriaService;
import com.confApi.db.confManager.familia.FamiliaService;
import com.confApi.db.confManager.faturas.FaturasService;
import com.confApi.db.wooba.checkin.CheckinService;
import com.confApi.hub.aereo.dto.Bilhete;
import com.confApi.hub.aereo.dto.Companhia;
import com.confApi.hub.aereo.dto.Passageiro;
import com.confApi.hub.aereo.dto.TrechoReserva;
import com.confApi.hub.aereo.dto.Voo;
import com.confApi.hub.limites.LimitesService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChatServiceRemarcacaoActionTest {
    private final OkHttpClient openAiClient = mock(OkHttpClient.class);
    private final AereoClient aereoClient = mock(AereoClient.class);
    private final AereoRegrasReservaService regrasReservaService =
            mock(AereoRegrasReservaService.class);
    private final ChatConfiancaReservaAereaService reservasService =
            mock(ChatConfiancaReservaAereaService.class);
    private ChatService service;

    @BeforeEach
    void setUp() {
        service = new ChatService(
                openAiClient,
                mock(OpenAIProperties.class),
                mock(ToolRouter.class),
                mock(ChatMemoriaService.class),
                mock(LimitesService.class),
                mock(FaturasService.class),
                mock(CheckinService.class),
                mock(FamiliaService.class),
                mock(AlertaTarifaService.class),
                reservasService,
                aereoClient,
                regrasReservaService);
    }

    @Test
    void deveCriarAcaoDeterministicaDeSelecaoQuandoNaoHaLocalizador() {
        List<ChatMessageDTO> messages = new ArrayList<>();

        List<String> keywords = service.actionApis(
                messages,
                request("Quero simular uma remarcacao"));
        List<ChatActionDTO> actions = service.extrairAcoesDisponiveis(messages);

        assertTrue(keywords.contains("selecionar_reserva_remarcacao"));
        assertEquals(1, actions.size());
        assertEquals("selecionar_reserva_remarcacao", actions.get(0).code());
        assertNull(actions.get(0).localizador());
        verifyNoInteractions(aereoClient);
    }

    @Test
    void deveUsarLocalizadorSomenteComoPreenchimentoDoSeletor() {
        List<ChatMessageDTO> messages = new ArrayList<>();

        List<String> keywords = service.actionApis(
                messages,
                request("Quero remarcar a reserva ABC123"));
        List<ChatActionDTO> actions = service.extrairAcoesDisponiveis(messages);

        assertTrue(keywords.contains("simular_remarcacao"));
        assertEquals(1, actions.size());
        assertEquals("simular_remarcacao", actions.get(0).code());
        assertEquals("ABC123", actions.get(0).localizador());
        assertTrue(actions.get(0).prompt().contains("preencher a busca"));
        assertTrue(actions.get(0).prompt().contains("Nao inicie"));
        verifyNoInteractions(aereoClient);
    }

    @Test
    void fraseSemPedidoDeRemarcacaoNaoDeveGerarAcaoDoSeletor() {
        List<ChatMessageDTO> messages = new ArrayList<>();

        service.actionApis(messages, request("A reserva ABC123 tem multa para remarcacao?"));
        List<ChatActionDTO> actions = service.extrairAcoesDisponiveis(messages);

        assertTrue(actions.stream().noneMatch(action ->
                "selecionar_reserva_remarcacao".equals(action.code())
                        || "simular_remarcacao".equals(action.code())));
    }

    @Test
    void palavraComumAposReservaNaoDeveSerInterpretadaComoLocalizador() {
        List<ChatMessageDTO> messages = new ArrayList<>();

        service.actionApis(messages, request("Quero remarcar a reserva emitida"));
        List<ChatActionDTO> actions = service.extrairAcoesDisponiveis(messages);

        assertEquals(1, actions.size());
        assertEquals("selecionar_reserva_remarcacao", actions.get(0).code());
        assertNull(actions.get(0).localizador());
    }

    @Test
    void deveClassificarDeterministicamenteAsFrasesComLocalizador() {
        assertEquals("reserva_aerea_detalhes",
                service.classificarIntencaoOperacionalDeterministica("Abrir reserva ABC123"));
        assertEquals("reserva_aerea_regras",
                service.classificarIntencaoOperacionalDeterministica("Cancelar ABC123"));
        assertEquals("reserva_aerea_regras",
                service.classificarIntencaoOperacionalDeterministica("Visualizar regras ABC123"));
        assertEquals("reserva_aerea_detalhes",
                service.classificarIntencaoOperacionalDeterministica("Mostre a reserva ABC123"));
        assertEquals("reserva_aerea_detalhes",
                service.classificarIntencaoOperacionalDeterministica(
                        "Mostre os dados do localizador ABC123"));
        assertEquals("reserva_aerea_detalhes",
                service.classificarIntencaoOperacionalDeterministica("Mostrar a reserva ABC123"));
        assertEquals("simular_remarcacao",
                service.classificarIntencaoOperacionalDeterministica("Simular remarcação ABC123"));
        verifyNoInteractions(openAiClient, aereoClient, regrasReservaService, reservasService);
    }

    @Test
    void consultaDeLocalizadorEmitidoElegivelDeveOferecerSimulacao() {
        when(aereoClient.carregarReserva(any()))
                .thenReturn(consultaReservaEmitidaElegivel("ABC123"));
        List<ChatMessageDTO> messages = new ArrayList<>();

        List<String> keywords = service.actionApis(
                messages, request("Mostre os dados do localizador ABC123"));
        List<ChatActionDTO> actions = service.extrairAcoesDisponiveis(messages);

        assertTrue(keywords.contains("reserva_aerea_detalhes"));
        ChatActionDTO simular = actions.stream()
                .filter(action -> "simular_remarcacao".equals(action.code()))
                .findFirst()
                .orElseThrow();
        assertEquals("ABC123", simular.localizador());
        assertTrue(simular.prompt().contains("preencher a busca"));
        verifyNoInteractions(openAiClient, regrasReservaService, reservasService);
    }

    @Test
    void statusEmitidaSemBilheteDeveGerarAlertaMasNaoOferecerSimulacao() {
        Reserva reserva = new Reserva();
        reserva.setLocalizador("ABC123");
        reserva.setStatus("EMITIDA");
        ConsultarLocalizadorResponse consulta = new ConsultarLocalizadorResponse();
        consulta.setReservas(List.of(reserva));
        when(aereoClient.carregarReserva(any())).thenReturn(consulta);
        List<ChatMessageDTO> messages = new ArrayList<>();

        service.actionApis(messages, request("Mostre os dados do localizador ABC123"));
        List<ChatActionDTO> actions = service.extrairAcoesDisponiveis(messages);

        assertTrue(messages.get(0).content().contains("RESERVA_EMITIDA"));
        assertTrue(actions.stream().noneMatch(action ->
                "simular_remarcacao".equals(action.code())));
    }

    @Test
    void consultaSingularNaoDeveVirarListagemMasPluralOuRecenciaDevemListar() {
        assertFalse(service.isListagemReservasRecentesDeterministica(
                "Mostre a reserva ABC123"));
        assertFalse(service.isListagemReservasRecentesDeterministica(
                "Mostrar a reserva ABC123"));
        assertTrue(service.isListagemReservasRecentesDeterministica(
                "Mostrar reservas"));
        assertTrue(service.isListagemReservasRecentesDeterministica(
                "Minhas reservas"));
        assertTrue(service.isListagemReservasRecentesDeterministica(
                "Mostre minha ultima reserva"));
        verifyNoInteractions(openAiClient, aereoClient, regrasReservaService, reservasService);
    }

    @Test
    void deveIdentificarAcaoSolicitadaSemLlmEComPnrAlfabeticoMinusculo() {
        assertEquals("abrir_reserva",
                service.identificarAcaoSolicitadaDeterministica("Abrir reserva kbtxlh"));
        assertEquals("preparar_cancelamento",
                service.identificarAcaoSolicitadaDeterministica("Cancelar kbtxlh"));
        assertEquals("consultar_regras",
                service.identificarAcaoSolicitadaDeterministica("Visualizar regras kbtxlh"));
        assertEquals("simular_remarcacao",
                service.identificarAcaoSolicitadaDeterministica("Simular remarcação kbtxlh"));
        assertNull(service.identificarAcaoSolicitadaDeterministica(
                "Abrir a reserva emitida"));
        assertNull(service.identificarAcaoSolicitadaDeterministica(
                "Liste minhas últimas reservas"));
        assertNull(service.identificarAcaoSolicitadaDeterministica(
                "Mostre a reserva ABC123"));
        verifyNoInteractions(openAiClient, aereoClient, regrasReservaService, reservasService);
    }

    @Test
    void acaoSolicitadaDeveRespeitarIntencaoDeRegrasENegacaoExplicita() {
        assertEquals("consultar_regras",
                service.identificarAcaoSolicitadaDeterministica(
                        "Abrir as regras da reserva ABC123"));
        assertEquals("consultar_regras",
                service.identificarAcaoSolicitadaDeterministica(
                        "Quais regras antes de abrir a reserva ABC123?"));
        assertNull(service.identificarAcaoSolicitadaDeterministica(
                "N\u00e3o quero simular remarca\u00e7\u00e3o ABC123"));
        assertNull(service.identificarAcaoSolicitadaDeterministica(
                "Nao quero abrir a reserva ABC123"));
        assertNull(service.identificarAcaoSolicitadaDeterministica(
                "N\u00e3o abra a reserva ABC123"));
        assertNull(service.identificarAcaoSolicitadaDeterministica(
                "N\u00e3o simule a remarca\u00e7\u00e3o ABC123"));
        assertNull(service.identificarAcaoSolicitadaDeterministica(
                "N\u00e3o remarque a reserva ABC123"));
        assertNull(service.identificarAcaoSolicitadaDeterministica(
                "N\u00e3o cancele a reserva ABC123"));

        assertEquals("abrir_reserva",
                service.identificarAcaoSolicitadaDeterministica("Abrir reserva ABC123"));
        assertEquals("simular_remarcacao",
                service.identificarAcaoSolicitadaDeterministica(
                        "Simular remarcacao da reserva ABC123"));
        assertEquals("preparar_cancelamento",
                service.identificarAcaoSolicitadaDeterministica("Cancelar reserva ABC123"));
        assertEquals("consultar_regras",
                service.identificarAcaoSolicitadaDeterministica(
                        "Visualizar regras da reserva ABC123"));
        verifyNoInteractions(openAiClient, aereoClient, regrasReservaService, reservasService);
    }

    @Test
    void deveNormalizarPnrAlfabeticoContextualSemAceitarPalavraComum() {
        ConsultarLocalizadorResponse consulta = consultaReserva("KBTXLH", true);
        when(aereoClient.carregarReserva(any())).thenReturn(consulta);
        when(regrasReservaService.enriquecer(consulta)).thenReturn(consulta);

        service.actionApis(new ArrayList<>(), request("Abrir reserva kbtxlh"));
        service.actionApis(new ArrayList<>(), request("Cancelar kbtxlh"));
        service.actionApis(new ArrayList<>(), request("Visualizar regras kbtxlh"));
        List<ChatMessageDTO> palavraComum = new ArrayList<>();
        service.actionApis(palavraComum, request("Abrir a reserva emitida"));

        ArgumentCaptor<ConsultarLocalizadorRequest> captor =
                ArgumentCaptor.forClass(ConsultarLocalizadorRequest.class);
        verify(aereoClient, times(3)).carregarReserva(captor.capture());
        assertTrue(captor.getAllValues().stream()
                .allMatch(item -> "KBTXLH".equals(item.getLocalizador())));
        assertTrue(palavraComum.get(0).content().contains("nao informou um localizador claro"));
        verifyNoInteractions(openAiClient);
    }

    @Test
    void deveGerarAberturaSemUsarClassificadorOuExtratorIa() {
        ConsultarLocalizadorResponse consulta = consultaReserva("ABC123", false);
        when(aereoClient.carregarReserva(any())).thenReturn(consulta);
        List<ChatMessageDTO> messages = new ArrayList<>();

        List<String> keywords = service.actionApis(
                messages, request("Abrir reserva ABC123"));
        List<ChatActionDTO> actions = service.extrairAcoesDisponiveis(messages);

        assertTrue(keywords.contains("reserva_aerea_detalhes"));
        assertTrue(actions.stream().anyMatch(action ->
                "abrir_reserva".equals(action.code())
                        && "ABC123".equals(action.localizador())));
        verifyNoInteractions(openAiClient, regrasReservaService);
    }

    @Test
    void cancelamentoDigitadoDeveSomentePrepararEExigirRegrasEConfirmacao() {
        ConsultarLocalizadorResponse consulta = consultaReserva("ABC123", true);
        when(aereoClient.carregarReserva(any())).thenReturn(consulta);
        when(regrasReservaService.enriquecer(consulta)).thenReturn(consulta);
        List<ChatMessageDTO> messages = new ArrayList<>();

        List<String> keywords = service.actionApis(
                messages, request("Cancelar ABC123"));
        List<ChatActionDTO> actions = service.extrairAcoesDisponiveis(messages);
        ChatActionDTO cancelar = actions.stream()
                .filter(action -> "preparar_cancelamento".equals(action.code()))
                .findFirst()
                .orElseThrow();

        assertTrue(keywords.contains("reserva_aerea_regras"));
        assertEquals("ABC123", cancelar.localizador());
        assertTrue(cancelar.requiresConfirmation());
        assertTrue(cancelar.requiresRules());
        assertTrue(cancelar.sensitive());
        assertTrue(cancelar.prompt().contains("Nao execute"));
        assertTrue(actions.stream().noneMatch(action ->
                "cancelar_reserva".equals(action.code())
                        || "executar_cancelamento".equals(action.code())));
        verifyNoInteractions(openAiClient);
    }

    @Test
    void deveListarReservasRecentesDaAgenciaComPayloadEstruturadoEAcoesPorReserva() {
        ReservaAereaRecenteItem item = new ReservaAereaRecenteItem();
        item.setReservaId(77);
        item.setLocalizador("ABC123");
        item.setStatus(3);
        item.setStatusDescricao("Emitida");
        item.setDataCriacao(LocalDateTime.of(2026, 8, 1, 10, 30));
        ChatActionDTO abrir = new ChatActionDTO(
                "abrir_reserva", "Abrir reserva", "Abrir", "ABC123", 77,
                false, false, false, "Abrir a reserva ABC123 no sistema.");
        item.setActions(List.of(abrir));
        ReservasAereasRecentesResponse recentes = new ReservasAereasRecentesResponse();
        recentes.setQuantidade(1);
        recentes.setReservas(List.of(item));
        when(reservasService.listarRecentes(321, 10)).thenReturn(recentes);
        when(reservasService.listarAcoes(recentes)).thenReturn(List.of(abrir));

        List<ChatMessageDTO> messages = new ArrayList<>();
        List<String> keywords = service.actionApis(
                messages,
                request("Liste minhas ultimas reservas"));
        List<ChatActionDTO> actions = service.extrairAcoesDisponiveis(messages);

        assertTrue(keywords.contains("ultimas_reservas_aereas"));
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).content().contains("chat.reservas-recentes.v1"));
        assertTrue(messages.get(0).content().contains("sem repetir cada reserva em texto"));
        assertEquals(1, actions.size());
        assertEquals("abrir_reserva", actions.get(0).code());
        assertEquals(77, actions.get(0).reservaId());
    }

    @Test
    void deveProduzirListagemDeterministicaComUmaUnicaCopiaDoPayloadEstruturado() throws Exception {
        ReservaAereaRecenteItem item = new ReservaAereaRecenteItem();
        item.setReservaId(77);
        item.setLocalizador("ABC123");
        ChatActionDTO abrir = new ChatActionDTO(
                "abrir_reserva", "Abrir reserva", "Abrir", "ABC123", 77,
                false, false, false, "Abrir a reserva ABC123 no sistema.");
        item.setActions(List.of(abrir));
        ReservasAereasRecentesResponse recentes = new ReservasAereasRecentesResponse();
        recentes.setQuantidade(1);
        recentes.setReservas(List.of(item));
        when(reservasService.listarRecentes(321, 10)).thenReturn(recentes);
        when(reservasService.listarAcoes(recentes)).thenReturn(List.of(abrir));

        ChatResponseDTO response = service.responderListagemReservasRecentes(
                request("Liste minhas ultimas reservas"));

        assertTrue(service.isListagemReservasRecentesDeterministica(
                "Liste minhas ultimas reservas"));
        assertNull(response.content());
        assertEquals(List.of("ultimas_reservas_aereas"), response.keywords());
        assertEquals(1, response.history().size());
        assertEquals(1, contarOcorrencias(
                response.history().get(0).content(), "chat.reservas-recentes.v1"));
        String conteudo = response.history().get(0).content();
        JsonNode payload = new ObjectMapper().findAndRegisterModules().readTree(
                conteudo.substring(conteudo.indexOf('{'), conteudo.lastIndexOf('}') + 1));
        assertFalse(payload.path("reservasRecentes").path("reservas").get(0).has("actions"));
        assertEquals(1, payload.path("actions").size());
        assertEquals(1, response.actions().size());
        assertEquals(77, response.actions().get(0).reservaId());
    }

    private int contarOcorrencias(String texto, String trecho) {
        int quantidade = 0;
        int indice = 0;
        while ((indice = texto.indexOf(trecho, indice)) >= 0) {
            quantidade++;
            indice += trecho.length();
        }
        return quantidade;
    }

    private ConsultarLocalizadorResponse consultaReserva(String localizador,
                                                         boolean permiteCancelar) {
        Reserva reserva = new Reserva();
        reserva.setLocalizador(localizador);
        reserva.setStatus("CONFIRMADA");
        reserva.setPermiteCancelar(permiteCancelar);
        ConsultarLocalizadorResponse response = new ConsultarLocalizadorResponse();
        response.setReservas(List.of(reserva));
        return response;
    }

    private ConsultarLocalizadorResponse consultaReservaEmitidaElegivel(String localizador) {
        Bilhete bilhete = new Bilhete();
        bilhete.setNumero("1271234567890");
        bilhete.setStatus("ATIVO");
        Passageiro passageiro = new Passageiro();
        passageiro.setBilhetes(List.of(bilhete));

        Voo voo = new Voo();
        voo.setDataPartida(new Date(System.currentTimeMillis() + 86_400_000L));
        TrechoReserva trecho = new TrechoReserva();
        trecho.setCompanhia(new Companhia(1, "G3", "GOL"));
        trecho.setVoos(List.of(voo));

        Reserva reserva = new Reserva();
        reserva.setLocalizador(localizador);
        reserva.setStatus("EMITIDA");
        reserva.setPassageiros(List.of(passageiro));
        reserva.setViagens(List.of(trecho));
        ConsultarLocalizadorResponse response = new ConsultarLocalizadorResponse();
        response.setReservas(List.of(reserva));
        return response;
    }

    private ConversationRequestDTO request(String input) {
        return new ConversationRequestDTO(
                "confia",
                "Confianca",
                "1",
                321L,
                101L,
                input,
                new ArrayList<>(),
                null,
                false,
                new ArrayList<>());
    }
}
