package com.confApi.chatconfianca.service;

import com.confApi.chatconfianca.dto.enums.*;
import com.confApi.chatconfianca.dto.model.Conversa;
import com.confApi.chatconfianca.dto.model.DepartamentoUnidade;
import com.confApi.chatconfianca.dto.model.Mensagem;
import com.confApi.chatconfianca.v2.ChatV2Capability;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;

/** Factual handoff snapshot. No model call, credential payload or inference of resolution. */
public final class ChatHandoffResumo {
    private ChatHandoffResumo() { }
    public record Resultado(String texto, String conteudoJson) { }
    private record Consulta(Long mensagemId,String intencao,String resultado,String descricao,String pergunta,String filtros) { }

    public static Resultado gerar(Conversa conversa,DepartamentoUnidade departamento,String motivo,
                                  List<Mensagem> mensagens,ObjectMapper mapper) {
        Objects.requireNonNull(conversa,"Conversa obrigatoria");
        Objects.requireNonNull(mapper,"Mapper obrigatorio");
        List<Mensagem> historico=(mensagens==null?List.<Mensagem>of():mensagens).stream()
                .filter(Objects::nonNull)
                .filter(m->Objects.equals(conversa.getId(),m.getConversaId()))
                .filter(m->m.getStatus()!=StatusMensagem.EXCLUIDA&&m.getExcluidaEm()==null)
                .filter(m->m.getVisibilidade()==VisibilidadeMensagem.PUBLICA||m.getVisibilidade()==null)
                .filter(m->m.getRemetenteTipo()==RemetenteTipo.BOT
                    ||m.getRemetenteTipo()==RemetenteTipo.USUARIO&&Objects.equals(m.getRemetenteCodgUsuario(),conversa.getSolicitanteCodgUsuario()))
                .sorted(Comparator.comparing(Mensagem::getEnviadaEm,Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Mensagem::getId,Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        Mensagem primeira=null,ultima=null,ultimoPedido=null,ultimoBot=null;
        List<Consulta> consultas=new ArrayList<>();
        for(Mensagem m:historico) {
            if(m.getRemetenteTipo()==RemetenteTipo.USUARIO&&!limpar(m.getConteudo(),360).isBlank()) {
                if(primeira==null)primeira=m;
                ultima=m;if(!somenteHandoff(m.getConteudo()))ultimoPedido=m;
            } else if(m.getRemetenteTipo()==RemetenteTipo.BOT) {
                ultimoBot=m;Consulta consulta=consulta(m,conversa,mapper);if(consulta!=null)consultas.add(consulta);
            }
        }
        String pedido=limpar(ultimoPedido!=null?ultimoPedido.getConteudo():ultima!=null?ultima.getConteudo():conversa.getDescricaoInicial(),360);
        String pendencia="Atendimento humano solicitado. A resolução do pedido ainda precisa ser confirmada com o cliente.";
        Consulta ultimaConsulta=consultas.isEmpty()?null:consultas.get(consultas.size()-1);
        if(ultimaConsulta!=null&&"AGUARDANDO_DADOS".equals(ultimaConsulta.resultado())&&!ultimaConsulta.pergunta().isBlank())
            pendencia="Informação pendente na última consulta: "+ultimaConsulta.pergunta();
        else if(ultimaConsulta!=null&&ultimaConsulta.resultado().startsWith("ERRO"))
            pendencia="A última consulta registrada falhou. Verificar a integração e o pedido com o cliente; não considerar ausência de dados.";
        else if(ultimaConsulta!=null&&"SEM_CONHECIMENTO".equals(ultimaConsulta.resultado()))
            pendencia="A IA não encontrou uma fonte publicada suficiente. Validar a orientação com a equipe responsável.";
        else if(ultimaConsulta!=null&&"ACAO_PREPARADA".equals(ultimaConsulta.resultado()))
            pendencia="Foi preparada uma ação/simulação. Isso não confirma emissão, pagamento, cancelamento ou remarcação efetivada.";

        StringBuilder texto=new StringBuilder("Resumo ConfIA para atendimento humano\n");
        linha(texto,"Departamento escolhido",departamento==null?null:departamento.getNomeExibicao(),100);
        linha(texto,"Protocolo",conversa.getProtocolo(),60);
        linha(texto,"Pedido informado pelo cliente",pedido,360);
        linha(texto,"Motivo do encaminhamento",motivo,220);
        linha(texto,"Mensagem inicial do cliente",primeira==null?null:primeira.getConteudo(),200);
        if(ultima!=ultimoPedido)linha(texto,"Última mensagem do cliente",ultima==null?null:ultima.getConteudo(),160);
        texto.append("\nConsultas e tentativas registradas:\n");
        List<Consulta> recentes=consultas.subList(Math.max(0,consultas.size()-6),consultas.size());
        if(recentes.isEmpty())texto.append("- Não há resultado técnico estruturado disponível. As mensagens, sozinhas, não comprovam execução ou resolução.\n");
        else for(Consulta c:recentes) {
            texto.append("- ").append(c.descricao()).append(": ").append(descreverResultado(c.resultado())).append('\n');
            if(!c.filtros().isBlank())texto.append("  Filtros informados: ").append(c.filtros()).append('\n');
        }
        linha(texto,"Pendente para a equipe",pendencia,320);
        linha(texto,"Última resposta da ConfIA (trecho, não comprovação de execução)",ultimoBot==null?null:ultimoBot.getConteudo(),280);
        if(mensagens==null)texto.append("\nResumo parcial: não foi possível carregar o histórico. Confirme o pedido com o cliente.\n");
        else if(historico.isEmpty())texto.append("\nNão há mensagens públicas válidas disponíveis para este resumo.\n");
        texto.append("\nResumo automático baseado nos registros disponíveis, sem reexecutar consultas. Leia o histórico para detalhes e confirme a situação atual antes de qualquer ação.");

        Map<String,Object> dados=new LinkedHashMap<>();
        dados.put("schema","chat.handoff-resumo.v1");dados.put("geradoEm",LocalDateTime.now().toString());
        dados.put("departamentoUnidadeId",departamento==null?null:departamento.getId());
        dados.put("historicoDisponivel",mensagens!=null);dados.put("pedido",pedido);dados.put("pendencia",pendencia);
        List<Map<String,Object>> registros=new ArrayList<>();
        for(Consulta c:recentes) {
            Map<String,Object> r=new LinkedHashMap<>();r.put("mensagemId",c.mensagemId());
            r.put("intencao",c.intencao());r.put("resultado",c.resultado());
            r.put("descricao",c.descricao());r.put("filtros",c.filtros());registros.add(r);
        }
        dados.put("consultas",registros);
        String json;
        try {json=mapper.writeValueAsString(dados);}catch(Exception ignored) {json="{\"schema\":\"chat.handoff-resumo.v1\",\"historicoDisponivel\":false}";}
        return new Resultado(limitar(texto.toString(),3800),json);
    }

    private static Consulta consulta(Mensagem mensagem,Conversa conversa,ObjectMapper mapper) {
        if(mensagem.getConteudoJson()==null||mensagem.getConteudoJson().length()>250_000)return null;
        try {
            JsonNode root=mapper.readTree(mensagem.getConteudoJson());if(root==null||!root.isObject())return null;
            Consulta planoNaoComprovado=null;
            JsonNode v2=root.path("confiaV2");
            if(v2.isObject()&&identidadeCompativel(v2,conversa)) {
                ChatV2Capability cap=ChatV2Capability.from(v2.path("intencao").asText());
                if(cap!=null&&cap!=ChatV2Capability.SAUDACAO&&cap!=ChatV2Capability.HUMANO) {
                    String resultado=resultado(v2.path("resultado").asText());
                    Consulta planejada=new Consulta(mensagem.getId(),cap.code,resultado,descrever(cap),
                            limpar(v2.path("pergunta").asText(""),220),filtros(v2.path("parametros")));
                    if(!v2.path("legado").asBoolean(false)&&!"NAO_COMPROVADO".equals(resultado))return planejada;
                    planoNaoComprovado=new Consulta(mensagem.getId(),cap.code,"NAO_COMPROVADO",descrever(cap),"",planejada.filtros());
                }
            }
            JsonNode faturas=root.path("consultaFaturas");
            if(faturas.isObject()&&identidadeCompativel(faturas,conversa)
                    &&(!faturas.has("conversa")||faturas.path("conversa").asLong()!=0&&faturas.path("conversa").asLong()==conversa.getId()))
                return new Consulta(mensagem.getId(),"financeiro.faturas",resultado(faturas.path("statusConsulta").asText()),"Faturas/boletos",
                        "",filtros(faturas.path("parametros")));
            if(root.path("reservasRecentes").isObject()) {
                JsonNode dados=root.path("reservasRecentes");String status=dados.path("statusConsulta").asText(dados.path("status").asText());
                if("OK".equals(status))status=dados.path("reservas").isArray()&&dados.path("reservas").isEmpty()?"SEM_RESULTADO":"DADOS_CONSULTADOS";
                return new Consulta(mensagem.getId(),"aereo.reservas_recentes",resultado(status),"Reservas aéreas recentes","","");
            }
            return planoNaoComprovado;
        }catch(Exception ignored) { /* A malformed legacy payload is not evidence of execution. */ }
        return null;
    }
    private static boolean identidadeCompativel(JsonNode n,Conversa c) {
        return (!n.hasNonNull("agencia")||c.getCodgAgencia()!=null&&n.path("agencia").asInt()==c.getCodgAgencia())
                &&(!n.hasNonNull("usuario")||c.getSolicitanteCodgUsuario()!=null&&n.path("usuario").asInt()==c.getSolicitanteCodgUsuario());
    }
    private static String resultado(String valor) {
        if("ERRO".equals(valor)||"ERROR".equals(valor))return "ERRO_INTEGRACAO";
        if("NAO_ENCONTRADA".equals(valor))return "SEM_RESULTADO";
        return Set.of("DADOS_CONSULTADOS","SEM_RESULTADO","ERRO_INTEGRACAO","ERRO_CONSULTA","CONSULTA_BLOQUEADA",
                "AGUARDANDO_DADOS","ACAO_PREPARADA","PESQUISA_PREPARADA","RESPONDIDO_COM_FONTE","SEM_CONHECIMENTO",
                "CACHE_SEM_DADOS","CAPACIDADE_INDISPONIVEL").contains(valor)?valor:"NAO_COMPROVADO";
    }
    private static String descreverResultado(String r) {
        return switch(r) {
            case "DADOS_CONSULTADOS" -> "dados consultados; isso não confirma resolução do pedido.";
            case "SEM_RESULTADO" -> "sem resultados nos filtros consultados, não ausência geral de registros.";
            case "ERRO_INTEGRACAO","ERRO_CONSULTA" -> "falha técnica; resultado não obtido.";
            case "CONSULTA_BLOQUEADA" -> "consulta bloqueada; não concluir inexistência de dados.";
            case "AGUARDANDO_DADOS" -> "aguardando informação; consulta não concluída.";
            case "ACAO_PREPARADA" -> "ação/simulação preparada, sem confirmação de execução.";
            case "PESQUISA_PREPARADA" -> "pesquisa preparada para abrir no portal; resultado posterior não confirmado.";
            case "RESPONDIDO_COM_FONTE" -> "orientação respondida com fonte cadastrada.";
            case "SEM_CONHECIMENTO" -> "fonte publicada insuficiente para responder.";
            case "CACHE_SEM_DADOS" -> "sem dados no cache, não confirmação de indisponibilidade.";
            case "CAPACIDADE_INDISPONIVEL" -> "funcionalidade indisponível nessa consulta.";
            default -> "execução não comprovada pelos metadados disponíveis.";
        };
    }
    private static String descrever(ChatV2Capability c) {
        return switch(c) {
            case LIMITES -> "Limite de crédito";case FATURAS -> "Faturas";case BOLETOS -> "Boletos";
            case PAGAMENTO -> "Formas de pagamento";case BSP -> "Calendário BSP";case RESERVAS -> "Reservas aéreas recentes";
            case RESERVA -> "Dados da reserva aérea";case REGRAS -> "Regras da reserva aérea";
            case REMARCACAO -> "Simulação de remarcação";case ACOES_RESERVA -> "Ações da reserva";
            case CHECKIN -> "Próximos embarques";case ALERTAS -> "Alertas de tarifa";case FAMILIAS -> "Famílias tarifárias";
            case VOOS -> "Pesquisa de voos";case TARIFA_IDA -> "Melhores tarifas de ida";case TARIFA_VOLTA -> "Melhores tarifas de ida e volta";
            case HOTEL -> "Pesquisa de hotéis";case HOTEL_LISTA -> "Reservas de hotel";case HOTEL_RESERVA -> "Dados da reserva de hotel";
            case HOTEL_DADOS -> "Dados do hotel";case PACOTE -> "Pesquisa de pacotes";case CONTATOS -> "Contatos de departamentos";
            case HORARIO -> "Horários de atendimento";case EMERGENCIA -> "Plantão emergencial";case TI -> "Suporte de acesso/portal";
            default -> "Orientação geral";
        };
    }
    private static String filtros(JsonNode params) {
        if(!params.isObject())return "";
        Map<String,String> labels=new LinkedHashMap<>();
        labels.put("origem","origem");labels.put("destino","destino");labels.put("localizador","localizador");
        labels.put("faturaPagamento","situação");labels.put("faturaTipoData","tipo de data");labels.put("faturaInicio","início");labels.put("faturaFim","fim");
        labels.put("faturaProduto","produto");labels.put("faturaNumero","fatura");
        for(String k:List.of("dataIda","dataVolta","dataInicio","dataFim","mes","mesIda","mesVolta","checkin","checkout"))labels.put(k,k);
        labels.put("reservaHotelLocalizador","localizador hotel");labels.put("listaHotelInicio","início hotel");labels.put("listaHotelFim","fim hotel");
        List<String> values=new ArrayList<>();
        for(var e:labels.entrySet()) {
            JsonNode v=params.get(e.getKey());if(v!=null&&v.isTextual()) {
                String valor=v.asText();
                if(valor.matches("[\\p{L}\\p{N} _/.-]{1,35}"))values.add(e.getValue()+"="+limpar(valor,35));
            }
        }
        return limitar(String.join("; ",values),180);
    }
    private static boolean somenteHandoff(String text) {
        String t=Normalizer.normalize(text==null?"":text,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);
        return t.matches("\\s*(?:(?:sim|ok)[,.! ]*)?(?:(?:quero|preciso|desejo|pode)\\s+)?(?:(?:falar|conversar)\\s+com\\s+)?(?:um\\s+|o\\s+)?(?:atendente|atendimento humano|humano)(?:\\s+por favor)?[.!? ]*");
    }
    private static void linha(StringBuilder texto,String label,String value,int max) {
        String limpo=limpar(value,max);if(!limpo.isBlank())texto.append(label).append(": ").append(limpo).append('\n');
    }
    private static String limpar(String value,int max) {
        if(value==null)return "";
        // An excerpt is for human reading only; do not repeat credentials or raw document payloads.
        String text=value.replaceAll("(?is)<[^>]*>"," ")
                .replaceAll("(?i)\\b(?:senha|password|token|authorization|api[_ -]?key)\\s*(?::|=|é|eh)\\s*[^\\s,;]+","[credencial omitida]")
                .replaceAll("(?i)\\bBearer\\s+[^\\s,;]+","[credencial omitida]")
                .replaceAll("(?<!\\d)\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}(?!\\d)","[documento omitido]")
                .replaceAll("(?<!\\d)(?:\\d[ -]?){12,18}\\d(?!\\d)","[número longo omitido]")
                .replaceAll("[\\p{Cntrl}\\s]+"," ").trim();
        if(text.startsWith("{")||text.startsWith("["))return "[conteúdo estruturado; consultar o histórico]";
        return limitar(text,max);
    }
    private static String limitar(String value,int max) {return value.length()<=max?value:value.substring(0,max-3).stripTrailing()+"...";}
}
