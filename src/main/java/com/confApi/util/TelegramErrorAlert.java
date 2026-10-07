package com.confApi.util;

import com.confApi.hub.telegram.TelegramService;
import com.confApi.hub.telegram.dto.MensagemRequest;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class TelegramErrorAlert {

    private static final Logger LOG = Logger.getLogger(TelegramErrorAlert.class.getName());
    private final TelegramService telegramService;

    public TelegramErrorAlert(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    public void enviar(Object source, String mensagem) {
        enviar(source, mensagem, null);
    }

    public void enviar(Object source, String mensagem, Exception e) {
        try {
            MensagemRequest msg = new MensagemRequest(mensagemComErro(mensagem, e));
            msg.setMetodo(metodoChamador());
            msg.setClasse(classe(source));
            msg.setProjeto("CONFAPI : " + getServerName());

            telegramService.enviarLogDeErros(msg);
        } catch (Exception telegramException) {
            LOG.log(Level.WARNING, "Nao foi possivel enviar alerta de erro para o Telegram.", telegramException);
        }
    }

    private String mensagemComErro(String mensagem, Exception e) {
        return ErrorDiagnostic.format(mensagem, e);
    }

    private String classe(Object source) {
        if (source == null) {
            return "Indefinida";
        }
        if (source instanceof Class<?>) {
            return ((Class<?>) source).getSimpleName();
        }
        return source.getClass().getSimpleName();
    }

    private String metodoChamador() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (!className.equals(Thread.class.getName())
                    && !className.equals(TelegramErrorAlert.class.getName())
                    && !element.getMethodName().equals("alertarErro")) {
                return element.getMethodName();
            }
        }
        return "desconhecido";
    }

    private String getServerName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "desconhecido";
        }
    }
}
