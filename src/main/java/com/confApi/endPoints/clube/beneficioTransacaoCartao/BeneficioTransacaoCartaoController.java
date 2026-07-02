package com.confApi.endPoints.clube.beneficioTransacaoCartao;

import com.confApi.db.clube.beneficioTransacaoCartao.BeneficioTransacaoCartao;
import com.confApi.db.clube.beneficioTransacaoCartao.BeneficioTransacaoCartaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clube/beneficioTransacaoCartao")
public class BeneficioTransacaoCartaoController {

    @Autowired
    private BeneficioTransacaoCartaoService beneficioTransacaoCartaoService;

    @GetMapping("/transacaoUsuarioExtrato/cartaoUsuario/{cartaoUsuarioId}")
    public List<BeneficioTransacaoCartao> getAllByTransacaoUsuarioExtrato(@PathVariable int cartaoUsuarioId) {
        System.out.println("1 : "+cartaoUsuarioId);
        System.out.println("2 : "+beneficioTransacaoCartaoService.getAllByTransacaoUsuarioExtrato(cartaoUsuarioId).size());
        return beneficioTransacaoCartaoService.getAllByTransacaoUsuarioExtrato(cartaoUsuarioId);
    }
}
