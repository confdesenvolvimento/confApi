package com.confApi.chatconfianca.hotel;
import com.confApi.chatconfianca.v2.*;
import com.confApi.chatconfianca.intencao.ChatConfiancaDecisaoIa;
import com.confApi.chatgpt.dto.*;
import com.confApi.chatgpt.service.ChatService;
import com.confApi.chatgpt.tools.ToolRouter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.confApi.chatconfianca.hotel.ChatIaHotelReservaDocumento.*;

class ChatIaHotelReservaServiceTest {
    ChatIaHotelReservaClient client=mock(ChatIaHotelReservaClient.class);
    ChatIaHotelReservaService service=new ChatIaHotelReservaService(client,true);
    ChatIaHotelDocumento.Configuracao config=ChatIaHotelServiceTest.config();
    ConversationRequestDTO session=new ConversationRequestDTO("confia","CGB","ERP",10L,20L,"Minha reserva de hotel H12345",List.of(),null,false,List.of());
    @BeforeEach void setup()throws Exception{when(client.configuracao()).thenReturn(config);}
    ChatV2Plan plan(){var p=ChatV2Plan.of(ChatV2Capability.HOTEL_RESERVA);p.setAgencia(10);p.setUsuario(20);p.getParametros().put("reservaHotelLocalizador","H12345");return p;}
    Reserva reserva(int id){var r=new Reserva();r.setId(id);r.setAgencia(10);r.setLocalizador("H12345");r.setHotel("Hotel Teste");r.setCidade("Recife");r.setEntrada("2027-01-10");r.setSaida("2027-01-15");r.setDetalhesCompletos(true);r.setHospedes(List.of("Hóspede Fictício"));return r;}
    ChatIaHotelReservaDocumento doc(Reserva... r){var d=new ChatIaHotelReservaDocumento();d.setConfiguracao(config);d.setConsultadoEm("2026-09-11T12:00:00Z");d.setReservas(List.of(r));return d;}
    @Test void abreIdSomenteDaListaValidadaEAgenciaDaSessao()throws Exception {
        var lista=ChatV2Plan.of(ChatV2Capability.HOTEL_LISTA);lista.setAgencia(10);lista.setUsuario(20);var s=new ChatIaHotelListaEstado();s.getOpcoes().put("1",71);lista.setListaHotelEstado(s);
        when(client.consultar(any(),anyInt())).thenReturn(doc(reserva(71)));
        assertEquals("DADOS_CONSULTADOS",service.consultarDaLista(lista,session,71).status());var cap=ArgumentCaptor.forClass(Consulta.class);verify(client).consultar(cap.capture(),eq(8));assertEquals(10,cap.getValue().getAgencia());assertEquals(71,cap.getValue().getSelecionada());assertNull(cap.getValue().getLocalizador());
        assertEquals("CONSULTA_BLOQUEADA",service.consultarDaLista(lista,session,999).status());assertEquals("CONSULTA_BLOQUEADA",service.consultarDaLista(plan(),session,71).status());
    }
    @Test void desabilitadoSemChamadas(){assertEquals("CAPACIDADE_INDISPONIVEL",new ChatIaHotelReservaService(client,false).consultarReserva(plan(),session).status());verifyNoInteractions(client);}
    @Test void sessaoOuAgenciaDiferenteBloqueada(){var p=plan();p.setAgencia(11);assertEquals("CONSULTA_BLOQUEADA",service.consultarReserva(p,session).status());assertEquals("CONSULTA_BLOQUEADA",service.consultarReserva(plan(),null).status());verifyNoInteractions(client);}
    @Test void usuarioDiferenteBloqueado(){var p=plan();p.setUsuario(21);assertEquals("CONSULTA_BLOQUEADA",service.consultarReserva(p,session).status());verifyNoInteractions(client);}
    @Test void configInativaNaoBusca()throws Exception{when(client.configuracao()).thenReturn(null);assertEquals("CONSULTA_BLOQUEADA",service.consultarReserva(plan(),session).status());verify(client,never()).consultar(any(),anyInt());}
    @Test void semFiltrosNaoListaTudoNemUsaLocalizadorAereo()throws Exception{var p=plan();p.getParametros().clear();p.getParametros().put("localizador","FLSEMF");assertEquals("AGUARDANDO_DADOS",service.consultarReserva(p,session).status());verify(client,never()).consultar(any(),anyInt());}
    @Test void hotelPublicoNaoViraFiltroPrivado()throws Exception{var p=plan();p.getParametros().clear();p.getParametros().put("hotelNome","Hotel Teste");assertEquals("AGUARDANDO_DADOS",service.consultarReserva(p,session).status());verify(client,never()).consultar(any(),anyInt());}
    @Test void consultaEnviaSomenteAgenciaDaSessao()throws Exception{when(client.consultar(any(),anyInt())).thenReturn(doc(reserva(78123)));var p=plan();p.getParametros().put("agencia","999");var r=service.consultarReserva(p,session);assertEquals("DADOS_CONSULTADOS",r.status());var cap=ArgumentCaptor.forClass(Consulta.class);verify(client).consultar(cap.capture(),eq(8));assertEquals(10,cap.getValue().getAgencia());assertEquals("H12345",cap.getValue().getLocalizador());assertFalse(r.texto().contains("78123"));assertTrue(r.texto().contains("10/01/2027"));assertTrue(r.texto().contains("Hóspede Fictício"));assertTrue(r.texto().contains("situação atual"));}
    @Test void retornoDeOutraAgenciaNaoVaza()throws Exception{var r=reserva(1);r.setAgencia(11);when(client.consultar(any(),anyInt())).thenReturn(doc(r));var out=service.consultarReserva(plan(),session);assertEquals("ERRO_INTEGRACAO",out.status());assertFalse(out.texto().contains("Hóspede"));assertFalse(out.texto().contains("H12345"));}
    @Test void homonimosNaoExibemHospedes()throws Exception{when(client.consultar(any(),anyInt())).thenReturn(doc(reserva(1),reserva(2)));var p=plan();var out=service.consultarReserva(p,session);assertEquals("AGUARDANDO_DADOS",out.status());assertFalse(out.texto().contains("Fictício"));assertEquals(Map.of("1",1,"2",2),p.getReservaHotelOpcoes());assertNull(p.getReservaHotelSelecionada());}
    @Test void truncadoEmUmPedeEscolha()throws Exception{var d=doc(reserva(1));d.setMaisResultados(true);when(client.consultar(any(),anyInt())).thenReturn(d);assertEquals("AGUARDANDO_DADOS",service.consultarReserva(plan(),session).status());}
    @Test void opcaoUsaIdServidor()throws Exception{var p=plan();p.getReservaHotelOpcoes().put("2",71);p.getParametros().put("reservaHotelOpcao","2");when(client.consultar(any(),anyInt())).thenReturn(doc(reserva(71)));assertEquals("DADOS_CONSULTADOS",service.consultarReserva(p,session).status());var cap=ArgumentCaptor.forClass(Consulta.class);verify(client).consultar(cap.capture(),eq(8));assertEquals(71,cap.getValue().getSelecionada());}
    @Test void opcaoInventadaNaoConsulta()throws Exception{var p=plan();p.getParametros().put("reservaHotelOpcao","5");assertEquals("AGUARDANDO_DADOS",service.consultarReserva(p,session).status());verify(client,never()).consultar(any(),anyInt());}
    @Test void outraReservaNaoSubstituiSelecionada()throws Exception{var p=plan();p.setReservaHotelSelecionada(71);when(client.consultar(any(),anyInt())).thenReturn(doc(reserva(72)));assertEquals("ERRO_INTEGRACAO",service.consultarReserva(p,session).status());}
    @Test void configMudouInvalidaEscolha()throws Exception{var p=plan();p.setReservaHotelConfiguracaoRevisao("antiga");p.getReservaHotelOpcoes().put("1",71);p.getParametros().put("reservaHotelOpcao","1");assertEquals("AGUARDANDO_DADOS",service.consultarReserva(p,session).status());verify(client,never()).consultar(any(),anyInt());}
    @Test void detalheIncompletoOuIdDuplicadoFalha()throws Exception{var r=reserva(1);r.setDetalhesCompletos(false);when(client.consultar(any(),anyInt())).thenReturn(doc(r));assertEquals("ERRO_INTEGRACAO",service.consultarReserva(plan(),session).status());when(client.consultar(any(),anyInt())).thenReturn(doc(r,r));assertEquals("ERRO_INTEGRACAO",service.consultarReserva(plan(),session).status());}
    @Test void ausenciaNaoConfirmaInexistenciaNoFornecedor()throws Exception{when(client.consultar(any(),anyInt())).thenReturn(doc());assertTrue(service.consultarReserva(plan(),session).texto().contains("não sincronizadas"));}
    @Test void executorSemV1ToolsOuSegundaIa()throws Exception{var chat=mock(ChatService.class);var tools=mock(ToolRouter.class);when(client.consultar(any(),anyInt())).thenReturn(doc(reserva(71)));var p=plan();p.getParametros().put("localizador","FLSEMF");new ChatV2Executor(chat,tools,new ObjectMapper(),null,service).executar(p,session,new ChatConfiancaDecisaoIa());assertEquals("DADOS_CONSULTADOS",p.getResultado());assertFalse(p.getParametros().containsKey("localizador"));verifyNoInteractions(chat,tools);}
    @Test void erroNaoExibeExcecaoOuBuscaEmOutraFonte()throws Exception{when(client.consultar(any(),anyInt())).thenThrow(new java.io.IOException("segredo"));var out=service.consultarReserva(plan(),session);assertEquals("ERRO_INTEGRACAO",out.status());assertFalse(out.texto().contains("segredo"));}
}
