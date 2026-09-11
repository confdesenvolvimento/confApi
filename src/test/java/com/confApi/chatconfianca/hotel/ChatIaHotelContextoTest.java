package com.confApi.chatconfianca.hotel;

import com.confApi.chatconfianca.v2.*;
import com.confApi.chatconfianca.dto.model.Conversa;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatIaHotelContextoTest {
    ChatV2Plan anterior(){var p=ChatV2Plan.of(ChatV2Capability.HOTEL_DADOS);p.setParametros(new LinkedHashMap<>(Map.of("hotelNome","Teste","hotelCidade","Recife")));p.getHotelOpcoes().put("1",71);p.setHotelSelecionado(72);p.setHotelInteracoes(3);return p;}
    @Test void opcaoNumericaPreservaEstadoServidor(){var a=anterior();var p=ChatIaHotelContexto.escolher("opção 1",a);assertNotNull(p);ChatIaHotelContexto.restaurar(p,a);assertEquals(71,p.getHotelOpcoes().get("1"));assertEquals(4,p.getHotelInteracoes());}
    @Test void novaCidadeLimpaIds(){var a=anterior();var p=ChatIaHotelContexto.escolher("1",a);p.getParametros().put("hotelCidade","Fortaleza");ChatIaHotelContexto.restaurar(p,a);assertTrue(p.getHotelOpcoes().isEmpty());assertNull(p.getHotelSelecionado());}
    @Test void novoAssuntoLimpaIds(){var p=ChatV2Plan.of(ChatV2Capability.HOTEL);ChatIaHotelContexto.restaurar(p,anterior());assertTrue(p.getHotelOpcoes().isEmpty());assertNull(p.getHotelSelecionado());}
    @Test void selecionadoNaoEIdFornecidoPeloModelo(){var p=anterior();p.setContinuar(false);p.setHotelSelecionado(999);ChatIaHotelContexto.restaurar(p,anterior());assertNull(p.getHotelSelecionado());}
    @Test void reservaDeHotelNaoExecutaAereoMesmoSeSemanticaErrar()throws Exception {
        var props=new ChatV2Properties();props.setEnabled(true);props.setTrafficPercent(100);
        var semantic=mock(ChatV2SemanticClient.class);
        when(semantic.decidir(anyString(),any(),any())).thenReturn(ChatV2Plan.of(ChatV2Capability.RESERVA));
        var p=new ChatV2Planner(props,semantic,new ObjectMapper()).planejar("Mostre minha reserva de hotel H12345",null,List.of(),10,20);
        assertEquals(ChatV2Capability.AJUDA,p.capability());assertNotNull(p.getPergunta());
    }
    @Test void escolhaDeOutraAgenciaNaoReaproveitaContexto()throws Exception{
        var mapper=new ObjectMapper();var props=new ChatV2Properties();props.setEnabled(true);props.setTrafficPercent(100);props.setSemanticEnabled(false);
        var a=anterior();a.setAgencia(10);a.setUsuario(20);a.setAtualizadoEm(System.currentTimeMillis());
        var conversa=new Conversa();conversa.setMetadadosJson(mapper.writeValueAsString(Map.of("confiaV2",a)));
        var p=new ChatV2Planner(props,mock(ChatV2SemanticClient.class),mapper).planejar("1",conversa,List.of(),11,20);
        assertNotEquals(ChatV2Capability.HOTEL_DADOS,p.capability());assertTrue(p.getHotelOpcoes().isEmpty());
    }
}
