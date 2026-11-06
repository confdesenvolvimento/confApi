package com.confApi.aereo;

import com.confApi.aereo.dto.PesquisaModalidadeRequest;
import com.confApi.aereo.dto.PesquisaRequestDTO;
import com.confApi.aereo.dto.PesquisaRequestDTOV2;
import com.confApi.aereo.dto.TarifarPesquisaRequest;
import com.confApi.aereo.eNums.ModalidadeTarifa;
import com.confApi.aereo.eNums.TipoTarifa;
import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings({"unchecked", "rawtypes"})
class AereoPesquisaModalidadeTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate rest = mock(RestTemplate.class);
    private final ConfAppService auth = mock(ConfAppService.class);
    private final AereoClient client = new AereoClient(rest);
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        UrlConfig.URL_CONFIANCA_HUB = "http://localhost.invalid";
        ReflectionTestUtils.setField(client, "confAppService", auth);
        AereoService service = mock(AereoService.class);
        AereoRegrasReservaService regras = mock(AereoRegrasReservaService.class);
        AereoController v1 = new AereoController(service, regras);
        AereoControllerV2 v2 = new AereoControllerV2(service, regras);
        ReflectionTestUtils.setField(v1, "aereoClient", client);
        ReflectionTestUtils.setField(v2, "aereoClient", client);
        mvc = MockMvcBuilders.standaloneSetup(v1, v2).build();
    }

    static Stream<Class<? extends PesquisaModalidadeRequest>> contratos() {
        return Stream.of(PesquisaRequestDTO.class, PesquisaRequestDTOV2.class, TarifarPesquisaRequest.class);
    }

    static Stream<Arguments> contratosInvalidos() {
        return contratos().flatMap(tipo -> Stream.of("\"INEXISTENTE\"", "\"RT\"", "0", "\"\"")
                .map(valor -> Arguments.of(tipo, valor)));
    }

    static Stream<Arguments> requestsLegados() {
        return rotas().flatMap(rota -> Stream.of("{}", "{\"modalidadeTarifa\":\"CONS\"}", "{\"modalidadeTarifa\":null}")
                .map(json -> Arguments.of(rota, json)));
    }

    static Stream<String> rotas() {
        return Stream.of("/v1/aereo/pesquisar", "/v2/aereo/pesquisar",
                "/v1/aereo/tarifarPesquisa", "/v2/aereo/tarifarPesquisa");
    }

    @ParameterizedTest
    @MethodSource("contratos")
    void ausenteConsENuloPreservamMesmoPayloadLegado(Class<? extends PesquisaModalidadeRequest> tipo) throws Exception {
        PesquisaModalidadeRequest ausente = mapper.readValue("{}", tipo);
        PesquisaModalidadeRequest cons = mapper.readValue("{\"modalidadeTarifa\":\"CONS\"}", tipo);
        PesquisaModalidadeRequest nulo = mapper.readValue("{\"modalidadeTarifa\":null}", tipo);
        assertEquals(ModalidadeTarifa.CONS, ausente.getModalidadeTarifa());
        assertEquals(ModalidadeTarifa.CONS, cons.getModalidadeTarifa());
        assertEquals(ModalidadeTarifa.CONS, nulo.getModalidadeTarifa());
        assertEquals(mapper.valueToTree(ausente), mapper.valueToTree(cons));
        assertEquals(mapper.valueToTree(ausente), mapper.valueToTree(nulo));
        assertFalse(mapper.valueToTree(ausente).has("modalidadeTarifa"));
    }

    @ParameterizedTest
    @MethodSource("contratosInvalidos")
    void modalidadeNaoAceitaCodigoRtOwNemOutroValor(Class<? extends PesquisaModalidadeRequest> tipo, String valor) {
        assertThrows(JsonProcessingException.class,
                () -> mapper.readValue("{\"modalidadeTarifa\":" + valor + "}", tipo));
    }

    @Test
    void rtOwContinuamIndependentesDaModalidadeComercial() throws Exception {
        PesquisaRequestDTO rt = mapper.readValue("""
                {"modalidadeTarifa":"OPER","pesquisa":{"tarifa":0}}
                """, PesquisaRequestDTO.class);
        PesquisaRequestDTOV2 ow = mapper.readValue("""
                {"modalidadeTarifa":"CONS","pesquisa":{"tarifa":1}}
                """, PesquisaRequestDTOV2.class);
        assertEquals(ModalidadeTarifa.OPER, rt.getModalidadeTarifa());
        assertEquals(TipoTarifa.RT, rt.getPesquisa().getTarifa());
        assertEquals(ModalidadeTarifa.CONS, ow.getModalidadeTarifa());
        assertEquals(TipoTarifa.OW, ow.getPesquisa().getTarifa());
    }

    @ParameterizedTest
    @MethodSource("rotas")
    void operRecebe400AntesDoTokenEHttp(String rota) throws Exception {
        mvc.perform(post(rota).contentType("application/json")
                        .content("{\"modalidadeTarifa\":\"OPER\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(auth, rest);
    }

    @ParameterizedTest
    @MethodSource("rotas")
    void modalidadeInvalidaRecebe400AntesDeExecutarPesquisa(String rota) throws Exception {
        mvc.perform(post(rota).contentType("application/json")
                        .content("{\"modalidadeTarifa\":\"OUTRA\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(auth, rest);
    }

    @ParameterizedTest
    @MethodSource("requestsLegados")
    void pesquisaTradicionalMantemOTransporteExistente(String rota, String json) throws Exception {
        ConfAppResp token = new ConfAppResp();
        token.setToken("token-teste");
        when(auth.token()).thenReturn(token);
        when(rest.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenAnswer(invocacao -> {
                    String endpoint = rota.endsWith("tarifarPesquisa") ? "/api/aereo/tarifarpesquisa" : "/api/aereo/pesquisa";
                    assertEquals("http://localhost.invalid" + endpoint, invocacao.getArgument(0));
                    HttpEntity<?> entity = invocacao.getArgument(2);
                    assertEquals("Bearer token-teste", entity.getHeaders().getFirst("Authorization"));
                    assertFalse(mapper.valueToTree(entity.getBody()).has("modalidadeTarifa"));
                    return ResponseEntity.ok(List.of());
                });
        mvc.perform(post(rota).contentType("application/json").content(json))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(auth).token();
        verify(rest).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class));
    }

    @Test
    void pesquisaEstritaTambemBloqueiaOperSemVirarFallback() {
        PesquisaRequestDTO request = new PesquisaRequestDTO();
        request.setModalidadeTarifa(ModalidadeTarifa.OPER);
        var erro = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> client.pesquisarDisponibilidadeEstrita(request));
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, erro.getStatus());
        verifyNoInteractions(auth, rest);
    }
}
