package com.confApi.mobile;

import com.confApi.chatconfianca.controller.ChatConfiancaRemarcacaoController;
import com.confApi.chatconfianca.service.ChatConfiancaRemarcacaoService;
import com.confApi.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MobileRemarcacaoControllerTest {
    ChatConfiancaRemarcacaoService service;
    MockMvc mvc;
    @BeforeEach void setup() {
        service=mock(ChatConfiancaRemarcacaoService.class);
        mvc=MockMvcBuilders.standaloneSetup(new ChatConfiancaRemarcacaoController(service,"api.confplus"))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    List<RequestBuilder> flow(Authentication principal, int user) {
        String base="/v1/chat-confianca/remarcacoes";
        List<RequestBuilder> requests=new ArrayList<>();
        requests.add(get(base+"/reservas-emitidas").param("conversaId","20").param("codgUsuario",String.valueOf(user)).principal(principal));
        requests.add(get(base+"/70").param("codgUsuario",String.valueOf(user)).principal(principal));
        for(String path:List.of("iniciar","70/trecho","70/passageiros","70/pesquisar","70/simular","70/forma-pagamento","70/encaminhar","70/voltar"))
            requests.add(post(base+"/"+path).principal(principal).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"codgUsuario\":"+user+",\"conversaId\":20,\"reservaId\":501,\"localizador\":\"ABC123\"}"));
        return requests;
    }
    @Test void verifiedMobileIdentityCanReachEachExistingSimulationStepForItself() throws Exception {
        for(RequestBuilder request:flow(new MobileChatAuthentication("fixture",42),42))mvc.perform(request).andExpect(status().isOk());
        assertEquals(10,mockingDetails(service).getInvocations().size());
    }
    @Test void anotherUserIsRejectedBeforeAnyBusinessOperation() throws Exception {
        for(RequestBuilder request:flow(new MobileChatAuthentication("fixture",42),99))mvc.perform(request).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void mobileCannotUseThePortalLoginNameToAccessAnotherUser() throws Exception {
        for(RequestBuilder request:flow(new MobileChatAuthentication("api.confplus",42),99))mvc.perform(request).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void ordinaryTechnicalIdentityCannotClaimMobileAccessWithAuthorities() throws Exception {
        var forged=new UsernamePasswordAuthenticationToken("technical",null,AuthorityUtils.createAuthorityList("ROLE_USER","ROLE_MOBILE"));
        for(RequestBuilder request:flow(forged,42))mvc.perform(request).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void gatewayAllowsListingAndBackButStillEnforcesTheSessionUser() {
        var session=new MobileSessions.Session("fixture","device",MobileUpstream.JSON.createObjectNode().put("id",42),true);
        assertDoesNotThrow(()->MobileRoutes.require("confapi","GET","v1/chat-confianca/remarcacoes/reservas-emitidas","conversaId=20&codgUsuario=42"));
        assertDoesNotThrow(()->MobileRoutes.require("confapi","POST","v1/chat-confianca/remarcacoes/70/voltar",null));
        assertThrows(MobileFailure.class,()->MobileRoutes.ownPath("confapi","v1/chat-confianca/remarcacoes/reservas-emitidas","codgUsuario=99",session));
    }
}
