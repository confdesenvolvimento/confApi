package com.confApi.chatconfianca.hotel;
import com.confApi.chatconfianca.v2.*;
import java.text.Normalizer;
import java.util.*;

public final class ChatIaHotelListaContexto {
    private ChatIaHotelListaContexto(){}
    public static final Set<String> FILTROS=Set.of("listaHotelHospede","listaHotelNome","listaHotelCidade","listaHotelTipoData","listaHotelInicio","listaHotelFim");
    public static final Set<String> PARAMETROS;
    static{var s=new HashSet<>(FILTROS);s.add("listaHotelComando");s.add("listaHotelOpcao");PARAMETROS=Set.copyOf(s);}
    public static Map<String,String> filtros(ChatV2Plan p){var m=new LinkedHashMap<String,String>();for(String k:FILTROS){String v=p.getParametros().get(k);if(v!=null&&!v.isBlank())m.put(k,v.trim());}return m;}
    public static ChatV2Plan comando(String mensagem,ChatV2Plan anterior){
        if(anterior==null||anterior.capability()!=ChatV2Capability.HOTEL_LISTA||anterior.getListaHotelEstado()==null)return null;
        String t=normal(mensagem).replaceAll("[.!?]","");
        String comando=null,opcao=null;
        if(t.matches("(?:mostre |mostrar |ver |quero ver )?mais|proxima(?: pagina)?|mais reservas"))comando="MAIS";
        else {
            var m=java.util.regex.Pattern.compile("^(?:(?:abra|abrir|ver|mostre|consultar) )?(?:a |o )?(?:reserva |opcao )?([1-9][0-9]?|primeira|primeiro|segunda|segundo|terceira|terceiro|quarta|quarto|quinta|quinto)$").matcher(t);
            if(m.matches()){comando="ABRIR";opcao=Map.of("primeira","1","primeiro","1","segunda","2","segundo","2","terceira","3","terceiro","3","quarta","4","quarto","4","quinta","5","quinto","5").getOrDefault(m.group(1),m.group(1));}
        }
        if(comando==null)return null;
        var p=ChatV2Plan.of(ChatV2Capability.HOTEL_LISTA);p.setContinuar(true);p.setFonte("V2_LISTA_HOTEL_COMANDO");
        p.getParametros().putAll(anterior.getListaHotelEstado().getFiltros());p.getParametros().put("listaHotelComando",comando);if(opcao!=null)p.getParametros().put("listaHotelOpcao",opcao);return p;
    }
    public static void restaurar(ChatV2Plan p,ChatV2Plan anterior){
        p.setListaHotelEstado(null);
        if(p.capability()!=ChatV2Capability.HOTEL_LISTA||anterior==null||anterior.capability()!=p.capability()||!p.isContinuar()||anterior.getListaHotelEstado()==null)return;
        for(String k:FILTROS)if(!normal(p.getParametros().get(k)).equals(normal(anterior.getListaHotelEstado().getFiltros().get(k))))return;
        p.setListaHotelEstado(anterior.getListaHotelEstado().copiar());
    }
    private static String normal(String s){return Normalizer.normalize(s==null?"":s.trim(),Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");}
}
