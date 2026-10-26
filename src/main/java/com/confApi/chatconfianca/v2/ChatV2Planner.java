package com.confApi.chatconfianca.v2;

import com.confApi.chatgpt.util.LocalizadorAereo;
import com.confApi.chatconfianca.financeiro.ChatFaturasFiltros;

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
        if(financeiroFaturas(anterior)&&!com.confApi.chatconfianca.financeiro.ChatFaturasContexto.permiteContinuidadeV2(historico,usuario,mapper))anterior=null;
        String t=normalizar(mensagem);
        ChatV2Plan p=continuarFaturas(mensagem,anterior);
        if(p==null)p=com.confApi.chatconfianca.hotel.ChatIaHotelListaContexto.comando(mensagem,anterior);
        if(p==null)p=com.confApi.chatconfianca.hotel.ChatIaHotelReservaContexto.escolher(mensagem,anterior);
        if(p==null)p=com.confApi.chatconfianca.hotel.ChatIaHotelContexto.escolher(mensagem,anterior);
        if(p==null)p=acaoPersistida(mensagem,historico,anterior);
        if(p==null)p=completarAeroportoPendente(mensagem,anterior);
        if(p==null)p=ajustarPeriodo(t,anterior);
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
                    completarCorrecaoDePeriodo(interpretado,anterior,t);
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
        // The generic reservations shortcut has always meant air reservations. Hotel listing
        // requires an explicit product or a valid hotel-reservation context, not a model guess.
        boolean listaReservas=pedidoListagemReservas(t);
        if(listaReservas&&!mencionaHotel&&(!contextoHotel||t.matches(".*\\b(aereo|aereas|aerea|voos|voo|passagens)\\b.*"))) {
            if(p.capability()==ChatV2Capability.HOTEL_LISTA) {
                String pergunta=p.getPergunta();
                p=ChatV2Plan.of(ChatV2Capability.RESERVAS);
                p.setFonte("V2_PRODUTO_VALIDADO");
                if(!perguntaDeHotel(pergunta))p.setPergunta(pergunta);
            }
            if(p.capability()==ChatV2Capability.RESERVAS&&perguntaDeHotel(p.getPergunta()))p.setPergunta(null);
        } else if(listaReservas&&(contextoHotel||mencionaHotel)&&p.capability()==ChatV2Capability.RESERVAS) {
            String pergunta=p.getPergunta();
            p=ChatV2Plan.of(ChatV2Capability.HOTEL_LISTA);
            p.setFonte("V2_PRODUTO_VALIDADO");
            if(!normalizar(pergunta).matches(".*\\b(aereo|aerea|aereas|voo|voos|passagem|passagens|localizador|companhia)\\b.*"))p.setPergunta(pergunta);
        }
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
        if(t.matches(".*\\b(faturas?|boletos?)\\b.*")&&!ChatFaturasFiltros.pedidoExplicito(mensagem)
                &&p.capability()!=ChatV2Capability.HUMANO) {
            if(t.matches(".*\\b(contato|telefone|whatsapp|email|e mail)\\b.*")) {
                p=ChatV2Plan.of(ChatV2Capability.CONTATOS);p.getParametros().put("setor","financeiro");
            } else {
                p=ChatV2Plan.of(ChatV2Capability.AJUDA);
                p.setPergunta("Posso consultar faturas e orientar o acesso aos documentos, mas não efetuo pagamento, baixa ou cancelamento. Deseja consultar ou falar com o atendimento financeiro?");
            }
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
        if(financeiroFaturas(p)) {
            p.setParametros(ChatFaturasFiltros.extrair(mensagem,p.getParametros(),LocalDate.now()));
            var consulta=ChatFaturasFiltros.validar(p.getParametros(),LocalDate.now());
            if(consulta.pergunta()!=null)p.setPergunta(consulta.pergunta());
        }
        retirarPerguntaDeRotaJaInformada(p);
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
        if(ChatFaturasFiltros.pedidoExplicito(t))return t.matches(".*\\bboletos?\\b.*")?ChatV2Capability.BOLETOS:ChatV2Capability.FATURAS;
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
        if(t.matches("limites?[?!. ]*|.*\\b(limite de credito|meus limites|meu limite|credito disponivel)\\b.*"))return ChatV2Capability.LIMITES;
        if(pedidoListagemReservas(t)||t.matches(".*\\b(ultimas vendas|liste a reserva)\\b.*"))return ChatV2Capability.RESERVAS;
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
    private static boolean pedidoListagemReservas(String t) {
        return t.matches(".*\\breservas\\b.*")
                &&t.matches(".*\\b(ultimas|recentes|minhas|liste|listar|listagem|mostre|mostrar|consultar|quais)\\b.*")
                &&!t.matches(".*\\b(cancelar|cancele|cancelamento|reembolsar|reembolso|remarcar|remarcacao|voucher|regras|emitir)\\b.*")
                &&!t.matches(".*\\b(carro|carros|seguro|seguros|pacote|pacotes)\\b.*");
    }
    private static boolean perguntaDeHotel(String pergunta) {
        String t=normalizar(pergunta);
        return t.matches(".*\\b(hotel|hoteis|hospedagem|hospede|hospedes|entrada|saida|check[ -]?out)\\b.*");
    }
    private static boolean pesquisaAerea(ChatV2Plan p) {
        return p!=null&&Set.of(ChatV2Capability.VOOS,ChatV2Capability.TARIFA_IDA,ChatV2Capability.TARIFA_VOLTA).contains(p.capability());
    }
    /** A narrow date-only answer can change the period, never the already authorized route. */
    private static boolean respostaSomentePeriodo(String t) {
        String meses="janeiro|fevereiro|marco|abril|maio|junho|julho|agosto|setembro|outubro|novembro|dezembro";
        if(!t.matches(".*(?:\\b(?:"+meses+")\\b|\\b20[0-9]{2}\\b|\\b[0-9]{1,2}[/-][0-9]{1,2}).*"))return false;
        return t.replaceAll("\\b(?:"+meses+"|agora|em|de|do|da|a|o|e|no|na|para|por|favor|ano|mes|dia|dias|ate|entre|corrigindo|correto|correta|isso|seria|sera)\\b","")
                .replaceAll("[0-9 /?\\-]","").isBlank();
    }
    private static void completarCorrecaoDePeriodo(ChatV2Plan atual,ChatV2Plan anterior,String texto) {
        if(!pesquisaAerea(anterior)||atual.capability()!=anterior.capability()||!respostaSomentePeriodo(texto))return;
        // Deliberately not a generic merge: null dates/months continue to mean removal.
        Map<String,String> params=new LinkedHashMap<>(atual.getParametros());
        for(String key:List.of("origem","destino","adt","cabine","politicaCompanhia","limiteAlternativas","limite")) {
            String value=anterior.getParametros().get(key);
            if(value!=null)params.put(key,value);
        }
        atual.setParametros(params);atual.setContinuar(true);
    }
    private static ChatV2Plan completarAeroportoPendente(String mensagem,ChatV2Plan anterior) {
        if(!pesquisaAerea(anterior)||anterior.getPergunta()==null)return null;
        String codigo=mensagem==null?"":mensagem.trim();
        // Only a literal IATA answer to one pending slot; free-form city names still use semantics.
        if(!codigo.matches("[A-Z]{3}")||Set.of("SIM","NAO","ANO","MES","IDA","VOO","OLA","BOM","POR","COM","DAS","DOS","SEU","SUA","QUE","TEM","SEM","UMA").contains(codigo))return null;
        String pergunta=normalizar(anterior.getPergunta());
        boolean origem=pergunta.matches(".*\\borigem\\b.*"),destino=pergunta.matches(".*\\bdestino\\b.*");
        if(origem==destino)return null;
        String key=origem?"origem":"destino",old=anterior.getParametros().get(key);
        if(old!=null&&!old.equals(codigo))return null; // An actual route change must be interpreted as a new request.
        ChatV2Plan p=ChatV2Plan.of(anterior.capability());p.setParametros(new LinkedHashMap<>(anterior.getParametros()));
        p.getParametros().put(key,codigo);p.setContinuar(true);p.setFonte("V2_CAMPO_PENDENTE");return p;
    }
    private static ChatV2Plan ajustarPeriodo(String texto,ChatV2Plan anterior) {
        if(!pesquisaAerea(anterior))return null;
        List<String> datas=List.of("dataIda","dataVolta","dataInicio","dataFim","dataIdaInicio","dataIdaFim","dataVoltaInicio","dataVoltaFim");
        List<String> meses=List.of("mes","mesIda","mesVolta");
        if(anterior.capability()!=ChatV2Capability.VOOS&&texto.matches("(?:em |para |agora )?(?:todos os meses|qualquer mes|qualquer periodo|sem (?:filtro de )?periodo)[? ]*")) {
            Map<String,String> params=new LinkedHashMap<>(anterior.getParametros());
            datas.forEach(params::remove);meses.forEach(params::remove);
            ChatV2Plan p=ChatV2Plan.of(anterior.capability());p.setParametros(params);p.setContinuar(true);p.setFonte("V2_CORRECAO_PERIODO");return p;
        }
        Matcher ano=Pattern.compile("(?:o )?(?:ano (?:e |correto e )?)?(?:para |em )?(20[0-9]{2})[? ]*").matcher(texto);
        if(!ano.matches())return null;
        Map<String,String> params=new LinkedHashMap<>(anterior.getParametros());
        try {
            int base=Integer.MAX_VALUE;
            for(String key:datas)if(params.containsKey(key))base=Math.min(base,LocalDate.parse(params.get(key)).getYear());
            for(String key:meses)if(params.containsKey(key))base=Math.min(base,java.time.YearMonth.parse(params.get(key)).getYear());
            if(base==Integer.MAX_VALUE)return null;
            int delta=Integer.parseInt(ano.group(1))-base;
            for(String key:datas)if(params.containsKey(key)) {
                LocalDate date=LocalDate.parse(params.get(key));
                // Do not silently turn Feb 29 into Feb 28 when correcting a year.
                params.put(key,LocalDate.of(date.getYear()+delta,date.getMonthValue(),date.getDayOfMonth()).toString());
            }
            for(String key:meses)if(params.containsKey(key))params.put(key,java.time.YearMonth.parse(params.get(key)).plusYears(delta).toString());
        }catch(RuntimeException ex){return null;}
        ChatV2Plan p=ChatV2Plan.of(anterior.capability());p.setContinuar(true);p.setFonte("V2_CORRECAO_PERIODO");p.setParametros(params);return p;
    }
    private static void retirarPerguntaDeRotaJaInformada(ChatV2Plan p) {
        if(!pesquisaAerea(p)||p.getPergunta()==null)return;
        String t=normalizar(p.getPergunta()).replaceAll("[? ]+$","");
        String prefix="(?:qual (?:e )?|informe |pode informar |de qual )";
        boolean origem=t.matches(prefix+"(?:a |sua )?(?:cidade de |aeroporto de )?origem")&&p.getParametros().containsKey("origem");
        boolean destino=t.matches(prefix+"(?:o |seu |a )?(?:cidade de |aeroporto de )?destino")&&p.getParametros().containsKey("destino");
        boolean rota=t.matches(prefix+"(?:a |sua )?(?:rota|origem e (?:o )?destino)")
                &&p.getParametros().containsKey("origem")&&p.getParametros().containsKey("destino");
        if(origem||destino||rota)p.setPergunta(null);
    }
    static boolean continuacao(String t,ChatV2Plan p) {
        if(financeiroFaturas(p))return ChatFaturasFiltros.continuacao(t);
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
        if(financeiroFaturas(p))params.keySet().retainAll(ChatFaturasFiltros.PARAMETROS);
        else ChatFaturasFiltros.PARAMETROS.forEach(params::remove);
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

    private static boolean financeiroFaturas(ChatV2Plan p) {
        return p!=null&&(p.capability()==ChatV2Capability.FATURAS||p.capability()==ChatV2Capability.BOLETOS);
    }
    private static ChatV2Plan continuarFaturas(String mensagem,ChatV2Plan anterior) {
        if(!financeiroFaturas(anterior)||!ChatFaturasFiltros.continuacao(mensagem))return null;
        Map<String,String> filtros=ChatFaturasFiltros.extrair(mensagem,anterior.getParametros(),LocalDate.now());
        ChatV2Capability capacidade="BOLETO".equals(filtros.get("faturaDocumento"))?ChatV2Capability.BOLETOS:ChatV2Capability.FATURAS;
        ChatV2Plan p=ChatV2Plan.of(capacidade);
        p.setParametros(filtros);
        p.setContinuar(true);p.setFonte("V2_FILTROS_FATURAS");return p;
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
