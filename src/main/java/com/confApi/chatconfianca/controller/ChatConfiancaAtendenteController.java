package com.confApi.chatconfianca.controller;

import com.confApi.chatconfianca.dto.model.AtendenteStatus;
import com.confApi.chatconfianca.dto.model.RespostaRapida;
import com.confApi.chatconfianca.service.ChatConfiancaService;
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
    private final ChatConfiancaService service;

    public ChatConfiancaAtendenteController(ChatConfiancaService service) {
        this.service = service;
    }

    @GetMapping("/respostas-rapidas")
    public List<RespostaRapida> listarRespostasRapidas(@RequestParam Integer codgUsuario,
                                                       @RequestParam(required = false) Long departamentoId,
                                                       @RequestParam(required = false) Integer codgUnidade,
                                                       @RequestParam(required = false) Long departamentoUnidadeId) {
        return service.listarRespostasRapidasAtendente(
                codgUsuario, departamentoId, codgUnidade, departamentoUnidadeId);
    }

    @PostMapping("/respostas-rapidas")
    public RespostaRapida salvarRespostaRapida(@RequestParam Integer codgUsuario,
                                               @RequestBody RespostaRapida entity) {
        return service.salvarRespostaRapidaAtendente(codgUsuario, entity);
    }

    @GetMapping("/status/{codgUsuario}")
    public AtendenteStatus buscarStatus(@PathVariable Integer codgUsuario,
                                        @RequestParam Integer codgUsuarioSolicitante) {
        return service.buscarAtendenteStatus(codgUsuarioSolicitante, codgUsuario);
    }

    @PostMapping("/status")
    public AtendenteStatus salvarStatus(@RequestParam Integer codgUsuarioSolicitante,
                                        @RequestBody AtendenteStatus status) {
        return service.salvarAtendenteStatus(codgUsuarioSolicitante, status);
    }
}
