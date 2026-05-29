package com.confApi.aereo.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class ExceptionDetailDeserializer extends JsonDeserializer<ExceptionDetail> {

    @Override
    public ExceptionDetail deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        JsonToken token = p.currentToken();

        if (token == JsonToken.VALUE_NULL) {
            return null;
        }

        if (token == JsonToken.VALUE_STRING) {
            String valor = p.getValueAsString();

            if (valor == null || valor.trim().isEmpty()) {
                return null;
            }

            ExceptionDetail detail = new ExceptionDetail();
            detail.setMessage(valor);
            return detail;
        }

        if (token == JsonToken.START_OBJECT) {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            return mapper.readValue(p, ExceptionDetail.class);
        }

        return null;
    }
}


