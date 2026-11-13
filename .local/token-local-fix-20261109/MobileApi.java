package com.confApi.mobile;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.InputStreamResource;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.*;

@RestController
@RequestMapping("/api/mobile")
public class MobileApi {
    private final MobileSessions sessions;private final MobileUpstream upstream;
    private final MobileChatDelegations delegations;
    public MobileApi(MobileSessions sessions,MobileUpstream upstream,MobileChatDelegations delegations){this.sessions=sessions;this.upstream=upstream;this.delegations=delegations;}
    @GetMapping("/config") public JsonNode config(HttpServletRequest request){sessions.limit("config:"+request.getRemoteAddr(),120,60);return upstream.publicProperties();}
    @PostMapping("/identificar") public Object identify(@RequestBody JsonNode body,HttpServletRequest request){return sessions.start(body.path("login").asText(),null,body.path("deviceId").asText(),request.getRemoteAddr());}
    @PostMapping("/entrar") public Object login(@RequestBody JsonNode body,HttpServletRequest request){return sessions.start(body.path("login").asText(),body.path("password").asText(),body.path("deviceId").asText(),request.getRemoteAddr());}
    @PostMapping("/renovar") public Object renew(@RequestBody JsonNode body,HttpServletRequest request){sessions.limit("renew:"+request.getRemoteAddr(),120,60);return sessions.renew(body.path("refreshToken").asText());}
    private MobileSessions.Session session(HttpServletRequest request){return sessions.require(request.getHeader("Authorization"));}
    @PostMapping("/sair") public ResponseEntity<?> close(HttpServletRequest request){sessions.close(session(request));return ResponseEntity.noContent().build();}
    @GetMapping("/perfil") public JsonNode profile(HttpServletRequest request){return sessions.profile(session(request));}
    @PostMapping("/codigo/enviar") public Object send(@RequestBody JsonNode body,HttpServletRequest request){return sessions.sendCode(session(request),body.path("channel").asText("sms"),body.path("copyEmail").asBoolean(false));}
    @PostMapping("/codigo/validar") public Object validate(@RequestBody JsonNode body,HttpServletRequest request){return sessions.validateCode(session(request),body.path("code").asText());}

