package com.confApi.db.confManager.seguro.coberturaDetalhada;

import com.confApi.endPoints.seguro.coberturaDetalhada.SeguroCoberturaDetalhadaAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SeguroCoberturaDetalhadaService {

    @Autowired
    private SeguroCoberturaDetalhadaAPI seguroCoberturaDetalhadaAPI;

    public List<SeguroCoberturaDetalhada> findAll(){
        return seguroCoberturaDetalhadaAPI.findAll();
    }

    public Optional<SeguroCoberturaDetalhada> findById(Integer id){
        return seguroCoberturaDetalhadaAPI.findById(id);
    }

    public List<SeguroCoberturaDetalhada>findBySeguroCoberturaCodgSeguroCobertura(Integer codgSeguroCobertura){
        return seguroCoberturaDetalhadaAPI.findBySeguroCoberturaCodgSeguroCobertura(codgSeguroCobertura);
    }

    public SeguroCoberturaDetalhada save(SeguroCoberturaDetalhada seguroCoberturaDetalhada){
        return seguroCoberturaDetalhadaAPI.save(seguroCoberturaDetalhada);
    }

    public List<SeguroCoberturaDetalhada> saveAll(List<SeguroCoberturaDetalhada> seguroCoberturaDetalhadas){
        return seguroCoberturaDetalhadaAPI.saveAll(seguroCoberturaDetalhadas);
    }

    public void deleteById(Integer id){
        seguroCoberturaDetalhadaAPI.deleteById(id);
    }
}
