package com.confApi.chatconfianca.hotel;

import com.confApi.confApp.ConfAppService;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatIaHotelClientTest {
    AtomicReference<Request> sent=new AtomicReference<>();
    ChatIaHotelClient client(int status,String body) {
        var auth=mock(ConfAppService.class,RETURNS_DEEP_STUBS);when(auth.token().getToken()).thenReturn("token-de-teste");
        var http=new OkHttpClient.Builder().addInterceptor(chain->{sent.set(chain.request());return new Response.Builder()
            .request(chain.request()).code(status).message("test").protocol(Protocol.HTTP_1_1).header("Content-Type","application/json")
            .body(ResponseBody.create(body,MediaType.get("application/json"))).build();}).build();
        return new ChatIaHotelClient(auth,()->"https://manager.invalid/",http);
    }
    @Test void consultaEGetAutenticadoComParametrosCodificados() {
        client(200,"{\"contrato\":\"hotel-dados-v1\",\"hoteis\":[]}").consultar("Hotel D'Água & Mar + 50%","São Paulo","Brasil",71,8);
        assertEquals("GET",sent.get().method());assertEquals("Bearer token-de-teste",sent.get().header("Authorization"));
        assertEquals("/chatIa/runtime/hotel-dados/consulta",sent.get().url().encodedPath());
        assertEquals("Hotel D'Água & Mar + 50%",sent.get().url().queryParameter("nome"));
        assertEquals("São Paulo",sent.get().url().queryParameter("cidade"));assertEquals("71",sent.get().url().queryParameter("hotelId"));
        assertNull(sent.get().url().queryParameter("codgAgencia"));assertNull(sent.get().body());
    }
    @Test void configuracaoRequerContratoDoEndpoint(){assertNull(client(204,"").configuracao());assertEquals("/chatIa/runtime/hotel-dados/configuracao",sent.get().url().encodedPath());}
    @Test void falhaHttpNaoViraDadosVazios(){assertThrows(Exception.class,()->client(403,"{}").consultar("Hotel","Recife",null,null,8));}
    @Test void redirectNaoETratadoComoResultado(){assertThrows(Exception.class,()->client(302,"{}").configuracao());}
}
