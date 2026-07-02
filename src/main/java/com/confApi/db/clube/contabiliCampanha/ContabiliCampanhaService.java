package com.confApi.db.clube.contabiliCampanha;

import com.confApi.endPoints.clube.contabiliCampanha.ContabiliCampanhaApi;
import com.confApi.endPoints.clube.message.ResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContabiliCampanhaService {

    @Autowired
    private ContabiliCampanhaApi contabiliCampanhaApi;


    public List<ContabiliCampanha> getAll() {
        return contabiliCampanhaApi.getAll();
    }

    public List<ContabiliCampanha> findAll(ContabiliCampanha contabiliCampanha) {
        return contabiliCampanhaApi.findAll(contabiliCampanha);
    }

    public ContabiliCampanha create(ContabiliCampanha contabiliCampanha) {
        return contabiliCampanhaApi.create(contabiliCampanha);
    }

    public ContabiliCampanha update(Integer id, ContabiliCampanha contabiliCampanha) {
        return contabiliCampanhaApi.update(id, contabiliCampanha);
    }

    public List<ContabiliCampanha> getRanking(Integer codgCampanha) {
        return contabiliCampanhaApi.getRanking(codgCampanha);
    }

    public ResponseMessage delete(Integer id) {
        return contabiliCampanhaApi.delete(id);
    }

    public ResponseMessage deleteIdCampanhaAll(Integer id) {
        return contabiliCampanhaApi.deleteIdCampanhaAll(id);
    }

    public List<?> relatorio(Integer campanhaId) {
        return contabiliCampanhaApi.relatorio(campanhaId);
    }
}
