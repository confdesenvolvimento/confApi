package com.confApi.hub.aereo;


import com.confApi.db.confManager.bandeira.Bandeira;
import com.confApi.endPoints.bandeira.BandeiraResponse;

public class BandeiraModel extends Bandeira {

    public BandeiraModel(Integer codgBandeira, String nomeBandeira, String siglaBandeira) {
        super(codgBandeira, nomeBandeira, siglaBandeira);
    }

    public BandeiraModel(Integer codgBandeira) {
        super(codgBandeira);
    }

    public BandeiraModel(BandeiraResponse bandeiraResponse) {
        super(bandeiraResponse.getCodgBandeira(), bandeiraResponse.getNomeBandeira(), bandeiraResponse.getSiglaBandeira());
    }
}