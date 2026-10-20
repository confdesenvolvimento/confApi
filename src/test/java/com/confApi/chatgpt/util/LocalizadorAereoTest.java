package com.confApi.chatgpt.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LocalizadorAereoTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "NICC3S", "NOABC12", "NROABC12", "NABCDE", "NOABCDE", "NROABCDE",
            "NABC1", "N123456789012", "ABC123", "LA9572345FRJD"
    })
    void preservaCodigoCompletoMesmoQuandoComecaComAbreviacaoDeNumero(String codigo) {
        for (String rotulo : new String[]{"localizador", "loc", "reserva", "PNR"}) {
            assertEquals(codigo, LocalizadorAereo.extrair("quero remarcar o " + rotulo + " " + codigo));
        }
        assertEquals(codigo, LocalizadorAereo.extrair(codigo));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "quero remarcar o localizador NICC3S",
            "quero remarcar o localizador NICC3S.",
            "Quero remarcar o localizador nicc3s!",
            "quero remarcar o localizador: NICC3S",
            "quero remarcar NICC3S\nLocalizador NICC3S",
            "quero remarcar o localizador NICC3S\nLocalizador NICC3S"
    })
    void preservaNicc3sNaFraseDoUsuarioENoContextoAcrescentadoPelaV2(String mensagem) {
        assertEquals("NICC3S", LocalizadorAereo.extrair(mensagem));
    }

    @ParameterizedTest
    @ValueSource(strings = {"n ", "no ", "nro ", "n. ", "no. ", "nro. ",
            "n.", "n:", "n#", "n-", "nº ", "n° ", "nº. ", "n°. "})
    void aceitaAbreviacaoDeNumeroSomenteComSeparador(String prefixo) {
        assertEquals("NICC3S", LocalizadorAereo.extrair("localizador " + prefixo + "NICC3S"));
        // Codigo sem algarismos nao pode depender do fallback alfanumerico.
        assertEquals("NABCDE", LocalizadorAereo.extrair("reserva " + prefixo + "NABCDE"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"N1234567890123", "NO123456789012", "NRO12345678901", "NABC"})
    void naoTransformaCodigoForaDoLimiteEmOutroCodigoValido(String codigo) {
        assertNull(LocalizadorAereo.extrair("localizador " + codigo));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "quero remarcar", "localizador", "reserva", "qual a multa"})
    void naoInventaLocalizadorQuandoNaoInformado(String mensagem) {
        assertNull(LocalizadorAereo.extrair(mensagem));
    }
}
