package com.confApi.chatconfianca.operacional;

import com.confApi.chatconfianca.client.ChatConfiancaTokenProvider;
import com.confApi.config.UrlConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Closed read-only endpoint, no personal URL fields, redirects, retries, cache or body logging. */
@Component
public class ChatResumoOperacionalHotelClient {
    private final ChatConfiancaTokenProvider auth;
    private final ObjectMapper mapper;
    private final Supplier<String> baseUrl;
    private final OkHttpClient transport;

    @Autowired public ChatResumoOperacionalHotelClient(ChatConfiancaTokenProvider auth, ObjectMapper mapper) {
        this(auth, mapper, () -> UrlConfig.URL_CONFIANCA_MANAGER, new OkHttpClient());
    }
    ChatResumoOperacionalHotelClient(ChatConfiancaTokenProvider auth, ObjectMapper mapper,
            Supplier<String> baseUrl, OkHttpClient transport) {
        this.auth = auth; this.mapper = mapper; this.baseUrl = baseUrl; this.transport = transport;
    }

    public ChatResumoOperacionalHotelDocumento consultar(ChatResumoOperacionalHotelDocumento.Consulta consulta) throws IOException {
        String base = baseUrl.get(), token = auth.bearerToken();
        if (base == null || base.isBlank() || token == null || token.isBlank()) throw new IOException("OPERACIONAL_HOTEL_CONEXAO_AUSENTE");
        var request = new Request.Builder().url(base.replaceAll("/+$", "") + "/chat-confianca/operacional/hotel")
                .header("Authorization", "Bearer " + token).header("Accept", "application/json").header("Cache-Control", "no-store")
                .post(RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsBytes(consulta))).build();
        var http = transport.newBuilder().connectTimeout(2, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS)
                .callTimeout(5, TimeUnit.SECONDS).retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).cache(null).build();
        try (Response response = http.newCall(request).execute()) {
            if (response.code() != 200 || response.body() == null) throw new ConsultaException(response.code());
            byte[] bytes = response.body().byteStream().readNBytes(65537);
            if (bytes.length > 65536) throw new IOException("OPERACIONAL_HOTEL_RESPOSTA_EXCESSIVA");
            return mapper.readValue(bytes, ChatResumoOperacionalHotelDocumento.class);
        }
    }

    public static final class ConsultaException extends IOException {
        private final int status;
        ConsultaException(int status) { super("OPERACIONAL_HOTEL_HTTP_" + status); this.status = status; }
        public int status() { return status; }
    }
}
