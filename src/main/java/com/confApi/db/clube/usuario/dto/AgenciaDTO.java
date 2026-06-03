package com.confApi.db.clube.usuario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
@NoArgsConstructor
@AllArgsConstructor
@Data
public class AgenciaDTO {
    private Integer codgAgencia;
    private String nomeAgencia;
    private Integer idWoobaAgencia;
}
