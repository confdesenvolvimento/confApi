package com.confApi.mobile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class MobileBundledConfigurationTest {
    @TempDir Path directory;
    private ByteArrayInputStream bundled(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
    @Test void bundledConfigurationWorksWithoutExternalFiles() {
        var data=MobileSettings.readConfiguration(null,directory.resolve("absent.json"),bundled("{\"enabled\":true,\"development\":false}"));
        assertTrue(data.path("enabled").asBoolean());
        assertFalse(data.path("development").asBoolean());
    }
    @Test void explicitOverrideWinsButMissingExplicitFileDoesNotEnableBundledConfig() throws Exception {
        Path file=directory.resolve("override.json");Files.writeString(file,"{\"enabled\":false}");
        assertFalse(MobileSettings.readConfiguration(file.toString(),file,bundled("{\"enabled\":true}")).path("enabled").asBoolean());
        assertFalse(MobileSettings.readConfiguration(directory.resolve("missing.json").toString(),file,bundled("{\"enabled\":true}")).path("enabled").asBoolean());
    }
    @Test void bundledProductionConfigurationWinsOverLegacyDevelopmentFile() throws Exception {
        Path file=directory.resolve("local.json");Files.writeString(file,"{\"development\":true}");
        assertFalse(MobileSettings.readConfiguration(null,file,bundled("{\"development\":false}")).path("development").asBoolean());
        assertTrue(MobileSettings.readConfiguration(null,file,null).path("development").asBoolean());
    }
    @Test void malformedBundledConfigurationFailsClosed() {
        assertThrows(IllegalStateException.class,()->MobileSettings.readConfiguration(null,directory.resolve("missing"),bundled("invalid")));
    }
    @Test void pinnedVersionDoesNotReadOrModifySharedDatabaseDestinations() {
        var settings=new MobileSettings(MobileUpstream.JSON.valueToTree(Map.of(
                "productionServices",Map.of("manager","https://trusted.test/confiancamanger4/"),
                "allowedProductionHosts",List.of("trusted.test"))));
        var upstream=new MobileUpstream(settings) {
            @Override protected com.fasterxml.jackson.databind.JsonNode request(String url,String method,com.fasterxml.jackson.databind.JsonNode body,String auth) {
                fail("Pinned destination must not request shared configuration");return null;
            }
        };
        assertEquals("https://trusted.test/confiancamanger4/",upstream.destination("manager"));
    }
    @Test void pinnedVersionStillRequiresHttpsAndAnAllowedHost() {
        for(String value:List.of("http://trusted.test/","https://untrusted.test/","https://user:password@trusted.test/")) {
            var settings=new MobileSettings(MobileUpstream.JSON.valueToTree(Map.of(
                    "productionServices",Map.of("manager",value),"allowedProductionHosts",List.of("trusted.test"))));
            assertThrows(MobileFailure.class,()->new MobileUpstream(settings).destination("manager"));
        }
    }
}
