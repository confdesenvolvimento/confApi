package com.confApi.aereo.reemissao;

import com.confApi.aereo.AereoClient;
import com.confApi.aereo.dto.PesquisaResponse;
import com.confApi.chatconfianca.client.ChatConfiancaManagerClient;
import com.confApi.chatconfianca.dto.model.*;
import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.chatconfianca.service.ChatConfiancaService;
import com.confApi.db.confManager.aeroporto.AeroportoService;
import com.confApi.db.confManager.regraAereaAlteracao.RegraAereaAlteracaoManagerService;
import com.confApi.db.confManager.regraAereaAlteracao.dto.*;
import com.confApi.hub.limites.LimitesService;
import com.confApi.exception.RegraDeNegocioException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReemissaoAzulServiceTest {
    final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    final ChatConfiancaManagerClient manager = mock(ChatConfiancaManagerClient.class);
    final ChatConfiancaService usuarios = mock(ChatConfiancaService.class);
    final AereoClient aereo = mock(AereoClient.class);
    final ReemissaoAzulHubClient hub = mock(ReemissaoAzulHubClient.class);
    final RegraAereaAlteracaoManagerService regras = mock(RegraAereaAlteracaoManagerService.class);
    final AeroportoService aeroportos = mock(AeroportoService.class);
    final LimitesService limites = mock(LimitesService.class);
    final String base = "reservaAereo/reemissao-azul";
    final LocalDate ida = LocalDate.now().plusDays(20), volta = ida.plusDays(5);
    ReemissaoAzulService service;
    ReemissaoAzulRegistro salvo;
    ObjectNode reserva;
    PesquisaResponse pesquisa;

    @BeforeEach void setup() throws Exception {
        service = new ReemissaoAzulService(manager, usuarios, aereo, hub, regras, aeroportos, limites, mapper);
        SessaoChatResponse sessao = new SessaoChatResponse(); RefUsuario usuario = new RefUsuario(); usuario.setCodgUsuario(2); sessao.setUsuario(usuario);
        RefAgencia agencia = new RefAgencia(); agencia.setCodgAgencia(1); sessao.setAgencia(agencia); RefUnidade unidade = new RefUnidade(); unidade.setCodgUnidade(3); sessao.setUnidade(unidade);
        when(usuarios.montarSessao(2, 1)).thenReturn(sessao);
        reserva = json("{\"localizador\":\"ABC123\",\"status\":3,\"codgAgencia\":{\"codgAgencia\":1},\"codgSistema\":{\"nomeSistema\":\"Azul\"}}");
        reserva.set("passageiros", json("{\"itens\":[{\"reservaValores\":[{\"valorTarifa\":100,\"valorTaxaEmbarque\":10,\"valorDu\":5}]}]}").get("itens"));
        when(manager.get("reservaAereo/10", JsonNode.class)).thenAnswer(i -> reserva);
        when(manager.get(endsWith("/disponibilidade"), eq(JsonNode.class))).thenReturn(json("{\"disponivel\":true}"));
        when(manager.post(endsWith("/atividades"), any(), eq(JsonNode.class))).thenReturn(json("{\"registrada\":true}"));
        when(manager.post(endsWith("/solicitar"), any(), eq(JsonNode.class))).thenReturn(json("{\"autorizada\":true}"));
        when(manager.post(endsWith("/sincronizar"), any(), eq(JsonNode.class))).thenReturn(json("{\"sincronizada\":true}"));
        when(manager.post(endsWith("/encerrar-sem-efetivacao"), any(), eq(JsonNode.class))).thenReturn(json("{\"encerrada\":true}"));
        when(manager.post(endsWith("/reconciliar"), any(), eq(JsonNode.class))).thenReturn(json("{\"reconciliada\":true}"));
        when(aeroportos.findIatasAeroportosNacionais()).thenReturn(Set.of("VCP", "SDU"));
        RegraAereaAlteracaoConsultaResponse regra = new RegraAereaAlteracaoConsultaResponse(); regra.setStatus("PERMITIDA"); var detalhe = new RegraAereaAlteracaoRegraResponse(); detalhe.setId(7L); detalhe.setPermiteAlteracao(true); regra.setRegra(detalhe); when(regras.consultar(any())).thenReturn(regra);
        when(hub.executar(eq("consultar"), any())).thenReturn(json("""
          {"status":"CONSULTADA","original":{"localizador":"ABC123","valor":100,"moeda":"BRL","trechos":[
          {"indice":0,"chave":"old","origem":"VCP","destino":"SDU","partida":"%sT10:00:00","chegada":"%sT11:00:00","familia":"Y"},
          {"indice":1,"chave":"return","origem":"SDU","destino":"VCP","partida":"%sT10:00:00","chegada":"%sT11:00:00","familia":"Y"}],
          "passageiros":[{"chave":"pax-1","tipo":"ADT","bebeAssociado":false}]}}
          """.formatted(ida, ida, volta, volta)));
        pesquisa = mapper.readValue("""
          {"trechos1":[{"sistema":"Azul","origem":{"codigoIata":"VCP"},"destino":{"codigoIata":"SDU"},"identificacaoDaViagem":"new","voos":[{"numeroVoo":"1234","origem":{"codigoIata":"VCP"},"destino":{"codigoIata":"SDU"},"dataPartida":"%sT10:00:00Z","horaPartida":"10:00","dataChegada":"%sT11:00:00Z","horaChegada":"11:00"}],"familias":[{"identificacaoDeVoo":"fare-new","familia":{"descricaoFamilia":"Mais Azul"},"preco":{"total":125.50}}]}]}
          """.formatted(ida.plusDays(1), ida.plusDays(1)), PesquisaResponse.class);
        when(aereo.pesquisarDisponibilidadeEstrita(any())).thenReturn(List.of(pesquisa));
        when(hub.executar(eq("preparar"), any())).thenReturn(json("{\"status\":\"SERVICOS\",\"servicos\":[],\"expiraEm\":\"" + Instant.now().plusSeconds(600) + "\"}"));
        when(hub.executar(eq("cotar"), any())).thenReturn(json("{\"status\":\"COTADA\",\"valorOriginal\":100,\"valorAdicional\":25.50,\"valorServicos\":0,\"moeda\":\"BRL\"}"));
        when(hub.executar(eq("concluir"), any())).thenReturn(json("{\"status\":\"CONCLUIDA\",\"valorPago\":25.50,\"original\":{\"localizador\":\"ABC123\"}}"));
        when(hub.executar(eq("conferir"), any())).thenAnswer(i -> consultaFornecedor());
    }
    ObjectNode json(String s) { try { return (ObjectNode) mapper.readTree(s); } catch (Exception ex) { throw new RuntimeException(ex); } }
    ObjectNode request() { return json("{\"codgUsuario\":2,\"codgAgenciaSessao\":1,\"reservaId\":10}"); }
    ObjectNode resposta(ObjectNode estado) {
        salvo = new ReemissaoAzulRegistro(); salvo.setId(estado.path("id").asText()); salvo.setStatus(estado.path("status").asText());
        salvo.setVersao(estado.path("versao").asInt()); salvo.setDadosJson(estado.toString()); return estado;
    }
    ObjectNode iniciar() { return resposta(service.iniciar(request())); }
    ObjectNode acao(String nome, ObjectNode pedido) { pedido.put("codgUsuario", 2).put("codgAgenciaSessao", 1).put("versao", salvo.getVersao()); return resposta(service.executar(salvo.getId(), nome, pedido)); }
    void ateSelecao() {
        ObjectNode inicial = iniciar(); acao("aceitar", json("{\"aceite\":true,\"politicaVersao\":\"" + inicial.path("politicaVersao").asText() + "\"}"));
        acao("trechos", json("{\"indices\":[0]}")); acao("pesquisar", json("{\"data\":\"" + ida.plusDays(1) + "\"}")); acao("selecionar", json("{\"opcao\":0,\"familia\":0}"));
    }
    void ateComparativo() {
        ateSelecao();
        acao("confirmar-voos", json("{}")); acao("preparar", json("{\"adicionarServicos\":false}")); acao("cotar", json("{}"));
    }
    @Test void fornecedorWoobaComCompanhiaAzulNaoHabilitaReemissaoDireta() { reserva.with("codgSistema").put("nomeSistema", "Wooba"); assertThrows(RegraDeNegocioException.class, this::iniciar); verifyNoInteractions(hub); }
    @Test void vooDaReservaAtualNaoEOferecidoComoNovaOpcao() {
        pesquisa.getTrechos1().get(0).setIdentificacaoDaViagem("old");
        ObjectNode inicial = iniciar(); acao("aceitar", json("{\"aceite\":true,\"politicaVersao\":\"" + inicial.path("politicaVersao").asText() + "\"}"));
        acao("trechos", json("{\"indices\":[0]}"));
        assertTrue(acao("pesquisar", json("{\"data\":\"" + ida.plusDays(1) + "\"}")).path("opcoes").isEmpty());
        verify(hub, never()).executar(eq("preparar"), any());
    }
    @Test void recusaSemMutacaoVoltaParaEscolhaEContinuaAteComparativo() {
        ateSelecao(); acao("confirmar-voos", json("{}"));
        when(hub.executar(eq("preparar"), any())).thenReturn(json("{\"status\":\"ERRO\",\"requerConferencia\":false,\"falhaRecuperavel\":true,\"mensagem\":\"Escolha outro voo.\"}"));
        ObjectNode rs = acao("preparar", json("{}")); assertEquals("SELECAO", rs.path("status").asText());
        assertTrue(rs.path("falhaRecuperavel").asBoolean()); assertFalse(rs.path("envioFinalRegistrado").asBoolean());
        assertEquals("PESQUISA", acao("voltar", json("{}")).path("status").asText());
        when(hub.executar(eq("preparar"), any())).thenReturn(json("{\"status\":\"SERVICOS\",\"servicos\":[]}"));
        acao("pesquisar", json("{\"data\":\"" + ida.plusDays(1) + "\"}")); acao("selecionar", json("{\"opcao\":0,\"familia\":0}"));
        acao("confirmar-voos", json("{}")); acao("preparar", json("{}"));
        assertEquals("COMPARATIVO", acao("cotar", json("{}")).path("status").asText());
        verify(manager, never()).post(endsWith("/solicitar"), any(), any()); verify(hub, never()).executar(eq("concluir"), any());
    }
    ObjectNode interromperPreparo() {
        ateSelecao(); acao("confirmar-voos", json("{}"));
        when(hub.executar(eq("preparar"), any())).thenReturn(json("""
            {"status":"ERRO","requerConferencia":true,"etapaAzul":"PESQUISAR_VOOS",
            "pagamentoEnviado":false,"pagamentoConfirmado":false,"checkoutEnviado":false,
            "mensagem":"Voo/tarifa selecionado nao esta disponivel na nova pesquisa."}
            """));
        ObjectNode interrompida = acao("preparar", json("{}"));
        assertEquals("CONFERENCIA", interrompida.path("status").asText()); return interrompida;
    }
    @Test void reabrirReservaAposFalhaNoPreparoIniciaNovaPoliticaComOriginalConferida() {
        ObjectNode anterior = interromperPreparo();
        ObjectNode nova = iniciar(); assertEquals("POLITICA", nova.path("status").asText());
        assertNotEquals(anterior.path("id").asText(), nova.path("id").asText());
        assertEquals(anterior.path("id").asText(), nova.path("simulacaoAnteriorId").asText());
        assertTrue(nova.path("reiniciadaSemConfirmacao").asBoolean());
        assertFalse(nova.path("envioFinalRegistrado").asBoolean());
        assertTrue(nova.path("escolhas").isEmpty()); assertTrue(nova.path("selecionados").isEmpty());
        for (String campo : List.of("aceiteEm", "cotacao", "valorAprovado", "dataPesquisa", "servicosFornecedor")) assertFalse(nova.has(campo), campo);
        verify(hub, times(1)).executar(eq("conferir"), any());
        verify(hub, times(1)).executar(eq("preparar"), any());
        verify(hub, never()).executar(eq("concluir"), any());
        verify(manager, never()).post(endsWith("/solicitar"), any(), any());
        verify(manager, never()).post(endsWith("/encerrar-sem-efetivacao"), any(), any());
    }
    @Test void atualizarAndamentoAposFalhaNoPreparoRetomaPoliticaUmaUnicaVez() {
        ObjectNode anterior = interromperPreparo(); String idAnterior = anterior.path("id").asText();
        ObjectNode nova = acao("consultar", json("{}")); String novoId = nova.path("id").asText();
        assertEquals("POLITICA", nova.path("status").asText());
        assertEquals(novoId, iniciar().path("id").asText());
        assertEquals(novoId, acao("consultar", json("{}")).path("id").asText());
        ObjectNode consultaAntiga = request();
        assertEquals(novoId, service.executar(idAnterior, "consultar", consultaAntiga).path("id").asText());
        verify(hub, times(1)).executar(eq("conferir"), any());
        verify(hub, times(2)).executar(eq("consultar"), any());
        verify(hub, times(1)).executar(eq("preparar"), any());
        verify(hub, never()).executar(eq("concluir"), any());
    }
    @Test void falhaDaConsultaAoFornecedorMantemSimulacaoInterrompida() {
        ObjectNode anterior = interromperPreparo();
        when(hub.executar(eq("conferir"), any())).thenThrow(new RuntimeException("indisponivel"));
        ObjectNode estado = iniciar(); assertEquals("CONFERENCIA", estado.path("status").asText());
        assertEquals(anterior.path("id"), estado.path("id")); assertFalse(estado.path("consultaAtualizada").asBoolean());
        verify(hub, times(1)).executar(eq("consultar"), any());
        verify(hub, never()).executar(eq("concluir"), any());
    }
    @Test void saldoPendenteNoFornecedorNaoReiniciaPreparo() {
        ObjectNode anterior = interromperPreparo(); ObjectNode consulta = consultaFornecedor();
        consulta.with("reserva").put("saldoPendente", 10);
        when(hub.executar(eq("conferir"), any())).thenReturn(consulta);
        ObjectNode estado = iniciar(); assertEquals("CONFERENCIA", estado.path("status").asText());
        assertEquals(anterior.path("id"), estado.path("id"));
        verify(hub, times(1)).executar(eq("consultar"), any()); verify(hub, never()).executar(eq("concluir"), any());
    }
    @Test void itinerarioAlteradoNoFornecedorNaoReiniciaPreparo() {
        ObjectNode anterior = interromperPreparo();
        when(hub.executar(eq("conferir"), any())).thenReturn(consultaDepoisDaTroca());
        ObjectNode estado = iniciar(); assertEquals("CONFERENCIA", estado.path("status").asText());
        assertEquals(anterior.path("id"), estado.path("id"));
        verify(hub, times(1)).executar(eq("consultar"), any()); verify(hub, never()).executar(eq("concluir"), any());
    }
    @Test void passageiroAlteradoNoFornecedorNaoReiniciaPreparo() {
        ObjectNode anterior = interromperPreparo(); ObjectNode consulta = consultaFornecedor();
        ((ObjectNode) consulta.path("reserva").path("passageiros").get(0)).put("chave", "outro");
        when(hub.executar(eq("conferir"), any())).thenReturn(consulta);
        ObjectNode estado = iniciar(); assertEquals("CONFERENCIA", estado.path("status").asText());
        assertEquals(anterior.path("id"), estado.path("id")); verify(hub, never()).executar(eq("concluir"), any());
    }
    @Test void indicadorDePagamentoEnviadoImpedeRetomadaDePreparo() {
        ateSelecao(); acao("confirmar-voos", json("{}"));
        when(hub.executar(eq("preparar"), any())).thenReturn(json("""
            {"status":"ERRO","requerConferencia":true,"pagamentoEnviado":true,"pagamentoConfirmado":false,"checkoutEnviado":false}
            """));
        ObjectNode anterior = acao("preparar", json("{}")); ObjectNode estado = iniciar();
        assertEquals("CONFERENCIA", estado.path("status").asText()); assertEquals(anterior.path("id"), estado.path("id"));
        verify(hub, never()).executar(eq("conferir"), any()); verify(hub, never()).executar(eq("concluir"), any());
    }
    @Test void retornoSemIndicadoresFinanceirosNaoAutorizaReinicioDoPreparo() {
        ateSelecao(); acao("confirmar-voos", json("{}"));
        when(hub.executar(eq("preparar"), any())).thenThrow(new RuntimeException("timeout"));
        ObjectNode anterior = acao("preparar", json("{}")); ObjectNode estado = iniciar();
        assertEquals("CONFERENCIA", estado.path("status").asText()); assertEquals(anterior.path("id"), estado.path("id"));
        verify(hub, never()).executar(eq("conferir"), any()); verify(hub, never()).executar(eq("concluir"), any());
    }
    @Test void falhaComResultadoIncertoNaoLiberaOutraPreparacao() {
        ateSelecao(); acao("confirmar-voos", json("{}"));
        when(hub.executar(eq("preparar"), any())).thenReturn(json("{\"status\":\"ERRO\",\"requerConferencia\":true,\"mensagem\":\"Conferir operacao.\"}"));
        assertEquals("CONFERENCIA", acao("preparar", json("{}")).path("status").asText());
        assertThrows(RegraDeNegocioException.class, () -> acao("voltar", json("{}")));
    }
    @Test void pesquisaComSerializacaoIsoDoSpringNaoFalhaAoLerFusoHorario() {
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        ObjectNode inicial = iniciar(); acao("aceitar", json("{\"aceite\":true,\"politicaVersao\":\"" + inicial.path("politicaVersao").asText() + "\"}"));
        acao("trechos", json("{\"indices\":[0]}"));
        ObjectNode rs = acao("pesquisar", json("{\"data\":\"" + ida.plusDays(1) + "\"}"));
        assertEquals("RESULTADOS", rs.path("status").asText()); assertEquals(1, rs.path("opcoes").size());
        JsonNode voo = rs.path("opcoes").get(0).path("voos").get(0);
        assertEquals(ida.plusDays(1) + "T10:00", voo.path("dataPartida").asText());
        assertEquals(ida.plusDays(1) + "T11:00", voo.path("dataChegada").asText());
    }
    @Test void aceitaTimestampIsoComESemFusoEDataComHoraSeparada() {
        for (String texto : List.of("2030-01-29T13:25:00.000+00:00", "2030-01-29T13:25:00.000+0000",
                "2030-01-29T13:25:00Z", "2030-01-29T10:25:00-03:00", "2030-01-29T10:25:00", "2030-01-29")) {
            ObjectNode voo = json("{}").put("dataPartida", texto).put("horaPartida", "10:25");
            assertEquals(LocalDateTime.of(2030, 1, 29, 10, 25), service.dataVoo(voo, true), texto);
        }
        ObjectNode timestamp = json("{}").put("dataPartida", Instant.parse("2030-01-29T13:25:00Z").toEpochMilli()).put("horaPartida", "10:25");
        assertEquals(LocalDateTime.of(2030, 1, 29, 10, 25), service.dataVoo(timestamp, true));
    }
    @Test void serializacaoComFusoPreservaDiaDoVooPertoDaMeiaNoite() {
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var voo = pesquisa.getTrechos1().get(0).getVoos().get(0);
        voo.setDataPartida(Date.from(ida.plusDays(2).atTime(2, 45).toInstant(ZoneOffset.UTC))); voo.setHoraPartida("23:45");
        voo.setDataChegada(Date.from(ida.plusDays(2).atTime(4, 30).toInstant(ZoneOffset.UTC))); voo.setHoraChegada("01:30");
        ObjectNode inicial = iniciar(); acao("aceitar", json("{\"aceite\":true,\"politicaVersao\":\"" + inicial.path("politicaVersao").asText() + "\"}"));
        acao("trechos", json("{\"indices\":[0]}"));
        for (String zona : List.of("UTC", "America/Sao_Paulo", "America/Cuiaba", "Asia/Tokyo")) {
            mapper.setTimeZone(TimeZone.getTimeZone(zona));
            ObjectNode rs = acao("pesquisar", json("{\"data\":\"" + ida.plusDays(1) + "\"}"));
            assertEquals(1, rs.path("opcoes").size(), zona);
            JsonNode normalizado = rs.path("opcoes").get(0).path("voos").get(0);
            assertEquals(ida.plusDays(1) + "T23:45", normalizado.path("dataPartida").asText(), zona);
            assertEquals(ida.plusDays(2) + "T01:30", normalizado.path("dataChegada").asText(), zona);
        }
    }
    @Test void horarioRealmenteAusenteOuInvalidoNaoEInventado() {
        for (ObjectNode voo : List.of(json("{}"), json("{\"dataPartida\":null,\"horaPartida\":\"10:25\"}"),
                json("{\"dataPartida\":\"2030-01-29\"}"), json("{\"dataPartida\":\"2030-01-29T10:25:00\",\"horaPartida\":\"25:00\"}"))) {
            assertThrows(RegraDeNegocioException.class, () -> service.dataVoo(voo, true));
        }
    }
    @Test void disponibilidadeUsaOrigemDestinoDataEPassageirosDoTrechoSelecionado() {
        ObjectNode inicial = iniciar(); acao("aceitar", json("{\"aceite\":true,\"politicaVersao\":\"" + inicial.path("politicaVersao").asText() + "\"}"));
        acao("trechos", json("{\"indices\":[0]}"));
        acao("pesquisar", json("{\"data\":\"" + ida.plusDays(1) + "\",\"origem\":\"GRU\",\"destino\":\"CGH\"}"));
        var captor = org.mockito.ArgumentCaptor.forClass(com.confApi.aereo.dto.PesquisaRequestDTO.class);
        verify(aereo).pesquisarDisponibilidadeEstrita(captor.capture()); var pedido = captor.getValue();
        assertEquals("VCP", pedido.getAeroportoOrigem().getCodigoIata()); assertEquals("SDU", pedido.getAeroportoDestino().getCodigoIata());
        assertEquals(com.confApi.aereo.eNums.TipoPesquisa.ONEWAY, pedido.getTipoPesquisa());
        assertEquals(java.sql.Date.valueOf(ida.plusDays(1)), pedido.getDataIda());
        assertEquals(1, pedido.getQtdADT()); assertEquals(0, pedido.getQtdCHD()); assertEquals(0, pedido.getQtdINF());
    }
    @Test void pesquisaDaVoltaRespeitaOrigemDestinoDaVolta() throws Exception {
        when(aereo.pesquisarDisponibilidadeEstrita(any())).thenReturn(List.of(pesquisaVolta()));
        ObjectNode inicial = iniciar(); acao("aceitar", json("{\"aceite\":true,\"politicaVersao\":\"" + inicial.path("politicaVersao").asText() + "\"}"));
        acao("trechos", json("{\"indices\":[1]}"));
        ObjectNode rs = acao("pesquisar", json("{\"data\":\"" + volta.plusDays(1) + "\"}"));
        assertEquals(1, rs.path("opcoes").size());
        var captor = org.mockito.ArgumentCaptor.forClass(com.confApi.aereo.dto.PesquisaRequestDTO.class);
        verify(aereo).pesquisarDisponibilidadeEstrita(captor.capture());
        assertEquals("SDU", captor.getValue().getAeroportoOrigem().getCodigoIata());
        assertEquals("VCP", captor.getValue().getAeroportoDestino().getCodigoIata());
    }
    @Test void resultadosExcluemOutrasRotasESegmentosIncompativeisPreservandoConexoesValidas() throws Exception {
        ObjectNode lote = mapper.valueToTree(pesquisa); var trechos = (com.fasterxml.jackson.databind.node.ArrayNode) lote.get("trechos1");
        ObjectNode valido = (ObjectNode) trechos.get(0);
        ObjectNode outraRota = valido.deepCopy(); outraRota.with("destino").put("codigoIata", "CGH"); trechos.add(outraRota);
        ObjectNode segmentoErrado = valido.deepCopy(); ((ObjectNode)segmentoErrado.path("voos").get(0)).with("origem").put("codigoIata", "GRU"); trechos.add(segmentoErrado);
        ObjectNode chegadaErrada = valido.deepCopy(); ((ObjectNode)chegadaErrada.path("voos").get(0)).with("destino").put("codigoIata", "CGH"); trechos.add(chegadaErrada);
        ObjectNode conexao = valido.deepCopy(); conexao.put("identificacaoDaViagem", "conexao");
        var segmentos = (com.fasterxml.jackson.databind.node.ArrayNode) conexao.get("voos"); ObjectNode segundo = ((ObjectNode) segmentos.get(0)).deepCopy();
        ((ObjectNode)segmentos.get(0)).with("destino").put("codigoIata", "CNF"); segundo.with("origem").put("codigoIata", "CNF"); segmentos.add(segundo); trechos.add(conexao);
        ObjectNode desconexo = conexao.deepCopy(); ((ObjectNode)desconexo.path("voos").get(1)).with("origem").put("codigoIata", "BSB"); trechos.add(desconexo);
        when(aereo.pesquisarDisponibilidadeEstrita(any())).thenReturn(List.of(mapper.treeToValue(lote, PesquisaResponse.class)));
        ObjectNode inicial = iniciar(); acao("aceitar", json("{\"aceite\":true,\"politicaVersao\":\"" + inicial.path("politicaVersao").asText() + "\"}"));
        acao("trechos", json("{\"indices\":[0]}")); ObjectNode rs = acao("pesquisar", json("{\"data\":\"" + ida.plusDays(1) + "\"}"));
        assertEquals(2, rs.path("opcoes").size()); assertEquals(2, rs.path("opcoes").get(1).path("voos").size());
        assertEquals("conexao", rs.path("opcoes").get(1).path("identificacaoDaViagem").asText());
    }
    @Test void ausenciaDeRegraPermiteContinuarAteCotacaoComAvisoEHistorico() {
        var pendente = new RegraAereaAlteracaoConsultaResponse(); pendente.setStatus("NAO_ENCONTRADO");
        pendente.setMensagem("Nao encontrei uma regra compativel."); when(regras.consultar(any())).thenReturn(pendente);
        ateComparativo(); assertEquals("COMPARATIVO", salvo.getStatus());
        var politica = json(salvo.getDadosJson()).path("politicas").get(0);
        assertEquals("PENDENTE_LINK", politica.path("status").asText()); assertTrue(politica.path("pendenteLink").asBoolean());
        assertFalse(politica.path("regra").has("permiteAlteracao"), "Aviso nao deve inventar uma regra aprovada");
        var atividades = org.mockito.ArgumentCaptor.forClass(ObjectNode.class);
        verify(manager, atLeastOnce()).post(endsWith("/atividades"), atividades.capture(), eq(JsonNode.class));
        assertTrue(atividades.getAllValues().stream().anyMatch(a -> a.path("descricao").asText().contains("link da regra sera disponibilizado")));
        assertFalse(atividades.getAllValues().stream().anyMatch(a -> a.path("descricao").asText().contains("Politica de alteracao aceita")));
        verify(hub, never()).executar(eq("concluir"), any());
    }
    @Test void regraAindaNaoHomologadaTambemExibeAvisoDoLinkPendente() {
        var pendente = new RegraAereaAlteracaoConsultaResponse(); pendente.setStatus("REGRA_NAO_HOMOLOGADA");
        when(regras.consultar(any())).thenReturn(pendente);
        assertTrue(iniciar().path("politicas").get(0).path("pendenteLink").asBoolean());
    }
    @Test void proibicaoExplicitaDaRegraContinuaSendoRespeitada() {
        var proibida = new RegraAereaAlteracaoConsultaResponse(); proibida.setStatus("NAO_PERMITIDA"); proibida.setMensagem("Alteracao nao permitida nesta tarifa.");
        var detalhe = new RegraAereaAlteracaoRegraResponse(); detalhe.setPermiteAlteracao(false); proibida.setRegra(detalhe);
        when(regras.consultar(any())).thenReturn(proibida);
        assertThrows(RegraDeNegocioException.class, this::iniciar); assertNull(salvo);
    }
    @Test void falhaTecnicaDaConsultaNaoViraRegraPendenteDeLink() {
        var erro = new RegraAereaAlteracaoConsultaResponse(); erro.setStatus("ERRO_CONSULTA"); erro.setMensagem("Manager indisponivel.");
        when(regras.consultar(any())).thenReturn(erro);
        assertThrows(RegraDeNegocioException.class, this::iniciar); assertNull(salvo);
    }
    @Test void simulacaoNaoCriaSolicitacaoNemDependeDeTabelaEGravaAtividadesNoHistorico() {
        ateComparativo(); acao("aprovar", json("{\"aceite\":true}"));
        verify(manager, never()).get(contains("persistencia/reemissoes-azul"), any());
        verify(manager, never()).post(endsWith("/solicitar"), any(), any());
        verify(hub, never()).executar(eq("concluir"), any());
        var atividades = org.mockito.ArgumentCaptor.forClass(ObjectNode.class);
        verify(manager, atLeastOnce()).post(endsWith("/atividades"), atividades.capture(), eq(JsonNode.class));
        assertTrue(atividades.getAllValues().stream().anyMatch(a -> "TRECHOS".equals(a.path("status").asText()) && a.path("descricao").asText().contains("aceita")));
        assertTrue(atividades.getAllValues().stream().noneMatch(a -> a.has("dadosJson") || a.has("pagamento")));
    }
    @Test void ultimaEtapaRegistraSolicitacaoAntesDeEnviarAoFornecedor() {
        ateComparativo(); acao("aprovar", json("{\"aceite\":true}"));
        clearInvocations(hub);
        var p = json("{\"pagamento\":{\"formaDePagamento\":2,\"bandeiraCartao\":\"VI\",\"numeroCartao\":\"4111111111111111\",\"codigoDeSeguranca\":\"123\",\"titularNome\":\"TESTE\",\"validade\":\"12/30\",\"parcelas\":1}}");
        assertEquals("CONCLUIDA", acao("concluir", p).path("status").asText());
        assertTrue(json(salvo.getDadosJson()).path("envioFinalRegistrado").asBoolean());
        assertTrue(json(salvo.getDadosJson()).path("confirmadaAzul").asBoolean());
        var ordem = inOrder(manager, hub);
        ordem.verify(manager).post(endsWith("/solicitar"), any(), eq(JsonNode.class));
        ordem.verify(hub).executar(eq("concluir"), any());
        verify(hub, never()).executar(eq("consultar"), any());
    }
    @Test void sessaoPerdidaNaoAutorizaRepeticaoDePagamento() {
        ateComparativo(); acao("aprovar", json("{\"aceite\":true}"));
        var reiniciado = new ReemissaoAzulService(manager, usuarios, aereo, hub, regras, aeroportos, limites, mapper);
        assertThrows(RegraDeNegocioException.class, () -> reiniciado.executar(salvo.getId(), "concluir", request()));
        when(manager.get(endsWith("/disponibilidade"), eq(JsonNode.class))).thenReturn(json("{\"disponivel\":false}"));
        ObjectNode acompanhamento = reiniciado.iniciar(request());
        assertEquals("POLITICA", acompanhamento.path("status").asText()); assertTrue(acompanhamento.path("reiniciadaSemConfirmacao").asBoolean());
        assertEquals("CONFERIDA", acompanhamento.path("conferenciaAnterior").path("status").asText()); assertFalse(acompanhamento.path("confirmadaAzul").asBoolean());
        ObjectNode consulta = request().put("versao", acompanhamento.path("versao").asInt());
        assertThrows(RegraDeNegocioException.class, () -> reiniciado.executar(acompanhamento.path("id").asText(), "concluir", consulta));
        verify(hub, never()).executar(eq("concluir"), any());
    }
    ObjectNode consultaFornecedor() {
        return json("""
          {"status":"CONFERIDA","fonte":"Azul","consultadoEm":"2026-10-29T16:00:00Z","reserva":{"localizador":"ABC123","situacao":"Confirmed","moeda":"BRL","valor":100,"saldoPendente":0,"trechos":[
          {"chave":"old","origin":"VCP","destination":"SDU","departure":"%sT10:00:00","arrival":"%sT11:00:00","familia":"Y","voos":[]},
          {"chave":"return","origin":"SDU","destination":"VCP","departure":"%sT10:00:00","arrival":"%sT11:00:00","familia":"Y","voos":[]}],
          "passageiros":[{"chave":"pax-1","tipo":"ADT","bebeAssociado":false}]}}
          """.formatted(ida, ida, volta, volta));
    }
    String pendenciaAnterior() {
        String operacao = UUID.randomUUID().toString();
        when(manager.get(endsWith("/disponibilidade"), eq(JsonNode.class))).thenReturn(json("{\"disponivel\":false,\"operacoesPendentes\":[\"" + operacao + "\"]}"));
        return operacao;
    }
    ObjectNode confirmacaoFornecedor(String operacao) {
        return json("""
          {"status":"CONCLUIDA","operacaoId":"%s","fonteConfirmacao":"Azul","confirmadaEm":"2026-10-29T16:00:00Z","valorPago":25.50,"original":{"localizador":"ABC123","moeda":"BRL","valor":125.50,"trechos":[
          {"chave":"new","origem":"VCP","destino":"SDU","partida":"%sT10:00:00","chegada":"%sT11:00:00","familia":"Y"},
          {"chave":"return","origem":"SDU","destino":"VCP","partida":"%sT10:00:00","chegada":"%sT11:00:00","familia":"Y"}],
          "passageiros":[{"chave":"pax-1","tipo":"ADT","bebeAssociado":false}]}}
          """.formatted(operacao, ida, ida, volta, volta));
    }
    ObjectNode consultaDepoisDaTroca() {
        ObjectNode consulta = consultaFornecedor(); consulta.with("reserva").put("valor", 125.50);
        ((ObjectNode) consulta.path("reserva").path("trechos").get(0)).put("chave", "new"); return consulta;
    }
    @Test void abreTentativaAnteriorEConsultaAzulAntesDeApresentarQualquerConfirmacao() {
        String operacao = pendenciaAnterior(); ObjectNode estado = iniciar();
        assertEquals("POLITICA", estado.path("status").asText()); assertEquals(operacao, estado.path("tentativasAnteriores").get(0).asText());
        assertEquals("Confirmed", estado.path("conferenciaAnterior").path("reserva").path("situacao").asText()); assertFalse(estado.path("confirmadaAzul").asBoolean());
        var ordem = inOrder(hub); ordem.verify(hub).executar(eq("conferir"), any()); ordem.verify(hub).executar(eq("consultar-resultado"), argThat(p -> operacao.equals(p.path("operacaoId").asText())));
        verify(manager, never()).post(endsWith("/solicitar"), any(), any()); verify(manager, never()).post(endsWith("/sincronizar"), any(), any());
        verify(hub, never()).executar(eq("preparar"), any()); verify(hub, never()).executar(eq("concluir"), any());
    }
    @Test void reinicioAutomaticoExigeNovosAceitesEEscolhasENaoCriaOutraSimulacaoAoAtualizar() {
        String anterior = pendenciaAnterior(); ObjectNode nova = iniciar(); String novoId = nova.path("id").asText();
        assertNotEquals(anterior, novoId); assertEquals("POLITICA", nova.path("status").asText());
        assertTrue(nova.path("reiniciadaSemConfirmacao").asBoolean()); assertFalse(nova.path("envioFinalRegistrado").asBoolean());
        assertTrue(nova.path("selecionados").isEmpty()); assertTrue(nova.path("escolhas").isEmpty());
        for (String campo : List.of("aceiteEm", "valorAprovado", "cotacao", "pagamento", "formaPagamento")) assertFalse(nova.has(campo), campo);
        assertThrows(RegraDeNegocioException.class, () -> acao("concluir", pagamentoVisa()));
        assertEquals(novoId, iniciar().path("id").asText()); verify(hub, times(1)).executar(eq("consultar"), any());
        verify(hub, never()).executar(eq("preparar"), any()); verify(hub, never()).executar(eq("concluir"), any());
    }
    @Test void novaSimulacaoNaoAutorizaRepetirPagamentoComResultadoAnteriorIncerto() {
        pendenciaAnterior(); ateSelecao(); acao("confirmar-voos", json("{}"));
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class, () -> acao("preparar", json("{}")));
        assertTrue(erro.getMessage().contains("data e referencia")); verify(hub, never()).executar(eq("preparar"), any());
        verify(hub, never()).executar(eq("concluir"), any()); verify(manager, never()).post(endsWith("/solicitar"), any(), any());
    }
    String pendenciaComData() {
        String id = UUID.randomUUID().toString(); String envio = Instant.now().minusSeconds(3600).toString();
        ObjectNode pendente = json("{\"disponivel\":false,\"operacoesPendentes\":[\"" + id + "\"],\"tentativasPendentes\":[{\"operacaoId\":\"" + id + "\",\"enviadaEm\":\"" + envio + "\"}]}");
        when(manager.get(endsWith("/disponibilidade"), eq(JsonNode.class))).thenReturn(pendente, pendente, json("{\"disponivel\":true}"));
        ObjectNode consulta = ((ObjectNode) hub.executar("consultar", json("{}"))).deepCopy(); consulta.with("original").put("valor", 115);
        when(hub.executar(eq("consultar"), any())).thenReturn(consulta);
        when(hub.executar(eq("cotar"), any())).thenReturn(json("{\"status\":\"COTADA\",\"valorOriginal\":115,\"valorAdicional\":25.50,\"valorServicos\":0,\"moeda\":\"BRL\"}"));
        when(hub.executar(eq("reconciliar-anterior"), any())).thenReturn(json("{\"status\":\"RECONCILIADA\",\"fonte\":\"Azul\",\"operacaoId\":\"" + id
                + "\",\"localizador\":\"ABC123\",\"semPagamentoAdicionalRegistrado\":true,\"valorReserva\":115,\"saldoPendente\":0,\"quantidadePagamentos\":1,\"tentativaEnviadaEm\":\"" + envio
                + "\",\"pagamentoOriginalEm\":\"" + Instant.now().minusSeconds(7200) + "\",\"consultadoEm\":\"" + Instant.now() + "\"}"));
        return id;
    }
    @Test void reconciliacaoAnteriorAconteceAntesDaCotacaoEPermiteConfirmacaoFinalUmaVez() {
        String anterior = pendenciaComData(); ateComparativo(); acao("aprovar", json("{\"aceite\":true}"));
        assertEquals("CONCLUIDA", acao("concluir", pagamentoVisa()).path("status").asText());
        var contexto = org.mockito.ArgumentCaptor.forClass(ReemissaoAzulRegistro.class);
        verify(manager).post(endsWith("/reconciliar"), contexto.capture(), eq(JsonNode.class)); assertEquals(anterior, contexto.getValue().getId());
        var ordem = inOrder(hub, manager);
        ordem.verify(hub).executar(eq("reconciliar-anterior"), any()); ordem.verify(manager).post(endsWith("/reconciliar"), any(), eq(JsonNode.class));
        ordem.verify(hub).executar(eq("preparar"), any()); ordem.verify(manager).post(endsWith("/solicitar"), any(), eq(JsonNode.class));
        ordem.verify(hub).executar(eq("concluir"), any()); verify(hub, times(1)).executar(eq("concluir"), any());
    }
    @Test void pagamentoAdicionalNaReconciliacaoInterrompeAntesDePrepararNovosTrechos() {
        pendenciaComData(); when(hub.executar(eq("reconciliar-anterior"), any())).thenReturn(json("{\"status\":\"ERRO\",\"mensagem\":\"Ha pagamento adicional registrado na Azul.\"}"));
        ateSelecao(); acao("confirmar-voos", json("{}"));
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class, () -> acao("preparar", json("{}")));
        assertTrue(erro.getMessage().contains("pagamento adicional")); verify(hub, never()).executar(eq("preparar"), any()); verify(hub, never()).executar(eq("concluir"), any());
        verify(manager, never()).post(endsWith("/reconciliar"), any(), any());
    }
    @Test void falhaAoRegistrarReconciliacaoNaoIniciaCotacaoNemNovoPagamento() {
        pendenciaComData(); when(manager.post(endsWith("/reconciliar"), any(), eq(JsonNode.class))).thenReturn(json("{\"reconciliada\":false}"));
        ateSelecao(); acao("confirmar-voos", json("{}")); assertThrows(RegraDeNegocioException.class, () -> acao("preparar", json("{}")));
        verify(hub, never()).executar(eq("preparar"), any()); verify(hub, never()).executar(eq("concluir"), any());
    }
    @Test void consultaIndisponivelMantemAcompanhamentoAbertoSemComprovarReemissao() {
        pendenciaAnterior(); when(hub.executar(eq("conferir"), any())).thenThrow(new RuntimeException("timeout"));
        ObjectNode estado = iniciar(); assertEquals("CONFERENCIA", estado.path("status").asText()); assertTrue(estado.path("somenteAcompanhamento").asBoolean());
        assertFalse(estado.path("consultaAtualizada").asBoolean()); assertFalse(estado.path("confirmadaAzul").asBoolean());
        when(hub.executar(eq("conferir"), any())).thenReturn(consultaFornecedor());
        assertTrue(acao("consultar", json("{}")).path("reiniciadaSemConfirmacao").asBoolean()); verify(hub, never()).executar(eq("concluir"), any());
    }
    @Test void confirmacaoDaOperacaoExigeCorrespondenciaComItinerarioAtualDaAzul() {
        String operacao = pendenciaAnterior(); when(hub.executar(eq("conferir"), any())).thenReturn(consultaDepoisDaTroca());
        when(hub.executar(eq("consultar-resultado"), any())).thenReturn(confirmacaoFornecedor(operacao));
        ObjectNode estado = iniciar(); assertEquals("CONFIRMADA_FORNECEDOR", estado.path("status").asText()); assertTrue(estado.path("confirmadaAzul").asBoolean());
        verify(manager, never()).post(endsWith("/sincronizar"), any(), any()); verify(manager, never()).post(endsWith("/solicitar"), any(), any());
        when(hub.executar(eq("conferir"), any())).thenReturn(consultaFornecedor());
        ObjectNode alterada = acao("consultar", json("{}")); assertEquals("POLITICA", alterada.path("status").asText()); assertFalse(alterada.path("confirmadaAzul").asBoolean());
    }
    @Test void confirmacaoDeOutraReferenciaOuSomenteStatusConfirmedNaoComprovaEstaTentativa() {
        pendenciaAnterior(); when(hub.executar(eq("conferir"), any())).thenReturn(consultaDepoisDaTroca());
        when(hub.executar(eq("consultar-resultado"), any())).thenReturn(confirmacaoFornecedor(UUID.randomUUID().toString()));
        ObjectNode estado = iniciar(); assertFalse(estado.path("confirmadaAzul").asBoolean()); assertEquals("POLITICA", estado.path("status").asText());
    }
    @Test void comprovacaoDeAusenciaDeEfetivacaoEncerraAReferenciaOriginalRecuperada() {
        String operacao = pendenciaAnterior(); when(hub.executar(eq("consultar-resultado"), any())).thenReturn(falhaSemEfetivacao());
        ObjectNode estado = iniciar(); assertEquals("POLITICA", estado.path("status").asText()); assertTrue(estado.path("reiniciadaSemConfirmacao").asBoolean());
        var contexto = org.mockito.ArgumentCaptor.forClass(ReemissaoAzulRegistro.class);
        verify(manager).post(endsWith("/encerrar-sem-efetivacao"), contexto.capture(), eq(JsonNode.class)); assertEquals(operacao, contexto.getValue().getId());
        verify(hub, never()).executar(eq("concluir"), any());
    }
    @Test void confirmacaoAnteriorNaoEApresentadaComoAtualQuandoConsultaFalha() {
        String operacao = pendenciaAnterior(); when(hub.executar(eq("conferir"), any())).thenReturn(consultaDepoisDaTroca());
        when(hub.executar(eq("consultar-resultado"), any())).thenReturn(confirmacaoFornecedor(operacao));
        assertTrue(iniciar().path("confirmadaAzul").asBoolean());
        when(hub.executar(eq("conferir"), any())).thenThrow(new RuntimeException("timeout"));
        ObjectNode atual = acao("consultar", json("{}")); assertFalse(atual.path("confirmadaAzul").asBoolean());
        assertFalse(atual.path("consultaAtualizada").asBoolean()); assertTrue(atual.has("consultaFornecedor"));
        assertEquals("CONFERENCIA", atual.path("status").asText()); verify(hub, never()).executar(eq("concluir"), any());
    }
    @Test void saldoPendenteOuPassageiroDivergenteImpedemConfirmacaoRecuperada() {
        String operacao = pendenciaAnterior(); when(hub.executar(eq("consultar-resultado"), any())).thenReturn(confirmacaoFornecedor(operacao));
        ObjectNode consulta = consultaDepoisDaTroca(); consulta.with("reserva").put("saldoPendente", 10);
        when(hub.executar(eq("conferir"), any())).thenReturn(consulta); assertFalse(iniciar().path("confirmadaAzul").asBoolean());
        consulta = consultaDepoisDaTroca(); ((ObjectNode) consulta.path("reserva").path("passageiros").get(0)).put("chave", "outro-passageiro");
        when(hub.executar(eq("conferir"), any())).thenReturn(consulta); assertFalse(acao("consultar", json("{}")).path("confirmadaAzul").asBoolean());
        verify(manager, never()).post(endsWith("/sincronizar"), any(), any()); verify(hub, never()).executar(eq("concluir"), any());
    }
    @Test void pedidoComVersaoAntigaNaoExecutaOutraTransicao() {
        ObjectNode original = iniciar();
        acao("aceitar", json("{\"aceite\":true,\"politicaVersao\":\"" + original.path("politicaVersao").asText() + "\"}"));
        ObjectNode antigo = request().put("versao", original.path("versao").asInt()).put("aceite", true).put("politicaVersao", original.path("politicaVersao").asText());
        assertThrows(RegraDeNegocioException.class, () -> service.executar(salvo.getId(), "aceitar", antigo));
    }
    @Test void reservaAlteradaNoPreparoImpedeEnvioDaSimulacaoAntiga() {
        ateSelecao(); acao("confirmar-voos", json("{}"));
        when(hub.executar(eq("preparar"), any())).thenReturn(json("{\"status\":\"ERRO\",\"falhaRecuperavel\":true,\"requerConferencia\":false,\"mensagem\":\"A reserva mudou durante a simulacao.\"}"));
        assertEquals("SELECAO", acao("preparar", json("{}")).path("status").asText());
        var enviados = org.mockito.ArgumentCaptor.forClass(JsonNode.class);
        verify(hub).executar(eq("preparar"), enviados.capture());
        assertEquals("old", enviados.getValue().path("originalEsperada").path("trechos").get(0).path("chave").asText());
        assertEquals("pax-1", enviados.getValue().path("originalEsperada").path("passageiros").get(0).path("chave").asText());
        verify(manager, never()).post(endsWith("/solicitar"), any(), any()); verify(hub, never()).executar(eq("concluir"), any());
    }
    @Test void erroNaConclusaoConservaEtapaEConfirmacaoFinanceiraNoHistorico() {
        ateComparativo(); acao("aprovar", json("{\"aceite\":true}"));
        when(hub.executar(eq("concluir"), any())).thenReturn(json("{\"status\":\"ERRO\",\"requerConferencia\":true,\"etapaAzul\":\"CHECKOUT\",\"codigoFalha\":\"HTTP_FORNECEDOR\",\"httpStatusAzul\":400,\"pagamentoEnviado\":true,\"pagamentoConfirmado\":true,\"checkoutEnviado\":true,\"mensagem\":\"Nao foi possivel concluir a etapa checkout na Azul (HTTP 400). Confira a reserva e o pagamento antes de tentar novamente.\"}"));
        ObjectNode resultado = acao("concluir", json("{\"pagamento\":{\"formaDePagamento\":2,\"bandeiraCartao\":\"VI\",\"numeroCartao\":\"4111111111111111\",\"codigoDeSeguranca\":\"123\",\"titularNome\":\"TESTE\",\"validade\":\"12/30\",\"parcelas\":1}}"));
        assertEquals("CONFERENCIA", resultado.path("status").asText()); assertEquals("CHECKOUT", resultado.path("etapaAzul").asText());
        assertTrue(resultado.path("pagamentoConfirmado").asBoolean()); assertFalse(resultado.path("confirmadaAzul").asBoolean());
        var atividades = org.mockito.ArgumentCaptor.forClass(ObjectNode.class);
        verify(manager, atLeastOnce()).post(endsWith("/atividades"), atividades.capture(), eq(JsonNode.class));
        assertTrue(atividades.getAllValues().stream().anyMatch(a -> "CONFERENCIA".equals(a.path("status").asText()) && a.path("descricao").asText().contains("checkout") && a.path("descricao").asText().contains("HTTP 400")));
        assertFalse(salvo.getDadosJson().contains("4111111111111111")); assertFalse(salvo.getDadosJson().contains("codigoDeSeguranca"));
    }
    ObjectNode falhaSemEfetivacao() {
        return json("{\"status\":\"ERRO\",\"semEfetivacaoConfirmada\":true,\"reservaOriginalConferida\":true,\"requerConferencia\":false,\"pagamentoEnviado\":false,\"pagamentoConfirmado\":false,\"checkoutEnviado\":false,\"mensagem\":\"Falha no recalculo. A reemissao nao foi efetivada.\"}");
    }
    ObjectNode pagamentoVisa() {
        return json("{\"pagamento\":{\"formaDePagamento\":2,\"bandeiraCartao\":\"VI\",\"numeroCartao\":\"4111111111111111\",\"codigoDeSeguranca\":\"123\",\"titularNome\":\"TESTE\",\"validade\":\"12/30\",\"parcelas\":1}}");
    }
    @Test void execucaoSemEfetivacaoEEncerradaELiberaNovaSimulacaoSemOutroPagamento() {
        ateComparativo(); acao("aprovar", json("{\"aceite\":true}")); String anterior = salvo.getId();
        when(hub.executar(eq("concluir"), any())).thenReturn(falhaSemEfetivacao());
        ObjectNode resultado = acao("concluir", pagamentoVisa()); assertEquals("NAO_CONCLUIDA", resultado.path("status").asText());
        assertFalse(resultado.path("confirmadaAzul").asBoolean());
        verify(manager).post(endsWith("/encerrar-sem-efetivacao"), any(), eq(JsonNode.class));
        verify(manager, never()).post(endsWith("/sincronizar"), any(), any());
        assertEquals("POLITICA", iniciar().path("status").asText()); assertNotEquals(anterior, salvo.getId());
        verify(hub, times(1)).executar(eq("concluir"), any());
    }
    @Test void resultadoIncompletoOuContraditorioNaoLiberaBloqueioDoHistorico() {
        ateComparativo(); acao("aprovar", json("{\"aceite\":true}"));
        ObjectNode contraditorio = falhaSemEfetivacao().put("pagamentoEnviado", true);
        when(hub.executar(eq("concluir"), any())).thenReturn(contraditorio);
        assertEquals("CONFERENCIA", acao("concluir", pagamentoVisa()).path("status").asText());
        verify(manager, never()).post(endsWith("/encerrar-sem-efetivacao"), any(), any());
    }
    @Test void encerramentoLocalPendenteEConsultadoSemRepetirPagamento() {
        ateComparativo(); acao("aprovar", json("{\"aceite\":true}"));
        when(hub.executar(eq("concluir"), any())).thenReturn(falhaSemEfetivacao());
        when(manager.post(endsWith("/encerrar-sem-efetivacao"), any(), eq(JsonNode.class))).thenReturn(json("{\"encerrada\":false}"));
        assertEquals("CONFERENCIA", acao("concluir", pagamentoVisa()).path("status").asText());
        when(hub.executar(eq("consultar-resultado"), any())).thenReturn(falhaSemEfetivacao());
        when(manager.post(endsWith("/encerrar-sem-efetivacao"), any(), eq(JsonNode.class))).thenReturn(json("{\"encerrada\":true}"));
        assertEquals("POLITICA", acao("consultar", json("{}")).path("status").asText());
        verify(hub, times(1)).executar(eq("concluir"), any());
    }
    @Test void atualizarAndamentoRecuperaConfirmacaoSemReenviarConclusaoFinanceira() {
        ateComparativo(); acao("aprovar", json("{\"aceite\":true}"));
        when(hub.executar(eq("concluir"), any())).thenThrow(new RuntimeException("timeout"));
        ObjectNode pagamento = json("{\"pagamento\":{\"formaDePagamento\":2,\"bandeiraCartao\":\"VI\",\"numeroCartao\":\"4111111111111111\",\"codigoDeSeguranca\":\"123\",\"titularNome\":\"TESTE\",\"validade\":\"12/30\",\"parcelas\":1}}");
        assertEquals("CONFERENCIA", acao("concluir", pagamento).path("status").asText());
        when(hub.executar(eq("consultar-resultado"), any())).thenReturn(json("{\"status\":\"CONCLUIDA\",\"valorPago\":25.50,\"original\":{\"localizador\":\"ABC123\"}}"));
        ObjectNode resultado = acao("consultar", json("{}")); assertEquals("CONCLUIDA", resultado.path("status").asText());
        assertTrue(resultado.path("confirmadaAzul").asBoolean()); verify(hub, times(1)).executar(eq("concluir"), any());
        verify(manager, times(1)).post(endsWith("/solicitar"), any(), eq(JsonNode.class));
        verify(manager, times(1)).post(endsWith("/sincronizar"), any(), eq(JsonNode.class));
    }
    @Test void andamentoSemConfirmacaoDoFornecedorNaoSincronizaNemAutorizaNovaCobranca() {
        ateComparativo(); acao("aprovar", json("{\"aceite\":true}"));
        when(hub.executar(eq("concluir"), any())).thenReturn(json("{\"status\":\"ERRO\",\"mensagem\":\"Conferir pagamento.\"}"));
        ObjectNode pagamento = json("{\"pagamento\":{\"formaDePagamento\":2,\"bandeiraCartao\":\"VI\",\"numeroCartao\":\"4111111111111111\",\"codigoDeSeguranca\":\"123\",\"titularNome\":\"TESTE\",\"validade\":\"12/30\",\"parcelas\":1}}");
        acao("concluir", pagamento);
        when(hub.executar(eq("consultar-resultado"), any())).thenReturn(json("{\"status\":\"ERRO\",\"mensagem\":\"Reserva nao confirmada.\"}"));
        assertEquals("POLITICA", acao("consultar", json("{}")).path("status").asText());
        assertThrows(RegraDeNegocioException.class, () -> acao("concluir", pagamento));
        verify(hub, times(1)).executar(eq("concluir"), any()); verify(manager, never()).post(endsWith("/sincronizar"), any(), any());
    }
    @Test void outraMoedaNaoEGravadaComoRecebimentoEmReais() { ObjectNode consulta = (ObjectNode) hub.executar("consultar", json("{}")); consulta.with("original").put("moeda", "USD"); assertThrows(RegraDeNegocioException.class, this::iniciar); assertNull(salvo); }
    @Test void reservaDeOutraAgenciaNaoPodeSerConsultada() { reserva.with("codgAgencia").put("codgAgencia", 99); assertThrows(RegraDeNegocioException.class, this::iniciar); verifyNoInteractions(hub); }
    @Test void exigeAceiteDaVersaoExibidaDaPolitica() { iniciar(); assertThrows(RegraDeNegocioException.class, () -> acao("aceitar", json("{\"aceite\":true,\"politicaVersao\":\"obsoleta\"}"))); assertEquals("POLITICA", salvo.getStatus()); }
    @Test void naoPermitePularDoAceiteAoPagamento() { iniciar(); assertThrows(RegraDeNegocioException.class, () -> acao("concluir", json("{}"))); verify(hub, never()).executar(eq("concluir"), any()); }
    @Test void valorAprovadoVemDaCotacaoPersistidaENaoDoBrowser() { ateComparativo(); ObjectNode rs = acao("aprovar", json("{\"aceite\":true,\"valorAprovado\":0.01}")); assertEquals(0, new java.math.BigDecimal("25.50").compareTo(rs.path("valorAprovado").decimalValue())); }
    @Test void comparativoUtilizaSomenteValoresAtuaisDeReservaValor() {
        ateComparativo(); ObjectNode dados = json(salvo.getDadosJson()); assertEquals(115, dados.path("cotacao").path("valorOriginal").asInt()); assertEquals(100, dados.path("cotacao").path("valorOriginalFornecedor").asInt()); assertEquals(0, new java.math.BigDecimal("25.50").compareTo(dados.path("cotacao").path("valorAdicional").decimalValue()));
    }
    @Test void reservaSemValoresLocaisNaoIniciaOperacao() { reserva.remove("passageiros"); assertThrows(RegraDeNegocioException.class, this::iniciar); assertNull(salvo); }
    @Test void alteracaoNosValoresLocaisAposAceiteImpedePagamentoAoFornecedor() {
        ateComparativo(); acao("aprovar", json("{\"aceite\":true}"));
        ((ObjectNode)reserva.path("passageiros").get(0).path("reservaValores").get(0)).put("valorTarifa", 999);
        assertThrows(RegraDeNegocioException.class, () -> acao("concluir", json("{\"pagamento\":{\"formaDePagamento\":1}}")));
        verify(hub, never()).executar(eq("concluir"), any()); assertEquals("PAGAMENTO", salvo.getStatus());
    }
    @Test void timeoutFinanceiroBloqueiaOutraTentativa() { ateComparativo(); acao("aprovar", json("{\"aceite\":true}")); when(hub.executar(eq("concluir"), any())).thenThrow(new RuntimeException("timeout")); ObjectNode p = json("{\"pagamento\":{\"formaDePagamento\":2,\"bandeiraCartao\":\"VI\",\"numeroCartao\":\"4111111111111111\",\"codigoDeSeguranca\":\"123\",\"titularNome\":\"TESTE\",\"validade\":\"12/30\",\"parcelas\":1}}"); assertEquals("CONFERENCIA", acao("concluir", p).path("status").asText()); assertThrows(RegraDeNegocioException.class, () -> acao("concluir", p)); verify(hub, times(1)).executar(eq("concluir"), any()); assertFalse(salvo.getDadosJson().contains("numeroCartao")); assertFalse(salvo.getDadosJson().contains("codigoDeSeguranca")); }
    @Test void respostaIncompletaNaoViraCotacaoZero() { when(hub.executar(eq("cotar"), any())).thenReturn(json("{\"status\":\"COTADA\"}")); ateComparativo(); assertEquals("CONFERENCIA", salvo.getStatus()); }
    @Test void moedaDivergenteNaCotacaoBloqueiaPagamento() { when(hub.executar(eq("cotar"), any())).thenReturn(json("{\"status\":\"COTADA\",\"valorOriginal\":100,\"valorAdicional\":25.50,\"moeda\":\"USD\"}")); ateComparativo(); assertEquals("CONFERENCIA", salvo.getStatus()); }
    @Test void dtoDePagamentoNaoExpoeDadosNoLog() { ReemissaoAzulRequest p = new ReemissaoAzulRequest(); p.setPagamento(Map.of("numeroCartao", "4111111111111111", "codigoDeSeguranca", "123")); assertFalse(p.toString().contains("411111")); assertFalse(p.toString().contains("123")); }
    @Test void sincronizacaoPendenteNaoRepetePagamento() {
        ateComparativo(); acao("aprovar", json("{\"aceite\":true}")); when(manager.post(endsWith("/sincronizar"), any(), eq(JsonNode.class))).thenThrow(new RuntimeException("Manager indisponivel"));
        ObjectNode p = json("{\"pagamento\":{\"formaDePagamento\":2,\"bandeiraCartao\":\"VI\",\"numeroCartao\":\"4111111111111111\",\"codigoDeSeguranca\":\"123\",\"titularNome\":\"TESTE\",\"validade\":\"12/30\",\"parcelas\":1}}");
        assertEquals("SINCRONIZANDO", acao("concluir", p).path("status").asText());
        when(manager.post(endsWith("/sincronizar"), any(), eq(JsonNode.class))).thenReturn(json("{\"sincronizada\":true}"));
        assertEquals("CONCLUIDA", acao("consultar", json("{}" )).path("status").asText()); verify(hub, times(1)).executar(eq("concluir"), any());
    }
    @Test void confApiNormalizaDatasAntesDeEnviarConfirmacaoAoManager() throws Exception {
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var segmento = pesquisa.getTrechos1().get(0).getVoos().get(0);
        segmento.setDataPartida(Date.from(Instant.parse(ida.plusDays(1) + "T05:00:00Z"))); segmento.setHoraPartida("02:00:00");
        segmento.setDataChegada(Date.from(Instant.parse(ida.plusDays(1) + "T08:10:00Z"))); segmento.setHoraChegada("05:10:00");
        ateComparativo(); acao("aprovar", json("{\"aceite\":true}"));
        assertEquals("CONCLUIDA", acao("concluir", pagamentoVisa()).path("status").asText());
        var c = org.mockito.ArgumentCaptor.forClass(ReemissaoAzulRegistro.class);
        verify(manager).post(endsWith("/sincronizar"), c.capture(), eq(JsonNode.class));
        JsonNode normalizado = mapper.readTree(c.getValue().getDadosJson()).path("escolhas").get(0).path("voo").path("voos").get(0);
        assertEquals(ida.plusDays(1) + "T02:00", normalizado.path("dataPartida").asText());
        assertEquals(ida.plusDays(1) + "T05:10", normalizado.path("dataChegada").asText());
        verify(hub, times(1)).executar(eq("concluir"), any());
    }
    PesquisaResponse pesquisaVolta() throws Exception {
        ObjectNode node = mapper.valueToTree(pesquisa); ObjectNode trecho = (ObjectNode) node.path("trechos1").get(0);
        trecho.with("origem").put("codigoIata", "SDU"); trecho.with("destino").put("codigoIata", "VCP"); trecho.put("identificacaoDaViagem", "new-return");
        ObjectNode voo = (ObjectNode) trecho.path("voos").get(0); voo.with("origem").put("codigoIata", "SDU"); voo.with("destino").put("codigoIata", "VCP"); voo.put("dataPartida", volta.plusDays(1) + "T10:00:00Z"); voo.put("dataChegada", volta.plusDays(1) + "T11:00:00Z");
        return mapper.treeToValue(node, PesquisaResponse.class);
    }
    @Test void idaEVoltaSelecionaCadaDataEEnviaUmaUnicaTrocaConjunta() throws Exception {
        when(aereo.pesquisarDisponibilidadeEstrita(any())).thenReturn(List.of(pesquisa), List.of(pesquisaVolta()));
        ObjectNode inicial = iniciar(); acao("aceitar", json("{\"aceite\":true,\"politicaVersao\":\"" + inicial.path("politicaVersao").asText() + "\"}")); acao("trechos", json("{\"indices\":[0,1]}"));
        acao("pesquisar", json("{\"data\":\"" + ida.plusDays(1) + "\"}")); assertEquals("PESQUISA", acao("selecionar", json("{\"opcao\":0,\"familia\":0}")).path("status").asText());
        acao("pesquisar", json("{\"data\":\"" + volta.plusDays(1) + "\"}")); ObjectNode rs = acao("selecionar", json("{\"opcao\":0,\"familia\":0}")); assertEquals("SELECAO", rs.path("status").asText()); assertEquals(2, rs.path("escolhas").size());
        acao("confirmar-voos", json("{}")); acao("preparar", json("{}"));
        var captor = org.mockito.ArgumentCaptor.forClass(JsonNode.class); verify(hub).executar(eq("preparar"), captor.capture()); assertEquals(2, captor.getValue().path("pedido").path("novosTrechos").size()); assertEquals(2, captor.getValue().path("pedido").path("trechosRemover").size());
    }
    @Test void permiteTrocarSomenteVoltaDepoisDeIdaUtilizada() throws Exception {
        ObjectNode consulta = (ObjectNode) hub.executar("consultar", json("{}")); ObjectNode trecho = (ObjectNode) consulta.path("original").path("trechos").get(0); trecho.put("partida", LocalDate.now().minusDays(2) + "T10:00:00"); trecho.put("chegada", LocalDate.now().minusDays(2) + "T11:00:00");
        when(aereo.pesquisarDisponibilidadeEstrita(any())).thenReturn(List.of(pesquisaVolta()));
        ObjectNode inicial = iniciar(); assertFalse(inicial.path("original").path("trechos").get(0).path("elegivel").asBoolean());
        acao("aceitar", json("{\"aceite\":true,\"politicaVersao\":\"" + inicial.path("politicaVersao").asText() + "\"}")); acao("trechos", json("{\"indices\":[1]}")); acao("pesquisar", json("{\"data\":\"" + volta.plusDays(1) + "\"}"));
        assertEquals("SELECAO", acao("selecionar", json("{\"opcao\":0,\"familia\":0}")).path("status").asText());
    }
}
