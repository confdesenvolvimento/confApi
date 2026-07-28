package com.confApi.db.confManager.agencia;

import com.confApi.db.confManager.agencia.dto.Agencia;
import lombok.Data;

import java.util.List;

@Data
public class AgenciaAssociadas {
    private List<Agencia> agencia;
}
