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
    @org.springframework.beans.factory.annotation.Value("${chat-confianca.reservas-consulta.enabled:true}")
    private boolean consultaReservasEnabled=true;
    public ChatV2Planner(ChatV2Properties properties, ChatV2SemanticClient semantic, ObjectMapper mapper) {
        this.properties=properties; this.semantic=semantic; this.mapper=mapper;
    }

    public ChatV2Plan planejar(String mensagem, Conversa conversa, List<Mensagem> historico,
                              Integer agencia, Integer usuario) {
        if(!properties.participa(agencia,usuario))return null;
        ChatV2Plan anterior=contexto(conversa,agencia,usuario);
        // Consultas factuais conhecidas nao precisam de uma chamada ao modelo para escolher a ferramenta.
        if(consultaReservasEnabled && properties.permite(ChatV2Capability.RESERVAS.code)
                &&!preservarOperacaoPorLocalizador(mensagem,anterior)
                &&(anterior==null||anterior.getIntencao()==null||!anterior.getIntencao().startsWith("hotel."))) {
            var estado=com.confApi.chatconfianca.reservas.ChatReservaConsultaContexto.recuperar(historico,
                    conversa==null?null:conversa.getId(),agencia==null?null:agencia.longValue(),usuario==null?null:usuario.longValue(),mapper);
            var pedido=com.confApi.chatconfianca.reservas.ChatReservaConsultaService.interpretarConsulta(mensagem,estado);
            if(pedido!=null){
                var consulta=ChatV2Plan.of(ChatV2Capability.RESERVAS);consulta.setFonte("V2_CONSULTA_REGISTROS");
                consulta.setPergunta(pedido.pergunta());consulta.setAgencia(agencia);consulta.setUsuario(usuario);
                consulta.setAtualizadoEm(System.currentTimeMillis());return consulta;
            }
        }
        if(financeiroFaturas(anterior)&&!com.confApi.chatconfianca.financeiro.ChatFaturasContexto.permiteContinuidadeV2(historico,usuario,mapper))anterior=null;
        String t=normalizar(mensagem);
        ChatV2Plan p=continuarHandoff(t,anterior);
        if(p==null)p=confirmarBuscaSemBagagem(t,anterior);
        if(p==null)p=detalharOpcaoAerea(mensagem,anterior);
        if(p==null)p=continuarFaturas(mensagem,anterior);
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
                    if(anterior==null)interpretado.setContinuar(false);
                    completarCorrecaoDePeriodo(interpretado,anterior,t);
                    preservarContinuacao(interpretado,anterior,t);
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
        preservarPedidoOperacional(p,anterior,t);
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
        if(p.capability()!=ChatV2Capability.HUMANO&&!properties.permite(p.getIntencao())) { p.setLegado(true);p.setFonte("V2_FORA_ESCOPO"); }
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
        if(pesquisaAerea(p)&&t.matches(".*\\bbagage(?:m|ns)\\b.*")
                && !t.matches(".*\\b(sem filtro de bagagem|ignorar bagagem|nao (?:precisa|preciso|quero) (?:de )?bagagem)\\b.*")) {
            p.setFiltroBagagemPendente(true);
            p.setPergunta("Esta pesquisa não permite filtrar bagagem incluída. Deseja consultar sem esse filtro e conferir a franquia nas regras da tarifa, ou falar com um atendente?");
        }
        if(anterior!=null&&Objects.equals(p.getIntencao(),anterior.getIntencao())&&p.isContinuar()
                &&Objects.equals(p.getParametros(),anterior.getParametros())) {
            p.setEsclarecimentosSemProgresso(anterior.getEsclarecimentosSemProgresso());
            if(p.getOpcoesAereas().isEmpty())p.setOpcoesAereas(anterior.getOpcoesAereas());
        }
        // A short answer that the model fails to understand is not progress or a new task.
        if(anterior!=null&&anterior.getPergunta()!=null&&p.capability()==ChatV2Capability.AJUDA
                &&identificar(t)==null&&t.length()<120) {
            p.setEsclarecimentosSemProgresso(anterior.getEsclarecimentosSemProgresso());
            p.setAssuntoHandoff(anterior.getAssuntoHandoff()==null?anterior.getIntencao():anterior.getAssuntoHandoff());
        }
        com.confApi.chatconfianca.hotel.ChatIaHotelContexto.restaurar(p,anterior);
        com.confApi.chatconfianca.hotel.ChatIaHotelReservaContexto.restaurar(p,anterior);
        com.confApi.chatconfianca.hotel.ChatIaHotelListaContexto.restaurar(p,anterior);
        p.setAgencia(agencia);p.setUsuario(usuario);p.setAtualizadoEm(System.currentTimeMillis());
        return p;
    }

    /** Um PNR informado para uma operacao existente nao muda o pedido para listagem. */
    public static boolean preservarOperacaoPorLocalizador(String mensagem,ChatV2Plan anterior) {
        String loc=LocalizadorAereo.extrair(mensagem);
        if(loc==null)return false;
        String t=normalizar(mensagem);
        // O extrator legado pode confundir "reserva da Nazare/para Fortaleza" com PNR.
        // Filtros estruturados de pessoa/rota e recencia tem precedencia sobre essa heuristica.
        var consulta=com.confApi.chatconfianca.reservas.ChatReservaConsultaParser.interpretar(mensagem,null);
        if(consulta!=null&&consulta.filtros()!=null
                &&(consulta.filtros().getNomePassageiro()!=null||consulta.filtros().getDestino()!=null
                ||consulta.filtros().getOrigem()!=null
                ||(consulta.filtros().getLocalizador()==null&&t.matches(".*\\b(?:ultim[oa]s?|recentes?)\\b.*"))))return false;
        ChatV2Capability explicita=identificar(t);
        if(explicita!=null&&explicita.usaLocalizador())return true;
        return t.replaceAll("[.!?]+$","").equals(loc.toLowerCase(Locale.ROOT))
                &&anterior!=null&&anterior.capability()!=null&&anterior.capability().usaLocalizador();
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
        if(t.matches(".*\\b(preparar|cancelar|emitir|reembolsar|reembolso|assentos|bilhetes|voucher)\\b.*"))return ChatV2Capability.ACOES_RESERVA;
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
    private static final List<String> PERIODO_AEREO=List.of("dataIda","dataVolta","dataInicio","dataFim",
            "dataIdaInicio","dataIdaFim","dataVoltaInicio","dataVoltaFim","mes","mesIda","mesVolta");

    /** Restore only the current authenticated task. Explicit changes/removals always win. */
    private static void preservarContinuacao(ChatV2Plan atual,ChatV2Plan anterior,String texto) {
        if(anterior==null)return;
        boolean referencia=atual.capability()==anterior.capability()
                &&(texto.matches(".*\\b(detalhe|detalhar|essa tarifa|essa opcao|esta tarifa|esta opcao)\\b.*")
                ||anterior.getPergunta()!=null&&texto.matches("(?:sim|nao|aereo|aerea|hotel|reembolso|cancelamento)[?!. ]*"));
        if(!atual.isContinuar()&&!referencia)return;
        atual.setContinuar(true);
        boolean pesquisa=pesquisaAerea(anterior)&&pesquisaAerea(atual);
        boolean reserva=atual.capability()==anterior.capability()&&reservaComLocalizador(atual);
        if(!pesquisa&&!reserva)return;
        Map<String,String> anteriores=new LinkedHashMap<>(anterior.getParametros());
        if(pesquisa) {
            boolean periodo=atual.getParametros().keySet().stream().anyMatch(PERIODO_AEREO::contains);
            boolean mudou=PERIODO_AEREO.stream().anyMatch(k->atual.getParametros().containsKey(k)
                    &&!Objects.equals(atual.getParametros().get(k),anteriores.get(k)));
            if(periodo&&mudou) {
                boolean ida=texto.matches(".*\\bida\\b.*"),volta=texto.matches(".*\\b(volta|retorno)\\b.*");
                if(ida&&!volta)List.of("mes","mesIda","dataIda","dataInicio","dataFim","dataIdaInicio","dataIdaFim").forEach(anteriores::remove);
                else if(volta&&!ida)List.of("mesVolta","dataVolta","dataVoltaInicio","dataVoltaFim").forEach(anteriores::remove);
                else PERIODO_AEREO.forEach(anteriores::remove);
            }
            if(respostaSomentePeriodo(texto)||texto.matches(".*\\b(todos os meses|qualquer periodo|sem (?:filtro de )?periodo)\\b.*"))
                PERIODO_AEREO.forEach(anteriores::remove);
            if(texto.matches(".*\\b(somente ida|so ida|apenas ida|sem volta)\\b.*")||atual.capability()==ChatV2Capability.TARIFA_IDA)
                List.of("dataVolta","dataVoltaInicio","dataVoltaFim","mesVolta").forEach(anteriores::remove);
            for(String key:List.of("origem","destino","cabine"))
                if(texto.matches(".*\\b(retire|remova|remover|exclua|sem)\\b.{0,20}\\b"+key+"\\b.*"))anteriores.remove(key);
            // A complete new route is not a continuation merely because a model says so.
            if(texto.matches(".*\\b(nova pesquisa|outra rota|nova rota)\\b.*"))return;
        }
        anteriores.putAll(atual.getParametros());atual.setParametros(anteriores);
        if(pesquisa) {
            for(String key:List.of("origem","destino","cabine"))
                if(texto.matches(".*\\b(retire|remova|remover|exclua|sem)\\b.{0,20}\\b"+key+"\\b.*"))anteriores.remove(key);
            if(texto.matches(".*\\b(retire|remova|exclua|sem)\\b.{0,20}\\b(?:data de )?volta\\b.*")
                    ||texto.matches(".*\\b(somente ida|so ida|apenas ida)\\b.*"))
                List.of("dataVolta","dataVoltaInicio","dataVoltaFim","mesVolta").forEach(anteriores::remove);
        }
    }

    private static ChatV2Plan continuarHandoff(String texto,ChatV2Plan anterior) {
        if(anterior==null||!(anterior.capability()==ChatV2Capability.HUMANO
                ||normalizar(anterior.getPergunta()).contains("deseja falar com o atendimento"))
                ||!texto.matches("(?:sim|pode|pode sim|quero|confirmo|continuar|ok|certo|atendente)[?!. ]*"))return null;
        ChatV2Plan p=ChatV2Plan.of(ChatV2Capability.HUMANO);p.setContinuar(true);
        p.setAssuntoHandoff(anterior.capability()==ChatV2Capability.HUMANO?anterior.getAssuntoHandoff():anterior.getIntencao());
        p.setPedidoOperacional(anterior.getPedidoOperacional());
        p.setFonte("V2_CONFIRMACAO_ATENDIMENTO");return p;
    }

    private static ChatV2Plan confirmarBuscaSemBagagem(String texto,ChatV2Plan anterior) {
        if(!pesquisaAerea(anterior)||!anterior.isFiltroBagagemPendente()
                ||!texto.matches("(?:sim|pode|pode sim|sem (?:o )?filtro|sem bagagem|consultar sem (?:o )?filtro|continue)[?!. ]*"))return null;
        ChatV2Plan p=ChatV2Plan.of(anterior.capability());p.setContinuar(true);
        p.setParametros(new LinkedHashMap<>(anterior.getParametros()));p.setFonte("V2_CRITERIO_CONFIRMADO");return p;
    }

    private static ChatV2Plan detalharOpcaoAerea(String mensagem,ChatV2Plan anterior) {
        if(!pesquisaAerea(anterior)||anterior.getOpcoesAereas()==null||anterior.getOpcoesAereas().isEmpty())return null;
        String texto=normalizar(mensagem);
        if(!texto.matches(".*\\b(detalhe|detalhar|detalhes|opcao|tarifa|essa|esta)\\b.*"))return null;
        Matcher datas=Pattern.compile("(?<!\\d)(\\d{1,2})/(\\d{1,2})(?:/(\\d{4}))?(?!\\d)").matcher(texto);
        List<String> pedidas=new ArrayList<>();while(datas.find())pedidas.add((datas.group(3)==null?"":datas.group(3)+"-")
                +String.format(Locale.ROOT,"%02d-%02d",Integer.parseInt(datas.group(2)),Integer.parseInt(datas.group(1))));
        List<Map<String,String>> candidatas=new ArrayList<>();
        for(Map<String,String> opcao:anterior.getOpcoesAereas()) {
            if(!Objects.equals(opcao.get("origem"),anterior.getParametros().get("origem")))continue;
            if(!Objects.equals(opcao.get("destino"),anterior.getParametros().get("destino")))continue;
            List<String> oferecidas=new ArrayList<>();
            for(String key:List.of("dataIda","dataVolta"))if(opcao.get(key)!=null) {
                oferecidas.add(opcao.get(key));if(opcao.get(key).length()==10)oferecidas.add(opcao.get(key).substring(5));
            }
            if(!pedidas.isEmpty()&&oferecidas.containsAll(pedidas))candidatas.add(opcao);
        }
        if(pedidas.isEmpty()&&anterior.getOpcoesAereas().size()==1)candidatas.add(anterior.getOpcoesAereas().get(0));
        if(pedidas.isEmpty()&&candidatas.isEmpty()) {
            Matcher indice=Pattern.compile("\\b(primeira|segunda|terceira|[1-9][0-9]?)\\s+(?:opcao|tarifa)\\b|\\bopcao\\s+([1-9][0-9]?)\\b").matcher(texto);
            if(indice.find()) {
                String valor=indice.group(1)==null?indice.group(2):indice.group(1);
                int numero=switch(valor){case "primeira"->1;case "segunda"->2;case "terceira"->3;default->Integer.parseInt(valor);};
                if(numero<=anterior.getOpcoesAereas().size())candidatas.add(anterior.getOpcoesAereas().get(numero-1));
            }
        }
        if(candidatas.size()!=1)return null;
        if(!Objects.equals(candidatas.get(0).get("origem"),anterior.getParametros().get("origem"))
                ||!Objects.equals(candidatas.get(0).get("destino"),anterior.getParametros().get("destino")))return null;
        ChatV2Plan p=ChatV2Plan.of(anterior.capability());p.setContinuar(true);
        Map<String,String> params=new LinkedHashMap<>(anterior.getParametros());PERIODO_AEREO.forEach(params::remove);
        params.putAll(candidatas.get(0));p.setParametros(params);p.setFonte("V2_OPCAO_AEREA_VALIDADA");return p;
    }

    private static void preservarPedidoOperacional(ChatV2Plan p,ChatV2Plan anterior,String texto) {
        String afirmativo=texto.replaceAll("\\bnao\\s+(?:(?:e|quero|desejo|preciso)(?: de)?\\s+)?(?:reembolso|reembolsar|cancelamento|cancelar)\\b","");
        String pedido=afirmativo.matches(".*\\b(reembolso|reembolsar)\\b.*")?"REEMBOLSO":
                afirmativo.matches(".*\\b(cancelamento|cancelar)\\b.*")?"CANCELAMENTO":null;
        if(pedido!=null)p.setPedidoOperacional(pedido);
        else if(anterior!=null&&(p.isContinuar()||p.capability()==ChatV2Capability.HUMANO))p.setPedidoOperacional(anterior.getPedidoOperacional());
        if("REEMBOLSO".equals(p.getPedidoOperacional())&&p.capability()==ChatV2Capability.ACOES_RESERVA
                &&texto.matches(".*\\b(acompanhar|andamento|status|prazo)\\b.*")) {
            p.setPergunta("Ainda não consulto o andamento de reembolso por aqui. Deseja falar com o atendimento responsável mantendo este pedido?");
        }
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
