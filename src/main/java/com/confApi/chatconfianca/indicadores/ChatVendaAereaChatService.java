package com.confApi.chatconfianca.indicadores;

import com.confApi.chatconfianca.dto.model.Mensagem;
import com.confApi.chatconfianca.dto.request.PerguntarConfiaRequest;
import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.chatgpt.dto.ChatMessageDTO;
import com.confApi.chatgpt.dto.ChatResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Entrada factual comum aos clientes e motores; nao permite somar uma pagina como total. */
@Service
public class ChatVendaAereaChatService {
    private final ChatVendaAereaIndicadoresService consultas;
    private final ObjectMapper mapper;
    @Value("${chat-confianca.vendas-aereas.enabled:true}")
    private boolean enabled = true;

    public ChatVendaAereaChatService(ChatVendaAereaIndicadoresService consultas, ObjectMapper mapper) {
        this.consultas = consultas;
        this.mapper = mapper;
    }

    /** Reconhecer nao consulta a origem. Mesmo desligado, o pedido nao cai no executor antigo. */
    public boolean podeAtender(PerguntarConfiaRequest req, SessaoChatResponse sessao, List<Mensagem> historico) {
        return pedido(req, sessao, historico) != null;
    }

    public ChatResponseDTO tentar(PerguntarConfiaRequest req, SessaoChatResponse sessao,
            Long conversaId, List<Mensagem> historico) {
        var pedido = pedido(req, sessao, historico);
        if (pedido == null) return null;
        if (!enabled) return resposta(req, sessao, conversaId, null, "CAPACIDADE_INDISPONIVEL",
                "A consulta de indicadores de vendas aéreas está desabilitada. Não usei uma lista limitada como total de vendas.");
        if (req.getCodgUsuario() == null || req.getCodgUsuario() < 1 || conversaId == null || conversaId < 1
                || agencia(sessao) == null || agencia(sessao) < 1)
            return resposta(req, sessao, conversaId, null, "CONSULTA_BLOQUEADA",
                    "Não consegui validar a agência e a conversa para consultar suas vendas. Confira a sessão e tente novamente.");
        if (pedido.pergunta() != null && !pedido.pergunta().isBlank())
            return ajustarObjetivo(pedido,resposta(req, sessao, conversaId, pedido.filtros(), "AGUARDANDO_DADOS", pedido.pergunta()));
        // Intencao escolhe a leitura desejada; a permissao financeira sera revalidada na origem.
        pedido.filtros().setIncluirValores("VALOR".equals(pedido.foco())||"RESUMO".equals(pedido.foco()));
        // O servico revalida o vinculo de usuario/unidade/agencia antes de qualquer IO.
        try {
            var resultado=consultas.consultar(pedido.filtros(), sessao, req.getCodgUsuario(), req.getCodgAgenciaSessao(), conversaId);
            if (resultado == null) throw new IllegalStateException("Resultado ausente.");
            return ajustarObjetivo(pedido,resultado);
        } catch (RuntimeException ex) {
            java.util.logging.Logger.getLogger(getClass().getName()).warning("VENDAS_AEREAS_RESPOSTA_ERRO tipo="+ex.getClass().getSimpleName());
            return resposta(req,sessao,conversaId,null,"ERRO_INTEGRACAO",
                    "Não consegui concluir a consulta das vendas agora. Isso não significa que o total seja zero. Tente novamente ou solicite atendimento.");
        }
    }

    private ChatVendaAereaIndicadoresParser.Pedido pedido(PerguntarConfiaRequest req,
            SessaoChatResponse sessao, List<Mensagem> historico) {
        if (req == null) return null;
        Integer codigoAgencia = agencia(sessao);
        var anterior = ChatVendaAereaIndicadoresContexto.recuperar(historico, req.getConversaId(),
                codigoAgencia == null ? null : codigoAgencia.longValue(),
                req.getCodgUsuario() == null ? null : req.getCodgUsuario().longValue(), mapper);
        return ChatVendaAereaIndicadoresParser.interpretar(req.getMensagem(), anterior);
    }

