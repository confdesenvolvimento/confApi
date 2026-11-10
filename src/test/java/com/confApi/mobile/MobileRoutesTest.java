package com.confApi.mobile;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class MobileRoutesTest {
    @Test void passengerSearchPreservesUserBeforeTheAgencySegment() {
        assertDoesNotThrow(() -> MobileRoutes.require("manager", "GET", "passageiroBase/search/Teste/42/99", null));
        assertDoesNotThrow(() -> MobileRoutes.ownPath("manager", "passageiroBase/search/Teste/42/99", null, session));
        assertThrows(MobileFailure.class, () -> MobileRoutes.ownPath("manager", "passageiroBase/search/Teste/43/42", null, session));
        assertThrows(MobileFailure.class, () -> MobileRoutes.require("manager", "GET", "passageiroBase/search/Teste/42", null));
    }
    @Test void ticketCancellationUsesTheManagerPutContract() {
        assertDoesNotThrow(() -> MobileRoutes.require("manager", "PUT", "bilheteAereo/cancelar/9990000000001", null));
        assertThrows(MobileFailure.class, () -> MobileRoutes.require("manager", "POST", "bilheteAereo/cancelar/9990000000001", null));
    }
    @Test void migratedCancellationHasAnExplicitMobileRoute() {
        assertDoesNotThrow(() -> MobileRoutes.require("confapi", "POST", "v2/aereo/cancelarReservaRequest", null));
        assertThrows(MobileFailure.class, () -> MobileRoutes.require("confapi", "GET", "v2/aereo/cancelarReservaRequest", null));
        assertThrows(MobileFailure.class, () -> MobileRoutes.require("confapi", "POST", "v2/aereo/cancelarReservaRequest/admin", null));
    }
    MobileSessions.Session session=new MobileSessions.Session("fixture","device",MobileUpstream.JSON.valueToTree(Map.of("id",42,"codigo",420,"tokenAcesso","own-key")),true);
    @ParameterizedTest @CsvSource({"manager,GET,dispositivo","manager,POST,usuario","manager,PUT,unidade/1","hub,POST,user/auth","hub,POST,menssage","confapi,POST,v1/chat-confianca/gestao/usuarios","manager,GET,propertiesapp","manager,POST,reservaAereo/deleteAll","confapi,GET,v1/aereo/admin"})
    void nonMobileAndAdministrativeRoutesAreClosed(String service,String method,String path){assertThrows(MobileFailure.class,()->MobileRoutes.require(service,method,path,null));}
    @ParameterizedTest @CsvSource({"manager,GET,notificacao/login/fixture/lido/0","manager,GET,alertas/usuariostatus/42","manager,GET,aeroporto/iata/CGB","manager,GET,hotel/buscarByNome","hub,POST,api/plantao/senha","confapi,POST,v1/ia/assistant/field","confapi,GET,v1/chat-confianca/sessao/42","confapi,GET,v1/chat-confianca/remarcacoes/reservas-emitidas","confapi,POST,v2/aereo/emitir/false","cache,POST,CacheAereo/aereo/melhores-ofertas","cservice,POST,api/token/mobile/gerartoken/own-key/1"})
    void existingMobileRoutesAreAllowed(String service,String method,String path){assertDoesNotThrow(()->MobileRoutes.require(service,method,path,null));}
    @Test void traversalAndEncodedSeparatorsAreRejected(){for(String p:new String[]{"../user/auth","reservaAereo/%2e%2e/user","reservaAereo/%2fuser","reservaAereo/%252e%252e","reservaAereo/1;admin","reservaAereo/1\\admin"})assertThrows(MobileFailure.class,()->MobileRoutes.require("manager","GET",p,null));}
    @Test void otherUserCannotBeSelected(){for(String p:new String[]{"v1/chat-confianca/sessao/43","v1/chat-confianca/notificacoes/resumo/43"})assertThrows(MobileFailure.class,()->MobileRoutes.ownPath("confapi",p,null,session));assertThrows(MobileFailure.class,()->MobileRoutes.ownPath("manager","notificacao/login/other/lido/0",null,session));assertDoesNotThrow(()->MobileRoutes.ownPath("manager","notificacao/login/fixture/lido/0",null,session));}
    @Test void queryAndBodyCannotEscalateIdentityOrPrivileges(){for(String q:new String[]{"codgUsuario=43","username=other","gestor=true","podeVerInternas=true"})assertThrows(MobileFailure.class,()->MobileRoutes.ownPath("confapi","v1/chat-confianca/conversas/1",q,session));for(var body:new Map[]{Map.of("codgUsuario",43),Map.of("usuarioLoginAcesso","other"),Map.of("gestor",true),Map.of("interna",true)})assertThrows(MobileFailure.class,()->MobileRoutes.identity(MobileUpstream.JSON.valueToTree(body),session));}
    @Test void cserviceOnlyUsesSessionOwnerKey(){assertThrows(MobileFailure.class,()->MobileRoutes.ownPath("cservice","api/token/gerartoken/stolen-key/1",null,session));assertDoesNotThrow(()->MobileRoutes.ownPath("cservice","api/token/gerartoken/own-key/1",null,session));}
    @Test void existingHotelOffersRemainAvailable(){
        for(String path:new String[]{"cacheHotel/oportunidades","cacheHotel/oportunidades/pacotes"})
            assertDoesNotThrow(()->MobileRoutes.require("cache","GET",path,"limite=4"));
        assertThrows(MobileFailure.class,()->MobileRoutes.require("cache","POST","cacheHotel/oportunidades",null));
    }
    @Test void reservationCreatorDoesNotReplaceTheAuthenticatedOperator(){
        var body=MobileUpstream.JSON.valueToTree(Map.of("codgReservaAereoDB",10,"usuarioCriacao2",Map.of("codgUsuario",43,"loginUsuario","creator")));
        assertDoesNotThrow(()->MobileRoutes.identity("confapi","POST","v2/aereo/emitir/false",body,session));
        ((com.fasterxml.jackson.databind.node.ObjectNode)body).put("usuarioId",43);
        assertThrows(MobileFailure.class,()->MobileRoutes.identity("confapi","POST","v2/aereo/emitir/false",body,session));
        var update=MobileUpstream.JSON.valueToTree(Map.of("codgUsuarioCriacao",Map.of("codgUsuario",43)));
        assertDoesNotThrow(()->MobileRoutes.identity("manager","PUT","reservaAereo/10",update,session));
        assertThrows(MobileFailure.class,()->MobileRoutes.identity("manager","POST","reservaAereo/saveapp",update,session));
    }
}
