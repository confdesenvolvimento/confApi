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
import java.util.ArrayList;
import java.util.HashMap;
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
        String resposta = respostaConfia == null || isBlank(respostaConfia.content())
                ? fallbackConfia()
                : respostaConfia.content();

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
        response.setSugerirAtendente(deveSugerirAtendente(request.getMensagem(), resposta));

        Mensagem mensagemBot = chatConfiancaService.registrarMensagemBot(
                conversa.getId(),
                resposta,
                metadadosRespostaConfia(response)
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
        try {
            Long codgAgencia = sessao.getAgencia() == null || sessao.getAgencia().getCodgAgencia() == null
                    ? 0L
                    : sessao.getAgencia().getCodgAgencia().longValue();
            List<ChatMessageDTO> messages = new ArrayList<>();
            messages.add(new ChatMessageDTO("system", profiles.systemPrompt(
                    "confia",
                    codgAgencia,
                    request.getCodgUsuario().longValue()
            )));
            messages.addAll(converterHistorico(historico, request.getCodgUsuario()));

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

            int firstActionMessageIndex = messages.size();
            List<String> keywords = chatService.actionApis(messages, conversation);
            var actions = chatService.extrairAcoesDisponiveis(messages.subList(firstActionMessageIndex, messages.size()));
            messages.add(new ChatMessageDTO("user", request.getMensagem()));

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("codgAgencia", codgAgencia);
            metadata.put("codgUsuario", request.getCodgUsuario());
            metadata.put("origem", "chat-confianca");

            ChatResponseDTO resposta = chatService.chat(
                    new ChatRequestDTO(messages, null, false, tools(request.getMensagem()), metadata),
                    keywords,
                    messages
            );
            if (resposta == null) {
                return null;
            }
            return new ChatResponseDTO(
                    resposta.id(),
                    resposta.content(),
                    resposta.toolCalls(),
                    resposta.audio(),
                    resposta.keywords(),
                    resposta.history(),
                    actions
            );
        } catch (Exception ex) {
            return null;
        }
    }

    private List<ToolDefinition> tools(String mensagem) throws IOException {
        String tipoConsulta = chatService.identificarTipoConsultaViagem(mensagem);
        if ("aereo".equals(tipoConsulta)) {
            return List.of(ToolSchemas.searchFlights());
        }
        if ("hotel".equals(tipoConsulta)) {
            return List.of(ToolSchemas.searchHotels());
        }
        return List.of();
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
    private String metadadosRespostaConfia(ChatConfiancaIaResponse response) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("origem", "CONFIA");
        dados.put("sugerirAtendente", response.isSugerirAtendente());
        dados.put("actions", response.getActions());
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
