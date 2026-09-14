package com.confApi.chatconfianca.v2;

import com.confApi.aereo.AereoClient;
import com.confApi.aereo.AereoRegrasReservaService;
import com.confApi.aereo.dto.ConsultarLocalizadorRequest;
import com.confApi.aereo.dto.ConsultarLocalizadorResponse;
import com.confApi.aereo.dto.Reserva;
import com.confApi.chatconfianca.dto.enums.RemetenteTipo;
import com.confApi.chatconfianca.dto.model.Conversa;
import com.confApi.chatconfianca.dto.model.Mensagem;
import com.confApi.chatconfianca.intencao.ChatConfiancaDecisaoIa;
import com.confApi.chatgpt.dto.*;
import com.confApi.chatgpt.service.ChatService;
import com.confApi.chatgpt.tools.ToolRouter;
import com.confApi.chatgpt.util.LocalizadorAereo;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatLocalizadorLongoTest {
    private static final String LOCALIZADOR = "LA9572345FRJD";
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private AereoClient aereo;
    private ChatService chat;
    private ChatV2SemanticClient semantic;
    private ChatV2Planner planner;
    private ChatV2Executor executor;

    @BeforeEach void setup() throws Exception {
        aereo = mock(AereoClient.class);
        chat = spy(new ChatService(null, null, null, null, null, null, null, null, null, null,
                aereo, mock(AereoRegrasReservaService.class)));
        doReturn(new ChatResponseDTO(null, "Reserva consultada", List.of(), null, List.of(), List.of()))
                .when(chat).chat(any(), any(), any());
        semantic = mock(ChatV2SemanticClient.class);
        ChatV2Properties properties = new ChatV2Properties();
        properties.setEnabled(true);
        properties.setTrafficPercent(100);
        planner = new ChatV2Planner(properties, semantic, mapper);
        executor = new ChatV2Executor(chat, mock(ToolRouter.class), mapper);
        when(aereo.carregarReserva(any())).thenAnswer(invocation -> {
            ConsultarLocalizadorRequest request = invocation.getArgument(0);
            Reserva reserva = new Reserva();
            reserva.setLocalizador(request.getLocalizador());
            reserva.setStatus("CONFIRMADA");
            ConsultarLocalizadorResponse response = new ConsultarLocalizadorResponse();
            response.setReservas(List.of(reserva));
            return response;
        });
    }

    private ConversationRequestDTO request(String input) {
        return new ConversationRequestDTO("confia", "Confianca", "321", 321L, 101L,
                input, new ArrayList<>(), null, false, new ArrayList<>());
    }

    private Conversa anterior(ChatV2Capability capability) throws Exception {
        ChatV2Plan state = ChatV2Plan.of(capability);
        state.setAgencia(321);
        state.setUsuario(101);
        state.setAtualizadoEm(System.currentTimeMillis());
        state.getParametros().put("localizador", "ABC123");
        Conversa conversa = new Conversa();
        conversa.setMetadadosJson(mapper.writeValueAsString(Map.of("confiaV2", state)));
        return conversa;
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC12", "ABC123", "ABC12345", "LA9572345FRJD", "ABCDEFGHJKLMN"})
    void pedidoDeAbrirChegaInteiroAoClienteAereoNasDuasVersoes(String code) {
        String input = "abra a reserva " + code.toLowerCase(Locale.ROOT);
        List<ChatMessageDTO> messages = new ArrayList<>();
        assertTrue(chat.actionApis(messages, request(input)).contains("reserva_aerea_detalhes"));
        assertTrue(messages.stream().anyMatch(message -> message.content().contains(code)));

        ChatV2Plan plan = planner.planejar(input, null, List.of(), 321, 101);
        assertEquals(ChatV2Capability.RESERVA, plan.capability());
        assertEquals(code, plan.getParametros().get("localizador"));
        executor.executar(plan, request(input), new ChatConfiancaDecisaoIa());
        assertEquals("DADOS_CONSULTADOS", plan.getResultado());
        ArgumentCaptor<ConsultarLocalizadorRequest> captor = ArgumentCaptor.forClass(ConsultarLocalizadorRequest.class);
        verify(aereo, times(2)).carregarReserva(captor.capture());
        for (ConsultarLocalizadorRequest sent : captor.getAllValues()) {
            assertEquals(code, sent.getLocalizador());
            assertEquals("321", sent.getAgencia().getCodgAgencia());
        }
        verifyNoInteractions(semantic);
    }

    @ParameterizedTest
    @ValueSource(strings = {"aereo.reserva_detalhes", "aereo.regra_tarifaria", "aereo.acoes_reserva", "aereo.simular_remarcacao"})
    void codigoIsoladoSubstituiReservaAnteriorSemPerderIntencao(String capability) throws Exception {
        ChatV2Plan plan = planner.planejar(LOCALIZADOR.toLowerCase(Locale.ROOT),
                anterior(ChatV2Capability.from(capability)), List.of(), 321, 101);
        assertEquals(capability, plan.getIntencao());
        assertEquals(LOCALIZADOR, plan.getParametros().get("localizador"));
        assertTrue(plan.isContinuar());
        verifyNoInteractions(semantic);
    }

    @Test void acaoPersistidaPreservaLocalizadorLongo() throws Exception {
        String prompt = "Consulte as regras da reserva " + LOCALIZADOR;
        Mensagem bot = new Mensagem();
        bot.setRemetenteTipo(RemetenteTipo.BOT);
        bot.setConteudoJson(mapper.writeValueAsString(Map.of("actions", List.of(Map.of(
                "code", "consultar_regras", "prompt", prompt, "localizador", LOCALIZADOR)))));
        ChatV2Plan plan = planner.planejar(prompt, anterior(ChatV2Capability.RESERVA), List.of(bot), 321, 101);
        assertEquals(ChatV2Capability.REGRAS, plan.capability());
        assertEquals("V2_ACAO_PERSISTIDA", plan.getFonte());
        assertEquals(LOCALIZADOR, plan.getParametros().get("localizador"));
        verifyNoInteractions(semantic);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC123", "LA9572345FRJD"})
    void fallbackLegadoPreservaCodigoCompleto(String code) {
        assertEquals(code, ReflectionTestUtils.invokeMethod(chat, "normalizarLocalizadorExtraido", "`" + code + "`"));
        assertEquals(code, ReflectionTestUtils.invokeMethod(chat, "extrairLocalizadorPorRegex", "reserva " + code));
    }

    @ParameterizedTest
    @ValueSource(strings = {"reserva: LA9572345FRJD", "PNR LA9572345FRJD", "loc LA9572345FRJD", "Abra a reserva la9572345frjd!"})
    void aceitaCodigoExplicitoComPontuacao(String input) {
        assertEquals(LOCALIZADOR, LocalizadorAereo.extrair(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"LA9572345FRJDX", "ABC1", "localizador", "anterior", "cancelamento"})
    void codigoInvalidoNaoETruncadoNemEnviadoAoClienteAereo(String value) {
        assertNull(LocalizadorAereo.extrair("abra a reserva " + value));
        ChatV2Plan plan = ChatV2Plan.of(ChatV2Capability.RESERVA);
        plan.getParametros().put("localizador", value);
        executor.executar(plan, request("abra a reserva " + value), new ChatConfiancaDecisaoIa());
        assertEquals("AGUARDANDO_DADOS", plan.getResultado());
        verifyNoInteractions(aereo);
    }
}
