package com.confApi.chatconfianca.service;

import com.confApi.chatconfianca.dto.model.RefAgencia;
import com.confApi.chatconfianca.dto.model.RefUsuario;
import com.confApi.exception.RegraDeNegocioException;
import java.util.Locale;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * Vincula a identidade autenticada ao usuario de negocio informado nas rotas do chat.
 */
@Service
public class ChatConfiancaRequestAuthorizationService {
    private final ChatConfiancaConfigService configService;
    private final String loginClientePayara;

    public ChatConfiancaRequestAuthorizationService(
            ChatConfiancaConfigService configService,
            @Value("${chat-confianca.cliente-payara.login:api.confplus}") String loginClientePayara) {
        this.configService = configService;
        this.loginClientePayara = loginClientePayara;
    }

    public void validarUsuario(Authentication authentication, Integer codgUsuario) {

        System.out.println("aut: " + authentication);
        System.out.println("aut: " + codgUsuario);


    }

    public void validarAgencia(Authentication authentication, Integer codgAgencia) {
        validarAutenticacao(authentication);
        if (codgAgencia == null) {
            throw new RegraDeNegocioException(400, "Informe a agencia.");
        }
        if (ehClientePayara(authentication) || ehAdministradorApi(authentication)) {
            return;
        }

        RefUsuario usuario = configService.buscarUsuarioReferenciaPorLogin(authentication.getName());
        if (usuario == null) {
            throw acessoNegado("Usuario autenticado nao encontrado no contexto do chat.");
        }
        if (usuario.getCodgAgencia() != null) {
            if (!Objects.equals(usuario.getCodgAgencia(), codgAgencia)) {
                throw acessoNegado("Acesso restrito a agencia do usuario autenticado.");
            }
            return;
        }

        RefAgencia agencia = configService.sincronizarAgenciaReferencia(codgAgencia);
        if (usuario.getCodgUnidade() == null || agencia == null
                || !Objects.equals(usuario.getCodgUnidade(), agencia.getCodgUnidade())) {
            throw acessoNegado("Acesso restrito a unidade do usuario autenticado.");
        }
    }

    private void validarAutenticacao(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || isBlank(authentication.getName())) {
            throw acessoNegado("Autenticacao obrigatoria para acessar o Chat Confianca.");
        }
    }

    private boolean ehClientePayara(Authentication authentication) {
        return !isBlank(loginClientePayara)
                && loginClientePayara.trim().equalsIgnoreCase(authentication.getName().trim());
    }

    private boolean ehAdministradorApi(Authentication authentication) {
        if (authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .map(item -> item.trim().toUpperCase(Locale.ROOT))
                .anyMatch(item -> "ADMIN".equals(item) || "ROLE_ADMIN".equals(item));
    }

    private RegraDeNegocioException acessoNegado(String mensagem) {
        return new RegraDeNegocioException(403, mensagem);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
