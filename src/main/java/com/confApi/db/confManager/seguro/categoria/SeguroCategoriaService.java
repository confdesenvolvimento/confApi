package com.confApi.db.confManager.seguro.categoria;

import com.confApi.endPoints.seguro.categoria.SeguroCategoriaAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SeguroCategoriaService {

    @Autowired
    private SeguroCategoriaAPI seguroCategoriaAPI;

    public List<SeguroCategoria> findAll(){
        return seguroCategoriaAPI.findAll();
    }

    public Optional<SeguroCategoria> findById(Integer id){
        return seguroCategoriaAPI.findById(id);
    }

    public Optional<SeguroCategoria>findBySeguroCoberturaCodgSeguroCobertura(Integer codgSeguroCobertura){
        return seguroCategoriaAPI.findBySeguroCoberturaCodgSeguroCobertura(codgSeguroCobertura);
    }

    public SeguroCategoria save(SeguroCategoria seguroCategoria){
        return seguroCategoriaAPI.save(seguroCategoria);
    }

    public List<SeguroCategoria> saveAll(List<SeguroCategoria> seguroCategorias){
        return seguroCategoriaAPI.saveAll(seguroCategorias);
    }

    public void deleteById(Integer id){
        seguroCategoriaAPI.deleteById(id);
    }
}
