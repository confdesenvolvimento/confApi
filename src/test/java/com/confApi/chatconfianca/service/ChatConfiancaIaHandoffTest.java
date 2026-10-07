package com.confApi.chatconfianca.service;
import com.confApi.chatconfianca.dto.enums.StatusConversa;
import com.confApi.chatconfianca.dto.model.Conversa;
import com.confApi.chatconfianca.dto.request.PerguntarConfiaRequest;
import com.confApi.chatconfianca.intencao.*;
import com.confApi.chatgpt.profile.ProfilePromptRegistry;
import com.confApi.chatgpt.service.ChatService;
import com.confApi.exception.RegraDeNegocioException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ChatConfiancaIaHandoffTest {
    final ChatConfiancaService persistence=mock(ChatConfiancaService.class);
    final ChatService chat=mock(ChatService.class);
    final ChatIaDecisaoAuditService audit=mock(ChatIaDecisaoAuditService.class);
    final ChatConfiancaIaService service=new ChatConfiancaIaService(persistence,chat,mock(ProfilePromptRegistry.class),
            new ObjectMapper(),mock(ChatIntencaoShadowService.class),mock(ChatConfiancaDecisaoIaService.class),
            mock(ChatMemoriaRecuperacaoShadowAuditService.class),audit);
    PerguntarConfiaRequest request(){var r=new PerguntarConfiaRequest();r.setConversaId(10L);r.setCodgUsuario(7);r.setDepartamentoUnidadeId(30L);r.setMensagem("Confirmo Financeiro");return r;}
    Conversa conversa(StatusConversa status,Integer atendente){var c=new Conversa();c.setId(10L);c.setDepartamentoUnidadeId(30L);c.setStatus(status);c.setAtendenteResponsavelCodgUsuario(atendente);return c;}
    @Test void endpointExigeEscolhaAntesDeQualquerEncaminhamento() {
        var r=request();r.setDepartamentoUnidadeId(null);assertThrows(RegraDeNegocioException.class,()->service.encaminharAtendente(r));verifyNoInteractions(persistence,chat,audit);
    }
    @Test void perguntaComFlagSemDestinoNaoGeraTurnoNemConsultaIa() {
        var r=request();r.setDepartamentoUnidadeId(null);r.setEncaminharAtendente(true);
        assertThrows(RegraDeNegocioException.class,()->service.perguntar(r));verifyNoInteractions(persistence,chat,audit);
    }
    @Test void filaNaoPrometeAtendimentoIniciado() {
        when(persistence.encaminharConversaParaAtendente(eq(10L),eq(7),eq(30L),anyString())).thenReturn(conversa(StatusConversa.AGUARDANDO_ATENDENTE,null));
        var r=service.encaminharAtendente(request());assertTrue(r.isAtendenteSolicitado());assertFalse(r.isSugerirAtendente());
        assertTrue(r.getMensagemAtendente().contains("na fila da equipe escolhida"));assertTrue(r.getMensagemAtendente().contains("aguardando"));
        verify(audit).registrarEncaminhamento(10L,30L);verifyNoInteractions(chat);
    }
    @Test void atribuicaoConfirmadaNaoDizAguardandoNaFila() {
        when(persistence.encaminharConversaParaAtendente(eq(10L),eq(7),eq(30L),anyString())).thenReturn(conversa(StatusConversa.EM_ATENDIMENTO,91));
        var r=service.encaminharAtendente(request());assertTrue(r.getMensagemAtendente().contains("atribuída"));assertFalse(r.getMensagemAtendente().contains("na fila"));
        verifyNoInteractions(chat);
    }
    @Test void erroDeValidacaoNaoRegistraHandoffComoSucesso() {
        when(persistence.encaminharConversaParaAtendente(eq(10L),eq(7),eq(30L),anyString())).thenThrow(new RegraDeNegocioException(400,"Destino indisponivel"));
        assertThrows(RegraDeNegocioException.class,()->service.encaminharAtendente(request()));verifyNoInteractions(chat,audit);
    }
}
