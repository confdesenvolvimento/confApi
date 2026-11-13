package com.confApi.authv2;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.*;import javax.servlet.http.*;import java.io.*;import java.util.*;
@Configuration @Order(1) public class V2Security extends WebSecurityConfigurerAdapter {
 @org.springframework.beans.factory.annotation.Autowired(required=false) private LocalDevelopment localDevelopment=LocalDevelopment.disabled();
 public static V2.Actor actor(HttpServletRequest req,String scope){var actor=(V2.Actor)req.getAttribute("auth.v2.actor");if(actor==null)throw new V2.Failure(401);actor.require(scope);return actor;}
 @Override protected void configure(HttpSecurity http)throws Exception {
  http.requestMatcher(req->{String p=req.getRequestURI().substring(req.getContextPath().length());return p.startsWith("/internal/v2/")||p.startsWith("/api/v2/")||(p.startsWith("/api/plantao/token")||(V2.enabled()&&p.startsWith("/api/auth/")));})
   .csrf().disable().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and().authorizeRequests().anyRequest().hasRole("V2").and()
   .addFilterBefore(new OncePerRequestFilter(){protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException {
    res.setHeader("Cache-Control","no-store");res.setHeader("X-Correlation-ID",UUID.randomUUID().toString());
    try{if(!V2.enabled())throw new V2.Failure(503);if((!req.isSecure()&&!localDevelopment.allows(req))||req.getQueryString()!=null)throw new V2.Failure(400);
     String localPath=req.getRequestURI().substring(req.getContextPath().length()); boolean legacy=localPath.startsWith("/api/")&&!localPath.startsWith("/api/v2/");
     var actor=V2.verify(req.getHeader("Authorization"),legacy?"confapi-v2":"manager-v2",legacy?"legacy:token":"v2:access");if(req.getRequestURI().substring(req.getContextPath().length()).startsWith("/internal/v2/") && !"SERVICE".equals(actor.claims().getClaim("token_use"))) throw new V2.Failure(403);req.setAttribute("auth.v2.actor",actor);
     SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor,null,List.of(new SimpleGrantedAuthority("ROLE_V2"))));chain.doFilter(req,res);
    }catch(V2.Failure e){res.setStatus(e.status);res.setContentType("application/json");V2.JSON.writeValue(res.getOutputStream(),Map.of("code","AUTH_V2_"+e.status));}finally{SecurityContextHolder.clearContext();}
   }},UsernamePasswordAuthenticationFilter.class);
 }
}
