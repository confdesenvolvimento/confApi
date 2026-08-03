package com.confApi.chatconfianca.controller;

import com.confApi.chatconfianca.dto.remarcacao.RemarcacaoRequest;
import com.confApi.chatconfianca.dto.remarcacao.RemarcacaoSimulacaoResponse;
import com.confApi.chatconfianca.dto.remarcacao.ReservasEmitidasRemarcacaoResponse;
import com.confApi.chatconfianca.service.ChatConfiancaRemarcacaoService;
import com.confApi.exception.RegraDeNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/chat-confianca/remarcacoes")
public class ChatConfiancaRemarcacaoController {
    private final ChatConfiancaRemarcacaoService service;
    private final String loginClientePayara;

    public ChatConfiancaRemarcacaoController(ChatConfiancaRemarcacaoService service,
                                             @Value("${chat-confianca.cliente-payara.login:api.confplus}")
                                             String loginClientePayara) {
        this.service = service;
        this.loginClientePayara = loginClientePayara;
    }

    @GetMapping("/reservas-emitidas")
    public ReservasEmitidasRemarcacaoResponse listarReservasEmitidas(
            @RequestParam Long conversaId,
            @RequestParam Integer codgUsuario,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataEmissaoInicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataEmissaoFim,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            Authentication authentication) {
        validarClientePayara(authentication);
        return service.listarReservasEmitidas(
                conversaId,
                codgUsuario,
                busca,
                dataEmissaoInicio,
                dataEmissaoFim,
                page,
                size);
    }

    @PostMapping("/iniciar")
    public RemarcacaoSimulacaoResponse iniciar(@RequestBody RemarcacaoRequest.Iniciar request,
                                                Authentication authentication) {
        validarClientePayara(authentication);
        return service.iniciar(request);
    }

    @PostMapping("/{id}/trecho")
    public RemarcacaoSimulacaoResponse selecionarTrecho(@PathVariable Long id,
                                                         @RequestBody RemarcacaoRequest.SelecionarTrecho request,
                                                         Authentication authentication) {
        validarClientePayara(authentication);
        return service.selecionarTrecho(id, request);
    }

    @PostMapping("/{id}/passageiros")
    public RemarcacaoSimulacaoResponse selecionarPassageiros(
            @PathVariable Long id,
            @RequestBody RemarcacaoRequest.SelecionarPassageiros request,
            Authentication authentication) {
        validarClientePayara(authentication);
        return service.selecionarPassageiros(id, request);
    }

    @PostMapping("/{id}/pesquisar")
    public RemarcacaoSimulacaoResponse pesquisar(@PathVariable Long id,
                                                 @RequestBody RemarcacaoRequest.Pesquisar request,
                                                 Authentication authentication) {
        validarClientePayara(authentication);
        return service.pesquisar(id, request);
    }

    @PostMapping("/{id}/simular")
    public RemarcacaoSimulacaoResponse simular(@PathVariable Long id,
                                               @RequestBody RemarcacaoRequest.Simular request,
                                               Authentication authentication) {
        validarClientePayara(authentication);
        return service.simular(id, request);
    }

    @PostMapping("/{id}/forma-pagamento")
    public RemarcacaoSimulacaoResponse selecionarFormaPagamento(
            @PathVariable Long id,
            @RequestBody RemarcacaoRequest.SelecionarFormaPagamento request,
            Authentication authentication) {
        validarClientePayara(authentication);
        return service.selecionarFormaPagamento(id, request);
    }

    @PostMapping("/{id}/encaminhar")
    public RemarcacaoSimulacaoResponse encaminhar(@PathVariable Long id,
                                                  @RequestBody RemarcacaoRequest.Encaminhar request,
                                                  Authentication authentication) {
        validarClientePayara(authentication);
        return service.encaminhar(id, request);
    }

    @GetMapping("/{id}")
    public RemarcacaoSimulacaoResponse consultar(@PathVariable Long id,
                                                 @RequestParam Integer codgUsuario,
                                                 Authentication authentication) {
        validarClientePayara(authentication);
        return service.consultar(id, codgUsuario);
    }

    private void validarClientePayara(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()
                || loginClientePayara == null || loginClientePayara.isBlank()
                || !loginClientePayara.trim().equalsIgnoreCase(authentication.getName().trim())) {
            throw acessoNegado();
        }
    }

    private RegraDeNegocioException acessoNegado() {
        return new RegraDeNegocioException(403,
                "Cliente nao autorizado para o fluxo de remarcacao do Chat Confianca.");
    }
}
