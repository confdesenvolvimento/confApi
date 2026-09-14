package com.confApi.chatconfianca.hotel;
import com.confApi.chatconfianca.client.ChatConfiancaTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.*;
import okio.Buffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatIaHotelReservaClientTest {
    AtomicReference<Request> sent=new AtomicReference<>();
    ChatIaHotelReservaClient client(int code,String body){var auth=mock(ChatConfiancaTokenProvider.class);when(auth.bearerToken()).thenReturn("teste");var http=new OkHttpClient.Builder().addInterceptor(chain->{sent.set(chain.request());return new Response.Builder().request(chain.request()).code(code).message("test").protocol(Protocol.HTTP_1_1).header("Location","https://outro.invalid").body(ResponseBody.create(body,MediaType.get("application/json"))).build();}).build();return new ChatIaHotelReservaClient(auth,new ObjectMapper(),()->"https://manager.invalid/",http);}
    @Test void leituraPrivadaUsaPostSemDadosPessoaisNaUrl()throws Exception{var q=new ChatIaHotelReservaDocumento.Consulta();q.setAgencia(10);q.setHospede("Pessoa Teste");client(200,"{}").consultar(q,8);var r=sent.get();assertEquals("POST",r.method());assertEquals("/chatIa/runtime/hotel-reserva/consulta",r.url().encodedPath());assertNull(r.url().query());assertEquals("Bearer teste",r.header("Authorization"));assertEquals("no-store",r.header("Cache-Control"));var b=new Buffer();r.body().writeTo(b);var json=new ObjectMapper().readTree(b.readUtf8());assertEquals(10,json.path("agencia").asInt());assertEquals("Pessoa Teste",json.path("hospede").asText());}
    @Test void configuracaoUsaGetE204()throws Exception{assertNull(client(204,"").configuracao());assertEquals("GET",sent.get().method());}
    @Test void falhaHttpRedirectOuRespostaEnormeFalhaFechado(){assertThrows(Exception.class,()->client(403,"{}").configuracao());assertThrows(Exception.class,()->client(302,"{}").configuracao());assertThrows(Exception.class,()->client(200," ".repeat(262145)).configuracao());}
}
