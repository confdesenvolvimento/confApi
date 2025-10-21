package com.confApi.chatgpt.controller;

import com.confApi.chatgpt.dto.TTSRequestDTO;
import com.confApi.chatgpt.service.SpeechToTextService;
import com.confApi.chatgpt.service.TextToSpeechService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

// AudioController.java
@RestController
@RequestMapping("/v1/ai")
@RequiredArgsConstructor
public class AudioController {
    private final SpeechToTextService stt;
    private final TextToSpeechService tts;

    @PostMapping(value="/stt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String,String> stt(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required=false) String model,
            @RequestParam(required=false) String language) throws IOException {
        String text = stt.transcribe(file, model, language);
        return Map.of("text", text);
    }

    @PostMapping(value="/tts", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> tts(@RequestBody TTSRequestDTO req) throws IOException {
        byte[] audio = tts.synthesize(req.text(), req.model(), req.voice(), req.format());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=voice."+ (req.format()==null? "mp3": req.format()))
                .body(audio);
    }
}
