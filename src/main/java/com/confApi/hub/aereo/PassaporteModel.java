package com.confApi.hub.aereo;

import com.confApi.endPoints.passaporte.PassaporteResponse;
import com.confApi.hub.aereo.dto.Passaporte;

public class PassaporteModel extends Passaporte {

    public PassaporteModel(String numero) {
        super(numero);
    }

    public PassaporteModel() {
    }

    public PassaporteModel(PassaporteResponse passaporteResponse) {
        super(passaporteResponse);
    }
}

