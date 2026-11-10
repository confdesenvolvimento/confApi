package com.confApi.aereo;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Controller, cliente e conversor reais; apenas HUB, Manager e token são simulados. */
class AereoReservaNascimentoPersistenciaTest {
    private final String hubOriginal = UrlConfig.URL_CONFIANCA_HUB;
    private final String managerOriginal = UrlConfig.URL_CONFIANCA_MANAGER;
    private final ObjectMapper json = new ObjectMapper();
    private MockRestServiceServer server;
    private MockMvc mvc;

    @BeforeEach void setup() {
        UrlConfig.URL_CONFIANCA_HUB = "http://fixture/hub/";
        UrlConfig.URL_CONFIANCA_MANAGER = "http://fixture/manager/";
        RestTemplate rest = new RestTemplate();
        server = MockRestServiceServer.bindTo(rest).build();
        AereoClientV2 client = new AereoClientV2(rest);
        ConfAppService auth = mock(ConfAppService.class);
        ConfAppResp token = new ConfAppResp(); token.setToken("fixture");
        when(auth.token()).thenReturn(token);
        ReflectionTestUtils.setField(client, "confAppService", auth);
        ReflectionTestUtils.setField(client, "aereoService", new AereoService());
        AereoControllerV2 controller = new AereoControllerV2();
        ReflectionTestUtils.setField(controller, "aereoClientV2", client);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @AfterEach void restore() {
        UrlConfig.URL_CONFIANCA_HUB = hubOriginal;
        UrlConfig.URL_CONFIANCA_MANAGER = managerOriginal;
    }

    @ParameterizedTest
    @CsvSource({"2002-10-10,2002-10-10", "10/10/2002,2002-10-10",
            "2002-10-15,2002-10-15", "15/10/2002,2002-10-15",
            "2002-03-04,2002-03-04", "04/03/2002,2002-03-04",
            "2000-02-29,2000-02-29", "29/02/2000,2000-02-29"})
    void reservaComPassageiroRetornaLocalizadorEGravaNascimentoNoManager(String nascimento, String dia) throws Exception {
        String entrada = """
            {"tipoTrecho":0,"tipoVooPesquisa":1,"sistema":"Wooba","fonte":0,
             "usuario":{"id":42,"agencia":{"codgAgencia":99,"codgUnidade":{"codgUnidade":7}}},
             "contatos":[{"nome":"PASSAGEIRO TESTE","email":"teste@example.invalid","numeroDDD":"11","numeroTelefone":"999999999"}],
             "passageiros":[{"nome":"PASSAGEIRO","sobrenome":"TESTE","faixaEtaria":"ADT","sexo":"M",
                "email":"teste@example.invalid","nascimento":"%s","cpf":"00123456789"}],
             "valoresReservaAdt":[{"tipoTarifaPax":1}],
             "trechos":[{"sistema":"Wooba","identificacaoDaViagem":"IDA",
               "familiaSelecionada":{"classe":"Y","baseTarifaria":"YTEST","familia":{"codgFamilia":"LIGHT"}},
               "voos":[{"numeroVoo":"1001","identificacaoDoVoo":"VOO1"}]}]}
            """.formatted(nascimento);
        String respostaHub = """
            {"reservas":[{"localizador":"TESTE1","sistema":"Wooba","status":"Reservada",
             "prazoEmissao":"2026-11-11T12:00:00.000-03:00",
             "contatos":[{"nome":"PASSAGEIRO TESTE","numeroDDD":"11","numeroTelefone":"999999999"}],
             "passageiros":[{"nome":"PASSAGEIRO","sobrenome":"TESTE","faixaEtaria":"ADT","sexo":"M","nascimento":"%sT12:00:00"}],
             "valorReserva":{"valorBase":{"valorPassageiroList":[{"nomePassageiro":"PASSAGEIRO TESTE","tarifa":100,"taxaEmbarque":20,"taxaDU":10}]}},
             "viagens":[{"companhia":{"codigoIata":"LA"},"origem":{"codigoIata":"CGB"},"destino":{"codigoIata":"GRU"},
               "voos":[{"numeroVoo":"1001","origem":{"codigoIata":"CGB"},"destino":{"codigoIata":"GRU"},
                 "ciaMandatoria":{"codigoIata":"LA"},"bagagemQuantidade":0,"bagagemPeso":0,
                 "dataPartida":"2026-11-20T12:00:00Z","dataChegada":"2026-11-20T14:00:00Z"}]}]}]}
            """.formatted(dia);
        server.expect(requestTo("http://fixture/hub/api/aereo/reservar"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.passageiros[0].nascimento").value(dia + "T12:00:00"))
                .andRespond(withSuccess(respostaHub, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://fixture/manager/reservaAereo"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.localizador").value("TESTE1"))
                .andExpect(jsonPath("$.codgSistema.nomeSistema").value("Wooba"))
                .andExpect(jsonPath("$.codgSistema.codgSistema").doesNotExist())
                .andExpect(jsonPath("$.passageiros[0].cpf").value("00123456789"))
                .andExpect(jsonPath("$.passageiros[0].email").value("teste@example.invalid"))
                .andExpect(request -> {
                    JsonNode body = json.readTree(((MockClientHttpRequest) request).getBodyAsString());
                    JsonNode data = body.path("passageiros").get(0).path("dataNascimento");
                    Date dataManager = json.treeToValue(data, Date.class);
                    assertEquals(LocalDate.parse(dia).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant(),
                            dataManager.toInstant(), "A gravação deve preservar a data civil e o horário já usado no Manager");
                    assertEquals(1, body.path("passageiros").get(0).path("reservaValores").size());
                    assertEquals(1, body.path("trechos").get(0).path("voos").size());
                })
                .andRespond(withSuccess("{\"codgReservaAereo\":77,\"localizador\":\"TESTE1\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://fixture/manager/notificacao?tipo=Exclusivo"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        mvc.perform(post("/v2/aereo/reservar").contentType(MediaType.APPLICATION_JSON).content(entrada))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.reservas[0].localizador").value("TESTE1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.reservas[0].passageiros[0].nascimento").value(dia + "T12:00:00"));
        server.verify();
    }
}
