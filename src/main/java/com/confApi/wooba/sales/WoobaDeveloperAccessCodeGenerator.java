package com.confApi.wooba.sales;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Component
public class WoobaDeveloperAccessCodeGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Clock clock;

    public WoobaDeveloperAccessCodeGenerator() {
        this(Clock.systemDefaultZone());
    }

    WoobaDeveloperAccessCodeGenerator(Clock clock) {
        this.clock = clock;
    }

    public String gerar(WoobaSalesProperties properties) {
        try {
            PublicKey publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder()
                            .decode(properties.getDeveloperAccessCodePublicKey())));

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            String payload = properties.getDeveloperAccessCodePrefix() + formatarData();
            byte[] encryptedData = cipher.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            throw new IllegalStateException("Nao foi possivel gerar developer-access-code da Wooba.", e);
        }
    }

    private String formatarData() {
        return LocalDate.now(clock).format(DATE_FORMATTER);
    }
}
