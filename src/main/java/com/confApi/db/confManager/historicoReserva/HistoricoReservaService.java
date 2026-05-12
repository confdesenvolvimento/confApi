package com.confApi.db.confManager.historicoReserva;

import com.confApi.db.confManager.historicoReserva.dto.HistoricoReserva;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HistoricoReservaService {

    @Autowired
    private  HistoricoReservaApi historicoReservaApi;

    public HistoricoReserva create(HistoricoReserva historicoReserva){
        return historicoReservaApi.create(historicoReserva);
    }

//    public void inserirLogHistoricoReservaAereo(Integer codgReservaAereo, String descricao, int flagInterno){
//        create(new HistoricoReserva());
//    }
//    public void inserirLogHistoricoReservaHotel(Integer codgReservaAereo, String descricao, int flagInterno){
//        create(new HistoricoReserva(0, codgReservaAereo, new Date(), descricao, NetUtil.ip(), flagInterno, new Usuario(Util.getUsuarioLogado().getCodgUsuario()), codgReservaHotel));
//    }
//    public void inserirLogHistoricoReservaSeguro(Integer codgReservaAereo, String descricao, int flagInterno){
//        historicoReservaApi.(codgReservaAereo,descricao, flagInterno);
//    }
//    public void inserirLogHistoricoReservaPacote(Integer codgReservaAereo, String descricao, int flagInterno){
//        historicoReservaApi.(codgReservaAereo,descricao, flagInterno);
//    }

    public List<HistoricoReserva> findByCodgReservaAereo(Integer codgReservaAereo){
        return historicoReservaApi.findByCodgReservaAereo(codgReservaAereo);
    }

    public List<HistoricoReserva> findByCodgReservaHotel(Integer codgReservaHotel){
        return historicoReservaApi.findByCodgReservaHotel(codgReservaHotel);
    }

    public List<HistoricoReserva> findByCodgReservaSeguro(Integer codgReservaSeguro){
        return historicoReservaApi.findByCodgReservaSeguro(codgReservaSeguro);
    }

    public List<HistoricoReserva> findByCodgReservaPacote(Integer codgReservaPacote){
        return historicoReservaApi.findByCodgReservaPacote(codgReservaPacote);
    }

}
