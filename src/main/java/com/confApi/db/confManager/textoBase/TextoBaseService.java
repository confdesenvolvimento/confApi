package com.confApi.db.confManager.textoBase;

import com.confApi.endPoints.textoBase.TextoBaseAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TextoBaseService {

    @Autowired
    private TextoBaseAPI textoBaseAPI;

    public List<TextoBase> findAll(){
        return textoBaseAPI.findAll();
    }

    public Optional<TextoBase> findById(Integer id){
        return textoBaseAPI.findById(id);
    }

    public TextoBase save(TextoBase seguroSegurado){
        return textoBaseAPI.save(seguroSegurado);
    }

    public void deleteById(Integer id){
        textoBaseAPI.deleteById(id);
    }
}
