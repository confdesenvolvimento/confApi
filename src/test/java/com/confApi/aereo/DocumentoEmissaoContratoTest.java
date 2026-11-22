package com.confApi.aereo;
import com.confApi.aereo.dto.EmitirRequest;
import com.confApi.hub.aereo.ReservaAereoModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DocumentoEmissaoContratoTest {
 @Test void documentosDoAppChegamAoHubComIdentidadeEValidade() throws Exception {
  var mapper=new ObjectMapper();
  var json="{\"documentosPassageiros\":[{\"idPassageiro\":\"7\",\"nome\":\"PASSAGEIRO\",\"sobrenome\":\"TESTE\",\"documento\":{\"tipo\":2,\"numero\":\"AB123456\",\"nacionalidade\":\"BR\",\"paisEmissor\":\"BR\",\"validade\":\"2090-12-31T12:00:00Z\"}}]}";
  var req=new EmitirRequest(mapper.readValue(json,ReservaAereoModel.class));
  var enviado=mapper.valueToTree(req).at("/documentosPassageiros/0"); assertEquals("7",enviado.path("idPassageiro").asText()); assertEquals("PASSAGEIRO",enviado.path("nome").asText()); assertEquals("AB123456",enviado.at("/documento/numero").asText()); assertEquals("2090-12-31T12:00:00Z",enviado.at("/documento/validade").asText()); assertEquals(2,enviado.at("/documento/tipo").asInt());
 }
}
