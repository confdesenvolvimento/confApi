package com.confApi.clientapp.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MViagensIntrospectionResponse(
        Boolean active,
        String customerPublicId,
        String sessionPublicId,
        String devicePublicId,
        String agencyLinkPublicId,
        Integer activeAgencyId,
        Long sessionVersion,
        Long contextVersion,
        List<String> scopes
) {
    public MViagensIntrospectionResponse {
        scopes = scopes == null ? List.of() : List.copyOf(scopes);
    }
}
