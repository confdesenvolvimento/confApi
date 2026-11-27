package com.confApi.hoteis;

import com.confApi.hoteis.model.reserva.ReservarRequestFront;
import com.confApi.hub.hotel.dto.HotelReserva;
import com.confApi.operacao.DivergenciaOperacao;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class DivergenciasHotel {
    private DivergenciasHotel() {}

    static List<DivergenciaOperacao> comparar(ReservarRequestFront pedido, HotelReserva resposta) {
        var reservas = resposta.getReservasHotelRsList();
        if (reservas == null || reservas.size() != 1 || reservas.get(0) == null) return List.of();
        var confirmada = reservas.get(0);
        if (confirmada.getIdentificador() == null || confirmada.getIdentificador().isBlank()
                || !confirmada(confirmada.getStatus())) return List.of();

        var divergencias = new ArrayList<DivergenciaOperacao>();
        var entrada = resposta.getDataEntrada() == null ? confirmada.getDataEntrada() : resposta.getDataEntrada();
        var saida = resposta.getDataSaida() == null ? confirmada.getDataSaida() : resposta.getDataSaida();
        DivergenciaOperacao.comparar(divergencias, "RESERVA", "DATA_ENTRADA",
                DivergenciaOperacao.dataUtc(pedido.getDtCheckIn()), DivergenciaOperacao.dataUtc(entrada));
        DivergenciaOperacao.comparar(divergencias, "RESERVA", "DATA_SAIDA",
                DivergenciaOperacao.dataUtc(pedido.getDtCheckOut()), DivergenciaOperacao.dataUtc(saida));
        var esperadas = pedido.getAcomodacoes();
        var retornadas = confirmada.getAcomodacoes();
        if (esperadas == null || retornadas == null || esperadas.size() != retornadas.size()) return divergencias;
        for (int i = 0; i < esperadas.size(); i++) {
            var esperada = esperadas.get(i);
            var retornada = retornadas.get(i);
            if (esperada == null || retornada == null || esperada.getCodgRoom() == null
                    || !esperada.getCodgRoom().equals(retornada.getCodgRoom())
                    || esperada.getTarifaHotel() == null || retornada.getTarifaHotel() == null) continue;
            var solicitada = esperada.getTarifaHotel();
            var fornecida = retornada.getTarifaHotel();
            String moedaSolicitada = moeda(solicitada.getMoeda());
            String moedaFornecida = moeda(fornecida.getMoeda());
            DivergenciaOperacao.comparar(divergencias, "RESERVA", "MOEDA", moedaSolicitada, moedaFornecida);
            if (moedaSolicitada != null && moedaSolicitada.equals(moedaFornecida)
                    && fornecida.getValorTotalEstadiaNet() != null && fornecida.getValorTotalEstadiaNet() > 0) {
                DivergenciaOperacao.comparar(divergencias, "RESERVA", "VALOR_TOTAL_NET",
                        DivergenciaOperacao.valor(solicitada.getValorTotalEstadiaNet()),
                        DivergenciaOperacao.valor(fornecida.getValorTotalEstadiaNet()));
            }
        }
        return divergencias.stream().distinct().toList();
    }

    private static boolean confirmada(String status) {
        return status != null && List.of("reserved", "confirmed", "confirmada").contains(status.strip().toLowerCase(Locale.ROOT));
    }

    private static String moeda(String moeda) {
        if (moeda == null || !moeda.matches("[a-zA-Z]{3}")) return null;
        return moeda.toUpperCase(Locale.ROOT);
    }
}