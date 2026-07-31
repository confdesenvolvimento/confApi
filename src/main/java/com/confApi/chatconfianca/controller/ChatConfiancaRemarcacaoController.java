package com.confApi.chatconfianca.controller;

import com.confApi.chatconfianca.dto.remarcacao.RemarcacaoRequest;
import com.confApi.chatconfianca.dto.remarcacao.RemarcacaoSimulacaoResponse;
import com.confApi.chatconfianca.service.ChatConfiancaRemarcacaoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/chat-confianca/remarcacoes")
public class ChatConfiancaRemarcacaoController {
    private final ChatConfiancaRemarcacaoService service;

    public ChatConfiancaRemarcacaoController(ChatConfiancaRemarcacaoService service) {
        this.service = service;
    }

    @PostMapping("/iniciar")
    public RemarcacaoSimulacaoResponse iniciar(@RequestBody RemarcacaoRequest.Iniciar request) {
        return service.iniciar(request);
    }

    @PostMapping("/{id}/trecho")
    public RemarcacaoSimulacaoResponse selecionarTrecho(@PathVariable Long id,
                                                        @RequestBody RemarcacaoRequest.SelecionarTrecho request) {
        return service.selecionarTrecho(id, request);
    }

    @PostMapping("/{id}/passageiros")
    public RemarcacaoSimulacaoResponse selecionarPassageiros(
            @PathVariable Long id,
            @RequestBody RemarcacaoRequest.SelecionarPassageiros request) {
        return service.selecionarPassageiros(id, request);
    }

    @PostMapping("/{id}/pesquisar")
    public RemarcacaoSimulacaoResponse pesquisar(@PathVariable Long id,
                                                 @RequestBody RemarcacaoRequest.Pesquisar request) {
        return service.pesquisar(id, request);
    }

    @PostMapping("/{id}/simular")
    public RemarcacaoSimulacaoResponse simular(@PathVariable Long id,
                                               @RequestBody RemarcacaoRequest.Simular request) {
        return service.simular(id, request);
    }

    @PostMapping("/{id}/forma-pagamento")
    public RemarcacaoSimulacaoResponse selecionarFormaPagamento(
            @PathVariable Long id,
            @RequestBody RemarcacaoRequest.SelecionarFormaPagamento request) {
        return service.selecionarFormaPagamento(id, request);
    }

    @PostMapping("/{id}/encaminhar")
    public RemarcacaoSimulacaoResponse encaminhar(@PathVariable Long id,
                                                  @RequestBody RemarcacaoRequest.Encaminhar request) {
        return service.encaminhar(id, request);
    }

    @GetMapping("/{id}")
    public RemarcacaoSimulacaoResponse consultar(@PathVariable Long id,
                                                 @RequestParam Integer codgUsuario) {
        return service.consultar(id, codgUsuario);
    }
}
