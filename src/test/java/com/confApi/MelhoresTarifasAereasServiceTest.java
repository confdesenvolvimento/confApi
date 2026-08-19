package com.confApi;

import com.confApi.cacheHotel.MelhorTarifaAereaDTO;
import com.confApi.cacheHotel.MelhoresTarifasAereasClient;
import com.confApi.cacheHotel.MelhoresTarifasAereasRequest;
import com.confApi.cacheHotel.MelhoresTarifasAereasResponse;
import com.confApi.cacheHotel.MelhoresTarifasAereasService;
import com.confApi.chatconfianca.client.ChatConfiancaTokenProvider;
import com.confApi.chatgpt.dto.ChatActionDTO;
import com.confApi.chatgpt.service.ChatService;
import com.confApi.chatgpt.tools.ToolDefinition;
import com.confApi.chatgpt.tools.ToolSchemas;
import com.confApi.config.UrlConfig;
import com.confApi.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MelhoresTarifasAereasServiceTest {

    @Test
    void consultaMesECabineEDevolveMensagemEmBrlComAcaoDePesquisa() {
        MelhoresTarifasAereasClient client = mock(MelhoresTarifasAereasClient.class);
        MelhoresTarifasAereasResponse cache = new MelhoresTarifasAereasResponse();
        cache.setStatus("OK");
        cache.setMoeda("BRL");
        cache.setPeriodoInicio(LocalDate.of(2027, 2, 1));
        cache.setPeriodoFim(LocalDate.of(2027, 2, 28));
        MelhorTarifaAereaDTO tarifa = tarifa(
                LocalDate.of(2027, 2, 12), "C", "Executiva", "3500.60", "LA");
        cache.setMelhorGeral(tarifa);
        cache.setMelhoresPorCabine(List.of(tarifa));
        cache.setMelhoresPorMesECabine(List.of(tarifa));
        when(client.consultar(org.mockito.ArgumentMatchers.any())).thenReturn(cache);

        MelhoresTarifasAereasService service = new MelhoresTarifasAereasService(client);
        Map<String, Object> resultado = service.consultar(Map.of(
                "origem", "cgb",
                "destino", "mco",
                "mes", "2027-02",
                "cabine", "executiva"));

        ArgumentCaptor<MelhoresTarifasAereasRequest> captor =
                ArgumentCaptor.forClass(MelhoresTarifasAereasRequest.class);
        verify(client).consultar(captor.capture());
        assertThat(captor.getValue().getOrigem()).isEqualTo("CGB");
        assertThat(captor.getValue().getDestino()).isEqualTo("MCO");
        assertThat(captor.getValue().getCabine()).isEqualTo("C");
        assertThat(captor.getValue().getDataInicio()).isEqualTo(LocalDate.of(2027, 2, 1));
        assertThat(captor.getValue().getDataFim()).isEqualTo(LocalDate.of(2027, 2, 28));

        assertThat(resultado.get("mensagem").toString())
                .contains("R$", "3.500,60", "Executiva", "12/02/2027")
                .doesNotContain("histor", "nova pesquisa");
        @SuppressWarnings("unchecked")
        List<ChatActionDTO> actions = (List<ChatActionDTO>) resultado.get("actions");
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).code()).isEqualTo("pesquisar_voos");
        assertThat(actions.get(0).localizador())
                .contains("origem=CGB", "destino=MCO", "dataIda=2027-02-12", "cabine=C");
    }

    @Test
    void semCabineConsultaTodasEDevolveMelhorOpcaoDeCadaCabine() {
        MelhoresTarifasAereasClient client = mock(MelhoresTarifasAereasClient.class);
        MelhoresTarifasAereasResponse cache = new MelhoresTarifasAereasResponse();
        cache.setStatus("OK");
        cache.setMoeda("BRL");
        MelhorTarifaAereaDTO economica = tarifa(
                LocalDate.of(2027, 2, 10), "Y", "Econômica", "900.00", "G3");
        MelhorTarifaAereaDTO premium = tarifa(
                LocalDate.of(2027, 2, 11), "W", "Econômica Premium", "1200.00", "AD");
        MelhorTarifaAereaDTO executiva = tarifa(
                LocalDate.of(2027, 2, 12), "C", "Executiva", "1800.00", "LA");
        MelhorTarifaAereaDTO primeira = tarifa(
                LocalDate.of(2027, 2, 13), "F", "Primeira Classe", "3000.00", "LA");
        cache.setMelhorGeral(economica);
        cache.setMelhoresPorCabine(List.of(economica, premium, executiva, primeira));
        cache.setMelhoresPorMesECabine(List.of(economica, premium, executiva, primeira));
        when(client.consultar(org.mockito.ArgumentMatchers.any())).thenReturn(cache);

        MelhoresTarifasAereasService service = new MelhoresTarifasAereasService(client);
        Map<String, Object> resultado = service.consultar(Map.of(
                "origem", "cgb",
                "destino", "bsb"));

        ArgumentCaptor<MelhoresTarifasAereasRequest> captor =
                ArgumentCaptor.forClass(MelhoresTarifasAereasRequest.class);
        verify(client).consultar(captor.capture());
        assertThat(captor.getValue().getCabine()).isNull();
        assertThat(resultado.get("mensagem").toString())
                .contains("Melhor opção por cabine", "Econômica", "Econômica Premium",
                        "Executiva", "Primeira Classe", "para 1 adulto")
                .doesNotContain("prefere", "informe a cabine");

        @SuppressWarnings("unchecked")
        List<ChatActionDTO> actions = (List<ChatActionDTO>) resultado.get("actions");
        List<ChatActionDTO> pesquisas = actions.stream()
                .filter(action -> "pesquisar_voos".equals(action.code()))
                .toList();
        assertThat(pesquisas).hasSize(4);
        assertThat(pesquisas.stream().map(ChatActionDTO::localizador).toList())
                .anyMatch(url -> url.contains("cabine=Y"))
                .anyMatch(url -> url.contains("cabine=W"))
                .anyMatch(url -> url.contains("cabine=C"))
                .anyMatch(url -> url.contains("cabine=F"));
    }

    @Test
    void modoAlternativasUsaDiasDistintosRespeitaQuantidadeEGeraRefinamentos() {
        MelhoresTarifasAereasClient client = mock(MelhoresTarifasAereasClient.class);
        MelhoresTarifasAereasResponse cache = new MelhoresTarifasAereasResponse();
        cache.setStatus("OK");
        MelhorTarifaAereaDTO dia10 = tarifa(
                LocalDate.of(2027, 1, 10), "Y", "Econômica", "900.00", "G3");
        MelhorTarifaAereaDTO dia12 = tarifa(
                LocalDate.of(2027, 1, 12), "W", "Econômica Premium", "950.00", "AD");
        MelhorTarifaAereaDTO dia11 = tarifa(
                LocalDate.of(2027, 1, 11), "C", "Executiva", "1200.00", "LA");
        MelhorTarifaAereaDTO fevereiro = tarifa(
                LocalDate.of(2027, 2, 3), "Y", "Econômica", "980.00", "G3");
        cache.setMelhorGeral(dia10);
        cache.setMelhoresPorDia(List.of(dia10, dia12, dia11));
        cache.setMelhoresPorCabine(List.of(dia10, dia12, dia11));
        cache.setMelhoresPorMesECabine(List.of(dia10, fevereiro));
        when(client.consultar(org.mockito.ArgumentMatchers.any())).thenReturn(cache);

        Map<String, Object> resultado = new MelhoresTarifasAereasService(client).consultar(
                Map.of(
                        "origem", "CGB",
                        "destino", "BSB",
                        "modoResposta", "alternativas",
                        "limiteAlternativas", 2));

        assertThat(resultado.get("modoResposta")).isEqualTo("alternativas");
        assertThat(resultado.get("quantidadeAplicada")).isEqualTo(2);
        assertThat(resultado.get("mensagem").toString())
                .contains("2 datas mais baratas", "10/01/2027", "12/01/2027",
                        "somente ida")
                .doesNotContain("11/01/2027");

        @SuppressWarnings("unchecked")
        List<ChatActionDTO> actions = (List<ChatActionDTO>) resultado.get("actions");
        assertThat(actions.stream().filter(action -> "pesquisar_voos".equals(action.code())))
                .hasSize(2);
        assertThat(actions).extracting(ChatActionDTO::code)
                .contains("comparar_cabines_tarifas", "ver_tarifas_mensais",
                        "ver_resumo_tarifas")
                .doesNotContain("ver_alternativas_tarifas");
    }

    @Test
    void modoCabinesComparaDiferencaAbsolutaEPercentual() {
        MelhoresTarifasAereasClient client = mock(MelhoresTarifasAereasClient.class);
        MelhoresTarifasAereasResponse cache = new MelhoresTarifasAereasResponse();
        cache.setStatus("OK");
        MelhorTarifaAereaDTO economica = tarifa(
                LocalDate.of(2027, 3, 10), "Y", "Econômica", "1000.00", "G3");
        MelhorTarifaAereaDTO executiva = tarifa(
                LocalDate.of(2027, 3, 11), "C", "Executiva", "1500.00", "LA");
        cache.setMelhorGeral(economica);
        cache.setMelhoresPorCabine(List.of(economica, executiva));
        when(client.consultar(org.mockito.ArgumentMatchers.any())).thenReturn(cache);

        Map<String, Object> resultado = new MelhoresTarifasAereasService(client).consultar(
                Map.of(
                        "origem", "CGB",
                        "destino", "BSB",
                        "modoResposta", "cabines"));

        assertThat(resultado.get("mensagem").toString())
                .contains("Comparação", "Econômica", "Executiva", "500,00", "50%",
                        "somente ida");
    }

    @Test
    void modoMensalSemCabineEscolheAMenorOpcaoDeCadaMes() {
        MelhoresTarifasAereasClient client = mock(MelhoresTarifasAereasClient.class);
        MelhoresTarifasAereasResponse cache = new MelhoresTarifasAereasResponse();
        cache.setStatus("OK");
        MelhorTarifaAereaDTO janeiroEconomica = tarifa(
                LocalDate.of(2027, 1, 9), "Y", "Econômica", "1000.00", "G3");
        MelhorTarifaAereaDTO janeiroExecutiva = tarifa(
                LocalDate.of(2027, 1, 11), "C", "Executiva", "900.00", "LA");
        MelhorTarifaAereaDTO fevereiroEconomica = tarifa(
                LocalDate.of(2027, 2, 8), "Y", "Econômica", "800.00", "AD");
        cache.setMelhorGeral(fevereiroEconomica);
        cache.setMelhoresPorCabine(List.of(fevereiroEconomica, janeiroExecutiva));
        cache.setMelhoresPorMesECabine(List.of(
                janeiroEconomica, janeiroExecutiva, fevereiroEconomica));
        when(client.consultar(org.mockito.ArgumentMatchers.any())).thenReturn(cache);

        Map<String, Object> resultado = new MelhoresTarifasAereasService(client).consultar(
                Map.of(
                        "origem", "CGB",
                        "destino", "BSB",
                        "modoResposta", "mensal"));

        assertThat(resultado.get("mensagem").toString())
                .contains("janeiro/2027", "11/01/2027", "Executiva",
                        "fevereiro/2027", "08/02/2027", "Econômica")
                .doesNotContain("09/01/2027");
    }

    @Test
    void schemaDeMelhoresTarifasNaoExigeDataExata() {
        ToolDefinition tool = ToolSchemas.searchCheapestAirfares();

        assertThat(tool.name()).isEqualTo("search_cheapest_airfares");
        assertThat(tool.jsonSchema().get("required"))
                .isEqualTo(List.of("origem", "destino"));
        assertThat(tool.jsonSchema().toString())
                .contains("modoResposta", "alternativas", "cabines", "mensal");
        assertThat(ToolSchemas.searchFlights().jsonSchema().toString())
                .contains("cabine");
    }

    @Test
    void reconhecePerguntaDeMelhorDiaSemConfundirHotel() {
        ChatService service = mock(ChatService.class, CALLS_REAL_METHODS);

        assertThat(service.isConsultaMelhorTarifaAerea(
                "Qual o dia mais barato de Cuiaba para Orlando na executiva?")).isTrue();
        assertThat(service.isConsultaMelhorTarifaAerea(
                "Qual a tarifa mais barata de Cuiaba para Brasilia?")).isTrue();
        assertThat(service.isConsultaMelhorTarifaAerea(
                "Qual o hotel mais barato de Orlando?")).isFalse();
        assertThat(service.isConsultaMelhorTarifaAerea(
                "Qual o voo mais barato?")).isFalse();
        assertThat(service.isConsultaMelhorTarifaAerea(
                "Menor tarifa CGB para MCO")).isTrue();
        assertThat(service.isConsultaMelhorTarifaAerea(
                "E na executiva?",
                List.of(new com.confApi.chatgpt.dto.ChatMessageDTO(
                        "assistant",
                        "A menor tarifa de CGB para MCO e em 12/02/2027.")))).isTrue();
        assertThat(service.isConsultaMelhorTarifaAerea(
                "E na premium?",
                List.of(new com.confApi.chatgpt.dto.ChatMessageDTO(
                        "assistant",
                        "A menor tarifa de CGB para MCO e em 12/02/2027.")))).isTrue();
        assertThat(service.isConsultaMelhorTarifaAerea(
                "E no proximo mes?",
                List.of(new com.confApi.chatgpt.dto.ChatMessageDTO(
                        "assistant",
                        "A menor tarifa de CGB para MCO e em 12/02/2027.")))).isTrue();
        assertThat(service.isConsultaMelhorTarifaAerea(
                "Mostre as 5 melhores datas",
                List.of(new com.confApi.chatgpt.dto.ChatMessageDTO(
                        "assistant",
                        "A menor tarifa de CGB para MCO e em 12/02/2027.")))).isTrue();
        assertThat(service.isConsultaMelhorTarifaAerea(
                "Quero a segunda opcao",
                List.of(new com.confApi.chatgpt.dto.ChatMessageDTO(
                        "assistant",
                        "A menor tarifa de CGB para MCO e em 12/02/2027.")))).isTrue();
        assertThat(service.isConsultaMelhorTarifaAerea(
                "Compare as cabines",
                List.of(new com.confApi.chatgpt.dto.ChatMessageDTO(
                        "assistant",
                        "A menor tarifa de CGB para MCO e em 12/02/2027.")))).isTrue();
        assertThat(service.isConsultaMelhorTarifaAerea(
                "Pode ser geral",
                List.of(new com.confApi.chatgpt.dto.ChatMessageDTO(
                        "assistant",
                        "A menor tarifa de CGB para MCO e em 12/02/2027.")))).isTrue();

        List<String> respostasPorModo = List.of(
                "A menor tarifa de CGB para MCO e em 12/02/2027.",
                "As 5 datas mais baratas de CGB para MCO sao:",
                "Comparacao das menores tarifas por cabine de CGB para MCO:",
                "Melhor dia de cada mes de CGB para MCO:");
        respostasPorModo.forEach(resposta -> assertThat(
                service.isConsultaMelhorTarifaAerea(
                        "E na executiva?",
                        List.of(new com.confApi.chatgpt.dto.ChatMessageDTO(
                                "assistant", resposta))))
                .as("refinamento apos resposta: %s", resposta)
                .isTrue());

        assertThat(service.isConsultaMelhorTarifaAerea(
                "Compare as opcoes",
                List.of(
                        new com.confApi.chatgpt.dto.ChatMessageDTO(
                                "assistant", "A menor tarifa de CGB para MCO e R$ 900,00."),
                        new com.confApi.chatgpt.dto.ChatMessageDTO(
                                "assistant", "Encontrei os hoteis solicitados."))))
                .isFalse();
        assertThat(service.isConsultaMelhorTarifaAerea(
                "E na executiva?",
                List.of(new com.confApi.chatgpt.dto.ChatMessageDTO(
                        "assistant", "Resposta sem marcador textual.")),
                true)).isTrue();
        assertThat(service.isConsultaMelhorTarifaAerea(
                "Compare os hoteis em Brasilia",
                List.of(new com.confApi.chatgpt.dto.ChatMessageDTO(
                        "assistant", "A menor tarifa de CGB para BSB e R$ 900,00.")),
                true)).isFalse();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void clienteUsaEndpointDoCacheEBearerToken() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ChatConfiancaTokenProvider tokenProvider = mock(ChatConfiancaTokenProvider.class);
        when(tokenProvider.bearerToken()).thenReturn("token-cache");
        when(restTemplate.exchange(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(HttpMethod.POST),
                org.mockito.ArgumentMatchers.any(HttpEntity.class),
                org.mockito.ArgumentMatchers.eq(MelhoresTarifasAereasResponse.class)))
                .thenReturn(ResponseEntity.ok(new MelhoresTarifasAereasResponse()));
        String urlAnterior = UrlConfig.URL_CONFIANCA_CACHEHOTEL;
        UrlConfig.URL_CONFIANCA_CACHEHOTEL = "http://cache.local/CacheHotelManger/";
        try {
            new com.confApi.cacheHotel.MelhoresTarifasAereasClient(
                    restTemplate,
                    tokenProvider).consultar(new MelhoresTarifasAereasRequest());

            ArgumentCaptor<HttpEntity> entity = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).exchange(
                    org.mockito.ArgumentMatchers.eq(
                            "http://cache.local/CacheHotelManger/CacheAereo/aereo/melhores-datas"),
                    org.mockito.ArgumentMatchers.eq(HttpMethod.POST),
                    entity.capture(),
                    org.mockito.ArgumentMatchers.eq(MelhoresTarifasAereasResponse.class));
            assertThat(entity.getValue().getHeaders().getFirst("Authorization"))
                    .isEqualTo("Bearer token-cache");
        } finally {
            UrlConfig.URL_CONFIANCA_CACHEHOTEL = urlAnterior;
        }
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void clienteSoExibeMensagemJsonCurtaEmErroQuatrocentos() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ChatConfiancaTokenProvider tokenProvider = mock(ChatConfiancaTokenProvider.class);
        when(tokenProvider.bearerToken()).thenReturn("token-cache");
        when(restTemplate.exchange(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(HttpMethod.POST),
                org.mockito.ArgumentMatchers.any(HttpEntity.class),
                org.mockito.ArgumentMatchers.eq(MelhoresTarifasAereasResponse.class)))
                .thenThrow(erroQuatrocentos(
                        "{\"mensagem\":\"Consulte no maximo 12 meses por vez.\"}"))
                .thenThrow(erroQuatrocentos("detalhe interno do manager"));

        String urlAnterior = UrlConfig.URL_CONFIANCA_CACHEHOTEL;
        UrlConfig.URL_CONFIANCA_CACHEHOTEL = "http://cache.local/CacheHotelManger/";
        try {
            MelhoresTarifasAereasClient client = new MelhoresTarifasAereasClient(
                    restTemplate, tokenProvider);
            assertThatThrownBy(() -> client.consultar(new MelhoresTarifasAereasRequest()))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessage("Consulte no maximo 12 meses por vez.");
            assertThatThrownBy(() -> client.consultar(new MelhoresTarifasAereasRequest()))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessage("Os criterios da consulta de tarifas aereas sao invalidos.");
        } finally {
            UrlConfig.URL_CONFIANCA_CACHEHOTEL = urlAnterior;
        }
    }

    private HttpClientErrorException erroQuatrocentos(String corpo) {
        return HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                HttpHeaders.EMPTY,
                corpo.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
    }

    private MelhorTarifaAereaDTO tarifa(LocalDate data,
                                         String cabine,
                                         String nomeCabine,
                                         String total,
                                         String iataCia) {
        MelhorTarifaAereaDTO dto = new MelhorTarifaAereaDTO();
        dto.setData(data);
        dto.setMes(data.toString().substring(0, 7));
        dto.setTotal(new BigDecimal(total));
        dto.setMoeda("BRL");
        dto.setCabine(cabine);
        dto.setNomeCabine(nomeCabine);
        dto.setIataCia(iataCia);
        return dto;
    }
}
