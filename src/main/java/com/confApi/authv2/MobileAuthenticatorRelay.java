package com.confApi.authv2;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/** Closed mobile facade. It never forwards a caller-supplied service identity or target URL. */
@RestController
@RequestMapping("/api/v2/autenticador-mobile")
public class MobileAuthenticatorRelay {
    @org.springframework.beans.factory.annotation.Autowired(required=false) private LocalDevelopment localDevelopment=LocalDevelopment.disabled();
    static final String ROOT="/api/v2/autenticador-mobile";
    private static final int MAX_BODY=16384;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();
    static boolean allowed(String method,String path) {
        if(!path.startsWith(ROOT+"/")) return false;
        String route=path.substring(ROOT.length());
        if("POST".equals(method)) return Set.of("/sessoes","/sessoes/confirmar","/sessoes/renovar","/sessoes/encerrar","/cadastros").contains(route)
                || route.matches("/cadastros/[A-Za-z0-9_-]{1,128}/(?:confirmacao/(?:enviar|validar)|ativar)")
                || route.matches("/desafios-portal/[A-Za-z0-9_-]{1,128}/codigo");
        if("GET".equals(method)) return Set.of("/perfil","/desafios-portal").contains(route)||route.matches("/cadastros/[A-Za-z0-9_-]{1,128}");
        return "PUT".equals(method)&&"/dispositivo/push".equals(route);
    }
    @RequestMapping(value={"","/**"})
    public ResponseEntity<?> relay(HttpServletRequest request) {
        String path=request.getRequestURI().substring(request.getContextPath().length());
        if(!allowed(request.getMethod(),path)) throw new V2.Failure(404);
        if(!V2.enabled()) throw new V2.Failure(503);
        if((!request.isSecure()&&!localDevelopment.allows(request))||request.getQueryString()!=null) throw new V2.Failure(400);
        try {
            byte[] body=request.getInputStream().readNBytes(MAX_BODY+1);
            if(body.length>MAX_BODY) return error(413,"CORPO_EXCEDIDO",Objects.toString(request.getAttribute("auth.v2.correlation"),UUID.randomUUID().toString()));
            if(body.length>0) { JsonNode data=V2.JSON.readTree(body); if(data==null||!data.isObject())throw new V2.Failure(400); }
            Map<String,String> headers=new LinkedHashMap<>();
            if(!("POST".equals(request.getMethod())&&(ROOT+"/sessoes").equals(path))) {
                String auth=request.getHeader("Authorization"),proof=request.getHeader("DPoP");
                if(auth==null||!auth.matches("DPoP [A-Za-z0-9_-]{43}")||proof==null||proof.isBlank()||proof.length()>8192)throw new V2.Failure(401);
                headers.put("Authorization",auth);headers.put("DPoP",proof);
            }
            if("POST".equals(request.getMethod())&&(ROOT+"/cadastros").equals(path)) {
                String key=request.getHeader("Idempotency-Key");
                if(key==null||!key.matches("[A-Za-z0-9_-]{16,128}"))throw new V2.Failure(400);
                headers.put("Idempotency-Key",key);
            }
            // No Host, Forwarded, X-Auth-Gateway or arbitrary caller headers are copied.
            headers.put("X-Auth-Gateway",gatewayToken());
            headers.put("X-Auth-Client-Key",V2.hash(request.getRemoteAddr()));
            headers.put("X-Auth-Client-IP",request.getRemoteAddr());
            return forward(path,request.getMethod(),body,headers);
        } catch(V2.Failure e) { throw e; }
        catch(com.fasterxml.jackson.core.JsonProcessingException e) { throw new V2.Failure(400); }
        catch(IOException e) { throw new V2.Failure(503); }
    }
    protected String gatewayToken() { return V2.token("AUTH_V2_MANAGER_TOKEN_FILE"); }
    protected ResponseEntity<?> forward(String path,String method,byte[] body,Map<String,String> headers) {
        try {
            String base=V2.setting("AUTH_V2_MANAGER_URL");URI uri=URI.create(base);
            if((!"https".equals(uri.getScheme())&&!localDevelopment.allows(uri))||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||base.endsWith("/"))throw new V2.Failure(503);
            var builder=HttpRequest.newBuilder(URI.create(base+path)).timeout(Duration.ofSeconds(12)).header("Accept","application/json").header("Content-Type","application/json");
            headers.forEach(builder::header);
            builder.method(method,body.length==0?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofByteArray(body));
            var response=http.send(builder.build(),HttpResponse.BodyHandlers.ofInputStream());
            byte[] data;try(var input=response.body()){data=input.readNBytes(65537);}
            if(data.length>65536)throw new V2.Failure(503);
            int status=response.statusCode();
            if(status==204)return ResponseEntity.noContent().header("Cache-Control","no-store").build();
            if(status<200||status>=300){
                if(!Set.of(400,401,403,404,409,413,429,503).contains(status))status=503;
                String code="AUTH_V2_"+status,correlation=UUID.randomUUID().toString();
                try{var failure=V2.JSON.readTree(data);String candidate=failure.path("code").asText(),id=failure.path("correlationId").asText();
                    if(candidate.matches("[A-Z0-9_]{1,80}"))code=candidate;if(id.matches("[a-fA-F0-9-]{36}"))correlation=id;
                }catch(Exception ignored){}
                return error(status,code,correlation);
            }
            JsonNode result=V2.JSON.readTree(data);if(result==null)throw new V2.Failure(503);
            return ResponseEntity.status(status).header("Cache-Control","no-store").body(result);
        }catch(V2.Failure e){throw e;}catch(InterruptedException e){Thread.currentThread().interrupt();throw new V2.Failure(503);}catch(Exception e){throw new V2.Failure(503);}
    }
    private static ResponseEntity<?> error(int status,String code,String correlation){return ResponseEntity.status(status).header("Cache-Control","no-store").header("X-Correlation-ID",correlation).body(Map.of("code",code,"correlationId",correlation));}
}
