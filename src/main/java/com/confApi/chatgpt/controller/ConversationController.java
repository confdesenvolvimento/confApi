package com.confApi.chatgpt.controller;

import com.confApi.chatgpt.dto.*;
import com.confApi.chatgpt.profile.ProfilePromptRegistry;
import com.confApi.chatgpt.service.ChatService;

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
        // 1) Mensagem de sistema baseada na identificação (perfil)
        //req.identificacao()  nao estou usando por enquanto
        String sys = profiles.systemPrompt("confia", req.codgAgencia(), req.codgUsuario());
        ChatMessageDTO system = new ChatMessageDTO("system", sys);

        // 2) Histórico (se vier) + dados adicionais do sistema + input atual
        List<ChatMessageDTO> messages = new ArrayList<>();
        messages.add(system);

        // 2.1) Histórico já tokenizado vindo do front
        if (req.history() != null && !req.history().isEmpty()) {
            messages.addAll(req.history());
        }

        // 2.2) (Opcional) Dados do sistema como no modelo antigo
        //     Caso você envie um header CSV "X-User-Data: chave1=valor1,chave2=valor2"
        chatService.actionApis(messages, req);
        /* if (userDataHeader != null && !userDataHeader.isBlank()) {
            for (String data : userDataHeader.split(",")) {
                String trimmed = data.trim();
                if (!trimmed.isEmpty()) {
                    // Mantém a mesma ideia do modelo antigo, mas agora como mensagem 'system' dedicada
                    messages.add(new ChatMessageDTO("system", "Dado do sistema: " + trimmed));
                }
            }
        }*/

        // 2.3) Mensagem atual do usuário
        messages.add(new ChatMessageDTO("user", req.input()));

        // 3) Metadados úteis (chegam no log/observabilidade do seu ChatService)
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("codgAgencia", req.codgAgencia());
        metadata.put("codgUsuario", req.codgUsuario());
        metadata.put("identificacao", req.identificacao());

        // 4) Monta o ChatRequestDTO
        ChatRequestDTO chatReq = new ChatRequestDTO(
                messages,
                req.model(),           // pode vir nulo -> ChatService usa default
                false,                 // stream off no endpoint /chat
                List.of(),             // tools podem ser injetadas conforme cenário
                metadata
        );

        // 5) Chama o serviço e retorna a resposta
        return chatService.chat(chatReq);
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
