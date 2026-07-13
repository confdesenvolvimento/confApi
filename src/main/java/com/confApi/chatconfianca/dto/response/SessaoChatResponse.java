package com.confApi.chatconfianca.dto.response;

import com.confApi.chatconfianca.dto.model.RefAgencia;
import com.confApi.chatconfianca.dto.model.RefUnidade;
import com.confApi.chatconfianca.dto.model.RefUsuario;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SessaoChatResponse {
    private RefUsuario usuario;
    private RefAgencia agencia;
    private RefUnidade unidade;
    private List<String> perfis = new ArrayList<>();
    private boolean atendente;
    private boolean gestor;
    private boolean admin;
}