package com.confApi.db.clube.cartaoUsuarioCia;

import com.confApi.db.clube.beneficioTransacaoCartao.BeneficioTransacaoCartao;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartaoUsuarioCiaDetalheDTO implements Serializable {
    private CartaoUsuarioCia cartaoUsuarioCia;
    private List<BeneficioTransacaoCartao> beneficioTransacaoCartaos;
}
