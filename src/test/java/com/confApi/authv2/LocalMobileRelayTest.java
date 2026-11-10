package com.confApi.authv2;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.ResponseEntity;
import javax.servlet.Filter;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LocalMobileRelayTest {
 static class Relay extends MobileAuthenticatorRelay {
  protected String gatewayToken(){return "Bearer synthetic-service";}
  protected ResponseEntity<?> forward(String path,String method,byte[] body,Map<String,String> headers){return ResponseEntity.accepted().body(Map.of("state","PROVISORIA"));}
 }
 @Configuration @EnableWebMvc @EnableWebSecurity @Import({MobileAuthenticatorSecurity.class,V2Security.class,V2Errors.class,LocalDevelopment.class})
 static class Config { @Bean Relay relay(){return new Relay();} }
 AnnotationConfigWebApplicationContext context;MockMvc mvc;
 void open(String profile){System.setProperty("AUTH_V2_ENABLED","true");context=new AnnotationConfigWebApplicationContext();context.setServletContext(new MockServletContext());context.getEnvironment().setActiveProfiles(profile);context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("local-test",Map.of("app.dev","true","app.dev.allowed-origins","http://192.168.50.4:8888")));context.register(Config.class);context.refresh();mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(context.getBean("springSecurityFilterChain",Filter.class)).build();}
 org.springframework.test.web.servlet.request.RequestPostProcessor local(){return r->{r.setServerName("192.168.50.4");r.setServerPort(8888);r.setLocalAddr("192.168.50.4");r.setRemoteAddr("192.168.50.6");return r;};}
 @AfterEach void close(){if(context!=null)context.close();System.clearProperty("AUTH_V2_ENABLED");}
 @Test void devHttpReachesAuthenticatorButProfileStillRequiresProof()throws Exception{open("dev");mvc.perform(post(MobileAuthenticatorRelay.ROOT+"/sessoes").with(local())).andExpect(status().isAccepted());mvc.perform(get(MobileAuthenticatorRelay.ROOT+"/perfil").with(local())).andExpect(status().isUnauthorized());}
 @Test void productionAndPublicPeersRefuseHttpEvenWithDevHeader()throws Exception{open("prod");mvc.perform(post(MobileAuthenticatorRelay.ROOT+"/sessoes").with(local()).header("dev","true")).andExpect(status().isBadRequest());}
 @Test void localModeRejectsPublicPeersOtherPortsAndQueries()throws Exception{open("dev");mvc.perform(post(MobileAuthenticatorRelay.ROOT+"/sessoes").with(local()).with(r->{r.setRemoteAddr("8.8.8.8");return r;})).andExpect(status().isBadRequest());mvc.perform(post(MobileAuthenticatorRelay.ROOT+"/sessoes").with(local()).with(r->{r.setServerPort(8889);return r;})).andExpect(status().isBadRequest());mvc.perform(post(MobileAuthenticatorRelay.ROOT+"/sessoes?dev=true").with(local())).andExpect(status().isBadRequest());}
}
