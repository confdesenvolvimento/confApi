package com.confApi.rodoviario;

import com.confApi.db.confManager.rodoviario.RodoviarioManagerClient;
import com.confApi.db.confManager.rodoviario.dto.ReservaRodoviarioDetalheDTO;
import com.confApi.hub.rodoviario.RodoviarioHubClient;
import com.confApi.rodoviario.dto.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RodoviarioControllerTest {
    RodoviarioHubClient hub;
    RodoviarioManagerClient manager;
    RodoviarioReservaService service;
    MockMvc mvc;
    ObjectMapper json = Jackson2ObjectMapperBuilder.json().build();
    @BeforeEach void setup() {
        hub = mock(RodoviarioHubClient.class); manager = mock(RodoviarioManagerClient.class); service = mock(RodoviarioReservaService.class);
        mvc = MockMvcBuilders.standaloneSetup(new RodoviarioController(manager), new RodoviarioHubController(hub, service, manager))
                .setControllerAdvice(new RodoviarioExceptionHandler(), new com.confApi.exception.GlobalExceptionHandler()).build();
    }
    @ParameterizedTest
    @ValueSource(strings = {"/v1/rodoviario/pesquisar", "/v1/rodoviario/pesquisa", "/rodoviario/pesquisar", "/rodoviario/pesquisa"})
    void pesquisaRecebeDtoComSistemaPadraoNosDoisPrefixosEAliases(String rota) throws Exception {
        String body = "{\"origem\":101,\"destino\":202,\"dataViagem\":\"2030-01-15\"}";
        var request = json.readValue(body, PesquisaRodoviarioRequestDTO.class);
        assertEquals("QueroPassagem", request.getSistema());
        assertEquals(java.time.LocalDate.of(2030, 1, 15), request.getDataViagem());
        assertEquals(101, request.getOrigem()); assertEquals(202, request.getDestino());
        when(hub.pesquisar(request)).thenReturn(json.readTree("{\"sistema\":\"QueroPassagem\",\"dados\":[]}"));
        mvc.perform(post(rota).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dados").isArray());
        verify(hub).pesquisar(request); verifyNoMoreInteractions(hub); verifyNoInteractions(service, manager);
    }
    @ParameterizedTest @ValueSource(strings = {"pesquisaconexoes", "sugestoesconexao"})
    void pesquisasDeConexaoUsamMesmoRequestTipado(String operacao) throws Exception {
        String body = "{\"sistema\":\"QueroPassagem\",\"origem\":101,\"destino\":202,\"dataViagem\":\"2030-01-15\"}";
        var request = json.readValue(body, PesquisaRodoviarioRequestDTO.class);
        var response = json.readTree("{\"sistema\":\"QueroPassagem\",\"dados\":[]}");
        when(hub.pesquisarConexoes(request)).thenReturn(response); when(hub.sugerirConexoes(request)).thenReturn(response);
        mvc.perform(post("/v1/rodoviario/" + operacao).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dados").isArray());
        if ("pesquisaconexoes".equals(operacao)) verify(hub).pesquisarConexoes(request); else verify(hub).sugerirConexoes(request);
        verifyNoMoreInteractions(hub); verifyNoInteractions(service, manager);
    }
    @ParameterizedTest @ValueSource(strings = {
            "{}",
            "{\"origem\":101,\"destino\":202}",
            "{\"origem\":\"CIT_1\",\"destino\":202,\"dataViagem\":\"2030-01-15\"}",
            "{\"origem\":101,\"destino\":101,\"dataViagem\":\"2030-01-15\"}",
            "{\"origem\":101,\"destino\":202,\"dataViagem\":\"2020-01-15\"}",
            "{\"origem\":101,\"destino\":202,\"dataViagem\":\"2030-02-30\"}",
            "{\"sistema\":\"\",\"origem\":101,\"destino\":202,\"dataViagem\":\"2030-01-15\"}",
            "{\"origem\":0,\"destino\":202,\"dataViagem\":\"2030-01-15\"}",
            "{\"origem\":101,\"destino\":-1,\"dataViagem\":\"2030-01-15\"}"
    })
    void pesquisaInvalidaRetorna400AntesDoHub(String body) throws Exception {
        mvc.perform(post("/v1/rodoviario/pesquisar").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("REQUEST_INVALIDO"));
        verifyNoInteractions(hub, manager, service);
    }
    @Test void reservaUsaAutenticacaoPadraoSemExigirSegundoToken() throws Exception {
        var result = new RodoviarioReservaOperacaoResponse(); result.setSuccess(true);
        when(service.reservar(any(), isNull())).thenReturn(result);
        mvc.perform(post("/v1/rodoviario/reservar").header("Authorization", "Bearer APP_TEST")
                .contentType(MediaType.APPLICATION_JSON).content(RodoviarioReservaServiceTest.REQUEST))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(service).reservar(any(RodoviarioCompraModel.class), isNull());
        verifyNoInteractions(hub, manager);
    }
    @Test void consultasDoManagerNaoExigemSegundoToken() throws Exception {
        when(manager.detalhe(10, null)).thenReturn(new ReservaRodoviarioDetalheDTO());
        mvc.perform(get("/v1/rodoviario/reservas/10/detalhe").header("Authorization", "Bearer APP_TEST"))
                .andExpect(status().isOk());
        mvc.perform(get("/v1/rodoviario/findAllReservas").header("Authorization", "Bearer APP_TEST"))
                .andExpect(status().isOk());
        verify(manager).detalhe(10, null);
        verify(manager).crud(eq(RodoviarioManagerClient.Recurso.reservas), isNull(), eq(org.springframework.http.HttpMethod.GET), isNull(), any(), isNull());
    }
    @Test void contratoDeReservaDelegaOperacaoCompletaComTokenIndividual() throws Exception {
        var result = new RodoviarioReservaOperacaoResponse(); result.setSuccess(true);
        when(service.reservar(any(), eq("USER_TEST"))).thenReturn(result);
        mvc.perform(post("/v1/rodoviario/reservar").header("X-Usuario-Token", "USER_TEST")
                .contentType(MediaType.APPLICATION_JSON).content(RodoviarioReservaServiceTest.REQUEST))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(service).reservar(any(RodoviarioCompraModel.class), eq("USER_TEST")); verifyNoInteractions(hub);
    }
    @Test void preservaEndpointsDoManagerSemConfundirComRotasDoHub() throws Exception {
        when(manager.detalhe(10, "USER_TEST")).thenReturn(new ReservaRodoviarioDetalheDTO());
        mvc.perform(get("/rodoviario/reservas/10/detalhe").header("X-Usuario-Token", "USER_TEST")).andExpect(status().isOk());
        mvc.perform(get("/v1/rodoviario/reservas/10/detalhe").header("X-Usuario-Token", "USER_TEST")).andExpect(status().isOk());
        verify(manager, times(2)).detalhe(10, "USER_TEST"); verifyNoInteractions(hub, service);
    }
    @Test void parametroInvalidoNaoSeTornaErroInterno() throws Exception {
        mvc.perform(get("/rodoviario/recurso-invalido").header("X-Usuario-Token", "USER_TEST"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("REQUEST_INVALIDO"));
        verifyNoInteractions(hub, manager, service);
    }
    @Test void falhaInesperadaERegistradaSemExporMensagemNoRetorno() throws Exception {
        when(hub.catalogo("viacoes", null, null)).thenThrow(new IllegalStateException("SEGREDO_TOKEN"));
        mvc.perform(get("/v1/rodoviario/viacoes"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SEGREDO"))));
    }
    @Test void jsonMalformadoContinuaRetornando400AntesDoFornecedor() throws Exception {
        mvc.perform(post("/v1/rodoviario/pesquisar").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("REQUEST_INVALIDO"));
        verifyNoInteractions(hub, manager, service);
    }
}
