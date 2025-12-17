package com.confApi.corporate.controller;

import com.confApi.chatgpt.dto.ChatRequestDTO;
import com.confApi.chatgpt.dto.ChatResponseDTO;
import com.confApi.corporate.dto.CompanhiaFamiliaDTO;
import com.confApi.corporate.dto.FamiliaDTO;
import com.confApi.corporate.mapper.FamiliaMapper;
import com.confApi.db.confManager.familia.FamiliaService;
import com.confApi.db.confManager.familia.dto.FamiliaCompanhia;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/services/external")
@RequiredArgsConstructor
public class CorporateController {


    @Autowired
    private FamiliaService familiaService;

    @GetMapping("/getFamiliaByNomecia")
    public List<CompanhiaFamiliaDTO> getFamiliaByNomecia(
            @RequestParam("parametro") String parametro) throws IOException {
        List<FamiliaCompanhia> lista = familiaService.findByNomeOuIataCia(parametro);
        return FamiliaMapper.toCompanhiaFamiliaDTOList(lista);
    }

    @GetMapping("/getAllFamilias")
    public List<CompanhiaFamiliaDTO> getAllFamilias() throws IOException {
        List<FamiliaCompanhia> lista = familiaService.findByAll();
        return FamiliaMapper.toCompanhiaFamiliaDTOList(lista);
    }
}
