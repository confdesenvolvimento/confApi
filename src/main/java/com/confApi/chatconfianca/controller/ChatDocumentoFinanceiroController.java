package com.confApi.chatconfianca.controller;

import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.chatconfianca.financeiro.ChatDocumentoFinanceiroService;
import com.confApi.chatconfianca.financeiro.ChatDocumentoFinanceiroService.Documento;
import com.confApi.chatconfianca.service.ChatConfiancaRequestAuthorizationService;
import com.confApi.chatconfianca.service.ChatConfiancaService;
import com.confApi.chatconfianca.service.ChatContextualAcessoService;
import com.confApi.exception.RegraDeNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Mesma rota autenticada para HUB e mobile. Nao publica arquivos em armazenamento publico. */
@RestController
@RequestMapping("/v1/chat-confianca")
public class ChatDocumentoFinanceiroController {
    private final ChatConfiancaRequestAuthorizationService authorization;
    private final ChatConfiancaService chat;
    private final ChatContextualAcessoService acesso;
    private final ChatDocumentoFinanceiroService documentos;
    @Value("${chat-confianca.ajuda-contextual.enabled:true}")
    private boolean enabled = true;

    public ChatDocumentoFinanceiroController(ChatConfiancaRequestAuthorizationService authorization,
            ChatConfiancaService chat, ChatContextualAcessoService acesso, ChatDocumentoFinanceiroService documentos) {
        this.authorization = authorization;
        this.chat = chat;
        this.acesso = acesso;
        this.documentos = documentos;
    }

    @PostMapping("/conversas/{conversaId}/faturas/{numero}/documentos/{tipo}/download")
    public ResponseEntity<byte[]> baixar(@PathVariable("conversaId") Long conversaId, @PathVariable("numero") int numero,
            @PathVariable("tipo") String tipo, @RequestParam("codgUsuario") Integer codgUsuario,
            @RequestParam(value = "codgAgenciaSessao", required = false) Integer codgAgenciaSessao, Authentication authentication) {
        if (!enabled) throw new RegraDeNegocioException(503, "A ajuda contextual esta temporariamente indisponivel.");
        authorization.validarUsuario(authentication, codgUsuario);
        SessaoChatResponse sessao = chat.montarSessao(codgUsuario, codgAgenciaSessao);
        acesso.validarSessao(sessao, codgUsuario, codgAgenciaSessao);
        authorization.validarAgencia(authentication, sessao.getAgencia().getCodgAgencia());
        chat.buscarConversaNaSessao(conversaId, codgUsuario, sessao);
        Documento arquivo = documentos.baixar(sessao, numero, tipo);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(arquivo.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + arquivo.nomeArquivo() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store, private, max-age=0")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header("X-Content-Type-Options", "nosniff")
                .body(arquivo.conteudo());
    }
}
