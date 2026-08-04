package com.confApi.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public class JsonLogUtil {

    private static final Logger LOG = Logger.getLogger(JsonLogUtil.class.getName());

    private JsonLogUtil() {
    }

    public static void logRequest(String descricao, Object request) {
        // Intencionalmente vazio: requests podem conter CPF, tokens e dados de pagamento.
    }

    public static void logResponse(String descricao, Object response) {
        // Intencionalmente vazio: responses podem conter localizadores, tokens e dados pessoais.
    }

    public static void logErro(String descricao, Exception e) {
        LOG.log(Level.SEVERE, descricao, e);
    }
}
