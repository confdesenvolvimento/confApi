package com.confApi.chatconfianca.v2;

import lombok.Data;
import java.util.LinkedHashMap;
import java.util.Map;

/** No agency/user IDs or arbitrary action names can be supplied by the model. */
@Data
public class ChatV2Plan {
    private String intencao;
    private boolean continuar;
    private String pergunta;
    private String assuntoHandoff;
    private Map<String,String> parametros = new LinkedHashMap<>();
    private String fonte = "V2_REGRAS";
    private String resultado = "AGUARDANDO_EXECUCAO";
    private String erro;
    private boolean legado;
    private long atualizadoEm;
    private Integer agencia;
    private Integer usuario;
    public ChatV2Capability capability() { return ChatV2Capability.from(intencao); }
    public static ChatV2Plan of(ChatV2Capability c) {
        ChatV2Plan p=new ChatV2Plan(); p.setIntencao(c.code); return p;
    }
}
