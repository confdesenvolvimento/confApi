package com.confApi.hub.telegram;

import com.confApi.hub.telegram.dto.MensagemRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hub/telegram")
public class TelegramController {

    @Autowired
    private TelegramService telegramService;

    @PostMapping("/enviarLogErros")
    public String enviarLogErros(@RequestBody MensagemRequest mensagem){
        return telegramService.enviarLogDeErros(mensagem);
    }
}
