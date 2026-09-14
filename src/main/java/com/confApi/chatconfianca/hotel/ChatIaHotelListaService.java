package com.confApi.chatconfianca.hotel;

import com.confApi.chatconfianca.v2.ChatV2Plan;
import com.confApi.chatgpt.dto.ConversationRequestDTO;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Service;
import com.confApi.chatconfianca.hotel.ChatIaHotelDocumento.Configuracao;
import com.confApi.chatconfianca.hotel.ChatIaHotelListaDocumento.*;
import static com.confApi.chatconfianca.hotel.ChatIaHotelService.clean;

@Service
public class ChatIaHotelListaService {
    private final ChatIaHotelListaClient client;
    private final ChatIaHotelReservaService reserva;
    private final boolean enabled;
    private final Clock clock;
    @Autowired public ChatIaHotelListaService(ChatIaHotelListaClient client,ChatIaHotelReservaService reserva,@Value("${chat-confianca.hotel-lista.enabled:false}") boolean enabled){this(client,reserva,enabled,Clock.system(ZoneId.of("America/Cuiaba")));}
    ChatIaHotelListaService(ChatIaHotelListaClient client,ChatIaHotelReservaService reserva,boolean enabled,Clock clock){this.client=client;this.reserva=reserva;this.enabled=enabled;this.clock=clock;}
    public record Resultado(String status,String texto){}
    public String estadoCadastro(){if(!enabled)return "IMPLEMENTADO_DESABILITADO";try{return valida(client.configuracao())?"CONFIGURADO_HABILITADO_SUJEITO_A_V2":"CONFIGURACAO_AUSENTE_OU_INATIVA";}catch(Exception ex){return "CONFIGURACAO_INDISPONIVEL";}}
    public Resultado listarReservas(ChatV2Plan p,ConversationRequestDTO sessao){
        if(!enabled)return resposta("CAPACIDADE_INDISPONIVEL","A listagem de reservas de hotel ainda não está habilitada.");
        if(sessao==null||sessao.codgAgencia()==null||sessao.codgUsuario()==null||sessao.codgAgencia()<=0||sessao.codgUsuario()<=0||sessao.codgAgencia()>Integer.MAX_VALUE||sessao.codgUsuario()>Integer.MAX_VALUE
                ||!Objects.equals(p.getAgencia(),sessao.codgAgencia().intValue())||!Objects.equals(p.getUsuario(),sessao.codgUsuario().intValue()))return resposta("CONSULTA_BLOQUEADA","Não foi possível validar a sessão deste atendimento.");
        try {
            var config=client.configuracao();if(!valida(config))return resposta("CONSULTA_BLOQUEADA","A listagem de hotel está indisponível na configuração atual.");
            String comando=p.getParametros().remove("listaHotelComando"),opcao=p.getParametros().remove("listaHotelOpcao");
            if(comando==null&&opcao!=null)comando="ABRIR";
            if(comando!=null&&!Set.of("LISTAR","MAIS","ABRIR").contains(comando))throw new IllegalArgumentException("Não reconheci a operação. Peça para listar reservas, mostrar mais ou abrir um número da última lista.");
            var anterior=p.getListaHotelEstado();
            if(anterior!=null&&!anterior.getRevisao().equals(config.getRevisao())){p.setListaHotelEstado(null);anterior=null;}
            boolean continuacao="MAIS".equals(comando)||"ABRIR".equals(comando);
            if(continuacao&&(anterior==null||!mesmosFiltros(p,anterior)))return perguntar(p,"A lista anterior não está mais válida para este pedido. Solicite novamente a listagem com os filtros desejados.");
            if("ABRIR".equals(comando)){
                Integer id=anterior.getOpcoes().get(opcao);
                if(id==null)return perguntar(p,"Escolha um dos números exibidos na última página. Para outra página, peça Mostrar mais.");
                if(reserva==null)return resposta("CAPACIDADE_INDISPONIVEL","A consulta individual da reserva de hotel não está habilitada.");
                anterior.setOpcaoSelecionada(opcao);p.setPergunta(null);
                var r=reserva.consultarDaLista(p,sessao,id);return resposta(r.status(),r.texto());
            }
            if("MAIS".equals(comando)&&!anterior.isTemMais()){p.setPergunta(null);return resposta("SEM_RESULTADO","Você chegou ao fim da lista para os filtros informados. Pode abrir um item da última página ou informar outro período.");}
            Consulta q=consulta(p,sessao.codgAgencia().intValue());
            int pagina=1;
            if("MAIS".equals(comando)){q.setAposData(anterior.getProximaData());q.setAposId(anterior.getProximoId());pagina=anterior.getPagina()+1;if(q.getAposData()==null||q.getAposId()==null)throw new IllegalStateException();}
            var doc=client.consultar(q,config.getTimeoutSegundos());
            try{validarRetorno(doc,q,config);}catch(RuntimeException ex){throw new IllegalStateException("HOTEL_LISTA_CONTRATO_INVALIDO",ex);}
            var estado=new ChatIaHotelListaEstado();estado.setFiltros(ChatIaHotelListaContexto.filtros(p));estado.setRevisao(config.getRevisao());estado.setPagina(pagina);
            estado.setTemMais(doc.isMaisResultados());estado.setProximaData(doc.getProximaData());estado.setProximoId(doc.getProximoId());
            p.setListaHotelEstado(estado);p.setPergunta(null);
            StringBuilder texto=new StringBuilder("Reservas de hotel da sua agência — página ").append(pagina).append(".\n").append(resumo(q));
            if(doc.getReservas().isEmpty()){texto.append("\nNão encontrei reservas nesta página para esses critérios. Isso não confirma ausência de reservas ainda não sincronizadas.");return resposta("SEM_RESULTADO",texto.toString());}
            int n=0;for(var r:doc.getReservas()){
                String numero=String.valueOf(++n);estado.getOpcoes().put(numero,r.getId());
                texto.append("\n\n").append(numero).append(". ").append(clean(r.getHotel(),180)).append(" / ").append(clean(r.getCidade(),120))
                    .append("\nLocalizador: ").append(clean(r.getLocalizador(),85)).append(" — Criada em: ").append(dataExibicao(r.getCriacao()))
                    .append("\nEntrada: ").append(dataExibicao(r.getEntrada())).append(" — Saída: ").append(dataExibicao(r.getSaida()));
            }
            texto.append("\n\nPara ver quartos e hóspedes, diga Abra a primeira, Abra a segunda ou informe o número da reserva nesta página.");
            if(doc.isMaisResultados())texto.append(" Para continuar, diga Mostre mais.");else texto.append(" Fim da lista para esses filtros.");
            texto.append("\nFonte: registros do Manager da sua agência. Não houve consulta ao fornecedor. Datas não comprovam confirmação ou cancelamento; o status atual deve ser conferido no portal.");
            return resposta("DADOS_CONSULTADOS",texto.toString());
        }catch(IllegalArgumentException ex){p.setListaHotelEstado(null);return perguntar(p,ex.getMessage()==null?"Confira os filtros e informe um período válido, de até 366 dias.":ex.getMessage());}
        catch(Exception ex){p.setListaHotelEstado(null);p.setErro("HOTEL_LISTA_CONSULTA_INDISPONIVEL");return resposta("ERRO_INTEGRACAO","Não consegui listar as reservas de hotel agora. Tente novamente ou solicite atendimento humano.");}
    }
    Consulta consulta(ChatV2Plan p,int agencia){
        var q=new Consulta();q.setAgencia(agencia);q.setHotel(param(p,"listaHotelNome",3));q.setHospede(param(p,"listaHotelHospede",3));q.setCidade(param(p,"listaHotelCidade",2));
        String tipo=p.getParametros().get("listaHotelTipoData"),inicio=p.getParametros().get("listaHotelInicio"),fim=p.getParametros().get("listaHotelFim");
        if(inicio==null&&fim==null&&(tipo==null||tipo.equals("CRIACAO"))){tipo="CRIACAO";var hoje=LocalDate.now(clock);inicio=hoje.minusDays(29).toString();fim=hoje.toString();}
        if(tipo==null||!Set.of("CRIACAO","ENTRADA","SAIDA").contains(tipo))throw new IllegalArgumentException("Esse período se refere à criação da reserva, entrada ou saída da hospedagem?");
        if(inicio==null||fim==null)throw new IllegalArgumentException("Informe a data inicial e final do período que deseja consultar.");
        LocalDate ini,f;
        try{ini=data(inicio);f=data(fim);}catch(RuntimeException ex){throw new IllegalArgumentException("Informe datas válidas no formato dia/mês/ano.");}
        if(f.isBefore(ini)||ChronoUnit.DAYS.between(ini,f)>365)throw new IllegalArgumentException("Informe um período em ordem cronológica, de até 366 dias.");
        q.setTipoData(tipo);q.setInicio(inicio);q.setFim(fim);
        p.getParametros().put("listaHotelTipoData",tipo);p.getParametros().put("listaHotelInicio",inicio);p.getParametros().put("listaHotelFim",fim);
        return q;
    }
    private static boolean mesmosFiltros(ChatV2Plan p,ChatIaHotelListaEstado s){return ChatIaHotelListaContexto.filtros(p).equals(s.getFiltros());}
    private static String param(ChatV2Plan p,String key,int min){String s=p.getParametros().get(key);if(s==null||s.isBlank())return null;if(s.trim().length()<min||s.length()>120||s.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Informe um nome de hóspede, hotel ou cidade válido, sem dados de documentos.");return s.trim();}
    private static LocalDate data(String s){if(!s.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))throw new IllegalArgumentException();var d=LocalDate.parse(s);if(d.getYear()<1900||d.getYear()>9998)throw new IllegalArgumentException();return d;}
    private static String dataExibicao(String s){try{return LocalDate.parse(s.substring(0,10)).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));}catch(Exception ex){return "não informada";}}
    private static String resumo(Consulta q){StringBuilder s=new StringBuilder("Período de ").append(Map.of("CRIACAO","criação","ENTRADA","entrada","SAIDA","saída").get(q.getTipoData())).append(": ").append(dataExibicao(q.getInicio())).append(" a ").append(dataExibicao(q.getFim())).append(" (inclusive).");if(q.getHospede()!=null)s.append(" Hóspede: ").append(clean(q.getHospede(),120)).append('.');if(q.getHotel()!=null)s.append(" Hotel: ").append(clean(q.getHotel(),120)).append('.');if(q.getCidade()!=null)s.append(" Cidade: ").append(clean(q.getCidade(),120)).append('.');return s.toString();}
    private static boolean valida(Configuracao c){return c!=null&&c.getRevisao()!=null&&!c.getRevisao().isBlank()&&faixa(c.getLimiteResultados(),1,10)&&faixa(c.getMaxSugestoes(),1,10)&&faixa(c.getTimeoutSegundos(),1,120);}
    private static boolean faixa(Integer n,int min,int max){return n!=null&&n>=min&&n<=max;}
    private static void validarRetorno(ChatIaHotelListaDocumento d,Consulta q,Configuracao c){
        if(d==null||!"hotel-lista-v1".equals(d.getContrato())||!valida(d.getConfiguracao())||!c.getRevisao().equals(d.getConfiguracao().getRevisao())||d.getReservas()==null||d.getReservas().size()>Math.min(5,Math.min(c.getLimiteResultados(),c.getMaxSugestoes())))throw new IllegalStateException();
        Instant.parse(d.getConsultadoEm());var ids=new HashSet<Integer>();Timestamp prev=q.getAposData()==null?null:Timestamp.valueOf(q.getAposData());Integer prevId=q.getAposId();
        Timestamp ini=Timestamp.valueOf(data(q.getInicio()).atStartOfDay()),fim=Timestamp.valueOf(data(q.getFim()).plusDays(1).atStartOfDay());
        for(var r:d.getReservas()){
            if(r==null||r.getId()==null||r.getId()<=0||!ids.add(r.getId())||!q.getAgencia().equals(r.getAgencia())||r.getDataOrdenacao()==null)throw new IllegalStateException();
            Timestamp atual=Timestamp.valueOf(r.getDataOrdenacao());if(atual.before(ini)||!atual.before(fim))throw new IllegalStateException();
            if(prev!=null){int cmp=atual.compareTo(prev);if(cmp==0)cmp=r.getId().compareTo(prevId);if(q.getTipoData().equals("CRIACAO")?cmp>=0:cmp<=0)throw new IllegalStateException();}prev=atual;prevId=r.getId();
        }
        if(d.isMaisResultados()){
            if(d.getReservas().isEmpty())throw new IllegalStateException();var last=d.getReservas().get(d.getReservas().size()-1);
            if(!last.getId().equals(d.getProximoId())||!last.getDataOrdenacao().equals(d.getProximaData()))throw new IllegalStateException();
        }else if(d.getProximaData()!=null||d.getProximoId()!=null)throw new IllegalStateException();
    }
    private static Resultado resposta(String s,String t){return new Resultado(s,t);}
    private static Resultado perguntar(ChatV2Plan p,String t){p.setPergunta(t);return resposta("AGUARDANDO_DADOS",t);}
    public static Map<String,Object> schema(){var campos=new LinkedHashMap<String,Object>();for(String k:ChatIaHotelListaContexto.PARAMETROS)campos.put(k,Map.of("type",List.of("string","null")));campos.put("listaHotelTipoData",Map.of("type",List.of("string","null"),"enum",Arrays.asList("CRIACAO","ENTRADA","SAIDA",null)));campos.put("listaHotelComando",Map.of("type",List.of("string","null"),"enum",Arrays.asList("LISTAR","MAIS","ABRIR",null)));return Map.of("type","object","additionalProperties",false,"required",ChatIaHotelListaContexto.PARAMETROS,"properties",campos);}
}