    private Integer agencia(SessaoChatResponse sessao) {
        return sessao == null || sessao.getAgencia() == null ? null : sessao.getAgencia().getCodgAgencia();
    }

    private ChatResponseDTO ajustarObjetivo(ChatVendaAereaIndicadoresParser.Pedido pedido, ChatResponseDTO resposta) {
        var payload=ChatVendaAereaIndicadoresContexto.payload(resposta,mapper);
        if (payload == null) throw new IllegalStateException("Resultado de vendas sem contrato factual.");
        // Contagens entregues nao comprovam valor financeiro completo ou permissao para ve-lo.
        ObjectNode parcial=((ObjectNode)payload).deepCopy();
        if(parcial.path("contexto").isObject()) ((ObjectNode)parcial.path("contexto")).put("foco",pedido.foco());
        boolean valorCompleto="COMPLETO".equals(payload.path("statusValor").asText())
                &&"DASHBOARD_TARIFA_EMBARQUE_DU_RAV_RC_COMBUSTIVEL_V1".equals(payload.path("composicaoValor").asText())
                &&payload.path("passageirosValorIncompleto").isIntegralNumber()
                &&payload.path("passageirosValorIncompleto").asLong(-1)==0;
        if(pedido.filtros().isIncluirValores()&&!valorCompleto&&"DADOS_CONSULTADOS".equals(payload.path("statusConsulta").asText()))
            parcial.put("resultadoTarefa","VALOR".equals(pedido.foco())?"VALOR_NAO_DISPONIVEL":"DADOS_PARCIAIS");
        var historico=new ArrayList<ChatMessageDTO>();
        for (var item:resposta.history()) {
            if (item!=null&&"system".equals(item.role())) try {
                var json=mapper.readTree(item.content());
                if (ChatVendaAereaIndicadoresContexto.SCHEMA.equals(json.path("schema").asText())) {
                    historico.add(new ChatMessageDTO("system",mapper.writeValueAsString(parcial)));
                    continue;
                }
            } catch (Exception ex) { throw new IllegalStateException("Resultado estruturado de vendas invalido.",ex); }
            historico.add(item);
        }
        return new ChatResponseDTO(resposta.id(),resposta.content(),resposta.toolCalls(),resposta.audio(),resposta.keywords(),historico,resposta.actions());
    }

    private ChatResponseDTO resposta(PerguntarConfiaRequest req, SessaoChatResponse sessao, Long conversa,
            VendaAereaIndicadoresFiltros filtros, String status, String texto) {
        try {
            var json = mapper.createObjectNode();
            json.put("schema", ChatVendaAereaIndicadoresContexto.SCHEMA);
            json.put("statusConsulta", status);
            json.put("resultadoTarefa", status);
            if (filtros != null) {
                var estado = new VendaAereaIndicadoresEstado();
                estado.setConversa(conversa);
                estado.setAgencia(agencia(sessao).longValue());
                estado.setUsuario(req.getCodgUsuario().longValue());
                estado.setAtualizadoEm(System.currentTimeMillis());
                estado.setFiltros(filtros);
                json.set("contexto", mapper.valueToTree(estado));
            }
            var barreira = mapper.createObjectNode();
            barreira.put("schema", "chat.reservas-consulta.v1");
            barreira.put("statusConsulta", "CONSULTA_BLOQUEADA");
            barreira.put("resultadoTarefa", status);
            return new ChatResponseDTO(null, texto, List.of(), null, List.of("aereo.vendas_indicadores"),
                    List.of(new ChatMessageDTO("system", mapper.writeValueAsString(json)),
                            new ChatMessageDTO("system", mapper.writeValueAsString(barreira))), List.of());
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao preparar o resultado dos indicadores de vendas.", ex);
        }
    }
}
