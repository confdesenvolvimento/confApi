package com.confApi.mobile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

/** Segredos lidos somente pelo servidor, fora do projeto e dos artefatos mobile. */
@Component
public class MobileSettings {
    private final JsonNode config;
    public MobileSettings() {
        this(read());
    }
    MobileSettings(JsonNode config) { this.config=config; }
    private static JsonNode read() {
        String configured=System.getProperty("confianca.mobile.config",System.getenv("CONFIANCA_MOBILE_CONFIG"));
        return readConfiguration(configured,Path.of(System.getProperty("user.home"),".confianca","confapi-mobile.json"),
                MobileSettings.class.getResourceAsStream("/confapi-mobile.json"));
    }
    static JsonNode readConfiguration(String configured,Path legacyDefault,InputStream bundled) {
        try (InputStream resource=bundled) {
            ObjectMapper json=new ObjectMapper();
            if(configured!=null) {
                Path file=Path.of(configured);
                return Files.isRegularFile(file)?json.readTree(Files.readAllBytes(file)):json.createObjectNode();
            }
            if(resource!=null)return json.readTree(resource);
            return Files.isRegularFile(legacyDefault)?json.readTree(Files.readAllBytes(legacyDefault)):json.createObjectNode();
        }
        catch(Exception failure) { throw new IllegalStateException("Configuração privada do mobile inválida"); }
    }
    boolean enabled() { return config.path("enabled").asBoolean(false); }
    boolean development() { return config.path("development").asBoolean(false); }
    String secret(String key) { String value=config.path(key).asText(""); if(value.isBlank())throw new MobileFailure(503,"CONFIGURACAO_INDISPONIVEL");return value; }
    String initial(String service) { return validate(config.path("services").path(service).asText("")); }
    String productionDestination(String service) {
        String value=config.path("productionServices").path(service).asText("");
        if(value.isBlank())return null;
        if(!allowedProductionOrigin(value))throw new MobileFailure(503,"DESTINO_NAO_AUTORIZADO");
        return validate(value);
    }
    String validate(String value) {
        try {
            URI uri=URI.create(value);
            if(uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null)throw new IllegalArgumentException();
            boolean local=development()&&"http".equals(uri.getScheme())&&local(uri.getHost());
            if(!local&&!"https".equals(uri.getScheme()))throw new IllegalArgumentException();
            return value.replaceAll("/+$","")+"/";
        } catch(Exception failure) { throw new MobileFailure(503,"DESTINO_INDISPONIVEL"); }
    }
    boolean allowedProductionOrigin(String value) {
        String origin=URI.create(validate(value)).getAuthority();
        for(JsonNode allowed:config.path("allowedProductionHosts"))if(allowed.asText().equalsIgnoreCase(origin))return true;
        return false;
    }
    static boolean local(String host) {
        if(host==null)return false;
        if(Set.of("localhost","::1","[::1]","0:0:0:0:0:0:0:1").contains(host))return true;
        if(!host.matches("\\d{1,3}(?:\\.\\d{1,3}){3}"))return false;
        int[] n=Arrays.stream(host.split("\\.")).mapToInt(Integer::parseInt).toArray();
        return Arrays.stream(n).allMatch(x->x<=255)&&(n[0]==127||n[0]==10||n[0]==192&&n[1]==168||n[0]==172&&n[1]>=16&&n[1]<=31);
    }
}
