package com.confApi.mobile;

import java.util.Objects;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

/** Criada apenas pelo filtro, depois de consumir a delegação da sessão mobile validada. */
public final class MobileChatAuthentication extends UsernamePasswordAuthenticationToken {
    private final Integer userId;
    MobileChatAuthentication(String login, Integer userId) {
        super(login, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
        this.userId = userId;
    }
    public boolean owns(Integer requestedUserId) {
        return isAuthenticated() && userId != null && userId > 0 && Objects.equals(userId, requestedUserId);
    }
}
