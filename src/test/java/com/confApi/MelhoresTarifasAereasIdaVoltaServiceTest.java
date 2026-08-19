package com.confApi;

import com.confApi.cacheHotel.CombinacaoTarifaAereaIdaVoltaDTO;
import com.confApi.cacheHotel.MelhorTarifaAereaDTO;
import com.confApi.cacheHotel.MelhoresTarifasAereasIdaVoltaClient;
import com.confApi.cacheHotel.MelhoresTarifasAereasIdaVoltaRequest;
import com.confApi.cacheHotel.MelhoresTarifasAereasIdaVoltaResponse;
import com.confApi.cacheHotel.MelhoresTarifasAereasIdaVoltaService;
import com.confApi.chatconfianca.client.ChatConfiancaTokenProvider;
import com.confApi.chatgpt.dto.ChatActionDTO;
import com.confApi.chatgpt.dto.ChatMessageDTO;
import com.confApi.chatgpt.service.ChatService;
import com.confApi.chatgpt.tools.ToolSchemas;
import com.confApi.config.UrlConfig;
import com.confApi.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MelhoresTarifasAereasIdaVoltaServiceTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "outras datas",
            "mostre 5 opcoes",
            "segunda opcao",
            "compare",
            "qual a diferenca?",
            "mes a mes",
            "mesma cia",
            "cias diferentes",
            "geral",
            "e de CGB para BSB?",
            "agora Cuiaba para Brasilia?",
            "e em fevereiro?",
            "na executiva"
    })
    void contextoEstruturadoIdaVoltaMantemRefinamentosNaToolRoundtrip(
            String mensagem) {
        ChatService service = mock(ChatService.class, CALLS_REAL_METHODS);

        assertThat(service.isConsultaMelhorTarifaAereaIdaVolta(
                mensagem,
                List.of(new ChatMessageDTO("assistant", "Resposta sem marcador.")),
                true,
                "ida_volta")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "agora so ida",
            "agora so de ida",
            "somente de ida",
            "apenas a ida",
            "agora sem volta"
    })
    void contextoIdaVoltaRespeitaTrocaExplicitaParaSomenteIda(String mensagem) {
        ChatService service = mock(ChatService.class, CALLS_REAL_METHODS);

        assertThat(service.isConsultaMelhorTarifaAereaIdaVolta(
                mensagem,
                List.of(new ChatMessageDTO("assistant", "Total combinado.")),
                true,
                "ida_volta")).isFalse();
        assertThat(service.isConsultaMelhorTarifaAerea(
                mensagem,
                List.of(new ChatMessageDTO("assistant", "Total combinado.")),
                true)).isTrue();
    }

    @Test
    void comparaCategoriasMostraTrechosEGeraActionsComIdaEVolta() {
        MelhoresTarifasAereasIdaVoltaClient client =
                mock(MelhoresTarifasAereasIdaVoltaClient.class);
        MelhoresTarifasAereasIdaVoltaResponse cache = respostaBase();
        cache.setMelhorCompanhiasDiferentes(combinacao(
                LocalDate.of(2026, 12, 29), LocalDate.of(2027, 1, 26),
                "3003.98", "1921.94", "1082.04", "G3", "LA", "Y"));
        cache.setMelhorMesmaCompanhia(combinacao(
                LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 6),
                "1970.71", "981.29", "989.42", "LA", "LA", "Y"));
        cache.setMelhorGeral(cache.getMelhorMesmaCompanhia());
        when(client.consultar(any())).thenReturn(cache);

        Map<String, Object> resultado = new MelhoresTarifasAereasIdaVoltaService(client)
                .consultar(Map.of("origem", "cgb", "destino", "mco"));

        ArgumentCaptor<MelhoresTarifasAereasIdaVoltaRequest> captor =
                ArgumentCaptor.forClass(MelhoresTarifasAereasIdaVoltaRequest.class);
        verify(client).consultar(captor.capture());
        assertThat(captor.getValue().getOrigem()).isEqualTo("CGB");
        assertThat(captor.getValue().getDestino()).isEqualTo("MCO");
        assertThat(captor.getValue().getDuracaoMinimaDias()).isNull();
        assertThat(captor.getValue().getDuracaoMaximaDias()).isNull();
        assertThat(captor.getValue().getLimiteAlternativas()).isEqualTo(5);

        String mensagem = resultado.get("mensagem").toString();
        assertThat(mensagem)
                .startsWith("\u2708 CGB \u2192 MCO\nIda e volta \u00B7 1 adulto \u00B7 Econ\u00F4mica")
                .contains(
                        "\u2605 MENOR PRE\u00C7O\nMesma companhia \u00B7 LA",
                        "Total: R$", "1.970,71",
                        "\u2197 Ida: 03/11/2026 \u00B7 LA \u00B7 R$", "981,29",
                        "\u2199 Volta: 06/11/2026 \u00B7 LA \u00B7 R$", "989,42",
                        "Perman\u00EAncia: 3 dias",
                        "\u21C4 OUTRA OP\u00C7\u00C3O\nCompanhias diferentes \u00B7 G3 / LA",
                        "3.003,98", "29/12/2026", "26/01/2027", "28 dias",
                        "\u2713 A op\u00E7\u00E3o com a mesma companhia (03/11\u201306/11) custa R$",
                        "1.033,27 menos que a op\u00E7\u00E3o com companhias diferentes (29/12\u201326/01).")
                .doesNotContain("garantida", "<", ">", "**", ";");
        assertThat(mensagem.indexOf("1.970,71"))
                .isLessThan(mensagem.indexOf("3.003,98"));
        assertThat(ocorrencias(mensagem, "Econ\u00F4mica")).isEqualTo(1);
        @SuppressWarnings("unchecked")
        List<ChatActionDTO> actions = (List<ChatActionDTO>) resultado.get("actions");
        List<ChatActionDTO> pesquisas = actions.stream()
                .filter(action -> "pesquisar_voos".equals(action.code()))
                .toList();
        assertThat(pesquisas).hasSize(2);
        assertThat(pesquisas).extracting(ChatActionDTO::label)
                .containsExactly(
                        "Menor pre\u00E7o \u00B7 03/11\u201306/11",
                        "Outra op\u00E7\u00E3o \u00B7 29/12\u201326/01");
        assertThat(pesquisas.get(0).localizador()).contains(
                "dataIda=2026-11-03", "dataVolta=2026-11-06", "cabine=Y");
        assertThat(pesquisas.get(1).localizador()).contains(
                "dataIda=2026-12-29", "dataVolta=2027-01-26", "cabine=Y");
        assertThat(actions).extracting(ChatActionDTO::code)
                .containsExactly(
                        "pesquisar_voos",
                        "pesquisar_voos",
                        "ver_alternativas_tarifas_ida_volta");
        assertThat(actions.get(2).label()).isEqualTo("Ver outras datas");
    }

    @Test
    void cabinesDiferentesSaoMostradasUmaVezEmCadaBloco() {
        MelhoresTarifasAereasIdaVoltaClient client =
                mock(MelhoresTarifasAereasIdaVoltaClient.class);
        MelhoresTarifasAereasIdaVoltaResponse cache = respostaBase();
        cache.setMelhorMesmaCompanhia(combinacao(
                LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 6),
                "1970.71", "981.29", "989.42", "LA", "LA", "Y"));
        cache.setMelhorCompanhiasDiferentes(combinacao(
                LocalDate.of(2026, 12, 29), LocalDate.of(2027, 1, 26),
                "3003.98", "1921.94", "1082.04", "G3", "LA", "C"));
        cache.setMelhorGeral(cache.getMelhorMesmaCompanhia());
        when(client.consultar(any())).thenReturn(cache);

        Map<String, Object> resultado = new MelhoresTarifasAereasIdaVoltaService(client)
                .consultar(Map.of("origem", "CGB", "destino", "MCO"));

        String mensagem = resultado.get("mensagem").toString();
        assertThat(mensagem)
                .startsWith("\u2708 CGB \u2192 MCO\nIda e volta \u00B7 1 adulto\n\n")
                .contains(
                        "Mesma companhia \u00B7 LA \u00B7 Econ\u00F4mica",
                        "Companhias diferentes \u00B7 G3 / LA \u00B7 Executiva");
        assertThat(ocorrencias(mensagem, "Econ\u00F4mica")).isEqualTo(1);
        assertThat(ocorrencias(mensagem, "Executiva")).isEqualTo(1);
    }

    @Test
    void alternativasMantemOrdemVisualLabelsEFiltrosDeCompanhia() {
        MelhoresTarifasAereasIdaVoltaClient client =
                mock(MelhoresTarifasAereasIdaVoltaClient.class);
        MelhoresTarifasAereasIdaVoltaResponse cache = respostaBase();
        CombinacaoTarifaAereaIdaVoltaDTO mesma = combinacao(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5),
                "1800.00", "850.00", "950.00", "LA", "LA", "Y");
        CombinacaoTarifaAereaIdaVoltaDTO diferente = combinacao(
                LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 6),
                "2000.00", "900.00", "1100.00", "G3", "LA", "Y");
        CombinacaoTarifaAereaIdaVoltaDTO alternativaMesma = combinacao(
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 15),
                "2100.00", "1000.00", "1100.00", "AD", "AD", "Y");
        CombinacaoTarifaAereaIdaVoltaDTO alternativaDiferente = combinacao(
                LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 16),
                "2200.00", "1050.00", "1150.00", "G3", "AD", "Y");
        cache.setMelhorMesmaCompanhia(mesma);
        cache.setAlternativasMesmaCompanhia(List.of(alternativaMesma));
        cache.setMelhorCompanhiasDiferentes(diferente);
        cache.setAlternativasCompanhiasDiferentes(List.of(alternativaDiferente));
        cache.setMelhorGeral(mesma);
        when(client.consultar(any())).thenReturn(cache);

        Map<String, Object> resultado = new MelhoresTarifasAereasIdaVoltaService(client)
                .consultar(Map.of(
                        "origem", "CGB",
                        "destino", "MCO",
                        "modoResposta", "alternativas",
                        "limiteAlternativas", 3));

        String mensagem = resultado.get("mensagem").toString();
        assertThat(mensagem)
                .contains(
                        "\u2605 MENOR PRE\u00C7O",
                        "\u21C4 OUTRA OP\u00C7\u00C3O",
                        "\u21C4 OP\u00C7\u00C3O 3",
                        "\u2713 A op\u00E7\u00E3o com a mesma companhia (01/10\u201305/10) custa R$",
                        "200,00 menos que a op\u00E7\u00E3o com companhias diferentes (02/10\u201306/10).")
                .doesNotContain("2.200,00");
        assertThat(mensagem.indexOf("1.800,00"))
                .isLessThan(mensagem.indexOf("2.000,00"));
        assertThat(mensagem.indexOf("2.000,00"))
                .isLessThan(mensagem.indexOf("2.100,00"));

        @SuppressWarnings("unchecked")
        List<ChatActionDTO> actions = (List<ChatActionDTO>) resultado.get("actions");
        assertThat(actions).extracting(ChatActionDTO::label)
                .containsExactly(
                        "Menor pre\u00E7o \u00B7 01/10\u201305/10",
                        "Outra op\u00E7\u00E3o \u00B7 02/10\u201306/10",
                        "Op\u00E7\u00E3o 3 \u00B7 10/10\u201315/10",
                        "Somente mesma companhia",
                        "Companhias diferentes");
        assertThat(actions).extracting(ChatActionDTO::code)
                .containsExactly(
                        "pesquisar_voos",
                        "pesquisar_voos",
                        "pesquisar_voos",
                        "ver_mesma_companhia_tarifas_ida_volta",
                        "ver_companhias_diferentes_tarifas_ida_volta")
                .doesNotContain("comparar_companhias_tarifas_ida_volta");
        assertThat(actions.get(3).prompt()).contains(
                "3 melhores alternativas com a mesma companhia");
        assertThat(actions.get(4).prompt()).contains(
                "3 melhores alternativas com companhias diferentes");
    }

    @Test
    void naoDuplicaPesquisaQuandoCategoriasGeramOMesmoLocalizador() {
        MelhoresTarifasAereasIdaVoltaClient client =
                mock(MelhoresTarifasAereasIdaVoltaClient.class);
        MelhoresTarifasAereasIdaVoltaResponse cache = respostaBase();
        cache.setMelhorCompanhiasDiferentes(combinacao(
                "2000.00", "900.00", "1100.00", "G3", "LA", "Y"));
        cache.setMelhorMesmaCompanhia(combinacao(
                "2200.00", "1000.00", "1200.00", "G3", "G3", "Y"));
        cache.setAlternativasMesmaCompanhia(List.of(combinacao(
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 15),
                "2300.00", "1100.00", "1200.00", "AD", "AD", "Y")));
        cache.setMelhorGeral(cache.getMelhorCompanhiasDiferentes());
        when(client.consultar(any())).thenReturn(cache);

        Map<String, Object> resultado = new MelhoresTarifasAereasIdaVoltaService(client)
                .consultar(Map.of(
                        "origem", "CGB",
                        "destino", "MCO",
                        "modoResposta", "alternativas",
                        "limiteAlternativas", 3));

        @SuppressWarnings("unchecked")
        List<ChatActionDTO> actions = (List<ChatActionDTO>) resultado.get("actions");
        List<ChatActionDTO> pesquisas = actions.stream()
                .filter(action -> "pesquisar_voos".equals(action.code()))
                .toList();
        assertThat(pesquisas).extracting(ChatActionDTO::label)
                .containsExactly(
                        "Menor pre\u00E7o \u00B7 10/09\u201317/09",
                        "Op\u00E7\u00E3o 3 \u00B7 10/10\u201315/10");
        assertThat(actions).extracting(ChatActionDTO::code)
                .containsExactly(
                        "pesquisar_voos",
                        "pesquisar_voos",
                        "ver_mesma_companhia_tarifas_ida_volta",
                        "ver_companhias_diferentes_tarifas_ida_volta");
    }

    @Test
    void politicaMesmaSemCategoriaNaoUsaMelhorGeralDeCompanhiasDiferentes() {
        MelhoresTarifasAereasIdaVoltaClient client =
                mock(MelhoresTarifasAereasIdaVoltaClient.class);
        MelhoresTarifasAereasIdaVoltaResponse cache = respostaBase();
        cache.setMelhorGeral(combinacao(
                "2000.00", "900.00", "1100.00", "G3", "LA", "Y"));
        cache.setMelhorCompanhiasDiferentes(cache.getMelhorGeral());
        when(client.consultar(any())).thenReturn(cache);

        Map<String, Object> resultado = new MelhoresTarifasAereasIdaVoltaService(client)
                .consultar(Map.of(
                        "origem", "CGB",
                        "destino", "MCO",
                        "politicaCompanhia", "mesma"));

        assertThat(resultado.get("mensagem").toString())
                .contains("N\u00E3o encontrei", "mesma companhia")
                .doesNotContain("2.000,00");
        @SuppressWarnings("unchecked")
        List<ChatActionDTO> actions = (List<ChatActionDTO>) resultado.get("actions");
        assertThat(actions).noneMatch(action -> "pesquisar_voos".equals(action.code()));
        assertThat(actions).extracting(ChatActionDTO::code)
                .containsExactly(
                        "ver_alternativas_tarifas_ida_volta",
                        "comparar_companhias_tarifas_ida_volta")
                .doesNotContain(
                        "ver_mesma_companhia_tarifas_ida_volta",
                        "ver_companhias_diferentes_tarifas_ida_volta");
    }

    @Test
    void modoCompararExplicitaCategoriaAusente() {
        MelhoresTarifasAereasIdaVoltaClient client =
                mock(MelhoresTarifasAereasIdaVoltaClient.class);
        MelhoresTarifasAereasIdaVoltaResponse cache = respostaBase();
        cache.setMelhorCompanhiasDiferentes(combinacao(
                "2000.00", "900.00", "1100.00", "G3", "LA", "Y"));
        cache.setMelhorGeral(cache.getMelhorCompanhiasDiferentes());
        when(client.consultar(any())).thenReturn(cache);

        Map<String, Object> resultado = new MelhoresTarifasAereasIdaVoltaService(client)
                .consultar(Map.of("origem", "CGB", "destino", "MCO"));

        assertThat(resultado.get("mensagem").toString())
                .contains("Companhias diferentes", "2.000,00",
                        "N\u00E3o encontrei uma combina\u00E7\u00E3o classificada com a mesma companhia")
                .doesNotContain("N\u00E3o encontrei uma combina\u00E7\u00E3o classificada com companhias diferentes");
    }

    @Test
    void schemaEDetectorPriorizamIdaVoltaEPermitemVoltarASomenteIda() {
        assertThat(ToolSchemas.searchCheapestRoundtripAirfares().name())
                .isEqualTo("search_cheapest_roundtrip_airfares");
        assertThat(ToolSchemas.searchCheapestRoundtripAirfares().jsonSchema().toString())
                .contains("dataIdaInicio", "dataVoltaInicio", "politicaCompanhia",
                        "companhias", "duracaoMinimaDias");
        @SuppressWarnings("unchecked")
        Map<String, Object> propriedades = (Map<String, Object>)
                ToolSchemas.searchCheapestRoundtripAirfares().jsonSchema()
                        .get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> modoResposta = (Map<String, Object>)
                propriedades.get("modoResposta");
        assertThat(modoResposta.get("enum"))
                .isEqualTo(List.of("resumo", "alternativas", "companhias"));
        assertThat(ToolSchemas.searchCheapestAirfares().jsonSchema().toString())
                .contains("cabines", "mensal");
        ChatService service = mock(ChatService.class, CALLS_REAL_METHODS);
        assertThat(service.isConsultaMelhorTarifaAereaIdaVolta(
                "Qual a tarifa mais barata ida e volta de Cuiaba para Orlando?"))
                .isTrue();
        assertThat(service.isConsultaMelhorTarifaAereaIdaVolta(
                "E ida e volta?",
                List.of(new ChatMessageDTO("assistant",
                        "A menor tarifa de CGB para MCO e R$ 900,00.")),
                false)).isTrue();
        assertThat(service.isConsultaMelhorTarifaAereaIdaVolta(
                "Agora so ida",
                List.of(new ChatMessageDTO("assistant",
                        "Para 1 adulto, ida e volta: total combinado de R$ 2.000,00.")),
                true)).isFalse();
        assertThat(service.isConsultaMelhorTarifaAerea(
                "Agora so ida",
                List.of(new ChatMessageDTO("assistant",
                        "Para 1 adulto, ida e volta: total combinado de R$ 2.000,00.")),
                true)).isTrue();
        assertThat(service.isConsultaMelhorTarifaAereaIdaVolta(
                "E em fevereiro?",
                List.of(new ChatMessageDTO("assistant", "Resposta sem marcador.")),
                true,
                "ida_volta")).isTrue();
        assertThat(service.isConsultaMelhorTarifaAereaIdaVolta(
                "E na executiva?",
                List.of(new ChatMessageDTO("assistant", "Resposta sem marcador.")),
                true,
                "ida_volta")).isTrue();
    }

    @Test
    void executivaPermaneceComoFiltroDaConsultaIdaVolta() {
        MelhoresTarifasAereasIdaVoltaClient client =
                mock(MelhoresTarifasAereasIdaVoltaClient.class);
        MelhoresTarifasAereasIdaVoltaResponse cache = respostaBase();
        cache.setStatus("SEM_DADOS_IDA");
        when(client.consultar(any())).thenReturn(cache);

        new MelhoresTarifasAereasIdaVoltaService(client).consultar(Map.of(
                "origem", "CGB",
                "destino", "MCO",
                "cabine", "executiva"));

        ArgumentCaptor<MelhoresTarifasAereasIdaVoltaRequest> captor =
                ArgumentCaptor.forClass(MelhoresTarifasAereasIdaVoltaRequest.class);
        verify(client).consultar(captor.capture());
        assertThat(captor.getValue().getCabine()).isEqualTo("C");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void clientUsaEndpointRoundtripEBearer() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ChatConfiancaTokenProvider tokenProvider = mock(ChatConfiancaTokenProvider.class);
        when(tokenProvider.bearerToken()).thenReturn("token-cache");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(MelhoresTarifasAereasIdaVoltaResponse.class)))
                .thenReturn(ResponseEntity.ok(new MelhoresTarifasAereasIdaVoltaResponse()));
        String anterior = UrlConfig.URL_CONFIANCA_CACHEHOTEL;
        UrlConfig.URL_CONFIANCA_CACHEHOTEL = "http://cache.local/CacheHotelManger/";
        try {
            new MelhoresTarifasAereasIdaVoltaClient(restTemplate, tokenProvider)
                    .consultar(new MelhoresTarifasAereasIdaVoltaRequest());
            ArgumentCaptor<HttpEntity> entity = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).exchange(eq(
                            "http://cache.local/CacheHotelManger/CacheAereo/aereo/melhores-datas/ida-volta"),
                    eq(HttpMethod.POST), entity.capture(),
                    eq(MelhoresTarifasAereasIdaVoltaResponse.class));
            assertThat(entity.getValue().getHeaders().getFirst("Authorization"))
                    .isEqualTo("Bearer token-cache");
        } finally {
            UrlConfig.URL_CONFIANCA_CACHEHOTEL = anterior;
        }
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void clientRoundtripSoPropagaMensagemJsonCurtaEmErroQuatrocentos() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ChatConfiancaTokenProvider tokenProvider = mock(ChatConfiancaTokenProvider.class);
        when(tokenProvider.bearerToken()).thenReturn("token-cache");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(MelhoresTarifasAereasIdaVoltaResponse.class)))
                .thenThrow(erroQuatrocentos("{\"status\":\"ERROR\",\"mensagem\":\"Duracao invalida.\"}"))
                .thenThrow(erroQuatrocentos("stack trace interno"));
        String anterior = UrlConfig.URL_CONFIANCA_CACHEHOTEL;
        UrlConfig.URL_CONFIANCA_CACHEHOTEL = "http://cache.local/CacheHotelManger/";
        try {
            MelhoresTarifasAereasIdaVoltaClient client =
                    new MelhoresTarifasAereasIdaVoltaClient(restTemplate, tokenProvider);
            assertThatThrownBy(() -> client.consultar(
                    new MelhoresTarifasAereasIdaVoltaRequest()))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessage("Duracao invalida.");
            assertThatThrownBy(() -> client.consultar(
                    new MelhoresTarifasAereasIdaVoltaRequest()))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessage("Os criterios da consulta de tarifas aereas de ida e volta sao invalidos.");
        } finally {
            UrlConfig.URL_CONFIANCA_CACHEHOTEL = anterior;
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

    private MelhoresTarifasAereasIdaVoltaResponse respostaBase() {
        MelhoresTarifasAereasIdaVoltaResponse response =
                new MelhoresTarifasAereasIdaVoltaResponse();
        response.setStatus("OK");
        response.setMoeda("BRL");
        MelhoresTarifasAereasIdaVoltaResponse.Periodo ida =
                new MelhoresTarifasAereasIdaVoltaResponse.Periodo();
        ida.setInicio(LocalDate.of(2026, 8, 14));
        ida.setFim(LocalDate.of(2027, 8, 13));
        MelhoresTarifasAereasIdaVoltaResponse.Periodo volta =
                new MelhoresTarifasAereasIdaVoltaResponse.Periodo();
        volta.setInicio(LocalDate.of(2026, 8, 14));
        volta.setFim(LocalDate.of(2027, 8, 13));
        MelhoresTarifasAereasIdaVoltaResponse.Periodos periodos =
                new MelhoresTarifasAereasIdaVoltaResponse.Periodos();
        periodos.setIda(ida);
        periodos.setVolta(volta);
        response.setPeriodos(periodos);
        MelhoresTarifasAereasIdaVoltaResponse.Regras regras =
                new MelhoresTarifasAereasIdaVoltaResponse.Regras();
        regras.setDuracaoMinimaDias(3);
        regras.setDuracaoMaximaDias(30);
        regras.setLimiteAlternativas(5);
        response.setRegras(regras);
        return response;
    }

    private CombinacaoTarifaAereaIdaVoltaDTO combinacao(String total,
                                                         String totalIda,
                                                         String totalVolta,
                                                         String ciaIda,
                                                         String ciaVolta,
                                                         String cabine) {
        return combinacao(
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 17),
                total, totalIda, totalVolta, ciaIda, ciaVolta, cabine);
    }

    private CombinacaoTarifaAereaIdaVoltaDTO combinacao(LocalDate dataIda,
                                                         LocalDate dataVolta,
                                                         String total,
                                                         String totalIda,
                                                         String totalVolta,
                                                         String ciaIda,
                                                         String ciaVolta,
                                                         String cabine) {
        CombinacaoTarifaAereaIdaVoltaDTO item = new CombinacaoTarifaAereaIdaVoltaDTO();
        item.setIda(trecho(dataIda, totalIda, ciaIda, cabine));
        item.setVolta(trecho(dataVolta, totalVolta, ciaVolta, cabine));
        item.setDuracaoDias((int) (dataVolta.toEpochDay() - dataIda.toEpochDay()));
        item.setTotal(new BigDecimal(total));
        item.setMesmaCompanhia(ciaIda.equals(ciaVolta));
        item.setMesmaCabine(true);
        return item;
    }

    private MelhorTarifaAereaDTO trecho(LocalDate data,
                                         String total,
                                         String cia,
                                         String cabine) {
        MelhorTarifaAereaDTO trecho = new MelhorTarifaAereaDTO();
        trecho.setData(data);
        trecho.setTotal(new BigDecimal(total));
        trecho.setMoeda("BRL");
        trecho.setCabine(cabine);
        trecho.setNomeCabine("Economica");
        trecho.setIataCia(cia);
        return trecho;
    }

    private int ocorrencias(String texto, String trecho) {
        int total = 0;
        int indice = 0;
        while ((indice = texto.indexOf(trecho, indice)) >= 0) {
            total++;
            indice += trecho.length();
        }
        return total;
    }
}
