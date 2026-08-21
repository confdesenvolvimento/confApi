package com.confApi.endPoints.reservaAereo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/reservaAereo")
public class ReservaAereoController {

    @Autowired
    private ReservaAereoService pesquisaResquestService;

    @RequestMapping(value = "/consultarLocalizador", method = RequestMethod.POST)
    public ResponseEntity<ReservaAereoResponse> pesquisaRequest(@RequestBody @Valid ReservaAereoConsultarLocalizadorRequest obj) throws Exception {
        return ResponseEntity.ok().body(pesquisaResquestService.consultarLocalizador(obj));
    }

    @GetMapping("/grupo/permissao")
    public ResponseEntity<Boolean> consultarPermissaoGrupo(
            @RequestParam Integer codgUsuarioSolicitante,
            @RequestParam String loginUsuarioSolicitante) {
        return ResponseEntity.ok(pesquisaResquestService.consultarPermissaoGrupo(
                codgUsuarioSolicitante, loginUsuarioSolicitante));
    }

    @GetMapping("/{id}/grupo")
    public ResponseEntity<Boolean> consultarGrupo(@PathVariable Integer id) {
        return ResponseEntity.ok(pesquisaResquestService.consultarGrupo(id));
    }

    @PutMapping("/{id}/grupo")
    public ResponseEntity<Void> atualizarGrupo(@PathVariable Integer id,
                                                @RequestBody GrupoReservaAereoRequest request) {
        pesquisaResquestService.atualizarGrupo(id, request);
        return ResponseEntity.ok().build();
    }
}

