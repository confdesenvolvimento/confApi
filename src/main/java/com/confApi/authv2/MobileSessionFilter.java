package com.confApi.authv2;
import org.springframework.stereotype.Component;import org.springframework.web.filter.OncePerRequestFilter;import javax.servlet.*;import javax.servlet.http.*;import java.io.*;import java.util.*;import org.springframework.security.core.context.SecurityContextHolder;import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;import org.springframework.security.core.authority.SimpleGrantedAuthority;
@Component public class MobileSessionFilter extends OncePerRequestFilter {
 protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws IOException,ServletException {
  String auth=req.getHeader("Authorization");if(auth==null||!auth.startsWith("DPoP ")){chain.doFilter(req,res);return;}
  try{if(!V2.enabled()||!req.isSecure()||req.getQueryString()!=null)throw new V2.Failure(401);String route=req.getRequestURI();requireRoute(req.getMethod(),route.substring(req.getContextPath().length()));String origin=V2.setting("AUTH_V2_RESOURCE_ORIGIN");String proof=req.getHeader("DPoP");if(proof==null||proof.length()>8192||auth.length()!=48||origin.isBlank())throw new V2.Failure(401);
   com.fasterxml.jackson.databind.JsonNode result;
   try{result=introspect(Map.of("token",auth.substring(5),"proof",proof,"method",req.getMethod(),"uri",origin+route));}
   catch(V2.Failure failure){if(failure.status==401||failure.status==403)throw new V2.Failure(401);throw failure;}
   if(!result.path("active").asBoolean()||!result.path("mfa").asBoolean()||result.path("userId").asText().isBlank()||result.path("companyId").asText().isBlank())throw new V2.Failure(401);
   var authenticated=new UsernamePasswordAuthenticationToken(result.path("userId").asText(),null,List.of(new SimpleGrantedAuthority("ROLE_USER")));authenticated.setDetails(result);SecurityContextHolder.getContext().setAuthentication(authenticated);req.setAttribute("auth.v2.identity",result);res.setHeader("Cache-Control","no-store");chain.doFilter(req,res);
  }catch(V2.Failure e){res.setStatus(e.status);res.setContentType("application/json");V2.JSON.writeValue(res.getOutputStream(),Map.of("code","AUTH_V2_"+e.status));}
 }

 protected com.fasterxml.jackson.databind.JsonNode introspect(Map<String,String> body){return V2.request(V2.setting("AUTH_V2_MANAGER_URL"),"/internal/v2/sessoes/introspectar","POST",body,V2.token("AUTH_V2_MANAGER_TOKEN_FILE"),null);}
 static void requireRoute(String method,String path){
  try{var routes=V2.JSON.readTree(V2.setting("AUTH_V2_MOBILE_ROUTES_JSON"));if(routes!=null&&routes.isArray())for(var route:routes){if(method.equals(route.path("method").asText())&&new org.springframework.util.AntPathMatcher().match(route.path("path").asText(),path))return;}}catch(Exception ignored){}
  throw new V2.Failure(403);
 }
}
