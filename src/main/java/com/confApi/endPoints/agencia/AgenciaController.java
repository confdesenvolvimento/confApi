package com.confApi.endPoints.agencia;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@RestController
@RequestMapping("/api/agencia")
public class AgenciaController implements Serializable {

    @Autowired
    private AgenciaApi agenciaApi;

    @PostMapping("/findByIdWoobaAgencia/{idWoobaAgencia}")
    public com.confApi.db.confManager.agencia.dto.Agencia findByIdWoobaAgencia(@PathVariable Integer idWoobaAgencia) {
        return agenciaApi.findByIdWoobaAgencia(idWoobaAgencia);
    }
}
