package com.confApi.db.clube.beneficioTransacaoCartao;

import com.confApi.endPoints.clube.beneficioTransacaoCartao.BeneficioTransacaoCartaoApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BeneficioTransacaoCartaoService {

    @Autowired
    private BeneficioTransacaoCartaoApi beneficioTransacaoCartaoApi;

    public List<BeneficioTransacaoCartao> getAllByTransacaoUsuarioExtrato(int cartaoUsuarioId) {
        return beneficioTransacaoCartaoApi.getAllByTransacaoUsuarioExtrato(cartaoUsuarioId);
    }
}
