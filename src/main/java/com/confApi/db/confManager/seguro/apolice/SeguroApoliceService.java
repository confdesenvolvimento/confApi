package com.confApi.db.confManager.seguro.apolice;

import com.confApi.endPoints.seguro.apolice.SeguroApoliceAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class SeguroApoliceService {

    @Autowired
    private SeguroApoliceAPI seguroApoliceAPI;

    public List<SeguroApolice> findAll(){
        return seguroApoliceAPI.findAll();
    }

    public Optional<SeguroApolice> findById(Integer id){
        return seguroApoliceAPI.findById(id);
    }

    public SeguroApolice save(SeguroApolice seguroApolice){
        return seguroApoliceAPI.save(seguroApolice);
    }

    public void deleteById(Integer id){
        seguroApoliceAPI.deleteById(id);
    }

}
