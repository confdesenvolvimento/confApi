package com.confApi.hub.aereo.dto;

import com.confApi.hub.aereo.PassageiroModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class PassageiroNascimentoTest {
    private final ObjectMapper json = new ObjectMapper();

    @ParameterizedTest
    @CsvSource({
            "2002-10-15, 2002-10-15", "15/10/2002, 2002-10-15",
            "2002-03-04, 2002-03-04", "04/03/2002, 2002-03-04",
            "2000-02-29, 2000-02-29", "29/02/2000, 2000-02-29"
    })
    void nascimentoSerializadoParaHubPreservaDataCivil(String entrada, String esperado) throws Exception {
        PassageiroModel model = new PassageiroModel();
        model.setNascimento(entrada);
        Passageiro passageiro = new Passageiro(model, null);

        assertEquals(esperado + "T12:00:00", passageiro.getNascimento());
        assertEquals(LocalDate.parse(esperado), passageiro.getDataNascimento().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate());
        // Reproduz a leitura como java.util.Date, que falhava no HUB com a string corrompida.
        String nascimentoJson = json.writeValueAsString(json.valueToTree(passageiro).get("nascimento"));
        Date dataHub = json.readValue(nascimentoJson, Date.class);
        assertEquals(LocalDate.parse(esperado).atTime(12, 0).toInstant(ZoneOffset.UTC), dataHub.toInstant());
        assertEquals(entrada, model.getNascimento());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings={" "})
    void nascimentoAusentePermaneceNulo(String entrada) {
        PassageiroModel model = new PassageiroModel();
        model.setNascimento(entrada);
        Passageiro passageiro = new Passageiro(model, null);
        assertNull(passageiro.getNascimento());
        assertNull(passageiro.getDataNascimento());
    }

    @ParameterizedTest
    @ValueSource(strings={"2002-02-29", "29/02/2002", "1900-02-29", "29/02/1900",
            "2002-04-31", "31/04/2002", "2002-13-15", "15/13/2002", "15/10", "0-15-2--20T12:00:00"})
    void rejeitaNascimentoInvalidoAntesDoEnvio(String entrada) {
        PassageiroModel model = new PassageiroModel();
        model.setNascimento(entrada);
        assertThrows(DateTimeParseException.class, () -> new Passageiro(model, null));
    }
}
