package com.confApi.db.clube.beneficioCartaoUsuario;

import com.confApi.endPoints.clube.beneficioCartaoUsuario.BeneficioCartaoUsuarioApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BeneficioCartaoUsuarioService {
    @Autowired
    private BeneficioCartaoUsuarioApi beneficioCartaoUsuarioApi;

    public boolean verificaBeneficio(int cartaoUsuarioId, int beneficioId) {
        return beneficioCartaoUsuarioApi.verificaBeneficio(cartaoUsuarioId, beneficioId);
    }
}
