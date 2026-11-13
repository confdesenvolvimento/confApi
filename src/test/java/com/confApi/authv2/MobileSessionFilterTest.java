package com.confApi.authv2;
import org.junit.jupiter.api.*;import org.springframework.mock.web.*;import org.springframework.security.core.context.SecurityContextHolder;import java.util.Map;import static org.junit.jupiter.api.Assertions.*;
class MobileSessionFilterTest {
 @AfterEach void cleanup(){for(String key:new String[]{"AUTH_V2_ENABLED","AUTH_V2_RESOURCE_ORIGIN","AUTH_V2_MOBILE_ROUTES_JSON"})System.clearProperty(key);SecurityContextHolder.clearContext();}
 @Test void mobileRoutesAreClosedUntilExplicitlyReviewed(){assertThrows(V2.Failure.class,()->MobileSessionFilter.requireRoute("GET","/api/reservas"));System.setProperty("AUTH_V2_MOBILE_ROUTES_JSON","[{\"method\":\"GET\",\"path\":\"/api/reservas/*\"}]");assertDoesNotThrow(()->MobileSessionFilter.requireRoute("GET","/api/reservas/123"));assertThrows(V2.Failure.class,()->MobileSessionFilter.requireRoute("DELETE","/api/reservas/123"));assertThrows(V2.Failure.class,()->MobileSessionFilter.requireRoute("GET","/api/usuarios/123"));}
 @Test void onlyServerIntrospectionProvidesIdentity()throws Exception{
  System.setProperty("AUTH_V2_ENABLED","true");System.setProperty("AUTH_V2_RESOURCE_ORIGIN","https://gateway.example.test");System.setProperty("AUTH_V2_MOBILE_ROUTES_JSON","[{\"method\":\"GET\",\"path\":\"/api/recurso\"}]");
  var filter=new MobileSessionFilter(){@Override protected com.fasterxml.jackson.databind.JsonNode introspect(Map<String,String> b){assertEquals("https://gateway.example.test/app/api/recurso",b.get("uri"));return V2.JSON.valueToTree(Map.of("active",true,"mfa",true,"userId","1","companyId","company-1"));}};
  var req=new MockHttpServletRequest("GET","/app/api/recurso");req.setContextPath("/app");req.setSecure(true);req.addHeader("Authorization","DPoP "+"a".repeat(43));req.addHeader("DPoP","synthetic-proof");
  var res=new MockHttpServletResponse();filter.doFilter(req,res,(r,s)->{assertEquals("1",SecurityContextHolder.getContext().getAuthentication().getName());assertNotNull(r.getAttribute("auth.v2.identity"));});assertEquals(200,res.getStatus());
 }
 @Test void unavailableIntrospectionCannotContinue()throws Exception{
  System.setProperty("AUTH_V2_ENABLED","true");System.setProperty("AUTH_V2_RESOURCE_ORIGIN","https://gateway.example.test");System.setProperty("AUTH_V2_MOBILE_ROUTES_JSON","[{\"method\":\"GET\",\"path\":\"/api/recurso\"}]");
  var filter=new MobileSessionFilter(){@Override protected com.fasterxml.jackson.databind.JsonNode introspect(Map<String,String> b){throw new V2.Failure(503);}};
  var req=new MockHttpServletRequest("GET","/api/recurso");req.setSecure(true);req.addHeader("Authorization","DPoP "+"a".repeat(43));req.addHeader("DPoP","synthetic-proof");var res=new MockHttpServletResponse();filter.doFilter(req,res,(r,s)->fail("Não deve executar o recurso"));assertEquals(503,res.getStatus());
 }

 private MockHttpServletRequest mobileRequest(){
  System.setProperty("AUTH_V2_ENABLED","true");System.setProperty("AUTH_V2_RESOURCE_ORIGIN","https://gateway.example.test");System.setProperty("AUTH_V2_MOBILE_ROUTES_JSON","[{\"method\":\"GET\",\"path\":\"/api/recurso\"}]");
  var req=new MockHttpServletRequest("GET","/api/recurso");req.setSecure(true);req.addHeader("Authorization","DPoP "+"a".repeat(43));req.addHeader("DPoP","synthetic-proof");return req;
 }
 @Test void revokedIdentityIs401SoTheAppClosesItsSession()throws Exception{
  for(int status:new int[]{401,403}){var req=mobileRequest();var res=new MockHttpServletResponse();var filter=new MobileSessionFilter(){protected com.fasterxml.jackson.databind.JsonNode introspect(Map<String,String> b){throw new V2.Failure(status);}};
   filter.doFilter(req,res,(r,s)->fail("Revoked identity cannot execute resource"));assertEquals(401,res.getStatus());}
 }
 @Test void resourcePermissionDenialRemains403()throws Exception{
  var req=mobileRequest();var res=new MockHttpServletResponse();var filter=new MobileSessionFilter(){protected com.fasterxml.jackson.databind.JsonNode introspect(Map<String,String> b){return V2.JSON.valueToTree(Map.of("active",true,"mfa",true,"userId","1","companyId","company-1"));}};
  filter.doFilter(req,res,(r,s)->((javax.servlet.http.HttpServletResponse)s).setStatus(403));assertEquals(403,res.getStatus());
 }

}
