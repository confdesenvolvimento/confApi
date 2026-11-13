package com.confApi.authv2;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;

/** Dedicated chain; the downstream authenticator checks the user's DPoP proof. */
@Configuration @Order(0)
public class MobileAuthenticatorSecurity extends WebSecurityConfigurerAdapter {
    @org.springframework.beans.factory.annotation.Autowired(required=false) private LocalDevelopment localDevelopment=LocalDevelopment.disabled();
    @Override protected void configure(HttpSecurity http) throws Exception {
        http.requestMatcher(request->{String path=request.getRequestURI().substring(request.getContextPath().length());return path.equals(MobileAuthenticatorRelay.ROOT)||path.startsWith(MobileAuthenticatorRelay.ROOT+"/");})
                .csrf().disable().httpBasic().disable().formLogin().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeRequests().anyRequest().permitAll().and()
                .addFilterBefore(new OncePerRequestFilter(){
                    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
                        String correlation=UUID.randomUUID().toString();req.setAttribute("auth.v2.correlation",correlation);
                        res.setHeader("Cache-Control","no-store");res.setHeader("Pragma","no-cache");res.setHeader("X-Correlation-ID",correlation);
                        try {
                            if(!V2.enabled())throw new V2.Failure(503);
                            if((!req.isSecure()&&!localDevelopment.allows(req))||req.getQueryString()!=null)throw new V2.Failure(400);
                            String path=req.getRequestURI().substring(req.getContextPath().length());
                            if(!MobileAuthenticatorRelay.allowed(req.getMethod(),path))throw new V2.Failure(404);
                            chain.doFilter(req,res);
                        }catch(V2.Failure e){res.setStatus(e.status);res.setContentType("application/json");V2.JSON.writeValue(res.getOutputStream(),Map.of("code","AUTH_V2_"+e.status,"correlationId",correlation));}
                    }
                },UsernamePasswordAuthenticationFilter.class);
    }
}
