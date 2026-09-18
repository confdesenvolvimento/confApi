package com.confApi.chatconfianca.configuracao;

import com.confApi.chatconfianca.intencao.*;
import com.confApi.confApp.*;
import com.confApi.config.UrlConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.*;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ChatIaShadowDiagnosticoTest {
    final List<LogRecord> logs=new ArrayList<>();
    final Logger logger=Logger.getLogger(ChatIaConfiguracaoShadowService.class.getName());
    final Handler handler=new Handler() {
        public void publish(LogRecord r) { logs.add(r); }
        public void flush() {}
        public void close() {}
    };
    final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    String original;
    RestTemplate http; ConfAppService auth; ChatIaConfiguracaoShadowService service;
    @BeforeEach void setup() {
        original=UrlConfig.URL_CONFIANCA_MANAGER; UrlConfig.URL_CONFIANCA_MANAGER="http://manager.invalid/";
        logger.addHandler(handler);
        http=new RestTemplate(); auth=mock(ConfAppService.class);
        ConfAppResp token=mock(ConfAppResp.class); when(token.getToken()).thenReturn("TOKEN_PRIVADO");
        when(auth.token()).thenReturn(token);
        service=new ChatIaConfiguracaoShadowService(http,auth,Runnable::run,mapper,100);
    }
    @AfterEach void cleanup() { logger.removeHandler(handler); UrlConfig.URL_CONFIANCA_MANAGER=original; }
    ChatConfiancaDecisaoIa decisao(String intent) {
        var d=new ChatConfiancaDecisaoIa();d.setIntencao(intent==null?"orientacao_geral":intent);
        var c=ChatIntencaoClassificacao.status(intent==null?"SEM_EVIDENCIA":"CLASSIFICADA");
        c.setCodigo(intent);c.setConfianca(intent==null?0:85);d.setClassificacaoCatalogo(c);
        return d;
    }
    void observe(long id, String intent, String text) {
        service.observar(764L,id,"CGB",1,decisao(intent),null,false,List.of(),7,321,text,true);
    }
    String ultimo() { return logs.get(logs.size()-1).getMessage(); }
    @Test void erroBspNaoApagaTopicoParaDataSeguinteNemVazaConteudo() throws Exception {
        when(auth.token()).thenThrow(new ResourceAccessException("TOKEN_PRIVADO",new SocketTimeoutException("resposta privada")))
                .thenReturn(mock(ConfAppResp.class));
        observe(3886,"financeiro.calendario_bsp","MENSAGEM_PRIVADA");
        assertTrue(ultimo().contains("etapa=AUTENTICACAO"));assertTrue(ultimo().contains("erroCodigo=TIMEOUT"));
        assertTrue(ultimo().contains("conversa=764 mensagem=3886"));assertTrue(ultimo().contains("duracaoMs="));
        assertNull(logs.get(logs.size()-1).getThrown());
        assertFalse(ultimo().contains("PRIVAD"));assertFalse(ultimo().contains("resposta privada"));
        // Clear only the isolated test object's cooldown. No sleep or application context.
        ((AtomicLong)ReflectionTestUtils.getField(service,"indisponivelAte")).set(0);
        var token=mock(ConfAppResp.class);when(token.getToken()).thenReturn("TOKEN_PRIVADO");when(auth.token()).thenReturn(token);
        var server=MockRestServiceServer.bindTo(http).build();
        server.expect(requestTo("http://manager.invalid/chatIa/runtime/configuracao?intencao=financeiro.calendario_bsp"))
                .andRespond(withSuccess("{\"configuracao\":null,\"etapas\":[],\"conhecimentos\":[]}",MediaType.APPLICATION_JSON));
        observe(3888,null,"10-08-2026");server.verify();
        var json=mapper.readTree(ultimo().substring("IA_CONFIG_SHADOW ".length()));
        assertEquals("financeiro.calendario_bsp",json.path("contexto").path("intencaoConsulta").asText());
        assertEquals("CONTINUIDADE_BSP_DATA",json.path("contexto").path("fonteIntencaoConsulta").asText());
        assertEquals(3886,json.path("contexto").path("mensagemOrigemContexto").asInt());
        assertTrue(json.path("contexto").path("confianca").isNull());
        assertFalse(json.path("proposta").path("aplicada").asBoolean());
        assertFalse(ultimo().contains("10-08-2026"));assertFalse(ultimo().contains("TOKEN_PRIVADO"));
    }
    @Test void errosHttpIndicamEtapaStatusSemCorpoOuUrl() {
        var server=MockRestServiceServer.bindTo(http).build();
        server.expect(anything()).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("TOKEN_PRIVADO corpo restrito").contentType(MediaType.TEXT_PLAIN));
        observe(3886,"financeiro.calendario_bsp","MENSAGEM_PRIVADA");server.verify();
        assertTrue(ultimo().contains("etapa=CONSULTA_CONFIGURACAO"));
        assertTrue(ultimo().contains("erroCodigo=HTTP_5XX httpStatus=500"));
        assertFalse(ultimo().contains("PRIVAD"));assertFalse(ultimo().contains("restrito"));assertFalse(ultimo().contains("manager.invalid"));
    }
    @Test void jsonInvalidoDistingueLeituraDeMetadados() {
        var server=MockRestServiceServer.bindTo(http).build();
        server.expect(anything()).andRespond(withSuccess("{\"configuracao\": TOKEN_PRIVADO",MediaType.APPLICATION_JSON));
        observe(3886,"financeiro.calendario_bsp","MENSAGEM_PRIVADA");server.verify();
        assertTrue(ultimo().contains("etapa=CONSULTA_CONFIGURACAO"));assertTrue(ultimo().contains("erroCodigo=JSON_INVALIDO"));
        assertFalse(ultimo().contains("PRIVAD"));assertNull(logs.get(logs.size()-1).getThrown());
    }
    @Test void urlAusenteNaoTentaAutenticar() {
        UrlConfig.URL_CONFIANCA_MANAGER=null;
        observe(3886,"financeiro.calendario_bsp","BSP");
        assertTrue(ultimo().contains("etapa=VALIDACAO_URL"));assertTrue(ultimo().contains("erroCodigo=URL_AUSENTE"));
        verifyNoInteractions(auth);
    }
    @Test void tokenAusenteDistingueAutenticacao() {
        when(auth.token()).thenReturn(null); observe(3886,"financeiro.calendario_bsp","BSP");
        assertTrue(ultimo().contains("etapa=AUTENTICACAO"));assertTrue(ultimo().contains("erroCodigo=TOKEN_AUSENTE"));
    }
    @Test void respostaVaziaDistingueContratoAusente() {
        var server=MockRestServiceServer.bindTo(http).build();
        server.expect(anything()).andRespond(withNoContent());
        observe(3886,"financeiro.calendario_bsp","BSP");server.verify();
        assertTrue(ultimo().contains("etapa=CONSULTA_CONFIGURACAO"));assertTrue(ultimo().contains("erroCodigo=RESPOSTA_VAZIA"));
    }
    @Test void erroDeSerializacaoTemEtapaPropriaSemDetalhesSensiveis() throws Exception {
        var brokenMapper=mock(ObjectMapper.class);
        when(brokenMapper.writeValueAsString(any())).thenThrow(
                new com.fasterxml.jackson.core.JsonProcessingException("TOKEN_PRIVADO") {});
        service=new ChatIaConfiguracaoShadowService(http,auth,Runnable::run,brokenMapper,100);
        observe(3886,null,"MENSAGEM_PRIVADA");
        assertTrue(ultimo().contains("etapa=SERIALIZACAO"));assertTrue(ultimo().contains("erroCodigo=JSON_INVALIDO"));
        assertFalse(ultimo().contains("PRIVAD"));assertNull(logs.get(logs.size()-1).getThrown());
    }
    @Test void erroDeResolucaoNaoSeConfundeComHttp() {
        var server=MockRestServiceServer.bindTo(http).build();
        server.expect(anything()).andRespond(withSuccess(
                "{\"configuracao\":{\"intencaoCodigo\":\"financeiro.calendario_bsp\"},\"conhecimentos\":[null]}",
                MediaType.APPLICATION_JSON));
        observe(3886,"financeiro.calendario_bsp","BSP");server.verify();
        assertTrue(ultimo().contains("etapa=RESOLUCAO"));assertTrue(ultimo().contains("erroCodigo=FALHA_INTERNA"));
    }
    @Test void turnoDeOutroAssuntoDuranteCooldownInvalidaContinuidade() throws Exception {
        when(auth.token()).thenThrow(new IllegalStateException());
        observe(3886,"financeiro.calendario_bsp","BSP");
        observe(3887,"institucional.suporte_ti","TI");
        ((AtomicLong)ReflectionTestUtils.getField(service,"indisponivelAte")).set(0);
        observe(3888,null,"10-08-2026");
        var json=mapper.readTree(ultimo().substring("IA_CONFIG_SHADOW ".length()));
        assertTrue(json.path("contexto").path("intencaoConsulta").isNull());
        verify(auth,times(1)).token();
    }
}
