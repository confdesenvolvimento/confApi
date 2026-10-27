package com.confApi.chatconfianca.reservas;

import com.confApi.chatconfianca.dto.model.Mensagem;
import com.confApi.chatconfianca.service.ChatConfiancaReservaAereaService;
import com.confApi.chatgpt.dto.ChatActionDTO;
import com.confApi.chatgpt.dto.ChatMessageDTO;
import com.confApi.chatgpt.dto.ChatResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Consulta factual comum a V1 e V2. Nunca executa emissao, cancelamento ou cobranca. */
@Service
public class ChatReservaConsultaService {
    private final ChatReservaConsultaClient client;
    private final ChatConfiancaReservaAereaService acoes;
    private final ObjectMapper mapper;
    @Value("${chat-confianca.reservas-consulta.enabled:true}")
    private boolean enabled=true;

    public ChatReservaConsultaService(ChatReservaConsultaClient client,ChatConfiancaReservaAereaService acoes,ObjectMapper mapper) {
        this.client=client;this.acoes=acoes;this.mapper=mapper;
    }

    public ChatResponseDTO tentar(String texto,Long conversa,Long agencia,Long usuario,List<Mensagem> historico) {
        if(!enabled)return null;
        ReservaConsultaEstado anterior=ChatReservaConsultaContexto.recuperar(historico,conversa,agencia,usuario,mapper);
        var pedido=interpretarConsulta(texto,anterior);
        if(pedido==null)return null;
        if(conversa==null||conversa<=0||agencia==null||agencia<=0||agencia>Integer.MAX_VALUE||usuario==null||usuario<=0)
            return responder("A agência autenticada não foi identificada. Não executei uma busca de reservas.","CONSULTA_BLOQUEADA",null,List.of());
        ReservaConsultaEstado estado=novoEstado(conversa,agencia,usuario);
        estado.setFiltros(copiar(pedido.filtros()));
        if(pedido.pergunta()!=null&&!pedido.pergunta().isBlank()) {
            if(anterior!=null)estado.setItens(anterior.getItens());
            estado.setSelecionadaId(null); // Uma pergunta de desambiguacao nunca confirma a selecao anterior.
            return responder(pedido.pergunta(),"AGUARDANDO_DADOS",estado,List.of());
        }
        try {
            ReservaConsultaPagina pagina=client.consultar(agencia.intValue(),copiar(pedido.filtros()));
            String prefixo="";
            boolean ampliada=false;
            ReservaConsultaFiltros f=pagina.getFiltrosAplicados();
            if(pedido.ampliarSeVazio()&&pagina.getTotalReservas()==0&&f.getNomePassageiro()!=null
                    &&(f.getOrigem()!=null||f.getDestino()!=null)) {
                var alternativa=copiar(f);alternativa.setOrigem(null);alternativa.setDestino(null);alternativa.setPage(0);
                prefixo="Não encontrei esse passageiro na rota consultada"
                        +(f.getDestino()==null?"":" para "+f.getDestino())+". ";
                // Somente a rota e removida. Agencia, nome, periodo e situacao permanecem iguais.
                pagina=client.consultar(agencia.intValue(),alternativa);ampliada=true;
                prefixo+=pagina.getTotalReservas()>0
                        ?"Encontrei reservas em outra rota, mantendo a mesma agência, período e situação:\n\n"
                        :"Também pesquisei outras rotas da mesma agência, mantendo o período e a situação.\n\n";
            }
            estado.setFiltros(copiar(pagina.getFiltrosAplicados()));
            estado.setItens(pagina.getReservas());
            boolean ultimaSingular=java.text.Normalizer.normalize(texto,java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).matches(".*\\bultim[oa]\\b.*")
                    &&pedido.filtros().getSize()==1&&pedido.filtros().getPage()==0;
            if(pagina.getReservas().size()==1&&(pagina.getTotalReservas()==1||ultimaSingular))
                estado.setSelecionadaId(pagina.getReservas().get(0).getReservaId());
            String resposta=prefixo+ChatReservaConsultaTexto.renderizar(pagina,pedido.foco());
            List<ChatActionDTO> actions=acoes==null?List.of():acoes.acoesParaConsulta(pagina.getReservas());
            return responder(resposta,pagina.getTotalReservas()==0?"SEM_RESULTADO":"DADOS_CONSULTADOS",estado,actions,
                    pagina,ampliada,pedido.filtros());
        } catch(Exception ex) {
            // Nao confundir falha/404/schema invalido com ausencia de reservas nem expor payload/PII.
            java.util.logging.Logger.getLogger(getClass().getName()).warning("CONSULTA_RESERVAS_ERRO tipo="+ex.getClass().getSimpleName());
            estado.setItens(List.of());estado.setSelecionadaId(null);
            return responder("Não consegui concluir a consulta de reservas agora. Isso não significa que não existam reservas. Tente novamente ou solicite atendimento.","ERRO_INTEGRACAO",estado,List.of());
        }
    }

    /** Mesma elegibilidade no planejamento e na execucao: codigo solto nao vira listagem. */
    public static ChatReservaConsultaParser.Pedido interpretarConsulta(String texto,ReservaConsultaEstado anterior) {
        String codigo=com.confApi.chatgpt.util.LocalizadorAereo.extrair(texto);
        if(anterior==null&&codigo!=null&&ChatReservaConsultaParser.normalizar(texto).replaceAll("[.!?]+$","")
                .equals(codigo.toLowerCase(Locale.ROOT)))return null;
        return ChatReservaConsultaParser.interpretar(texto,anterior);
    }

    private ReservaConsultaEstado novoEstado(Long conversa,Long agencia,Long usuario) {
        var e=new ReservaConsultaEstado();e.setConversa(conversa);e.setAgencia(agencia);e.setUsuario(usuario);
        e.setAtualizadoEm(System.currentTimeMillis());e.setItens(List.of());return e;
    }
    private ReservaConsultaFiltros copiar(ReservaConsultaFiltros filtros) {
        return filtros==null?new ReservaConsultaFiltros():mapper.convertValue(filtros,ReservaConsultaFiltros.class);
    }
    private ChatResponseDTO responder(String texto,String status,ReservaConsultaEstado estado,List<ChatActionDTO> actions) {
        return responder(texto,status,estado,actions,null,false,null);
    }
    private ChatResponseDTO responder(String texto,String status,ReservaConsultaEstado estado,List<ChatActionDTO> actions,
            ReservaConsultaPagina pagina,boolean ampliada,ReservaConsultaFiltros solicitados) {
        try {
            var json=mapper.createObjectNode();json.put("schema","chat.reservas-consulta.v1");
            json.put("statusConsulta",status);json.put("resultadoTarefa",status);
            if(estado!=null)json.set("contexto",mapper.valueToTree(estado));
            if(pagina!=null){json.put("totalReservas",pagina.getTotalReservas());json.put("totalLocalizadores",pagina.getTotalLocalizadores());
                json.put("page",pagina.getPage());json.put("size",pagina.getSize());json.put("hasNext",pagina.isHasNext());
                json.put("criterioContagem",pagina.getCriterioContagem());json.put("buscaAmpliada",ampliada);
                json.set("filtrosSolicitados",mapper.valueToTree(solicitados));}
            return new ChatResponseDTO(null,texto,List.of(),null,List.of("aereo.consulta_reservas"),
                    List.of(new ChatMessageDTO("system",mapper.writeValueAsString(json))),actions);
        }catch(Exception ex){throw new IllegalStateException("Falha ao preparar resposta da consulta de reservas.",ex);}
    }
}
