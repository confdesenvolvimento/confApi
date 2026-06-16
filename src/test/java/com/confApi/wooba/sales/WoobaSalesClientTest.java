package com.confApi.wooba.sales;

import com.confApi.wooba.sales.dto.WoobaSalesDetailsResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WoobaSalesClientTest {

    @Test
    void deveEnviarDeveloperTokenEAccessCodeGeradoNoDetails() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);

        WoobaSalesProperties properties = new WoobaSalesProperties();
        properties.setBaseUrl("http://localhost/wooba/");
        properties.setIdentifier("LOGIN");
        properties.setPassword("SENHA");
        properties.setDeveloperToken("8c9e1803-c944-4d60-89c3-ac0f00f3e489");

        WoobaSalesClient client = new WoobaSalesClient(
                restTemplate,
                properties,
                new WoobaDeveloperAccessCodeGenerator()
        );

        server.expect(requestTo("http://localhost/wooba/details"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    assertEquals("8c9e1803-c944-4d60-89c3-ac0f00f3e489",
                            request.getHeaders().getFirst("developer-token"));
                    String accessCode = request.getHeaders().getFirst("developer-access-code");
                    assertNotNull(accessCode);
                    assertEquals(128, Base64.getDecoder().decode(accessCode).length);
                })
                .andRespond(withSuccess("""
                        {
                          "Success": true,
                          "Transaction": {
                            "Header": {
                              "Locator": "ABC123"
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        WoobaSalesDetailsResponse response = client.details("AIR-123");

        assertNotNull(response.getTransaction());
        server.verify();
    }
}
