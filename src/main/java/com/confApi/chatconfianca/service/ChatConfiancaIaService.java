package com.confApi.chatconfianca.service;

import com.confApi.chatconfianca.dto.enums.PrioridadeConversa;
import com.confApi.chatconfianca.dto.enums.RemetenteTipo;
import com.confApi.chatconfianca.dto.model.Conversa;
import com.confApi.chatconfianca.dto.model.DepartamentoUnidade;
import com.confApi.chatconfianca.dto.model.Mensagem;
import com.confApi.chatconfianca.dto.request.PerguntarConfiaRequest;
import com.confApi.chatconfianca.dto.response.ChatConfiancaIaResponse;
import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.chatgpt.dto.ChatMessageDTO;
import com.confApi.chatgpt.dto.ChatRequestDTO;
import com.confApi.chatgpt.dto.ChatResponseDTO;
import com.confApi.chatgpt.dto.ChatActionDTO;
import com.confApi.chatgpt.dto.ConversationRequestDTO;
import com.confApi.chatgpt.profile.ProfilePromptRegistry;
import com.confApi.chatgpt.service.ChatService;
import com.confApi.chatgpt.tools.ToolDefinition;
import com.confApi.chatgpt.tools.ToolSchemas;
import com.confApi.exception.RegraDeNegocioException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ChatConfiancaIaService {
    private final ChatConfiancaService chatConfiancaService;
    private final ChatService chatService;
    private final ProfilePromptRegistry profiles;
    private final ObjectMapper objectMapper;

    public ChatConfiancaIaService(ChatConfiancaService chatConfiancaService,
                                  ChatService chatService,
                                  ProfilePromptRegistry profiles,
                                  ObjectMapper objectMapper) {
        this.chatConfiancaService = chatConfiancaService;
        this.chatService = chatService;
        this.profiles = profiles;
        this.objectMapper = objectMapper;
    }

    public ChatConfiancaIaResponse perguntar(PerguntarConfiaRequest request) {
        validarPergunta(request);

        SessaoChatResponse sessao = chatConfiancaService.montarSessao(request.getCodgUsuario(), request.getCodgAgenciaSessao());
        Conversa conversa = request.getConversaId() == null
                ? null
                : chatConfiancaService.buscarConversa(request.getConversaId());
        List<Mensagem> historico = conversa == null
                ? new ArrayList<>()
                : chatConfiancaService.listarMensagens(conversa.getId(), request.getCodgUsuario(), false, false);

        DepartamentoUnidade departamento = resolverDepartamento(request, conversa);
        if (conversa == null) {
            conversa = chatConfiancaService.iniciarConversaAssistida(
                    request.getCodgUsuario(),
                    departamento.getId(),
                    assuntoOuPadrao(request, departamento),
                    request.getMensagem(),
                    request.getPrioridade(),
                    metadadosInicioConfia(request, departamento),
                    request.getCodgAgenciaSessao()
            );
        }

        Mensagem mensagemUsuario = chatConfiancaService.registrarMensagemUsuarioAssistida(
                conversa.getId(),
                request.getCodgUsuario(),
                request.getMensagem()
        );

        ChatConfiancaIaResponse response = new ChatConfiancaIaResponse();
        response.setConversa(conversa);
        response.setMensagemUsuario(mensagemUsuario);
        response.setDepartamentoSugerido(departamento);

        ChatResponseDTO respostaConfia = chamarConfia(request, sessao, historico);
        JsonNode payloadReservasRecentes = extrairPayloadReservasRecentes(respostaConfia);
        Map<String, Object> payloadMelhoresTarifasAereas =
                extrairPayloadMelhoresTarifasAereas(respostaConfia);
        String resposta = payloadReservasRecentes != null
                ? respostaReservasRecentesOuFallback(payloadReservasRecentes)
                : (respostaConfia == null || isBlank(respostaConfia.content())
                ? fallbackConfia()
                : respostaConfia.content());

        String jsonPesquisaAereo = extrairJsonPesquisaViagem(resposta);
        if (jsonPesquisaAereo != null) {
            response.setResposta(jsonPesquisaAereo);
            response.setSugerirAtendente(false);
            return response;
        }

        response.setResposta(resposta);
        if (respostaConfia != null && respostaConfia.actions() != null) {
            response.getActions().addAll(respostaConfia.actions());
        }
        String acaoSolicitada = chatService.identificarAcaoSolicitadaDeterministica(
                request.getMensagem());
        if (!isBlank(acaoSolicitada) && response.getActions().stream()
                .filter(Objects::nonNull)
                .anyMatch(action -> acaoSolicitada.equals(action.code()))) {
            response.setAcaoSolicitada(acaoSolicitada);
        }
        response.setSugerirAtendente(deveSugerirAtendente(request.getMensagem(), resposta));

        Mensagem mensagemBot = chatConfiancaService.registrarMensagemBot(
                conversa.getId(),
                resposta,
                metadadosRespostaConfia(
                        response,
                        payloadReservasRecentes,
                        payloadMelhoresTarifasAereas)
        );
        response.setMensagemBot(mensagemBot);

        if (Boolean.TRUE.equals(request.getEncaminharAtendente())) {
            Conversa encaminhada = chatConfiancaService.encaminharConversaParaAtendente(
                    conversa.getId(),
                    request.getCodgUsuario(),
                    "Cliente solicitou atendimento humano durante conversa com a ConfIA."
            );
            response.setConversa(encaminhada);
            response.setAtendenteSolicitado(true);
            response.setMensagemAtendente("Encaminhei seu atendimento para a equipe humana.");
        }

        return response;
    }

    public ChatConfiancaIaResponse encaminharAtendente(PerguntarConfiaRequest request) {
        if (request == null || request.getConversaId() == null || request.getCodgUsuario() == null) {
            throw regra("Informe a conversa e o usuario.");
        }
        Conversa conversa = chatConfiancaService.encaminharConversaParaAtendente(
                request.getConversaId(),
                request.getCodgUsuario(),
                isBlank(request.getMensagem())
                        ? "Cliente solicitou atendimento humano."
                        : request.getMensagem()
        );
        ChatConfiancaIaResponse response = new ChatConfiancaIaResponse();
        response.setConversa(conversa);
        response.setAtendenteSolicitado(true);
        response.setSugerirAtendente(false);
        response.setMensagemAtendente("Voce esta aguardando um atendente humano.");
        return response;
    }

    private ChatResponseDTO chamarConfia(PerguntarConfiaRequest request,
                                         SessaoChatResponse sessao,
                                         List<Mensagem> historico) {
        List<ChatMessageDTO> messages = new ArrayList<>();
        try {
            Long codgAgencia = sessao.getAgencia() == null || sessao.getAgencia().getCodgAgencia() == null
                    ? 0L
                    : sessao.getAgencia().getCodgAgencia().longValue();
            ConversationRequestDTO conversation = new ConversationRequestDTO(
                    "confia",
                    sessao.getUnidade() == null ? "Confianca" : sessao.getUnidade().getNomeUnidade(),
                    idErp(sessao),
                    codgAgencia,
                    request.getCodgUsuario().longValue(),
                    request.getMensagem(),
                    messages,
                    null,
                    false,
                    new ArrayList<>()
            );

            if (chatService.isListagemReservasRecentesDeterministica(request.getMensagem())) {
                return chatService.responderListagemReservasRecentes(conversation);
            }

            messages.add(new ChatMessageDTO("system", profiles.systemPrompt(
                    "confia",
                    codgAgencia,
                    request.getCodgUsuario().longValue()
            )));
            messages.addAll(converterHistorico(historico, request.getCodgUsuario()));

            int firstActionMessageIndex = messages.size();
            List<String> keywords = chatService.actionApis(messages, conversation);
            var actions = chatService.extrairAcoesDisponiveis(messages.subList(firstActionMessageIndex, messages.size()));
            messages.add(new ChatMessageDTO("user", request.getMensagem()));

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("codgAgencia", codgAgencia);
            metadata.put("codgUsuario", request.getCodgUsuario());
            metadata.put("origem", "chat-confianca");
            Map<String, Object> contextoLocalTarifas =
                    extrairContextoLocalMelhoresTarifas(historico);
            if (!contextoLocalTarifas.isEmpty()) {
                // Metadado consumido apenas pelo ChatService; nao integra o payload OpenAI.
                metadata.put("contextoLocalMelhoresTarifasAereas", contextoLocalTarifas);
            }

            ChatResponseDTO resposta = chatService.chat(
                    new ChatRequestDTO(
                            messages,
                            null,
                            false,
                            tools(
                                    request.getMensagem(),
                                    messages,
                                    !contextoLocalTarifas.isEmpty(),
                                    Objects.toString(
                                            contextoLocalTarifas.get("tipoViagem"), null)),
                            metadata),
                    keywords,
                    null
            );
            if (resposta == null) {
                return null;
            }
            List<ChatActionDTO> todasAcoes = mesclarAcoes(actions, resposta.actions());
            return new ChatResponseDTO(
                    resposta.id(),
                    resposta.content(),
                    resposta.toolCalls(),
                    resposta.audio(),
                    resposta.keywords(),
                    resposta.history(),
                    todasAcoes
            );
        } catch (Exception ex) {
            return new ChatResponseDTO(
                    null,
                    null,
                    new ArrayList<>(),
                    null,
                    new ArrayList<>(),
                    messages,
                    chatService.extrairAcoesDisponiveis(messages));
        }
    }

    private List<ToolDefinition> tools(String mensagem,
                                       List<ChatMessageDTO> contexto,
                                       boolean possuiContextoEstruturadoTarifas,
                                       String tipoViagemContexto) throws IOException {
        if (chatService.isConsultaMelhorTarifaAereaIdaVolta(
                mensagem, contexto, possuiContextoEstruturadoTarifas,
                tipoViagemContexto)) {
            return List.of(ToolSchemas.searchCheapestRoundtripAirfares());
        }
        if (chatService.isConsultaMelhorTarifaAerea(
                mensagem, contexto, possuiContextoEstruturadoTarifas)) {
            return List.of(ToolSchemas.searchCheapestAirfares());
        }
        String tipoConsulta = chatService.identificarTipoConsultaViagem(mensagem);
        if ("aereo".equals(tipoConsulta)) {
            return List.of(ToolSchemas.searchFlights());
        }
        if ("hotel".equals(tipoConsulta)) {
            return List.of(ToolSchemas.searchHotels());
        }
        return List.of();
    }

    private List<ChatActionDTO> mesclarAcoes(List<ChatActionDTO> primeiras,
                                             List<ChatActionDTO> adicionais) {
        List<ChatActionDTO> resultado = new ArrayList<>();
        if (primeiras != null) {
            resultado.addAll(primeiras);
        }
        if (adicionais == null) {
            return resultado;
        }
        for (ChatActionDTO action : adicionais) {
            if (action == null) {
                continue;
            }
            boolean duplicada = resultado.stream()
                    .filter(Objects::nonNull)
                    .anyMatch(item -> Objects.equals(item.code(), action.code())
                            && Objects.equals(item.localizador(), action.localizador()));
            if (!duplicada) {
                resultado.add(action);
            }
        }
        return resultado;
    }

    private List<ChatMessageDTO> converterHistorico(List<Mensagem> historico, Integer codgUsuario) {
        if (historico == null || historico.isEmpty()) {
            return new ArrayList<>();
        }
        return historico.stream()
                .filter(Objects::nonNull)
                .filter(item -> !isBlank(item.getConteudo()))
                .map(item -> new ChatMessageDTO(roleHistorico(item, codgUsuario), item.getConteudo()))
                .collect(Collectors.toList());
    }

    private Map<String, Object> extrairContextoLocalMelhoresTarifas(
            List<Mensagem> historico) {
        if (historico == null || historico.isEmpty()) {
            return Map.of();
        }
        for (int i = historico.size() - 1; i >= 0; i--) {
            Mensagem mensagem = historico.get(i);
            if (mensagem == null || mensagem.getRemetenteTipo() != RemetenteTipo.BOT) {
                continue;
            }
            if (isBlank(mensagem.getConteudoJson())) {
                return Map.of();
            }
            try {
                JsonNode raiz = objectMapper.readTree(mensagem.getConteudoJson());
                JsonNode payloadIdaVolta = raiz.path("melhoresTarifasAereasIdaVolta");
                if (payloadIdaVolta.isObject()
                        && "chat.melhores-tarifas-aereas-ida-volta.v1".equals(
                                payloadIdaVolta.path("schema").asText())) {
                    Map<String, Object> contexto = contextoIdaVolta(payloadIdaVolta);
                    return contexto.containsKey("origem") && contexto.containsKey("destino")
                            ? contexto
                            : Map.of();
                }
                JsonNode payload = raiz.path("melhoresTarifasAereas");
                if (!payload.isObject() || !"chat.melhores-tarifas-aereas.v1".equals(
                        payload.path("schema").asText())) {
                    return Map.of();
                }
                Map<String, Object> contexto = new LinkedHashMap<>();
                contexto.put("tipoViagem", "somente_ida");
                copiarIataContexto(payload, contexto, "origem");
                copiarIataContexto(payload, contexto, "destino");
                copiarCabineContexto(payload, contexto);
                copiarDataContexto(payload, contexto, "periodoInicio");
                copiarDataContexto(payload, contexto, "periodoFim");
                copiarModoContexto(payload, contexto);
                int quantidade = payload.path("quantidadeAplicada").asInt(0);
                if (quantidade >= 1 && quantidade <= 10) {
                    contexto.put("limiteAlternativas", quantidade);
                }
                if (contexto.containsKey("origem") && contexto.containsKey("destino")) {
                    return contexto;
                }
            } catch (Exception ignored) {
                // Historico legado ou malformado nao deve bloquear a conversa.
            }
            return Map.of();
        }
        return Map.of();
    }

    private Map<String, Object> contextoIdaVolta(JsonNode payload) {
        Map<String, Object> contexto = new LinkedHashMap<>();
        contexto.put("tipoViagem", "ida_volta");
        copiarIataContexto(payload, contexto, "origem");
        copiarIataContexto(payload, contexto, "destino");
        copiarCabineContexto(payload, contexto);
        copiarDataContexto(payload, contexto, "dataIdaInicio");
        copiarDataContexto(payload, contexto, "dataIdaFim");
        copiarDataContexto(payload, contexto, "dataVoltaInicio");
        copiarDataContexto(payload, contexto, "dataVoltaFim");
        if (contexto.containsKey("dataIdaInicio")) {
            contexto.put("periodoInicio", contexto.get("dataIdaInicio"));
        }
        if (contexto.containsKey("dataIdaFim")) {
            contexto.put("periodoFim", contexto.get("dataIdaFim"));
        }
        copiarInteiroContexto(payload, contexto, "duracaoMinimaDias", 1, 365);
        copiarInteiroContexto(payload, contexto, "duracaoMaximaDias", 1, 365);
        copiarInteiroContexto(payload, contexto, "quantidadeAplicada", 1, 10,
                "limiteAlternativas");
        String politica = payload.path("politicaCompanhia").asText("")
                .toLowerCase(Locale.ROOT);
        if (List.of("comparar", "mesma", "diferentes").contains(politica)) {
            contexto.put("politicaCompanhia", politica);
        }
        String modo = payload.path("modoResposta").asText("").toLowerCase(Locale.ROOT);
        if (List.of("resumo", "alternativas", "companhias").contains(modo)) {
            contexto.put("modoResposta", modo);
        }
        return contexto;
    }

    private void copiarInteiroContexto(JsonNode payload,
                                       Map<String, Object> contexto,
                                       String campo,
                                       int minimo,
                                       int maximo) {
        copiarInteiroContexto(payload, contexto, campo, minimo, maximo, campo);
    }

    private void copiarInteiroContexto(JsonNode payload,
                                       Map<String, Object> contexto,
                                       String campo,
                                       int minimo,
                                       int maximo,
                                       String destino) {
        if (!payload.path(campo).canConvertToInt()) {
            return;
        }
        int valor = payload.path(campo).asInt();
        if (valor >= minimo && valor <= maximo) {
            contexto.put(destino, valor);
        }
    }

    private void copiarIataContexto(JsonNode payload,
                                    Map<String, Object> contexto,
                                    String campo) {
        String iata = payload.path(campo).asText("").trim().toUpperCase(Locale.ROOT);
        if (iata.matches("[A-Z]{3}")) {
            contexto.put(campo, iata);
        }
    }

    private void copiarCabineContexto(JsonNode payload,
                                      Map<String, Object> contexto) {
        String cabine = payload.path("cabine").asText("").trim().toUpperCase(Locale.ROOT);
        if (cabine.matches("[YWCF]")) {
            contexto.put("cabine", cabine);
        }
    }

    private void copiarDataContexto(JsonNode payload,
                                    Map<String, Object> contexto,
                                    String campo) {
        String valor = payload.path(campo).asText(null);
        if (valor == null) {
            return;
        }
        try {
            contexto.put(campo, LocalDate.parse(valor).toString());
        } catch (RuntimeException ignored) {
            // Ignora datas legadas invalidas.
        }
    }

    private void copiarModoContexto(JsonNode payload,
                                    Map<String, Object> contexto) {
        String modo = payload.path("modoResposta").asText("").toLowerCase(Locale.ROOT);
        if (List.of("resumo", "alternativas", "cabines", "mensal").contains(modo)) {
            contexto.put("modoResposta", modo);
        }
    }

    private String roleHistorico(Mensagem mensagem, Integer codgUsuario) {
        if (mensagem.getRemetenteTipo() == RemetenteTipo.BOT) {
            return "assistant";
        }
        if (mensagem.getRemetenteTipo() == RemetenteTipo.SISTEMA) {
            return "system";
        }
        return Objects.equals(mensagem.getRemetenteCodgUsuario(), codgUsuario) ? "user" : "assistant";
    }

    private DepartamentoUnidade resolverDepartamento(PerguntarConfiaRequest request, Conversa conversa) {
        List<DepartamentoUnidade> departamentos = chatConfiancaService.listarDepartamentosDisponiveisPorUsuario(request.getCodgUsuario(), request.getCodgAgenciaSessao());
        if (departamentos == null || departamentos.isEmpty()) {
            throw regra("Nenhum departamento disponivel para o chat.");
        }
        Long departamentoId = conversa != null ? conversa.getDepartamentoUnidadeId() : request.getDepartamentoUnidadeId();
        if (departamentoId != null) {
            return departamentos.stream()
                    .filter(item -> departamentoId.equals(item.getId()))
                    .findFirst()
                    .orElse(departamentos.get(0));
        }
        return departamentos.stream()
                .max((a, b) -> Integer.compare(scoreDepartamento(a, request.getMensagem()), scoreDepartamento(b, request.getMensagem())))
                .orElse(departamentos.get(0));
    }

    private int scoreDepartamento(DepartamentoUnidade departamento, String mensagem) {
        String texto = normalizar((departamento.getNomeExibicao() == null ? "" : departamento.getNomeExibicao())
                + " " + (departamento.getMensagemAbertura() == null ? "" : departamento.getMensagemAbertura()));
        String entrada = normalizar(mensagem);
        int score = 0;
        for (String termo : entrada.split(" ")) {
            if (termo.length() > 3 && texto.contains(termo)) {
                score += 3;
            }
        }
        score += scorePorGrupo(texto, entrada, "financeiro", "limite", "fatura", "boleto", "cobranca", "pagamento");
        score += scorePorGrupo(texto, entrada, "reembolso", "cancelamento", "devolucao", "estorno");
        score += scorePorGrupo(texto, entrada, "aereo", "voo", "localizador", "reserva", "bilhete", "emissao", "checkin");
        score += scorePorGrupo(texto, entrada, "hotel", "hospedagem", "diaria");
        score += scorePorGrupo(texto, entrada, "suporte", "sistema", "erro", "acesso", "senha", "tecnologia");
        return score;
    }

    private int scorePorGrupo(String departamento, String entrada, String... termos) {
        boolean deptCombina = false;
        boolean entradaCombina = false;
        for (String termo : termos) {
            deptCombina = deptCombina || departamento.contains(termo);
            entradaCombina = entradaCombina || entrada.contains(termo);
        }
        return deptCombina && entradaCombina ? 10 : 0;
    }

    private boolean deveSugerirAtendente(String pergunta, String resposta) {
        String texto = normalizar((pergunta == null ? "" : pergunta) + " " + (resposta == null ? "" : resposta));
        return texto.contains("falar com atendente")
                || texto.contains("atendente humano")
                || texto.contains("nao consegui")
                || texto.contains("nao posso")
                || texto.contains("encaminhar")
                || texto.contains("reembolso")
                || texto.contains("cancelamento")
                || texto.contains("estorno");
    }

    private String assuntoOuPadrao(PerguntarConfiaRequest request, DepartamentoUnidade departamento) {
        if (!isBlank(request.getAssunto())) {
            return request.getAssunto().trim();
        }
        if (!isBlank(request.getMensagem())) {
            return request.getMensagem().trim().length() > 80
                    ? request.getMensagem().trim().substring(0, 80)
                    : request.getMensagem().trim();
        }
        return departamento.getNomeExibicao();
    }

    private String metadadosInicioConfia(PerguntarConfiaRequest request, DepartamentoUnidade departamento) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("origem", "CONFIA");
        dados.put("departamentoSugeridoId", departamento.getId());
        dados.put("departamentoSugerido", departamento.getNomeExibicao());
        dados.put("prioridade", request.getPrioridade() == null ? PrioridadeConversa.NORMAL : request.getPrioridade());
        try {
            return objectMapper.writeValueAsString(dados);
        } catch (JsonProcessingException e) {
            return null;
        }
    }


    private String extrairJsonPesquisaViagem(String conteudo) {
        JsonNode json = lerJsonRespostaConfia(conteudo);
        if (!isRespostaAereo(json) && !isRespostaHotel(json)) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(json);
        } catch (JsonProcessingException e) {
            return json.toString();
        }
    }

    private JsonNode lerJsonRespostaConfia(String conteudo) {
        if (isBlank(conteudo)) {
            return null;
        }
        try {
            String jsonLimpo = conteudo
                    .replace("<br/>", "")
                    .replace("<br>", "")
                    .replace("Segue Json Completo.", "")
                    .trim();
            int inicio = jsonLimpo.indexOf('{');
            int fim = jsonLimpo.lastIndexOf('}');
            if (inicio >= 0 && fim > inicio) {
                jsonLimpo = jsonLimpo.substring(inicio, fim + 1);
            }
            return objectMapper.readTree(jsonLimpo);
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isRespostaAereo(JsonNode json) {
        if (json == null || json.isMissingNode() || !json.isObject()) {
            return false;
        }
        String tipo = textoJson(json, "tipo");
        if ("hotel".equalsIgnoreCase(tipo)) {
            return false;
        }
        if ("aereo".equalsIgnoreCase(tipo) || "voo".equalsIgnoreCase(tipo)) {
            return true;
        }
        return "OK".equalsIgnoreCase(textoJson(json, "status"))
                && isTextoJson(json, "origem")
                && isTextoJson(json, "destino")
                && (isTextoJson(json, "dataIda") || isTextoJson(json, "mesIda"));
    }

    private boolean isRespostaHotel(JsonNode json) {
        if (json == null || json.isMissingNode() || !json.isObject()) {
            return false;
        }
        String tipo = textoJson(json, "tipo");
        if ("hotel".equalsIgnoreCase(tipo)) {
            return true;
        }
        if ("OK".equalsIgnoreCase(textoJson(json, "statusHotel"))) {
            return true;
        }
        return isTextoJson(json, "dataEntrada")
                && isTextoJson(json, "dataSaida")
                && isTextoJson(json, "nomeCidade");
    }

    private boolean isTextoJson(JsonNode json, String campo) {
        String valor = textoJson(json, campo);
        return valor != null && !valor.trim().isEmpty();
    }

    private String textoJson(JsonNode json, String campo) {
        if (json == null || !json.has(campo) || json.path(campo).isNull()) {
            return null;
        }
        return json.path(campo).asText(null);
    }
    private JsonNode extrairPayloadReservasRecentes(ChatResponseDTO response) {
        if (response == null || response.history() == null) {
            return null;
        }
        List<ChatMessageDTO> history = response.history();
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessageDTO message = history.get(i);
            if (message == null || !"system".equals(message.role())) {
                continue;
            }
            JsonNode json = lerJsonRespostaConfia(message.content());
            if (json != null
                    && "chat.reservas-recentes.v1".equals(json.path("schema").asText())
                    && json.path("reservasRecentes").isObject()) {
                return json;
            }
        }
        return null;
    }

    private Map<String, Object> extrairPayloadMelhoresTarifasAereas(
            ChatResponseDTO response) {
        if (response == null || response.toolCalls() == null) {
            return null;
        }
        for (int i = response.toolCalls().size() - 1; i >= 0; i--) {
            var toolCall = response.toolCalls().get(i);
            if (toolCall == null
                    || !("search_cheapest_airfares".equals(toolCall.name())
                    || "search_cheapest_roundtrip_airfares".equals(toolCall.name()))
                    || toolCall.arguments() == null) {
                continue;
            }
            Object schema = toolCall.arguments().get("schema");
            if ("chat.melhores-tarifas-aereas.v1".equals(schema)
                    || "chat.melhores-tarifas-aereas-ida-volta.v1".equals(schema)) {
                return new LinkedHashMap<>(toolCall.arguments());
            }
        }
        return null;
    }

    private String respostaReservasRecentesOuFallback(JsonNode payload) {
        if (payload == null) {
            return fallbackConfia();
        }
        JsonNode recentes = payload.path("reservasRecentes");
        String status = recentes.path("status").asText("OK");
        String mensagem = recentes.path("mensagem").asText(null);
        if (!"OK".equalsIgnoreCase(status)) {
            return isBlank(mensagem)
                    ? "Nao foi possivel consultar as reservas recentes agora. Tente novamente."
                    : mensagem;
        }
        int quantidade = recentes.path("quantidade").asInt(0);
        if (quantidade == 0) {
            return isBlank(mensagem)
                    ? "Nao encontrei reservas aereas recentes para sua agencia."
                    : mensagem;
        }
        return quantidade == 1
                ? "Encontrei 1 reserva recente da sua agencia."
                : "Encontrei " + quantidade + " reservas recentes da sua agencia.";
    }

    private String metadadosRespostaConfia(
            ChatConfiancaIaResponse response,
            JsonNode payloadReservasRecentes,
            Map<String, Object> payloadMelhoresTarifasAereas) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("origem", "CONFIA");
        dados.put("sugerirAtendente", response.isSugerirAtendente());
        dados.put("acaoSolicitada", response.getAcaoSolicitada());
        dados.put("actions", response.getActions());
        if (payloadReservasRecentes != null) {
            dados.put("schema", payloadReservasRecentes.path("schema").asText());
            dados.put("reservasRecentes", objectMapper.convertValue(
                    payloadReservasRecentes.path("reservasRecentes"), Object.class));
        }
        if (payloadMelhoresTarifasAereas != null) {
            Object schema = payloadMelhoresTarifasAereas.get("schema");
            if ("chat.melhores-tarifas-aereas-ida-volta.v1".equals(schema)) {
                dados.put("melhoresTarifasAereasIdaVolta", payloadMelhoresTarifasAereas);
            } else {
                dados.put("melhoresTarifasAereas", payloadMelhoresTarifasAereas);
            }
        }
        try {
            return objectMapper.writeValueAsString(dados);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String fallbackConfia() {
        return "Nao consegui responder isso com seguranca agora. Posso encaminhar para um atendente humano.";
    }

    private String idErp(SessaoChatResponse sessao) {
        if (sessao.getAgencia() != null && !isBlank(sessao.getAgencia().getCodgSistemaBackoffice())) {
            return sessao.getAgencia().getCodgSistemaBackoffice();
        }
        if (sessao.getUnidade() != null && !isBlank(sessao.getUnidade().getCodgSistemaBackoffice())) {
            return sessao.getUnidade().getCodgSistemaBackoffice();
        }
        return "confia";
    }

    private void validarPergunta(PerguntarConfiaRequest request) {
        if (request == null || request.getCodgUsuario() == null) {
            throw regra("Informe o usuario.");
        }
        if (isBlank(request.getMensagem())) {
            throw regra("Informe a mensagem.");
        }
    }

    private RegraDeNegocioException regra(String mensagem) {
        return new RegraDeNegocioException(400, mensagem);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizar(String value) {
        if (value == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT);
    }
}
