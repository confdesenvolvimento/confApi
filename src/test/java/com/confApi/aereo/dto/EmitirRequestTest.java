package com.confApi.aereo.dto;

import com.confApi.hub.aereo.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EmitirRequestTest {
    @ParameterizedTest
    @ValueSource(ints = {2, 35})
    void devePreservarModalidadeCartaoEParcelas(int codigo) {
        ReservaAereoModel reserva = reserva(codigo);
        EmitirRequest request = new EmitirRequest(reserva);
        assertEquals(codigo, request.getFormaDePagamento());
        assertEquals("VI", request.getBandeiraCartao());
        assertEquals("4111111111111111", request.getNumeroCartao());
        assertEquals("123", request.getCodigoDeSeguranca());
        assertEquals("TITULAR TESTE", request.getTitularNome());
        assertEquals("04/28", request.getValidade());
        assertEquals(2, request.getParcelas());
        assertEquals(100.0, request.getValor());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 70})
    void naoDeveEnviarCartaoParaFaturadoOuPrazoEspecial(int codigo) {
        EmitirRequest request = new EmitirRequest(reserva(codigo));
        assertEquals(codigo, request.getFormaDePagamento());
        assertNull(request.getNumeroCartao());
        assertNull(request.getParcelas());
    }

    private ReservaAereoModel reserva(int codigo) {
        ReservaAereoModel reserva = new ReservaAereoModel();
        FormaPagamentoModel forma = new FormaPagamentoModel();
        forma.setCodgFormaPagto(codigo);
        reserva.setFormaPagamentoSelecionada(forma);
        CartaoModel cartao = new CartaoModel();
        cartao.setSiglaBandeira("VI");
        cartao.setNumeroCartao("4111111111111111");
        cartao.setCodgSegurancaCartao("123");
        cartao.setTitularBandeira("TITULAR TESTE");
        cartao.setValidadeCartao("04/28");
        cartao.setQuantidadeParcelas("2");
        cartao.setParcelasCartao(List.of(new ParcelaCartaoModel(2)));
        RecebimentoModel recebimento = new RecebimentoModel();
        recebimento.setCartaoSelecionado(cartao);
        recebimento.setValorPagamento(100.0);
        reserva.setRecebimento(recebimento);
        return reserva;
    }
}
