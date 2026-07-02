package com.confApi.db.clube.beneficioTransacaoCartao;

import com.confApi.db.clube.cartaoUsuarioCia.CartaoUsuarioCia;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Data
public class BeneficioTransacaoCartao implements Serializable {

        private Integer codgBeneficioTransacaoCartao;
        private CartaoUsuarioCia cartaoUsuarioCia;
        private Integer tipoTransacao;
        private Double valorTransacao;
        private String descricaoTransacao;
        private Date dataCadastro;
        private Integer usuarioLogadoId;
        private String usuarioLogadoNome;
        private String usuarioLogadoLogin;
}
