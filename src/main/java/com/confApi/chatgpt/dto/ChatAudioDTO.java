package com.confApi.chatgpt.dto;

public record ChatAudioDTO(
        String format,       // ex: "mp3"
        String mediaType,    // ex: "audio/mpeg"
        String base64,       // audio em Base64
        Integer lengthBytes  // tamanho em bytes
) {}
