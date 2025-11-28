package com.confApi.hub.aereo;

import com.confApi.endPoints.contato.ContatoResponse;
import com.confApi.hub.aereo.dto.Contato;

public class ContatoModel extends Contato {
    public ContatoModel() {
    }

    public ContatoModel(ContatoResponse contato) {
        super(contato);
    }

}

