package com.confApi.aereo;

import com.confApi.aereo.dto.*;
import com.confApi.db.confManager.usuario.dto.UsuarioDto;
import com.confApi.hub.aereo.ReservaValoresAereo;
import com.confApi.hub.aereo.dto.Contato;
import com.confApi.hub.aereo.dto.Passageiro;
import com.confApi.hub.aereo.dto.TrechoReserva;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AereoReservaTaxasPersistenciaTest {
    private final ObjectMapper json = new ObjectMapper();

    private Reserva reserva(List<Passageiro> passageiros, List<ValorPassageiro> valores) {
        Reserva r = mock(Reserva.class, RETURNS_DEEP_STUBS);
        when(r.getSistema()).thenReturn("Wooba");
        TrechoReserva trecho = mock(TrechoReserva.class, RETURNS_DEEP_STUBS);
        when(trecho.getVoos()).thenReturn(List.of());
        when(r.getViagens()).thenReturn(List.of(trecho));
        when(r.getPrazoEmissao()).thenReturn("2026-11-11T12:00:00.000-03:00");
        when(r.getPassageiros()).thenReturn(passageiros);
        when(r.getContatos()).thenReturn(List.of(new Contato()));
        when(r.getValorReserva().getValorBase().getValorPassageiroList()).thenReturn(valores);
        return r;
    }

    private Passageiro passageiro(String nome, String faixa) {
        Passageiro p = new Passageiro(); p.setNome(nome); p.setSobrenome("TESTE"); p.setFaixaEtaria(faixa); return p;
    }

    private PreReserva preReserva() {
        PreReserva pre = new PreReserva(); UsuarioDto usuario = new UsuarioDto(); usuario.setId(1); pre.setUsuario(usuario);
        pre.setValoresReservaAdt(List.of(new ReservaValoresAereo(1)));
        pre.setValoresReservaChd(List.of(new ReservaValoresAereo(2)));
        pre.setValoresReservaInf(List.of(new ReservaValoresAereo(3)));
        return pre;
    }

    private ValorPassageiro valor(String nome, double rav, double rc) throws Exception {
        return json.readValue("""
            {"nomePassageiro":"%s TESTE","tarifa":3396.99,"taxaEmbarque":498.66,
             "taxaDU":0,"rav":%s,"rc":%s,"taxaAssento":0}
            """.formatted(nome, rav, rc), ValorPassageiro.class);
    }

    @ParameterizedTest
    @CsvSource({"ADT,237.79,0,4133.44", "ADT,237.79,50,4183.44", "CHD,100,25,4020.65", "INF,0,10,3905.65"})
    void gravaTaxasDoFornecedorMesmoComPreReservaSemValores(String faixa, double rav, double rc, double total) throws Exception {
        Reserva r = reserva(List.of(passageiro("PASSAGEIRO", faixa)), List.of(valor("PASSAGEIRO", rav, rc)));
        var db = new AereoService().convertToReservaAereo(r, preReserva());
        var salvo = db.getPassageiros().get(0).getReservaValores().get(0);
        assertEquals(rav, salvo.getValorRav());
        assertEquals(rc, salvo.getValorRc());
        assertEquals(total, salvo.getValorTarifa() + salvo.getValorTaxaEmbarque() + salvo.getValorDu()
                + salvo.getValorRav() + salvo.getValorRc(), 0.001);
        assertEquals(rav, json.valueToTree(salvo).path("valorRav").asDouble());
    }

    @Test void naoTrocaPrecosEntrePassageirosNemAlteraPreReservaCompartilhada() throws Exception {
        PreReserva pre = preReserva();
        Reserva r = reserva(List.of(passageiro("PRIMEIRO", "ADT"), passageiro("SEGUNDO", "ADT")),
                List.of(valor("SEGUNDO", 20, 2), valor("PRIMEIRO", 10, 1)));
        var passageiros = new AereoService().convertToReservaAereo(r, pre).getPassageiros();
        assertEquals(10, passageiros.get(0).getReservaValores().get(0).getValorRav());
        assertEquals(1, passageiros.get(0).getReservaValores().get(0).getValorRc());
        assertEquals(20, passageiros.get(1).getReservaValores().get(0).getValorRav());
        assertEquals(2, passageiros.get(1).getReservaValores().get(0).getValorRc());
        assertEquals(0, pre.getValoresReservaAdt().get(0).getValorTarifa());
    }

    @ParameterizedTest
    @CsvSource({"Wooba,Azul,Wooba", "Azul,Wooba,Azul", ",Wooba,Wooba"})
    void gravaIntegracaoDoFornecedorSemFixarIdDoCadastro(String fornecedor, String pedido, String esperado) throws Exception {
        Reserva r = reserva(List.of(passageiro("PASSAGEIRO", "ADT")), List.of(valor("PASSAGEIRO", 237.79, 0)));
        when(r.getSistema()).thenReturn(fornecedor);
        PreReserva pre = preReserva(); pre.setSistema(pedido);
        var sistema = new AereoService().convertToReservaAereo(r, pre).getCodgSistema();
        assertEquals(esperado, sistema.getNomeSistema());
        assertNull(sistema.getCodgSistema());
        assertFalse(json.valueToTree(sistema).has("codgSistema"));
    }

    @Test void naoInventaIntegracaoQuandoFornecedorEPedidoNaoInformamSistema() throws Exception {
        Reserva r = reserva(List.of(passageiro("PASSAGEIRO", "ADT")), List.of(valor("PASSAGEIRO", 237.79, 0)));
        when(r.getSistema()).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> new AereoService().convertToReservaAereo(r, preReserva()));
    }
}
