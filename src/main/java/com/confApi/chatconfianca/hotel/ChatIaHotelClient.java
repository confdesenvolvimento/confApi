package com.confApi.chatconfianca.hotel;

import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.springframework.http.*;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/** Authenticated GETs only. No hotel supplier calls, booking APIs or shared result cache. */
@Component
public class ChatIaHotelClient {
    private final ConfAppService auth;
    private final java.util.function.Supplier<String> baseUrl;
    private final OkHttpClient transport;
    @org.springframework.beans.factory.annotation.Autowired
    public ChatIaHotelClient(ConfAppService auth) {
        this(auth,()->UrlConfig.URL_CONFIANCA_MANAGER,new OkHttpClient.Builder()
                .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).build());
    }
    ChatIaHotelClient(ConfAppService auth,java.util.function.Supplier<String> baseUrl,OkHttpClient transport) {
        this.auth=auth;this.baseUrl=baseUrl;this.transport=transport;
    }
    public ChatIaHotelDocumento.Configuracao configuracao() {
        return get("configuracao",null,null,null,null,4,ChatIaHotelDocumento.Configuracao.class);
    }
    public ChatIaHotelDocumento consultar(String nome,String cidade,String pais,Integer id,int timeout) {
        return get("consulta",nome,cidade,pais,id,timeout,ChatIaHotelDocumento.class);
    }
    private <T> T get(String path,String nome,String cidade,String pais,Integer id,int timeout,Class<T> type) {
        String base=baseUrl.get();
        if(base==null||base.isBlank())throw new IllegalStateException("HOTEL_URL_AUSENTE");
        var uri=UriComponentsBuilder.fromHttpUrl(base.endsWith("/")?base:base+"/").path("chatIa/runtime/hotel-dados/"+path);
        if(nome!=null)uri.queryParam("nome","{nome}").queryParam("cidade","{cidade}");
        java.util.Map<String,Object> params=new java.util.HashMap<>();params.put("nome",nome);params.put("cidade",cidade);
        if(pais!=null){uri.queryParam("pais","{pais}");params.put("pais",pais);}
        if(id!=null)uri.queryParam("hotelId",id);
        var token=auth.token();
        if(token==null||token.getToken()==null||token.getToken().isBlank())throw new IllegalStateException("HOTEL_TOKEN_AUSENTE");
        var headers=new HttpHeaders();headers.setBearerAuth(token.getToken());headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        int seconds=Math.max(1,Math.min(8,timeout));
        var client=transport.newBuilder().connectTimeout(Math.min(2,seconds),TimeUnit.SECONDS)
                .readTimeout(seconds,TimeUnit.SECONDS).callTimeout(seconds,TimeUnit.SECONDS)
                .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).build();
        var http=new RestTemplate(new OkHttp3ClientHttpRequestFactory(client));
        var response=http.exchange(uri.encode().buildAndExpand(params).toUri(),HttpMethod.GET,new HttpEntity<>(headers),type);
        if(response.getStatusCode()==HttpStatus.NO_CONTENT)return null;
        if(response.getStatusCode()!=HttpStatus.OK)throw new IllegalStateException("HOTEL_HTTP_INVALIDO");
        return response.getBody();
    }
}
