package com.confApi.chatconfianca.financeiro;

import com.confApi.chatconfianca.client.ChatConfiancaManagerClient;
import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.exception.RegraDeNegocioException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** Permissao funcional consultada no servidor em cada leitura/download, sem cache compartilhado. */
@Service
public class ChatPermissaoFinanceiraService {
    private final ChatConfiancaManagerClient manager;
    public ChatPermissaoFinanceiraService(ChatConfiancaManagerClient manager) { this.manager = manager; }

    public void validar(SessaoChatResponse sessao) {
        if (sessao == null || sessao.getUsuario() == null || sessao.getUsuario().getCodgUsuario() == null
                || sessao.getUsuario().getCodgUsuario() <= 0) throw negado();
        Integer usuario = sessao.getUsuario().getCodgUsuario();
        final Permissao permissao;
        try {
            permissao = manager.get("chat-confianca/consultas/permissoes-financeiras/usuario/" + usuario, Permissao.class);
        } catch (RuntimeException ex) { throw negado(); }
        if (permissao == null || !"chat.permissao-financeira.v1".equals(permissao.schema())
                || !Objects.equals(usuario, permissao.codgUsuario()) || !Boolean.TRUE.equals(permissao.ativo())
                || !Boolean.TRUE.equals(permissao.podeConsultarFaturas())
                || !Objects.equals(sessao.getUsuario().getCodgAgencia(), permissao.codgAgencia())
                || !Objects.equals(sessao.getUsuario().getCodgUnidade(), permissao.codgUnidade())) throw negado();
    }

    private static RegraDeNegocioException negado() {
        return new RegraDeNegocioException(403, "Nao foi possivel confirmar sua permissao para consultar faturas. Procure o administrador da agencia.");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Permissao(String schema, Integer codgUsuario, Integer codgAgencia, Integer codgUnidade,
            Boolean ativo, Boolean podeConsultarFaturas) { }
}
