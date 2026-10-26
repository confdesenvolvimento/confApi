package com.confApi.chatconfianca.indicadores;

import com.confApi.chatconfianca.dto.enums.RemetenteTipo;
import com.confApi.chatconfianca.dto.enums.StatusMensagem;
import com.confApi.chatconfianca.dto.enums.TipoMensagem;
import com.confApi.chatconfianca.dto.model.Mensagem;
import com.confApi.chatgpt.dto.ChatResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Recupera criterios, nao resultados, somente do ultimo BOT factual persistido e do mesmo dono. */
public final class ChatVendaAereaIndicadoresContexto {
    public static final String SCHEMA = "chat.vendas-aereas.v1";
    private static final long TTL = Duration.ofMinutes(30).toMillis();
    private static final Set<String> ESTADOS = Set.of("DADOS_CONSULTADOS", "SEM_RESULTADO", "AGUARDANDO_DADOS");
    private static final Set<String> FOCOS = Set.of("RESUMO", "VALOR", "LOCALIZADORES", "BILHETES", "PASSAGEIROS", "DETALHES");
    private static final Set<String> STATUS_VALOR = Set.of("COMPLETO", "INCOMPLETO", "INDISPONIVEL_SEM_PERMISSAO", "INDISPONIVEL");
    private static final Set<String> MOTIVOS_VALOR = Set.of("NAO_SOLICITADO", "SEM_PERMISSAO", "DADOS_INCOMPLETOS", "FONTE_INDISPONIVEL");
    private static final String COMPOSICAO_VALOR = "DASHBOARD_TARIFA_EMBARQUE_DU_RAV_RC_COMBUSTIVEL_V1";
    private ChatVendaAereaIndicadoresContexto() { }

    public static VendaAereaIndicadoresEstado recuperar(List<Mensagem> historico, Long conversa, Long agencia,
                                                        Long usuario, ObjectMapper mapper) {
        if (historico == null || mapper == null || !positivo(conversa) || !positivo(agencia) || !positivo(usuario)) return null;
        long agora = System.currentTimeMillis();
        for (int i = historico.size()-1; i >= 0; i--) {
            Mensagem mensagem = historico.get(i);
            if (mensagem == null || mensagem.getExcluidaEm() != null || mensagem.getStatus() == StatusMensagem.EXCLUIDA) continue;
            if (!Objects.equals(conversa, mensagem.getConversaId())) return null;
            if (mensagem.getRemetenteTipo() == RemetenteTipo.USUARIO) {
                if (mensagem.getRemetenteCodgUsuario() == null || !Objects.equals(usuario, mensagem.getRemetenteCodgUsuario().longValue())) return null;
                String t = ChatVendaAereaIndicadoresParser.normalizar(mensagem.getConteudo());
                if (ChatVendaAereaIndicadoresParser.mudancaDeAssunto(t)
                        || t.matches(".*\\b(?:abra|abrir|carregue|carregar) (?:a |essa |esta )?reserva\\b.*")) return null;
                continue;
            }
            if (mensagem.getRemetenteTipo() != RemetenteTipo.BOT) {
                if (mensagem.getRemetenteTipo() != RemetenteTipo.SISTEMA || mensagem.getTipo() == TipoMensagem.TRANSFERENCIA
                        || mensagem.getTipo() == TipoMensagem.ENCERRAMENTO || interrompe(mensagem.getConteudo())) return null;
                continue;
            }
            JsonNode raiz = ler(mensagem.getConteudoJson(), mapper);
            JsonNode valor = raiz == null ? null : raiz.get("indicadoresVendas");
            // Outra resposta de trabalho nunca permite ressuscitar um total anterior.
            if (valor == null || !valor.isObject()) {
                if (!neutro(mensagem.getConteudo()) || outraTarefa(raiz)) return null;
                continue;
            }
            if (!SCHEMA.equals(valor.path("schema").asText()) || !ESTADOS.contains(valor.path("statusConsulta").asText())
                    || interrompe(mensagem.getConteudo())) return null;
            JsonNode c = valor.path("contexto");
            if (!Objects.equals(conversa, inteiro(c.get("conversa"))) || !Objects.equals(agencia, inteiro(c.get("agencia")))
                    || !Objects.equals(usuario, inteiro(c.get("usuario")))) return null;
            Long instante = inteiro(c.get("atualizadoEm"));
            if (instante == null || instante > agora || agora-instante > TTL || mensagem.getEnviadaEm() == null
                    || mensagem.getEnviadaEm().isAfter(LocalDateTime.now().plusSeconds(5))
                    || mensagem.getEnviadaEm().isBefore(LocalDateTime.now().minusMinutes(30))) return null;
            try {
                var estado = mapper.treeToValue(c, VendaAereaIndicadoresEstado.class);
                if (!FOCOS.contains(estado.getFoco()==null?"":estado.getFoco())
                        || !validar(estado.getFiltros(), "AGUARDANDO_DADOS".equals(valor.path("statusConsulta").asText()))) return null;
                estado.setFiltros(ChatVendaAereaIndicadoresParser.copiar(estado.getFiltros()));
                return estado;
            } catch (Exception ignored) { return null; }
        }
        return null;
    }

