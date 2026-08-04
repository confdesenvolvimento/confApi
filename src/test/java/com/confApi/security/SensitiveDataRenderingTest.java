package com.confApi.security;

import com.confApi.carros.dto.CartaoDeCreditoCarroHub;
import com.confApi.db.confManager.bandeira.Bandeira;
import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.endPoints.cartao.CartaoResponse;
import com.confApi.hub.aereo.CartaoDeCreditoHub;
import com.confApi.hub.aereo.CartaoModel;
import com.confApi.hub.aereo.RecebimentoModel;
import com.confApi.wooba.webhook.WoobaWebhookRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitiveDataRenderingTest {

    private static final String PAN = "4111111111111111";
    private static final String CVV = "987";
    private static final String HOLDER = "CLIENTE TESTE";
    private static final String EXPIRATION = "12/40";
    private static final String AUTHORIZATION = "AUTH-SECRET";
    private static final String TRANSACTION = "TX-SECRET";
    private static final String PIX = "000201-pix-copia-e-cola";
    private static final String LINK = "https://payment.invalid/secret";

    @Test
    void paymentModelsDoNotRenderCardData() {
        CartaoModel card = cardModel();

        String cardText = card.toString();
        assertRedacted(cardText, PAN, CVV, HOLDER, EXPIRATION, AUTHORIZATION, TRANSACTION);
        assertTrue(cardText.contains("possuiNumeroCartao=true"));

        RecebimentoModel payment = new RecebimentoModel();
        payment.setCartaoSelecionado(card);
        payment.setAssinatura("signature-secret");
        payment.setLink(LINK);

        String paymentText = payment.toString();
        assertRedacted(paymentText, PAN, CVV, "signature-secret", LINK);
        assertTrue(paymentText.contains("possuiCartao=true"));
    }

    @Test
    void persistenceModelDoesNotRenderCardOrPixData() {
        Recebimento payment = persistedPayment();

        String rendered = payment.toString();
        assertRedacted(rendered, PAN, CVV, HOLDER, EXPIRATION, AUTHORIZATION, TRANSACTION, PIX, LINK);
        assertTrue(rendered.contains("possuiCartao=true"));
        assertTrue(rendered.contains("possuiPix=true"));
    }

    @Test
    void lombokGeneratedRenderingExcludesSensitivePaymentFields() {
        CartaoDeCreditoHub airCard = new CartaoDeCreditoHub();
        airCard.setNumero(PAN);
        airCard.setCodigoDeSeguranca(CVV);
        airCard.setTitularCPF("12345678901");
        airCard.setTitularNome(HOLDER);
        airCard.setValidade(EXPIRATION);
        airCard.setAutorizacao(AUTHORIZATION);
        assertRedacted(airCard.toString(), PAN, CVV, "12345678901", HOLDER, EXPIRATION, AUTHORIZATION);

        CartaoDeCreditoCarroHub carCard = new CartaoDeCreditoCarroHub();
        carCard.setNumeroCartao(PAN);
        carCard.setCvv(CVV);
        carCard.setNomeProprietario(HOLDER);
        carCard.setValidadeMes(12);
        carCard.setValidadeAno(2040);
        assertRedacted(carCard.toString(), PAN, CVV, HOLDER, "2040");

        CartaoResponse response = new CartaoResponse(persistedPayment());
        assertRedacted(response.toString(), PAN, CVV, HOLDER, EXPIRATION, AUTHORIZATION, TRANSACTION);
    }

    @Test
    void webhookRenderingDoesNotExposeReservationIdentifiers() {
        WoobaWebhookRequest request = new WoobaWebhookRequest();
        request.setId(987654321L);
        request.setUniqueId("unique-id-secret");
        request.setLocator("locator-secret");
        request.setTicket("ticket-secret");
        request.setLastUpdate("last-update-secret");

        assertRedacted(
                request.toString(),
                "987654321",
                "unique-id-secret",
                "locator-secret",
                "ticket-secret",
                "last-update-secret"
        );
        assertRedacted(
                request.resumo(),
                "987654321",
                "unique-id-secret",
                "locator-secret",
                "ticket-secret",
                "last-update-secret"
        );
        assertTrue(request.resumo().contains("possuiLocator=true"));
    }

    private CartaoModel cardModel() {
        CartaoModel card = new CartaoModel();
        card.setNumeroCartao(PAN);
        card.setCodgSegurancaCartao(CVV);
        card.setTitularBandeira(HOLDER);
        card.setValidadeCartao(EXPIRATION);
        card.setCodgAutorizacao(AUTHORIZATION);
        card.setCodgTransacao(TRANSACTION);
        return card;
    }

    private Recebimento persistedPayment() {
        Recebimento payment = new Recebimento();
        payment.setNumrCartao(PAN);
        payment.setCodgSegCartao(CVV);
        payment.setTitularCartao(HOLDER);
        payment.setValidadeCartao(EXPIRATION);
        payment.setCodgAutCartao(AUTHORIZATION);
        payment.setCodgTransacao(TRANSACTION);
        payment.setCopiacolaPix(PIX);
        payment.setQrcodePix("qr-secret");
        payment.setAssinaturaEletronica("signature-secret");
        payment.setLink(LINK);
        payment.setQtdeParcela(1);
        payment.setCodgBandeira(new Bandeira(1));
        return payment;
    }

    private void assertRedacted(String rendered, String... secrets) {
        for (String secret : secrets) {
            assertFalse(rendered.contains(secret), () -> "Sensitive value rendered: " + secret);
        }
    }
}
