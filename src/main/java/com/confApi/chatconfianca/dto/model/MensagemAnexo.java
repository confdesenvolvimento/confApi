package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class MensagemAnexo {
    private Long id;
    private Long mensagemId;
    private String nomeOriginal;
    private String nomeArmazenado;
    private String caminhoStorage;
    private String urlPublica;
    private String mimeType;
    private Long tamanhoBytes;
    private String hashSha256;
    private LocalDateTime criadoEm;
}