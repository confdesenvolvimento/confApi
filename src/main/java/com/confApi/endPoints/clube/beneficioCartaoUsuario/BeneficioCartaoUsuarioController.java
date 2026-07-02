package com.confApi.endPoints.clube.beneficioCartaoUsuario;

import com.confApi.db.clube.beneficioCartaoUsuario.BeneficioCartaoUsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clube/beneficioCartaoUsuario")
public class BeneficioCartaoUsuarioController {

    @Autowired
    private BeneficioCartaoUsuarioService beneficioCartaoUsuarioService;

    @GetMapping("/verificaBeneficio/cartaoUsuario/{cartaoUsuarioId}/beneficio/{beneficioId}")
    public boolean verificaBeneficio(@PathVariable int cartaoUsuarioId, @PathVariable int beneficioId) {
        return beneficioCartaoUsuarioService.verificaBeneficio(cartaoUsuarioId, beneficioId);
    }
}