    @RequestMapping("/services/{service}/**") public ResponseEntity<?> service(@PathVariable String service,HttpServletRequest request)throws IOException {
        var session=session(request);
        String prefix=request.getContextPath()+"/api/mobile/services/"+service+"/";
        String path=request.getRequestURI().substring(prefix.length());String method=request.getMethod();
        // Contratos próprios do login atual. Nunca permitir consulta de outro usuário.
        if("manager".equals(service)&&"GET".equals(method)&&path.equals("dispositivo/login/"+session.login))return ResponseEntity.ok(sessions.device(session));
        byte[] body=request.getInputStream().readNBytes(20_000_001);
        if(body.length>20_000_000)throw new MobileFailure(413,"CORPO_EXCEDIDO");
        if("manager".equals(service)&&"POST".equals(method)&&path.equals("dispositivo/novo")) {
            JsonNode result=sessions.register(session,MobileUpstream.JSON.readTree(body));return ResponseEntity.status(result.path("autorizado").asBoolean()?200:202).body(result);
        }
        if("manager".equals(service)&&"GET".equals(method)&&path.equals("usuario/findByLogin/"+session.login)) {
            sessions.requireActive(session);
            return ResponseEntity.ok(MobileUpstream.sanitized(upstream.call("manager",path,"GET",null)));
        }
        MobileRoutes.require(service,method,path,request.getQueryString());
        sessions.requireActive(session);MobileRoutes.ownPath(service,path,request.getQueryString(),session);
        sessions.limit("operation:"+session.login,180,60);
        if("manager".equals(service)&&(path.startsWith("token/")||path.startsWith("tokenNumber/"))) {
            String key=session.profile.path("tokenAcesso").asText("");
            if(key.isBlank())throw new MobileFailure(403,"TOKEN_INDISPONIVEL");
            if(path.startsWith("token/")&&!path.equals("token/"+key))throw new MobileFailure(403,"TOKEN_DE_OUTRO_USUARIO");
            if(path.startsWith("tokenNumber/")) {
                JsonNode owner=upstream.call("manager","token/"+key,"GET",null);
                String id=path.substring(path.lastIndexOf('/')+1);
                if(!owner.path("codgToken").asText().equals(id))throw new MobileFailure(403,"TOKEN_DE_OUTRO_USUARIO");
            }
        }
        String contentType=request.getContentType();
        if(body.length>0&&(contentType==null||!contentType.toLowerCase(Locale.ROOT).startsWith("application/json"))
            &&!("confapi".equals(service)&&Set.of("v1/ai/stt","v1/ai/sttrequest").contains(path)&&contentType!=null&&contentType.startsWith("multipart/form-data")))throw new MobileFailure(415,"FORMATO_INVALIDO");
        if(body.length>0&&contentType!=null&&contentType.toLowerCase(Locale.ROOT).startsWith("application/json")) {
            JsonNode input=MobileUpstream.JSON.readTree(body);MobileRoutes.identity(service,method,path,input,session);
            if("hub".equals(service)&&path.equals("api/plantao/senha")&&!session.login.equalsIgnoreCase(input.path("login").asText()))throw new MobileFailure(403,"USUARIO_DIVERGENTE");
            if("cservice".equals(service)&&path.contains("gerartoken/")) {
                var fixed=MobileUpstream.JSON.createObjectNode();fixed.put("usuarioLogin",upstream.settings.secret("cserviceLogin"));fixed.put("usuarioSenha",upstream.settings.secret("cservicePassword"));fixed.put("usuarioLoginAcesso",session.login);body=MobileUpstream.JSON.writeValueAsBytes(fixed);
            }
        }
        MobileUpstream.Response response;
        if ("confapi".equals(service) && path.startsWith("v1/chat-confianca/")) {
            String grant=delegations.issue(session,method,path,request.getQueryString(),body);
            try { response=upstream.forwardWithIdentity(path,request.getQueryString(),method,body,contentType,grant); }
            finally { delegations.discard(grant); }
        } else response=upstream.forward(service,path,request.getQueryString(),method,body,contentType);
        if(response.status()<200||response.status()>=300) {
            int status=response.status();if(!Set.of(400,401,403,404,405,409,422,429,503).contains(status))status=502;
            byte[] error;try(var stream=response.body()){error=stream.readNBytes(65_537);}
            return ResponseEntity.status(status).body(upstream.publicError(status,response.contentType(),error));
        }
        var outgoing=ResponseEntity.status(response.status());
        response.headers().forEach((name,value)->{if(!name.equalsIgnoreCase("Content-Length")||!response.contentType().toLowerCase(Locale.ROOT).contains("json"))outgoing.header(name,value);});
        if(response.contentType().toLowerCase(Locale.ROOT).contains("json")) {
            byte[] bytes;try(var stream=response.body()){bytes=stream.readNBytes(20_000_001);}
            if(bytes.length>20_000_000)throw new MobileFailure(502,"RESPOSTA_EXCEDIDA");
            if(bytes.length==0)return outgoing.build();
            return outgoing.body(MobileUpstream.sanitized(MobileUpstream.JSON.readTree(bytes)));
        }
        InputStream input=response.body();
        int first=input.read();
        if(first==-1){input.close();return outgoing.build();}
        // ResponseEntity<?> usa conversores de mensagens; o recurso preserva o streaming e fecha a entrada.
        InputStream content=new SequenceInputStream(new ByteArrayInputStream(new byte[]{(byte)first}),input);
        return outgoing.header("Content-Type",response.contentType()).body(new InputStreamResource(content));
    }
    @ExceptionHandler(MobileFailure.class) public ResponseEntity<?> failure(MobileFailure failure){return ResponseEntity.status(failure.status).body(Map.of("code",failure.code));}
    @ExceptionHandler({com.fasterxml.jackson.core.JsonProcessingException.class,IllegalArgumentException.class}) public ResponseEntity<?> invalid(){return ResponseEntity.badRequest().body(Map.of("code","DADOS_INVALIDOS"));}
}
