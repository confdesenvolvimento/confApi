package com.confApi.aereo.familiaCompanhia;

import com.confApi.aereo.AereoClientV2;
import com.confApi.aereo.dto.CompanhiaFamiliaModel;
import com.confApi.db.confManager.familia.dto.FamiliaCompanhia;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/familiaCompanhia")
public class FamiliaCompanhiaController {

    @Autowired
    private FamiliaCompanhiaService familiaCompanhiaService;

    @GetMapping(
            value = "/findByTipoRota/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public List<CompanhiaFamiliaModel> findAByTipoRota(@PathVariable Integer id){
        List<CompanhiaFamiliaModel> teste = familiaCompanhiaService.findByTipoRota(id);
        return teste;
    }
}
