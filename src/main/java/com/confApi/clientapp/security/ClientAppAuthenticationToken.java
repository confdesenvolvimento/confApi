package com.confApi.clientapp.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public final class ClientAppAuthenticationToken extends AbstractAuthenticationToken {

    private final ClientAppPrincipal principal;

    public ClientAppAuthenticationToken(
            ClientAppPrincipal principal,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(authorities);
        this.principal = principal;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public ClientAppPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
        if (authenticated) {
            throw new IllegalArgumentException("Use the authenticated constructor");
        }
        super.setAuthenticated(false);
    }
}
