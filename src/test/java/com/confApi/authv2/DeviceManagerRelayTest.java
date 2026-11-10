package com.confApi.authv2;
import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DeviceManagerRelayTest {
    static class Relay extends DeviceManagerRelay {
        String path,method,authorization,key;JsonNode body;int calls;
        @Override protected Object forward(String path,String method,JsonNode body,String authorization,String key){this.path=path;this.method=method;this.body=body;this.authorization=authorization;this.key=key;calls++;return Map.of();}
    }
    MockHttpServletRequest request(String scope,String use){var r=new MockHttpServletRequest();r.setAttribute("auth.v2.actor",new V2.Actor("cservice3",Set.of(scope),new JWTClaimsSet.Builder().claim("token_use",use).build()));r.addHeader("Authorization","Bearer original-cservice-identity");r.addHeader("Idempotency-Key","synthetic-idempotency");return r;}
    @Test void activationPreservesCallerScopeBodyAndIdempotency(){var relay=new Relay();var body=V2.JSON.valueToTree(Map.of("thumbprint","synthetic","expectedVersion",4));relay.activate("request-1",body,request("device:activate","SERVICE"));assertEquals("/internal/v2/dispositivos/solicitacoes/request-1/ativar",relay.path);assertEquals("POST",relay.method);assertSame(body,relay.body);assertEquals("Bearer original-cservice-identity",relay.authorization);assertEquals("synthetic-idempotency",relay.key);}
    @Test void invalidScopeOperatorAndPathCannotReachManager(){var relay=new Relay();assertThrows(V2.Failure.class,()->relay.activate("request-1",V2.JSON.createObjectNode(),request("device:read","SERVICE")));assertThrows(V2.Failure.class,()->relay.activate("request-1",V2.JSON.createObjectNode(),request("device:activate","OPERATOR")));assertThrows(V2.Failure.class,()->relay.status("../other",request("device:read","SERVICE")));assertEquals(0,relay.calls);}
    @Test void profileAndContactHaveSeparateScopes(){var relay=new Relay();relay.profile("7",request("profile:read","SERVICE"));assertEquals("/internal/v2/usuarios/7/perfil-mobile",relay.path);assertThrows(V2.Failure.class,()->relay.contact("request-1",request("profile:read","SERVICE")));}
}
