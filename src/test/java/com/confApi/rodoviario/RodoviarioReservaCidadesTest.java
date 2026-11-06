package com.confApi.rodoviario;

import com.confApi.db.confManager.rodoviario.dto.RodoviarioDestinoDTO;
import com.confApi.db.confManager.rodoviario.dto.SalvarReservaRodoviarioRequest;
import com.confApi.rodoviario.dto.RodoviarioCompraModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RodoviarioReservaCidadesTest {
    final ObjectMapper json=Jackson2ObjectMapperBuilder.json().build();
    final RodoviarioReservaMapper mapper=new RodoviarioReservaMapper(json);
    SalvarReservaRodoviarioRequest pedido() throws Exception {
        var dados=json.readValue(RodoviarioReservaServiceTest.REQUEST,RodoviarioCompraModel.class).getDadosManager();
        dados.getTrechos().get(0).getTrecho().setCodgCidadeOrigem(319);
        dados.getTrechos().get(0).getTrecho().setCodgCidadeDestino(99);
        return dados;
    }
    List<RodoviarioDestinoDTO> catalogo(){return List.of(
            new RodoviarioDestinoDTO(3692,319,"ROD_1"),new RodoviarioDestinoDTO(7233,99,"ROD_806"),
            new RodoviarioDestinoDTO(4000,319,"ROD_999"),new RodoviarioDestinoDTO(3469,99,"CIT_799"),
            new RodoviarioDestinoDTO(319,555,"CIT_1285"),new RodoviarioDestinoDTO(99,888,"CIT_1087"));}
    ObjectNode compra() throws Exception {
        return (ObjectNode) json.readTree(RodoviarioReservaServiceTest.COMPRA.replace("CIT_15","ROD_806"));
    }
    @Test void recebeCidadeESelecionaTerminalDaCompraSemConfundirIdsOuAlterarPayloadOriginal() throws Exception {
        var pedido=pedido();
        var salvo=mapper.mapearCidades(pedido,compra(),catalogo());
        assertEquals(3692,salvo.getTrechos().get(0).getTrecho().getCodgCidadeOrigem());
        assertEquals(7233,salvo.getTrechos().get(0).getTrecho().getCodgCidadeDestino());
        assertEquals(319,pedido.getTrechos().get(0).getTrecho().getCodgCidadeOrigem());
        assertEquals(99,pedido.getTrechos().get(0).getTrecho().getCodgCidadeDestino());
        assertEquals(pedido.getChaveIdempotencia(),salvo.getChaveIdempotencia());
    }
    @Test void terminalDeOutraCidadeNaoPodeSerUsadoMesmoComCodigoExternoCorreto() throws Exception {
        var destinos=new ArrayList<>(catalogo());destinos.removeIf(d->d.codgVinculoCidadeSistema()==7233);
        destinos.add(new RodoviarioDestinoDTO(7233,100,"ROD_806"));
        var erro=assertThrows(RodoviarioIntegracaoException.class,()->mapper.mapearCidades(pedido(),compra(),destinos));
        assertEquals("CONFERENCIA_NECESSARIA",erro.getResposta().path("codigo").asText());
    }
    @Test void duplicidadeDeVinculosParaMesmoTerminalExigeConferencia() throws Exception {
        var destinos=new ArrayList<>(catalogo());destinos.add(new RodoviarioDestinoDTO(8000,319,"ROD_1"));
        var erro=assertThrows(RodoviarioIntegracaoException.class,()->mapper.mapearCidades(pedido(),compra(),destinos));
        assertTrue(erro.getDiagnosticoLocal().contains("Multiplos vinculos"));
    }
    @Test void associaIdaEVoltaPelasCidadesETerminaisMesmoComRetornoInvertido() throws Exception {
        var pedido=pedido();
        var volta=json.convertValue(pedido.getTrechos().get(0),SalvarReservaRodoviarioRequest.TrechoCompleto.class);
        volta.getTrecho().setCodgCidadeOrigem(99);volta.getTrecho().setCodgCidadeDestino(319);
        pedido.getTrechos().add(volta);
        var compra=compra();ObjectNode ida=compra.withArray("trechos").get(0).deepCopy();ObjectNode retorno=ida.deepCopy();
        ((ObjectNode)retorno.get("origem")).put("codigoFornecedor","ROD_806");
        ((ObjectNode)retorno.get("destino")).put("codigoFornecedor","ROD_1");
        retorno.put("partida","2030-01-20T09:00:00");compra.set("trechos",json.createArrayNode().add(retorno).add(ida));
        var salvo=mapper.mapearCidades(pedido,compra,catalogo());
        assertEquals(3692,salvo.getTrechos().get(0).getTrecho().getCodgCidadeOrigem());
        assertEquals(7233,salvo.getTrechos().get(1).getTrecho().getCodgCidadeOrigem());
        assertEquals("2030-01-20",salvo.getTrechos().get(1).getTrecho().getDataPartida().toString());
    }
    @Test void rejeitaCidadeZeroAntesDeOperarFornecedor() throws Exception {
        var pedido=pedido();pedido.getTrechos().get(0).getTrecho().setCodgCidadeOrigem(0);
        assertEquals(400,assertThrows(RodoviarioIntegracaoException.class,()->mapper.validar(pedido)).getStatus());
    }
}
