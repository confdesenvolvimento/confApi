package com.confApi.chatconfianca.v2;

import com.confApi.chatgpt.util.LocalizadorAereo;

import com.confApi.chatconfianca.dto.model.Conversa;
import com.confApi.chatconfianca.dto.model.Mensagem;
import com.confApi.chatconfianca.dto.enums.RemetenteTipo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ChatV2Planner {
    private final ChatV2Properties properties;
    private final ChatV2SemanticClient semantic;
    private final ObjectMapper mapper;
    public ChatV2Planner(ChatV2Properties properties, ChatV2SemanticClient semantic, ObjectMapper mapper) {
        this.properties=properties; this.semantic=semantic; this.mapper=mapper;
    }

    public ChatV2Plan planejar(String mensagem, Conversa conversa, List<Mensagem> historico,
                              Integer agencia, Integer usuario) {
        if(!properties.participa(agencia,usuario))return null;
        ChatV2Plan anterior=contexto(conversa,agencia,usuario);
        String t=normalizar(mensagem);
        ChatV2Plan p=com.confApi.chatconfianca.hotel.ChatIaHotelListaContexto.comando(mensagem,anterior);
        if(p==null)p=com.confApi.chatconfianca.hotel.ChatIaHotelReservaContexto.escolher(mensagem,anterior);
        if(p==null)p=com.confApi.chatconfianca.hotel.ChatIaHotelContexto.escolher(mensagem,anterior);
        if(p==null)p=acaoPersistida(mensagem,historico,anterior);
        if(p==null) {
            ChatV2Capability c=identificar(t);
            boolean continua=anterior!=null && (c==anterior.capability() || (c==null&&continuacao(t,anterior)));
            if(c==null&&continua)c=anterior.capability();
            p=c==null?null:ChatV2Plan.of(c);
            if(p!=null&&continua) { p.getParametros().putAll(anterior.getParametros()); p.setContinuar(true); p.setFonte("V2_CONTEXTO"); }
            if(p!=null)extrairLocais(p,mensagem);
            boolean precisaSemantica=p==null || p.capability().tool!=null || !comandoInequivoco(t,p);
            if(precisaSemantica&&properties.isSemanticEnabled()) {
                try {
                    ChatV2Plan interpretado=semantic.decidir(mensagem,anterior,LocalDate.now());
                    // Semantic output is a complete snapshot. Null explicitly clears a slot.
                    // Merging here would resurrect cancelled dates/filters from the preceding turn.
                    if(anterior==null)interpretado.setContinuar(false);
                    p=interpretado;
                } catch(Exception ex) {
                    if(p==null) {
                        p=ChatV2Plan.of(ChatV2Capability.AJUDA);
                        p.setLegado(properties.isLegacyFallbackEnabled());
                    }
                    p.setErro("PLANEJADOR_INDISPONIVEL");
                    p.setFonte("V2_FALLBACK_CONTROLADO");
                    if(p.capability().tool!=null)p.setPergunta("Nao consegui interpretar os parametros da viagem agora. Pode confirmar a rota e o periodo?");
                    else if(!p.isLegado())p.setPergunta("Nao consegui interpretar esse pedido com seguranca agora. Pode reformular ou pedir atendimento humano?");
                }
            }
        }
        if(p==null) {p=ChatV2Plan.of(ChatV2Capability.AJUDA);p.setPergunta("Sobre qual assunto voce precisa de ajuda: financeiro, reservas, voos, hoteis ou atendimento?");}
        if(p.capability()==ChatV2Capability.HUMANO&&anterior!=null) {
            p.setAssuntoHandoff(anterior.capability()==ChatV2Capability.HUMANO?anterior.getAssuntoHandoff():anterior.getIntencao());
        }

        boolean contextoHotel=anterior!=null&&(anterior.capability()==ChatV2Capability.HOTEL_RESERVA||anterior.capability()==ChatV2Capability.HOTEL_LISTA)
                &&!t.matches(".*\\b(aereo|aerea|voo|voos|passagem|passagens|financeiro|boleto|boletos|fatura|faturas|seguro|carro|pacote)\\b.*");
        boolean mencionaHotel=t.matches(".*\\b(hotel|hoteis|hospedagem|hospedagens)\\b.*");
        if((mencionaHotel||contextoHotel)&&p.capability()!=ChatV2Capability.HUMANO) {
            if(t.matches(".*\\b(voucher|cancelar|cancele|cancelamento|reembolsar|reembolso|remarcar|remarque|alterar|altere|regras)\\b.*")) {
                p=ChatV2Plan.of(ChatV2Capability.AJUDA);
                p.setPergunta("Posso ler os dados cadastrados da reserva de hotel, mas alterações, voucher e regras da tarifa reservada precisam ser verificados no portal ou com atendimento humano. Como prefere continuar?");
            } else if(mencionaHotel&&t.matches(".*\\b(reserva|reservas|localizador)\\b.*")
                    &&p.capability()!=ChatV2Capability.HOTEL_RESERVA&&p.capability()!=ChatV2Capability.HOTEL_LISTA&&p.capability()!=ChatV2Capability.HOTEL) {
                p=ChatV2Plan.of(ChatV2Capability.HOTEL_RESERVA);
                p.setPergunta("Qual o localizador da reserva de hotel, nome do hóspede ou nome do hotel?");
            } else if(contextoHotel&&p.isContinuar()&&p.capability().usaLocalizador()) {
                p=ChatV2Plan.of(ChatV2Capability.HOTEL_RESERVA);
                p.setPergunta("Confirme o localizador da reserva de hotel que deseja consultar.");
            }
        }
        // A hotel locator must occur in this turn or a validated HOTEL reservation context, never an air context.
        if(p.capability()==ChatV2Capability.HOTEL_RESERVA) {
            String loc=p.getParametros().get("reservaHotelLocalizador");
            boolean informado=loc!=null&&Pattern.compile("(?i)(?<![\\p{L}\\p{N}])"+Pattern.quote(loc)+"(?![\\p{L}\\p{N}])").matcher(mensagem).find();
            boolean retido=anterior!=null&&anterior.capability()==p.capability()&&p.isContinuar()
                    &&Objects.equals(loc,anterior.getParametros().get("reservaHotelLocalizador"));
            if(loc!=null&&!informado&&!retido){p.getParametros().remove("reservaHotelLocalizador");p.setPergunta(null);}
        }
        if((mencionaHotel||contextoHotel)&&p.capability()==ChatV2Capability.HOTEL_LISTA
                &&t.matches(".*\\b(confirmadas?|canceladas?|pendentes?)\\b.*")) {
            p.setPergunta("Ainda não posso filtrar reservas de hotel por status com segurança. Deseja consultar por período, hóspede, hotel ou cidade, sem esse filtro de status?");
        }
        if(!properties.permite(p.getIntencao())) { p.setLegado(true);p.setFonte("V2_FORA_ESCOPO"); }
        // Origin may never be inferred from agency/base. Require evidence in the request or retained state.
        if(p.capability()!=null && p.capability().tool!=null && p.capability()!=ChatV2Capability.HOTEL
                && (anterior==null || !p.isContinuar())
                && !t.matches(".*(\\bde\\b.+\\bpara\\b|saindo|origem).*")
                && !mensagem.matches("(?s).*\\b[A-Z]{3}\\s*(?:para|-|/|>)\\s*[A-Z]{3}\\b.*")) {
            p.getParametros().remove("origem");
        }
        restringirParametrosAoAssunto(p,mensagem);
        com.confApi.chatconfianca.hotel.ChatIaHotelContexto.restaurar(p,anterior);

        com.confApi.chatconfianca.hotel.ChatIaHotelReservaContexto.restaurar(p,anterior);
        com.confApi.chatconfianca.hotel.ChatIaHotelListaContexto.restaurar(p,anterior);
        p.setAgencia(agencia);p.setUsuario(usuario);p.setAtualizadoEm(System.currentTimeMillis());
        return p;
    }

    ChatV2Plan contexto(Conversa conversa,Integer agencia,Integer usuario) {
        try {
            if(conversa==null||conversa.getMetadadosJson()==null)return null;
            JsonNode n=mapper.readTree(conversa.getMetadadosJson()).path("confiaV2");
            if(!n.isObject())return null;
            ChatV2Plan p=mapper.treeToValue(n,ChatV2Plan.class);
            long age=System.currentTimeMillis()-p.getAtualizadoEm();
            return Objects.equals(agencia,p.getAgencia())&&Objects.equals(usuario,p.getUsuario())
                    && p.capability()!=null&&age>=0&&age<=Math.max(1,properties.getContextMinutes())*60_000L ? p:null;
        } catch(Exception ex) {return null;}
    }

    static ChatV2Capability identificar(String t) {
        if(t.matches(".*\\b(atendente|falar com alguem|atendimento humano)\\b.*"))return ChatV2Capability.HUMANO;
        if(t.matches("(oi|ola|bom dia|boa tarde|boa noite)( tudo bem)?[!?., ]*"))return ChatV2Capability.SAUDACAO;

        boolean hotel=t.matches(".*\\b(hotel|hoteis|hospedagem|hospedagens)\\b.*");
        if(hotel&&t.matches(".*\\b(voucher|cancelar|reembolsar)\\b.*"))return ChatV2Capability.AJUDA;
        if(hotel&&t.matches(".*\\b(reservas|hospedagens)\\b.*"))return ChatV2Capability.HOTEL_LISTA;
        if(hotel&&t.matches(".*\\b(reserva|reservas|localizador)\\b.*"))return ChatV2Capability.HOTEL_RESERVA;
        if(hotel&&t.matches(".*\\b(informacoes|informacao|dados|detalhes|descricao|endereco|telefone|contato|piscina|estacionamento|wifi|wi fi|academia|cafe|servicos|horario|check[ -]?in|check[ -]?out)\\b.*"))return ChatV2Capability.HOTEL_DADOS;
        if(t.matches(".*\\bbsp\\b.*"))return ChatV2Capability.BSP;
        if(t.matches(".*\\b(pacote|pacotes)\\b.*"))return ChatV2Capability.PACOTE;
        if(t.matches(".*\\b(reemissao|remarcar|remarcacao)\\b.*")&&!t.matches(".*\\b(regra|regras|multa|multas)\\b.*"))return ChatV2Capability.REMARCACAO;
        if(t.matches(".*\\b(regra|regras|multa|multas)\\b.*"))return ChatV2Capability.REGRAS;
        if(t.matches(".*\\b(preparar|cancelar|emitir|reembolsar|assentos|bilhetes|voucher)\\b.*"))return ChatV2Capability.ACOES_RESERVA;
        if(t.matches(".*\\b(ida e volta|retornando|voltando)\\b.*"))return ChatV2Capability.TARIFA_VOLTA;
        if(t.matches(".*\\b(voos?|passagens?|tarifas?|rota)\\b.*")) {
            if(t.matches(".*\\b(barat[a-z]*|menor|melhor[a-z]*)\\b.*"))return ChatV2Capability.TARIFA_IDA;
            if(t.matches(".*\\b(pesquise|pesquisar|buscar|busque|cotar|rota)\\b.*"))return ChatV2Capability.VOOS;
        }
        if(t.matches(".*\\b(fatura|faturas)\\b.*"))return ChatV2Capability.FATURAS;
        if(t.matches(".*\\b(boleto|boletos)\\b.*"))return ChatV2Capability.BOLETOS;
        if(t.matches("limites?[?!. ]*|.*\\b(limite de credito|meus limites|meu limite|credito disponivel)\\b.*"))return ChatV2Capability.LIMITES;
        if(t.matches(".*\\b(ultimas reservas|minhas reservas|ultimas vendas|reservas recentes|liste a reserva)\\b.*"))return ChatV2Capability.RESERVAS;

        if(t.matches(".*\\b(localizador|(?:abrir|abra|abre)(?: a)? reserva|detalhes da reserva)\\b.*"))return ChatV2Capability.RESERVA;
        if(t.matches(".*\\b(familia|familias|bagagem)\\b.*"))return ChatV2Capability.FAMILIAS;
        if(t.matches(".*\\b(check[ -]?ins?|embarques)\\b.*")
                && !t.matches(".*\\b(hotel|hoteis|hospedagem|hospedagens)\\b.*"))return ChatV2Capability.CHECKIN;
        if(t.matches(".*\\b(alerta|alertas)\\b.*"))return ChatV2Capability.ALERTAS;
        if(t.matches(".*\\b(hotel|hoteis|hospedagem|hospedagens)\\b.*"))return ChatV2Capability.HOTEL;
        if(t.matches(".*\\b(emergencial|emergencia|plantao)\\b.*"))return ChatV2Capability.EMERGENCIA;
        if(t.matches(".*\\b(telefone|contato|contatos|email|e mail)\\b.*"))return ChatV2Capability.CONTATOS;
        if(t.matches(".*\\b(horario|funcionamento|que horas)\\b.*"))return ChatV2Capability.HORARIO;
        if(t.matches(".*\\b(erro no portal|suporte ti|nao consigo acessar|erro no aplicativo)\\b.*"))return ChatV2Capability.TI;
        if(t.matches(".*\\b(forma de pagamento|formas de pagamento)\\b.*"))return ChatV2Capability.PAGAMENTO;
        return null;
    }
    static boolean continuacao(String t,ChatV2Plan p) {
        return t.matches("^(e |sim\\b|nao\\b|cade\\b|como pego|onde baixar|me mande|me mostre as tarifas|pesquise entao|emissao |\\d).*")

                || (reservaComLocalizador(p)&&LocalizadorAereo.isValido(t))
                || (p.getPergunta()!=null&&t.length()<120);
    }
    private static boolean reservaComLocalizador(ChatV2Plan p) {
        return Set.of(ChatV2Capability.RESERVA,ChatV2Capability.REGRAS,ChatV2Capability.ACOES_RESERVA,ChatV2Capability.REMARCACAO).contains(p.capability());
    }
    private static boolean comandoInequivoco(String t,ChatV2Plan p) {
        if(p.capability()==ChatV2Capability.SAUDACAO||p.capability()==ChatV2Capability.HUMANO)return true;
        if(p.capability()==ChatV2Capability.CHECKIN && t.matches(
                "(?:quais (?:sao )?|(?:liste|listar|mostre|consultar|quero consultar) )?(?:os |meus |proximos )*(?:check[ -]?ins?|embarques)[? ]*"))return true;
        if(t.matches("(?:me mostre |quero consultar |consultar )?(?:meus? |minhas? )?(limites?|limite de credito|faturas?|boletos?|alertas?)[?!. ]*"))return true;
        if(t.matches("(?:liste |listar )?(?:minhas |as )?(?:ultimas reservas|reservas recentes)[?!. ]*"))return true;
        if(p.capability()==ChatV2Capability.BSP&&p.getParametros().containsKey("dataEmissao"))return true;

        if(p.capability()==ChatV2Capability.RESERVA && LocalizadorAereo.isValido(p.getParametros().get("localizador"))
                && t.matches("(?:abrir|abra|abre)(?: a)? reserva [a-z0-9]{5,13}[?!. ]*"))return true;
        if(p.isContinuar()&&reservaComLocalizador(p)&&LocalizadorAereo.isValido(t))return true;
        return p.isContinuar()&&t.matches("(e )?(no faturado[? ]*|como pego ela em pdf[? ]*|onde baixar[? ]*)");
    }
    /** Validate topic-specific arguments after semantics and again before executing. */
    static void restringirParametrosAoAssunto(ChatV2Plan p,String mensagem) {
        if(p==null || p.capability()==null)return;
        Map<String,String> params=new LinkedHashMap<>(p.getParametros()==null?Map.of():p.getParametros());
        if(!p.capability().usaLocalizador())params.remove("localizador");
        if(p.capability()==ChatV2Capability.HOTEL_DADOS) {
            params.keySet().retainAll(Set.of("hotelNome","hotelCidade","hotelPais","hotelOpcao"));
        } else {
            for(String key:List.of("hotelNome","hotelCidade","hotelPais","hotelOpcao"))params.remove(key);
            p.setHotelSelecionado(null);p.getHotelOpcoes().clear();p.setHotelConfiguracaoRevisao(null);
        }

        if(p.capability()==ChatV2Capability.HOTEL_RESERVA) {
            params.keySet().retainAll(com.confApi.chatconfianca.hotel.ChatIaHotelReservaContexto.PARAMETROS);
        } else {
            com.confApi.chatconfianca.hotel.ChatIaHotelReservaContexto.PARAMETROS.forEach(params::remove);
            p.getReservaHotelOpcoes().clear();p.setReservaHotelSelecionada(null);p.setReservaHotelConfiguracaoRevisao(null);
        }
        if(p.capability()==ChatV2Capability.HOTEL_LISTA)params.keySet().retainAll(com.confApi.chatconfianca.hotel.ChatIaHotelListaContexto.PARAMETROS);
        else {com.confApi.chatconfianca.hotel.ChatIaHotelListaContexto.PARAMETROS.forEach(params::remove);p.setListaHotelEstado(null);}
        if(p.capability()==ChatV2Capability.CONTATOS) {
            String t=normalizar(mensagem);
            List<String> setores=new ArrayList<>();
            for(String setor:List.of("financeiro","grupos","ti"))
                if(t.matches(".*\\b"+setor+"\\b.*") || (setor.equals("ti") && t.contains("t i")))setores.add(setor);
            // Preserve only a unique explicit sector in this turn, never merge old semantic slots.
            if(setores.size()==1)params.put("setor",setores.get(0));
        }
        p.setParametros(params);
    }

    static String normalizar(String t) {return Normalizer.normalize(t==null?"":t,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/?-]+"," ").trim();}
    static void extrairLocais(ChatV2Plan p,String mensagem) {
        String t=normalizar(mensagem);

        String localizador=LocalizadorAereo.extrair(mensagem);
        if(localizador!=null)p.getParametros().put("localizador",localizador);
        for(String companhia:List.of("latam","gol","azul"))if(t.matches(".*\\b"+companhia+"\\b.*"))p.getParametros().put("companhia",companhia.toUpperCase(Locale.ROOT));
        if(t.contains("faturado"))p.getParametros().put("modalidade","FATURADO");
        for(String setor:List.of("grupos","financeiro","ti"))if(t.matches(".*\\b"+setor+"\\b.*")||("ti".equals(setor)&&t.contains("t i")))p.getParametros().put("setor",setor);
        Matcher rota=Pattern.compile("\\b([A-Z]{3})\\s+(?:para|[-/>])\\s*([A-Z]{3})\\b").matcher(mensagem);
        if(rota.find()){p.getParametros().put("origem",rota.group(1));p.getParametros().put("destino",rota.group(2));}
        if(p.capability()==ChatV2Capability.BSP) {
            Matcher data=Pattern.compile("\\b(\\d{2})[/-](\\d{2})(?:[/-](\\d{4}))?\\b").matcher(mensagem);
            if(data.find())try {p.getParametros().put("dataEmissao",LocalDate.of(data.group(3)==null?LocalDate.now().getYear():Integer.parseInt(data.group(3)),Integer.parseInt(data.group(2)),Integer.parseInt(data.group(1))).toString());}catch(RuntimeException ex){p.setPergunta("Qual a data de emissao valida, no formato dia/mes/ano?");}
        }
    }

    private ChatV2Plan acaoPersistida(String mensagem,List<Mensagem> historico,ChatV2Plan anterior) {
        if(historico==null)return null;
        for(int i=historico.size()-1;i>=Math.max(0,historico.size()-10);i--) {
            Mensagem m=historico.get(i);
            if(m.getRemetenteTipo()!=RemetenteTipo.BOT || m.getExcluidaEm()!=null || m.getConteudoJson()==null)continue;
            try {
                JsonNode payload=mapper.readTree(m.getConteudoJson());
                for(JsonNode action:payload.path("actions")) {
                    if(!mensagem.trim().equals(action.path("prompt").asText().trim()))continue;
                    String code=action.path("code").asText();
                    ChatV2Capability c=code.contains("tarifas_ida_volta")?ChatV2Capability.TARIFA_VOLTA:
                        code.contains("tarifas")?ChatV2Capability.TARIFA_IDA:
                        code.equals("consultar_regras")?ChatV2Capability.REGRAS:
                        code.contains("remarcacao")?ChatV2Capability.REMARCACAO:
                        Set.of("abrir_reserva","preparar_emissao","preparar_cancelamento","preparar_reembolso","preparar_alteracao","consultar_bilhetes","preparar_reenvio_voucher","consultar_assentos","consultar_checkin").contains(code)?ChatV2Capability.ACOES_RESERVA:null;
                    if(c==null)return null;
                    if(c.tool!=null&&(anterior==null||anterior.capability()!=c))return null;
                    // An old button must not execute against the route of a newer search.
                    if(c.tool!=null && (!payload.path("confiaV2").isObject()
                            || !payload.path("confiaV2").path("parametros").equals(mapper.valueToTree(anterior.getParametros()))))return null;
                    ChatV2Plan p=ChatV2Plan.of(c);p.setFonte("V2_ACAO_PERSISTIDA");
                    if(anterior!=null&&anterior.capability()==c){p.setContinuar(true);p.getParametros().putAll(anterior.getParametros());}
                    String localizador=action.path("localizador").asText("");

                    if(LocalizadorAereo.isValido(localizador))p.getParametros().put("localizador",localizador.toUpperCase(Locale.ROOT));
                    if(code.contains("alternativas"))p.getParametros().put("modoResposta","alternativas");
                    if(code.contains("mesma_companhia"))p.getParametros().put("politicaCompanhia","mesma");
                    if(code.contains("companhias_diferentes"))p.getParametros().put("politicaCompanhia","diferentes");
                    if(code.contains("comparar_companhias"))p.getParametros().put("politicaCompanhia","comparar");
                    if(code.contains("mensal"))p.getParametros().put("modoResposta","mensal");
                    return p;
                }
            }catch(Exception ignored) { /* Invalid historical payload cannot become an executable action. */ }
        }
        return null;
    }
}
