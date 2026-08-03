package com.confApi.chatconfianca.dto.response;

import com.confApi.chatconfianca.dto.model.Conversa;
import com.confApi.chatconfianca.dto.model.DepartamentoUnidade;
import com.confApi.chatconfianca.dto.model.Mensagem;
import com.confApi.chatgpt.dto.ChatActionDTO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChatConfiancaIaResponse {
    private Conversa conversa;
    private Mensagem mensagemUsuario;
    private Mensagem mensagemBot;
    private DepartamentoUnidade departamentoSugerido;
    private String resposta;
    private boolean sugerirAtendente;
    private boolean atendenteSolicitado;
    private String mensagemAtendente;
    private String acaoSolicitada;
    private List<ChatActionDTO> actions = new ArrayList<>();
}
