package com.confApi.mobile;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.springframework.stereotype.Component;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.io.*;

@Component
public class MobileUpstream {
    static final ObjectMapper JSON=new ObjectMapper();
    final MobileSettings settings;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build();
    private String technicalToken;
    private Instant tokenUntil=Instant.EPOCH;
    private Map<String,String> destinations=Map.of();
    private Instant configUntil=Instant.EPOCH;
    private JsonNode propertyRows;
    private Instant propertiesUntil=Instant.EPOCH;
    MobileUpstream(MobileSettings settings) { this.settings=settings; }
    protected synchronized String technicalToken() {
        if(technicalToken!=null&&tokenUntil.isAfter(Instant.now()))return technicalToken;
        var result=request(settings.initial("hub")+"user/auth","POST",JSON.valueToTree(Map.of("login",settings.secret("technicalLogin"),"senha",settings.secret("technicalPassword"))),null);
        String token=result.path("token").asText("");
        if(token.isBlank())throw new MobileFailure(503,"INTEGRACAO_INDISPONIVEL");
        technicalToken=token;tokenUntil=Instant.now().plusSeconds(60);return token;
    }
    protected JsonNode request(String url,String method,JsonNode body,String authorization) {
        try {
            var builder=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(40)).header("Accept","application/json").header("Content-Type","application/json");
            if(authorization!=null)builder.header("Authorization","Bearer "+authorization);
            builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body.toString()));
            var response=http.send(builder.build(),HttpResponse.BodyHandlers.ofInputStream());
            byte[] bytes;try(var stream=response.body()){bytes=stream.readNBytes(2_000_001);}
            if(bytes.length>2_000_000)throw new MobileFailure(502,"RESPOSTA_INVALIDA");
            if(response.statusCode()<200||response.statusCode()>=300)throw new MobileFailure(response.statusCode()==401?401:502,response.statusCode()==401?"ACESSO_INVALIDO":"INTEGRACAO_INDISPONIVEL");
            if(bytes.length==0)return JSON.createObjectNode();
            try{return JSON.readTree(bytes);}catch(com.fasterxml.jackson.core.JsonProcessingException notJson){return TextNode.valueOf(new String(bytes,java.nio.charset.StandardCharsets.UTF_8));}
        } catch(MobileFailure failure) { throw failure; }
        catch(InterruptedException failure) { Thread.currentThread().interrupt();throw new MobileFailure(503,"INTEGRACAO_INDISPONIVEL"); }
        catch(Exception failure) { throw new MobileFailure(503,"INTEGRACAO_INDISPONIVEL"); }
    }
    JsonNode call(String service,String path,String method,JsonNode body) { return request(destination(service)+path,method,body,technicalToken()); }
    private synchronized JsonNode properties() {
        if(propertyRows==null||!propertiesUntil.isAfter(Instant.now())) {
            JsonNode rows=request(settings.initial("manager")+"propertiesapp","GET",null,technicalToken());
            if(!rows.isArray())throw new MobileFailure(502,"CONFIGURACAO_INVALIDA");
            propertyRows=rows;propertiesUntil=Instant.now().plusSeconds(60);
        }
        return propertyRows;
    }
    synchronized String destination(String service) {
        if(settings.development())return settings.initial(service);
        if(!configUntil.isAfter(Instant.now())) {
            var names=Map.of("manager","baseUrl","hub","baseUrlHub","cache","urlCache","confapi","urlApi","cservice","urlCservice","email","urlEmailBase","orcamento","urlOrcamentoClienteBase");
            var found=new HashMap<String,String>();
            JsonNode rows=properties();
            for(var entry:names.entrySet())for(JsonNode row:rows) {
                if(row.path("status").asInt()==1&&(entry.getValue().equalsIgnoreCase(row.path("nome").asText())||("app"+entry.getValue()).equalsIgnoreCase(row.path("nome").asText()))) {
                    String value=row.path("descricao").asText("");
                    if(!settings.allowedProductionOrigin(value))throw new MobileFailure(503,"DESTINO_NAO_AUTORIZADO");
                    found.put(entry.getKey(),settings.validate(value));
                }
            }
            destinations=Map.copyOf(found);configUntil=Instant.now().plusSeconds(60);
        }
        String value=destinations.get(service);
        if(value==null)throw new MobileFailure(503,"DESTINO_NAO_CONFIGURADO");
        return value;
    }
    JsonNode publicProperties() {
        Set<String> allowed=Set.of("versao","chatia","emitir","hotel","seguro","novidade","ofertas","codigovalidacaoemail","codigovalidacaosms","codigovalidacaowhatsapp","smshabilitado","quantidademeses","orcamentoaereo","apporcamentoaereo","orcamentohotel","apporcamentohotel","orcamentocarro","apporcamentocarro","orcamentoseguro","apporcamentoseguro","orcamentocomposto","apporcamentocomposto","oecamentocomposto","habilitarleituracartao","apphabilitarleituracartao","remarcacao","appremarcacao");
        ArrayNode result=JSON.createArrayNode();
        for(JsonNode row:properties()) {
            String name=row.path("nome").asText("").trim();
            if(name.matches("(?i)(?:app)?urlOrcamentoClienteBase")&&row.path("status").asInt()==1) {
                var safe=result.addObject();safe.put("nome",name);safe.put("status",1);safe.put("descricao",settings.validate(row.path("descricao").asText()));
            }
            if(allowed.contains(name.toLowerCase(Locale.ROOT))||name.matches("(?i)(aereo|hotel|carro)(faturado|prazoespecial|cartao|linkpagamento)")) {
                var safe=result.addObject();safe.put("nome",name);safe.put("status",row.path("status").asInt());String description=row.path("descricao").asText("").trim();safe.put("descricao",description.matches("(?i)true|false|sim|n[aã]o|yes|no|on|off|ativo|inativo|[0-9]{1,4}")?description:"");
            }
        }
        return result;
    }
    boolean channelEnabled(String channel) {
        String key="whatsapp".equals(channel)?"codigovalidacaowhatsapp":"smshabilitado";
        for(JsonNode row:publicProperties())if(key.equalsIgnoreCase(row.path("nome").asText()))return row.path("status").asInt()==1;
        return false;
    }
    private String emailDestination() {
        if(settings.development()) {
            String value=settings.secret("emailSendUrl");settings.validate(value);return value;
        }
        for(JsonNode row:properties()) {
            if(row.path("status").asInt()==1&&row.path("nome").asText().matches("(?i)(?:app)?urlEmail")) {
                String value=row.path("descricao").asText();
                if(!settings.allowedProductionOrigin(value))throw new MobileFailure(503,"DESTINO_NAO_AUTORIZADO");
                return value;
            }
        }
        throw new MobileFailure(503,"DESTINO_NAO_CONFIGURADO");
    }
    void deliver(JsonNode profile,String channel,String code) {
        if("email".equals(channel)) {
            String email=profile.path("email").asText("");
            if(email.isBlank()||email.length()>254||email.contains("\r")||email.contains("\n"))throw new MobileFailure(409,"CONTATO_INDISPONIVEL");
            request(emailDestination(),"POST",JSON.valueToTree(Map.of("emails",email,"codigo",code)),technicalToken());return;
        }
        if(!Set.of("sms","whatsapp").contains(channel)||!channelEnabled(channel))throw new MobileFailure(409,"CANAL_INDISPONIVEL");
        String phone=profile.path("celular").asText("").replaceAll("\\s+","");
        if(!phone.matches("\\+?[0-9]{10,15}"))throw new MobileFailure(409,"CONTATO_INDISPONIVEL");
        call("hub","menssage"+("whatsapp".equals(channel)?"/whatsapp":""),"POST",JSON.valueToTree(Map.of("to",phone,"contents",List.of(Map.of("text","CONFIANÇA CONSOLIDADORA: Use o código "+code+" para validar seu celular no ConfApp")))));
    }
    void deliverLoginCode(JsonNode profile,String code) {
        // Consulta sem candidato: apenas lê a preferência existente, sem ativar ou cadastrar aparelho.
        JsonNode device=call("manager","dispositivo/login/"+profile.path("login").asText(),"GET",null);
        boolean email=device.path("enviarEmail").asInt()==1;
        boolean delivered=false;MobileFailure failure=null;
        try { deliver(profile,"sms",code);delivered=true; } catch(MobileFailure error){failure=error;}
        if(email)try { deliver(profile,"email",code);delivered=true; } catch(MobileFailure error){failure=error;}
        if(!delivered)throw failure;
    }
    record Response(int status,String contentType,InputStream body,Map<String,String> headers) {
        Response(int status,String contentType,InputStream body){this(status,contentType,body,Map.of());}
    }
    static Map<String,String> publicResponseHeaders(java.net.http.HttpHeaders headers) {
        var result=new HashMap<String,String>();
        for(String name:List.of("Contagem","Content-Length"))headers.firstValue(name).map(String::trim)
            .filter(value->value.matches("[0-9]{1,18}")).ifPresent(value->result.put(name,value));
        for(String name:List.of("X-Transcript","X-Client-Message-Id"))headers.firstValue(name)
            .filter(value->value.length()<=8192&&value.chars().noneMatch(Character::isISOControl))
            .ifPresent(value->result.put(name,value));
        return Map.copyOf(result);
    }
    JsonNode publicError(int status,String contentType,byte[] bytes) {
        var result=JSON.createObjectNode();result.put("code","OPERACAO_"+status);
        if(status==502||bytes.length>65_536)return result;
        try {
            if(contentType.toLowerCase(Locale.ROOT).contains("json")) {
                JsonNode original=JSON.readTree(bytes);
                // Preserva só os campos públicos usados pelo app, nunca dumps de exceções/requisições.
                for(String field:List.of("mensagem","message","detail","error","timestamp")) {
                    JsonNode value=original.path(field);
                    if(value.isTextual()){String safe=publicMessage(value.asText());if(!safe.isBlank())result.put(field,safe);}
                }
            } else if(contentType.toLowerCase(Locale.ROOT).startsWith("text/plain")) {
                String safe=publicMessage(new String(bytes,java.nio.charset.StandardCharsets.UTF_8));
                if(!safe.isBlank())result.put("message",safe);
            }
        } catch(Exception ignored) { /* Mensagem genérica para conteúdo inválido. */ }
        return result;
    }
    private String publicMessage(String value) {
        if(value.length()>4000||value.indexOf('\0')>=0)return "";
        if(technicalToken!=null&&value.contains(technicalToken))return "";
        if(settings!=null)for(String key:List.of("technicalLogin","technicalPassword","cserviceLogin","cservicePassword")) {
            try{if(value.contains(settings.secret(key)))return "";}catch(MobileFailure absent){/* Sem configuração desse segredo. */}
        }
        if(value.matches("(?is).*\\b(?:Bearer|Basic)\\s+[A-Za-z0-9_+/=.-]+.*")
            ||value.matches("(?s).*eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+.*"))return "";
        return value.trim();
    }
    Response forward(String service,String path,String query,String method,byte[] body,String contentType) {
        return forward(service,path,query,method,body,contentType,null);
    }
    Response forwardWithIdentity(String path,String query,String method,byte[] body,String contentType,String delegation) {
        if (!path.startsWith("v1/chat-confianca/")) throw new MobileFailure(404,"ROTA_INDISPONIVEL");
        return forward("confapi",path,query,method,body,contentType,delegation);
    }
    private Response forward(String service,String path,String query,String method,byte[] body,String contentType,String delegation) {
        try {
            String uri=destination(service)+path+(query==null?"":"?"+query);
            var builder=HttpRequest.newBuilder(URI.create(uri)).timeout(Duration.ofSeconds(120)).header("Authorization","Bearer "+technicalToken()).header("Accept","*/*");
            if(contentType!=null)builder.header("Content-Type",contentType);
            if(delegation!=null)builder.header(MobileChatDelegations.HEADER,delegation);
            var response=http.send(builder.method(method,body.length==0?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofByteArray(body)).build(),HttpResponse.BodyHandlers.ofInputStream());
            if(response.statusCode()>=300&&response.statusCode()<400){response.body().close();throw new MobileFailure(502,"REDIRECIONAMENTO_NAO_PERMITIDO");}
            return new Response(response.statusCode(),response.headers().firstValue("Content-Type").orElse("application/octet-stream"),response.body(),publicResponseHeaders(response.headers()));
        } catch(MobileFailure failure){throw failure;}catch(InterruptedException failure){Thread.currentThread().interrupt();throw new MobileFailure(503,"INTEGRACAO_INDISPONIVEL");}catch(Exception failure){throw new MobileFailure(503,"INTEGRACAO_INDISPONIVEL");}
    }
    static JsonNode sanitized(JsonNode value) {
        JsonNode copy=value.deepCopy();scrub(copy);return copy;
    }
    private static void scrub(JsonNode value) {
        if(value.isObject()) {
            ((ObjectNode)value).remove(List.of("senha","password","chaveToken","chaveUsuario","usuarioSenha","technicalPassword","technicalLogin","refreshToken"));
            value.elements().forEachRemaining(MobileUpstream::scrub);
        } else if(value.isArray())value.elements().forEachRemaining(MobileUpstream::scrub);
    }
}
