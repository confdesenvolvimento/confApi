package com.confApi.hub.hotel.mapper;

import com.confApi.hoteis.model.reserva.HotelAcomodacaoFront;
import com.confApi.hoteis.model.reserva.ReservarRequestFront;
import com.confApi.hub.hotel.dto.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public final class HotelReservaMapper {

    private HotelReservaMapper() {}

    // Ajuste os formatos conforme seu front:
    // - Se o front manda "yyyy-MM-dd" ou "dd/MM/yyyy"
    private static final SimpleDateFormat DF_DDMMYYYY = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat DF_YYYYMMDD = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Converte o request do Front para o ReservarRequest esperado pelo HUB.
     */
    public static ReservarRequest toHub(ReservarRequestFront front) {
        if (front == null) return null;

        ReservarRequest hub = new ReservarRequest();

        // 1) Datas + dados principais
        hub.setDtCheckIn(front.getDtCheckIn());
        hub.setDtCheckOut(front.getDtCheckOut());

        // Esses podem vir do front (se vier nulo, segue nulo como no legado)
        hub.setDataCriacao(front.getDataCriacao());
        hub.setInfoGlobal(front.getInfoGlobal());
        hub.setIdentificador(front.getIdentificador());
        hub.setStatus(front.getStatus());

        hub.setCodgHotel(front.getCodgHotel());
        hub.setCodgCidade(front.getCodgCidade());
        hub.setSearchToken(front.getSearchToken());

        // 2) Acomodações
        List<HotelAcomodacaoFront> acomodacoesFront =
                front.getAcomodacoes() == null ? Collections.emptyList() : front.getAcomodacoes();

        hub.setAcomodacoes(new ArrayList<>());

        for (HotelAcomodacaoFront aFront : acomodacoesFront) {
            if (aFront == null) continue;

            HotelAcomodacao aHub = new HotelAcomodacao();
            aHub.setCodgPlanoTarifa(aFront.getCodgPlanoTarifa());
            aHub.setCodgRoom(aFront.getCodgRoom());
            aHub.setSiglaTipoQuarto(aFront.getSiglaTipoQuarto());
            aHub.setSistema(aFront.getSistema());

            // Tarifa
            if (aFront.getTarifaHotel() != null) {
                TarifaHotel th = new TarifaHotel();
                th.setValorTotalEstadiaNet(aFront.getTarifaHotel().getValorTotalEstadiaNet());
                th.setMoeda(aFront.getTarifaHotel().getMoeda());
                aHub.setTarifaHotel(th);
            } else {
                aHub.setTarifaHotel(null);
            }

            // Forma pagamento
            aHub.setFormaPagamento(aFront.getFormaPagamento());

            // Hóspedes (com idade + dataNascimento formatada)
            if (aFront.getHospedes() != null && !aFront.getHospedes().isEmpty()) {
                aHub.setHospedes(new ArrayList<>());

                for (Hospedes h : aFront.getHospedes()) {
                    if (h == null) continue;

                    // Regra do legado:
                    // - se dataNascimento != null:
                    //   - calcula idade
                    //   - converte formato da data e seta no próprio campo
                    if (h.getDataNascimento() != null && !String.valueOf(h.getDataNascimento()).trim().isEmpty()) {

                        Date dn = tryParseToDate(String.valueOf(h.getDataNascimento()).trim());
                        if (dn != null) {
                            h.setIdade(calcularIdade(dn));
                          //  h.setDataNascimento(converterFormatoData(dn)); // dd/MM/yyyy
                        } else {
                            // se não conseguir parsear, mantém como veio (ou você pode lançar erro)
                            // h.setDataNascimento(h.getDataNascimento());
                        }
                    }
                    aHub.getHospedes().add(h);
                }
            } else {
                aHub.setHospedes(new ArrayList<>()); // evita null
            }
            hub.getAcomodacoes().add(aHub);
        }

        return hub;
    }

    // =========================
    // Helpers (iguais ao legado)
    // =========================

    private static int calcularIdade(Date nascimento) {
        Calendar nasc = Calendar.getInstance();
        nasc.setTime(nascimento);

        Calendar hoje = Calendar.getInstance();

        int idade = hoje.get(Calendar.YEAR) - nasc.get(Calendar.YEAR);

        int mesHoje = hoje.get(Calendar.MONTH);
        int mesNasc = nasc.get(Calendar.MONTH);

        if (mesHoje < mesNasc || (mesHoje == mesNasc && hoje.get(Calendar.DAY_OF_MONTH) < nasc.get(Calendar.DAY_OF_MONTH))) {
            idade--;
        }
        return Math.max(0, idade);
    }

    private static String converterFormatoData(Date data) {
        // legado costuma mandar dd/MM/yyyy
        return DF_DDMMYYYY.format(data);
    }

    /**
     * Tenta parsear data recebida do front.
     * Aceita "yyyy-MM-dd" e "dd/MM/yyyy".
     */
    private static Date tryParseToDate(String value) {
        if (value == null || value.isBlank()) return null;

        // já veio com tempo? corta só a data se necessário
        // ex: "2025-12-21T16:56:42.401" -> "2025-12-21"
        String v = value.trim();
        if (v.length() >= 10 && v.charAt(4) == '-' && v.charAt(7) == '-') {
            v = v.substring(0, 10);
        }

        try {
            // yyyy-MM-dd
            if (v.length() == 10 && v.charAt(4) == '-' && v.charAt(7) == '-') {
                return DF_YYYYMMDD.parse(v);
            }
            // dd/MM/yyyy
            if (v.length() == 10 && v.charAt(2) == '/' && v.charAt(5) == '/') {
                return DF_DDMMYYYY.parse(v);
            }
        } catch (ParseException ignored) {
            // cai fora
        }
        return null;
    }
}

