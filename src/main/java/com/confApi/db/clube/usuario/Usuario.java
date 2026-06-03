package com.confApi.db.clube.usuario;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Usuario {

    private Integer codgUsuario;
    private String nomeUsuario;
    private String loginUsuario;
    private Integer idUsuarioManger;
    private Integer idWoobaUsuario;
    private String cpf;
    private String pix;
    private String tipoPix;
    @Temporal(TemporalType.DATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    private Date dataCadastro;
    @Temporal(TemporalType.DATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    private Date dataAlteracao;
    private Integer codgAgencia;
    private String nomeAgencia;
    private Integer idWoobaAgencia;
    private Integer codgUnidade;
    private String nomeUnidade;
    private Integer idWoobaUnidade;
}
