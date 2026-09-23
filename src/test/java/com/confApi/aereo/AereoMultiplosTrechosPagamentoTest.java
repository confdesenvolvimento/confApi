package com.confApi.aereo;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AereoMultiplosTrechosPagamentoTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private String urlOriginal;
    private MockRestServiceServer hub;
    private MockMvc mvc;

    @BeforeEach
    void configurar() {
        urlOriginal = UrlConfig.URL_CONFIANCA_HUB;
        UrlConfig.URL_CONFIANCA_HUB = "http://localhost/hub/";
        RestTemplate http = new RestTemplate();
        hub = MockRestServiceServer.createServer(http);
        AereoClient client = new AereoClient(http);
        ConfAppService auth = mock(ConfAppService.class);
        ConfAppResp token = new ConfAppResp();
        token.setToken("token-teste");
        when(auth.token()).thenReturn(token);
        ReflectionTestUtils.setField(client, "confAppService", auth);

        AereoService service = mock(AereoService.class);
        AereoRegrasReservaService regras = mock(AereoRegrasReservaService.class);
        AereoController v1 = new AereoController(service, regras);
        AereoControllerV2 v2 = new AereoControllerV2(service, regras);
        ReflectionTestUtils.setField(v1, "aereoClient", client);
        ReflectionTestUtils.setField(v2, "aereoClient", client);
        mvc = MockMvcBuilders.standaloneSetup(v1, v2).build();
    }

    @AfterEach
    void restaurar() {
        UrlConfig.URL_CONFIANCA_HUB = urlOriginal;
        hub.verify();
    }

    @Test
    void devePreservarTrechosAlternativasESegmentosNaPesquisa() throws Exception {
        hub.expect(requestTo("http://localhost/hub/api/aereo/pesquisa"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.tipoPesquisa").value("MULTIPLOSTRECHOS"))
                .andExpect(jsonPath("$.multiplosTrechos.length()").value(3))
                .andExpect(jsonPath("$.multiplosTrechos[0].origem").value("CGB"))
                .andExpect(jsonPath("$.multiplosTrechos[2].destino").value("CGB"))
                .andExpect(jsonPath("$.multiplosTrechos[2].data").value(1790200000000L))
                .andRespond(withSuccess("""
                        [{
                          "viagensMultiplosTrechos": [
                            {"sistema":"Wooba","identificacaoDaViagem":"IDA-A","isNdc":true,
                             "familias":[{"classe":"Y"},{"classe":"B"}],
                             "voos":[{"numeroVoo":"100","tipoSegmento":1}]},
                            {"sistema":"Wooba","identificacaoDaViagem":"IDA-B",
                             "voos":[{"numeroVoo":"101","tipoSegmento":1}]},
                            {"sistema":"Wooba","identificacaoDaViagem":"MEIO",
                             "voos":[{"numeroVoo":"200","tipoSegmento":2}]},
                            {"sistema":"Wooba","identificacaoDaViagem":"FIM",
                             "voos":[{"numeroVoo":"300","tipoSegmento":3}]}
                          ],
                          "trechos1":[], "trechos2":[]
                        }]
                        """, MediaType.APPLICATION_JSON));

        JsonNode response = enviar("/v2/aereo/pesquisar", """
                {"tipoPesquisa":"MULTIPLOSTRECHOS","qtdADT":1,
                 "multiplosTrechos":[
                   {"origem":"CGB","destino":"GRU","data":1790000000000},
                   {"origem":"GRU","destino":"BSB","data":1790100000000},
                   {"origem":"BSB","destino":"CGB","data":1790200000000}
                 ]}
                """);
        assertEquals(true, response.isArray());
        JsonNode trechos = response.get(0).path("viagensMultiplosTrechos");
        assertEquals(4, trechos.size());
        assertEquals(2, trechos.get(0).path("familias").size());
        assertEquals(true, trechos.get(0).path("isNdc").asBoolean());
        assertEquals("FIM", trechos.get(3).path("identificacaoDaViagem").asText());
        assertEquals(3, trechos.get(3).path("voos").get(0).path("tipoSegmento").asInt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v1/aereo", "/v2/aereo"})
    void devePreservarIdentificadoresEClassesNaTarifacaoEReserva(String prefixo) throws Exception {
        hub.expect(requestTo("http://localhost/hub/api/aereo/tarifar"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.viagensMultiplas[0]").value("IDA-A"))
                .andExpect(jsonPath("$.viagensMultiplas[1]").value("MEIO"))
                .andExpect(jsonPath("$.viagensMultiplas[2]").value("FIM"))
                .andExpect(jsonPath("$.classes[2].trecho").value(3))
                .andExpect(jsonPath("$.classes[2].identificacaoDeVoo").value("V3"))
                .andRespond(withSuccess("""
                        {"trecho1":[
                          {"identificacaoDaViagem":"IDA-A","voos":[{"tipoSegmento":1}]},
                          {"identificacaoDaViagem":"MEIO","voos":[{"tipoSegmento":2}]},
                          {"identificacaoDaViagem":"FIM","voos":[{"tipoSegmento":3}]}
                        ],"trecho2":[]}
                        """, MediaType.APPLICATION_JSON));
        hub.expect(requestTo("http://localhost/hub/api/aereo/reservar"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.identificacaoViagemMultipla[0]").value("IDA-A"))
                .andExpect(jsonPath("$.identificacaoViagemMultipla[1]").value("MEIO"))
                .andExpect(jsonPath("$.identificacaoViagemMultipla[2]").value("FIM"))
                .andExpect(jsonPath("$.classesSelecionadas[2].trecho").value(3))
                .andRespond(withSuccess("{\"reservas\":[]}", MediaType.APPLICATION_JSON));

        String classes = """
                [{"trecho":1,"classe":"Y","identificacaoDeVoo":"V1"},
                 {"trecho":2,"classe":"Y","identificacaoDeVoo":"V2"},
                 {"trecho":3,"classe":"B","identificacaoDeVoo":"V3"}]
                """;
        JsonNode tarifa = enviar(prefixo + "/tarifar", """
                {"sistema":"Wooba","viagensMultiplas":["IDA-A","MEIO","FIM"],"classes":%s}
                """.formatted(classes));
        assertEquals(3, tarifa.path("trecho1").size());
        assertEquals(3, tarifa.path("trecho1").get(2).path("voos").get(0).path("tipoSegmento").asInt());
        enviar(prefixo + "/reservar", """
                {"sistema":"Wooba","identificacaoViagemMultipla":["IDA-A","MEIO","FIM"],
                 "classesSelecionadas":%s}
                """.formatted(classes));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v1/aereo", "/v2/aereo"})
    void deveConsultarPrazoEspecialEEnviarSeuCodigoNaEmissao(String prefixo) throws Exception {
        String configuracao = """
                {"localizador":"TESTE1","configuracoesDeEmissao":{"opcoesDePagamentoWooba":[
                  {"descricao":"Faturado","codigoFormaDeRecebimento":1,"faturado":true},
                  {"descricao":"Prazo Especial","codigoFormaDeRecebimento":70,"faturado":true,
                   "codigoFormaDeRecebimentoTipo":1,"exigirAutorizacaoManual":true}
                ]}}
                """;
        hub.expect(requestTo("http://localhost/hub/api/aereo/iniciaremissao"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token-teste"))
                .andExpect(jsonPath("$.localizador").value("TESTE1"))
                .andExpect(jsonPath("$.sistema").value("Wooba"))
                .andRespond(withSuccess(configuracao, MediaType.APPLICATION_JSON));
        hub.expect(requestTo("http://localhost/hub/api/aereo/emitir"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.formaDePagamento").value(70))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        JsonNode resposta = enviar(prefixo + "/iniciarEmissao",
                "{\"localizador\":\"TESTE1\",\"sistema\":\"Wooba\"}");
        assertEquals(mapper.readTree(configuracao), resposta);
        int codigoSelecionado = resposta.path("configuracoesDeEmissao")
                .path("opcoesDePagamentoWooba").get(1).path("codigoFormaDeRecebimento").asInt();
        enviar(prefixo + "/emitir",
                "{\"localizador\":\"TESTE1\",\"sistema\":\"Wooba\",\"formaDePagamento\":" + codigoSelecionado + "}");
    }

    @Test
    void devePreservarErroDeNegocioNaConsultaDePagamento() throws Exception {
        hub.expect(requestTo("http://localhost/hub/api/aereo/iniciaremissao"))
                .andRespond(withSuccess(
                        "{\"exception\":{\"message\":\"Localizador indisponivel\"}}",
                        MediaType.APPLICATION_JSON));
        JsonNode resposta = enviar("/v1/aereo/iniciarEmissao", "{\"localizador\":\"TESTE1\"}");
        assertEquals("Localizador indisponivel", resposta.path("exception").path("message").asText());
    }

    @Test
    void deveManterContratoDeIdaVoltaComUrlSemBarraFinal() throws Exception {
        UrlConfig.URL_CONFIANCA_HUB = "http://localhost/hub";
        hub.expect(requestTo("http://localhost/hub/api/aereo/pesquisa"))
                .andRespond(withSuccess("""
                        [{"trechos1":[{"identificacaoDaViagem":"IDA"}],
                          "trechos2":[{"identificacaoDaViagem":"VOLTA"}]}]
                        """, MediaType.APPLICATION_JSON));
        JsonNode resposta = enviar("/v2/aereo/pesquisar", "{\"tipoPesquisa\":\"ROUNDTRIP\"}");
        assertEquals(true, resposta.isArray());
        assertEquals("IDA", resposta.get(0).path("trechos1").get(0).path("identificacaoDaViagem").asText());
        assertEquals("VOLTA", resposta.get(0).path("trechos2").get(0).path("identificacaoDaViagem").asText());
    }

    private JsonNode enviar(String caminho, String json) throws Exception {
        String resposta = mvc.perform(post(caminho).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(resposta);
    }
}
