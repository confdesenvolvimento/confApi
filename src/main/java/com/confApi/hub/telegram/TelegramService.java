package com.confApi.hub.telegram;

import com.confApi.hub.telegram.dto.MensagemRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TelegramService {

    @Autowired
    private TelegramAPI telegramAPI;

    public String enviarLogDeErros (MensagemRequest mensagem){
        return telegramAPI.enviarLogDeErros(mensagem);
    }
}
