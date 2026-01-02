package com.confApi.db.confManager.sistema.model;

import com.confApi.db.confManager.sistema.Sistema;

import java.io.Serializable;

public class SistemaModel extends Sistema implements Serializable {

    private Boolean isSelecionado = true;



    public Boolean getIsSelecionado() {
        return isSelecionado;
    }

    public void setIsSelecionado(Boolean isSelecionado) {
        this.isSelecionado = isSelecionado;
    }

}
