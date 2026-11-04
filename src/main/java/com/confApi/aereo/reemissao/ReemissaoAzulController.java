package com.confApi.aereo.reemissao;

import com.confApi.exception.RegraDeNegocioException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/aereo/reemissoes-azul")
public class ReemissaoAzulController {
    private final ReemissaoAzulService service;
    private final String cliente;
    private final ObjectMapper mapper;
    public ReemissaoAzulController(ReemissaoAzulService service, @Value("${chat-confianca.cliente-payara.login:api.confplus}") String cliente, ObjectMapper mapper) { this.service = service; this.cliente = cliente; this.mapper = mapper; }
    @PostMapping("/iniciar") public ObjectNode iniciar(@RequestBody ReemissaoAzulRequest pedido, Authentication auth) { validar(auth); return service.iniciar(mapper.valueToTree(pedido)); }
    @PostMapping("/{id}/{acao}") public ObjectNode executar(@PathVariable String id, @PathVariable String acao, @RequestBody ReemissaoAzulRequest pedido, Authentication auth) { validar(auth); return service.executar(id, acao, mapper.valueToTree(pedido)); }
    private void validar(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || cliente == null || !cliente.equalsIgnoreCase(auth.getName())) throw new RegraDeNegocioException(403, "Cliente nao autorizado para reemissao.");
    }
}
