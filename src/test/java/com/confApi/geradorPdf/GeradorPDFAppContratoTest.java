package com.confApi.geradorPdf;

import com.confApi.db.confManager.usuario.dto.UsuarioConfDto;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.endPoints.reservaAereo.ReservaAereoResponse;
import com.confApi.geradorPdf.aereo.*;
import com.confApi.hub.aereo.ConsultarLocalizadorResponseHub;
import com.confApi.hub.aereo.ReservaAereoModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.junit.jupiter.api.Assertions.*;

class GeradorPDFAppContratoTest {
    private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();

    @ParameterizedTest
    @ValueSource(strings = {"\"bagagens\":null,\"assentos\":null,", "\"bagagens\":[],", "\"bagagens\":[],\"assentos\":[],"})
    void montaPdfCanceladoComListasOpcionaisAusentes(String listas) throws Exception {
        var hub = mapper.readValue("""
            {"reservas":[{"localizador":"ABC123","status":"Cancelada","sistema":"Wooba",
              "passageiros":[{"nome":"TESTE","sobrenome":"PASSAGEIRO"}],
              "viagens":[{"companhia":{"codigoIata":"G3"},"origem":{"codigoIata":"GIG"},
                "destino":{"codigoIata":"SJK"},"voos":[{%s
                  "numeroVoo":"2116","ciaMandatoria":{"codigoIata":"G3"},
                  "origem":{"codigoIata":"GIG"},"destino":{"codigoIata":"SJK"}}]}]}]}
            """.formatted(listas), ConsultarLocalizadorResponseHub.class);
        var db = mapper.readValue("""
            {"codgReservaAereo":77,"localizador":"ABC123","status":2,
             "codgCompanhiaAerea":{"codgCompanhiaAerea":3,"iataCia":"G3"},
             "codgAgencia":{"codgAgencia":42,"nomeAgencia":"Agência Teste"},
             "codgUsuarioCriacao":{"codgUsuario":9,"loginUsuario":"operador"}}
            """, ReservaAereo.class);
        var reserva = new ReservaAereoModel(new ReservaAereoResponse(hub, db));
        var envio = new EnvioReservaAereoPDF(new GeradorAereoPDFModel(reserva, new UsuarioConfDto(), new PlanoViagemReservaAereoPDF()));
        var pdf = envio.getReservaAereoModelPDF();
        assertEquals("Cancelada", pdf.getStatusReserva());
        assertEquals("ABC123", pdf.getLocalizador());
        assertEquals(77L, pdf.getCodgReservaAereoDB());
        assertEquals(1, pdf.getPassageiros().size());
        var voo = pdf.getTrechos().get(0).getVoos().get(0);
        assertEquals("2116", voo.getNumeroVoo());
        assertTrue(voo.getBagagens().isEmpty());
        assertTrue(voo.getAssentos().isEmpty());
    }

    @Test
    void preservaAssuntoEMensagemDeCancelamentoDoApp() throws Exception {
        var request = mapper.readValue("""
            {"reservaLocalizaodr":"ABC123","emails":"operador@example.test;agencia@example.test",
             "semValores":false,"assunto":"Reserva Aérea Cancelada - ABC123",
             "mensagem":"A reserva aérea foi cancelada com sucesso."}
            """, GeradorAereoPDF.class);
        var plano = new PlanoViagemReservaAereoPDF(request);
        assertEquals("Reserva Aérea Cancelada - ABC123", plano.getAssunto());
        assertEquals("A reserva aérea foi cancelada com sucesso.", plano.getMensagem());
        assertEquals("operador@example.test;agencia@example.test", plano.getEmailPara());
    }

    @Test
    void mantemEnvioLegadoSemCamposOpcionais() throws Exception {
        var request = mapper.readValue("{\"reservaLocalizaodr\":\"ABC123\"}", GeradorAereoPDF.class);
        var plano = new PlanoViagemReservaAereoPDF(request);
        assertEquals("Plano de viagem - ABC123", plano.getAssunto());
        assertTrue(plano.getMensagem().contains("ABC123"));
        assertFalse(plano.isSemValores());
    }

    @Test
    void assuntoEmBrancoUsaPadraoEMantemSemValores() throws Exception {
        var request = mapper.readValue("""
            {"reservaLocalizaodr":"ABC123","assunto":" ","mensagem":" ","semValores":true}
            """, GeradorAereoPDF.class);
        var plano = new PlanoViagemReservaAereoPDF(request);
        assertEquals("Plano de viagem - ABC123", plano.getAssunto());
        assertTrue(plano.getMensagem().contains("ABC123"));
        assertTrue(plano.isSemValores());
    }
}
