package com.confApi.hub.limites.dto;

public class LimiteCreditoRQ {
    private String idErp;

    public LimiteCreditoRQ() {
    }

    public LimiteCreditoRQ(String idErp) {
        this.idErp = idErp;
    }

    /**
     * @return the idErp
     */
    public String getIdErp() {
        return idErp;
    }

    /**
     * @param idErp the idErp to set
     */
    public void setIdErp(String idErp) {
        this.idErp = idErp;
    }
}

