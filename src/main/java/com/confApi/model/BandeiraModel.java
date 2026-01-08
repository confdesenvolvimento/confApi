package com.confApi.model;

import com.confApi.db.confManager.bandeira.Bandeira;
import lombok.Data;

@Data
public class BandeiraModel extends Bandeira {

    public BandeiraModel(Integer codgBandeira, String nomeBandeira, String siglaBandeira) {
        super(codgBandeira, nomeBandeira, siglaBandeira);
    }

    public BandeiraModel() {
    }
}
