package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.confApi.chatconfianca.dto.enums.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class Mensagem {
    private Long id;
    private Long conversaId;
    private Integer remetenteCodgUsuario;
    private RemetenteTipo remetenteTipo;
    private TipoMensagem tipo;
    private VisibilidadeMensagem visibilidade;
    private String conteudo;
    private String conteudoJson;
    private Long respostaMensagemId;
    private StatusMensagem status;
    private List<MensagemAnexo> anexos = new ArrayList<>();
    private LocalDateTime enviadaEm;
    private LocalDateTime editadaEm;
    private LocalDateTime excluidaEm;
}