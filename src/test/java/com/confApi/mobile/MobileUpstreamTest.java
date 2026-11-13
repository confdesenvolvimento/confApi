package com.confApi.mobile;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class MobileUpstreamTest {
    @Test void publicConfigurationPreservesVersionAndExistingFeatureFlags() {
        List<Map<String,Object>> rows=new ArrayList<>();
        rows.add(Map.of("nome","versao","status",335,"descricao","versão mínima"));
        for(String name:List.of("chatia","emitir","hotel","seguro","novidade","ofertas","codigovalidacaoemail","codigovalidacaosms"))rows.add(Map.of("nome",name,"status",0,"descricao","descrição interna"));
        JsonNode result=new Fake(true,MobileUpstream.JSON.valueToTree(rows)).publicProperties();
        assertEquals(9,result.size());assertEquals("versao",result.get(0).path("nome").asText());assertEquals(335,result.get(0).path("status").asInt());
        for(int i=1;i<result.size();i++)assertEquals(0,result.get(i).path("status").asInt());
        assertFalse(result.toString().contains("descrição interna"));
    }
    @Test void onlyPublicAppMetadataIsForwarded() {
        var headers=java.net.http.HttpHeaders.of(Map.of("contagem",List.of("12"),"Authorization",List.of("private-token"),"Set-Cookie",List.of("private-cookie")),(name,value)->true);
        assertEquals(Map.of("Contagem","12"),MobileUpstream.publicResponseHeaders(headers));
        assertTrue(MobileUpstream.publicResponseHeaders(java.net.http.HttpHeaders.of(Map.of("Contagem",List.of("invalid")),(name,value)->true)).isEmpty());
    }
    @Test void audioAndDownloadMetadataReachTheAppWithoutAuthHeaders(){
        var result=MobileUpstream.publicResponseHeaders(java.net.http.HttpHeaders.of(Map.of(
            "X-Transcript",List.of("Pesquisar voo para Cuiaba"),"X-Client-Message-Id",List.of("message-123"),
            "Content-Length",List.of("123"),"Authorization",List.of("private-token"),"Set-Cookie",List.of("private-cookie")),(n,v)->true));
        assertEquals(Map.of("X-Transcript","Pesquisar voo para Cuiaba","X-Client-Message-Id","message-123","Content-Length","123"),result);
    }
    @Test void businessErrorMessagesArePreservedWithoutTechnicalSecrets(){
        var f=new Fake(true,MobileUpstream.JSON.createArrayNode());
        var result=f.publicError(409,"application/json",("{\"message\":\"Tarifa indisponível\",\"senha\":\"private-password\",\"stackTrace\":\"internal\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("Tarifa indisponível",result.path("message").asText());assertFalse(result.has("senha"));assertFalse(result.has("stackTrace"));
        for(String secret:List.of("private-password","private-user","Bearer sensitive-token","eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4In0.signature")){
            var hidden=f.publicError(400,"application/json",MobileUpstream.JSON.valueToTree(Map.of("message",secret)).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            assertFalse(hidden.has("message"));
        }
        assertFalse(f.publicError(502,"text/plain","internal error".getBytes()).has("message"));
    }
    @Test void loginEmailCopyHonorsTheExistingDevicePreferenceAndNeverActivatesIt(){
        var f=org.mockito.Mockito.spy(new Fake(true,MobileUpstream.JSON.createArrayNode()));
        var p=MobileUpstream.JSON.valueToTree(Map.of("login","fixture","email","fixture@example.test"));
        org.mockito.Mockito.doReturn(MobileUpstream.JSON.valueToTree(Map.of("enviarEmail",1))).when(f).call("manager","dispositivo/login/fixture","GET",null);
        org.mockito.Mockito.doNothing().when(f).deliver(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString());
        f.deliverLoginCode(p,"123456");
        org.mockito.Mockito.verify(f).deliver(p,"sms","123456");org.mockito.Mockito.verify(f).deliver(p,"email","123456");
        org.mockito.Mockito.verify(f).call("manager","dispositivo/login/fixture","GET",null);
        org.mockito.Mockito.clearInvocations(f);
        org.mockito.Mockito.doReturn(MobileUpstream.JSON.valueToTree(Map.of("enviarEmail",0))).when(f).call("manager","dispositivo/login/fixture","GET",null);
        f.deliverLoginCode(p,"654321");org.mockito.Mockito.verify(f,org.mockito.Mockito.never()).deliver(p,"email","654321");
    }
    static class Fake extends MobileUpstream {
        final List<String> calls=new ArrayList<>();JsonNode rows;
        Fake(boolean dev,JsonNode rows){super(new MobileSettings(JSON.valueToTree(Map.of("development",dev,"services",Map.of("manager","https://manager.test/","hub","https://hub.test/"),"allowedProductionHosts",List.of("manager.test"),"technicalLogin","private-user","technicalPassword","private-password"))));this.rows=rows;}
        @Override protected JsonNode request(String url,String method,JsonNode body,String auth){calls.add(url);if(url.endsWith("user/auth")){assertEquals("private-user",body.path("login").asText());return JSON.valueToTree(Map.of("token","private-token"));}assertEquals("private-token",auth);return rows;}
    }
    @Test void productionDestinationsComeFromDatabaseWithAliases(){var f=new Fake(false,MobileUpstream.JSON.valueToTree(List.of(Map.of("nome","appBaseUrl","status",1,"descricao","https://manager.test/prod/"))));assertEquals("https://manager.test/prod/",f.destination("manager"));assertEquals(List.of("https://hub.test/user/auth","https://manager.test/propertiesapp"),f.calls);}
    @Test void developmentNeverFallsBackToProduction(){var f=new Fake(true,MobileUpstream.JSON.createArrayNode());assertEquals("https://manager.test/",f.destination("manager"));assertTrue(f.calls.isEmpty());assertThrows(MobileFailure.class,()->f.destination("cache"));}
    @Test void databaseCannotRedirectTechnicalCredential(){var f=new Fake(false,MobileUpstream.JSON.valueToTree(List.of(Map.of("nome","baseUrl","status",1,"descricao","https://untrusted.test/"))));assertThrows(MobileFailure.class,()->f.destination("manager"));}
    @Test void productionEmailUsesDatabaseDestinationAndServerProfile(){var f=new Fake(false,MobileUpstream.JSON.valueToTree(List.of(Map.of("nome","appUrlEmail","status",1,"descricao","https://manager.test/mail"))));f.deliver(MobileUpstream.JSON.valueToTree(Map.of("email","fixture@example.test")),"email","123456");assertTrue(f.calls.contains("https://manager.test/mail"));}
    @Test void publicConfigurationNeverIncludesInternalUrlsOrCredentialProperties(){var f=new Fake(true,MobileUpstream.JSON.valueToTree(List.of(Map.of("nome","baseUrl","status",1,"descricao","https://manager.test/"),Map.of("nome","technicalPassword","status",1,"descricao","private-password"),Map.of("nome","smsHabilitado","status",1,"descricao","private-password"),Map.of("nome","orcamentoCarro","status",1,"descricao","true"))));String response=f.publicProperties().toString();assertFalse(response.contains("private-"));assertFalse(response.contains("manager.test"));assertTrue(response.contains("smsHabilitado"));assertTrue(response.contains("true"));}
    @Test void secretsAreRecursivelyRemovedFromProfiles(){JsonNode result=MobileUpstream.sanitized(MobileUpstream.JSON.valueToTree(Map.of("senha","secret","chaveToken","token","nested",List.of(Map.of("usuarioSenha","secret","nome","kept")))));assertFalse(result.toString().contains("secret"));assertFalse(result.has("chaveToken"));assertTrue(result.toString().contains("kept"));}
    @Test void productionRequiresHttpsAndDevelopmentAllowsOnlyPrivateHttp(){var prod=new MobileSettings(MobileUpstream.JSON.createObjectNode());var dev=new MobileSettings(MobileUpstream.JSON.valueToTree(Map.of("development",true)));assertThrows(MobileFailure.class,()->prod.validate("http://192.168.1.2:8088/"));assertEquals("http://192.168.1.2:8088/",dev.validate("http://192.168.1.2:8088"));for(String url:List.of("http://public.test/","https://user:password@trusted.test/","https://trusted.test/?redirect=x","file:///private"))assertThrows(MobileFailure.class,()->dev.validate(url));}
}
