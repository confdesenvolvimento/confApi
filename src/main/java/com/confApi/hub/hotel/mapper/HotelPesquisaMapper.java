package com.confApi.hub.hotel.mapper;

import com.confApi.hoteis.model.pesquisa.HotelPesquisaIdadeCriancaFront;
import com.confApi.hoteis.model.pesquisa.HotelPesquisaModelFront;
import com.confApi.hoteis.model.pesquisa.HotelPesquisaQuartoFront;
import com.confApi.hub.hotel.dto.HotelPesquisaIdadeCrianca;
import com.confApi.hub.hotel.dto.HotelPesquisaModel;
import com.confApi.hub.hotel.dto.HotelPesquisaQuarto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HotelPesquisaMapper {

    private HotelPesquisaMapper() {}


    public static HotelPesquisaModel toHub(HotelPesquisaModelFront req) {
        HotelPesquisaModel model = new HotelPesquisaModel();

        model.setDataEntrada(parse(req.getDataEntrada()));
        model.setDataSaida(parse(req.getDataSaida()));

        model.setQuantidadeQuartos(req.getQuantidadeQuartos());
        model.setQuantidadeNoites(req.getQuantidadeNoites());

        model.setCodgCidade(req.getCodgCidade());
        model.setNomeCidade(req.getNomeCidade());
        model.setNomeEstado(req.getNomeEstado());
        model.setNomePais(req.getNomePais());

        List<HotelPesquisaQuarto> quartos = new ArrayList<>();
        int idx = 1;

        for (HotelPesquisaQuartoFront q : req.getQuartoPesquisa()) {
            HotelPesquisaQuarto hpq = new HotelPesquisaQuarto();
            hpq.setId(idx++);
            hpq.setNomeQuartoPesquisa("Quarto " + hpq.getId());
            hpq.setQtdAdultos(q.getQtdAdultos());
            hpq.setQtdCriancas(q.getQtdCriancas());
            List<HotelPesquisaIdadeCrianca> idadesCriancas = new ArrayList<>();
            for(HotelPesquisaIdadeCriancaFront idadeCrianca : q.getIdadeCriancas()){
                HotelPesquisaIdadeCrianca hotelPesquisaIdadeCrianca = new HotelPesquisaIdadeCrianca();
                hotelPesquisaIdadeCrianca.setIdadeCrianca(idadeCrianca.getIdadeCrianca());
                idadesCriancas.add(hotelPesquisaIdadeCrianca);
            }
            hpq.setIdadeCriancas(idadesCriancas);
            quartos.add(hpq);
        }

        model.setQuartoPesquisa(quartos);

        // comissão extra
       /* model.setTipoComissao(req.getPesquisa().getTipoComissao());
        model.setValorComissao(req.getPesquisa().getValorComissao());
        model.setPercentualComissao(req.getPesquisa().getPercentualComissao());*/
        return model;
    }



    private static String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static Integer nullSafeInt(Integer v, int def) {
        return v == null ? def : v;
    }
    private static final SimpleDateFormat DF =
            new SimpleDateFormat("yyyy-MM-dd");

    private static Date parse(String value) {
        try {
            return value == null ? null : DF.parse(value);
        } catch (ParseException e) {
            throw new IllegalArgumentException(
                    "Data inválida (esperado yyyy-MM-dd): " + value
            );
        }
    }
}
