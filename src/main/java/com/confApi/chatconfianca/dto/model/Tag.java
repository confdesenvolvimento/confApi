package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class Tag {
    private Long id;
    private String nome;
    private String corHex;
    private Boolean ativo;
    private LocalDateTime criadoEm;
}
