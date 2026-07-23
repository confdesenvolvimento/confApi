package com.confApi.endPoints.reservaAereo;

import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.hub.aereo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReservaAereoService {

    @Autowired
    private ReservaAereoApi reservaAereoApi;

    public ReservaAereoResponse consultarLocalizador(ReservaAereoConsultarLocalizadorRequest obj) {
        ConsultarLocalizadorRequestHub consultarLocalizadorRequestHub = new ConsultarLocalizadorRequestHub(obj);
        ConsultarLocalizadorResponseHub pesquisaResponseHubList = reservaAereoApi.reservaAereoConsultaLocalizadorHub(consultarLocalizadorRequestHub);
        ReservaAereo pesquisaResponseDb = reservaAereoApi.reservaAereoConsultaLocalizadorDb(obj.getLocalizador());
        ReservaAereoResponse reservaAereoResponse = new ReservaAereoResponse(pesquisaResponseHubList, pesquisaResponseDb);

        return reservaAereoResponse;
    }
}
