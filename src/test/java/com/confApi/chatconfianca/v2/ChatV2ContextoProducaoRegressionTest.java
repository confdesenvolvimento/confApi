package com.confApi.chatconfianca.v2;

import com.confApi.chatconfianca.dto.model.Conversa;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Sanitized reproductions of production 1074, 1075, 1144, 1207 and 1208. */
class ChatV2ContextoProducaoRegressionTest {
    final ObjectMapper mapper=new ObjectMapper();
    ChatV2SemanticClient semantic;ChatV2Planner planner;
    @BeforeEach void setup(){
        var props=new ChatV2Properties();props.setEnabled(true);props.setTrafficPercent(100);props.setSemanticEnabled(true);
        semantic=mock(ChatV2SemanticClient.class);planner=new ChatV2Planner(props,semantic,mapper);
    }
    ChatV2Plan state(ChatV2Capability c,Map<String,String> params){
        var p=ChatV2Plan.of(c);p.setParametros(new LinkedHashMap<>(params));p.setAgencia(10);p.setUsuario(20);p.setAtualizadoEm(System.currentTimeMillis());return p;
    }
    Conversa conversation(ChatV2Plan p)throws Exception{
        if(p==null)return null;
        var c=new Conversa();c.setMetadadosJson(mapper.writeValueAsString(Map.of("confiaV2",p)));return c;
    }
    ChatV2Plan plan(String message,ChatV2Plan prior,ChatV2Plan interpreted)throws Exception{
        when(semantic.decidir(anyString(),any(),any())).thenReturn(interpreted);
        return planner.planejar(message,conversation(prior),List.of(),10,20);
    }
    @Test void janeiroDe2027PreservaRotaMesmoSeModeloDisserNovaConversa()throws Exception{
        var prior=state(ChatV2Capability.TARIFA_IDA,Map.of("origem","CGB","destino","BSB","mes","2026-01","adt","2"));
        prior.setPergunta("Qual o ano da viagem?");
        var interpreted=ChatV2Plan.of(ChatV2Capability.TARIFA_IDA);interpreted.getParametros().put("mes","2027-01");
        interpreted.setPergunta("Qual e a origem?");
        var p=plan("janeiro de 2027",prior,interpreted);
        assertEquals("CGB",p.getParametros().get("origem"));assertEquals("BSB",p.getParametros().get("destino"));
        assertEquals("2027-01",p.getParametros().get("mes"));assertEquals("2",p.getParametros().get("adt"));
        assertTrue(p.isContinuar());assertNull(p.getPergunta());assertDoesNotThrow(()->ChatV2Arguments.validar(p,mapper));
    }
    @Test void correcaoMesNaoRessuscitaDataExataAnterior()throws Exception{
        var prior=state(ChatV2Capability.TARIFA_VOLTA,Map.of("origem","CGB","destino","BSB","dataIda","2027-01-08","dataVolta","2027-01-12"));
        var interpreted=ChatV2Plan.of(ChatV2Capability.TARIFA_VOLTA);interpreted.getParametros().put("mesIda","2027-05");
        var p=plan("em maio de 2027",prior,interpreted);
        assertEquals("CGB",p.getParametros().get("origem"));assertEquals("2027-05",p.getParametros().get("mesIda"));
        assertFalse(p.getParametros().containsKey("dataIda"));assertFalse(p.getParametros().containsKey("dataVolta"));
    }
    @ParameterizedTest @ValueSource(strings={"2027","o ano e 2027","ano 2027","para 2027"})
    void anoIsoladoAtualizaPeriodoSemReclassificar(String text)throws Exception{
        var prior=state(ChatV2Capability.TARIFA_VOLTA,Map.of("origem","CGB","destino","BSB","mesIda","2026-12","mesVolta","2027-01"));
        var p=plan(text,prior,null);
        assertEquals("2027-12",p.getParametros().get("mesIda"));assertEquals("2028-01",p.getParametros().get("mesVolta"));
        assertEquals("CGB",p.getParametros().get("origem"));assertTrue(p.isContinuar());assertNull(p.getPergunta());verifyNoInteractions(semantic);
    }
    @Test void anoComDiaBissextoInvalidoNaoEAlteradoSilenciosamente()throws Exception{
        var prior=state(ChatV2Capability.VOOS,Map.of("origem","CGB","destino","BSB","dataIda","2028-02-29"));
        var interpreted=ChatV2Plan.of(ChatV2Capability.VOOS);interpreted.setPergunta("Qual a data valida para 2029?");
        var p=plan("2029",prior,interpreted);assertNotNull(p.getPergunta());assertFalse(p.getParametros().containsKey("dataIda"));verify(semantic).decidir(anyString(),any(),any());
    }
    @ParameterizedTest @ValueSource(strings={"todos os meses","em qualquer mes","sem filtro de periodo"})
    void cacheNaoExigeMesSeUsuarioRemovePeriodo(String text)throws Exception{
        var prior=state(ChatV2Capability.TARIFA_VOLTA,Map.of("origem","CGB","destino","BSB","mesIda","2027-01","dataIda","2027-01-08"));
        var p=plan(text,prior,null);assertEquals(Map.of("origem","CGB","destino","BSB"),p.getParametros());
        assertNull(p.getPergunta());assertDoesNotThrow(()->ChatV2Arguments.validar(p,mapper));verifyNoInteractions(semantic);
    }
    @Test void pesquisaConvencionalContinuaExigindoDataExata()throws Exception{
        var prior=state(ChatV2Capability.VOOS,Map.of("origem","CGB","destino","BSB","dataIda","2027-01-08"));
        var interpreted=ChatV2Plan.of(ChatV2Capability.VOOS);interpreted.setContinuar(true);interpreted.setParametros(new LinkedHashMap<>(Map.of("origem","CGB","destino","BSB")));
        var p=plan("todos os meses",prior,interpreted);assertThrows(IllegalArgumentException.class,()->ChatV2Arguments.validar(p,mapper));verify(semantic).decidir(anyString(),any(),any());
    }
    @Test void exclusaoExplicitaDeOrigemNaoERestaurada()throws Exception{
        var prior=state(ChatV2Capability.TARIFA_IDA,Map.of("origem","CGB","destino","BSB"));
        var interpreted=ChatV2Plan.of(ChatV2Capability.TARIFA_IDA);interpreted.setContinuar(true);interpreted.getParametros().put("mes","2027-01");
        assertFalse(plan("retire a origem e busque em janeiro de 2027",prior,interpreted).getParametros().containsKey("origem"));
    }
    @Test void novaRotaNaoReaproveitaOrigemAnterior()throws Exception{
        var prior=state(ChatV2Capability.TARIFA_IDA,Map.of("origem","CGB","destino","BSB","mes","2027-01"));
        var interpreted=ChatV2Plan.of(ChatV2Capability.TARIFA_IDA);interpreted.setParametros(new LinkedHashMap<>(Map.of("origem","GRU","destino","REC")));
        var p=plan("nova pesquisa GRU para REC",prior,interpreted);
        assertEquals("GRU",p.getParametros().get("origem"));assertEquals("REC",p.getParametros().get("destino"));assertFalse(p.getParametros().containsKey("mes"));
    }
    @Test void trocaDeAssuntoNaoHerdaRotaNaCorrecaoSemantica()throws Exception{
        var prior=state(ChatV2Capability.TARIFA_IDA,Map.of("origem","CGB","destino","BSB"));
        var interpreted=ChatV2Plan.of(ChatV2Capability.BSP);interpreted.getParametros().put("dataEmissao","2027-01-08");
        var p=plan("calendario BSP janeiro 2027",prior,interpreted);assertEquals(ChatV2Capability.BSP,p.capability());assertFalse(p.getParametros().containsKey("origem"));
    }
    @Test void outraAgenciaUsuarioOuTtlNaoRecuperaRota()throws Exception{
        for(String invalid:List.of("agencia","usuario","ttl")){
            var prior=state(ChatV2Capability.TARIFA_IDA,Map.of("origem","CGB","destino","BSB"));
            if(invalid.equals("agencia"))prior.setAgencia(11);if(invalid.equals("usuario"))prior.setUsuario(21);if(invalid.equals("ttl"))prior.setAtualizadoEm(1);
            var interpreted=ChatV2Plan.of(ChatV2Capability.TARIFA_IDA);interpreted.setContinuar(true);interpreted.getParametros().put("mes","2027-01");
            var p=plan("janeiro de 2027",prior,interpreted);assertFalse(p.getParametros().containsKey("origem"));assertFalse(p.isContinuar());
        }
    }
    @ParameterizedTest @ValueSource(strings={"Liste as 10 ultimas reservas da minha agencia","Quais reservas aereas foram criadas hoje?","Mostre minhas reservas recentes"})
    void listagemGenericaOuAereaNaoPodeVirarHotel(String text)throws Exception{
        var interpreted=ChatV2Plan.of(ChatV2Capability.HOTEL_LISTA);interpreted.getParametros().put("listaHotelTipoData","CRIACAO");interpreted.setPergunta("Qual a data de entrada?");
        var p=plan(text,null,interpreted);assertEquals(ChatV2Capability.RESERVAS,p.capability());assertNull(p.getPergunta());assertFalse(p.getParametros().containsKey("listaHotelTipoData"));
    }
    @Test void perguntaDeHospedagemNaoBloqueiaIntencaoAerea()throws Exception{
        var interpreted=ChatV2Plan.of(ChatV2Capability.RESERVAS);interpreted.setPergunta("A data se refere a criacao, entrada ou saida?");
        var p=plan("Liste as 10 ultimas reservas da minha agencia",null,interpreted);assertNull(p.getPergunta());assertEquals(ChatV2Capability.RESERVAS,p.capability());
    }
    @Test void contextoValidoDeReservasDeHotelPreservaProduto()throws Exception{
        var prior=state(ChatV2Capability.HOTEL_LISTA,Map.of());
        var p=plan("Liste as 10 ultimas reservas da minha agencia",prior,ChatV2Plan.of(ChatV2Capability.HOTEL_LISTA));
        assertEquals(ChatV2Capability.HOTEL_LISTA,p.capability());
    }
    @Test void aereoExplicitoSobrepoeContextoAnteriorHotel()throws Exception{
        var prior=state(ChatV2Capability.HOTEL_LISTA,Map.of());
        var p=plan("Liste as reservas aereas",prior,ChatV2Plan.of(ChatV2Capability.HOTEL_LISTA));assertEquals(ChatV2Capability.RESERVAS,p.capability());
    }
    @Test void hotelExplicitoNaoViraAereo()throws Exception{
        var p=plan("Liste as reservas de hotel",null,ChatV2Plan.of(ChatV2Capability.HOTEL_LISTA));assertEquals(ChatV2Capability.HOTEL_LISTA,p.capability());
    }
    @Test void iataCompletaCampoPendenteSemPerderDatasOuOutroAeroporto()throws Exception{
        var prior=state(ChatV2Capability.VOOS,Map.of("origem","CGB","dataIda","2027-01-08","dataVolta","2027-01-12"));
        prior.setPergunta("Qual e o destino?");
        var p=plan("BSB",prior,null);assertEquals("CGB",p.getParametros().get("origem"));assertEquals("BSB",p.getParametros().get("destino"));
        assertEquals("2027-01-08",p.getParametros().get("dataIda"));assertEquals("2027-01-12",p.getParametros().get("dataVolta"));
        assertNull(p.getPergunta());assertDoesNotThrow(()->ChatV2Arguments.validar(p,mapper));verifyNoInteractions(semantic);
    }
    @Test void simNaoEUsadoComoAeroportoPendente()throws Exception{
        var prior=state(ChatV2Capability.TARIFA_IDA,Map.of("origem","CGB"));prior.setPergunta("Qual o destino?");
        var interpreted=ChatV2Plan.of(ChatV2Capability.TARIFA_IDA);interpreted.setPergunta("Informe o destino.");
        var p=plan("SIM",prior,interpreted);assertFalse(p.getParametros().containsKey("destino"));assertNotNull(p.getPergunta());verify(semantic).decidir(anyString(),any(),any());
    }
    @Test void iataSemPerguntaPendenteNaoReaproveitaRotaAnterior()throws Exception{
        var prior=state(ChatV2Capability.TARIFA_IDA,Map.of("origem","CGB"));
        var interpreted=ChatV2Plan.of(ChatV2Capability.AJUDA);interpreted.setPergunta("O que deseja consultar?");
        var p=plan("BSB",prior,interpreted);assertFalse(p.getParametros().containsKey("origem"));verify(semantic).decidir(anyString(),any(),any());
    }
    @Test void hotelExplicitoCorrigeErroSemanticoSemPedirLocalizador()throws Exception{
        var interpreted=ChatV2Plan.of(ChatV2Capability.RESERVAS);interpreted.getParametros().put("localizador","ABC123");interpreted.setPergunta("Qual o localizador aereo?");
        var p=plan("Liste as reservas de hotel",null,interpreted);assertEquals(ChatV2Capability.HOTEL_LISTA,p.capability());assertNull(p.getPergunta());assertTrue(p.getParametros().isEmpty());
    }
    @Test void correcaoDeProdutoPreservaPerguntaGenuinaSemTrazerParametrosHotel()throws Exception{
        var interpreted=ChatV2Plan.of(ChatV2Capability.HOTEL_LISTA);interpreted.getParametros().putAll(Map.of("listaHotelCidade","Recife","listaHotelOpcao","2","localizador","ABC123"));
        interpreted.setPergunta("Qual cidade consultar primeiro: Manaus ou Recife?");
        var p=plan("Quais reservas tenho para Manaus ou Recife?",null,interpreted);assertEquals(ChatV2Capability.RESERVAS,p.capability());
        assertEquals("Qual cidade consultar primeiro: Manaus ou Recife?",p.getPergunta());assertTrue(p.getParametros().isEmpty());
    }
    @Test void contextoHotelExpiradoNaoDeterminaProduto()throws Exception{
        var prior=state(ChatV2Capability.HOTEL_LISTA,Map.of());prior.setAtualizadoEm(1);
        assertEquals(ChatV2Capability.RESERVAS,plan("Liste as 10 ultimas reservas",prior,ChatV2Plan.of(ChatV2Capability.HOTEL_LISTA)).capability());
    }
    @Test void contextoNoFuturoNaoRestauraRota()throws Exception{
        var prior=state(ChatV2Capability.TARIFA_IDA,Map.of("origem","CGB","destino","BSB","mes","2026-01"));prior.setAtualizadoEm(System.currentTimeMillis()+60_000);
        var interpreted=ChatV2Plan.of(ChatV2Capability.TARIFA_IDA);interpreted.setContinuar(true);interpreted.getParametros().put("mes","2027-01");
        var p=plan("janeiro de 2027",prior,interpreted);assertFalse(p.isContinuar());assertFalse(p.getParametros().containsKey("origem"));
    }
    @Test void perguntaRealmenteAmbiguaNaoESuprimida()throws Exception{
        var interpreted=ChatV2Plan.of(ChatV2Capability.TARIFA_IDA);interpreted.setParametros(new LinkedHashMap<>(Map.of("origem","CGB","destino","BSB")));
        interpreted.setPergunta("Qual destino consultar primeiro: Brasilia ou Recife?");
        var p=plan("menor tarifa de CGB para BSB ou REC",null,interpreted);assertNotNull(p.getPergunta());
    }
}
