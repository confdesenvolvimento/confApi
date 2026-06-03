package com.confApi.endPoints.clube.quemParticipa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clube/quemParticipa")
public class QuemParticipaController {

    @Autowired
    private QuemParticipaApi quemParticipaApi;

    @GetMapping("/consulta")
    public ResponseEntity<?> consulta() {
        return ResponseEntity.ok(quemParticipaApi.consulta());
    }


}
