package com.confApi.chatconfianca.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AnexoDownloadResponse {
    private String nomeArquivo;
    private String mimeType;
    private byte[] conteudo;
}