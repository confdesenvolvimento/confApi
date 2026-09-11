package com.confApi.chatconfianca.hotel;

import com.confApi.chatconfianca.v2.*;
import java.util.*;
import java.util.regex.Pattern;

/** Candidate IDs are server-owned state, never inferred by the language model. */
public final class ChatIaHotelContexto {
    private ChatIaHotelContexto() {}
    public static ChatV2Plan escolher(String mensagem,ChatV2Plan anterior) {
        if(anterior==null||anterior.capability()!=ChatV2Capability.HOTEL_DADOS||anterior.getHotelOpcoes().isEmpty())return null;
        var m=Pattern.compile("(?i)^(?:op[çc][aã]o |hotel |o )?([1-5])[.! ]*$").matcher(mensagem.trim());
        if(!m.matches())return null;
        var p=ChatV2Plan.of(ChatV2Capability.HOTEL_DADOS);p.setContinuar(true);p.setFonte("V2_ESCOLHA_HOTEL");
        p.getParametros().putAll(anterior.getParametros());p.getParametros().put("hotelOpcao",m.group(1));
        return p;
    }
    public static void restaurar(ChatV2Plan p,ChatV2Plan anterior) {
        p.setHotelSelecionado(null);p.setHotelOpcoes(new LinkedHashMap<>());p.setHotelInteracoes(1);
        if(p.capability()!=ChatV2Capability.HOTEL_DADOS)return;
        if(anterior==null||!p.isContinuar()||anterior.capability()!=p.capability())return;
        for(String k:List.of("hotelNome","hotelCidade","hotelPais"))
            if(!Objects.equals(normal(p.getParametros().get(k)),normal(anterior.getParametros().get(k))))return;
        p.setHotelOpcoes(new LinkedHashMap<>(anterior.getHotelOpcoes()));
        p.setHotelSelecionado(anterior.getHotelSelecionado());
        p.setHotelInteracoes(Math.min(101,anterior.getHotelInteracoes()+1));
    }
    private static String normal(String s) {return s==null?"":s.trim().toLowerCase(Locale.ROOT);}
}
