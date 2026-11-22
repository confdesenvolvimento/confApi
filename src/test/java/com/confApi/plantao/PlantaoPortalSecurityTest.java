package com.confApi.plantao;

import com.confApi.authv2.*;
import com.confApi.config.SecurityConfig;
import com.confApi.mobile.MobileChatDelegations;
import com.confApi.security.jwt.JwtService;
import com.confApi.service.UsuarioServiceImpl;
import com.confApi.repo.UsuarioRepository;
import org.springframework.core.env.MapPropertySource;
import javax.servlet.Filter;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PlantaoPortalSecurityTest {
    @Configuration @EnableWebMvc @Import({SecurityConfig.class,V2Security.class,PlantaoTokenController.class})
    static class Config {
        @Bean UsuarioRepository repository(){return mock(UsuarioRepository.class);}
        @Bean JwtService jwtService(){var jwt=mock(JwtService.class);when(jwt.tokenValido("portal-test")).thenReturn(true);when(jwt.obterLoginUsuario("portal-test")).thenReturn("portal");return jwt;}
        @Bean UsuarioServiceImpl users(){var users=mock(UsuarioServiceImpl.class);when(users.loadUserByUsername("portal")).thenReturn(User.withUsername("portal").password("synthetic").roles("USER").build());return users;}
        @Bean MobileSessionFilter mobileSessionFilter(){return new MobileSessionFilter();}
        @Bean MobileChatDelegations delegations(){return new MobileChatDelegations();}
        @Bean PlantaoTokenService v2(){return mock(PlantaoTokenService.class);}
        @Bean LegacyPlantaoTokenService legacy(){return new LegacyPlantaoTokenService(new org.springframework.boot.web.client.RestTemplateBuilder(),"https://unused.example.test",new LegacyPlantaoTokenDecoder("synthetic")) {
            @Override protected String generate(String login,String channel){return "0007";}
            @Override protected boolean consume(String login,String channel,String code){return true;}
        };}
    }
    AnnotationConfigWebApplicationContext context;MockMvc mvc;
    @BeforeEach void setup(){System.setProperty("AUTH_V2_ENABLED","false");context=new AnnotationConfigWebApplicationContext();context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",java.util.Map.of("security.jwt.expiracao","300","security.jwt.chave-assinatura","synthetic")));context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(context.getBean("springSecurityFilterChain",Filter.class)).build();}
    @AfterEach void cleanup(){context.close();System.clearProperty("AUTH_V2_ENABLED");}
    @Test void portalLegadoAutenticadoRecebeDesafioSemCodigoEValidaUmaVez() throws Exception {
        String body="{\"login\":\"alice\",\"canal\":\"APP\",\"reference\":\""+"r".repeat(43)+"\"}";
        var result=mvc.perform(post("/api/plantao/token/gerar").header("Authorization","Bearer portal-test").contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.success").value(true)).andReturn();
        var json=V2.JSON.readTree(result.getResponse().getContentAsString());assertTrue(json.path("token").isNull());
        var validation=V2.JSON.createObjectNode().put("login","alice").put("canal","APP").put("token","0007")
                .put("reference",json.path("reference").asText()).put("challengeId",json.path("challengeId").asText());
        mvc.perform(post("/api/plantao/token/validar").header("Authorization","Bearer portal-test").contentType("application/json").content(validation.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        mvc.perform(post("/api/plantao/token/validar").header("Authorization","Bearer portal-test").contentType("application/json").content(validation.toString())).andExpect(status().isUnauthorized());
        verifyNoInteractions(context.getBean(PlantaoTokenService.class));
    }
    @Test void semJwtOuJwtInvalidoContinuaBloqueado() throws Exception {
        mvc.perform(post("/api/plantao/token/gerar").contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/plantao/token/gerar").header("Authorization","Bearer invalid").contentType("application/json").content("{}")).andExpect(status().isForbidden());
    }
    @Test void v2LigadoNaoAceitaJwtLegadoENaoFazFallback() throws Exception {
        System.setProperty("AUTH_V2_ENABLED","true");
        mvc.perform(post("/api/plantao/token/gerar").secure(true).header("Authorization","Bearer portal-test").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(context.getBean(PlantaoTokenService.class));
    }
    @Test void rotasInternasV2ContinuamIndisponiveisQuandoDesligadas() throws Exception {
        mvc.perform(post("/internal/v2/autenticacao/primaria").secure(true).header("Authorization","Bearer portal-test"))
                .andExpect(status().isServiceUnavailable());
    }
    @Test void controllerSelecionaV2SemChamarLegadoMesmoSeV2Falhar() {
        System.setProperty("AUTH_V2_ENABLED","true");
        var v2=mock(PlantaoTokenService.class);var legacy=mock(LegacyPlantaoTokenService.class);
        var request=LegacyPlantaoTokenServiceTest.request("r".repeat(43));
        when(v2.gerar(request,"Bearer v2")).thenReturn(PlantaoTokenResponse.falha("indisponivel"));
        assertEquals(400,new PlantaoTokenController(v2,legacy).gerar(request,"Bearer v2").getStatusCodeValue());
        verifyNoInteractions(legacy);
    }
}
