package com.confApi.chatgpt.controller;

import com.confApi.chatgpt.dto.*;
import com.confApi.chatgpt.profile.ProfilePromptRegistry;
import com.confApi.chatgpt.service.ChatService;

import com.confApi.chatgpt.tools.ToolDefinition;
import com.confApi.chatgpt.tools.ToolSchemas;
import com.confApi.db.confManager.chatMemoria.ChatMemoriaService;
import com.confApi.db.confManager.chatMemoria.dto.ChatMemoria;
import com.confApi.hub.limites.LimitesService;
import com.confApi.hub.limites.dto.Disponibilidade;
import com.confApi.hub.limites.dto.LimiteCreditoRQ;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.validation.Valid;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/v1/ai/conversation")
@RequiredArgsConstructor
public class ConversationController {



    private final ChatService chatService;
    private final ProfilePromptRegistry profiles;

    @PostMapping(value = "/chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public ChatResponseDTO chat(@Valid @RequestBody ConversationRequestDTO req) throws IOException {
        List<ChatMessageDTO> messages = new ArrayList<>();
        List<ChatMessageDTO> history = req.history() == null ? new ArrayList<>() : req.history();
        List<String> keywords = req.keywords() == null ? new ArrayList<>() : req.keywords();

        if (history.isEmpty()) {
            String sys = profiles.systemPrompt("confia", req.codgAgencia(), req.codgUsuario());
            ChatMessageDTO system = new ChatMessageDTO("system", sys);
            messages.add(system);
        }

        if (!history.isEmpty()) {
            messages.addAll(history);
        }

        int firstActionMessageIndex = messages.size();
        keywords = chatService.actionApis(messages, req);
        List<ChatActionDTO> actions = chatService.extrairAcoesDisponiveis(
                messages.subList(firstActionMessageIndex, messages.size())
        );

        messages.add(new ChatMessageDTO("user", req.input()));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("codgAgencia", req.codgAgencia());
        metadata.put("codgUsuario", req.codgUsuario());
        metadata.put("identificacao", req.identificacao());

        List<ToolDefinition> tools = new ArrayList<>();

        String tipoConsulta = chatService.identificarTipoConsultaViagem(req.input());
        //Aqui é para identificar se é busca de aereo ou hotel
        if ("aereo".equals(tipoConsulta)) {
            tools.add(ToolSchemas.searchFlights());
        } else if ("hotel".equals(tipoConsulta)) {
            tools.add(ToolSchemas.searchHotels());
        }

        ChatRequestDTO chatReq = new ChatRequestDTO(
                messages,
                req.model(),
                false,
                tools,
                metadata
        );

        ChatResponseDTO charResp = chatService.chat(chatReq, keywords, messages);

        if (keywords != null && !keywords.isEmpty() && charResp.keywords() != null) {
            keywords.removeIf(Objects::isNull);
            for (String kw : keywords) {
                if (!charResp.keywords().contains(kw)) {
                    charResp.keywords().add(kw);
                }
            }
        }

        return new ChatResponseDTO(
                charResp.id(),
                charResp.content(),
                charResp.toolCalls(),
                charResp.audio(),
                charResp.keywords(),
                charResp.history(),
                actions
        );
    }


    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@Valid @RequestBody ConversationRequestDTO req) {
        String sys = profiles.systemPrompt(req.identificacao(), req.codgAgencia(), req.codgUsuario());
        ChatMessageDTO system = new ChatMessageDTO("system", sys);

        List<ChatMessageDTO> messages = new ArrayList<>();
        messages.add(system);
        if (req.history() != null && !req.history().isEmpty()) {
            messages.addAll(req.history());
        }
        messages.add(new ChatMessageDTO("user", req.input()));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("codgAgencia", req.codgAgencia());
        metadata.put("codgUsuario", req.codgUsuario());
        metadata.put("identificacao", req.identificacao());

        ChatRequestDTO chatReq = new ChatRequestDTO(
                messages,
                req.model(),
                true,     // stream on neste endpoint
                List.of(),
                metadata
        );

        return chatService.stream(chatReq);
    }


}
