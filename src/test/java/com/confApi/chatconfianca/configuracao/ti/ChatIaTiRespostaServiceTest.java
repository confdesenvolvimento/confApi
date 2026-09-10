package com.confApi.chatconfianca.configuracao.ti;

import com.confApi.chatconfianca.dto.enums.*;
import com.confApi.chatconfianca.dto.model.*;
import com.confApi.chatconfianca.dto.request.PerguntarConfiaRequest;
import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.chatconfianca.intencao.*;
import com.confApi.chatconfianca.v2.ChatV2Plan;
import com.confApi.chatgpt.dto.*;
import com.confApi.chatgpt.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatIaTiRespostaServiceTest {
    ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    ChatIaTiProperties props; ChatIaTiConteudoClient client; ChatService chat; ChatIaTiRespostaService service;
    Conversa conversa; PerguntarConfiaRequest request; SessaoChatResponse sessao;
    ChatConfiancaDecisaoIa decisao; ChatIaTiDocumento documento;
    @BeforeEach void setup() throws Exception {
        props = new ChatIaTiProperties(); props.setEnabled(true); props.setSamplePercent(100);
        props.setUsuarios(Set.of(7)); props.setAgencias(Set.of(321));
        client=mock(ChatIaTiConteudoClient.class); chat=mock(ChatService.class);
        service=new ChatIaTiRespostaService(props,client,chat,mapper);
        conversa=new Conversa(); conversa.setId(774L); conversa.setStatus(StatusConversa.NOVA);
        conversa.setDepartamentoUnidadeId(999L); // Existing technical ConfIA linkage is not a human.
        request=new PerguntarConfiaRequest();request.setConversaId(774L);request.setCodgUsuario(7);
        request.setCodgAgenciaSessao(321);request.setMensagem("Qual contato do suporte de TI? MENSAGEM_PRIVADA");
        sessao=new SessaoChatResponse(); RefAgencia a=new RefAgencia();a.setCodgAgencia(321);sessao.setAgencia(a);
        RefUnidade u=new RefUnidade();u.setCodgUnidade(1);u.setNomeUnidade("CGB");sessao.setUnidade(u);
        decisao=new ChatConfiancaDecisaoIa();decisao.setIntencao("orientacao_geral");decisao.setModo("LEGADO_FALLBACK");
        var c=ChatIntencaoClassificacao.status("CLASSIFICADA");c.setCodigo(ChatIaTiRespostaService.INTENCAO);c.setConfianca(85);
        decisao.setClassificacaoCatalogo(c);
        documento=ChatIaTiTestData.documento();when(client.carregar()).thenAnswer(inv->documento);
        when(chat.responderTiSomenteTexto(anyList())).thenReturn(resposta("Contate ti@example.invalid."));
    }
    ChatResponseDTO resposta(String texto) {return new ChatResponseDTO("teste",texto,List.of(),null,List.of(),List.of(),List.of());}
    ChatIaTiRespostaService.Resultado tentar() {return service.tentar(conversa,3947L,request,sessao,decisao,null,1);}
    @Test void configuracaoPadraoNaoLiberaNinguem() {
        var p=new ChatIaTiProperties();assertFalse(p.permite(7,321,774L));
        p.setEnabled(true);p.setSamplePercent(100);assertFalse(p.permite(7,321,774L));
        p.setUsuarios(Set.of(7));assertFalse(p.permite(7,321,774L));
        p.setAgencias(Set.of(321));assertTrue(p.permite(7,321,774L));
        assertFalse(p.permite(8,321,774L));assertFalse(p.permite(7,322,774L));
        assertFalse(p.permite(null,321,774L));assertFalse(p.permite(7,321,null));
        for(int n:List.of(-1,0,101)){p.setSamplePercent(n);assertFalse(p.permite(7,321,774L));}
        p.setSamplePercent(1);assertFalse(p.permite(7,321,774L));assertTrue(p.permite(7,321,800L));
    }
    @Test void propriedadesSaoVinculadasSemConsultarServicosExternos() {
        new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(ChatIaTiProperties.class,ChatIaTiHttpConfig.class,
                ChatIaTiConteudoClient.class,ChatIaTiRespostaService.class)
            .withBean(com.confApi.confApp.ConfAppService.class,()->mock(com.confApi.confApp.ConfAppService.class))
            .withBean(ChatService.class,()->chat).withBean(ObjectMapper.class,()->mapper)
            .withPropertyValues("chat-confianca.ia-config.ti-resposta.enabled=true",
                "chat-confianca.ia-config.ti-resposta.sample-percent=100",
                "chat-confianca.ia-config.ti-resposta.usuarios=7",
                "chat-confianca.ia-config.ti-resposta.agencias=321")
            .run(ctx->{
                assertNull(ctx.getStartupFailure());
                assertTrue(ctx.getBean(ChatIaTiProperties.class).permite(7,321,774L));
                assertNotNull(ctx.getBean(ChatIaTiRespostaService.class));
                verifyNoInteractions(ctx.getBean(com.confApi.confApp.ConfAppService.class),chat);
            });
    }
    @Test void perguntaPendenteOuPlanoConflitantePreservamDecisorAtual() {
        var plano=ChatV2Plan.of(com.confApi.chatconfianca.v2.ChatV2Capability.TI);
        plano.setPergunta("Qual erro aparece?");
        assertNull(service.tentar(conversa,3947L,request,sessao,decisao,plano,1));
        plano.setPergunta(null);plano.setIntencao("financeiro.calendario_bsp");
        assertNull(service.tentar(conversa,3947L,request,sessao,decisao,plano,1));
        verifyNoInteractions(client,chat);
    }
    @Test void flagShadowNaoCriaExecutorNemCliente() {
        new ApplicationContextRunner().withPropertyValues("chat-confianca.ia-config.shadow-enabled=true")
            .withUserConfiguration(ChatIaTiRespostaService.class,ChatIaTiConteudoClient.class,ChatIaTiHttpConfig.class)
            .run(ctx->{assertNull(ctx.getStartupFailure());assertTrue(ctx.getBeansOfType(ChatIaTiRespostaService.class).isEmpty());});
    }
    @Test void desligadoOuNaoSelecionadoNaoChamaNada() {
        props.setEnabled(false);assertNull(tentar());
        props.setEnabled(true);props.setSamplePercent(1);assertNull(tentar());
        props.setSamplePercent(100);props.setAgencias(Set.of(999));assertNull(tentar());
        verifyNoInteractions(client,chat);assertFalse(decisao.isAplicada());
    }
    @Test void exigeClassificacaoTiAtualSemEvidenciaConflitante() {
        for(String status:List.of("SEM_EVIDENCIA","AMBIGUA","DESABILITADO")){
            decisao.getClassificacaoCatalogo().setStatus(status);assertNull(tentar());
        }
        decisao.getClassificacaoCatalogo().setStatus("CLASSIFICADA");
        decisao.getClassificacaoCatalogo().setCodigo("financeiro.calendario_bsp");assertNull(tentar());
        decisao.getClassificacaoCatalogo().setCodigo(ChatIaTiRespostaService.INTENCAO);
        for(Integer score:Arrays.asList(null,79,101)){decisao.getClassificacaoCatalogo().setConfianca(score);assertNull(tentar());}
        verifyNoInteractions(client,chat);
    }
    @Test void humanoOuEquipeEscolhidaNaoEntramNoPiloto() {
        request.setEncaminharAtendente(true);assertNull(tentar());request.setEncaminharAtendente(false);
        request.setDepartamentoUnidadeId(3L);assertNull(tentar());request.setDepartamentoUnidadeId(null);
        request.setMensagem("Quero um atendente de TI");assertNull(tentar());
        request.setMensagem("Quero falar com uma pessoa de TI");assertNull(tentar());
        request.setMensagem("Qual contato de TI?");
        conversa.setAtendenteResponsavelCodgUsuario(99);assertNull(tentar());conversa.setAtendenteResponsavelCodgUsuario(null);
        for(StatusConversa status:StatusConversa.values())
            if(status!=StatusConversa.NOVA && status!=StatusConversa.AGUARDANDO_SOLICITANTE){
                conversa.setStatus(status);assertNull(tentar());
            }
        verifyNoInteractions(client,chat);
    }
    @Test void identidadeOuMensagemAusenteImpedeLeituraDeConteudo() {
        sessao.setUnidade(null);assertNull(tentar());
        sessao=new SessaoChatResponse();assertNull(tentar());
        assertNull(service.tentar(null,3947L,request,sessao,decisao,null,1));
        assertNull(service.tentar(conversa,null,request,sessao,decisao,null,1));
        verifyNoInteractions(client,chat);
    }
    @Test void decisaoAplicadaDeOutroAssuntoOuAcaoNaoSubstituida() {
        decisao.setAplicada(true);decisao.setIntencao("financeiro.limites");assertNull(tentar());
        decisao.setAplicada(false);decisao.setAcao("limites");assertNull(tentar());
        decisao.setAcao(null);decisao.setFerramenta("search_flights");assertNull(tentar());
        verifyNoInteractions(client,chat);
    }
    @Test void sucessoUsaPerfilFonteNovaESomenteUmaGeracaoSemFerramentas() throws Exception {
        var r=tentar();assertTrue(r.aplicada());assertFalse(r.resposta().content().contains("memória 10"));
        assertEquals(10,r.auditoria().get("memoriaId"));
        assertEquals("CADASTRO_TI",decisao.getModo());assertTrue(decisao.isAplicada());
        assertEquals(List.of(10),decisao.getMemorias().stream().map(ChatIntencaoRuntimeDto.Memoria::getCodgMemoria).toList());
        assertNull(decisao.getDepartamento());assertNull(decisao.getAcao());assertNull(decisao.getFerramenta());
        ArgumentCaptor<List<ChatMessageDTO>> captor=ArgumentCaptor.forClass(List.class);
        verify(chat).responderTiSomenteTexto(captor.capture());verifyNoMoreInteractions(chat);verify(client).carregar();
        var m=captor.getValue();assertEquals(List.of("system","system","user","user"),m.stream().map(ChatMessageDTO::role).toList());
        assertTrue(m.get(1).content().contains("PERFIL_PRIVADO"));assertTrue(m.get(2).content().contains("FONTE_PRIVADA"));
        assertTrue(m.get(3).content().contains("MENSAGEM_PRIVADA"));
        assertFalse(m.get(0).content().contains("FONTE_PRIVADA"));assertFalse(m.get(1).content().contains("FONTE_PRIVADA"));
        String audit=mapper.writeValueAsString(r.auditoria());assertFalse(audit.contains("PRIVAD"));assertFalse(audit.contains("example.invalid"));
        assertEquals(1L,r.auditoria().get("perfilVersao"));assertEquals(2L,r.auditoria().get("conhecimentoVersao"));
        assertTrue(r.resposta().actions().isEmpty());assertTrue(r.resposta().toolCalls().isEmpty());assertTrue(r.resposta().history().isEmpty());
    }
    @Test void limiteInteracoesSugereHumanoSemEncaminhar() {
        var r=service.tentar(conversa,3947L,request,sessao,decisao,null,8);
        assertTrue(r.aplicada());assertTrue(r.sugerirAtendente());assertTrue(r.resposta().content().contains("escolha ou confirme"));
        assertNull(decisao.getDepartamento());assertFalse(Boolean.TRUE.equals(request.getEncaminharAtendente()));
    }
    @Test void perfilEditadoERecarregadoNoProximoTurnoSemCache() throws Exception {
        tentar();documento.setPerfilVersao(2L);documento.setOrientacoes("NOVO_PERFIL");
        var r=tentar();assertEquals(2L,r.auditoria().get("perfilVersao"));verify(client,times(2)).carregar();
        ArgumentCaptor<List<ChatMessageDTO>> captor=ArgumentCaptor.forClass(List.class);
        verify(chat,times(2)).responderTiSomenteTexto(captor.capture());
        assertTrue(captor.getAllValues().get(1).get(1).content().contains("NOVO_PERFIL"));
        assertFalse(captor.getAllValues().get(1).get(1).content().contains("PERFIL_PRIVADO"));
    }
    @Test void limiarEditavelMaisAltoBloqueiaSemGerar() {
        documento.setConfiancaMinima(90);var r=tentar();assertFalse(r.aplicada());
        assertEquals("FALLBACK_CONFIANCA_INSUFICIENTE",r.auditoria().get("status"));verifyNoInteractions(chat);
        assertEquals("LEGADO_FALLBACK",decisao.getModo());
    }
    @Test void limiarCadastradoBaixoNaoRemovePisoTecnico() {
        documento.setConfiancaMinima(0);decisao.getClassificacaoCatalogo().setConfianca(79);assertNull(tentar());
        verifyNoInteractions(client,chat);
    }
    @Test void fonteAusenteOuErroNaoMutaDecisaoNemVazaExcecao() throws Exception {
        documento=null;var vazio=tentar();assertFalse(vazio.aplicada());
        when(client.carregar()).thenThrow(new IllegalStateException("TOKEN_PRIVADO"));
        var falha=tentar();assertFalse(falha.aplicada());assertEquals("CONTEUDO",falha.auditoria().get("etapaFalha"));
        assertFalse(mapper.writeValueAsString(falha.auditoria()).contains("TOKEN_PRIVADO"));
        assertEquals("LEGADO_FALLBACK",decisao.getModo());assertFalse(decisao.isAplicada());verifyNoInteractions(chat);
    }
    @Test void contratoFonteOuTamanhoInvalidoNaoGeraResposta() {
        List<Consumer<ChatIaTiDocumento>> invalidos=List.of(d->d.setVersaoContrato("futura"),d->d.setCodgMemoria(2),
            d->d.setEscopo("BASE"),d->d.setFonte(""),d->d.setTextoMemoria(""),d->d.setOrientacoes(""),
            d->d.setPerfilVersao(null),d->d.setEtapaHumanoVersao(-1L),d->d.setConfiancaMinima(101),
            d->d.setTextoMemoria("x".repeat(16001)),d->d.setOrientacoes("x".repeat(12001)),
            d->d.setVigenteAte(LocalDate.now().minusDays(1)),d->d.setVigenteDe(LocalDate.now().plusDays(1)));
        for(var alterar:invalidos){
            documento=ChatIaTiTestData.documento();alterar.accept(documento);var r=tentar();
            assertFalse(r.aplicada());assertEquals("FALLBACK_CONTRATO_OU_FONTE_INVALIDA",r.auditoria().get("status"));
            assertFalse(decisao.isAplicada());
        }
        verifyNoInteractions(chat);
    }
    @Test void vigenciaIncluiLimites() {
        documento.setVigenteDe(LocalDate.now());documento.setVigenteAte(LocalDate.now());
        assertTrue(ChatIaTiRespostaService.valido(documento,LocalDate.now()));
    }
    @Test void geracaoFalhaRetornaAoCaminhoAtualSemAlterarDecisao() throws Exception {
        when(chat.responderTiSomenteTexto(anyList())).thenThrow(new IOException("RESPOSTA_PRIVADA"));
        var r=tentar();assertFalse(r.aplicada());assertEquals("GERACAO",r.auditoria().get("etapaFalha"));
        assertFalse(mapper.writeValueAsString(r.auditoria()).contains("PRIVADA"));assertFalse(decisao.isAplicada());
    }
    @Test void respostaVaziaJsonHtmlFerramentaOuAcaoERejeitada() throws Exception {
        for(ChatResponseDTO invalida:Arrays.asList(null,resposta(""),resposta("{\"pesquisa\":true}"),resposta("<script>"),
                resposta("x".repeat(6001)),new ChatResponseDTO("t","Resposta",List.of(new ToolCallDTO("search_flights",Map.of())),null,List.of(),List.of()))) {
            when(chat.responderTiSomenteTexto(anyList())).thenReturn(invalida);
            var r=tentar();assertFalse(r.aplicada());assertEquals("FALLBACK_RESPOSTA_INVALIDA",r.auditoria().get("status"));
            assertFalse(decisao.isAplicada());
        }
    }
}