    public static JsonNode payload(ChatResponseDTO resposta, ObjectMapper mapper) {
        if (resposta == null || resposta.history() == null || mapper == null) return null;
        for (int i=resposta.history().size()-1; i>=0; i--) {
            var m=resposta.history().get(i);
            if(m==null||!"system".equals(m.role()))continue;
            JsonNode n=ler(m.content(),mapper);
            if(n!=null&&n.isObject()&&SCHEMA.equals(n.path("schema").asText()))return n;
        }
        return null;
    }

    public static String anexar(String existente, ChatResponseDTO resposta, Long conversa, ObjectMapper mapper) {
        JsonNode payload=payload(resposta,mapper);
        if(payload==null)return existente;
        try {
            JsonNode original=ler(existente,mapper);
            ObjectNode raiz=original instanceof ObjectNode?(ObjectNode)original:mapper.createObjectNode();
            ObjectNode copia=mapper.createObjectNode();
            String status=payload.path("statusConsulta").asText();
            copia.put("schema",SCHEMA);copia.put("statusConsulta",status);
            copia.put("resultadoTarefa",payload.path("resultadoTarefa").asText(status));
            copiarEstadoFinanceiro(payload,copia);
            if(ESTADOS.contains(status)) {
                var estado=mapper.treeToValue(payload.path("contexto"),VendaAereaIndicadoresEstado.class);
                if(!positivo(conversa)||!Objects.equals(conversa,estado.getConversa())||!positivo(estado.getAgencia())
                        ||!positivo(estado.getUsuario())||estado.getAtualizadoEm()==null
                        ||!FOCOS.contains(estado.getFoco()==null?"":estado.getFoco())
                        ||!validar(estado.getFiltros(),"AGUARDANDO_DADOS".equals(status)))
                    throw new IllegalArgumentException("Criterios de vendas nao pertencem a esta conversa.");
                estado.setFiltros(ChatVendaAereaIndicadoresParser.copiar(estado.getFiltros()));
                copia.set("contexto",mapper.valueToTree(estado));
            }
            // Falhas persistem apenas uma barreira: nao recuperam dados nem criterios de uma consulta antiga.
            raiz.set("indicadoresVendas",copia);
            return mapper.writeValueAsString(raiz);
        } catch(Exception ex) { throw new IllegalStateException("Nao foi possivel persistir criterios da consulta gerencial.",ex); }
    }

