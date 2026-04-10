package com.confApi.hub.telegram;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TelegramService {

    @Autowired
    private TelegramAPI telegramAPI;

    public String enviarLogDeErros (String mensagem){
        return telegramAPI.enviarLogDeErros(mensagem);
    }
}
