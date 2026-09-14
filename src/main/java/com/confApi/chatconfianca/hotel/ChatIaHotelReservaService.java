package com.confApi.chatconfianca.hotel;

import com.confApi.chatconfianca.v2.ChatV2Plan;
import com.confApi.chatgpt.dto.ConversationRequestDTO;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.confApi.chatconfianca.hotel.ChatIaHotelDocumento.Configuracao;
import com.confApi.chatconfianca.hotel.ChatIaHotelReservaDocumento.*;
import static com.confApi.chatconfianca.hotel.ChatIaHotelService.clean;

@Service
public class ChatIaHotelReservaService {
    private final ChatIaHotelReservaClient client;
    private final boolean enabled;
    public ChatIaHotelReservaService(ChatIaHotelReservaClient client,@Value("${chat-confianca.hotel-reserva.enabled:false}") boolean enabled) {
        this.client=client;this.enabled=enabled;
    }
    public record Resultado(String status,String texto) {}
    public String estadoCadastro() {
        if(!enabled)return "IMPLEMENTADO_DESABILITADO";
        try{return valida(client.configuracao())?"CONFIGURADO_HABILITADO_SUJEITO_A_V2":"CONFIGURACAO_AUSENTE_OU_INATIVA";}
        catch(Exception ex){return "CONFIGURACAO_INDISPONIVEL";}
    }
    public Resultado consultarReserva(ChatV2Plan p,ConversationRequestDTO sessao) {return consultarReserva(p,sessao,null);}
    /** Only the server-owned last list page can open a reservation without a user search filter. */
    public Resultado consultarDaLista(ChatV2Plan lista,ConversationRequestDTO sessao,Integer id) {
        if(lista==null||lista.capability()!=com.confApi.chatconfianca.v2.ChatV2Capability.HOTEL_LISTA||id==null||id<=0
                ||lista.getListaHotelEstado()==null||!lista.getListaHotelEstado().getOpcoes().containsValue(id))
            return resposta("CONSULTA_BLOQUEADA","Escolha uma reserva da última página válida.");
        var p=ChatV2Plan.of(com.confApi.chatconfianca.v2.ChatV2Capability.HOTEL_RESERVA);p.setAgencia(lista.getAgencia());p.setUsuario(lista.getUsuario());
        return consultarReserva(p,sessao,id);
    }
    private Resultado consultarReserva(ChatV2Plan p,ConversationRequestDTO sessao,Integer idDaLista) {
        if(!enabled)return resposta("CAPACIDADE_INDISPONIVEL","A consulta de reservas de hotel ainda não está habilitada. Você pode solicitar atendimento humano.");
        if(sessao==null||sessao.codgUsuario()==null||sessao.codgUsuario()<=0||sessao.codgAgencia()==null||sessao.codgAgencia()<=0
                ||sessao.codgAgencia()>Integer.MAX_VALUE||sessao.codgUsuario()>Integer.MAX_VALUE
                ||!Objects.equals(p.getAgencia(),sessao.codgAgencia().intValue())||!Objects.equals(p.getUsuario(),sessao.codgUsuario().intValue()))
            return resposta("CONSULTA_BLOQUEADA","Não foi possível validar a sessão deste atendimento.");
        try {
            var config=client.configuracao();
            if(!valida(config))return resposta("CONSULTA_BLOQUEADA","A consulta de reservas de hotel está indisponível na configuração atual. Você pode solicitar atendimento humano.");
            String revisao=p.getReservaHotelConfiguracaoRevisao();
            if(revisao!=null&&!revisao.equals(config.getRevisao())){p.getReservaHotelOpcoes().clear();p.setReservaHotelSelecionada(null);}
            p.setReservaHotelConfiguracaoRevisao(config.getRevisao());
            var q=new Consulta();q.setAgencia(sessao.codgAgencia().intValue());
            q.setLocalizador(param(p,"reservaHotelLocalizador",2,85));q.setHospede(param(p,"reservaHotelHospede",3,120));
            q.setHotel(param(p,"reservaHotelNome",3,120));q.setCidade(param(p,"reservaHotelCidade",2,120));
            if(q.getLocalizador()==null&&q.getHospede()==null&&q.getHotel()==null&&idDaLista==null)return perguntar(p,pergunta(config));
            q.setSelecionada(idDaLista==null?p.getReservaHotelSelecionada():idDaLista);
            String opcao=p.getParametros().remove("reservaHotelOpcao");
            if(opcao!=null){q.setSelecionada(p.getReservaHotelOpcoes().get(opcao));if(q.getSelecionada()==null)return perguntar(p,"Escolha um número da última lista válida ou informe novamente o localizador, hóspede ou hotel.");}
            var doc=client.consultar(q,config.getTimeoutSegundos());
            if(doc==null||!"hotel-reserva-v1".equals(doc.getContrato())||!valida(doc.getConfiguracao())||doc.getReservas()==null||doc.getReservas().size()>5)throw new IllegalStateException();
            Instant.parse(doc.getConsultadoEm());
            if(!config.getRevisao().equals(doc.getConfiguracao().getRevisao())){
                p.getReservaHotelOpcoes().clear();p.setReservaHotelSelecionada(null);
                return perguntar(p,"A configuração mudou durante a consulta. Confirme novamente qual reserva de hotel deseja consultar.");
            }
            var ids=new HashSet<Integer>();
            for(var r:doc.getReservas())if(r==null||r.getId()==null||r.getId()<=0||!ids.add(r.getId())||!q.getAgencia().equals(r.getAgencia())
                    ||r.getQuartos()==null||r.getHospedes()==null||r.getQuartos().size()>50||r.getHospedes().size()>50)throw new IllegalStateException();
            p.getReservaHotelOpcoes().clear();p.setReservaHotelSelecionada(null);
            if(doc.getReservas().isEmpty())return perguntar(p,"Não encontrei essa reserva de hotel nos registros da sua agência consultados. Confira o localizador, nome do hóspede ou hotel. Reservas ainda não sincronizadas podem não aparecer.");
            if(q.getSelecionada()!=null&&(doc.getReservas().size()!=1||!q.getSelecionada().equals(doc.getReservas().get(0).getId())))throw new IllegalStateException();
            if(doc.getReservas().size()>1||doc.isMaisResultados()) {
                StringBuilder text=new StringBuilder("Qual destas reservas de hotel deseja consultar?\n");int i=0;
                for(var r:doc.getReservas()){
                    String n=String.valueOf(++i);p.getReservaHotelOpcoes().put(n,r.getId());
                    text.append("\n").append(n).append(". ").append(rotulo(r));
                }
                if(doc.isMaisResultados())text.append("\nHá outros resultados. Refine pelo localizador, nome completo do hóspede ou cidade do hotel.");
                return perguntar(p,text.toString());
            }
            Reserva r=doc.getReservas().get(0);if(!r.isDetalhesCompletos())throw new IllegalStateException();
            p.setReservaHotelSelecionada(r.getId());p.setPergunta(null);
            // Private factual rendering: guest/room results never go to an additional model call.
            StringBuilder text=new StringBuilder("Dados cadastrados da reserva de hotel:\n\n").append(rotulo(r));
            text.append("\n\nQuartos:");
            if(r.getQuartos().isEmpty())text.append(" não informados na fonte consultada.");
            for(var quarto:r.getQuartos())text.append("\n- ").append(clean(quarto.getNome(),180)).append(" / ").append(clean(quarto.getTipo(),180)).append(" — Regime: ").append(clean(quarto.getRegime(),250));
            text.append("\n\nHóspedes:");
            if(r.getHospedes().isEmpty())text.append(" não informados na fonte consultada.");
            for(String h:r.getHospedes())text.append("\n- ").append(clean(h,310));
            if(r.isDetalhesLimitados())text.append("\nExibição limitada aos primeiros 50 quartos/hóspedes. Consulte a relação completa no portal.");
            text.append("\n\nFonte: registros de reservas de hotel da sua agência. Não houve consulta ao fornecedor nem alteração da reserva.\nA situação atual deve ser confirmada no portal; as datas cadastradas não comprovam confirmação ou cancelamento.");
            if(p.getReservaHotelInteracoes()>=config.getQtdInteracoesAtendente())text.append("\nSe preferir, use Falar com atendente e escolha ou confirme o departamento.");
            return resposta("DADOS_CONSULTADOS",text.toString());
        }catch(IllegalArgumentException ex){return perguntar(p,"Informe um localizador válido, nome do hóspede ou nome do hotel. Não envie senha, documento ou cartão.");}
        catch(Exception ex){p.getReservaHotelOpcoes().clear();p.setReservaHotelSelecionada(null);p.setErro("HOTEL_RESERVA_CONSULTA_INDISPONIVEL");return resposta("ERRO_INTEGRACAO","Não consegui consultar a reserva de hotel agora. Tente novamente ou solicite atendimento humano.");}
    }
    private static String rotulo(Reserva r){return "Localizador: "+clean(r.getLocalizador(),85)+" — "+clean(r.getHotel(),180)+" / "+clean(r.getCidade(),120)+"\nEntrada: "+data(r.getEntrada())+" — Saída: "+data(r.getSaida());}
    private static String data(String s){try{return LocalDate.parse(s.substring(0,10)).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));}catch(Exception ex){return "não informada";}}
    private static String param(ChatV2Plan p,String k,int min,int max){String s=p.getParametros().get(k);if(s==null||s.isBlank())return null;if(s.trim().length()<min||s.length()>max||s.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException();return s.trim();}
    private static boolean valida(Configuracao c){return c!=null&&c.getRevisao()!=null&&!c.getRevisao().isBlank()&&c.getOrientacoes()!=null&&!c.getOrientacoes().isBlank()&&faixa(c.getLimiteResultados(),1,10)&&faixa(c.getMaxSugestoes(),1,10)&&faixa(c.getTimeoutSegundos(),1,120)&&faixa(c.getQtdInteracoesAtendente(),1,100);}
    private static boolean faixa(Integer i,int min,int max){return i!=null&&i>=min&&i<=max;}
    private static String pergunta(Configuracao c){String s=clean(c.getPergunta(),500);return s.isBlank()?"Qual o localizador da reserva de hotel, nome do hóspede ou nome do hotel?":s;}
    private static Resultado resposta(String s,String t){return new Resultado(s,t);}
    private static Resultado perguntar(ChatV2Plan p,String t){p.setPergunta(t);return resposta("AGUARDANDO_DADOS",t);}
    public static Map<String,Object> schema(){return Map.of("type","object","additionalProperties",false,"description","Informe localizador OU hóspede OU hotel; cidade refina. Identidade da agência vem da sessão, nunca do modelo.","properties",Map.of(
        "reservaHotelLocalizador",Map.of("type","string","minLength",2,"maxLength",85),"reservaHotelHospede",Map.of("type","string","minLength",3,"maxLength",120),
        "reservaHotelNome",Map.of("type","string","minLength",3,"maxLength",120),"reservaHotelCidade",Map.of("type","string","maxLength",120),"reservaHotelOpcao",Map.of("type","string","enum",List.of("1","2","3","4","5"))));}
}
