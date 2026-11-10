package com.confApi.hoteis;

import com.confApi.confApp.*;
import com.confApi.db.confManager.hotel.model.ReservaHotelModel;
import com.confApi.db.confManager.reservaHotel.dto.ReservaHotel;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.endPoints.usuario.UsuarioApi;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class HotelV2PersistenceTest {
    HotelServiceV2 service; MockRestServiceServer server; ReservaHotelModel pedido; String managerAnterior;
    @BeforeEach void setup() {
        managerAnterior = com.confApi.config.UrlConfig.URL_CONFIANCA_MANAGER;
        com.confApi.config.UrlConfig.URL_CONFIANCA_MANAGER = "http://manager.invalid/";
        RestTemplate http = new RestTemplate(); server = MockRestServiceServer.bindTo(http).build();
        service = new HotelServiceV2(http);
        ConfAppService auth = mock(ConfAppService.class); ConfAppResp token = new ConfAppResp(); token.setToken("local-test");
        when(auth.token()).thenReturn(token); ReflectionTestUtils.setField(service, "confAppService", auth);
        UsuarioApi usuarios = mock(UsuarioApi.class); Usuario usuario = new Usuario(); usuario.setCodgUsuario(8);
        when(usuarios.consultaUsuarioByLogin("teste")).thenReturn(usuario); ReflectionTestUtils.setField(service, "usuarioApi", usuarios);
        pedido = new ReservaHotelModel(); pedido.setCodgReservaHotelDB(42L); pedido.setCodgHotel("999");
        pedido.setLocalizador("HOT42"); pedido.setUsuarioCancelamento("teste");
    }
    @AfterEach void verify() {
        com.confApi.config.UrlConfig.URL_CONFIANCA_MANAGER = managerAnterior;
        server.verify();
    }
    void baixa() {
        server.expect(r -> assertTrue(r.getURI().getPath().endsWith("/reservaHotel/cancelar/42")))
                .andExpect(method(HttpMethod.PUT)).andExpect(header("Authorization", "Bearer local-test"))
                .andExpect(jsonPath("$.codgReservaHotel").value(42)).andRespond(withNoContent());
    }
    void consulta(int status, String localizador) {
        server.expect(r -> { assertTrue(r.getURI().getPath().endsWith("/reservaHotel/42")); assertFalse(r.getURI().getPath().contains("//")); })
                .andExpect(method(HttpMethod.GET)).andRespond(withSuccess("{\"codgReservaHotel\":42,\"localizador\":\""+localizador+"\",\"status\":"+status+"}",MediaType.APPLICATION_JSON));
    }
    @Test void baixaUsaCodigoDaReservaEConfereStatusPersistido() { baixa(); consulta(2,"HOT42"); service.cancelarReservaDB(pedido); }
    @Test void statusAntigoNaoConfirmaBaixa() { baixa(); consulta(1,"HOT42"); assertThrows(IllegalStateException.class,()->service.cancelarReservaDB(pedido)); }
    @Test void localizadorDiferenteNaoConfirmaCadastro() { consulta(2,"OUTRO"); assertThrows(IllegalStateException.class,()->service.consultarReservaDB(pedido)); }
    @Test void respostaVaziaNaoConfirmaCriacao() {
        server.expect(r -> assertTrue(r.getURI().getPath().endsWith("/reservaHotel"))).andExpect(method(HttpMethod.POST)).andRespond(withNoContent());
        ReservaHotel reserva = new ReservaHotel(); reserva.setLocalizador("HOT42");
        assertThrows(IllegalStateException.class,()->service.criarReservaDB(reserva));
    }
}
