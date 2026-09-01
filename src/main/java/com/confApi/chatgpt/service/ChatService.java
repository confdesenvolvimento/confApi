package com.confApi.chatgpt.service;


import com.confApi.aereo.AereoClient;
import com.confApi.aereo.AereoRegrasReservaService;
import com.confApi.aereo.dto.ConsultarLocalizadorRequest;
import com.confApi.aereo.dto.ConsultarLocalizadorResponse;
import com.confApi.aereo.dto.Reserva;
import com.confApi.aereo.dto.regrasAereas.RegrasAereasReservaResponse;
import com.confApi.chatconfianca.dto.reserva.ReservasAereasRecentesResponse;
import com.confApi.chatconfianca.service.ChatConfiancaReservaAereaService;
import com.confApi.chatgpt.config.ChatHistoryUtil;
import com.confApi.chatgpt.config.OpenAIProperties;
import com.confApi.chatgpt.dto.*;
import com.confApi.chatgpt.tools.ToolRouter;
import com.confApi.db.confManager.alertaTarifa.AlertaTarifaService;
import com.confApi.db.confManager.alertaTarifa.dto.AlertaTarifaDTO;
import com.confApi.db.confManager.alertaTarifa.dto.ia.AlertaTarifaIAResponse;
import com.confApi.db.confManager.chatMemoria.ChatMemoriaService;
import com.confApi.db.confManager.chatMemoria.dto.ChatMemoria;
import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.db.confManager.aeroporto.Aeroporto;
import com.confApi.db.confManager.companhiaAerea.CompanhiaAerea;
import com.confApi.db.confManager.familia.FamiliaService;
import com.confApi.db.confManager.familia.dto.FamiliaCompanhia;
import com.confApi.db.confManager.familia.dto.ia.FamiliaIAResponse;
import com.confApi.db.confManager.passageiro.Passageiro;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.db.confManager.trecho.Trecho;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.db.confManager.voo.Voo;
import com.confApi.db.wooba.checkin.CheckinService;
import com.confApi.db.wooba.checkin.dto.Checkin72Horas;
import com.confApi.db.wooba.checkin.dto.CheckinRQ;
import com.confApi.db.wooba.checkin.dto.ia.CheckinIAResponse;
import com.confApi.db.wooba.checkin.dto.ia.ReservaCheckInIA;
import com.confApi.db.confManager.faturas.FaturasService;
import com.confApi.db.confManager.faturas.dto.FaturaIA;
import com.confApi.db.confManager.faturas.dto.FaturaSicaRQ;
import com.confApi.db.confManager.faturas.dto.FaturaSicaRS;
import com.confApi.db.confManager.faturas.dto.model.FaturaResponseIA;
import com.confApi.db.confManager.regraAereaAlteracao.dto.RegraAereaAlteracaoConsultaResponse;
import com.confApi.db.confManager.regraAereaReembolso.dto.RegraAereaReembolsoConsultaResponse;
import com.confApi.hub.limites.LimitesService;
import com.confApi.hub.limites.dto.Disponibilidade;
import com.confApi.hub.limites.dto.LimiteCreditoRQ;
import com.confApi.model.IdentificacaoAgenciaModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import okio.BufferedSource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ChatService.java
@Service
@RequiredArgsConstructor
public class ChatService {
    private static final int LIMITE_ULTIMAS_RESERVAS_AEREAS = 10;
    private static final int LIMITE_PASSAGEIROS_RESUMO = 5;
    private static final int LIMITE_TRECHOS_RESUMO = 4;
    private static final int LIMITE_VOOS_RESUMO = 8;
    private static final Set<String> COMPANHIAS_REMARCACAO_SUPORTADAS =
            Set.of("G3", "LA", "JJ", "AD");

    private final OkHttpClient client;
    private final OpenAIProperties props;
    private final ToolRouter tools;

    private final ChatMemoriaService chatMemoriaService;
    private final LimitesService limitesService;
    private final FaturasService faturasService;
    private final CheckinService checkinService;
    private final FamiliaService familiaService;
    private final AlertaTarifaService alertaTarifaService;
    private final ChatConfiancaReservaAereaService chatConfiancaReservaAereaService;
    private final AereoClient aereoClient;
    private final AereoRegrasReservaService regrasReservaService;


    private final ObjectMapper mapper = new ObjectMapper()
            .findAndRegisterModules()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

    public ChatResponseDTO chat(ChatRequestDTO req, List<String> keywords, List<ChatMessageDTO> history) throws IOException {
        String model = Optional.ofNullable(req.model()).orElse(props.getChatModel());
        ObjectMapper om = new ObjectMapper().findAndRegisterModules();

        // 0) Normaliza e aplica trim no histórico
        List<ChatMessageDTO> baseHistory = (history != null) ? history : new ArrayList<>();
        baseHistory = ChatHistoryUtil.trimHistory(baseHistory);

        // 1) Constrói mensagens a enviar: history + mensagens do turno (req.messages)
        List<Map<String, Object>> workingMessages = new ArrayList<>();

        // 1.1) Adiciona o histórico (na ordem)
        for (ChatMessageDTO m : baseHistory) {
            workingMessages.add(Map.of("role", m.role(), "content", m.content()));
        }

        // 1.2) Adiciona as mensagens deste turno
        if (req.messages() != null && !req.messages().isEmpty()) {
            for (ChatMessageDTO m : req.messages()) {
                workingMessages.add(Map.of("role", m.role(), "content", m.content()));
            }
        }

        // 1.3) Tools (se houver)
        List<Map<String, Object>> toolsSpec = null;
        if (req.tools() != null && !req.tools().isEmpty()) {
            toolsSpec = req.tools().stream().map(td -> Map.of(
                    "type", "function",
                    "function", Map.of(
                            "name", td.name(),
                            "description", td.description(),
                            "parameters", td.jsonSchema()
                    ))).toList();
        }
        String ferramentaMelhoresTarifas = req.tools() != null
                && req.tools().size() == 1
                && isFerramentaMelhoresTarifas(req.tools().get(0).name())
                && (ultimaMensagemTemRotaDeTarifa(req.messages())
                || contextoLocalTarifasTemRota(req.metadata()))
                ? req.tools().get(0).name()
                : null;
        boolean forcarConsultaMelhoresTarifas = ferramentaMelhoresTarifas != null;

        // 2) Loop de execução até não haver mais tool_calls
        List<ToolCallDTO> collectedToolCalls = new ArrayList<>();
        List<ChatActionDTO> toolActions = new ArrayList<>();
        String assistantContentFinal = null;
        String completionId = null;

        while (true) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", model);
            payload.put("messages", workingMessages);
            if (toolsSpec != null) {
                payload.put("tools", toolsSpec);
                if (forcarConsultaMelhoresTarifas) {
                    payload.put("tool_choice", collectedToolCalls.isEmpty()
                            ? Map.of(
                                    "type", "function",
                                    "function", Map.of(
                                            "name", ferramentaMelhoresTarifas))
                            : "none");
                }
            }

            Request request = new Request.Builder()
                    .url(props.getBaseUrl() + "/v1/chat/completions")
                    .post(RequestBody.create(
                            MediaType.parse("application/json"),
                            om.writeValueAsBytes(payload)))
                    .build();

            try (Response r = client.newCall(request).execute()) {
                String json = Objects.requireNonNull(r.body()).string();
                JsonNode root = om.readTree(json);
                completionId = root.path("id").asText();

                JsonNode choice = root.path("choices").get(0);
                JsonNode msgNode = choice.path("message");
                String assistantContent = msgNode.path("content").asText(null);

                // Verifica tool_calls
                JsonNode tc = msgNode.path("tool_calls");
                boolean hasToolCalls = tc.isArray() && tc.size() > 0;

                if (hasToolCalls) {
                    boolean executouMelhoresTarifas = false;
                    Map<String, Object> assistantToolMessage = new LinkedHashMap<>();
                    assistantToolMessage.put("role", "assistant");
                    if (assistantContent != null) {
                        assistantToolMessage.put("content", assistantContent);
                    }
                    assistantToolMessage.put(
                            "tool_calls",
                            om.convertValue(
                                    tc,
                                    new TypeReference<List<Map<String, Object>>>() {
                                    }));
                    workingMessages.add(assistantToolMessage);

                    // Para cada tool_call: executa e devolve role:"tool"
                    for (JsonNode n : tc) {
                        String name = n.path("function").path("name").asText();
                        String argsStr = n.path("function").path("arguments").asText("{}");
                        String toolCallId = n.path("id").asText(); // alguns providers retornam

                        Map<String, Object> args = om.readValue(argsStr, new com.fasterxml.jackson.core.type.TypeReference<>() {});
                        if ("search_cheapest_airfares".equals(name)) {
                            args = completarArgumentosComContextoLocalTarifas(
                                    args, req.metadata(), req.messages());
                        } else if ("search_cheapest_roundtrip_airfares".equals(name)) {
                            args = completarArgumentosComContextoLocalTarifasIdaVolta(
                                    args, req.metadata(), req.messages());
                        }
                        Map<String, Object> result = tools.execute(name, args);
                        collectedToolCalls.add(new ToolCallDTO(name, result));
                        executouMelhoresTarifas |= isFerramentaMelhoresTarifas(name);

                        String toolContent = om.writeValueAsString(result);
                        Map<String, Object> toolMsg = new HashMap<>();
                        toolMsg.put("role", "tool");
                        toolMsg.put("name", name);
                        toolMsg.put("content", toolContent);
                        if (toolCallId != null && !toolCallId.isEmpty()) {
                            toolMsg.put("tool_call_id", toolCallId);
                        }
                        workingMessages.add(toolMsg);
                    }

                    if (executouMelhoresTarifas) {
                        ToolCallDTO ultimaTool = collectedToolCalls.get(
                                collectedToolCalls.size() - 1);
                        Object mensagem = ultimaTool.arguments().get("mensagem");
                        assistantContentFinal = mensagem == null ? "" : mensagem.toString();
                        break;
                    }

                    // Continua o loop: o modelo responderá agora já “vendo” os resultados das tools
                    continue;
                }

                // Sem tool_calls → resposta final
                assistantContentFinal = (assistantContent != null) ? assistantContent : "";
                break;
            }
        }

        // 3) Constrói histórico atualizado para retornar
        List<ChatMessageDTO> updatedHistory = new ArrayList<>(baseHistory);
        if (req.messages() != null && !req.messages().isEmpty()) {
            updatedHistory.addAll(req.messages());
        }
        updatedHistory.add(new ChatMessageDTO("assistant", assistantContentFinal));

        if (!collectedToolCalls.isEmpty()) {
            ToolCallDTO lastTool = collectedToolCalls.get(collectedToolCalls.size() - 1);

            if ("search_hotels".equals(lastTool.name()) || "search_flights".equals(lastTool.name())) {
                assistantContentFinal = om.writeValueAsString(lastTool.arguments());
            } else if (isFerramentaMelhoresTarifas(lastTool.name())) {
                Object mensagem = lastTool.arguments().get("mensagem");
                if (mensagem != null && !mensagem.toString().isBlank()) {
                    assistantContentFinal = mensagem.toString();
                }
                Object actions = lastTool.arguments().get("actions");
                if (actions != null) {
                    toolActions = om.convertValue(
                            actions,
                            new TypeReference<List<ChatActionDTO>>() {
                            });
                }
            }
        }
        if (!updatedHistory.isEmpty()) {
            updatedHistory.set(
                    updatedHistory.size() - 1,
                    new ChatMessageDTO("assistant", assistantContentFinal));
        }

