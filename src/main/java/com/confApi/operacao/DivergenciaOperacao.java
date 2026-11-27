package com.confApi.operacao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record DivergenciaOperacao(String operacaoId, String operacao, String campo, String solicitado, String retornado) {
    public static DivergenciaOperacao registrar(String operacao, String campo, String solicitado, String retornado) {
        String id = RastreioReserva.id();
        return new DivergenciaOperacao(id == null ? UUID.randomUUID().toString() : id, operacao, campo, solicitado, retornado);
    }

    public static void comparar(List<DivergenciaOperacao> divergencias, String operacao, String campo, Object solicitado, Object retornado) {
        if (solicitado != null && retornado != null && !Objects.equals(solicitado, retornado))
            divergencias.add(registrar(operacao, campo, solicitado.toString(), retornado.toString()));
    }

    public static LocalDate dataUtc(Date data) {
        return data == null ? null : Instant.ofEpochMilli(data.getTime()).atOffset(ZoneOffset.UTC).toLocalDate();
    }

    public static BigDecimal valor(Double valor) {
        return valor == null || !Double.isFinite(valor) ? null
                : BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
    }
}