package com.confApi.db.clube.cartaoUsuarioCia;

import com.confApi.db.clube.cartaoCia.CartaoCia;
import com.confApi.db.clube.usuario.UsuarioClube;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Data
public class CartaoUsuarioCia implements Serializable {

    private Integer codgCartaoUsuarioCia;
    private UsuarioClube usuario;
    private CartaoCia cartaoCia;
    private String numeroCartao;
    private Date validade;
    private Date dataCriacao;
    private Double saldo;
    private Integer statusCartao;
}
