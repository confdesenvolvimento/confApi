package com.confApi.db.confManager.seguro.reserva;

import com.confApi.db.confManager.seguro.reserva.DTO.CancelamentoRequestDTO;
import com.confApi.endPoints.seguro.reserva.SeguroReservaAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SeguroReservaService {


    @Autowired
    private SeguroReservaAPI seguroReservaAPI;

    public List<SeguroReserva> findAll(){
        return seguroReservaAPI.findAll();
    }

    public Optional<SeguroReserva> findById(Integer id){
        return seguroReservaAPI.findById(id);
    }

    public Optional<SeguroReserva> findByLocalizador(String localizador){
        return seguroReservaAPI.findByLocalizador(localizador);
    }

    public SeguroReserva save(SeguroReserva seguroReserva){
        return seguroReservaAPI.save(seguroReserva);
    }

    public void deleteById(Integer id){
        seguroReservaAPI.deleteById(id);
    }

    public void cancelarReservaSeguro(CancelamentoRequestDTO dto) {
        seguroReservaAPI.cancelarReserva(dto, dto.getCodgReserva());
    }

}
