package com.confApi.mobile;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;

@Configuration @Order(-5)
public class MobileSecurity extends WebSecurityConfigurerAdapter {
    private final MobileSettings settings;
    public MobileSecurity(MobileSettings settings){this.settings=settings;}
    @Override protected void configure(HttpSecurity http)throws Exception {
        http.requestMatcher(request->{String path=request.getRequestURI().substring(request.getContextPath().length());return path.equals("/api/mobile")||path.startsWith("/api/mobile/");})
            .csrf().disable().httpBasic().disable().formLogin().disable().requestCache().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and().authorizeRequests().anyRequest().permitAll().and()
            .addFilterBefore(new OncePerRequestFilter(){
                @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException {
                    res.setHeader("Cache-Control","no-store");res.setHeader("Pragma","no-cache");res.setHeader("X-Content-Type-Options","nosniff");
                    boolean local=settings.development()&&MobileSettings.local(req.getRemoteAddr())&&MobileSettings.local(req.getServerName())&&req.getHeader("Forwarded")==null&&req.getHeader("X-Forwarded-For")==null;
                    if(!settings.enabled()||!req.isSecure()&&!local){res.setStatus(settings.enabled()?400:503);res.setContentType("application/json");res.getWriter().write("{\"code\":\"MOBILE_INDISPONIVEL\"}");return;}
                    if(req.getContentLengthLong()>20_000_000){res.sendError(413);return;}
                    chain.doFilter(req,res);
                }
            },UsernamePasswordAuthenticationFilter.class);
    }
}
