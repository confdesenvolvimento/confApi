package com.confApi.chatconfianca.controller;

import com.confApi.chatconfianca.dto.model.AtendenteStatus;
import com.confApi.chatconfianca.dto.model.RespostaRapida;
import com.confApi.chatconfianca.service.ChatConfiancaConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/chat-confianca/atendente")
public class ChatConfiancaAtendenteController {
    private final ChatConfiancaConfigService service;

    public ChatConfiancaAtendenteController(ChatConfiancaConfigService service) {
        this.service = service;
    }

    @GetMapping("/respostas-rapidas")
    public List<RespostaRapida> listarRespostasRapidas(@RequestParam(required = false) Long departamentoId,
                                                       @RequestParam(required = false) Integer codgUnidade) {
        return service.listarRespostasRapidas(departamentoId, codgUnidade, true);
    }

    @PostMapping("/respostas-rapidas")
    public RespostaRapida salvarRespostaRapida(@RequestBody RespostaRapida entity) {
        return service.salvarRespostaRapida(entity);
    }

    @GetMapping("/status/{codgUsuario}")
    public AtendenteStatus buscarStatus(@PathVariable Integer codgUsuario) {
        return service.buscarAtendenteStatus(codgUsuario);
    }

    @PostMapping("/status")
    public AtendenteStatus salvarStatus(@RequestBody AtendenteStatus status) {
        return service.salvarAtendenteStatus(status);
    }
}