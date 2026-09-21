package com.confApi.chatconfianca.hotel;

import com.confApi.chatconfianca.client.ChatConfiancaTokenProvider;
import com.confApi.config.UrlConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import org.springframework.stereotype.Component;

/** No response cache, redirects, supplier calls, personal URL parameters or body logging. */
@Component
public class ChatIaHotelReservaClient {
    private final ChatConfiancaTokenProvider auth;
    private final ObjectMapper mapper;
    private final java.util.function.Supplier<String> baseUrl;
    private final OkHttpClient transport;
    @org.springframework.beans.factory.annotation.Autowired
    public ChatIaHotelReservaClient(ChatConfiancaTokenProvider auth,ObjectMapper mapper) {
        this(auth,mapper,()->UrlConfig.URL_CONFIANCA_MANAGER,new OkHttpClient());
    }
    ChatIaHotelReservaClient(ChatConfiancaTokenProvider auth,ObjectMapper mapper,java.util.function.Supplier<String> baseUrl,OkHttpClient transport) {
        this.auth=auth;this.mapper=mapper;this.baseUrl=baseUrl;this.transport=transport;
    }
    public ChatIaHotelDocumento.Configuracao configuracao() throws IOException {
        return request(null,4,ChatIaHotelDocumento.Configuracao.class);
    }
    public ChatIaHotelReservaDocumento consultar(ChatIaHotelReservaDocumento.Consulta consulta,int timeout) throws IOException {
        return request(consulta,timeout,ChatIaHotelReservaDocumento.class);
    }
    private <T> T request(ChatIaHotelReservaDocumento.Consulta q,int timeout,Class<T> type) throws IOException {
        String base=baseUrl.get(),token=auth.bearerToken();
        if(base==null||base.isBlank()||token==null||token.isBlank())throw new IOException("HOTEL_RESERVA_CONEXAO_AUSENTE");
        Request.Builder r=new Request.Builder().url(base.replaceAll("/+$","")+"/chatIa/runtime/hotel-reserva/"+(q==null?"configuracao":"consulta"))
                .header("Authorization","Bearer "+token).header("Accept","application/json").header("Cache-Control","no-store");
        if(q!=null)r.post(RequestBody.create(MediaType.parse("application/json"),mapper.writeValueAsBytes(q)));
        int seconds=Math.max(1,Math.min(8,timeout));
        var http=transport.newBuilder().connectTimeout(Math.min(2,seconds),TimeUnit.SECONDS).readTimeout(seconds,TimeUnit.SECONDS)
                .callTimeout(seconds,TimeUnit.SECONDS).retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).cache(null).build();
        try(Response response=http.newCall(r.build()).execute()) {
            if(response.code()==204&&q==null)return null;
            if(response.code()!=200||response.body()==null)throw new IOException("HOTEL_RESERVA_HTTP_"+response.code());
            byte[] bytes=response.body().byteStream().readNBytes(262145);
            if(bytes.length>262144)throw new IOException("HOTEL_RESERVA_RESPOSTA_EXCESSIVA");
            return mapper.readValue(bytes,type);
        }
    }
}
