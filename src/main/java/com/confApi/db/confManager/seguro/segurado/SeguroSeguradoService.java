package com.confApi.db.confManager.seguro.segurado;

import com.confApi.endPoints.seguro.segurado.SeguroSeguradoAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SeguroSeguradoService {

    @Autowired
    private SeguroSeguradoAPI seguroSeguradoAPI;

    public List<SeguroSegurado> findAll(){
        return seguroSeguradoAPI.findAll();
    }

    public Optional<SeguroSegurado> findById(Integer id){
        return seguroSeguradoAPI.findById(id);
    }

    public SeguroSegurado save(SeguroSegurado seguroSegurado){
        return seguroSeguradoAPI.save(seguroSegurado);
    }

    public List<SeguroSegurado> saveAll(List<SeguroSegurado> seguroSegurados){
        return seguroSeguradoAPI.saveAll(seguroSegurados);
    }

    public void deleteById(Integer id){
        seguroSeguradoAPI.deleteById(id);
    }
}
