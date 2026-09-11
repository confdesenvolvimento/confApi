package com.confApi.chatconfianca.hotel;

import com.confApi.chatconfianca.v2.*;
import com.confApi.chatgpt.dto.*;
import com.confApi.chatgpt.service.ChatService;
import com.confApi.chatconfianca.intencao.ChatConfiancaDecisaoIa;
import com.confApi.chatgpt.tools.ToolRouter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.confApi.chatconfianca.hotel.ChatIaHotelDocumento.*;

class ChatIaHotelServiceTest {
    ChatIaHotelClient client=mock(ChatIaHotelClient.class);
    ChatService chat=mock(ChatService.class);ObjectMapper mapper=new ObjectMapper();
    ChatIaHotelService service=new ChatIaHotelService(client,chat,mapper,true);
    ConversationRequestDTO session=new ConversationRequestDTO("confia","CGB","ERP",10L,20L,"Tem piscina?",List.of(),null,false,List.of());
    Configuracao config;
    @BeforeEach void setup(){config=config();when(client.configuracao()).thenReturn(config);}
    static Configuracao config(){var c=new Configuracao();c.setRevisao("1:0:1");c.setOrientacoes("Responder de forma resumida");c.setRestricoes("Não inventar serviços");c.setLimiteResultados(5);c.setTimeoutSegundos(8);c.setMaxSugestoes(3);c.setQtdInteracoesAtendente(8);c.setPergunta("Qual hotel e cidade?");return c;}
    ChatV2Plan plan(){var p=ChatV2Plan.of(ChatV2Capability.HOTEL_DADOS);p.getParametros().putAll(Map.of("hotelNome","Hotel Teste","hotelCidade","Recife"));return p;}
    Hotel hotel(int id){var h=new Hotel();h.setId(id);h.setNome("Hotel Teste");h.setCidade("Recife");h.setPais("Brasil");h.setEndereco("Rua Teste, 10");h.setDescricao("<b>Hotel cadastrado</b>");return h;}
    ChatIaHotelDocumento doc(Hotel... hotels){var d=new ChatIaHotelDocumento();d.setConfiguracao(config);d.setConsultadoEm("2026-09-10T12:00:00Z");d.setHoteis(List.of(hotels));return d;}
    @Test void desabilitadoNaoChamaManagerNemModelo(){var s=new ChatIaHotelService(client,chat,mapper,false);assertEquals("CAPACIDADE_INDISPONIVEL",s.consultarHotel(plan(),session).status());verifyNoInteractions(client,chat);}
    @Test void configuracaoAusenteBloqueiaConsulta(){when(client.configuracao()).thenReturn(null);assertEquals("CONSULTA_BLOQUEADA",service.consultarHotel(plan(),session).status());verify(client,never()).consultar(any(),any(),any(),any(),anyInt());verifyNoInteractions(chat);}
    @Test void sessaoInvalidaNaoConsulta(){assertEquals("CONSULTA_BLOQUEADA",service.consultarHotel(plan(),null).status());verifyNoInteractions(client,chat);}
    @Test void cidadeAusentePerguntaSemConsultar(){var p=plan();p.getParametros().remove("hotelCidade");var r=service.consultarHotel(p,session);assertEquals("AGUARDANDO_DADOS",r.status());assertEquals("Qual hotel e cidade?",r.texto());verify(client,never()).consultar(any(),any(),any(),any(),anyInt());}
    @Test void hotelAusenteNaoInventaIndisponibilidade(){when(client.consultar(any(),any(),any(),any(),anyInt())).thenReturn(doc());assertTrue(service.consultarHotel(plan(),session).texto().contains("não significa"));verifyNoInteractions(chat);}
    @Test void homonimosPedemEscolhaSemModelo(){var p=plan();when(client.consultar(any(),any(),any(),any(),anyInt())).thenReturn(doc(hotel(11),hotel(12)));var r=service.consultarHotel(p,session);assertEquals("AGUARDANDO_DADOS",r.status());assertEquals(Map.of("1",11,"2",12),p.getHotelOpcoes());assertNull(p.getHotelSelecionado());verifyNoInteractions(chat);}
    @Test void resultadoTruncadoEmUmNaoViraHotelUnico(){var d=doc(hotel(11));d.setMaisResultados(true);when(client.consultar(any(),any(),any(),any(),anyInt())).thenReturn(d);assertEquals("AGUARDANDO_DADOS",service.consultarHotel(plan(),session).status());verifyNoInteractions(chat);}
    @Test void escolhaValidaUsaSomenteIdPersistido(){var p=plan();p.getHotelOpcoes().put("1",11);p.getParametros().put("hotelOpcao","1");when(client.consultar(any(),any(),any(),any(),anyInt())).thenReturn(doc(hotel(11)));assertEquals("DADOS_CONSULTADOS",service.consultarHotel(p,session).status());verify(client).consultar("Hotel Teste","Recife",null,11,8);assertEquals(11,p.getHotelSelecionado());assertFalse(p.getParametros().containsKey("hotelOpcao"));}
    @Test void escolhaInventadaNaoConsulta(){var p=plan();p.getParametros().put("hotelOpcao","3");assertEquals("AGUARDANDO_DADOS",service.consultarHotel(p,session).status());verify(client,never()).consultar(any(),any(),any(),any(),anyInt());}
    @Test void managerNaoPodeTrocarIdSelecionado(){var p=plan();p.setHotelSelecionado(11);when(client.consultar(any(),any(),any(),any(),anyInt())).thenReturn(doc(hotel(12)));assertEquals("ERRO_INTEGRACAO",service.consultarHotel(p,session).status());verifyNoInteractions(chat);}
    @Test void falhaDeIntegracaoNaoConsultaV1(){when(client.consultar(any(),any(),any(),any(),anyInt())).thenThrow(new IllegalStateException("segredo"));var r=service.consultarHotel(plan(),session);assertEquals("ERRO_INTEGRACAO",r.status());assertFalse(r.texto().contains("segredo"));verifyNoInteractions(chat);}
    @Test void perfilAplicadoSemIdsNemHistoricoNoModelo()throws Exception {
        when(client.consultar(any(),any(),any(),any(),anyInt())).thenReturn(doc(hotel(78123)));
        when(chat.responderHotelSomenteTexto(any())).thenReturn(new ChatResponseDTO(null,"O cadastro não informa piscina.",List.of(),null,List.of(),List.of(),List.of()));
        var p=plan();var r=service.consultarHotel(p,session);assertEquals("DADOS_CONSULTADOS",r.status());assertTrue(r.texto().contains("Fonte: cadastro de hotéis"));assertFalse(r.texto().contains("78123"));
        var messages=org.mockito.ArgumentCaptor.forClass(List.class);verify(chat).responderHotelSomenteTexto(messages.capture());
        String prompt=mapper.writeValueAsString(messages.getValue());assertTrue(prompt.contains(config.getOrientacoes()));assertFalse(prompt.contains("78123"));assertFalse(prompt.contains("codgAgencia"));assertEquals("1:0:1",p.getHotelConfiguracaoRevisao());
    }
    @Test void modeloFalhaMantemResumoCadastralSemJson()throws Exception {when(client.consultar(any(),any(),any(),any(),anyInt())).thenReturn(doc(hotel(11)));when(chat.responderHotelSomenteTexto(any())).thenThrow(new java.io.IOException());var r=service.consultarHotel(plan(),session);assertEquals("DADOS_CONSULTADOS",r.status());assertTrue(r.texto().contains("Hotel cadastrado"));assertFalse(r.texto().contains("<b>"));}
    @Test void modeloComJsonOuLinkRejeitado(){assertFalse(ChatIaHotelService.textoValido("{\"hotel\":1}"));assertFalse(ChatIaHotelService.textoValido("Veja https://falso.com"));assertFalse(ChatIaHotelService.textoValido("Fonte: cadastro de conhecimento 8"));assertTrue(ChatIaHotelService.textoValido("Não consta no cadastro."));}
    @Test void ofertaHumanaNaoExecutaHandoff(){var p=plan();p.setHotelInteracoes(8);when(client.consultar(any(),any(),any(),any(),anyInt())).thenReturn(doc(hotel(11)));assertTrue(service.consultarHotel(p,session).texto().contains("Falar com atendente"));}
    @Test void executorIsolaHotelDeAereoEDeTools(){var p=plan();p.getParametros().put("localizador","FLSEMF");var router=mock(ToolRouter.class);when(client.consultar(any(),any(),any(),any(),anyInt())).thenReturn(doc(hotel(11)));new ChatV2Executor(chat,router,mapper,service).executar(p,session,new ChatConfiancaDecisaoIa());assertEquals("DADOS_CONSULTADOS",p.getResultado());assertFalse(p.getParametros().containsKey("localizador"));verifyNoInteractions(router);}
}
