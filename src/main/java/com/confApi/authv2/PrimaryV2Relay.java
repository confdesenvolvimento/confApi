package com.confApi.authv2;
import org.springframework.web.bind.annotation.*;import javax.servlet.http.HttpServletRequest;import com.fasterxml.jackson.databind.JsonNode;
@RestController public class PrimaryV2Relay {
 @PostMapping("/internal/v2/autenticacao/primaria/portal") public Object primary(@RequestBody JsonNode b,HttpServletRequest r){V2Security.actor(r,"portal:primary");return V2.request(V2.setting("AUTH_V2_MANAGER_URL"),"/internal/v2/autenticacao/primaria/portal","POST",b,r.getHeader("Authorization"),null);}
 @PostMapping("/internal/v2/autenticacao/portal/concluir") public Object complete(@RequestBody JsonNode b,HttpServletRequest r){V2Security.actor(r,"portal:complete");return V2.request(V2.setting("AUTH_V2_MANAGER_URL"),"/internal/v2/autenticacao/portal/concluir","POST",b,r.getHeader("Authorization"),null);}
}
