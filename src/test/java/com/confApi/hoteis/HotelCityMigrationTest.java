package com.confApi.hoteis;

import com.confApi.cacheHotel.cidade.*;
import com.confApi.confApp.*;
import com.confApi.config.UrlConfig;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HotelCityMigrationTest {
    private final String managerOriginal=UrlConfig.URL_CONFIANCA_MANAGER;
    private final String cacheOriginal=UrlConfig.URL_CONFIANCA_CACHEHOTEL;
    @AfterEach void restore(){UrlConfig.URL_CONFIANCA_MANAGER=managerOriginal;UrlConfig.URL_CONFIANCA_CACHEHOTEL=cacheOriginal;}

    @Test void cidadesPassamPeloControllerManagerECacheComEspacosEAcentos() throws Exception {
        UrlConfig.URL_CONFIANCA_MANAGER="http://fixture/manager";
        UrlConfig.URL_CONFIANCA_CACHEHOTEL="http://fixture/cache/";
        RestTemplate rest=new RestTemplate();
        MockRestServiceServer server=MockRestServiceServer.bindTo(rest).build();
        ConfAppService auth=mock(ConfAppService.class);
        ConfAppResp token=new ConfAppResp();token.setToken("fixture");when(auth.token()).thenReturn(token);
        CacheHotelCidadeAPI cache=new CacheHotelCidadeAPI();
        ReflectionTestUtils.setField(cache,"restTemplate",rest);ReflectionTestUtils.setField(cache,"confAppService",auth);
        HotelCityService service=new HotelCityService(rest,auth,cache);
        server.expect(requestTo("http://fixture/manager/hotel/buscarCidadesComQuantidadeHoteis?nome=sao%20p&query=sao%20p"))
            .andExpect(method(HttpMethod.GET)).andExpect(header("Authorization","Bearer fixture"))
            .andRespond(withSuccess("[{\"codeCidade\":123,\"quantidadeHoteis\":12}]",MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://fixture/cache/cidadeHtl/search?query=sao%20p"))
            .andRespond(withSuccess("[]",MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://fixture/cache/cidadeHtl/search?query=sao"))
            .andRespond(withSuccess("""
                [{"id":123,"codeCidade":"C123","nomeCidade":"São Paulo","nomeEstado":"São Paulo","nomePais":"Brasil","codePais":"40","codeEstado":"35"},
                 {"id":456,"codeCidade":"C456","nomeCidade":"São José","codePais":"40","codeEstado":"35"}]
                """,MediaType.APPLICATION_JSON));
        var mvc=MockMvcBuilders.standaloneSetup(new HotelSearchController(mock(HotelSearchService.class),service)).build();
        String response=mvc.perform(get("/v1/hoteis/cidades").param("query","sao p"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        var cidades=new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
        assertEquals(1,cidades.size());assertEquals("São Paulo",cidades.get(0).path("nomeCidade").asText());
        assertEquals("C123",cidades.get(0).path("codeCidade").asText());assertEquals(12,cidades.get(0).path("quantidadeHoteis").asInt());
        server.verify();
    }
}
