package com.confApi.chatconfianca.service;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ChatConfiancaIaAutoEncerramentoJob {

    private static final Logger LOGGER = Logger.getLogger(ChatConfiancaIaAutoEncerramentoJob.class.getName());

    private final ChatConfiancaService chatConfiancaService;

    @Value("${chat-confianca.ia.auto-encerramento-minutos:120}")
    private int minutosInatividade;

    public ChatConfiancaIaAutoEncerramentoJob(ChatConfiancaService chatConfiancaService) {
        this.chatConfiancaService = chatConfiancaService;
    }

    @Scheduled(
            initialDelayString = "${chat-confianca.ia.auto-encerramento-initial-delay-ms:300000}",
            fixedDelayString = "${chat-confianca.ia.auto-encerramento-delay-ms:900000}"
    )
    public void executar() {
        try {
            int encerradas = chatConfiancaService.encerrarConversasIaInativas(minutosInatividade);
            if (encerradas > 0) {
                LOGGER.info("Conversas ConfIA autoencerradas por inatividade: " + encerradas);
            }
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "Nao foi possivel executar autoencerramento de conversas ConfIA.", ex);
        }
    }
}