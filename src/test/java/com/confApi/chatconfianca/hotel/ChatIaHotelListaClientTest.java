package com.confApi.chatconfianca.hotel;
import com.confApi.chatconfianca.client.ChatConfiancaTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.*;
import okio.Buffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ChatIaHotelListaClientTest {
    AtomicReference<Request> sent=new AtomicReference<>();
    ChatIaHotelListaClient client(int code,String body){var auth=mock(ChatConfiancaTokenProvider.class);when(auth.bearerToken()).thenReturn("teste");var http=new OkHttpClient.Builder().addInterceptor(chain->{sent.set(chain.request());return new Response.Builder().request(chain.request()).code(code).message("test").protocol(Protocol.HTTP_1_1).header("Location","https://outro.invalid").body(ResponseBody.create(body,MediaType.get("application/json"))).build();}).build();return new ChatIaHotelListaClient(auth,new ObjectMapper(),()->"https://manager.invalid",http);}
    @Test void filtrosECursorNoCorpoNuncaNaUrl()throws Exception{var q=new ChatIaHotelListaDocumento.Consulta();q.setAgencia(10);q.setHospede("Pessoa Ficticia");q.setAposId(71);q.setAposData("2026-09-10 00:00:00");client(200,"{}").consultar(q,8);var r=sent.get();assertEquals("POST",r.method());assertEquals("/chatIa/runtime/hotel-lista/consulta",r.url().encodedPath());assertNull(r.url().query());assertEquals("no-store",r.header("Cache-Control"));assertEquals("Bearer teste",r.header("Authorization"));var b=new Buffer();r.body().writeTo(b);var json=new ObjectMapper().readTree(b.readUtf8());assertEquals(10,json.path("agencia").asInt());assertEquals(71,json.path("aposId").asInt());}
    @Test void configuracaoGetE204()throws Exception{assertNull(client(204,"").configuracao());assertEquals("GET",sent.get().method());}
    @Test void falhasRedirectRespostasGrandesNaoSaoListas(){assertThrows(Exception.class,()->client(403,"{}").configuracao());assertThrows(Exception.class,()->client(302,"{}").configuracao());assertThrows(Exception.class,()->client(200," ".repeat(262145)).configuracao());}
}
