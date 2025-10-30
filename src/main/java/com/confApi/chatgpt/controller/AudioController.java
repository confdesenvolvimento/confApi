package com.confApi.chatgpt.controller;

import com.confApi.chatgpt.dto.*;
import com.confApi.chatgpt.profile.ProfilePromptRegistry;
import com.confApi.chatgpt.service.ChatService;
import com.confApi.chatgpt.service.SpeechToTextService;
import com.confApi.chatgpt.service.TextToSpeechService;
import com.confApi.db.confManager.chatMemoria.ChatMemoriaService;
import com.confApi.hub.limites.LimitesService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AudioController.java
 */
@RestController
@RequestMapping("/v1/ai")
@RequiredArgsConstructor
public class AudioController {
    private final SpeechToTextService stt;
    private final TextToSpeechService tts;

    private final ProfilePromptRegistry profiles;
    private final ChatMemoriaService chatMemoriaService;
    private final LimitesService limitesService;
    private final ChatService chatService;

    private final ObjectMapper mapper;

    @PostMapping(value = "/stt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> stt(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String language
    ) throws IOException {
        String text = stt.transcribe(file, model, language);
        return Map.of("text", text);
    }

    @PostMapping(value = "/sttrequest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatResponseDTO> sttRequest(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "request", required = false) String requestJson, // <- recebe como String
            @RequestPart(value = "clientMessageId", required = false) String clientMessageId,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String language
    ) throws IOException, NoSuchAlgorithmException {
        // Parse seguro do JSON opcional
        ConversationRequestDTO request = null;
        if (requestJson != null && !requestJson.isBlank()) {
            request = mapper.readValue(requestJson, ConversationRequestDTO.class);
        }

        String mdl = "gpt-4o-mini-transcribe";
        String lang = (language != null && !language.isBlank()) ? language : "pt";

        // 1) Transcreve
        String transcript = stt.transcribe(file, mdl, lang);

        // 2) Monta mensagens para o chat
        List<ChatMessageDTO> messages = new ArrayList<>();
        if (request != null && request.history() != null && !request.history().isEmpty()) {
            messages.addAll(request.history());
        }
        chatService.actionApis(messages, request);

        String ident = (request != null && request.identificacao() != null) ? request.identificacao() : "chat";
        Long codAg = (request != null) ? request.codgAgencia() : null;
        Long codUsr = (request != null) ? request.codgUsuario() : null;

        String sys = profiles.systemPrompt(ident, codAg, codUsr);
        messages.add(new ChatMessageDTO("system", sys));
        messages.add(new ChatMessageDTO("user", transcript));

        Map<String, Object> metadata = new HashMap<>();
        if (request != null) {
            metadata.put("codgAgencia", request.codgAgencia());
            metadata.put("codgUsuario", request.codgUsuario());
            metadata.put("identificacao", request.identificacao());
            if (request.idErp() != null) metadata.put("idErp", request.idErp());
        }

        ChatRequestDTO chatReq = new ChatRequestDTO(
                messages,
                "gpt-4o-mini",                 // usa o modelo resolvido
                false,               // stream
                List.of(),           // toolCalls requisitados
                metadata
        );

        ChatResponseDTO chat = chatService.chat(chatReq, request.keywords(), request.history());

        byte[] bytes = null;
        ChatAudioDTO audio = null;
        String reply = chat.content() == null ? "" : chat.content().trim();
        if (!reply.isEmpty()) {
            bytes = tts.synthesize(reply, null, null, "mp3");
            System.out.println("Bytes recebidos: " + bytes.length);
            String b64 = java.util.Base64.getEncoder().encodeToString(bytes);
            audio = new ChatAudioDTO("mp3", "audio/mpeg", b64, bytes.length);
        }
/*NETAO AJUSTARMOS AQUI*/
        ChatResponseDTO body = new ChatResponseDTO(chat.id(), chat.content(), chat.toolCalls(), audio, null, null);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Transcript", transcript);
        if (clientMessageId != null && !clientMessageId.isBlank()) {
            headers.add("X-Client-Message-Id", clientMessageId);
        }
        if (bytes != null) {
            headers.add("X-Audio-Length", String.valueOf(bytes.length));
            headers.add("X-Audio-Format", "mp3");
            headers.add("X-Audio-Mime", "audio/mpeg");
            // hash e preview para auditoria
            String sha256 = java.util.HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
            String head16 = java.util.HexFormat.of().formatHex(
                    java.util.Arrays.copyOfRange(bytes, 0, Math.min(16, bytes.length)));
            headers.add("X-Audio-SHA256", sha256);
            headers.add("X-Audio-Head16", head16);
        }
        headers.add("Access-Control-Expose-Headers",
                "X-Transcript,X-Client-Message-Id,X-Audio-Length,X-Audio-Format,X-Audio-Mime,X-Audio-SHA256,X-Audio-Head16");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);

//        // 3) Responde via chat (mantém contrato atual do app que lê content/toolCalls)
//        ChatResponseDTO response = chatService.chat(chatReq);
//        System.out.println("RESPOSTA DO CHAT: " + response);
//
//        return null;
    }

    @PostMapping(value = "/tts", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> tts(@RequestBody TTSRequestDTO req) throws IOException {
        byte[] audio = tts.synthesize(req.text(), req.model(), req.voice(), req.format());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=voice." + (req.format() == null ? "mp3" : req.format()))
                .body(audio);
    }

}
