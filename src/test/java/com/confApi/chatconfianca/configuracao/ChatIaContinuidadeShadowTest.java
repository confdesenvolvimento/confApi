package com.confApi.chatconfianca.configuracao;

import com.confApi.chatconfianca.configuracao.ChatIaConfiguracaoResolver.Contexto;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ChatIaContinuidadeShadowTest {
    final AtomicLong clock = new AtomicLong(1);
    final ChatIaContinuidadeShadow state = new ChatIaContinuidadeShadow(clock::get, 2, 100);
    Contexto ctx(long conversa, long mensagem, String intent, String base, int unidade, boolean humano) {
        return new Contexto(conversa, mensagem, intent == null ? "orientacao_geral" : intent, intent,
                base, unidade, intent == null ? null : 85, humano, Set.of(7L), Set.of(), "SUCESSO", "LEGADO", "CLASSIFICADOR");
    }
    Contexto ctx(long mensagem, String intent) { return ctx(764, mensagem, intent, "CGB", 1, false); }
    void iniciar() { state.observar(ctx(3886, "financeiro.calendario_bsp"), 7, 321, "Quero o calendario BSP", true, false); }
    Contexto data(String texto) { return state.observar(ctx(3888, null), 7, 321, texto, true, true); }

    @ParameterizedTest @ValueSource(strings={"10-08-2026","10/08/2026"," 10/08/2026 "})
    void dataContinuaBspSemInventarScoreNemAlterarDecisao(String texto) {
        iniciar(); Contexto original = ctx(3888, null);
        Contexto result = state.observar(original, 7, 321, texto, true, true);
        assertEquals("financeiro.calendario_bsp", result.intencaoConsulta());
        assertEquals("orientacao_geral", result.intencaoAtual());
        assertNull(result.confianca()); assertEquals("CONTEXTO_SEM_NOVO_SCORE", result.fonteConfianca());
        assertEquals("CONTINUIDADE_BSP_DATA", result.fonteIntencaoConsulta()); assertEquals(3886L, result.mensagemOrigemContexto());
        assertNull(original.intencaoConsulta()); assertFalse(result.toString().contains(texto.trim()));
    }
    @ParameterizedTest @ValueSource(strings={"31/02/2026","10/08-2026","2026-08-10","10/08","sim",
            "cancele","quero voo 10/08/2026","10/08/2026 e limites","10/08/0000",""})
    void naoAdivinhaAssuntoDeTextoOuDataInvalida(String texto) {
        iniciar(); assertNull(data(texto).intencaoConsulta());
        assertNull(state.observar(ctx(3890,null),7,321,"10/08/2026",true,true).intencaoConsulta());
    }
    @Test void dataSemAssuntoAnteriorNaoViraBsp() { assertNull(data("10/08/2026").intencaoConsulta()); }
    @Test void novaIntencaoTemPrioridadeELimpaAssunto() {
        iniciar();
        var other = ctx(3887, "institucional.suporte_ti");
        assertSame(other, state.observar(other,7,321,"contato TI",true,false));
        assertNull(data("10/08/2026").intencaoConsulta());
    }
    @Test void pedidoHumanoOuConversaNaoAssistidaInterrompe() {
        for (boolean humano : List.of(true,false)) {
            iniciar();
            var current=ctx(764,3887,null,"CGB",1,humano);
            state.observar(current,7,321,"10/08/2026",humano,true);
            assertNull(data("10/08/2026").intencaoConsulta());
        }
    }
    @Test void exigeAusenciaDeEvidenciaAtualNaoAmbiguidadeOuFalhaDoClassificador() {
        iniciar();
        assertNull(state.observar(ctx(3888,null),7,321,"10/08/2026",true,false).intencaoConsulta());
    }
    @Test void isolaConversaUsuarioAgenciaBaseEUnidadeMesmoAoVoltarAoEscopoAnterior() {
        for (int variation=0; variation<5; variation++) {
            var local = new ChatIaContinuidadeShadow(clock::get, 10, 100);
            local.observar(ctx(3886,"financeiro.calendario_bsp"),7,321,"BSP",true,false);
            var next=ctx(variation==0?765:764,3888,null,variation==3?"PMW":"CGB",variation==4?2:1,false);
            assertNull(local.observar(next,variation==1?8:7,variation==2?322:321,"10/08/2026",true,true).intencaoConsulta());
            if (variation>0)
                assertNull(local.observar(ctx(3890,null),7,321,"10/08/2026",true,true).intencaoConsulta());
        }
    }
    @Test void identidadeAusenteLimpaEImpedeContinuidade() {
        iniciar(); assertNull(state.observar(ctx(3888,null),null,321,"10/08/2026",true,true).intencaoConsulta());
        assertNull(state.observar(ctx(3890,null),7,321,"10/08/2026",true,true).intencaoConsulta());
    }
    @Test void contextoExpiraSemRenovacaoPorDatasRepetidas() {
        iniciar(); clock.set(90); assertNotNull(data("10/08/2026").intencaoConsulta());
        clock.set(101); assertNull(state.observar(ctx(3890,null),7,321,"11/08/2026",true,true).intencaoConsulta());
    }
    @Test void limiteDeConversasRemoveMaisAntiga() {
        iniciar();
        state.observar(ctx(765,3887,"financeiro.calendario_bsp","CGB",1,false),7,321,"BSP",true,false);
        state.observar(ctx(766,3888,"financeiro.calendario_bsp","CGB",1,false),7,321,"BSP",true,false);
        assertNull(data("10/08/2026").intencaoConsulta());
    }
    @Test void turnoAntigoOuDuplicadoNaoRessuscitaAssunto() {
        iniciar();
        state.observar(ctx(3890,"institucional.suporte_ti"),7,321,"TI",true,false);
        state.observar(ctx(3889,"financeiro.calendario_bsp"),7,321,"BSP",true,false);
        assertNull(state.observar(ctx(3891,null),7,321,"10/08/2026",true,true).intencaoConsulta());
    }
    @Test void assuntoSemScoreValidoNaoIniciaContinuidade() {
        var c = ctx(3886,"financeiro.calendario_bsp");
        var withoutScore=new Contexto(c.conversaId(),c.mensagemId(),c.intencaoAtual(),c.intencaoConsulta(),
                c.base(),c.unidade(),null,false,Set.of(),Set.of(),"SUCESSO","V2_SEMANTICA","NAO_DISPONIVEL");
        state.observar(withoutScore,7,321,"BSP",true,false);
        assertNull(data("10/08/2026").intencaoConsulta());
    }
}
