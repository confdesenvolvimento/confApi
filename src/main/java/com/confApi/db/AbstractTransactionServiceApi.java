package com.confApi.db;

import com.confApi.confApp.ConfAppService;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class AbstractTransactionServiceApi {
    public String sendHttpApiPost(String requestData, String url) {
        String responseBody = null;
        try {
            HttpClient httpClient = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + new ConfAppService().token())
                    .POST(HttpRequest.BodyPublishers.ofString(requestData, StandardCharsets.UTF_8))
                    .build();
            System.err.println("Bearer: "+new ConfAppService().token());
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            responseBody = response.body();

        } catch (URISyntaxException | IOException | InterruptedException ex) {
            Logger.getLogger(AbstractTransactionServiceApi.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Não foi possivel concluir a solicitação, tente novamente.");

        }
        return responseBody;
    }




    public int sendHttpApiPostPublicNew(String requestData, String url) {
        int statusCode = 0;
        try {
            HttpClient httpClient = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestData, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            statusCode = response.statusCode(); // Retorna o código de status

        } catch (URISyntaxException | IOException | InterruptedException ex) {
            Logger.getLogger(AbstractTransactionServiceApi.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Não foi possivel concluir a solicitação, tente novamente.");
        }
        return statusCode;
    }
    public String sendHttpApiPostPublic(String requestData, String url) {


        String responseBody = null;
        try {
            HttpClient httpClient = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .header("Content-Type", "application/json")
                    /*.header("Authorization", "Bearer " + Util.getUsuarioLogado().getChaveToken())*/
                    .POST(HttpRequest.BodyPublishers.ofString(requestData, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            responseBody = response.body();

        } catch (URISyntaxException | IOException | InterruptedException ex) {
            Logger.getLogger(AbstractTransactionServiceApi.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Não foi possivel concluir a solicitação, tente novamente.");
        }


        return responseBody;
    }

    public String sendHttpApiPut(String requestData, String url) {
        String responseBody = null;
        try {
            HttpClient httpClient = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + new ConfAppService().token())
                    .PUT(HttpRequest.BodyPublishers.ofString(requestData, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            responseBody = response.body();

        } catch (URISyntaxException | IOException | InterruptedException ex) {
            Logger.getLogger(AbstractTransactionServiceApi.class.getName()).log(Level.SEVERE, null, ex);
        }
        return responseBody;
    }

    public String sendHttpApiPatch(String requestData, String url) {
        String responseBody = null;
        try {
            HttpClient httpClient = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + new ConfAppService().token())
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(requestData, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            responseBody = response.body();

        } catch (URISyntaxException | IOException | InterruptedException ex) {
            Logger.getLogger(AbstractTransactionServiceApi.class.getName()).log(Level.SEVERE, null, ex);
        }
        return responseBody;
    }

    public String sendHttpApiPutPublic(String requestData, String url) {
        String responseBody = null;
        try {
            HttpClient httpClient = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .header("Content-Type", "application/json")
                    // .header("Authorization", "Bearer " + Util.getUsuarioLogado().getChaveToken())
                    .PUT(HttpRequest.BodyPublishers.ofString(requestData, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            responseBody = response.body();

        } catch (URISyntaxException | IOException | InterruptedException ex) {
            Logger.getLogger(AbstractTransactionServiceApi.class.getName()).log(Level.SEVERE, null, ex);
        }
        return responseBody;
    }

    public String sendHttpApiGetPublic(String url) {
        String responseBody = null;
        try {
            HttpClient httpClient = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .header("Content-Type", "application/json")
                    //.header("Authorization", "Bearer " + Util.getUsuarioLogado().getChaveToken())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            responseBody = response.body();

        } catch (URISyntaxException | IOException | InterruptedException ex) {
            Logger.getLogger(AbstractTransactionServiceApi.class.getName()).log(Level.SEVERE, null, ex);
        }

        return responseBody;
    }

    public String sendHttpApiGet(String url) {
        String responseBody = null;
        //UtilDebug.sysError("BEARER: "+Util.checkUserSystemToken());
        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + new ConfAppService().token())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            responseBody = response.body();

        } catch (URISyntaxException | IOException | InterruptedException ex) {
            Logger.getLogger(AbstractTransactionServiceApi.class.getName()).log(Level.SEVERE, null, ex);
        }

        return responseBody;
    }

    public String sendHttpApiDelete(String url) {
        String responseBody = null;
        try {
            HttpClient httpClient = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + new ConfAppService().token())
                    .DELETE() // Changed to DELETE
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            responseBody = response.body();

        } catch (URISyntaxException | IOException | InterruptedException ex) {
            Logger.getLogger(AbstractTransactionServiceApi.class.getName()).log(Level.SEVERE, null, ex);
        }

        return responseBody;
    }
}
