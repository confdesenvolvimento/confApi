package com.confApi.chatconfianca.hotel;
import com.confApi.chatconfianca.v2.*;
import com.confApi.chatconfianca.dto.model.Conversa;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatIaHotelListaContextoTest {
    ObjectMapper mapper=new ObjectMapper();
    ChatV2Plan anterior(){var p=ChatV2Plan.of(ChatV2Capability.HOTEL_LISTA);p.setAgencia(10);p.setUsuario(20);p.setAtualizadoEm(System.currentTimeMillis());p.getParametros().putAll(Map.of("listaHotelTipoData","CRIACAO","listaHotelInicio","2026-08-16","listaHotelFim","2026-09-14"));var s=new ChatIaHotelListaEstado();s.setFiltros(ChatIaHotelListaContexto.filtros(p));s.getOpcoes().putAll(Map.of("1",71,"2",72));s.setPagina(2);s.setRevisao("1");s.setTemMais(true);s.setProximoId(72);s.setProximaData("2026-09-10 10:00:00");p.setListaHotelEstado(s);return p;}
    ChatV2Plan planejar(String texto,ChatV2Plan a,int agencia,int usuario,ChatV2Plan semantico)throws Exception{var prop=new ChatV2Properties();prop.setEnabled(true);prop.setTrafficPercent(100);prop.setSemanticEnabled(semantico!=null);var c=new Conversa();c.setMetadadosJson(mapper.writeValueAsString(Map.of("confiaV2",a)));var s=mock(ChatV2SemanticClient.class);if(semantico!=null)when(s.decidir(anyString(),any(),any())).thenReturn(semantico);return new ChatV2Planner(prop,s,mapper).planejar(texto,c,List.of(),agencia,usuario);}
    @Test void comandosSemModeloNaoInventamIds()throws Exception{for(String t:List.of("Abra a segunda","2","opção 2","mostre a segunda")){var p=planejar(t,anterior(),10,20,null);assertEquals(ChatV2Capability.HOTEL_LISTA,p.capability());assertEquals("2",p.getParametros().get("listaHotelOpcao"));assertEquals(72,p.getListaHotelEstado().getOpcoes().get("2"));}assertEquals("MAIS",planejar("Mostre mais",anterior(),10,20,null).getParametros().get("listaHotelComando"));}
    @Test void alterouFiltroReiniciaSelecao(){var a=anterior();var p=ChatIaHotelListaContexto.comando("mostre mais",a);p.getParametros().put("listaHotelCidade","Recife");ChatIaHotelListaContexto.restaurar(p,a);assertNull(p.getListaHotelEstado());}
    @Test void estadoRestauradoNaoCompartilhaMapas(){var a=anterior();var p=ChatIaHotelListaContexto.comando("1",a);ChatIaHotelListaContexto.restaurar(p,a);p.getListaHotelEstado().getOpcoes().clear();assertFalse(a.getListaHotelEstado().getOpcoes().isEmpty());}
    @Test void outraAgenciaUsuarioOuContextoExpiradoNaoUsaPagina()throws Exception{for(int[] ids:List.of(new int[]{11,20},new int[]{10,21})){assertNull(planejar("2",anterior(),ids[0],ids[1],null).getListaHotelEstado());}var a=anterior();a.setAtualizadoEm(1);assertNull(planejar("2",a,10,20,null).getListaHotelEstado());}
    @Test void novoAssuntoLimpaLista()throws Exception{var p=planejar("meus boletos",anterior(),10,20,null);assertEquals(ChatV2Capability.BOLETOS,p.capability());assertNull(p.getListaHotelEstado());assertFalse(p.getParametros().containsKey("listaHotelInicio"));}
    @Test void naoReaproveitaSelecaoDeHotelPublicoOuAereo(){assertNull(ChatIaHotelListaContexto.comando("2",ChatV2Plan.of(ChatV2Capability.HOTEL_DADOS)));assertNull(ChatIaHotelListaContexto.comando("mostre mais",ChatV2Plan.of(ChatV2Capability.RESERVAS)));}
    @Test void planejadorAceitaNovaListaSemForcarConsultaIndividual()throws Exception{var s=ChatV2Plan.of(ChatV2Capability.HOTEL_LISTA);s.getParametros().put("listaHotelCidade","Recife");var p=planejar("Quais reservas de hotel tenho para Recife?",anterior(),10,20,s);assertEquals(ChatV2Capability.HOTEL_LISTA,p.capability());assertNull(p.getPergunta());assertEquals("Recife",p.getParametros().get("listaHotelCidade"));}
    @Test void hotelListaNaoViraCancelamentoAereo()throws Exception{assertEquals(ChatV2Capability.AJUDA,planejar("Cancele essa reserva",anterior(),10,20,ChatV2Plan.of(ChatV2Capability.ACOES_RESERVA)).capability());}
    @Test void statusNaoSuportadoNaoEIgnorado()throws Exception{var p=planejar("Mostre reservas de hotel confirmadas",anterior(),10,20,ChatV2Plan.of(ChatV2Capability.HOTEL_LISTA));assertNotNull(p.getPergunta());assertTrue(p.getPergunta().contains("status"));}
}
