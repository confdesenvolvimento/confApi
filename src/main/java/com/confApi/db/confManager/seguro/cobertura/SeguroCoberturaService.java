package com.confApi.db.confManager.seguro.cobertura;

import com.confApi.endPoints.seguro.cobertura.SeguroCoberturaAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SeguroCoberturaService {

    @Autowired
    private SeguroCoberturaAPI seguroCoberturaAPI;

    public List<SeguroCobertura> findAll(){
        return seguroCoberturaAPI.findAll();
    }

    public Optional<SeguroCobertura> findById(Integer id){
        return seguroCoberturaAPI.findById(id);
    }

    public Optional<SeguroCobertura>findBySeguroReservaCodgReservaSeguro(Integer codgReservaSeguro){
        return seguroCoberturaAPI.findBySeguroReservaCodgReservaSeguro(codgReservaSeguro);
    }

    public SeguroCobertura save(SeguroCobertura seguroCobertura){
        return seguroCoberturaAPI.save(seguroCobertura);
    }

    public void deleteById(Integer id){
        seguroCoberturaAPI.deleteById(id);
    }
}
