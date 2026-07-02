package com.confApi.db.clube.beneficioCartaoUsuario;

import com.confApi.db.clube.beneficioCartao.BeneficioCartao;
import com.confApi.db.clube.cartaoUsuarioCia.CartaoUsuarioCia;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Data
public class BeneficioCartaoUsuario implements Serializable {

    private Integer codgBeneficioCartaoUsuario;
    private CartaoUsuarioCia cartaoUsuarioCia;
    private BeneficioCartao beneficioCartao;
    private Integer status;
}
