package com.confApi.carros.dto;

import com.confApi.db.confManager.usuario.Usuario;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelarCarroModel {

    private CancelarReservaCarroRequestDTO cancelarReservaCarroRequestDTO;
    private Usuario usuario;
    private String descricaoMotivoCancelamento;
}
