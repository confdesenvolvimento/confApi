package com.confApi.chatconfianca.admin;
import com.confApi.chatconfianca.v2.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ChatIaCatalogoControllerTest {
    @Test @SuppressWarnings("unchecked") void expõeSomenteCatalogoFechadoSemExecutarNada() {
        var result=new ChatIaCatalogoController().catalogo();
        assertEquals(false,result.get("cadastrosAplicadosAoChat"));
        var acoes=(List<Map<String,Object>>)result.get("acoes");
        assertEquals(ChatV2Capability.values().length,acoes.size());
        assertEquals(new HashSet<>(ChatV2Capability.codes()),new HashSet<>(acoes.stream().map(a->a.get("codigo")).toList()));
        assertFalse(acoes.stream().anyMatch(a->"vendas.total_anual".equals(a.get("codigo"))));
        assertTrue(acoes.stream().allMatch(a->a.get("executor")!=null&&a.get("metodo")!=null));
    }
    @Test @SuppressWarnings("unchecked") void metodosExibidosExistemNoCodigoDestaVersao() throws Exception {
        Map<String,String> classes=Map.ofEntries(
                Map.entry("LimitesService","com.confApi.hub.limites.LimitesService"),
                Map.entry("ChatService","com.confApi.chatgpt.service.ChatService"),
                Map.entry("AlertaTarifaService","com.confApi.db.confManager.alertaTarifa.AlertaTarifaService"),
                Map.entry("ToolRouter","com.confApi.chatgpt.tools.ToolRouter"),
                Map.entry("MelhoresTarifasAereasService","com.confApi.cacheHotel.MelhoresTarifasAereasService"),
                Map.entry("MelhoresTarifasAereasIdaVoltaService","com.confApi.cacheHotel.MelhoresTarifasAereasIdaVoltaService"),
                Map.entry("PacoteMelhorOfertaService","com.confApi.cacheHotel.PacoteMelhorOfertaService"),
                Map.entry("ChatV2Executor","com.confApi.chatconfianca.v2.ChatV2Executor"));
        var acoes=(List<Map<String,Object>>)new ChatIaCatalogoController().catalogo().get("acoes");
        for(var a:acoes) {
            Class<?> cls=Class.forName(classes.get(a.get("executor")),false,getClass().getClassLoader());
            assertTrue(Arrays.stream(cls.getDeclaredMethods()).anyMatch(m->m.getName().equals(a.get("metodo"))),a.get("codigo").toString());
        }
    }
    @Test @SuppressWarnings("unchecked") void schemasReutilizadosEConfirmacaoPreservada() {
        var acoes=(List<Map<String,Object>>)new ChatIaCatalogoController().catalogo().get("acoes");
        for(var a:acoes) {
            var c=ChatV2Capability.from((String)a.get("codigo"));
            if(c.tool!=null)assertEquals(ChatV2Arguments.tool(c).jsonSchema(),a.get("parametros"));
            if(Set.of(ChatV2Capability.HUMANO,ChatV2Capability.REMARCACAO,ChatV2Capability.ACOES_RESERVA).contains(c))
                assertEquals(true,a.get("exigeConfirmacao"));
        }
    }
}
