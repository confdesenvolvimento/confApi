package com.confApi.carros;

import com.confApi.carros.dto.CarroErroMensagem;
import com.confApi.carros.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CarroErroMensagemTest {
    private static final String SUPORTE = "Please report the error code to Mobility support: 6b5d3a55-e84f-4d2b-b214-49d546c3f238";
    private static final String FECHADA = "A loja não está aberta no horário selecionado.";

    private List<ErrorResponse> erros(String mensagem) {
        var erro = new ErrorResponse();
        erro.setErrorDescription(mensagem);
        return List.of(erro);
    }

    @Test void referenciaNaoOcultaMotivoNoCampoMensagem() {
        assertEquals(FECHADA + " | " + SUPORTE, CarroErroMensagem.obter(erros(SUPORTE), null, FECHADA, "Falha"));
    }

    @Test void referenciaNaoOcultaMotivoNoReturnMessage() {
        assertEquals(FECHADA + " | " + SUPORTE,
                CarroErroMensagem.obter(erros(SUPORTE), Map.of("message", FECHADA), null, "Falha"));
    }

    @Test void motivoEspecificoContinuaPrioritarioEPreservaReferencia() {
        assertEquals(FECHADA + " | " + SUPORTE, CarroErroMensagem.obter(erros(FECHADA), SUPORTE, "Falha", "Fallback"));
    }

    @Test void apenasReferenciaNaoViraMotivoInventado() {
        assertEquals(SUPORTE, CarroErroMensagem.obter(erros(SUPORTE), SUPORTE, SUPORTE, "Falha"));
    }

    @Test void falhaSemDescricaoUsaFallback() {
        assertEquals("Falha", CarroErroMensagem.obter(null, Map.of(), "", "Falha"));
    }

    @Test void preservaMotivosNaoRelacionadosAoHorarioDaLoja() {
        assertEquals("Documento inválido.", CarroErroMensagem.obter(erros("Documento inválido."), "Falha", null, "Fallback"));
    }
}
