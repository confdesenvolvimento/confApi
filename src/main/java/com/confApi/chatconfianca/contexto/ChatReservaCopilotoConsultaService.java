package com.confApi.chatconfianca.contexto;

import com.confApi.chatconfianca.dto.model.Conversa;
import com.confApi.chatconfianca.dto.enums.StatusConversa;
import com.confApi.chatconfianca.dto.request.PerguntarConfiaRequest;
import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.chatconfianca.dto.reserva.ReservaAereaRecenteItem;
import com.confApi.chatconfianca.reservas.ChatReservaConsultaContexto;
import com.confApi.chatconfianca.service.ChatContextualAcessoService;
import com.confApi.chatgpt.dto.ChatMessageDTO;
import com.confApi.chatgpt.dto.ChatResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** Enriquece apenas o resultado factual do turno, nunca dados extraidos de texto do modelo. */
@Service
public class ChatReservaCopilotoConsultaService {
    private final ChatReservaCopilotoService copiloto;
    private final ChatContextualAcessoService acesso;
    private final ObjectMapper mapper;
    public ChatReservaCopilotoConsultaService(ChatReservaCopilotoService copiloto,ChatContextualAcessoService acesso,ObjectMapper mapper) {
        this.copiloto=copiloto;this.acesso=acesso;this.mapper=mapper;
    }

    public ChatResponseDTO enriquecer(PerguntarConfiaRequest req,SessaoChatResponse sessao,Conversa conversa,ChatResponseDTO resposta) {
        if(req==null||!ChatContextoClienteContrato.suportaCopiloto(req.getContextoCliente())||!copiloto.habilitado())return resposta;
        JsonNode payload=ChatReservaConsultaContexto.payload(resposta,mapper);
        if(payload==null||!"DADOS_CONSULTADOS".equals(payload.path("statusConsulta").asText()))return resposta;
        JsonNode contexto=payload.path("contexto"),itens=contexto.path("itens");
        if(!contexto.path("selecionadaId").canConvertToInt()||!itens.isArray()||itens.size()!=1)return resposta;
        try {
            if(conversa==null||sessao==null||sessao.getAgencia()==null
                    ||conversa.getId()==null||conversa.getId()<=0
                    ||conversa.getStatus()==null||conversa.getEncerradoEm()!=null||conversa.getAtendenteResponsavelCodgUsuario()!=null
                    ||java.util.Set.of(StatusConversa.AGUARDANDO_ATENDENTE,StatusConversa.EM_ATENDIMENTO,StatusConversa.TRANSFERIDA,
                            StatusConversa.ENCERRADA,StatusConversa.CANCELADA,StatusConversa.RESOLVIDA).contains(conversa.getStatus())
                    ||!"CONFIA".equalsIgnoreCase(mapper.readTree(conversa.getMetadadosJson()).path("origem").asText())
                    ||contexto.path("conversa").asLong()!=conversa.getId()
                    ||!Objects.equals(contexto.path("agencia").asInt(),sessao.getAgencia().getCodgAgencia())
                    ||!Objects.equals(contexto.path("usuario").asInt(),req.getCodgUsuario()))
                return bloqueio();
            long idade=System.currentTimeMillis()-contexto.path("atualizadoEm").asLong();
            if(idade<0||idade>300_000)return bloqueio();
            acesso.validarSessao(sessao,req.getCodgUsuario(),req.getCodgAgenciaSessao());
            ReservaAereaRecenteItem item=mapper.treeToValue(itens.get(0),ReservaAereaRecenteItem.class);
            if(!Objects.equals(item.getReservaId(),contexto.path("selecionadaId").asInt()))return bloqueio();
            var acoes=copiloto.acoes(item,req.getContextoCliente());
            if(acoes.isEmpty())return new ChatResponseDTO(resposta.id(),resposta.content(),resposta.toolCalls(),
                    resposta.audio(),resposta.keywords(),resposta.history(),List.of());
            return new ChatResponseDTO(resposta.id(),resposta.content()
                    +"\n\nPróximos passos: escolha uma opção para esta reserva. A simulação será revalidada no fluxo existente; nenhum atendimento ou operação comercial foi iniciado.",
                    resposta.toolCalls(),resposta.audio(),resposta.keywords(),resposta.history(),acoes);
        }catch(Exception ex) {
            java.util.logging.Logger.getLogger(getClass().getName()).warning("COPILOTO_CONSULTA_BLOQUEADO tipo="+ex.getClass().getSimpleName());
            return bloqueio();
        }
    }

    private ChatResponseDTO bloqueio() {
        String json="{\"schema\":\"chat.reservas-consulta.v1\",\"statusConsulta\":\"CONSULTA_BLOQUEADA\",\"resultadoTarefa\":\"CONSULTA_BLOQUEADA\"}";
        return new ChatResponseDTO(null,"Não consegui confirmar a referência e as permissões para preparar as próximas ações. Abra novamente a reserva na sua agência e tente outra vez.",
                List.of(),null,List.of(),List.of(new ChatMessageDTO("system",json)),List.of());
    }

    public static JsonNode payloadRegras(ChatResponseDTO resposta,ObjectMapper mapper) {
        if(resposta==null||resposta.history()==null)return null;
        for(var h:resposta.history())if(h!=null&&"system".equals(h.role())&&h.content()!=null&&h.content().length()<131072)
            try {JsonNode j=mapper.readTree(h.content());if("chat.copiloto-regras.v1".equals(j.path("schema").asText()))return j;}
            catch(Exception ignored){ }
        return null;
    }

    public static String anexarRegras(String existente,ChatResponseDTO resposta,ObjectMapper mapper) {
        JsonNode payload=payloadRegras(resposta,mapper);if(payload==null)return existente;
        try {
            JsonNode original=existente==null?null:mapper.readTree(existente);
            ObjectNode raiz=original instanceof ObjectNode?(ObjectNode)original:mapper.createObjectNode();
            raiz.set("copilotoRegras",payload);
            // O encerramento desta tarefa e uma barreira para referencias mais antigas, inclusive em caso de erro.
            ObjectNode referencia=((ObjectNode)payload).deepCopy();
            referencia.put("schema",ChatAjudaContextualHistorico.SCHEMA);
            referencia.put("intencao","aereo.regra_tarifaria");
            raiz.set("ajudaContextual",referencia);
            return mapper.writeValueAsString(raiz);
        }catch(Exception ex){throw new IllegalStateException("Falha ao registrar resultado de regras do copiloto.",ex);}
    }
}
