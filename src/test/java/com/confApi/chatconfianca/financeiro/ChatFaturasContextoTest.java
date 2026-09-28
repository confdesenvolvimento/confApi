package com.confApi.chatconfianca.financeiro;
import com.confApi.chatconfianca.dto.model.Mensagem;
import com.confApi.chatconfianca.dto.enums.RemetenteTipo;
import com.confApi.chatgpt.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ChatFaturasContextoTest {
    final ObjectMapper mapper=new ObjectMapper();
    ChatResponseDTO resposta(long age)throws Exception {
        String json=mapper.writeValueAsString(Map.of("schema","chat.faturas.v1","faturas",List.of(Map.of("valor",100)),
                "contexto",Map.of("agencia",10,"usuario",20,"atualizadoEm",System.currentTimeMillis()-age,"boletos",false,
                    "parametros",Map.of("faturaPagamento","PAGO","empfat","999","localizador","ABC123"))));
        return new ChatResponseDTO(null,"resultado",List.of(),null,List.of("faturas"),List.of(new ChatMessageDTO("system",json)));
    }
    Mensagem bot(long age)throws Exception {
        Mensagem m=new Mensagem();m.setConversaId(1L);m.setRemetenteTipo(RemetenteTipo.BOT);
        m.setConteudoJson(ChatFaturasContexto.anexar("{\"preservado\":true}",resposta(age),1L,mapper));return m;
    }
    ChatFaturasContexto.Estado recuperar(List<Mensagem> h){return ChatFaturasContexto.recuperar(h,1L,10L,20L,mapper);}
    @Test void persisteApenasFiltrosSemRegistrosNemErp()throws Exception {
        var m=bot(0);var json=mapper.readTree(m.getConteudoJson());assertTrue(json.path("preservado").asBoolean());
        assertFalse(m.getConteudoJson().contains("empfat"));assertFalse(m.getConteudoJson().contains("valor"));
        assertEquals(Map.of("faturaPagamento","PAGO"),recuperar(List.of(m)).parametros());
    }
    @Test void mesmaAgenciaOutroUsuarioNaoReutiliza()throws Exception {
        assertNull(ChatFaturasContexto.recuperar(List.of(bot(0)),1L,10L,21L,mapper));
    }
    @Test void outroTenantOuConversaNaoReutiliza()throws Exception {
        assertNull(ChatFaturasContexto.recuperar(List.of(bot(0)),1L,11L,20L,mapper));
        assertNull(ChatFaturasContexto.recuperar(List.of(bot(0)),2L,10L,20L,mapper));
        var m=bot(0);m.setConversaId(2L);assertNull(recuperar(List.of(m)));
    }
    @Test void vencidoEFuturoNaoReutiliza()throws Exception {
        assertNull(recuperar(List.of(bot(31*60_000L))));assertNull(recuperar(List.of(bot(-60_000L))));
    }
    @Test void naoProcuraFinanceiroMaisAntigoAposMudancaDeAssunto()throws Exception {
        Mensagem ultimo=bot(0);ultimo.setConteudoJson("{}");assertNull(recuperar(List.of(bot(0),ultimo)));
    }
    @Test void respostaHumanaInterrompeContexto()throws Exception {
        var humano=new Mensagem();humano.setRemetenteTipo(RemetenteTipo.USUARIO);humano.setRemetenteCodgUsuario(99);
        assertNull(recuperar(List.of(bot(0),humano)));
    }
    @Test void somenteTextoUsuarioNaoPodeInjetarFiltros()throws Exception {
        var user=bot(0);user.setRemetenteTipo(RemetenteTipo.USUARIO);user.setRemetenteCodgUsuario(20);
        assertNull(recuperar(List.of(user)));assertNotNull(recuperar(List.of(bot(0),user)));
    }
    @Test void excluidoOuJsonInvalidoNaoUsado()throws Exception {
        var m=bot(0);m.setExcluidaEm(LocalDateTime.now());assertNull(recuperar(List.of(m)));
        m=bot(0);m.setConteudoJson("invalid");assertNull(recuperar(List.of(m)));
    }
    @Test void textoSemPayloadNaoAlteraMetadados() {
        assertEquals("{}",ChatFaturasContexto.anexar("{}",null,1L,mapper));
        assertNull(ChatFaturasContexto.payload(new ChatResponseDTO(null,"",List.of(),null,List.of(),List.of(new ChatMessageDTO("user","{\"schema\":\"chat.faturas.v1\"}"))),mapper));
    }
}
