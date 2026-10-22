package com.confApi.rodoviario;

import com.confApi.db.confManager.rodoviario.RodoviarioManagerClient;
import com.confApi.hub.rodoviario.RodoviarioHubClient;
import com.confApi.rodoviario.dto.RodoviarioCompraModel;
import com.confApi.rodoviario.dto.RodoviarioConsultaRequest;
import com.confApi.rodoviario.dto.RodoviarioReservaOperacaoResponse;
import com.confApi.rodoviario.dto.PesquisaRodoviarioRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

import static com.confApi.hub.rodoviario.RodoviarioHubClient.Operacao.*;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/v1/rodoviario", "/rodoviario"})
public class RodoviarioHubController {
    private final RodoviarioHubClient hub;
    private final RodoviarioReservaService reservas;
    private final RodoviarioManagerClient manager;

    @GetMapping("/viacoes")
    public JsonNode viacoes(@RequestParam(required = false) String sistema) {
        return hub.catalogo("viacoes", null, sistema);
    }

    @GetMapping("/viacoes/{codigo}")
    public JsonNode viacao(@PathVariable String codigo, @RequestParam(required = false) String sistema) {
        return hub.catalogo("viacoes", codigo, sistema);
    }

    @GetMapping("/localidades")
    public JsonNode localidades(@RequestParam(required = false) String sistema) {
        return hub.catalogo("localidades", null, sistema);
    }

    @GetMapping("/localidades/{codigo}")
    public JsonNode localidade(@PathVariable String codigo, @RequestParam(required = false) String sistema) {
        return hub.catalogo("localidades", codigo, sistema);
    }

    @GetMapping("/chavepagamento")
    public JsonNode chave(@RequestParam(required = false) String sistema) {
        return hub.catalogo("chavepagamento", null, sistema);
    }

    @PostMapping({"/pesquisar", "/pesquisa"})
    public JsonNode pesquisar(@Valid @RequestBody PesquisaRodoviarioRequestDTO request) {
        return hub.pesquisar(request);
    }

    @PostMapping("/pesquisaconexoes")
    public JsonNode conexoes(@Valid @RequestBody PesquisaRodoviarioRequestDTO request) {
        return hub.pesquisarConexoes(request);
    }

    @PostMapping("/sugestoesconexao")
    public JsonNode sugestoes(@Valid @RequestBody PesquisaRodoviarioRequestDTO request) {
        return hub.sugerirConexoes(request);
    }

    @PostMapping({"/mapadeassentos", "/mapaDeAssentos"})
    public JsonNode assentos(@RequestBody JsonNode r) {
        return hub.post(mapadeassentos, r);
    }

    @PostMapping("/reservar")
    public RodoviarioReservaOperacaoResponse reservar(@RequestBody RodoviarioCompraModel r,
                                                      @RequestHeader("X-Usuario-Token") String usuario) {
        RodoviarioReservaOperacaoResponse teste = reservas.reservar(r, usuario);
        System.out.println("RESPONSE RESERVAR: " + teste);
        return teste;
    }

    @PostMapping("/emitir")
    public RodoviarioReservaOperacaoResponse emitir(@RequestBody RodoviarioCompraModel r,
                                                    @RequestHeader("X-Usuario-Token") String usuario) {
        return reservas.emitir(r, usuario);
    }

    @PostMapping({"/consultarReserva", "/consultar"})
    public RodoviarioReservaOperacaoResponse consultar(@RequestBody RodoviarioConsultaRequest r,
                                                       @RequestHeader("X-Usuario-Token") String usuario) {
        return reservas.consultarReserva(r, usuario);
    }

    @PostMapping({"/cancelarReserva", "/cancelarcompra"})
    public RodoviarioReservaOperacaoResponse cancelar(@RequestBody RodoviarioConsultaRequest r,
                                                      @RequestHeader("X-Usuario-Token") String usuario) {
        return reservas.cancelarReserva(r, usuario, false);
    }

    @PostMapping({"/cancelarBilhete", "/cancelarbilhete"})
    public RodoviarioReservaOperacaoResponse cancelarBilhete(@RequestBody RodoviarioConsultaRequest r,
                                                             @RequestHeader("X-Usuario-Token") String usuario) {
        return reservas.cancelarReserva(r, usuario, true);
    }

    @PostMapping("/sincronizar")
    public RodoviarioReservaOperacaoResponse sincronizar(@RequestBody RodoviarioCompraModel r,
                                                         @RequestHeader("X-Usuario-Token") String usuario) {
        return reservas.reconciliar(r, usuario);
    }

    @GetMapping("/findAllReservas")
    public JsonNode listar(@RequestParam MultiValueMap<String, String> filtros,
                           @RequestHeader("X-Usuario-Token") String usuario) {
        return manager.crud(RodoviarioManagerClient.Recurso.reservas, null, HttpMethod.GET, null, filtros, usuario);
    }
}
