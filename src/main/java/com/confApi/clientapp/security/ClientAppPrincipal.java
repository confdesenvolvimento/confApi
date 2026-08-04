package com.confApi.clientapp.security;

import java.io.Serializable;
import java.util.List;

public record ClientAppPrincipal(
        String customerPublicId,
        String sessionPublicId,
        String devicePublicId,
        String agencyLinkPublicId,
        Integer activeAgencyId,
        Long sessionVersion,
        Long contextVersion,
        List<String> scopes
) implements Serializable {
    public ClientAppPrincipal {
        scopes = List.copyOf(scopes);
    }
}
