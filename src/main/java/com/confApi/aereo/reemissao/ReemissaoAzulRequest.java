package com.confApi.aereo.reemissao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.*;
import lombok.Data;

@Data @JsonIgnoreProperties(ignoreUnknown = true)
public class ReemissaoAzulRequest {
    private Integer codgUsuario, codgAgenciaSessao, reservaId, versao, opcao, familia;
    private Boolean aceite, adicionarServicos;
    private String politicaVersao, data;
    private List<Integer> indices;
    private List<String> passageiroKeys;
    private List<Map<String, Object>> servicos;
    private Map<String, Object> pagamento;
    @Override public String toString() { return "ReemissaoAzulRequest{dados protegidos}"; }
}
