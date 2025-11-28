package com.confApi.endPoints.reservaAereo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/reservaAereo")
public class ReservaAereoController {

    @Autowired
    private ReservaAereoService pesquisaResquestService;

    @RequestMapping(value = "/consultarLocalizador", method = RequestMethod.POST)
    public ResponseEntity<ReservaAereoResponse> pesquisaRequest(@RequestBody @Valid ReservaAereoConsultarLocalizadorRequest obj) throws Exception {
        return ResponseEntity.ok().body(pesquisaResquestService.consultarLocalizador(obj));
    }
}

