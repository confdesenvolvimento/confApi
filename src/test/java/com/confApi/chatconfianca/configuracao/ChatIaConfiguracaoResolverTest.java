package com.confApi.chatconfianca.configuracao;

import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import com.confApi.chatconfianca.configuracao.ChatIaConfiguracaoSnapshot.*;
import com.confApi.chatconfianca.configuracao.ChatIaConfiguracaoResolver.*;
import static org.junit.jupiter.api.Assertions.*;

class ChatIaConfiguracaoResolverTest {
    final ChatIaConfiguracaoResolver resolver = new ChatIaConfiguracaoResolver();
    final LocalDate hoje = LocalDate.of(2026, 9, 10);
    ChatIaConfiguracaoSnapshot s;
    Configuracao c;
    Conhecimento k;
    Etapa e;
    @BeforeEach void setup() {
        s = new ChatIaConfiguracaoSnapshot(); c = new Configuracao();
        c.setId(1L); c.setVersao(0L); c.setIntencaoCodigo("institucional.suporte_ti");
        c.setStatus(1); c.setIntencaoStatus(1); c.setTemaId(2L); c.setTemaStatus(1);
        c.setPerfilId(3L); c.setPerfilStatus(1); c.setPerfilEscopo("GERAL"); c.setConfiancaMinima(80);
        s.setConfiguracao(c);
        k = conhecimento(10, "GERAL"); s.getConhecimentos().add(k);
        e = new Etapa(); e.setId(4L); e.setOrdem(1); e.setTipo("CONSULTAR_CONHECIMENTO");
        e.setCondicao("SEMPRE"); e.setStatus(1); s.getEtapas().add(e);
    }
    Conhecimento conhecimento(int id, String escopo) {
        Conhecimento k = new Conhecimento(); k.setId((long) id); k.setCodgMemoria(id); k.setStatus(1);
        k.setMemoriaStatus(1); k.setEditorialVersao(1); k.setStatusPublicacao("PUBLICADO");
        k.setVinculoIntencaoAtivo(true); k.setPossuiFonte(true); k.setPossuiTexto(true);
        k.setEscopo(escopo); k.setMemoriaBase("geral"); return k;
    }
    Contexto ctx(String base, Integer unidade, Integer confianca, boolean humano) {
        return new Contexto(1L, 2L, c.getIntencaoCodigo(), c.getIntencaoCodigo(), base, unidade,
                confianca, humano, Set.of(99L), Set.of(10), "ERRO_INTEGRACAO", "TESTE", "TESTE");
    }
    Resultado resolver() { return resolver.resolver(s, ctx("PMW", 7, 90, false), hoje); }
    @Test void tiGeralGeraPropostaSemAplicarNemDepartamento() {
        var r = resolver(); assertEquals("PLANO_PARA_COMPARACAO", r.getStatus());
        assertEquals(Set.of(10), r.getMemoriasPropostas()); assertFalse(r.isAplicada());
        assertNull(r.getDepartamentoSugeridoId()); assertEquals("PROPOSTA", r.getEtapas().get(0).status());
        assertEquals("NAO_AVALIADA_PELO_SHADOW", r.getAutorizacaoAcoes());
    }
    @Test void bspRespeitaVigenciaInclusiveLimites() {
        c.setIntencaoCodigo("financeiro.calendario_bsp");
        k.setVigenteDe(LocalDate.of(2026, 1, 1)); k.setVigenteAte(LocalDate.of(2026, 12, 31));
        var ctx = ctx("PMW", 7, 90, false);
        assertFalse(resolver.resolver(s, ctx, k.getVigenteDe()).getMemoriasPropostas().isEmpty());
        assertFalse(resolver.resolver(s, ctx, k.getVigenteAte()).getMemoriasPropostas().isEmpty());
        assertTrue(resolver.resolver(s, ctx, LocalDate.of(2027, 1, 1)).getMemoriasPropostas().isEmpty());
    }
    @Test void pmwSemEditorialNaoFicaPublicadaAutomaticamente() {
        k.setEscopo("BASE"); k.setBase("PMW"); k.setMemoriaBase("PMW");
        k.setEditorialVersao(null); k.setStatusPublicacao(null);
        assertTrue(resolver().getConhecimentos().get(0).motivos().contains("MEMORIA_NAO_PUBLICADA"));
        assertTrue(resolver().getMemoriasPropostas().isEmpty());
    }
    @Test void naoUsaDadosDeOutraBaseNemAliasInferido() {
        k.setEscopo("BASE"); k.setBase("PMW"); k.setMemoriaBase("PMW");
        for (String base : List.of("CGR", "Palmas", "Confianca"))
            assertTrue(resolver.resolver(s, ctx(base, 7, 90, false), hoje).getMemoriasPropostas().isEmpty());
        assertFalse(resolver.resolver(s, ctx("pmw", 7, 90, false), hoje).getMemoriasPropostas().isEmpty());
    }
    @Test void unidadeEBaseOriginalSaoIntersecao() {
        k.setEscopo("UNIDADE"); k.setCodgUnidade(7); k.setEditorialUnidade(7); k.setMemoriaBase("PMW");
        assertEquals(Set.of(10), resolver().getMemoriasPropostas());
        assertTrue(resolver.resolver(s, ctx("CGR", 7, 90, false), hoje).getMemoriasPropostas().isEmpty());
        assertTrue(resolver.resolver(s, ctx("PMW", 8, 90, false), hoje).getMemoriasPropostas().isEmpty());
    }
    @Test void geralNaoAmpliaEscopoOriginalOuEditorial() {
        k.setMemoriaBase("PMW"); k.setEditorialUnidade(7);
        assertTrue(resolver.resolver(s, ctx("CGR", 7, 90, false), hoje).getMemoriasPropostas().isEmpty());
        assertTrue(resolver.resolver(s, ctx("PMW", 8, 90, false), hoje).getMemoriasPropostas().isEmpty());
    }
    @Test void apenasMesmoTemaNaoBasta() {
        k.setVinculoIntencaoAtivo(false);
        assertTrue(resolver().getConhecimentos().get(0).motivos().contains("SEM_VINCULO_ATIVO_COM_INTENCAO"));
        assertEquals("SEM_CONHECIMENTO_ELEGIVEL", resolver().getEtapas().get(0).status());
    }
    @Test void cadeiaInativaExplicaBloqueiosSemAtivar() {
        c.setStatus(0); c.setTemaStatus(0); c.setPerfilStatus(0); c.setIntencaoStatus(0); k.setStatus(0); e.setStatus(0);
        var r = resolver(); assertEquals("CADASTRO_BLOQUEADO", r.getStatus());
        assertTrue(r.getMotivos().containsAll(List.of("CONFIGURACAO_INATIVA", "TEMA_INATIVO_OU_AUSENTE", "PERFIL_INATIVO_OU_AUSENTE", "INTENCAO_LEGADA_INATIVA")));
        assertTrue(r.getMemoriasPropostas().isEmpty()); assertEquals(0, c.getStatus());
    }
    @Test void perfilVemDoVinculoNaoDoCodigoOuBase() {
        c.setPerfilEscopo("BASE"); c.setPerfilBase("CGR");
        assertTrue(resolver().getMotivos().contains("PERFIL_FORA_ESCOPO"));
        c.setPerfilId(null); assertTrue(resolver().getMotivos().contains("PERFIL_NAO_VINCULADO"));
    }
    @Test void confiancaBaixaOuAusentePedeEsclarecimento() {
        assertEquals("ESCLARECER", resolver.resolver(s, ctx("PMW", 7, 79, false), hoje).getStatus());
        assertEquals("ESCLARECER", resolver.resolver(s, ctx("PMW", 7, null, false), hoje).getStatus());
        assertEquals("PLANO_PARA_COMPARACAO", resolver.resolver(s, ctx("PMW", 7, 80, false), hoje).getStatus());
    }
    @Test void prioridadeDeEscopoEComplementoExplicito() {
        Conhecimento pmw = conhecimento(2, "BASE"); pmw.setBase("PMW"); s.getConhecimentos().add(pmw);
        assertEquals(Set.of(2), resolver().getMemoriasPropostas());
        pmw.setPermitirComplementoGeral(true); assertEquals(Set.of(10, 2), resolver().getMemoriasPropostas());
        pmw.setStatusPublicacao("RASCUNHO"); assertTrue(resolver().getMemoriasPropostas().isEmpty());
    }
    @Test void pedidoHumanoTemPrecedenciaEExigeConfirmacao() {
        c.setDepartamentoId(99L);
        var r = resolver.resolver(s, ctx("PMW", 7, 90, true), hoje);
        assertEquals("CONFIRMAR_DEPARTAMENTO", r.getStatus()); assertEquals(99L, r.getDepartamentoSugeridoId());
        assertTrue(r.getEtapas().isEmpty()); assertTrue(r.getMemoriasPropostas().isEmpty());
        c.setDepartamentoId(98L); assertEquals("ESCOLHER_DEPARTAMENTO", resolver.resolver(s, ctx("PMW", 7, 90, true), hoje).getStatus());
        c.setStatus(0); assertNull(resolver.resolver(s, ctx("PMW", 7, 90, true), hoje).getDepartamentoSugeridoId());
        s.setConfiguracao(null); assertEquals("ESCOLHER_DEPARTAMENTO", resolver.resolver(s, ctx("PMW", 7, 90, true), hoje).getStatus());
    }
    void acao(String codigo) {
        e.setTipo("CONSULTAR_ACAO"); e.setAcaoId(5L); e.setAcaoCodigo(codigo); e.setAcaoStatus(1);
        e.setAcaoTemaId(2L); e.setLimiteResultados(5); e.setTimeoutSegundos(12);
    }
    @Test void acaoArbitrariaOuDeOutroTemaNaoProposta() {
        acao("vendas.total_anual"); assertEquals("ACAO_FORA_CATALOGO", resolver().getEtapas().get(0).status());
        acao("financeiro.limites"); e.setAcaoTemaId(9L); assertEquals("ACAO_DE_OUTRO_TEMA", resolver().getEtapas().get(0).status());
    }
    @Test void confirmacaoTecnicaNaoPodeSerRemovidaPeloCadastro() {
        acao("aereo.simular_remarcacao"); e.setExigirConfirmacao(false);
        assertEquals("EXIGE_CONFIRMACAO_NAO_CONSULTAR", resolver().getEtapas().get(0).status());
        e.setTipo("SUGERIR_ACAO"); assertEquals("PROPOSTA", resolver().getEtapas().get(0).status());
        assertTrue(resolver().getEtapas().get(0).exigeConfirmacao());
    }
    @Test void resultadoAtualNaoProvaResultadoDeNovaEtapa() {
        for (String condicao : List.of("COM_DADOS", "SEM_DADOS", "DADOS_FALTANTES")) {
            e.setCondicao(condicao); assertEquals("PENDENTE_RESULTADO_DA_ETAPA", resolver().getEtapas().get(0).status());
        }
    }
    @Test void versaoIncompativelOuIntencaoDiferenteBloqueia() {
        s.setVersaoContrato("futura"); assertEquals("CONTRATO_INCOMPATIVEL", resolver().getStatus());
        s.setVersaoContrato("ia-config-shadow-v1");
        Contexto outro = new Contexto(1L, 2L, "financeiro.limites", "financeiro.limites", "PMW", 7, 90, false, Set.of(), Set.of(), "OK", "TESTE", "TESTE");
        assertEquals("CADASTRO_BLOQUEADO", resolver.resolver(s, outro, hoje).getStatus());
    }
    void pilotoTi(boolean ativo) {
        int status = ativo ? 1 : 0;
        long versao = ativo ? 1L : 0L;
        c.setId(9L); c.setVersao(versao); c.setStatus(status);
        c.setTemaId(2L); c.setTemaStatus(status); c.setTemaVersao(versao);
        c.setPerfilId(2L); c.setPerfilStatus(status); c.setPerfilVersao(versao);
        c.setMaxSugestoes(3); c.setQtdInteracoesAtendente(8); c.setDepartamentoId(null);
        k.setId(1L); k.setStatus(status); k.setVersao(ativo ? 2L : 1L);
        k.setPrioridade(0); k.setPermitirComplementoGeral(false);
        e.setId(25L); e.setStatus(status); e.setVersao(versao);
        Etapa pergunta = new Etapa(); pergunta.setId(26L); pergunta.setOrdem(2);
        pergunta.setTipo("PERGUNTAR"); pergunta.setCondicao("SEM_DADOS");
        pergunta.setStatus(status); pergunta.setVersao(versao);
        Etapa humano = new Etapa(); humano.setId(27L); humano.setOrdem(3);
        humano.setTipo("OFERECER_ATENDIMENTO"); humano.setCondicao("A_PEDIDO_USUARIO");
        humano.setStatus(status); humano.setVersao(versao);
        s.getEtapas().addAll(List.of(pergunta, humano));
        Conhecimento pmw = conhecimento(2, "BASE"); pmw.setId(3L);
        pmw.setBase("PMW"); pmw.setMemoriaBase("PMW"); pmw.setStatus(0);
        pmw.setEditorialVersao(null); pmw.setStatusPublicacao(null);
        s.getConhecimentos().add(pmw);
    }
    @Test void pilotoTiAtivoComTresEtapasSoPropoeMemoriaGeralSemAcao() {
        pilotoTi(true);
        var r = resolver.resolver(s, ctx("PMW", 7, 85, false), hoje);
        assertEquals("PLANO_PARA_COMPARACAO", r.getStatus());
        assertEquals(9L, r.getConfiguracaoId()); assertEquals(1L, r.getConfiguracaoVersao());
        assertEquals(2L, r.getTemaId()); assertEquals(1L, r.getTemaVersao());
        assertEquals(2L, r.getPerfilId()); assertEquals(1L, r.getPerfilVersao());
        assertEquals(Set.of(10), r.getMemoriasPropostas()); assertTrue(r.getMotivos().isEmpty());
        assertEquals(List.of(25L, 26L, 27L), r.getEtapas().stream().map(EtapaResultado::id).toList());
        assertEquals(List.of("PROPOSTA", "PENDENTE_RESULTADO_DA_ETAPA", "AGUARDANDO_PEDIDO_USUARIO"),
                r.getEtapas().stream().map(EtapaResultado::status).toList());
        assertTrue(r.getEtapas().stream().allMatch(etapa -> etapa.acaoId() == null && etapa.acaoCodigo() == null));
        var geral = r.getConhecimentos().stream().filter(m -> m.memoriaId() == 10).findFirst().orElseThrow();
        assertEquals(1L, geral.vinculoId()); assertEquals(2L, geral.versao()); assertTrue(geral.motivos().isEmpty());
        var pmw = r.getConhecimentos().stream().filter(m -> m.memoriaId() == 2).findFirst().orElseThrow();
        assertTrue(pmw.motivos().containsAll(List.of("VINCULO_INATIVO", "MEMORIA_NAO_PUBLICADA")));
        assertFalse(r.isAplicada()); assertNull(r.getDepartamentoSugeridoId());
        assertEquals("NAO_AVALIADA_PELO_SHADOW", r.getAutorizacaoAcoes());
    }
    @Test void pilotoTiNaoLiberaOutrasIntencoesComTemaEPerfilAtivos() {
        pilotoTi(true);
        c.setStatus(0); c.setVersao(0L);
        for (String codigo : List.of("institucional.contatos_departamentos",
                "institucional.horario_atendimento", "institucional.atendimento_emergencial")) {
            c.setIntencaoCodigo(codigo);
            var r = resolver();
            assertEquals("CADASTRO_BLOQUEADO", r.getStatus(), codigo);
            assertEquals(List.of("CONFIGURACAO_INATIVA"), r.getMotivos(), codigo);
            assertTrue(r.getMemoriasPropostas().isEmpty(), codigo);
            assertTrue(r.getEtapas().stream().noneMatch(etapa -> "PROPOSTA".equals(etapa.status())), codigo);
            assertFalse(r.isAplicada());
        }
    }
    @Test void pilotoTiPedidoHumanoSemDepartamentoNaoConsultaNemTransfere() {
        pilotoTi(true);
        for (Integer confianca : Arrays.asList(85, 79, null)) {
            var r = resolver.resolver(s, ctx("PMW", 7, confianca, true), hoje);
            assertEquals("ESCOLHER_DEPARTAMENTO", r.getStatus());
            assertNull(r.getDepartamentoSugeridoId()); assertFalse(r.isAplicada());
            assertTrue(r.getEtapas().isEmpty()); assertTrue(r.getMemoriasPropostas().isEmpty());
        }
    }
    @Test void pilotoTiAtivoNaoIgnoraPublicacaoDaFonte() {
        pilotoTi(true); k.setStatusPublicacao("RASCUNHO");
        var r = resolver();
        assertEquals("SEM_ETAPA_PROPONIVEL", r.getStatus());
        assertTrue(r.getMemoriasPropostas().isEmpty()); assertFalse(r.isAplicada());
        assertEquals("SEM_CONHECIMENTO_ELEGIVEL", r.getEtapas().get(0).status());
        assertEquals("PENDENTE_RESULTADO_DA_ETAPA", r.getEtapas().get(1).status());
    }
    @Test void pilotoTiAntesDaAtivacaoEDepoisDaReversaoPermaneceBloqueado() {
        pilotoTi(false);
        for (long versao : List.of(0L, 2L)) {
            c.setVersao(versao); c.setTemaVersao(versao); c.setPerfilVersao(versao);
            k.setVersao(versao + 1L); s.getEtapas().forEach(etapa -> etapa.setVersao(versao));
            var r = resolver();
            assertEquals("CADASTRO_BLOQUEADO", r.getStatus()); assertFalse(r.isAplicada());
            assertTrue(r.getMotivos().containsAll(List.of("CONFIGURACAO_INATIVA",
                    "TEMA_INATIVO_OU_AUSENTE", "PERFIL_INATIVO_OU_AUSENTE")));
            assertTrue(r.getMemoriasPropostas().isEmpty());
            assertTrue(r.getEtapas().stream().allMatch(etapa -> "ETAPA_INATIVA".equals(etapa.status())));
        }
    }
    @Test void faltaDeFonteEConteudoBloqueia() {
        k.setPossuiFonte(false); k.setPossuiTexto(false);
        assertTrue(resolver().getMemoriasPropostas().isEmpty());
        assertTrue(resolver().getConhecimentos().get(0).motivos().containsAll(List.of("MEMORIA_SEM_FONTE", "MEMORIA_SEM_CONTEUDO")));
    }
}
