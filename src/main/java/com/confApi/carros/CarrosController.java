package com.confApi.carros;

import com.confApi.carros.dto.PesquisaRequestCarroDTO;
import com.confApi.carros.dto.PesquisaResponseCarroDTO;
import com.confApi.hub.carro.HubCarroClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/carro")
public class CarrosController {

    private final CarrosService service;

    @Autowired
    private HubCarroClient hubCarroClient;

    public CarrosController(CarrosService service) {
        this.service = service;
    }

    @PostMapping("/pesquisar")
    public List<PesquisaResponseCarroDTO> pesquisar(@RequestBody PesquisaRequestCarroDTO req) {
//        List<PlanoSeguroDTO> resultado = mockPlanosSeguro();
        List<PesquisaResponseCarroDTO> resultado = hubCarroClient.pesquisarDisponibilidade(req);
        return resultado;
    }

}
