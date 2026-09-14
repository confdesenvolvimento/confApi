package com.confApi.chatconfianca.hotel;

import com.confApi.chatconfianca.v2.*;
import java.util.*;
import java.util.regex.Pattern;

/** Separate hotel booking state; planner validates agency/user/TTL before calling this class. */
public final class ChatIaHotelReservaContexto {
    private ChatIaHotelReservaContexto() {}
    public static final Set<String> PARAMETROS=Set.of("reservaHotelLocalizador","reservaHotelHospede","reservaHotelNome","reservaHotelCidade","reservaHotelOpcao");
    public static ChatV2Plan escolher(String mensagem,ChatV2Plan anterior) {
        if(anterior==null||anterior.capability()!=ChatV2Capability.HOTEL_RESERVA||anterior.getReservaHotelOpcoes().isEmpty())return null;
        var m=Pattern.compile("(?i)^(?:op[çc][aã]o |reserva |a )?([1-5])[.! ]*$").matcher(mensagem.trim());
        if(!m.matches())return null;
        var p=ChatV2Plan.of(ChatV2Capability.HOTEL_RESERVA);p.setContinuar(true);p.setFonte("V2_ESCOLHA_RESERVA_HOTEL");
        p.getParametros().putAll(anterior.getParametros());p.getParametros().put("reservaHotelOpcao",m.group(1));return p;
    }
    public static void restaurar(ChatV2Plan p,ChatV2Plan anterior) {
        p.setReservaHotelSelecionada(null);p.setReservaHotelOpcoes(new LinkedHashMap<>());p.setReservaHotelInteracoes(1);p.setReservaHotelConfiguracaoRevisao(null);
        if(p.capability()!=ChatV2Capability.HOTEL_RESERVA||anterior==null||!p.isContinuar()||anterior.capability()!=p.capability())return;
        for(String k:PARAMETROS)if(!k.equals("reservaHotelOpcao")&&!Objects.equals(normal(p.getParametros().get(k)),normal(anterior.getParametros().get(k))))return;
        p.setReservaHotelOpcoes(new LinkedHashMap<>(anterior.getReservaHotelOpcoes()));p.setReservaHotelSelecionada(anterior.getReservaHotelSelecionada());
        p.setReservaHotelConfiguracaoRevisao(anterior.getReservaHotelConfiguracaoRevisao());
        p.setReservaHotelInteracoes(Math.min(101,anterior.getReservaHotelInteracoes()+1));
    }
    private static String normal(String s){return s==null?"":s.trim().toLowerCase(Locale.ROOT);}
}