        return new ChatResponseDTO(
                completionId,
                assistantContentFinal,
                collectedToolCalls,
                null,
                keywords == null ? new ArrayList<>() : keywords,
                updatedHistory,
                toolActions
        );
    }


    public Flux<String> stream(ChatRequestDTO req) {
        // faz chamada SSE (stream=true) e emite os deltas como texto
        return Flux.create(sink -> {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("model", Optional.ofNullable(req.model()).orElse(props.getChatModel()));
                body.put("stream", true);
                body.put("messages", req.messages().stream()
                        .map(m -> Map.of("role", m.role(), "content", m.content()))
                        .toList());

                Request request = new Request.Builder()
                        .url(props.getBaseUrl() + "/v1/chat/completions")
                        .post(RequestBody.create(
                                MediaType.parse("application/json"),
                                new ObjectMapper().writeValueAsBytes(body)))
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        sink.error(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try (ResponseBody rb = response.body()) {
                            BufferedSource src = rb.source();
                            while (!src.exhausted()) {
                                String line = src.readUtf8LineStrict();
                                if (line.startsWith("data: ")) {
                                    String data = line.substring(6).trim();
                                    if ("[DONE]".equals(data)) break;
                                    sink.next(data);
                                }
                            }
                            sink.complete();
                        }
                    }
                });
            } catch (Exception ex) {
                sink.error(ex);
            }
        });
    }

    public List<String> actionApis(List<ChatMessageDTO> messages, ConversationRequestDTO req) {
        return actionApis(messages, req, null);
    }

    public List<String> actionApis(List<ChatMessageDTO> messages,
                                   ConversationRequestDTO req,
                                   String keywordDecidida) {
        String keyword = keywordDecidida == null || keywordDecidida.isBlank()
                ? inferirKeywordOperacional(req.input())
                : keywordDecidida.trim();
        boolean intencaoDeterministica = keyword != null;
        if (keyword == null) {
            keyword = "desconhecido";
            try {
                keyword = conversationAgentIA(req.input());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        List<String> keywords = Optional.ofNullable(req.keywords()).orElseGet(ArrayList::new);
        String localizadorReserva = null;
        boolean contextoReservaAerea = keywords.contains("reserva_aerea_detalhes") || keywords.contains("reserva_aerea_regras");
        if (isKeywordSeletorRemarcacao(keyword)) {
            localizadorReserva = extrairLocalizadorDeterministico(req.input());
            messages.add(montarMensagemSeletorRemarcacao(localizadorReserva));
        } else if (deveTentarCarregarReservaAerea(req.input(), keyword) || contextoReservaAerea) {
            localizadorReserva = intencaoDeterministica
                    ? extrairLocalizadorDeterministico(req.input())
                    : extrairLocalizador(req.input());
            if (localizadorReserva != null && "desconhecido".equals(keyword)) {
                keyword = keywords.contains("reserva_aerea_regras") ? "reserva_aerea_regras" : "reserva_aerea_detalhes";
            } else if (localizadorReserva != null && "reserva_aerea_detalhes".equals(keyword)) {
                keyword = "reserva_aerea_detalhes";
            }
        }
        /*
*   - "limites"
            - "faturas"
            - "boletos"
            - "checkin"
            - "ultimas_vendas"
            - "ultimas_reservas_aereas"
            - "reserva_aerea_detalhes"
            - "familias"
* */


        if (keyword.equals("alertas") && !keywords.contains(keyword)) {
            List<AlertaTarifaDTO> alertaTarifaDTOList = alertaTarifaService.listarPorUsuario(req.codgUsuario().intValue());
            AlertaTarifaIAResponse alertaTarifaIAResponse = new AlertaTarifaIAResponse();
            alertaTarifaIAResponse.getTarifas().addAll(alertaTarifaDTOList);
           // System.out.println("AlertaTarifaDTO: " + alertaTarifaIAResponse.toString());
            messages.add(new ChatMessageDTO("system", "Dado do sistema: " + alertaTarifaIAResponse.toString()));
        }

        if (keyword.equals("desconhecido") && !keywords.contains(keyword)) {
            List<ChatMemoria> chatMemorias = chatMemoriaService.findByBase(req.unidade());
            for (ChatMemoria chtMemoria : chatMemorias) {
             //   System.out.println("Memoria: " + chtMemoria.getText());
                messages.add(new ChatMessageDTO("system", "Dado do sistema: " + chtMemoria.getText()));
            }
        }
        boolean consultaFinanceiraBloqueada = isKeywordFinanceira(keyword)
                && !keywords.contains(keyword)
                && !possuiContextoFinanceiroDaAgencia(req);
        if (consultaFinanceiraBloqueada) {
            messages.add(new ChatMessageDTO("system",
                    "Dado do sistema: a consulta financeira nao foi executada porque a agencia "
                            + "autenticada nao possui identificacao ERP valida. Nao utilize dados "
                            + "de outra agencia ou da unidade como alternativa."));
        }

        if (keyword.equals("limites") && !keywords.contains(keyword) && !consultaFinanceiraBloqueada) {
            /*Consultar limites de credito*/
           // System.out.println("Limite Erp: " + req.idErp());
            Disponibilidade limitesDisponiveis = limitesService.consultaLimiteApi(new LimiteCreditoRQ(req.idErp()));
            messages.add(new ChatMessageDTO("system", "Dado do sistema: " + limitesDisponiveis.gerarResumoLimites()));

        }
        if (keyword.equals("faturas") && !keywords.contains(keyword) && !consultaFinanceiraBloqueada) {
            /* Consultar Faturas*/
            // montarMensagemFaturas(req);
            messages.add(montarMensagemFaturas(req));
        }

        if (keyword.equals("boletos") && !keywords.contains(keyword) && !consultaFinanceiraBloqueada) {
            /* Consultar Boletos*/
            messages.add(montarMensagemFaturasBoleto(req));
            // montarMensagemFaturasBoleto(req);
        }
        if (keyword.equals("checkin") && !keywords.contains(keyword)) {

            /*Consultar Checkin proximos 72 horas*/
            messages.add(buscarCheckinsProximos(req));
        }
        if ((keyword.equals("ultimas_reservas_aereas") || keyword.equals("ultimas_vendas")) && !keywords.contains(keyword)) {
            /*Consultar ultimas reservas aereas do usuario*/
            messages.add(listarUltimasVendas(req));
        }
        if ("reserva_aerea_detalhes".equals(keyword) && (localizadorReserva != null || !keywords.contains(keyword))) {
            messages.add(carregarDadosReservaAerea(req, localizadorReserva));
        }
        if ("reserva_aerea_regras".equals(keyword) && (localizadorReserva != null || !keywords.contains(keyword))) {
            messages.add(carregarReservaAereaComRegras(req, localizadorReserva));
        }
        if (keyword.contains("familias") && !keywords.contains(keyword)) {
            String[] partes = keyword.split(";");

            if (partes.length >= 2 && !partes[1].trim().isEmpty()) {
                messages.add(listarFamilias(req, partes[1].trim()));
            } else {
               // System.out.println("Keyword em formato inválido para familias: " + keyword);
            }
        }

        keywords.removeIf(Objects::isNull);
        if (!keywords.contains(keyword)) {
            keywords.add(keyword);
        }
        return keywords;
    }

    private boolean isKeywordFinanceira(String keyword) {
        return "limites".equals(keyword) || "faturas".equals(keyword) || "boletos".equals(keyword);
    }

    private boolean possuiContextoFinanceiroDaAgencia(ConversationRequestDTO req) {
        if (req == null || req.codgAgencia() == null || req.codgAgencia() <= 0
                || req.idErp() == null || req.idErp().trim().isEmpty()) {
            return false;
        }
        return !"confia".equalsIgnoreCase(req.idErp().trim());
    }

    public boolean isListagemReservasRecentesDeterministica(String input) {
        return isKeywordUltimasReservasAereas(classificarIntencaoOperacionalDeterministica(input));
    }

    public String identificarKeywordOperacionalDeterministica(String input) {
        return classificarIntencaoOperacionalDeterministica(input);
    }

    String classificarIntencaoOperacionalDeterministica(String input) {
        return inferirKeywordOperacional(input);
    }

    public String identificarAcaoSolicitadaDeterministica(String input) {
        String keyword = classificarIntencaoOperacionalDeterministica(input);
        if (keyword == null || isKeywordUltimasReservasAereas(keyword)) {
            return null;
        }

        String localizador = extrairLocalizadorDeterministico(input);
        if (localizador == null) {
            return null;
        }

        String normalizado = normalizarTexto(input);
        if (possuiNegacaoExplicitaDeAcao(normalizado)) {
            return null;
        }
        if ("simular_remarcacao".equals(keyword)) {
            return "simular_remarcacao";
        }
        if ("reserva_aerea_regras".equals(keyword)) {
            return contemAlgum(normalizado, "cancelar", "cancele", "cancelamento")
                    ? "preparar_cancelamento"
                    : "consultar_regras";
        }
        if ("reserva_aerea_detalhes".equals(keyword)
                && contemAlgum(normalizado, "abrir", "abra", "abre")) {
            return "abrir_reserva";
        }
        return null;
    }

    private boolean possuiNegacaoExplicitaDeAcao(String textoNormalizado) {
        if (textoNormalizado == null || textoNormalizado.isBlank()) {
            return false;
        }
        return Pattern.compile(
                        "\\bnao\\b(?:\\s+(?:quero|desejo|pretendo|vou|preciso))?"
                                + "(?:\\s+[a-z0-9]+){0,3}\\s+"
                                + "(?:abrir|abra|simular|simule|remarcar|remarque|cancelar|cancele)\\b")
                .matcher(textoNormalizado)
                .find();
    }

    public ChatResponseDTO responderListagemReservasRecentes(ConversationRequestDTO req) {
        ChatMessageDTO payload = listarUltimasVendas(req);
        List<ChatMessageDTO> structuredHistory = List.of(payload);
        return new ChatResponseDTO(
                null,
                null,
                new ArrayList<>(),
                null,
                List.of("ultimas_reservas_aereas"),
                structuredHistory,
                extrairAcoesDisponiveis(structuredHistory)
        );
    }

    public List<ChatActionDTO> extrairAcoesDisponiveis(List<ChatMessageDTO> messages) {
        List<ChatActionDTO> actions = new ArrayList<>();
        Set<String> adicionadas = new HashSet<>();
        if (messages == null) {
            return actions;
        }

        for (ChatMessageDTO message : messages) {
            if (message == null || !"system".equals(message.role()) || message.content() == null) {
                continue;
            }
            JsonNode root = extrairJsonDadoSistema(message.content());
            if (root == null) {
                continue;
            }

            String localizadorConsultado = root.path("localizadorConsultado").asText(null);
            String localizadorContexto = root.path("localizadorContexto").asText(localizadorConsultado);
            adicionarChatActions(actions, adicionadas, root.path("acoesDisponiveis"), localizadorContexto);
            adicionarChatActions(actions, adicionadas, root.path("actions"), localizadorContexto);

            JsonNode reservasRecentes = root.path("reservasRecentes").path("reservas");
            adicionarAcoesReservas(actions, adicionadas, reservasRecentes, localizadorConsultado);

            JsonNode reservas = root.path("reservas");
            adicionarAcoesReservas(actions, adicionadas, reservas, localizadorConsultado);
        }

        return actions;
    }

    private void adicionarAcoesReservas(List<ChatActionDTO> actions,
                                        Set<String> adicionadas,
                                        JsonNode reservas,
                                        String localizadorPadrao) {
        if (reservas == null || !reservas.isArray()) {
            return;
        }
        for (JsonNode reserva : reservas) {
            String localizador = reserva.path("localizador").asText(localizadorPadrao);
            adicionarChatActions(actions, adicionadas, reserva.path("acoesDisponiveis"), localizador);
            adicionarChatActions(actions, adicionadas, reserva.path("actions"), localizador);
        }
    }

    private void adicionarChatActions(List<ChatActionDTO> actions, Set<String> adicionadas, JsonNode acoes, String localizador) {
        if (acoes == null || !acoes.isArray()) {
            return;
        }

        for (JsonNode acao : acoes) {
            ChatActionDTO action = montarChatActionDTO(acao, localizador);
            if (action == null) {
                continue;
            }
            String chave = action.code() + "|" + action.localizador() + "|" + action.reservaId();
            if (adicionadas.add(chave)) {
                actions.add(action);
            }
        }
    }

    private ChatActionDTO montarChatActionDTO(JsonNode acao, String localizador) {
        if (acao == null || acao.isMissingNode()) {
            return null;
        }

        String code = textoAcao(acao, "codigo", "code");
        if (code == null || code.isBlank()) {
            return null;
        }

        String label = primeiroTextoAcao(acao, code, "titulo", "label");
        String description = primeiroTextoAcao(acao, "", "descricao", "description");
        Boolean requiresConfirmation = primeiroBooleanAcao(
                acao, "precisaConfirmacao", "requiresConfirmation");
        Boolean requiresRules = primeiroBooleanAcao(
                acao, "exigeConsultaRegras", "requiresRules");
        Boolean sensitive = primeiroBooleanAcao(acao, "operacaoSensivel", "sensitive");
        String localizadorAcao = primeiroTextoAcao(acao, localizador, "localizador");
        Integer reservaId = acao.hasNonNull("reservaId") ? acao.path("reservaId").asInt() : null;
        String prompt = primeiroTextoAcao(acao, null, "prompt");
        if (prompt == null || prompt.isBlank()) {
            prompt = montarPromptAcaoChat(code, label, localizadorAcao,
                    requiresConfirmation, requiresRules, sensitive);
        }

        return new ChatActionDTO(
                code,
                label,
                description,
                localizadorAcao,
                reservaId,
                requiresConfirmation,
                requiresRules,
                sensitive,
                prompt
        );
    }

    private String textoAcao(JsonNode node, String... fields) {
        return primeiroTextoAcao(node, null, fields);
    }

    private String primeiroTextoAcao(JsonNode node, String valorPadrao, String... fields) {
        if (node != null) {
            for (String field : fields) {
                if (node.hasNonNull(field)) {
                    String value = node.path(field).asText(null);
                    if (value != null && !value.isBlank()) {
                        return value;
                    }
                }
            }
        }
        return valorPadrao;
    }

    private Boolean primeiroBooleanAcao(JsonNode node, String... fields) {
        if (node != null) {
            for (String field : fields) {
                if (node.has(field)) {
                    return node.path(field).asBoolean(false);
                }
            }
        }
        return false;
    }

    private String montarPromptAcaoChat(String code, String label, String localizador,
                                        Boolean requiresConfirmation, Boolean requiresRules, Boolean sensitive) {
        String loc = localizador == null || localizador.isBlank() ? "" : " " + localizador;
        String prompt = switch (code) {
            case "consultar_regras" ->
                    "Consulte as regras, multas, reembolso, cancelamento e alteracao/remarcacao da reserva" + loc + ".";
            case "abrir_reserva" ->
                    "Abrir a reserva" + loc + " no sistema.";
            case "preparar_emissao" ->
                    "A emissao nao e executada pelo chat. Abra a reserva" + loc
                            + " no sistema para consultar e concluir a emissao, se ela estiver disponivel.";
            case "consultar_bilhetes" ->
                    "Mostre os bilhetes e e-tickets da reserva" + loc + ".";
            case "preparar_reenvio_voucher" ->
                    "Quero preparar o reenvio do voucher ou e-ticket da reserva" + loc + ".";
            case "consultar_assentos" ->
                    "Consulte os assentos disponiveis da reserva" + loc + ".";
            case "consultar_checkin" ->
                    "Consulte a possibilidade de check-in da reserva" + loc + ".";
            case "preparar_cancelamento" ->
                    "Quero preparar o cancelamento da reserva" + loc + ". Consulte as regras antes.";
            case "preparar_reembolso" ->
                    "Quero preparar o reembolso da reserva" + loc + ". Consulte as regras antes.";
            case "preparar_alteracao" ->
                    "Quero preparar a alteracao ou remarcacao da reserva" + loc + ". Consulte as regras antes.";
            case "selecionar_reserva_remarcacao" ->
                    "Abra o seletor de reservas emitidas para eu escolher qual desejo remarcar.";
            case "simular_remarcacao" ->
                    "Abra o seletor de reservas emitidas e use o localizador" + loc
                            + " apenas para preencher a busca. Nao inicie a simulacao antes da minha selecao.";
            default ->
                    label + " da reserva" + loc + ".";
        };

        if (Boolean.TRUE.equals(sensitive) || Boolean.TRUE.equals(requiresConfirmation)) {
            prompt += " Nao execute a operacao ainda; apenas prepare, explique os impactos e peca minha confirmacao explicita.";
        }
        if (Boolean.TRUE.equals(requiresRules)) {
            prompt += " Se ainda nao houver regras carregadas, consulte as regras primeiro.";
        }
        return prompt;
    }

    private JsonNode extrairJsonDadoSistema(String content) {
        int inicio = content.indexOf('{');
        if (inicio < 0) {
            return null;
        }

        int fim = localizarFimObjetoJson(content, inicio);
        if (fim <= inicio) {
            return null;
        }

        try {
            return mapper.readTree(content.substring(inicio, fim + 1));
        } catch (Exception e) {
            return null;
        }
    }

    private int localizarFimObjetoJson(String text, int inicio) {
        int profundidade = 0;
        boolean emString = false;
        boolean escape = false;

        for (int i = inicio; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '"') {
                emString = !emString;
                continue;
            }
            if (emString) {
                continue;
            }
            if (c == '{') {
                profundidade++;
            } else if (c == '}') {
                profundidade--;
                if (profundidade == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String inferirKeywordOperacional(String input) {
        String normalizado = normalizarTexto(input);
        if (normalizado.isBlank()) {
            return null;
        }

        if (contemAlgum(normalizado, "limite", "limites")
                && contemAlgum(normalizado, "credito", "disponivel", "saldo")) {
            return "limites";
        }
        if (contemAlgum(normalizado, "boleto", "boletos", "linha digitavel")) {
            return "boletos";
        }
        if (contemAlgum(normalizado, "fatura", "faturas")) {
            return "faturas";
        }

        String localizadorDeterministico = extrairLocalizadorDeterministico(input);

        boolean perguntaSobreRegra = contemAlgum(normalizado,
                "multa", "regra", "taxa", "penalidade", "reembolso", "cancelamento", "cancelar",
                "quanto custa", "qual valor");
        boolean expressaoDeVontade = contemAlgum(normalizado,
                "quero", "preciso", "desejo", "gostaria", "fazer", "iniciar");
        boolean intencaoOperacionalRemarcacao = normalizado.contains("remarcar")
                || (normalizado.contains("simular")
                && contemAlgum(normalizado, "remarcacao", "alteracao", "alterar", "mudanca", "troca"))
                || (normalizado.contains("remarcacao")
                && expressaoDeVontade)
                || (expressaoDeVontade && normalizado.contains("alterar")
                && contemAlgum(normalizado, "data", "voo", "reserva", "passagem"));
        boolean pedidoRemarcacao = intencaoOperacionalRemarcacao && !perguntaSobreRegra;
        if (pedidoRemarcacao) {
            return localizadorDeterministico == null
                    ? "selecionar_reserva_remarcacao" : "simular_remarcacao";
        }

        boolean assuntoReservaAerea = contemAlgum(normalizado,
                "reserva", "localizador", "bilhete", "passagem", "aereo", "voo");
        boolean assuntoRegra = contemAlgum(normalizado,
                "regra", "multa", "reembolso", "reembolsar", "alteracao", "alterar",
                "remarcacao", "remarcar", "cancelamento", "cancelar", "penalidade");
        boolean pedidoAbrir = contemAlgum(normalizado, "abrir", "abra", "abre");
        boolean comandoDadosReserva = contemAlgum(normalizado,
                "dados", "detalhe", "detalhes", "carrega", "carregar", "consulta", "consultar",
                "ver", "mostre", "mostrar", "visualizar", "informacao", "informacoes", "status",
                "emitir", "emissao", "assento", "assentos", "bilhete", "bilhetes", "eticket",
                "voucher", "acoes", "acao", "fazer", "checkin");

        if (localizadorDeterministico != null && assuntoRegra) {
            return "reserva_aerea_regras";
        }
        if (localizadorDeterministico != null
                && (pedidoAbrir || assuntoReservaAerea || comandoDadosReserva)) {
            return "reserva_aerea_detalhes";
        }

        if (assuntoReservaAerea && assuntoRegra) {
            return "reserva_aerea_regras";
        }
        boolean assuntoListagemReserva = contemAlgum(normalizado,
                "reserva", "reservas", "venda", "vendas", "passagem", "passagens", "aereo", "aereas", "voo", "voos");
        boolean referenciaRecencia = contemAlgum(normalizado,
                "ultima", "ultimas", "recente", "recentes");
        boolean assuntoPlural = contemAlgum(normalizado,
                "reservas", "vendas", "passagens", "voos");
        boolean comandoListagem = contemAlgum(normalizado,
                "listar", "liste", "lista", "relacao", "mostre", "mostrar", "visualizar",
                "exibir", "minhas", "meus", "quais");
        boolean pedidoListagemRecente = referenciaRecencia
                || (comandoListagem && assuntoPlural);

        if (pedidoListagemRecente && assuntoListagemReserva) {
            return "ultimas_reservas_aereas";
        }
        boolean pedidoDadosReserva = assuntoReservaAerea
                && (pedidoAbrir || comandoDadosReserva);

        if (pedidoDadosReserva) {
            return "reserva_aerea_detalhes";
        }
        return null;
    }

    private boolean isKeywordUltimasReservasAereas(String keyword) {
        return "ultimas_reservas_aereas".equals(keyword) || "ultimas_vendas".equals(keyword);
    }

    private boolean isKeywordCarregarReservaAerea(String keyword) {
        return "reserva_aerea_regras".equals(keyword) || "reserva_aerea_detalhes".equals(keyword);
    }

    private boolean isKeywordSeletorRemarcacao(String keyword) {
        return "selecionar_reserva_remarcacao".equals(keyword) || "simular_remarcacao".equals(keyword);
    }

    private boolean deveTentarCarregarReservaAerea(String input, String keyword) {
        if (isKeywordCarregarReservaAerea(keyword)) {
            return true;
        }
        if (input == null || input.isBlank() || isKeywordUltimasReservasAereas(keyword)) {
            return false;
        }
        String normalizado = normalizarTexto(input);
        return contemAlgum(normalizado, "localizador", "reserva", "bilhete", "passagem", "aereo", "voo");
    }

    private boolean contemAlgum(String texto, String... termos) {
        for (String termo : termos) {
            if (texto.contains(termo)) {
                return true;
            }
        }
        return false;
    }

    private String normalizarTexto(String value) {
        if (value == null) {
            return "";
        }
        String semAcento = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT);
    }

    public ChatMessageDTO carregarReservaAereaComRegras(ConversationRequestDTO req) {
        return carregarReservaAereaComRegras(req, extrairLocalizador(req.input()));
    }

    public ChatMessageDTO carregarReservaAereaComRegras(ConversationRequestDTO req, String localizador) {
        if (localizador == null) {
            return new ChatMessageDTO("system",
                    "Dado do sistema (reserva_aerea_regras): o usuario quer consultar dados, regras, multas, reembolso, cancelamento ou remarcacao de uma reserva aerea, " +
                            "mas nao informou um localizador claro. Peca o localizador antes de afirmar regras ou valores.");
        }

        try {
            ConsultarLocalizadorRequest request = montarRequestConsultaLocalizador(req, localizador);
            ConsultarLocalizadorResponse response = regrasReservaService.enriquecer(aereoClient.carregarReserva(request));
            Map<String, Object> resumo = resumirReservaAereaComRegras(localizador, response);
            return new ChatMessageDTO("system",
                    "Dado do sistema (reserva_aerea_regras): " + mapper.writeValueAsString(resumo) +
                            "\nResponda em texto claro. Traga os dados da reserva e informe regras de cancelamento, reembolso e alteracao/remarcacao quando os dados existirem. " +
                            "Se a consulta falhar ou faltarem dados, explique a limitacao e solicite o dado faltante. " +
                            "Nao confirme cancelamento, reembolso ou remarcacao executados. Para cancelamento, apenas prepare a solicitacao, " +
                            "explique os impactos e exija confirmacao explicita antes de qualquer fluxo operacional.");
        } catch (Exception e) {
            return new ChatMessageDTO("system",
                    "Dado do sistema (reserva_aerea_regras): nao foi possivel carregar a reserva " + localizador +
                            " para consultar dados e regras agora. Oriente o usuario a conferir o localizador ou tentar novamente.");
        }
    }

    public ChatMessageDTO carregarDadosReservaAerea(ConversationRequestDTO req) {
        return carregarDadosReservaAerea(req, extrairLocalizador(req.input()));
    }

    public ChatMessageDTO carregarDadosReservaAerea(ConversationRequestDTO req, String localizador) {
        if (localizador == null) {
            return new ChatMessageDTO("system",
                    "Dado do sistema (reserva_aerea_detalhes): o usuario quer consultar os dados de uma reserva aerea, " +
                            "mas nao informou um localizador claro. Peca o localizador antes de afirmar dados da reserva.");
        }

        try {
            ConsultarLocalizadorRequest request = montarRequestConsultaLocalizador(req, localizador);
            ConsultarLocalizadorResponse response = aereoClient.carregarReserva(request);
            Map<String, Object> resumo = resumirReservaAereaDados(localizador, response);
            return new ChatMessageDTO("system",
                    "Dado do sistema (reserva_aerea_detalhes): " + mapper.writeValueAsString(resumo) +
                            "\nResponda em texto claro trazendo somente os dados da reserva encontrada: localizador, status, sistema, datas, prazo, passageiros, trechos/voos, bagagem, bilhetes e valores quando existirem. " +
                            "Quando existirem acoesDisponiveis ou alertasOperacionais, use esses dados para sugerir os proximos passos. " +
                            "Nao fale de regras, multa, reembolso, cancelamento ou remarcacao se estes dados nao foram solicitados nesta mensagem; nesse caso, ofereca apenas consultar as regras. " +
                            "A emissao nao e executada no chat. Se o usuario pedir emissao e a reserva permitir, informe que ele deve usar Abrir reserva para concluir no sistema; " +
                            "nao gere nem sugira uma acao preparar_emissao. Nao confirme cancelamento, reembolso, remarcacao ou reenvio executados sem confirmacao explicita do usuario. " +
                            "Se a consulta falhar ou faltarem dados, explique a limitacao e solicite o dado faltante.");
        } catch (Exception e) {
            return new ChatMessageDTO("system",
                    "Dado do sistema (reserva_aerea_detalhes): nao foi possivel carregar os dados da reserva " + localizador +
                            " agora. Oriente o usuario a conferir o localizador ou tentar novamente.");
        }
    }

    private ConsultarLocalizadorRequest montarRequestConsultaLocalizador(ConversationRequestDTO req, String localizador) {
        ConsultarLocalizadorRequest request = new ConsultarLocalizadorRequest();
        request.setSistema("Wooba");
        request.setLocalizador(localizador);

        com.confApi.aereo.dto.Agencia agencia = new com.confApi.aereo.dto.Agencia();
       agencia.setCodgAgencia(req.codgAgencia() == null ? null : String.valueOf(req.codgAgencia()));
      //  agencia.setCodgAgencia(req.idErp() == null ? null : String.valueOf(req.idErp()));
        agencia.setCodgSistemaBackoffice(req.idErp());
        agencia.setNome(req.codgAgencia() == null ? null : String.valueOf(req.codgAgencia()));
        agencia.setUnidade(req.unidade());
        request.setAgencia(agencia);

        IdentificacaoAgenciaModel identificacao = new IdentificacaoAgenciaModel();
        identificacao.setCodgAgencia(req.codgAgencia() == null ? null : req.codgAgencia().intValue());
        identificacao.setCodgUsuario(req.codgUsuario() == null ? null : req.codgUsuario().intValue());
        identificacao.setCodgErp(parseInteger(req.idErp()));
        request.setIdentificacaoAgenciaModel(null);
        return request;
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.replaceAll("\\D+", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ChatMessageDTO montarMensagemSeletorRemarcacao(String localizador) {
        String codigo = localizador == null
                ? "selecionar_reserva_remarcacao" : "simular_remarcacao";
        List<Map<String, Object>> acoes = new ArrayList<>();
        adicionarAcao(
                acoes,
                codigo,
                localizador == null ? "Selecionar reserva" : "Simular remarcacao",
                localizador == null
                        ? "Escolha uma reserva emitida da sua agencia para iniciar a simulacao."
                        : "Abra o seletor com o localizador informado como filtro inicial.",
                false,
                false,
                false);

        Map<String, Object> contexto = new LinkedHashMap<>();
        contexto.put("tipoConsulta", "seletor_remarcacao");
        if (localizador != null) {
            contexto.put("localizadorContexto", localizador);
        }
        contexto.put("acoesDisponiveis", acoes);
        try {
            return new ChatMessageDTO(
                    "system",
                    "Dado do sistema (seletor_remarcacao): " + mapper.writeValueAsString(contexto)
                            + "\nOriente o usuario a escolher uma reserva no seletor. "
                            + "Nao afirme que a simulacao foi iniciada apenas porque um localizador foi informado.");
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Nao foi possivel montar a acao de selecao da reserva.", ex);
        }
    }

    private String extrairLocalizadorDeterministico(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String texto = normalizarTexto(input).toUpperCase(Locale.ROOT);
        Matcher explicito = Pattern.compile(
                "\\b(?:LOCALIZADOR|RESERVA|PNR)\\s*(?:N(?:O|RO)?\\.?\\s*)?[:#-]?\\s*([A-Z0-9]{5,8})\\b",
                Pattern.CASE_INSENSITIVE)
                .matcher(texto);
        if (explicito.find()) {
            String candidato = explicito.group(1).toUpperCase(Locale.ROOT);
            if (!isPalavraComumLocalizador(candidato)) {
                return candidato;
            }
        }

        Matcher contextoOperacional = Pattern.compile(
                "\\b(?:ABRIR|ABRA|CANCELAR|CANCELE|REGRA|REGRAS|REMARCAR|REMARCACAO|SIMULAR)\\b"
                        + "(?:\\s+[A-Z]+){0,4}\\s+([A-Z0-9]{5,8})\\s*[?.!]*$")
                .matcher(texto);
        if (contextoOperacional.find()) {
            String candidato = contextoOperacional.group(1).toUpperCase(Locale.ROOT);
            if (!isPalavraComumLocalizador(candidato)) {
                return candidato;
            }
        }

        Matcher alfanumerico = Pattern.compile("\\b(?=[A-Z0-9]{5,8}\\b)(?=[A-Z0-9]*[A-Z])(?=[A-Z0-9]*[0-9])[A-Z0-9]+\\b")
                .matcher(texto);
        while (alfanumerico.find()) {
            String candidato = alfanumerico.group();
            if (!isPalavraComumLocalizador(candidato)) {
                return candidato;
            }
        }
        return null;
    }

    private boolean isPalavraComumLocalizador(String candidato) {
        return candidato == null || Set.of(
                "QUERO", "REGRA", "REGRAS", "MULTA", "AEREO", "AEREA", "VOOS", "VOO",
                "LOCALIZADOR", "RESERVA", "REEMBOLSO", "ALTERACAO", "REMARCACAO", "REMARCAR",
                "SIMULAR", "BILHETE", "PASSAGEM", "CANCELAMENTO", "POSSUI", "TENHO", "SABER",
                "DESEJO", "PRECISO", "GOSTARIA", "ALTERAR", "EMITIDA", "EMITIDO", "RECENTE",
                "RECENTES", "DESEJADA", "DESEJADO", "CANCELAR", "CANCELE", "ABRIR", "ABRA",
                "MOSTRAR", "MOSTRE", "VISUALIZAR", "ULTIMA", "ULTIMAS", "RESERVAS", "LISTAR",
                "MINHA", "MINHAS", "CONSULTAR", "EMITIR", "AGORA"
        ).contains(candidato);
    }

    private String extrairLocalizador(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String localizadorDeterministico = extrairLocalizadorDeterministico(input);
        if (localizadorDeterministico != null) {
            return localizadorDeterministico;
        }

        String localizadorIA = extrairLocalizadorIA(input);
        if (localizadorIA != null) {
            return localizadorIA;
        }

        return extrairLocalizadorPorRegex(input);
    }

    private String extrairLocalizadorIA(String input) {
        try {
            String resposta = agenteIADecisor("ExtratorLocalizadorIA", profileExtratorLocalizadorIA(), input);
            return normalizarLocalizadorExtraido(resposta);
        } catch (Exception e) {
            return null;
        }
    }

    private String profileExtratorLocalizadorIA() {
        return """
                Voce e um extrator de localizador de reserva aerea.
                Leia a mensagem do usuario e retorne somente o localizador mais provavel.

                Regras:
                - Responda apenas com o codigo do localizador, sem frases, sem JSON, sem markdown e sem pontuacao.
                - O localizador costuma ter de 5 a 8 caracteres alfanumericos.
                - Converta letras para maiusculas.
                - Ignore palavras comuns como reserva, localizador, multa, regra, reembolso, alteracao, remarcacao, passagem e bilhete.
                - Se nao houver localizador claro, retorne vazio.

                Exemplos:
                Usuario: "ver regras do localizador abc123"
                Resposta: ABC123

                Usuario: "a reserva X7K9PQ tem multa?"
                Resposta: X7K9PQ
                """;
    }

    private String normalizarLocalizadorExtraido(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalizado = value
                .replace("`", "")
                .replace("\"", "")
                .trim()
                .toUpperCase(Locale.ROOT);
        if (normalizado.isBlank() || normalizado.equals("NULL") || normalizado.equals("NULO")) {
            return null;
        }

        Matcher matcher = Pattern.compile("\\b[A-Z0-9]{5,8}\\b").matcher(normalizado);
        return matcher.find() ? matcher.group() : null;
    }

    private String extrairLocalizadorPorRegex(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        Matcher matcher = Pattern.compile("\\b[A-Z0-9]{5,8}\\b").matcher(input.toUpperCase(Locale.ROOT));
        while (matcher.find()) {
            String candidato = matcher.group();
            if (!isPalavraComumLocalizador(candidato)) {
                return candidato;
            }
        }
        return null;
    }

    private Map<String, Object> resumirReservaAereaComRegras(String localizador, ConsultarLocalizadorResponse response) {
        Map<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("tipoConsulta", "reserva_aerea_regras");
        resumo.put("localizadorConsultado", localizador);
        resumo.put("statusConsulta", response == null || response.getException() != null ? "ERRO_CONSULTA" : "OK");
        if (response != null && response.getException() != null) {
            resumo.put("erro", response.getException());
        }

        List<Map<String, Object>> reservas = new ArrayList<>();
        if (response != null && response.getReservas() != null) {
            for (Reserva reserva : response.getReservas()) {
                reservas.add(resumirReserva(reserva));
            }
        }
        resumo.put("quantidadeReservas", reservas.size());
        resumo.put("reservas", reservas);
        return resumo;
    }

    private Map<String, Object> resumirReservaAereaDados(String localizador, ConsultarLocalizadorResponse response) {
        Map<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("tipoConsulta", "reserva_aerea_detalhes");
        resumo.put("localizadorConsultado", localizador);
        resumo.put("statusConsulta", response == null || response.getException() != null ? "ERRO_CONSULTA" : "OK");
        if (response != null && response.getException() != null) {
            resumo.put("erro", response.getException());
        }

        List<Map<String, Object>> reservas = new ArrayList<>();
        if (response != null && response.getReservas() != null) {
            for (Reserva reserva : response.getReservas()) {
                reservas.add(resumirReservaDados(reserva));
            }
        }
        resumo.put("quantidadeReservas", reservas.size());
        resumo.put("reservas", reservas);
        return resumo;
    }

    private Map<String, Object> resumirReserva(Reserva reserva) {
        Map<String, Object> map = new LinkedHashMap<>(resumirReservaDados(reserva));
        if (reserva == null) {
            return map;
        }
        map.put("cancelamento", resumirCancelamento(reserva));
        map.put("regrasAereas", resumirRegras(reserva.getRegrasAereas()));
        return map;
    }

    private Map<String, Object> resumirReservaDados(Reserva reserva) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (reserva == null) {
            return map;
        }
        putIfNotNull(map, "localizador", reserva.getLocalizador());
        putIfNotNull(map, "status", reserva.getStatus());
        putIfNotNull(map, "sistema", reserva.getSistema());
        putIfNotNull(map, "dataEmissao", reserva.getDataEmissao());
        putIfNotNull(map, "dataCriacao", reserva.getDataCriacao());
        putIfNotBlank(map, "prazoEmissao", reserva.getPrazoEmissao());
        putIfNotNull(map, "permiteEmitir", reserva.getPermiteEmitir());
        putIfNotNull(map, "permiteCancelar", reserva.getPermiteCancelar());
        putIfNotNull(map, "mapaDeAssentosDisponivel", reserva.getMapaDeAssentosDisponivel());
        map.put("quantidadePassageiros", reserva.getPassageiros() == null ? 0 : reserva.getPassageiros().size());
        List<Map<String, Object>> passageiros = resumirPassageirosHub(reserva.getPassageiros());
        if (!passageiros.isEmpty()) {
            map.put("passageiros", passageiros);
        }
        map.put("quantidadeTrechos", reserva.getViagens() == null ? 0 : reserva.getViagens().size());
        List<Map<String, Object>> viagens = resumirViagensHub(reserva.getViagens());
        if (!viagens.isEmpty()) {
            map.put("viagens", viagens);
        }
        Map<String, Object> valores = resumirValorReserva(reserva.getValorReserva());
        if (!valores.isEmpty()) {
            map.put("valores", valores);
        }
        List<Map<String, Object>> acoesDisponiveis = montarAcoesDisponiveisReserva(reserva);
        if (!acoesDisponiveis.isEmpty()) {
            map.put("acoesDisponiveis", acoesDisponiveis);
        }
        List<Map<String, Object>> alertasOperacionais = montarAlertasOperacionaisReserva(reserva);
        if (!alertasOperacionais.isEmpty()) {
            map.put("alertasOperacionais", alertasOperacionais);
        }
        return map;
    }

    private List<Map<String, Object>> montarAcoesDisponiveisReserva(Reserva reserva) {
        if (reserva == null) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> acoes = montarAcoesDisponiveisLocalizador(reserva.getLocalizador());
        if (podeSimularRemarcacao(reserva)) {
            adicionarAcao(acoes, "simular_remarcacao", "Simular remarcacao",
                    "Pesquisar outro voo da mesma companhia e calcular uma previa da remarcacao.",
                    false, true, false);
        }
        if (!isReservaCancelada(reserva) && Boolean.TRUE.equals(reserva.getPermiteCancelar())) {
            adicionarAcao(acoes, "preparar_cancelamento", "Cancelar",
                    "Consultar as regras e preparar a solicitacao de cancelamento.",
                    true, true, true);
        }
        return acoes;
    }

    private boolean podeSimularRemarcacao(Reserva reserva) {
        return reserva != null
                && !isReservaCancelada(reserva)
                && isReservaEmitida(reserva)
                && possuiBilheteAtivo(reserva)
                && possuiVooFuturo(reserva)
                && companhiaSuportadaRemarcacao(reserva);
    }

    private boolean companhiaSuportadaRemarcacao(Reserva reserva) {
        if (reserva == null || reserva.getViagens() == null) {
            return false;
        }
        for (com.confApi.hub.aereo.dto.TrechoReserva trecho : reserva.getViagens()) {
            if (trecho == null || trecho.getCompanhia() == null
                    || trecho.getCompanhia().getCodigoIata() == null) {
                continue;
            }
            String iata = trecho.getCompanhia().getCodigoIata().trim().toUpperCase(Locale.ROOT);
            if (COMPANHIAS_REMARCACAO_SUPORTADAS.contains(iata)) {
                return true;
            }
        }
        return false;
    }

    private boolean possuiBilheteAtivo(Reserva reserva) {
        if (reserva == null || reserva.getPassageiros() == null) {
            return false;
        }
        for (com.confApi.hub.aereo.dto.Passageiro passageiro : reserva.getPassageiros()) {
            if (passageiro == null || passageiro.getBilhetes() == null) {
                continue;
            }
            for (com.confApi.hub.aereo.dto.Bilhete bilhete : passageiro.getBilhetes()) {
                if (bilhete == null) {
                    continue;
                }
                String status = normalizarTexto(bilhete.getStatus());
                if (!status.contains("cancel") && !status.contains("reembols")) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Map<String, Object>> montarAcoesDisponiveisLocalizador(String localizador) {
        List<Map<String, Object>> acoes = new ArrayList<>();
        if (localizador == null || localizador.isBlank()) {
            return acoes;
        }

        adicionarAcao(acoes, "consultar_regras", "Consultar regras",
                "Carregar multa, reembolso, cancelamento e alteracao/remarcacao desta reserva.",
                false, true, false);

        adicionarAcao(acoes, "abrir_reserva", "Abrir reserva",
                "Fechar o chat e abrir esta reserva aerea no sistema.",
                false, false, false);

        return acoes;
    }

    private void adicionarAcao(List<Map<String, Object>> acoes, String codigo, String titulo, String descricao,
                               boolean precisaConfirmacao, boolean exigeConsultaRegras, boolean operacaoSensivel) {
        Map<String, Object> acao = new LinkedHashMap<>();
        acao.put("codigo", codigo);
        acao.put("titulo", titulo);
        acao.put("descricao", descricao);
        acao.put("precisaConfirmacao", precisaConfirmacao);
        acao.put("exigeConsultaRegras", exigeConsultaRegras);
        acao.put("operacaoSensivel", operacaoSensivel);
        acoes.add(acao);
    }

    private List<Map<String, Object>> montarAlertasOperacionaisReserva(Reserva reserva) {
        List<Map<String, Object>> alertas = new ArrayList<>();
        if (reserva == null) {
            return alertas;
        }

        if (isReservaCancelada(reserva)) {
            adicionarAlerta(alertas, "RESERVA_CANCELADA", "Reserva com status de cancelamento.", reserva.getStatus());
        }
        if (isReservaEmitida(reserva)) {
            adicionarAlerta(alertas, "RESERVA_EMITIDA", "Reserva possui emissao ou bilhete.", null);
        } else if (Boolean.TRUE.equals(reserva.getPermiteEmitir())) {
            adicionarAlerta(alertas, "PENDENTE_EMISSAO",
                    "Reserva candidata a emissao. A emissao nao e executada pelo chat; abra a reserva no sistema.",
                    reserva.getPrazoEmissao());
        }
        if (reserva.getPrazoEmissao() != null && !reserva.getPrazoEmissao().isBlank()) {
            adicionarAlerta(alertas, "PRAZO_EMISSAO", "Existe prazo de emissao informado.", reserva.getPrazoEmissao());
        }
        Date proximaPartida = obterProximaPartida(reserva);
        if (proximaPartida != null && isDentroDasProximasHoras(proximaPartida, 72)) {
            adicionarAlerta(alertas, "VOO_PROXIMO_72H", "Ha voo nas proximas 72 horas.", proximaPartida);
        }
        if (reserva.getPassageiros() == null || reserva.getPassageiros().isEmpty()) {
            adicionarAlerta(alertas, "SEM_PASSAGEIROS", "A consulta nao retornou passageiros.", null);
        }
        if (!possuiVoos(reserva)) {
            adicionarAlerta(alertas, "SEM_VOOS", "A consulta nao retornou voos/trechos.", null);
        }
        return alertas;
    }

    private void adicionarAlerta(List<Map<String, Object>> alertas, String codigo, String mensagem, Object detalhe) {
        Map<String, Object> alerta = new LinkedHashMap<>();
        alerta.put("codigo", codigo);
        alerta.put("mensagem", mensagem);
        putIfNotNull(alerta, "detalhe", detalhe);
        alertas.add(alerta);
    }

    private boolean isReservaCancelada(Reserva reserva) {
        if (reserva == null || reserva.getStatus() == null) {
            return false;
        }
        String status = normalizarTexto(reserva.getStatus());
        return status.contains("cancel");
    }

    private boolean isReservaEmitida(Reserva reserva) {
        if (reserva == null) {
            return false;
        }
        String status = normalizarTexto(reserva.getStatus());
        boolean statusEmitido = "emitida".equals(status)
                || "emitido".equals(status)
                || "issued".equals(status)
                || "ticketed".equals(status);
        return statusEmitido || reserva.getDataEmissao() != null || possuiBilhetes(reserva);
    }

    private boolean possuiBilhetes(Reserva reserva) {
        if (reserva == null || reserva.getPassageiros() == null) {
            return false;
        }
        for (com.confApi.hub.aereo.dto.Passageiro passageiro : reserva.getPassageiros()) {
            if (passageiro != null && passageiro.getBilhetes() != null && !passageiro.getBilhetes().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean possuiVoos(Reserva reserva) {
        if (reserva == null || reserva.getViagens() == null) {
            return false;
        }
        for (com.confApi.hub.aereo.dto.TrechoReserva viagem : reserva.getViagens()) {
            if (viagem != null && viagem.getVoos() != null && !viagem.getVoos().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean possuiVooFuturo(Reserva reserva) {
        Date proximaPartida = obterProximaPartida(reserva);
        return proximaPartida != null && proximaPartida.after(new Date());
    }

    private boolean possuiVooProximo(Reserva reserva, int horas) {
        Date proximaPartida = obterProximaPartida(reserva);
        return proximaPartida != null && isDentroDasProximasHoras(proximaPartida, horas);
    }

    private Date obterProximaPartida(Reserva reserva) {
        if (reserva == null || reserva.getViagens() == null) {
            return null;
        }

        Date agora = new Date();
        Date proxima = null;
        for (com.confApi.hub.aereo.dto.TrechoReserva viagem : reserva.getViagens()) {
            if (viagem == null || viagem.getVoos() == null) {
                continue;
            }
            for (com.confApi.hub.aereo.dto.Voo voo : viagem.getVoos()) {
                if (voo == null || voo.getDataPartida() == null) {
                    continue;
                }
                Date partida = aplicarHora(voo.getDataPartida(), voo.getHoraPartida());
                if (partida.before(agora)) {
                    continue;
                }
                if (proxima == null || partida.before(proxima)) {
                    proxima = partida;
                }
            }
        }
        return proxima;
    }

    private Date aplicarHora(Date data, String hora) {
        if (data == null || hora == null || hora.isBlank()) {
            return data;
        }

        Matcher matcher = Pattern.compile("^(\\d{1,2}):(\\d{2})").matcher(hora.trim());
        if (!matcher.find()) {
            return data;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(data);
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(matcher.group(1)));
        calendar.set(Calendar.MINUTE, Integer.parseInt(matcher.group(2)));
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private boolean isDentroDasProximasHoras(Date data, int horas) {
        Date agora = new Date();
        if (data.before(agora)) {
            return false;
        }
        long limite = agora.getTime() + horas * 60L * 60L * 1000L;
        return data.getTime() <= limite;
    }

    private List<Map<String, Object>> resumirPassageirosHub(List<com.confApi.hub.aereo.dto.Passageiro> passageiros) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        if (passageiros == null) {
            return resultado;
        }
        for (com.confApi.hub.aereo.dto.Passageiro passageiro : passageiros) {
            if (passageiro == null) {
                continue;
            }
            resultado.add(resumirPassageiroHub(passageiro));
            if (resultado.size() >= LIMITE_PASSAGEIROS_RESUMO) {
                break;
            }
        }
        return resultado;
    }

    private Map<String, Object> resumirPassageiroHub(com.confApi.hub.aereo.dto.Passageiro passageiro) {
        Map<String, Object> map = new LinkedHashMap<>();
        putIfNotBlank(map, "nome", montarNomePassageiroHub(passageiro));
        putIfNotBlank(map, "idPassageiro", passageiro.getIdPassageiro());
        putIfNotBlank(map, "faixaEtaria", passageiro.getFaixaEtaria());
        putIfNotBlank(map, "nascimento", passageiro.getNascimento());
        putIfNotNull(map, "dataNascimento", passageiro.getDataNascimento());
        putIfNotBlank(map, "sexo", passageiro.getSexo());
        if (passageiro.getBilhetes() != null && !passageiro.getBilhetes().isEmpty()) {
            map.put("quantidadeBilhetes", passageiro.getBilhetes().size());
            map.put("bilhetes", resumirBilhetesHub(passageiro.getBilhetes()));
        }
        return map;
    }

    private String montarNomePassageiroHub(com.confApi.hub.aereo.dto.Passageiro passageiro) {
        StringJoiner joiner = new StringJoiner(" ");
        if (passageiro.getNome() != null && !passageiro.getNome().isBlank()) {
            joiner.add(passageiro.getNome().trim());
        }
        if (passageiro.getNomeDoMeio() != null && !passageiro.getNomeDoMeio().isBlank()) {
            joiner.add(passageiro.getNomeDoMeio().trim());
        }
        if (passageiro.getSobrenome() != null && !passageiro.getSobrenome().isBlank()) {
            joiner.add(passageiro.getSobrenome().trim());
        }
        return joiner.toString();
    }

    private List<Map<String, Object>> resumirBilhetesHub(List<com.confApi.hub.aereo.dto.Bilhete> bilhetes) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        if (bilhetes == null) {
            return resultado;
        }
        for (com.confApi.hub.aereo.dto.Bilhete bilhete : bilhetes) {
            if (bilhete == null) {
                continue;
            }
            Map<String, Object> map = new LinkedHashMap<>();
            putIfNotBlank(map, "numero", bilhete.getNumero());
            putIfNotBlank(map, "status", bilhete.getStatus());
            putIfNotNull(map, "dataEmissao", bilhete.getDataDeEmissao());
            putIfNotBlank(map, "passageiro", bilhete.getPassageiro());
            putIfNotBlank(map, "paxRef", bilhete.getPaxRef());
            resultado.add(map);
            if (resultado.size() >= LIMITE_PASSAGEIROS_RESUMO) {
                break;
            }
        }
        return resultado;
    }

    private List<Map<String, Object>> resumirViagensHub(List<com.confApi.hub.aereo.dto.TrechoReserva> viagens) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        if (viagens == null) {
            return resultado;
        }
        for (com.confApi.hub.aereo.dto.TrechoReserva viagem : viagens) {
            if (viagem == null) {
                continue;
            }
            resultado.add(resumirTrechoHub(viagem));
            if (resultado.size() >= LIMITE_TRECHOS_RESUMO) {
                break;
            }
        }
        return resultado;
    }

    private Map<String, Object> resumirTrechoHub(com.confApi.hub.aereo.dto.TrechoReserva trecho) {
        Map<String, Object> map = new LinkedHashMap<>();
        putIfNotBlank(map, "sistema", trecho.getSistema());
        putIfNotBlank(map, "identificacaoDaViagem", trecho.getIdentificacaoDaViagem());
        putIfNotNull(map, "duracao", trecho.getDuracao());
        putIfNotBlank(map, "tempoDeDuracao", trecho.getTempoDeDuracao());
        putIfNotNull(map, "numeroParadas", trecho.getNumeroParadas());
        putIfNotEmpty(map, "companhia", resumirCompanhiaHub(trecho.getCompanhia()));
        putIfNotEmpty(map, "origem", resumirAeroportoHub(trecho.getOrigem()));
        putIfNotEmpty(map, "destino", resumirAeroportoHub(trecho.getDestino()));
        List<Map<String, Object>> voos = resumirVoosHub(trecho.getVoos());
        if (!voos.isEmpty()) {
            map.put("voos", voos);
        }
        return map;
    }

    private List<Map<String, Object>> resumirVoosHub(List<com.confApi.hub.aereo.dto.Voo> voos) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        if (voos == null) {
            return resultado;
        }
        for (com.confApi.hub.aereo.dto.Voo voo : voos) {
            if (voo == null) {
                continue;
            }
            resultado.add(resumirVooHub(voo));
            if (resultado.size() >= LIMITE_VOOS_RESUMO) {
                break;
            }
        }
        return resultado;
    }

    private Map<String, Object> resumirVooHub(com.confApi.hub.aereo.dto.Voo voo) {
        Map<String, Object> map = new LinkedHashMap<>();
        putIfNotBlank(map, "numeroVoo", voo.getNumeroVoo());
        putIfNotBlank(map, "status", voo.getStatus());
        putIfNotNull(map, "dataPartida", voo.getDataPartida());
        putIfNotBlank(map, "horaPartida", voo.getHoraPartida());
        putIfNotNull(map, "dataChegada", voo.getDataChegada());
        putIfNotBlank(map, "horaChegada", voo.getHoraChegada());
        putIfNotBlank(map, "duracao", voo.getDuracao());
        putIfNotNull(map, "qtdEscalas", voo.getQtdEscalas());
        putIfNotBlank(map, "classe", voo.getClasse());
        putIfNotBlank(map, "cabine", voo.getCabine());
        putIfNotBlank(map, "familia", voo.getFamilia());
        putIfNotBlank(map, "familiaCodigo", voo.getFamiliaCodigo());
        putIfNotBlank(map, "baseTarifaria", voo.getBaseTarifaria());
        putIfNotBlank(map, "localizadorCia", voo.getLocalizadorCia());
        putIfNotNull(map, "conexao", voo.getIsConexao());
        putIfNotNull(map, "reembolsavel", voo.getIsReembolsavel());
        putIfNotNull(map, "bagagemInclusa", voo.getBagagemInclusa());
        putIfNotNull(map, "bagagemQuantidade", voo.getBagagemQuantidade());
        putIfNotNull(map, "bagagemPeso", voo.getBagagemPeso());
        putIfNotBlank(map, "bagagemUnidadeDeMedida", voo.getBagagemUnidadeDeMedida());
        putIfNotEmpty(map, "origem", resumirAeroportoHub(voo.getOrigem()));
        putIfNotEmpty(map, "destino", resumirAeroportoHub(voo.getDestino()));
        putIfNotEmpty(map, "ciaMandatoria", resumirCompanhiaHub(voo.getCiaMandatoria()));
        putIfNotEmpty(map, "ciaOperadora", resumirCompanhiaHub(voo.getCiaOperadora()));
        return map;
    }

    private Map<String, Object> resumirAeroportoHub(com.confApi.hub.aereo.dto.Aeroporto aeroporto) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (aeroporto == null) {
            return map;
        }
        putIfNotBlank(map, "iata", aeroporto.getCodigoIata());
        putIfNotBlank(map, "nome", aeroporto.getDescricao());
        return map;
    }

    private Map<String, Object> resumirCompanhiaHub(com.confApi.hub.aereo.dto.Companhia companhia) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (companhia == null) {
            return map;
        }
        putIfNotNull(map, "id", companhia.getId());
        putIfNotBlank(map, "iata", normalizarIataCia(companhia.getCodigoIata()));
        putIfNotBlank(map, "nome", companhia.getDescricao());
        return map;
    }

    private Map<String, Object> resumirValorReserva(com.confApi.aereo.dto.ValorReserva valorReserva) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (valorReserva == null) {
            return map;
        }
        putIfNotNull(map, "saldoDevedor", valorReserva.getSaldoDevedor());
        putIfNotNull(map, "valor", valorReserva.getValor());
        Map<String, Object> valorBase = resumirValorBase(valorReserva.getValorBase());
        if (!valorBase.isEmpty()) {
            map.put("valorBase", valorBase);
        }
        return map;
    }

    private Map<String, Object> resumirValorBase(com.confApi.aereo.dto.ValorBase valorBase) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (valorBase == null) {
            return map;
        }
        putIfNotNull(map, "tarifa", valorBase.getTarifa());
        putIfNotNull(map, "cambio", valorBase.getCambio());
        putIfNotNull(map, "taxaEmbarque", valorBase.getTaxaEmbarque());
        putIfNotNull(map, "taxaDU", valorBase.getTaxaDU());
        putIfNotNull(map, "RAV", valorBase.getRAV());
        putIfNotNull(map, "RC", valorBase.getRC());
        putIfNotNull(map, "MKP", valorBase.getMKP());
        putIfNotNull(map, "taxaAssento", valorBase.getTaxaAssento());
        putIfNotNull(map, "total", valorBase.getTotal());
        List<Map<String, Object>> valoresPorPassageiro = resumirValoresPassageiro(valorBase.getValorPassageiroList());
        if (!valoresPorPassageiro.isEmpty()) {
            map.put("valoresPorPassageiro", valoresPorPassageiro);
        }
        return map;
    }

    private List<Map<String, Object>> resumirValoresPassageiro(List<com.confApi.aereo.dto.ValorPassageiro> valoresPassageiro) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        if (valoresPassageiro == null) {
            return resultado;
        }
        for (com.confApi.aereo.dto.ValorPassageiro valorPassageiro : valoresPassageiro) {
            if (valorPassageiro == null) {
                continue;
            }
            Map<String, Object> map = new LinkedHashMap<>();
            putIfNotBlank(map, "nomePassageiro", valorPassageiro.getNomePassageiro());
            putIfNotNull(map, "tarifa", valorPassageiro.getTarifa());
            putIfNotNull(map, "taxaEmbarque", valorPassageiro.getTaxaEmbarque());
            putIfNotNull(map, "taxaDU", valorPassageiro.getTaxaDU());
            putIfNotNull(map, "RAV", valorPassageiro.getRAV());
            putIfNotNull(map, "RC", valorPassageiro.getRC());
            putIfNotNull(map, "MKP", valorPassageiro.getMKP());
            putIfNotNull(map, "taxaAssento", valorPassageiro.getTaxaAssento());
            putIfNotNull(map, "total", valorPassageiro.getTotal());
            resultado.add(map);
            if (resultado.size() >= LIMITE_PASSAGEIROS_RESUMO) {
                break;
            }
        }
        return resultado;
    }

    private Map<String, Object> resumirCancelamento(Reserva reserva) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (reserva == null) {
            map.put("status", "NAO_CONSULTADO");
            return map;
        }

        putIfNotNull(map, "permiteCancelarReserva", reserva.getPermiteCancelar());
        RegrasAereasReservaResponse regras = reserva.getRegrasAereas();
        if (regras == null || regras.getReembolso() == null) {
            map.put("baseRegra", "reembolso");
            map.put("status", "SEM_REGRA_REEMBOLSO");
            return map;
        }

        RegraAereaReembolsoConsultaResponse reembolso = regras.getReembolso();
        map.put("baseRegra", "reembolso");
        putIfNotNull(map, "status", reembolso.getStatus());
        putIfNotNull(map, "mensagem", reembolso.getMensagem());
        if (reembolso.getRegra() != null) {
            putIfNotNull(map, "momento", reembolso.getRegra().getMomento());
            putIfNotNull(map, "permiteReembolso", reembolso.getRegra().getPermiteReembolso());
            putIfNotNull(map, "statusReembolso", reembolso.getRegra().getStatusReembolso());
            putIfNotNull(map, "aplicaMulta", reembolso.getRegra().getAplicaMulta());
            putIfNotNull(map, "tipoMulta", reembolso.getRegra().getTipoMulta());
            putIfNotNull(map, "moedaMulta", reembolso.getRegra().getMoedaMulta());
            putIfNotNull(map, "valorMultaFixo", reembolso.getRegra().getValorMultaFixo());
            putIfNotNull(map, "percentualMulta", reembolso.getRegra().getPercentualMulta());
            putIfNotNull(map, "tituloUsuario", reembolso.getRegra().getTituloUsuario());
            putIfNotNull(map, "descricaoUsuario", reembolso.getRegra().getDescricaoUsuario());
            putIfNotNull(map, "observacao", reembolso.getRegra().getObservacao());
        }
        if (reembolso.getCalculo() != null) {
            putIfNotNull(map, "valorMultaCalculado", reembolso.getCalculo().getValorMulta());
            putIfNotNull(map, "valorPrevistoReembolso", reembolso.getCalculo().getValorPrevistoReembolso());
            putIfNotNull(map, "mensagemCalculo", reembolso.getCalculo().getMensagem());
            putIfNotNull(map, "alertas", reembolso.getCalculo().getAlertas());
        }
        return map;
    }

    private Map<String, Object> resumirRegras(RegrasAereasReservaResponse regras) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (regras == null) {
            map.put("status", "NAO_CONSULTADO");
            return map;
        }
        putIfNotNull(map, "status", regras.getStatus());
        putIfNotNull(map, "mensagem", regras.getMensagem());
        putIfNotNull(map, "dadosReservaUtilizados", regras.getDadosReservaUtilizados());
        map.put("reembolso", resumirReembolso(regras.getReembolso()));
        map.put("alteracao", resumirAlteracao(regras.getAlteracao()));
        map.put("reembolsosPorMomento", resumirReembolsos(regras.getReembolsos()));
        map.put("alteracoesPorMomento", resumirAlteracoes(regras.getAlteracoes()));
        map.put("quantidadeRegrasReembolso", regras.getReembolsos() == null ? 0 : regras.getReembolsos().size());
        map.put("quantidadeRegrasAlteracao", regras.getAlteracoes() == null ? 0 : regras.getAlteracoes().size());
        return map;
    }

    private List<Map<String, Object>> resumirReembolsos(List<RegraAereaReembolsoConsultaResponse> reembolsos) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        if (reembolsos == null) {
            return resultado;
        }
        for (RegraAereaReembolsoConsultaResponse reembolso : reembolsos) {
            resultado.add(resumirReembolso(reembolso));
        }
        return resultado;
    }

    private List<Map<String, Object>> resumirAlteracoes(List<RegraAereaAlteracaoConsultaResponse> alteracoes) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        if (alteracoes == null) {
            return resultado;
        }
        for (RegraAereaAlteracaoConsultaResponse alteracao : alteracoes) {
            resultado.add(resumirAlteracao(alteracao));
        }
        return resultado;
    }

    private Map<String, Object> resumirReembolso(RegraAereaReembolsoConsultaResponse response) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (response == null) {
            map.put("status", "NAO_CONSULTADO");
            return map;
        }
        putIfNotNull(map, "status", response.getStatus());
        putIfNotNull(map, "mensagem", response.getMensagem());
        if (response.getRegra() != null) {
            putIfNotNull(map, "momento", response.getRegra().getMomento());
            putIfNotNull(map, "permiteReembolso", response.getRegra().getPermiteReembolso());
            putIfNotNull(map, "statusReembolso", response.getRegra().getStatusReembolso());
            putIfNotNull(map, "aplicaMulta", response.getRegra().getAplicaMulta());
            putIfNotNull(map, "tipoMulta", response.getRegra().getTipoMulta());
            putIfNotNull(map, "moedaMulta", response.getRegra().getMoedaMulta());
            putIfNotNull(map, "valorMultaFixo", response.getRegra().getValorMultaFixo());
            putIfNotNull(map, "percentualMulta", response.getRegra().getPercentualMulta());
            putIfNotNull(map, "percentualReembolso", response.getRegra().getPercentualReembolso());
            putIfNotNull(map, "tituloUsuario", response.getRegra().getTituloUsuario());
            putIfNotNull(map, "descricaoUsuario", response.getRegra().getDescricaoUsuario());
            putIfNotNull(map, "observacao", response.getRegra().getObservacao());
        }
        if (response.getCalculo() != null) {
            putIfNotNull(map, "valorMultaCalculado", response.getCalculo().getValorMulta());
            putIfNotNull(map, "valorPrevistoReembolso", response.getCalculo().getValorPrevistoReembolso());
            putIfNotNull(map, "mensagemCalculo", response.getCalculo().getMensagem());
            putIfNotNull(map, "alertas", response.getCalculo().getAlertas());
        }
        return map;
    }

    private Map<String, Object> resumirAlteracao(RegraAereaAlteracaoConsultaResponse response) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (response == null) {
            map.put("status", "NAO_CONSULTADO");
            return map;
        }
        putIfNotNull(map, "status", response.getStatus());
        putIfNotNull(map, "mensagem", response.getMensagem());
        if (response.getRegra() != null) {
            putIfNotNull(map, "momento", response.getRegra().getMomento());
            putIfNotNull(map, "permiteAlteracao", response.getRegra().getPermiteAlteracao());
            putIfNotNull(map, "statusAlteracao", response.getRegra().getStatusAlteracao());
            putIfNotNull(map, "cobraDiferencaTarifaria", response.getRegra().getCobraDiferencaTarifaria());
            putIfNotNull(map, "aplicaMulta", response.getRegra().getAplicaMulta());
            putIfNotNull(map, "tipoMulta", response.getRegra().getTipoMulta());
            putIfNotNull(map, "moedaMulta", response.getRegra().getMoedaMulta());
            putIfNotNull(map, "valorMultaFixo", response.getRegra().getValorMultaFixo());
            putIfNotNull(map, "percentualMulta", response.getRegra().getPercentualMulta());
            putIfNotNull(map, "multaIsentaAbaixoDeHoras", response.getRegra().getMultaIsentaAbaixoDeHoras());
            putIfNotNull(map, "tituloUsuario", response.getRegra().getTituloUsuario());
            putIfNotNull(map, "descricaoUsuario", response.getRegra().getDescricaoUsuario());
            putIfNotNull(map, "observacao", response.getRegra().getObservacao());
        }
        if (response.getCalculo() != null) {
            putIfNotNull(map, "valorMultaCalculado", response.getCalculo().getValorMulta());
            putIfNotNull(map, "multaIsentaPorAntecedencia", response.getCalculo().getMultaIsentaPorAntecedencia());
            putIfNotNull(map, "horasDesdeEmissao", response.getCalculo().getHorasDesdeEmissao());
            putIfNotNull(map, "limiteHorasIsencaoMulta", response.getCalculo().getLimiteHorasIsencaoMulta());
            putIfNotNull(map, "diferencaTarifaria", response.getCalculo().getDiferencaTarifaria());
            putIfNotNull(map, "totalPrevisto", response.getCalculo().getTotalPrevisto());
            putIfNotNull(map, "resumoCalculo", response.getCalculo().getResumo());
        }
        return map;
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private void putIfNotBlank(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    private void putIfNotEmpty(Map<String, Object> map, String key, Map<String, Object> value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, value);
        }
    }

    public ChatMessageDTO listarFamilias(ConversationRequestDTO req, String cia) {
        List<FamiliaCompanhia> familiaCompanhiaList = familiaService.findByNomeOuIataCia(cia);
        FamiliaIAResponse familiaIAResponse = new FamiliaIAResponse();
        familiaIAResponse.setFamiliaCompanhias(familiaCompanhiaList);
        return new ChatMessageDTO("system", "Dado do sistema: " + familiaIAResponse.toString());
    }

    public ChatMessageDTO listarUltimasVendas(ConversationRequestDTO req) {
        try {
            if (req.codgAgencia() == null) {
                return mensagemErroReservasRecentes(
                        "A agencia da sessao nao foi identificada para consultar as reservas.");
            }
            Integer codgAgencia = req.codgAgencia().intValue();
            ReservasAereasRecentesResponse recentes =
                    chatConfiancaReservaAereaService.listarRecentes(
                            codgAgencia, LIMITE_ULTIMAS_RESERVAS_AEREAS);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("schema", ChatConfiancaReservaAereaService.SCHEMA_RESERVAS_RECENTES);
            payload.put("reservasRecentes", recentes);
            payload.put("actions", chatConfiancaReservaAereaService.listarAcoes(recentes));

            return new ChatMessageDTO("system",
                    "Dado do sistema (reservas_aereas_recentes_agencia): "
                            + mapper.writeValueAsString(payload)
                            + "\nO painel estruturado exibira todos os detalhes e botoes. "
                            + "Responda somente com uma introducao curta e a quantidade encontrada, "
                            + "sem repetir cada reserva em texto. Se a lista estiver vazia, informe isso claramente. "
                            + "A emissao nao e executada pelo chat; emissaoCandidata e apenas um aviso e a reserva "
                            + "deve ser aberta no sistema para qualquer emissao.");

        } catch (Exception e) {
            return mensagemErroReservasRecentes(
                    "Nao foi possivel listar as reservas aereas recentes da agencia agora.");
        }
    }

    private ChatMessageDTO mensagemErroReservasRecentes(String mensagem) {
        try {
            ReservasAereasRecentesResponse recentes = new ReservasAereasRecentesResponse();
            recentes.setStatus("ERRO");
            recentes.setMensagem(mensagem);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("schema", ChatConfiancaReservaAereaService.SCHEMA_RESERVAS_RECENTES);
            payload.put("reservasRecentes", recentes);
            payload.put("actions", List.of());
            return new ChatMessageDTO("system",
                    "Dado do sistema (reservas_aereas_recentes_agencia): "
                            + mapper.writeValueAsString(payload)
                            + "\nInforme claramente que a consulta nao pode ser concluida agora e sugira tentar novamente.");
        } catch (JsonProcessingException ex) {
            return new ChatMessageDTO("system", "Dado do sistema: " + mensagem);
        }
    }

    private List<Map<String, Object>> resumirReservasAereasUsuario(List<ReservaAereo> reservas) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        if (reservas == null) {
            return resultado;
        }

        for (ReservaAereo reserva : reservas) {
            if (reserva == null) {
                continue;
            }
            resultado.add(resumirReservaAereaUsuario(reserva));
            if (resultado.size() >= LIMITE_ULTIMAS_RESERVAS_AEREAS) {
                break;
            }
        }
        return resultado;
    }

    private String extrairPrimeiroLocalizadorResumo(List<Map<String, Object>> reservasResumo) {
        if (reservasResumo == null || reservasResumo.isEmpty()) {
            return null;
        }

        Object localizador = reservasResumo.get(0).get("localizador");
        return localizador == null ? null : localizador.toString();
    }

    private Map<String, Object> resumirReservaAereaUsuario(ReservaAereo reserva) {
        Map<String, Object> map = new LinkedHashMap<>();
        putIfNotNull(map, "codgReservaAereo", reserva.getCodgReservaAereo());
        putIfNotBlank(map, "localizador", reserva.getLocalizador());
        putIfNotNull(map, "status", reserva.getStatus());
        putIfNotNull(map, "dataCriacao", formatarDataSistema(reserva.getDataCriacao()));
        putIfNotNull(map, "dataEmissao", formatarDataSistema(reserva.getDataEmissao()));
        putIfNotNull(map, "dataLimiteEmissao", formatarDataSistema(reserva.getDataLimiteEmissao()));
        putIfNotNull(map, "dataCancelamento", formatarDataSistema(reserva.getDataCancelamento()));
        putIfNotBlank(map, "motivoCancelamento", reserva.getDescMotivoCancelamento());
        putIfNotBlank(map, "regraReserva", reserva.getRegraReserva());
        putIfNotNull(map, "valorTotalReserva", reserva.getValorTotalReserva());

        Map<String, Object> usuarioCriacao = resumirUsuarioReserva(reserva.getCodgUsuarioCriacao());
        if (!usuarioCriacao.isEmpty()) {
            map.put("usuarioCriacao", usuarioCriacao);
        }

        Map<String, Object> agencia = resumirAgenciaReserva(reserva.getCodgAgencia());
        if (!agencia.isEmpty()) {
            map.put("agencia", agencia);
        }

        Map<String, Object> companhia = resumirCompanhiaReserva(reserva.getCodgCompanhiaAerea());
        if (!companhia.isEmpty()) {
            map.put("companhia", companhia);
        }

        map.put("quantidadePassageiros", reserva.getPassageiros() == null ? 0 : reserva.getPassageiros().size());
        map.put("passageiros", resumirPassageirosReserva(reserva.getPassageiros()));
        map.put("quantidadeTrechos", reserva.getTrechos() == null ? 0 : reserva.getTrechos().size());
        map.put("trechos", resumirTrechosReserva(reserva.getTrechos()));
        return map;
    }

    private Map<String, Object> resumirUsuarioReserva(Usuario usuario) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (usuario == null) {
            return map;
        }
        putIfNotNull(map, "codgUsuario", usuario.getCodgUsuario());
        putIfNotBlank(map, "nome", usuario.getNomeCompleto());
        putIfNotBlank(map, "login", usuario.getLoginUsuario());
        return map;
    }

    private Map<String, Object> resumirAgenciaReserva(Agencia agencia) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (agencia == null) {
            return map;
        }
        putIfNotNull(map, "codgAgencia", agencia.getCodgAgencia());
        putIfNotBlank(map, "nome", agencia.getNomeAgencia());
        return map;
    }

    private Map<String, Object> resumirCompanhiaReserva(CompanhiaAerea companhia) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (companhia == null) {
            return map;
        }
        putIfNotNull(map, "codgCompanhiaAerea", companhia.getCodgCompanhiaAerea());
        putIfNotBlank(map, "iata", normalizarIataCia(companhia.getIataCia()));
        putIfNotBlank(map, "nome", companhia.getNomeCia());
        return map;
    }

    private List<String> resumirPassageirosReserva(List<Passageiro> passageiros) {
        List<String> nomes = new ArrayList<>();
        if (passageiros == null) {
            return nomes;
        }

        for (Passageiro passageiro : passageiros) {
            if (passageiro == null) {
                continue;
            }
            String nome = montarNomePassageiro(passageiro);
            if (!nome.isBlank()) {
                nomes.add(nome);
            }
            if (nomes.size() >= LIMITE_PASSAGEIROS_RESUMO) {
                break;
            }
        }
        return nomes;
    }

    private String montarNomePassageiro(Passageiro passageiro) {
        StringJoiner joiner = new StringJoiner(" ");
        if (passageiro.getNomePassageiro() != null && !passageiro.getNomePassageiro().isBlank()) {
            joiner.add(passageiro.getNomePassageiro().trim());
        }
        if (passageiro.getMeioNomePassageiro() != null && !passageiro.getMeioNomePassageiro().isBlank()) {
            joiner.add(passageiro.getMeioNomePassageiro().trim());
        }
        if (passageiro.getSobrenomePassageiro() != null && !passageiro.getSobrenomePassageiro().isBlank()) {
            joiner.add(passageiro.getSobrenomePassageiro().trim());
        }
        return joiner.toString();
    }

    private List<Map<String, Object>> resumirTrechosReserva(List<Trecho> trechos) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        if (trechos == null) {
            return resultado;
        }

        for (Trecho trecho : trechos) {
            if (trecho == null) {
                continue;
            }
            resultado.add(resumirTrechoReserva(trecho));
            if (resultado.size() >= LIMITE_TRECHOS_RESUMO) {
                break;
            }
        }
        return resultado;
    }

    private Map<String, Object> resumirTrechoReserva(Trecho trecho) {
        Map<String, Object> map = new LinkedHashMap<>();
        putIfNotNull(map, "dataPartida", formatarDataSistema(trecho.getDataPartida()));
        putIfNotNull(map, "dataChegada", formatarDataSistema(trecho.getDataChegada()));
        putIfNotBlank(map, "origem", resumirAeroporto(trecho.getCodgAeroportoOrigem()));
        putIfNotBlank(map, "destino", resumirAeroporto(trecho.getCodgAeroportoDestino()));

        Map<String, Object> companhia = resumirCompanhiaReserva(trecho.getCodgCompanhiaAerea());
        if (!companhia.isEmpty()) {
            map.put("companhia", companhia);
        }
        map.put("voos", resumirVoosReserva(trecho.getVoos()));
        return map;
    }

    private List<Map<String, Object>> resumirVoosReserva(List<Voo> voos) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        if (voos == null) {
            return resultado;
        }

        for (Voo voo : voos) {
            if (voo == null) {
                continue;
            }
            resultado.add(resumirVooReserva(voo));
            if (resultado.size() >= LIMITE_VOOS_RESUMO) {
                break;
            }
        }
        return resultado;
    }

    private Map<String, Object> resumirVooReserva(Voo voo) {
        Map<String, Object> map = new LinkedHashMap<>();
        putIfNotBlank(map, "numeroVoo", voo.getNumeroVoo());
        putIfNotBlank(map, "origem", resumirAeroporto(voo.getCodgAeroportoOrigem()));
        putIfNotBlank(map, "destino", resumirAeroporto(voo.getCodgAeroportoDestino()));
        putIfNotNull(map, "dataHoraPartida", formatarDataSistema(voo.getDataHoraPartida()));
        putIfNotNull(map, "dataHoraChegada", formatarDataSistema(voo.getDataHoraChegada()));
        putIfNotBlank(map, "classeTarifa", voo.getClasseTarifa());
        putIfNotBlank(map, "baseTarifa", voo.getBaseTarifa());
        putIfNotBlank(map, "familia", voo.getFamilia());
        putIfNotNull(map, "qtdBagagem", voo.getQtdBagagem());
        putIfNotNull(map, "qtdEscalas", voo.getQtdEscalas());
        putIfNotBlank(map, "cabine", voo.getCabine());
        putIfNotBlank(map, "statusVoo", voo.getStatusVoo());

        Map<String, Object> companhia = resumirCompanhiaReserva(voo.getCodgCompanhiaAerea());
        if (!companhia.isEmpty()) {
            map.put("companhia", companhia);
        }
        return map;
    }

    private String resumirAeroporto(Aeroporto aeroporto) {
        if (aeroporto == null) {
            return null;
        }
        if (aeroporto.getIataAeroporto() != null && !aeroporto.getIataAeroporto().isBlank()) {
            return aeroporto.getIataAeroporto();
        }
        return aeroporto.getNomeAeroporto();
    }

    private String normalizarIataCia(String iata) {
        if (iata != null && iata.equalsIgnoreCase("JJ")) {
            return "LA";
        }
        return iata;
    }

    private String formatarDataSistema(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(date);
    }


    public ChatMessageDTO buscarCheckinsProximos(ConversationRequestDTO req) {
        String resultadoJson;

        // 1) Busca lista no serviço (null-safe)
        List<Checkin72Horas> checkinList = Optional
                .ofNullable(checkinService.findCheckin72Horas(new CheckinRQ(req.idErp(), 2)))
                .orElseGet(java.util.Collections::emptyList);

        try {
            // 2) Converte List<Checkin72Horas> -> List<ReservaCheckInIA> sem serializar antes
            List<ReservaCheckInIA> rcIA = mapper.convertValue(
                    checkinList,
                    new com.fasterxml.jackson.core.type.TypeReference<List<ReservaCheckInIA>>() {
                    }
            );

            // 3) Monta o wrapper de resposta
            CheckinIAResponse fResponse = new CheckinIAResponse();
            fResponse.setReservaCheckInIA(
                    Optional.ofNullable(rcIA).orElseGet(java.util.ArrayList::new)
            );

            // 4) Serializa o OBJETO (não toString)
            resultadoJson = mapper.writeValueAsString(fResponse);

           // System.out.println("[buscarCheckinsProximos] itens convertidos: " + fResponse.getReservaCheckInIA().size());

        } catch (Exception e) {
         //   System.out.println("[buscarCheckinsProximos] Erro ao montar resposta" + e);
            // fallback mínimo para não quebrar o fluxo
            resultadoJson = "{\"reservaCheckInIA\":[]}";
        }

        return new ChatMessageDTO("system", "Dado do sistema: " + resultadoJson);
    }

    public ChatMessageDTO montarMensagemFaturas(ConversationRequestDTO req) {
        // 1) Monta o request
        FaturaSicaRQ faturaSicaRQ = new FaturaSicaRQ();
        faturaSicaRQ.setInvoiceType("TODOS");
        faturaSicaRQ.setEmpfat(req.idErp());
        faturaSicaRQ.setTipoData("TODAS");
        faturaSicaRQ.setDataInicio(null);
        faturaSicaRQ.setDataFim(null);
        faturaSicaRQ.setPagamento("ABERTO");
        faturaSicaRQ.setDisabledAFaturar(false);

        // 2) Consulta o serviço
        List<FaturaSicaRS> faturas = Collections.emptyList();
        try {
            faturas = Optional.ofNullable(faturasService.faturaSica(faturaSicaRQ))
                    .orElse(Collections.emptyList());
        } catch (Exception e) {
            System.out.println("Erro ao consultar faturas no faturasService " + e);
        }

        // 3) Converte/normaliza datas (dd/MM/yyyy) com null-safety
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        for (FaturaSicaRS f : faturas) {
            try {
                if (f.getDataFatura() != null && !f.getDataFatura().isBlank()) {
                    f.setConvertDataFatura(formatter.parse(f.getDataFatura()));
                }
                if (f.getDataVen() != null && !f.getDataVen().isBlank()) {
                    f.setConvertDataVen(formatter.parse(f.getDataVen()));
                }
            } catch (ParseException pe) {
                System.out.println("Falha ao parsear datas da fatura: " + pe);

            }
        }

        // 4) Prepara ObjectMapper (um só), tolerante a campos extras
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 5) Mapeia diretamente a lista de FaturaSicaRS -> List<FaturaIA> sem serializar antes
        List<FaturaIA> faturasIA = mapper.convertValue(
                faturas,
                new TypeReference<List<FaturaIA>>() {
                }
        );

        // 6) Monta o wrapper de resposta
        FaturaResponseIA fResponse = new FaturaResponseIA();
        fResponse.setFaturas(
                Optional.ofNullable(faturasIA).orElseGet(ArrayList::new)
        );

        // 7) Serializa o objeto (não use toString())
        String resultadoJson;
        try {
            resultadoJson = mapper.writeValueAsString(fResponse);
        } catch (JsonProcessingException e) {
            System.out.println("Erro serializando FaturaResponseIA " + e);

            // fallback mínimo para não quebrar o fluxo
            resultadoJson = "{\"faturas\":[]}";
        }

        // 8) Retorna já no formato de mensagem de sistema
        return new ChatMessageDTO("system", "Dado do sistema: " + resultadoJson);
    }

    public ChatMessageDTO montarMensagemFaturasBoleto(ConversationRequestDTO req) {
        // 1) Monta o request
        FaturaSicaRQ rq = new FaturaSicaRQ();
        rq.setInvoiceType("TODOS");
        rq.setEmpfat(req.idErp());
        rq.setTipoData("TODAS");
        rq.setDataInicio(null);
        rq.setDataFim(null);
        rq.setPagamento("ABERTO");
        rq.setDisabledAFaturar(false);

        // 2) Consulta serviço com fallback seguro
        List<FaturaSicaRS> faturas = Collections.emptyList();
        try {
            faturas = Optional.ofNullable(faturasService.faturaSica(rq))
                    .orElse(Collections.emptyList());
        } catch (Exception e) {
            System.out.println("Erro ao consultar faturas (boletos) " + e);

        }

        // 3) Normaliza datas (dd/MM/yyyy)
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        for (FaturaSicaRS f : faturas) {
            try {
                if (f.getDataFatura() != null && !f.getDataFatura().isBlank()) {
                    f.setConvertDataFatura(formatter.parse(f.getDataFatura()));
                }
                if (f.getDataVen() != null && !f.getDataVen().isBlank()) {
                    f.setConvertDataVen(formatter.parse(f.getDataVen()));
                }
            } catch (ParseException pe) {
                System.out.println("Falha ao parsear datas:  " + pe);

            }
        }

        // 4) Remove situações indesejadas
        final Set<String> SITUACOES_REMOVER = Set.of(
                "Faturada Crédito",
                "À Faturar"
        );
        faturas.removeIf(f -> {
            String s = Optional.ofNullable(f.getSituacao()).orElse("").trim();
            // compara ignorando acentuação? Aqui, apenas case-insensitive:
            return SITUACOES_REMOVER.stream().anyMatch(x -> x.equalsIgnoreCase(s));
        });

        // 5) Mapper único e resiliente
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 6) Converte para o modelo da IA
        List<FaturaIA> faturasIA = mapper.convertValue(
                faturas,
                new TypeReference<List<FaturaIA>>() {
                }
        );

        // 7) Empacota na resposta
        FaturaResponseIA resp = new FaturaResponseIA();
        resp.setFaturas(Optional.ofNullable(faturasIA).orElseGet(ArrayList::new));

        // 8) Serializa o objeto (não usar toString())
        String json;
        try {
            json = mapper.writeValueAsString(resp);
        } catch (JsonProcessingException e) {
            System.out.println("Erro serializando FaturaResponseIA (boletos):  " + e);
            json = "{\"faturas\":[]}";
        }

        // 9) Retorna a mensagem pronta para o chat
        return new ChatMessageDTO("system", "Dado do sistema (boletos): " + json);
    }

    public String conversationAgentIA(String input) throws IOException {
        String resposta = agenteIADecisor("AgentIA", profileAgentIA(), input);
        return resposta == null || resposta.isBlank() ? "desconhecido" : resposta;
    }

    private String agenteIADecisor(String nomeAgente, String perfilAgente, String input) throws IOException {
        List<ChatMessageDTO> messages = new ArrayList<>();
        messages.add(new ChatMessageDTO("system", perfilAgente));
        messages.add(new ChatMessageDTO("user", input == null ? "" : input));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("agent", nomeAgente);
        metadata.put("timestamp", new Date());

        ChatRequestDTO chatReq = new ChatRequestDTO(
                messages,
                null,
                false,
                List.of(),
                metadata
        );

        ChatResponseDTO response = chat(chatReq, null, null);
        if (response != null && response.content() != null && !response.content().isEmpty()) {
            return response.content().trim();
        }
        return "";
    }

    public FieldAssistantResponseDTO assistField(FieldAssistantRequestDTO req) throws IOException {
        List<ChatMessageDTO> messages = new ArrayList<>();

        String systemPrompt = """
        Você é um assistente de preenchimento de campos em sistema.
        Sua função é ajudar o usuário a escrever melhor, resumir, corrigir ou sugerir conteúdo.
        Responda sempre de forma objetiva, útil e pronta para uso.
        Quando solicitado, devolva conteúdo em JSON válido.
        
Retorne APENAS um JSON válido.
Não use markdown.
Não use ```json.
Não adicione explicações fora do JSON.

Formato esperado:
{
  "resultado": "...",
  "resumo": "...",
  "sugestoes": ["...", "...", "..."],
  "observacao": "..."
}
""";


        messages.add(new ChatMessageDTO("system", systemPrompt));

        StringBuilder prompt = new StringBuilder();
        prompt.append("Tipo de assistência: ").append(req.tipo()).append("\n");
        prompt.append("Campo: ").append(req.campo()).append("\n");

        if (req.labelCampo() != null) {
            prompt.append("Label do campo: ").append(req.labelCampo()).append("\n");
        }
        if (req.contexto() != null) {
            prompt.append("Contexto: ").append(req.contexto()).append("\n");
        }
        if (req.tom() != null) {
            prompt.append("Tom desejado: ").append(req.tom()).append("\n");
        }
        if (req.tamanho() != null) {
            prompt.append("Tamanho desejado: ").append(req.tamanho()).append("\n");
        }
        if (req.valorAtual() != null) {
            prompt.append("Texto atual: ").append(req.valorAtual()).append("\n");
        }

        if (req.dadosExtras() != null && !req.dadosExtras().isEmpty()) {
            prompt.append("Dados extras:\n");
            req.dadosExtras().forEach((k, v) -> prompt.append("- ").append(k).append(": ").append(v).append("\n"));
        }

        prompt.append("""
        
        Gere a resposta no formato JSON com esta estrutura:
        {
          "resultado": "...",
          "resumo": "...",
          "sugestoes": ["...", "...", "..."],
          "observacao": "..."
        }
        """);

        messages.add(new ChatMessageDTO("user", prompt.toString()));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("codgAgencia", req.codgAgencia());
        metadata.put("codgUsuario", req.codgUsuario());
        metadata.put("assistenciaCampo", true);
        metadata.put("tipo", req.tipo().name());

        ChatRequestDTO chatReq = new ChatRequestDTO(
                messages,
                null,
                false,
                List.of(),
                metadata
        );

        ChatResponseDTO resp = chat(chatReq, null, messages);

        String conteudo = resp.content();// ajuste conforme seu DTO real
        Map<String, Object> json = parseJsonSeguro(conteudo);

        return new FieldAssistantResponseDTO(
                req.campo(),
                req.tipo().name(),
                req.valorAtual(),
                asString(json.get("resultado")),
                asString(json.get("resumo")),
                asStringList(json.get("sugestoes")),
                asString(json.get("observacao"))
        );
    }

    private Map<String, Object> parseJsonSeguro(String content) {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("resultado", content);
        fallback.put("resumo", null);
        fallback.put("sugestoes", List.of());
        fallback.put("observacao", "Resposta retornada em texto livre.");

        if (content == null || content.isBlank()) {
            return fallback;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();

            // 1) remove espaços nas bordas
            String json = content.trim();

            // 2) remove bloco markdown ```json ... ```
            if (json.startsWith("```")) {
                json = json.replaceFirst("^```json\\s*", "");
                json = json.replaceFirst("^```\\s*", "");
                json = json.replaceFirst("\\s*```$", "");
                json = json.trim();
            }

            // 3) tenta localizar o trecho entre o primeiro { e o último }
            int ini = json.indexOf("{");
            int fim = json.lastIndexOf("}");
            if (ini >= 0 && fim > ini) {
                json = json.substring(ini, fim + 1);
            }

            // 4) desserializa
            return mapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return fallback;
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private List<String> asStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private String profileAgentIA() {
        return """
                Você é o **AgentIA**, um agente auxiliar da Confiança Consolidadora.
                Sua função é analisar a frase do usuário, entender sua intenção e retornar apenas
                a *keyword* correspondente ao método que deve ser executado pela IA principal.

                O sistema possui dados sobre:
                - Agências de viagens e seus usuários;
                - Companhias aéreas e famílias tarifárias;
                - Formas de pagamento e limites de crédito;
                - Faturas e boletos;
                - Check-ins e embarques próximos (72h);
                - Vendas e reservas recentes.
                - Reservas aéreas e regras de multa, reembolso, cancelamento, alteração e remarcação.

                Responda **somente com a keyword da intenção**, sem explicações.
                Palavras-chave possíveis:
                - "limites"
                - "faturas"
                - "boletos"
                - "checkin"
                - "ultimas_vendas"
                - "ultimas_reservas_aereas"
                - "familias"
                - "alertas"
                - "reserva_aerea_detalhes"
                - "reserva_aerea_regras"
                - "selecionar_reserva_remarcacao"
                - "simular_remarcacao"

                Exemplos:
                - Pergunta: "Quais são meus limites de crédito?" → Resposta: "limites"
                - Pergunta: "Me mostre as últimas vendas" → Resposta: "ultimas_vendas"
                - Pergunta: "Liste minhas últimas reservas" → Resposta: "ultimas_reservas_aereas"
                - Pergunta: "Quero ver as famílias da GOL" → Resposta: "familias;GOL"
                - Pergunta: "Carregue os dados da reserva ABC123" → Resposta: "reserva_aerea_detalhes"
                - Pergunta: "Consulte o localizador XYZ789" → Resposta: "reserva_aerea_detalhes"
                - Pergunta: "Posso emitir a reserva ABC123?" → Resposta: "reserva_aerea_detalhes"
                - Pergunta: "Mostre os bilhetes do localizador XYZ789" → Resposta: "reserva_aerea_detalhes"
                - Pergunta: "Tem assento disponivel na reserva ABC123?" → Resposta: "reserva_aerea_detalhes"
                - Pergunta: "A reserva ABC123 tem multa para remarcação?" → Resposta: "reserva_aerea_regras"
                - Pergunta: "Consulte o reembolso do localizador XYZ789" → Resposta: "reserva_aerea_regras"
                - Pergunta: "Quero cancelar a reserva ABC123" → Resposta: "reserva_aerea_regras"
                - Pergunta: "Quero simular uma remarcação" → Resposta: "selecionar_reserva_remarcacao"
                - Pergunta: "Quero remarcar a reserva ABC123" → Resposta: "simular_remarcacao"
                - Pergunta fora do contexto → Resposta: "desconhecido"
                """;
    }

    private boolean ultimaMensagemTemRotaDeTarifa(List<ChatMessageDTO> messages) {
        if (messages == null) {
            return false;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessageDTO message = messages.get(i);
            if (message != null
                    && "user".equals(message.role())
                    && (isConsultaMelhorTarifaAerea(message.content())
                    || isConsultaMelhorTarifaAereaIdaVolta(message.content()))) {
                return true;
            }
        }
        return false;
    }

    private boolean isFerramentaMelhoresTarifas(String nome) {
        return "search_cheapest_airfares".equals(nome)
                || "search_cheapest_roundtrip_airfares".equals(nome);
    }

    private boolean contextoLocalTarifasTemRota(Map<String, Object> metadata) {
        Map<?, ?> contexto = contextoLocalTarifas(metadata);
        return iataValido(contexto.get("origem")) && iataValido(contexto.get("destino"));
    }

    private Map<String, Object> completarArgumentosComContextoLocalTarifas(
            Map<String, Object> argumentos,
            Map<String, Object> metadata,
            List<ChatMessageDTO> messages) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        if (argumentos != null) {
            resultado.putAll(argumentos);
        }
        Map<?, ?> contexto = contextoLocalTarifas(metadata);
        boolean rotaExplicitaMudou = rotaExplicitaMudou(resultado, contexto);
        copiarContextoSeAusente(resultado, contexto, "origem", "origem");
        copiarContextoSeAusente(resultado, contexto, "destino", "destino");

        String modo = Objects.toString(resultado.get("modoResposta"), "");
        if ("cabines".equalsIgnoreCase(modo)
                || ultimaMensagemSolicitaTodasCabines(messages)) {
            resultado.remove("cabine");
        } else if (!rotaExplicitaMudou) {
            copiarContextoSeAusente(resultado, contexto, "cabine", "cabine");
        }

        boolean periodoExplicito = resultado.containsKey("mes")
                || resultado.containsKey("mesIda")
                || resultado.containsKey("dataInicio")
                || resultado.containsKey("dataFim");
        if (!rotaExplicitaMudou && !periodoExplicito) {
            copiarContextoSeAusente(
                    resultado, contexto, "periodoInicio", "dataInicio");
            copiarContextoSeAusente(
                    resultado, contexto, "periodoFim", "dataFim");
        }
        if (!rotaExplicitaMudou && "alternativas".equalsIgnoreCase(modo)) {
            copiarContextoSeAusente(
                    resultado, contexto, "limiteAlternativas", "limiteAlternativas");
        }
        return resultado;
    }

    private Map<String, Object> completarArgumentosComContextoLocalTarifasIdaVolta(
            Map<String, Object> argumentos,
            Map<String, Object> metadata,
            List<ChatMessageDTO> messages) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        if (argumentos != null) {
            resultado.putAll(argumentos);
        }
        Map<?, ?> contexto = contextoLocalTarifas(metadata);
        boolean rotaExplicitaMudou = rotaExplicitaMudou(resultado, contexto);
        copiarContextoSeAusente(resultado, contexto, "origem", "origem");
        copiarContextoSeAusente(resultado, contexto, "destino", "destino");

        String modo = Objects.toString(resultado.get("modoResposta"), "");
        if (ultimaMensagemSolicitaTodasCabines(messages)) {
            resultado.remove("cabine");
        } else if (!rotaExplicitaMudou) {
            copiarContextoSeAusente(resultado, contexto, "cabine", "cabine");
        }
        if (rotaExplicitaMudou) {
            return resultado;
        }

        copiarPeriodoIdaVoltaSeAusente(resultado, contexto,
                "mesIda", "dataIda", "dataIdaInicio", "dataIdaFim");
        if (!resultado.containsKey("mesIda")
                && !resultado.containsKey("dataIda")
                && !resultado.containsKey("dataIdaInicio")
                && !resultado.containsKey("dataIdaFim")) {
            copiarContextoSeAusente(resultado, contexto,
                    "periodoInicio", "dataIdaInicio");
            copiarContextoSeAusente(resultado, contexto,
                    "periodoFim", "dataIdaFim");
        }
        copiarPeriodoIdaVoltaSeAusente(resultado, contexto,
                "mesVolta", "dataVolta", "dataVoltaInicio", "dataVoltaFim");
        copiarContextoSeAusente(resultado, contexto,
                "duracaoMinimaDias", "duracaoMinimaDias");
        copiarContextoSeAusente(resultado, contexto,
                "duracaoMaximaDias", "duracaoMaximaDias");
        copiarContextoSeAusente(resultado, contexto,
                "politicaCompanhia", "politicaCompanhia");
        if ("alternativas".equalsIgnoreCase(modo)) {
            copiarContextoSeAusente(resultado, contexto,
                    "limiteAlternativas", "limiteAlternativas");
        }
        return resultado;
    }

    private void copiarPeriodoIdaVoltaSeAusente(Map<String, Object> destino,
                                                Map<?, ?> contexto,
                                                String mes,
                                                String data,
                                                String inicio,
                                                String fim) {
        boolean explicito = destino.containsKey(mes)
                || destino.containsKey(data)
                || destino.containsKey(inicio)
                || destino.containsKey(fim);
        if (!explicito) {
            copiarContextoSeAusente(destino, contexto, inicio, inicio);
            copiarContextoSeAusente(destino, contexto, fim, fim);
        }
    }

    private boolean rotaExplicitaMudou(Map<String, Object> argumentos,
                                       Map<?, ?> contexto) {
        return campoExplicitoMudou(argumentos, contexto, "origem")
                || campoExplicitoMudou(argumentos, contexto, "destino");
    }

    private boolean campoExplicitoMudou(Map<String, Object> argumentos,
                                        Map<?, ?> contexto,
                                        String campo) {
        Object atual = argumentos.get(campo);
        Object anterior = contexto.get(campo);
        return iataValido(atual)
                && iataValido(anterior)
                && !atual.toString().trim().equalsIgnoreCase(anterior.toString().trim());
    }

    private boolean ultimaMensagemSolicitaTodasCabines(List<ChatMessageDTO> messages) {
        if (messages == null) {
            return false;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessageDTO message = messages.get(i);
            if (message == null || !"user".equals(message.role()) || message.content() == null) {
                continue;
            }
            String texto = Normalizer.normalize(message.content(), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .trim()
                    .toLowerCase(Locale.ROOT);
            return texto.equals("geral")
                    || texto.contains("pode ser geral")
                    || texto.contains("todas as cabines")
                    || texto.contains("qualquer cabine")
                    || texto.contains("sem preferencia");
        }
        return false;
    }

    private Map<?, ?> contextoLocalTarifas(Map<String, Object> metadata) {
        if (metadata == null) {
            return Map.of();
        }
        Object contexto = metadata.get("contextoLocalMelhoresTarifasAereas");
        return contexto instanceof Map<?, ?> mapa ? mapa : Map.of();
    }

    private void copiarContextoSeAusente(Map<String, Object> destino,
                                         Map<?, ?> contexto,
                                         String campoContexto,
                                         String campoDestino) {
        Object atual = destino.get(campoDestino);
        if (atual != null && !atual.toString().isBlank()) {
            return;
        }
        Object valor = contexto.get(campoContexto);
        if (valor != null && !valor.toString().isBlank()) {
            destino.put(campoDestino, valor);
        }
    }

    private boolean iataValido(Object valor) {
        return valor != null
                && valor.toString().trim().toUpperCase(Locale.ROOT).matches("[A-Z]{3}");
    }

    public boolean isSolicitacaoSomenteIda(String input) {
        String texto = normalizarTarifa(input);
        return texto.contains("so ida")
                || texto.contains("so de ida")
                || texto.contains("somente ida")
                || texto.contains("somente de ida")
                || texto.contains("apenas ida")
                || texto.contains("apenas a ida")
                || texto.contains("apenas de ida")
                || texto.contains("sem volta")
                || texto.contains("agora ida") && !texto.contains("volta");
    }

    public boolean isConsultaMelhorTarifaAereaIdaVolta(String input) {
        if (input == null || input.isBlank() || isSolicitacaoSomenteIda(input)) {
            return false;
        }
        String texto = normalizarTarifa(input);
        if (temContextoHotel(texto) || !temRotaTarifa(texto)) {
            return false;
        }
        boolean idaVolta = mencionaIdaVolta(texto);
        boolean perguntaTarifa = texto.contains("mais barato")
                || texto.contains("mais barata")
                || texto.contains("menor preco")
                || texto.contains("menor tarifa")
                || texto.contains("melhor tarifa")
                || texto.contains("melhores tarifas")
                || texto.contains("melhores datas")
                || texto.contains("mais em conta")
                || texto.contains("total combinado")
                || texto.contains("preco")
                || texto.contains("tarifa");
        return idaVolta && perguntaTarifa;
    }

    public boolean isConsultaMelhorTarifaAereaIdaVolta(
            String input,
            List<ChatMessageDTO> contexto,
            boolean possuiContextoEstruturado) {
        return isConsultaMelhorTarifaAereaIdaVolta(
                input, contexto, possuiContextoEstruturado, null);
    }

    public boolean isConsultaMelhorTarifaAereaIdaVolta(
            String input,
            List<ChatMessageDTO> contexto,
            boolean possuiContextoEstruturado,
            String tipoViagemContexto) {
        if (isConsultaMelhorTarifaAereaIdaVolta(input)) {
            return true;
        }
        if (input == null || input.isBlank() || isSolicitacaoSomenteIda(input)) {
            return false;
        }
        String texto = normalizarTarifa(input);
        if (temContextoHotel(texto)) {
            return false;
        }
        String ultimaResposta = contexto == null
                ? ""
                : ultimaRespostaAssistenteNormalizada(contexto);
        boolean contextoIdaVolta = "ida_volta".equals(tipoViagemContexto)
                || ultimaResposta.contains("ida e volta")
                || ultimaResposta.contains("total combinado")
                || ultimaResposta.contains("companhias diferentes")
                || ultimaResposta.contains("mesma companhia");
        if (mencionaIdaVolta(texto)) {
            boolean contextoTarifa = contextoIdaVolta
                    || ultimaResposta.contains("a menor tarifa de ")
                    || ultimaResposta.contains("datas mais baratas de ")
                    || ultimaResposta.contains("melhor opcao por cabine");
            return possuiContextoEstruturado || contextoTarifa;
        }
        boolean novaBuscaConvencional = texto.contains("pesquise voo")
                || texto.contains("pesquisar voo")
                || texto.contains("buscar voo")
                || texto.contains("busque voo")
                || texto.contains("ver disponibilidade")
                || texto.contains("consultar disponibilidade");
        if (novaBuscaConvencional) {
            return false;
        }
        boolean refinamento = texto.contains("mesma companhia")
                || texto.contains("mesma cia")
                || texto.contains("cias iguais")
                || texto.contains("companhias iguais")
                || texto.contains("companhias diferentes")
                || texto.contains("companhia diferente")
                || texto.contains("cias diferentes")
                || texto.contains("cias separadas")
                || texto.contains("cias distintas")
                || texto.contains("companhias distintas")
                || texto.contains("compare as companhias")
                || texto.contains("comparar companhias")
                || texto.contains("alternativa")
                || texto.contains("opcao")
                || texto.contains("opcoes")
                || texto.contains("mais opcoes")
                || texto.contains("outras datas")
                || texto.contains("melhores datas")
                || texto.contains("segunda opcao")
                || texto.contains("terceira opcao")
                || texto.contains("compare")
                || texto.contains("comparar")
                || texto.contains("diferenca")
                || texto.contains("mes a mes")
                || texto.contains("cada mes")
                || texto.contains("cabine")
                || texto.contains("economica")
                || texto.contains("premium")
                || texto.contains("executiva")
                || texto.contains("primeira classe")
                || texto.contains("resumo")
                || texto.equals("geral")
                || texto.contains("pode ser geral")
                || texto.contains("todas as cabines")
                || texto.contains("qualquer cabine")
                || texto.contains("sem preferencia")
                || texto.contains("proximo mes")
                || texto.contains("outro mes")
                || texto.contains("duracao")
                || temRotaTarifa(texto)
                || (texto.contains(" para ")
                && (texto.startsWith("agora ") || texto.startsWith("e ")))
                || texto.matches(".*\\b[0-9]+\\s+dias?\\b.*")
                || texto.matches(".*\\b(?:[2-9]|10)\\s+(?:dias|datas|opcoes)\\b.*")
                || texto.matches(".*\\b20[0-9]{2}-[0-9]{2}(?:-[0-9]{2})?\\b.*")
                || texto.matches(".*\\b[0-3]?[0-9]/[01]?[0-9](?:/20[0-9]{2})?\\b.*")
                || texto.matches(".*\\b(janeiro|fevereiro|marco|abril|maio|junho|"
                + "julho|agosto|setembro|outubro|novembro|dezembro)\\b.*");
        return refinamento && contextoIdaVolta;
    }

    private boolean mencionaIdaVolta(String texto) {
        return texto.contains("ida e volta")
                || texto.contains("ida/volta")
                || texto.contains("ida-volta")
                || texto.contains("roundtrip")
                || texto.contains("round trip")
                || texto.contains("bate e volta")
                || texto.contains("com retorno")
                || texto.matches(".*\\bvolta\\b.*");
    }

    private boolean temContextoHotel(String texto) {
        return texto.contains("hotel")
                || texto.contains("hoteis")
                || texto.contains("hospedagem")
                || texto.contains("diaria")
                || texto.contains("check-in")
                || texto.contains("checkout");
    }

    private boolean temRotaTarifa(String texto) {
        return texto.matches(
                ".*\\b[a-z]{3}\\b\\s*(?:para|a|x|/|-)\\s*\\b[a-z]{3}\\b.*")
                || (texto.contains(" para ") && (texto.contains(" de ")
                || texto.contains(" saindo ")))
                || (texto.contains(" entre ") && texto.contains(" e "));
    }

    private String normalizarTarifa(String input) {
        if (input == null) {
            return "";
        }
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    public boolean isConsultaMelhorTarifaAerea(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        String texto = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        boolean perguntaPreco = texto.contains("mais barato")
                || texto.contains("mais barata")
                || texto.contains("menor preco")
                || texto.contains("menor tarifa")
                || texto.contains("melhor tarifa")
                || texto.contains("melhores tarifas")
                || texto.contains("mais em conta")
                || texto.contains("preco por dia")
                || texto.contains("tarifa por dia")
                || texto.contains("dia mais barato");
        boolean contextoHotel = texto.contains("hotel")
                || texto.contains("hospedagem")
                || texto.contains("diaria")
                || texto.contains("check-in")
                || texto.contains("checkout");
        boolean rotaInformada = texto.matches(
                ".*\\b[a-z]{3}\\b\\s*(?:para|a|x|/|-)\\s*\\b[a-z]{3}\\b.*")
                || (texto.contains(" para ") && (texto.contains(" de ")
                || texto.contains(" saindo ")))
                || (texto.contains(" entre ") && texto.contains(" e "));
        return perguntaPreco && rotaInformada && !contextoHotel;
    }

    public boolean isConsultaMelhorTarifaAerea(String input,
                                               List<ChatMessageDTO> contexto) {
        return isConsultaMelhorTarifaAerea(input, contexto, false);
    }

    public boolean isConsultaMelhorTarifaAerea(String input,
                                               List<ChatMessageDTO> contexto,
                                               boolean possuiContextoEstruturado) {
        if (isConsultaMelhorTarifaAerea(input)) {
            return true;
        }
        if (input == null || input.isBlank() || contexto == null) {
            return false;
        }
        String texto = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        if (texto.contains("hotel")
                || texto.contains("hoteis")
                || texto.contains("hospedagem")
                || texto.contains("diaria")
                || texto.contains("check-in")
                || texto.contains("checkout")) {
            return false;
        }
        if (isSolicitacaoSomenteIda(input)) {
            if (possuiContextoEstruturado) {
                return true;
            }
            String ultimaRespostaAssistente = ultimaRespostaAssistenteNormalizada(contexto);
            return ultimaRespostaAssistente.contains("ida e volta")
                    || ultimaRespostaAssistente.contains("total combinado")
                    || ultimaRespostaAssistente.contains("a menor tarifa de ");
        }
        boolean complemento = texto.contains("economica")
                || texto.contains("premium")
                || texto.contains("executiva")
                || texto.contains("primeira classe")
                || texto.contains("cabine")
                || texto.contains("alternativa")
                || texto.contains("mais opcoes")
                || texto.contains("outras datas")
                || texto.contains("melhores datas")
                || texto.contains("compare")
                || texto.contains("comparar")
                || texto.contains("diferenca")
                || texto.contains("mes a mes")
                || texto.contains("cada mes")
                || texto.contains("segunda opcao")
                || texto.contains("terceira opcao")
                || texto.contains("resumo")
                || texto.contains("todas as cabines")
                || texto.contains("pode ser geral")
                || texto.contains("qualquer cabine")
                || texto.contains("sem preferencia")
                || texto.equals("geral")
                || texto.contains("proximo mes")
                || texto.contains("outro mes")
                || texto.matches(".*\\b(?:[2-9]|10)\\s+(?:dias|datas|opcoes)\\b.*")
                || texto.matches(".*\\b20[0-9]{2}-[0-9]{2}\\b.*")
                || texto.matches(".*\\b(janeiro|fevereiro|marco|abril|maio|junho|"
                + "julho|agosto|setembro|outubro|novembro|dezembro)\\b.*");
        if (!complemento) {
            return false;
        }
        if (possuiContextoEstruturado) {
            return true;
        }
        String ultimaRespostaAssistente = ultimaRespostaAssistenteNormalizada(contexto);
        return ultimaRespostaAssistente.contains("a menor tarifa de ")
                || ultimaRespostaAssistente.contains("melhor opcao por cabine")
                || ultimaRespostaAssistente.contains("data mais barata de ")
                || ultimaRespostaAssistente.contains("datas mais baratas de ")
                || ultimaRespostaAssistente.contains(
                        "comparacao das menores tarifas por cabine de ")
                || ultimaRespostaAssistente.contains("melhor dia de cada mes de ")
                || ultimaRespostaAssistente.contains("nao encontrei tarifas de ");
    }

    private String ultimaRespostaAssistenteNormalizada(List<ChatMessageDTO> contexto) {
        for (int i = contexto.size() - 1; i >= 0; i--) {
            ChatMessageDTO message = contexto.get(i);
            if (message == null
                    || !"assistant".equals(message.role())
                    || message.content() == null) {
                continue;
            }
            return Normalizer.normalize(message.content(), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .toLowerCase(Locale.ROOT);
        }
        return "";
    }

    public String identificarTipoConsultaViagem(String input) throws IOException {
        String prompt = """
            Você é um classificador de intenção.
            Analise a mensagem do usuário e responda somente com uma das opções abaixo:

            - aereo
            - hotel
            - desconhecido

            Regras:
            - Responda "hotel" se a mensagem for sobre hospedagem, hotel, estadia, check-in/check-out, quartos, hóspedes, diária.
            - Responda "aereo" se a mensagem for sobre voo, passagem aérea, origem/destino de aeroporto, ida/volta, companhia aérea.
            - Responda "desconhecido" se não estiver claro.

            Responda apenas com uma palavra.
            """;

        String resposta = agenteIADecisor("classificador_viagem", prompt, input);
        return resposta == null || resposta.isBlank() ? "desconhecido" : resposta.trim().toLowerCase(Locale.ROOT);
    }
}