    /** Flags de completude, nunca quantias, moedas, totais ou nomes retornados pela consulta. */
    private static void copiarEstadoFinanceiro(JsonNode origem,ObjectNode destino) {
        String status=codigo(origem,"statusValor",STATUS_VALOR);
        String motivo=codigo(origem,"motivoValor",MOTIVOS_VALOR);
        String composicao=codigo(origem,"composicaoValor",Set.of(COMPOSICAO_VALOR));
        JsonNode contador=origem.get("passageirosValorIncompleto");
        boolean temContador=contador!=null&&!contador.isNull();
        if(status==null) {
            if(motivo!=null||composicao!=null||temContador)throw new IllegalArgumentException("Estado financeiro incompleto.");
            return; // AGUARDANDO_DADOS e respostas anteriores nao ganham um status financeiro inventado.
        }
        if(!temContador||!contador.isIntegralNumber()||!contador.canConvertToLong()||contador.longValue()<0)
            throw new IllegalArgumentException("Contagem de completude financeira invalida.");
        long incompletos=contador.longValue();
        boolean coerente=switch(status) {
            case "COMPLETO" -> motivo==null&&COMPOSICAO_VALOR.equals(composicao)&&incompletos==0;
            case "INCOMPLETO" -> "DADOS_INCOMPLETOS".equals(motivo)&&COMPOSICAO_VALOR.equals(composicao);
            case "INDISPONIVEL_SEM_PERMISSAO" -> "SEM_PERMISSAO".equals(motivo)&&composicao==null&&incompletos==0;
            case "INDISPONIVEL" -> Set.of("NAO_SOLICITADO","FONTE_INDISPONIVEL").contains(motivo==null?"":motivo)
                    &&composicao==null&&incompletos==0;
            default -> false;
        };
        if(!coerente)throw new IllegalArgumentException("Estado financeiro inconsistente.");
        destino.put("statusValor",status);
        if(motivo!=null)destino.put("motivoValor",motivo);
        if(composicao!=null)destino.put("composicaoValor",composicao);
        destino.put("passageirosValorIncompleto",incompletos);
    }
    private static String codigo(JsonNode origem,String campo,Set<String> permitidos) {
        JsonNode valor=origem.get(campo);
        if(valor==null||valor.isNull())return null;
        if(!valor.isTextual()||!permitidos.contains(valor.textValue()))throw new IllegalArgumentException("Codigo financeiro invalido.");
        return valor.textValue();
    }

    private static boolean validar(VendaAereaIndicadoresFiltros f, boolean aguardando) {
        if(f==null||f.getPage()==null||f.getPage()<0||f.getPage()>10000||f.getSize()==null||f.getSize()<1||f.getSize()>50
                ||!Set.of("NENHUM","COMPANHIA").contains(f.getAgrupamento()==null?"":f.getAgrupamento()))return false;
        for(String valor:List.of(seguro(f.getOrigem()),seguro(f.getDestino()),seguro(f.getCompanhia()),seguro(f.getNomePassageiro())))
            if(valor.length()>100||valor.matches("(?s).*[\\p{Cntrl}].*"))return false;
        if(f.getInicio()==null||f.getFim()==null)return aguardando&&f.getInicio()==null&&f.getFim()==null;
        return !f.getFim().isBefore(f.getInicio())&&ChronoUnit.DAYS.between(f.getInicio(),f.getFim())+1<=366;
    }
    private static boolean outraTarefa(JsonNode n) {
        if(n==null)return false;
        return n.has("consultaReservas")||n.has("ajudaContextual")||n.has("copilotoRegras")||n.has("confiaV2")
                ||n.has("decisaoIa")||n.has("observabilidade");
    }
    private static boolean neutro(String texto) {
        return ChatVendaAereaIndicadoresParser.normalizar(texto).matches("(?:de nada|por nada|disponha|obrigad[oa]|obrigad[oa] por avisar|ok|certo|estou a disposicao)[!. ]*");
    }
    private static boolean interrompe(String texto) {
        return ChatVendaAereaIndicadoresParser.normalizar(texto).matches(".*\\b(?:encerrad[oa]|transferid[oa]|atendente assumiu|atendimento humano iniciado)\\b.*");
    }
    private static String seguro(String v) { return v==null?"":v; }
    private static boolean positivo(Long v) { return v!=null&&v>0; }
    private static Long inteiro(JsonNode n) { return n!=null&&n.isIntegralNumber()&&n.canConvertToLong()?n.longValue():null; }
    private static JsonNode ler(String texto,ObjectMapper mapper) {
        if(texto==null||texto.isBlank()||texto.length()>1_000_000)return null;
        try{return mapper.readTree(texto);}catch(Exception ignored){return null;}
    }
}
