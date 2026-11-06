package com.confApi.rodoviario;

import com.confApi.db.confManager.rodoviario.RodoviarioManagerClient;
import com.confApi.db.confManager.rodoviario.RodoviarioManagerClient.Recurso;
import com.confApi.db.confManager.rodoviario.dto.RegistrarRecebimentoRodoviarioRequest;
import com.confApi.db.confManager.rodoviario.dto.ReservaRodoviarioDetalheDTO;
import com.confApi.db.confManager.rodoviario.dto.SalvarReservaRodoviarioRequest;
import com.confApi.db.confManager.rodoviario.dto.StatusRodoviarioRequest;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/rodoviario", "/v1/rodoviario"})
public class RodoviarioController {
    private final RodoviarioManagerClient client;

    @ExceptionHandler(HttpStatusCodeException.class)
    public ResponseEntity<String> erroManager(HttpStatusCodeException ex) {
        return ResponseEntity.status(ex.getRawStatusCode()).contentType(MediaType.APPLICATION_JSON).body(ex.getResponseBodyAsString());
    }

    @PostMapping("/reservas/salvarCompleta")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaRodoviarioDetalheDTO salvarCompleta(@RequestBody SalvarReservaRodoviarioRequest r, @RequestHeader(value = "X-Usuario-Token", required = false) String u) {
        return client.salvarCompleta(r, u);
    }

    @GetMapping("/reservas/{id}/detalhe")
    public ReservaRodoviarioDetalheDTO detalhe(@PathVariable Integer id, @RequestHeader(value = "X-Usuario-Token", required = false) String u) {
        return client.detalhe(id, u);
    }

    @GetMapping("/{recurso}")
    public JsonNode listar(@PathVariable Recurso recurso, @RequestParam MultiValueMap<String, String> filtros, @RequestHeader(value = "X-Usuario-Token", required = false) String u) {
        return client.crud(recurso, null, HttpMethod.GET, null, filtros, u);
    }

    @GetMapping("/{recurso}/{id}")
    public JsonNode consultar(@PathVariable Recurso recurso, @PathVariable Integer id, @RequestHeader(value = "X-Usuario-Token", required = false) String u) {
        return client.crud(recurso, id, HttpMethod.GET, null, null, u);
    }

    @PostMapping("/{recurso}")
    @ResponseStatus(HttpStatus.CREATED)
    public JsonNode criar(@PathVariable Recurso recurso, @RequestBody JsonNode r, @RequestHeader(value = "X-Usuario-Token", required = false) String u) {
        return client.crud(recurso, null, HttpMethod.POST, r, null, u);
    }

    @PutMapping("/{recurso}/{id}")
    public JsonNode alterar(@PathVariable Recurso recurso, @PathVariable Integer id, @RequestBody JsonNode r, @RequestHeader(value = "X-Usuario-Token", required = false) String u) {
        return client.crud(recurso, id, HttpMethod.PUT, r, null, u);
    }

    @DeleteMapping("/{recurso}/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Recurso recurso, @PathVariable Integer id, @RequestHeader(value = "X-Usuario-Token", required = false) String u) {
        client.crud(recurso, id, HttpMethod.DELETE, null, null, u);
    }

    @PutMapping("/reservas/{id}/statusFornecedor")
    public JsonNode status(@PathVariable Integer id, @RequestBody StatusRodoviarioRequest r, @RequestHeader(value = "X-Usuario-Token", required = false) String u) {
        return client.trocar("reservaRodoviario/" + id + "/statusFornecedor", HttpMethod.PUT, r, null, u, JsonNode.class, true);
    }

    @GetMapping("/reservas/{id}/historico")
    public JsonNode historico(@PathVariable Integer id, @RequestHeader(value = "X-Usuario-Token", required = false) String u) {
        return client.trocar("reservaRodoviario/" + id + "/historico", HttpMethod.GET, null, null, u, JsonNode.class, true);
    }

    @PostMapping("/reservas/{id}/historico")
    @ResponseStatus(HttpStatus.CREATED)
    public void nota(@PathVariable Integer id, @RequestBody JsonNode r, @RequestHeader(value = "X-Usuario-Token", required = false) String u) {
        client.trocar("reservaRodoviario/" + id + "/historico", HttpMethod.POST, r, null, u, Void.class, true);
    }

    @GetMapping("/reservas/{id}/recebimentos")
    public JsonNode recebimentos(@PathVariable Integer id, @RequestHeader(value = "X-Usuario-Token", required = false) String u) {
        return client.trocar("reservaRodoviario/" + id + "/recebimentos", HttpMethod.GET, null, null, u, JsonNode.class, true);
    }

    @PostMapping("/reservas/{id}/recebimentos")
    @ResponseStatus(HttpStatus.CREATED)
    public JsonNode recebimento(@PathVariable Integer id, @RequestBody RegistrarRecebimentoRodoviarioRequest r, @RequestHeader(value = "X-Usuario-Token", required = false) String u) {
        return client.trocar("reservaRodoviario/" + id + "/recebimentos", HttpMethod.POST, r, null, u, JsonNode.class, true);
    }

    @GetMapping("/destinos")
    public JsonNode destinos(@RequestParam MultiValueMap<String, String> filtros) {
        return client.trocar("rodoviario/destinos", HttpMethod.GET, null, filtros, null, JsonNode.class, false);
    }

    @GetMapping("/destinos/{id}/substops")
    public JsonNode substops(@PathVariable Integer id, @RequestParam MultiValueMap<String, String> filtros) {
        return client.trocar("rodoviario/destinos/" + id + "/substops", HttpMethod.GET, null, filtros, null, JsonNode.class, false);
    }
}
