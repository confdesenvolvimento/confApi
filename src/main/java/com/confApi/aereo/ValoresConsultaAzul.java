package com.confApi.aereo;

import com.confApi.aereo.dto.Reserva;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.db.confManager.reservaAereo.ReservaAereoValoresRQ;
import com.confApi.db.confManager.reservaValor.ReservaValor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/** A ConfAPI confere os precos Azul e prepara a atualizacao atomica do Manager. */
final class ValoresConsultaAzul {
    private ValoresConsultaAzul() { }

    static ReservaAereoValoresRQ preparar(Reserva fornecedor, ReservaAereo banco) {
        exigir(fornecedor != null && banco != null && banco.getCodgReservaAereo() != null && banco.getCodgReservaAereo() > 0
                && !nome(banco.getLocalizador()).isEmpty() && nome(banco.getLocalizador()).equals(nome(fornecedor.getLocalizador()))
                && fornecedor.getValorReserva() != null && fornecedor.getValorReserva().getValorBase() != null
                && banco.getPassageiros() != null && !banco.getPassageiros().isEmpty(), "Reserva ou valores ausentes.");
        var base = fornecedor.getValorReserva().getValorBase(); var valores = base.getValorPassageiroList();
        exigir(valores != null && valores.size() == banco.getPassageiros().size() && fornecedor.getPassageiros() != null
                && fornecedor.getPassageiros().size() == valores.size(), "Valores incompletos por passageiro.");
        var novos = new ArrayList<ReservaAereoValoresRQ.ValorPassageiro>();
        var anteriores = new ArrayList<ReservaAereoValoresRQ.ValorPassageiro>();
        Set<Integer> ids = new HashSet<>(); BigDecimal soma = BigDecimal.ZERO, venda = BigDecimal.ZERO;
        for (var pax : banco.getPassageiros()) {
            exigir(pax != null && pax.getCodgPassageiro() > 0 && ids.add(pax.getCodgPassageiro())
                    && pax.getReservaValores() != null && pax.getReservaValores().size() == 1 && pax.getReservaValores().get(0) != null,
                    "Cadastro de valores ambiguo.");
            var correspondentes = valores.stream().filter(v -> v != null && corresponde(v.getNomePassageiro(), pax.getNomePassageiro(), pax.getSobrenomePassageiro())).toList();
            long homonimos = banco.getPassageiros().stream().filter(p -> p != null && nome(p.getNomePassageiro()).equals(nome(pax.getNomePassageiro()))
                    && nome(p.getSobrenomePassageiro()).equals(nome(pax.getSobrenomePassageiro()))).count();
            long presentes = fornecedor.getPassageiros().stream().filter(p -> p != null && nome(p.getNome()).equals(nome(pax.getNomePassageiro()))
                    && nome(p.getSobrenome()).equals(nome(pax.getSobrenomePassageiro()))).count();
            exigir(correspondentes.size() == 1 && homonimos == 1 && presentes == 1, "Passageiro sem correspondencia unica.");
            var atual = correspondentes.get(0); var salvo = pax.getReservaValores().get(0);
            exigir("BRL".equals(salvo.getMoeda()), "Moeda local divergente.");
            BigDecimal totalFornecedor = dinheiro(atual.getTarifa()).add(dinheiro(atual.getTaxaEmbarque())).add(dinheiro(atual.getTaxaDU()))
                    .add(dinheiro(zero(atual.getRAV()))).add(dinheiro(zero(atual.getRC()))).add(dinheiro(zero(atual.getTaxaAssento())));
            exigir(totalFornecedor.compareTo(dinheiro(atual.getTotal())) == 0, "Total Azul por passageiro divergente.");
            soma = soma.add(totalFornecedor);
            BigDecimal tarifa = dinheiro(atual.getTarifa()).add(dinheiro(salvo.getValorTarifa()).subtract(dinheiro(salvo.getValorTarifaNet())));
            dinheiro(tarifa.doubleValue());
            BigDecimal total = tarifa.add(dinheiro(atual.getTaxaEmbarque())).add(dinheiro(atual.getTaxaDU()))
                    .add(dinheiro(zero(salvo.getValorRav()))).add(dinheiro(zero(salvo.getValorRc())))
                    .add(dinheiro(zero(salvo.getValorAssento()))).add(dinheiro(zero(salvo.getValorTaxaCombustivel())));
            novos.add(new ReservaAereoValoresRQ.ValorPassageiro(pax.getCodgPassageiro(), tarifa.doubleValue(), atual.getTaxaEmbarque(), atual.getTaxaDU(),
                    zero(salvo.getValorRav()), zero(salvo.getValorRc()), zero(salvo.getValorAssento()), total.doubleValue(), atual.getTarifa()));
            anteriores.add(new ReservaAereoValoresRQ.ValorPassageiro(pax.getCodgPassageiro(), salvo.getValorTarifa(), salvo.getValorTaxaEmbarque(), salvo.getValorDu(),
                    zero(salvo.getValorRav()), zero(salvo.getValorRc()), zero(salvo.getValorAssento()), totalLocal(salvo).doubleValue(), salvo.getValorTarifaNet()));
            venda = venda.add(total);
        }
        exigir(soma.compareTo(dinheiro(base.getTotal())) == 0 && soma.compareTo(dinheiro(fornecedor.getValorReserva().getValor())) == 0,
                "Total da consulta Azul divergente.");
        return new ReservaAereoValoresRQ(banco.getLocalizador(), venda.doubleValue(), novos, anteriores);
    }

    static boolean diverge(ReservaAereoValoresRQ pedido) {
        for (int i = 0; i < pedido.passageiros().size(); i++) {
            var novo = pedido.passageiros().get(i); var antigo = pedido.anteriores().get(i);
            Double[] atuais = {novo.tarifa(), novo.embarque(), novo.du(), novo.rav(), novo.rc(), novo.assento(), novo.tarifaNet()};
            Double[] salvos = {antigo.tarifa(), antigo.embarque(), antigo.du(), antigo.rav(), antigo.rc(), antigo.assento(), antigo.tarifaNet()};
            for (int j = 0; j < atuais.length; j++) if (dinheiro(atuais[j]).compareTo(dinheiro(salvos[j])) != 0) return true;
        }
        return false;
    }
    private static BigDecimal totalLocal(ReservaValor v) {
        return dinheiro(v.getValorTarifa()).add(dinheiro(v.getValorTaxaEmbarque())).add(dinheiro(v.getValorDu()))
                .add(dinheiro(zero(v.getValorRav()))).add(dinheiro(zero(v.getValorRc())))
                .add(dinheiro(zero(v.getValorAssento()))).add(dinheiro(zero(v.getValorTaxaCombustivel())));
    }
    private static BigDecimal dinheiro(Double v) {
        exigir(v != null && Double.isFinite(v) && v >= 0, "Preco invalido."); return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }
    private static Double zero(Double v) { return v == null ? 0D : v; }
    private static String nome(String v) { return v == null ? "" : v.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT); }
    private static boolean corresponde(String completo, String primeiro, String sobrenome) {
        if (nome(primeiro).isEmpty() || nome(sobrenome).isEmpty()) return false;
        String normalizado = nome(completo).replaceAll("\\s*/\\s*", "/");
        return normalizado.equals(nome(sobrenome) + "/" + nome(primeiro)) || normalizado.equals(nome(primeiro) + " " + nome(sobrenome))
                || normalizado.equals(nome(sobrenome) + " " + nome(primeiro));
    }
    private static void exigir(boolean c, String msg) { if (!c) throw new IllegalArgumentException(msg); }
}
