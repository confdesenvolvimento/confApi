package com.confApi.rodoviario;

import com.confApi.db.confManager.rodoviario.dto.*;
import com.confApi.rodoviario.dto.RodoviarioCompraModel;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import java.math.BigDecimal;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RodoviarioReservaMapperTest {
    private final ObjectMapper json = Jackson2ObjectMapperBuilder.json().build();
    private final RodoviarioReservaMapper mapper = new RodoviarioReservaMapper(json);
    private SalvarReservaRodoviarioRequest template() throws Exception {
        return json.readValue(RodoviarioReservaServiceTest.REQUEST, RodoviarioCompraModel.class).getDadosManager();
    }
    @Test void associaNomesComAcentosEEspacosSemModificarMetadadosLocais() throws Exception {
        var compra = json.readTree(RodoviarioReservaServiceTest.COMPRA.replace("Maria Silva", "MAR\u00cdA   S\u00cdLVA"));
        var metadados = template();
        var mapped = mapper.mapear(metadados, compra, Map.of(11, "ROD_1", 22, "CIT_15"));
        assertEquals("ABC123", mapped.getTrechos().get(0).getPassageiros().get(0).getBilhetes().get(0).getNumerBilhete());
        assertNull(metadados.getReserva().getLocalizador());
    }
    @Test void vinculaIdaEVoltaPelosDestinosMesmoSeFornecedorInverterOrdem() throws Exception {
        var metadados = template();
        var volta = json.convertValue(metadados.getTrechos().get(0), SalvarReservaRodoviarioRequest.TrechoCompleto.class);
        volta.getTrecho().setCodgCidadeOrigem(22); volta.getTrecho().setCodgCidadeDestino(11); metadados.getTrechos().add(volta);
        ObjectNode compra = (ObjectNode) json.readTree(RodoviarioReservaServiceTest.COMPRA);
        ObjectNode idaFornecedor = compra.withArray("trechos").get(0).deepCopy();
        ObjectNode voltaFornecedor = idaFornecedor.deepCopy();
        ((ObjectNode) voltaFornecedor.get("origem")).put("codigoFornecedor", "CIT_15");
        ((ObjectNode) voltaFornecedor.get("destino")).put("codigoFornecedor", "ROD_1");
        voltaFornecedor.put("partida", "2030-01-20T09:00:00");
        voltaFornecedor.put("chegada", "2030-01-20T19:00:00");
        compra.set("trechos", json.createArrayNode().add(voltaFornecedor).add(idaFornecedor));
        var mapped = mapper.mapear(metadados, compra, Map.of(11, "ROD_1", 22, "CIT_15"));
        assertEquals("2030-01-15", mapped.getTrechos().get(0).getTrecho().getDataPartida().toString());
        assertEquals("2030-01-20", mapped.getTrechos().get(1).getTrecho().getDataPartida().toString());
    }
    @Test void estadoPagoComBilhetePendenteNaoConfirmaReserva() throws Exception {
        assertEquals(0, mapper.status(json.readTree(RodoviarioReservaServiceTest.COMPRA.replace("emitted", "pending"))));
        assertEquals(0, mapper.status(json.readTree(RodoviarioReservaServiceTest.COMPRA.replace("paid", "canceled"))));
    }
    @Test void preservaTotalDeVendaInformadoPeloFrontECustoRealDoFornecedor() throws Exception {
        var req = template(); req.getValores().setValorTotalMkp(new BigDecimal("20.00"));
        var mapped = mapper.mapear(req, json.readTree(RodoviarioReservaServiceTest.COMPRA), Map.of(11, "ROD_1", 22, "CIT_15"));
        assertEquals(new BigDecimal("20.00"), mapped.getValores().getValorTotalMkp());
        assertEquals(0, new BigDecimal("22.40").compareTo(mapped.getValores().getValorTotalNet()));
    }
    @Test void rejeitaValorInvalidoAntesDaOperacaoExterna() throws Exception {
        var req = template(); req.getValores().setValorTotalMkp(new BigDecimal("-1.00"));
        assertEquals(400, assertThrows(RodoviarioIntegracaoException.class, () -> mapper.validar(req)).getStatus());
    }
    @Test void divergenciaDeDestinosIndicaVinculosECodigosQuePrecisamSerConferidos() throws Exception {
        var compra = json.readTree(RodoviarioReservaServiceTest.COMPRA
                .replace("\"codigoFornecedor\":\"ROD_1\"", "\"codigoFornecedor\":\"ROD_2340\"")
                .replace("\"codigoFornecedor\":\"CIT_15\"", "\"codigoFornecedor\":\"ROD_1\""));
        var erro = assertThrows(RodoviarioIntegracaoException.class,
                () -> mapper.mapear(template(), compra, Map.of(11, "ROD_1", 22, "CIT_15")));
        String motivo = erro.getDiagnosticoLocal();
        assertEquals("CONFERENCIA_NECESSARIA", erro.getResposta().path("codigo").asText());
        assertTrue(motivo.contains("origem 11 corresponde a ROD_1"));
        assertTrue(motivo.contains("destino 22 corresponde a CIT_15"));
        assertTrue(motivo.contains("ROD_2340 -> ROD_1"));
        assertFalse(motivo.contains("Maria Silva")); assertFalse(motivo.contains("12345678909"));
    }
    @Test void diagnosticoDeDestinosNaoIncluiTextoArbitrarioDoFornecedor() throws Exception {
        ObjectNode compra = (ObjectNode) json.readTree(RodoviarioReservaServiceTest.COMPRA);
        ((ObjectNode) compra.withArray("trechos").get(0).path("origem")).put("codigoFornecedor", "SEGREDO_TOKEN\n");
        var erro = assertThrows(RodoviarioIntegracaoException.class,
                () -> mapper.mapear(template(), compra, Map.of(11, "ROD_1", 22, "CIT_15")));
        assertTrue(erro.getDiagnosticoLocal().contains("codigo nao reconhecido"));
        assertFalse(erro.getDiagnosticoLocal().contains("SEGREDO_TOKEN"));
    }
}
