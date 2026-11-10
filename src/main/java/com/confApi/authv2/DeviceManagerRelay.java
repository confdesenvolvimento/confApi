package com.confApi.authv2;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

/** CService calls this business API; only Manager has access to the device database. */
@RestController
public class DeviceManagerRelay {
    protected Object send(String path,String method,JsonNode body,HttpServletRequest request,String scope,boolean idempotent) {
        var actor=V2Security.actor(request,scope);
        if(!"SERVICE".equals(actor.claims().getClaim("token_use")))throw new V2.Failure(403);
        String key=idempotent?request.getHeader("Idempotency-Key"):null;
        if(idempotent&&(key==null||!key.matches("[A-Za-z0-9_-]{16,128}")))throw new V2.Failure(400);
        return forward(path,method,body,request.getHeader("Authorization"),key);
    }
    protected Object forward(String path,String method,JsonNode body,String authorization,String key) {
        return V2.request(V2.setting("AUTH_V2_MANAGER_URL"),path,method,body,authorization,key);
    }
    private String id(String id){if(id==null||!id.matches("[A-Za-z0-9_-]{1,128}"))throw new V2.Failure(400);return id;}
    private String enrollment(String id){return "/internal/v2/dispositivos/solicitacoes/"+id(id);}
    @PostMapping("/internal/v2/autenticacao/primaria") public Object primary(@RequestBody JsonNode b,HttpServletRequest r){return send("/internal/v2/autenticacao/primaria","POST",b,r,"auth:primary",false);}
    @PostMapping("/internal/v2/autenticacao/primaria/introspectar") public Object introspect(@RequestBody JsonNode b,HttpServletRequest r){return send("/internal/v2/autenticacao/primaria/introspectar","POST",b,r,"auth:introspect",false);}
    @GetMapping("/internal/v2/dispositivos/usuarios/{id}/autorizacao") public Object authorization(@PathVariable String id,HttpServletRequest r){return send("/internal/v2/dispositivos/usuarios/"+id(id)+"/autorizacao","GET",null,r,"device:read",false);}
    @GetMapping("/internal/v2/usuarios/{id}/perfil-mobile") public Object profile(@PathVariable String id,HttpServletRequest r){return send("/internal/v2/usuarios/"+id(id)+"/perfil-mobile","GET",null,r,"profile:read",false);}
    @PostMapping("/internal/v2/dispositivos/solicitacoes") public Object enroll(@RequestBody JsonNode b,HttpServletRequest r){return send("/internal/v2/dispositivos/solicitacoes","POST",b,r,"device:enroll",true);}
    @GetMapping("/internal/v2/dispositivos/solicitacoes/{id}") public Object status(@PathVariable String id,HttpServletRequest r){return send(enrollment(id),"GET",null,r,"device:read",false);}
    @PostMapping("/internal/v2/dispositivos/solicitacoes/{id}/ativar") public Object activate(@PathVariable String id,@RequestBody JsonNode b,HttpServletRequest r){return send(enrollment(id)+"/ativar","POST",b,r,"device:activate",true);}
    @GetMapping("/internal/v2/dispositivos/solicitacoes/{id}/contato") public Object contact(@PathVariable String id,HttpServletRequest r){return send(enrollment(id)+"/contato","GET",null,r,"contact:read",false);}
    @PostMapping("/internal/v2/dispositivos/solicitacoes/{id}/confirmacao") public Object confirm(@PathVariable String id,@RequestBody JsonNode b,HttpServletRequest r){return send(enrollment(id)+"/confirmacao","POST",b,r,"contact:confirm",false);}
    @PostMapping("/internal/v2/dispositivos/solicitacoes/{id}/entregas") public Object deliver(@PathVariable String id,@RequestBody JsonNode b,HttpServletRequest r){return send(enrollment(id)+"/entregas","POST",b,r,"contact:deliver",true);}
    @PutMapping("/internal/v2/dispositivos/usuarios/{id}/push") public Object push(@PathVariable String id,@RequestBody JsonNode b,HttpServletRequest r){return send("/internal/v2/dispositivos/usuarios/"+id(id)+"/push","PUT",b,r,"device:push",false);}
}
