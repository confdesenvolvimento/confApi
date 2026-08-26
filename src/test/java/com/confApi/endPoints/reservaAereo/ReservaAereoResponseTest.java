package com.confApi.endPoints.reservaAereo;

import com.confApi.db.confManager.passageiro.Passageiro;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.db.confManager.reservaValor.ReservaValor;
import com.confApi.hub.aereo.BilheteHub;
import com.confApi.hub.aereo.ConsultarLocalizadorResponseHub;
import com.confApi.hub.aereo.PassageiroHub;
import com.confApi.hub.aereo.RecebimentoModel;
import com.confApi.hub.aereo.ReservaHub;
import com.confApi.hub.aereo.ValorBaseHub;
import com.confApi.hub.aereo.ValorReservaHub;
import com.confApi.endPoints.recebimento.RecebimentoResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReservaAereoResponseTest {

    private static final double TOLERANCIA = 0.001;

    @Test
    void deveManterValoresDoHubQuandoBancoTambemPossuiValores() {
        ReservaAereoResponse response = new ReservaAereoResponse(criarHub(true), criarReservaDb());

        assertEquals(2_491.84, response.getTarifaGeral(), TOLERANCIA);
        assertEquals(109.13, response.getTaxaEmbarqueGeral(), TOLERANCIA);
        assertEquals(249.18, response.getTaxaDUGeral(), TOLERANCIA);
        assertEquals(2_850.15, response.getValorTotalReserva(), TOLERANCIA);
        assertEquals("14/06/1994", response.getPassageiros().get(0).getNascimento());
        assertEquals(1, response.getPassageiros().get(0).getBilhetes().get(0).getStatus());
    }

    @Test
    void deveUsarBancoComoFallbackSemSomarTarifaETarifaNet() {
        ReservaAereoResponse response = new ReservaAereoResponse(criarHub(false), criarReservaDb());

        assertEquals(2_491.84, response.getTarifaGeral(), TOLERANCIA);
        assertEquals(109.13, response.getTaxaEmbarqueGeral(), TOLERANCIA);
        assertEquals(249.18, response.getTaxaDUGeral(), TOLERANCIA);
        assertEquals(2_850.15, response.getValorTotalReserva(), TOLERANCIA);
    }

    @Test
    void deveConverterRecebimentoSemFormaDePagamentoDetalhadaOuCartao() {
        RecebimentoResponse response = new RecebimentoResponse();
        response.setCodgFormaPagamento(5);
        response.setNomeFormaPagamento("Faturado");
        response.setValorEntrada(2_850.15);
        response.setValorPagamento(2_850.15);

        RecebimentoModel model = new RecebimentoModel(response);

        assertEquals(5, model.getFormaDePagamento().getCodgFormaPagto());
        assertEquals("Faturado", model.getNomeFormaPagamento());
        assertEquals(2_850.15, model.getValorPagamento(), TOLERANCIA);
    }

    private ConsultarLocalizadorResponseHub criarHub(boolean incluirValores) {
        PassageiroHub passageiro = new PassageiroHub();
        passageiro.setNome("HULLY");
        passageiro.setSobrenome("DA COSTA");
        passageiro.setNascimento(Date.from(
                LocalDate.of(1994, 6, 14)
                        .atStartOfDay(ZoneId.of("America/Sao_Paulo"))
                        .toInstant()
        ));
        BilheteHub bilhete = new BilheteHub();
        bilhete.setStatus("Ativa");
        bilhete.setNumero("5771234567890");
        passageiro.setBilhetes(List.of(bilhete));

        ReservaHub reserva = new ReservaHub();
        reserva.setLocalizador("VMJ9GJ");
        reserva.setPassageiros(List.of(passageiro));

        if (incluirValores) {
            ValorBaseHub valorBase = new ValorBaseHub();
            valorBase.setTarifa(2_491.84);
            valorBase.setTaxaEmbarque(109.13);
            valorBase.setTaxaDU(249.18);
            valorBase.setTotal(2_850.15);

            ValorReservaHub valorReserva = new ValorReservaHub();
            valorReserva.setValorBase(valorBase);
            reserva.setValorReserva(valorReserva);
        }

        ConsultarLocalizadorResponseHub hub = new ConsultarLocalizadorResponseHub();
        hub.setReservas(List.of(reserva));
        return hub;
    }

    private ReservaAereo criarReservaDb() {
        ReservaValor valor = new ReservaValor();
        valor.setValorTarifa(2_491.84);
        valor.setValorTarifaNet(2_491.84);
        valor.setValorTaxaEmbarque(109.13);
        valor.setValorDu(249.18);

        Passageiro passageiro = new Passageiro();
        passageiro.setNomePassageiro("HULLY");
        passageiro.setSobrenomePassageiro("DA COSTA");
        passageiro.setReservaValores(List.of(valor));

        ReservaAereo reserva = new ReservaAereo();
        reserva.setCodgReservaAereo(1);
        reserva.setPassageiros(List.of(passageiro));
        return reserva;
    }
}
