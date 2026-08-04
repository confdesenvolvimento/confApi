package com.confApi.clientapp.api;

import com.confApi.clientapp.security.ClientAppPrincipal;

import java.util.List;

public record ClientAppSessionResponse(
        String sessionId,
        String customerId,
        String activeAgencyId,
        String contextVersion,
        List<String> scopes
) {
    public static ClientAppSessionResponse from(ClientAppPrincipal principal) {
        return new ClientAppSessionResponse(
                principal.sessionPublicId(),
                principal.customerPublicId(),
                String.valueOf(principal.activeAgencyId()),
                String.valueOf(principal.contextVersion()),
                principal.scopes()
        );
    }
}
