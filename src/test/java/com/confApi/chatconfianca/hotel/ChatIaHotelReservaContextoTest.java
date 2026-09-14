package com.confApi.chatconfianca.hotel;
import com.confApi.chatconfianca.v2.*;
import com.confApi.chatconfianca.dto.model.Conversa;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatIaHotelReservaContextoTest {
    ObjectMapper mapper=new ObjectMapper();
    ChatV2Plan anterior(){var p=ChatV2Plan.of(ChatV2Capability.HOTEL_RESERVA);p.setAgencia(10);p.setUsuario(20);p.setAtualizadoEm(System.currentTimeMillis());p.getParametros().put("reservaHotelHospede","Hóspede Teste");p.getReservaHotelOpcoes().put("1",71);p.setReservaHotelSelecionada(72);return p;}
    ChatV2Plan planejar(String input,ChatV2Plan anterior,int agencia,int usuario,ChatV2Plan semantico)throws Exception{
        var props=new ChatV2Properties();props.setEnabled(true);props.setTrafficPercent(100);props.setSemanticEnabled(semantico!=null);
        var s=mock(ChatV2SemanticClient.class);if(semantico!=null)when(s.decidir(anyString(),any(),any())).thenReturn(semantico);
        var c=new Conversa();c.setMetadadosJson(mapper.writeValueAsString(Map.of("confiaV2",anterior)));
        return new ChatV2Planner(props,s,mapper).planejar(input,c,List.of(),agencia,usuario);
    }
    @Test void escolhaNumericaPreservaIdServidor()throws Exception{var p=planejar("1",anterior(),10,20,null);assertEquals(ChatV2Capability.HOTEL_RESERVA,p.capability());assertEquals(Map.of("1",71),p.getReservaHotelOpcoes());assertEquals("1",p.getParametros().get("reservaHotelOpcao"));}
    @Test void trocaFiltroLimpaIds(){var a=anterior();var p=ChatIaHotelReservaContexto.escolher("1",a);p.getParametros().put("reservaHotelHospede","Outro Hóspede");ChatIaHotelReservaContexto.restaurar(p,a);assertNull(p.getReservaHotelSelecionada());assertTrue(p.getReservaHotelOpcoes().isEmpty());}
    @Test void outraAgenciaOutroUsuarioOuExpiradoNaoUsaOpcao()throws Exception{for(int[] ids:List.of(new int[]{11,20},new int[]{10,21})){var p=planejar("1",anterior(),ids[0],ids[1],null);assertNotEquals(ChatV2Capability.HOTEL_RESERVA,p.capability());assertTrue(p.getReservaHotelOpcoes().isEmpty());}var a=anterior();a.setAtualizadoEm(1);assertNotEquals(ChatV2Capability.HOTEL_RESERVA,planejar("1",a,10,20,null).capability());}
    @Test void localizadorAereoInventadoPelaSemanticaNaoReaproveitado()throws Exception{var a=ChatV2Plan.of(ChatV2Capability.RESERVA);a.setAgencia(10);a.setUsuario(20);a.setAtualizadoEm(System.currentTimeMillis());a.getParametros().put("localizador","FLSEMF");var s=ChatV2Plan.of(ChatV2Capability.HOTEL_RESERVA);s.setContinuar(true);s.getParametros().put("reservaHotelLocalizador","FLSEMF");var p=planejar("Mostre minha reserva de hotel",a,10,20,s);assertNull(p.getParametros().get("reservaHotelLocalizador"));assertTrue(p.getReservaHotelOpcoes().isEmpty());}
    @Test void localizadorExplicitoAceitoMesmoIgualAoAereo()throws Exception{var s=ChatV2Plan.of(ChatV2Capability.HOTEL_RESERVA);s.getParametros().put("reservaHotelLocalizador","FLSEMF");assertEquals("FLSEMF",planejar("Minha reserva de hotel localizador FLSEMF",anterior(),10,20,s).getParametros().get("reservaHotelLocalizador"));}
    @Test void cancelarEmContinuacaoNuncaViraAcaoAerea()throws Exception{var s=ChatV2Plan.of(ChatV2Capability.ACOES_RESERVA);s.setContinuar(false);var p=planejar("Cancele essa reserva",anterior(),10,20,s);assertEquals(ChatV2Capability.AJUDA,p.capability());assertNotNull(p.getPergunta());}
    @Test void novaPesquisaNaoConsultaReserva()throws Exception{var s=ChatV2Plan.of(ChatV2Capability.HOTEL);var p=planejar("Quero reservar um hotel para janeiro",anterior(),10,20,s);assertEquals(ChatV2Capability.HOTEL,p.capability());assertTrue(p.getReservaHotelOpcoes().isEmpty());}
}
