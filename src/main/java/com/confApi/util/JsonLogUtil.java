package com.confApi.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.logging.Level;
import java.util.logging.Logger;

public class JsonLogUtil {

    private static final Logger LOG = Logger.getLogger(JsonLogUtil.class.getName());

    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /*
     * Ative ou desative aqui.
     * true  = imprime os JSONs
     * false = não imprime nada
     */
    private static final boolean ATIVO = true;

    private JsonLogUtil() {
    }

    public static void logRequest(String descricao, Object request) {
        if (!ATIVO) {
            return;
        }

        try {
            LOG.log(Level.INFO,
                    "\n================ REQUEST JSON - {0} ================\n{1}\n====================================================",
                    new Object[]{
                            descricao,
                            mapper.writeValueAsString(request)
                    });
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Erro ao gerar log JSON do request: " + descricao, e);
        }
    }

    public static void logResponse(String descricao, Object response) {
        if (!ATIVO) {
            return;
        }

        try {
            LOG.log(Level.INFO,
                    "\n================ RESPONSE JSON - {0} ================\n{1}\n=====================================================",
                    new Object[]{
                            descricao,
                            mapper.writeValueAsString(response)
                    });
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Erro ao gerar log JSON do response: " + descricao, e);
        }
    }

    public static void logErro(String descricao, Exception e) {
        LOG.log(Level.SEVERE, descricao, e);
    }
}