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
        System.out.println("consultarLocalizadorRequestHub: " + consultarLocalizadorRequestHub);

        ConsultarLocalizadorResponseHub pesquisaResponseHubList = reservaAereoApi.reservaAereoConsultaLocalizadorHub(consultarLocalizadorRequestHub);
        System.out.println("pesquisaResponseHubList: " + pesquisaResponseHubList);

        ReservaAereo pesquisaResponseDb = reservaAereoApi.reservaAereoConsultaLocalizadorDb(obj.getLocalizador());

        ReservaAereoResponse reservaAereoResponse = new ReservaAereoResponse(pesquisaResponseHubList, pesquisaResponseDb);

        System.out.println(reservaAereoResponse.toString());
        System.out.println(reservaAereoResponse.getPassageiros());
        System.out.println(reservaAereoResponse.getTrechos());

        return reservaAereoResponse;
    }
}
