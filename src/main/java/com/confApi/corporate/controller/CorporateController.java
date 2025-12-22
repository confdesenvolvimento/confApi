package com.confApi.corporate.controller;

import com.confApi.corporate.dto.CompanhiaFamiliaDTO;
import com.confApi.corporate.dto.AeroportoDTO.AeroportoDTO;
import com.confApi.corporate.mapper.AeroportoMapper;
import com.confApi.corporate.mapper.FamiliaMapper;
import com.confApi.db.confManager.aeroporto.Aeroporto;
import com.confApi.db.confManager.aeroporto.AeroportoService;
import com.confApi.db.confManager.aeroporto.DTO.AeroportoParamRq;
import com.confApi.db.confManager.familia.FamiliaService;
import com.confApi.db.confManager.familia.dto.FamiliaCompanhia;
import com.confApi.db.confManager.usuario.dto.AuthRequestDto;
import com.confApi.endPoints.usuario.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/services/external")
public class CorporateController {


    @Autowired
    private FamiliaService familiaService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AeroportoService aeroportoService;

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

    @PostMapping("/getUserUsuario")
    public ResponseEntity<Object> autenficarUsuarioExterno(@RequestBody AuthRequestDto requestDto) throws IOException {
        if (requestDto.getLoginUsuario() != null && requestDto.getLoginUsuario().length() > 20) {
            return ResponseEntity
                    .badRequest()
                    .body("Login é muito grande. Máximo permitido: 20 caracteres.");
        }
        return ResponseEntity.ok()
                .body(usuarioService
                        .autenficarUsuarioExterno(requestDto.getLoginUsuario())
                        .getBody());
    }

    @GetMapping("/iataParametros")
    public List<AeroportoDTO> getAeroportoByParametros(AeroportoParamRq paramRq) throws IOException {
        List<Aeroporto> lista = aeroportoService.findAeroportoByParametros(paramRq);
        return AeroportoMapper.toAeroportoDTOList(lista);
    }

    @GetMapping("/iataPais/{iataPais}")
    public List<AeroportoDTO> getAeroportoByIataPais(@PathVariable String iataPais) throws IOException {
        List<Aeroporto> lista = aeroportoService.findAeroportoByIataPais(iataPais);
        return AeroportoMapper.toAeroportoDTOList(lista);
    }

    @GetMapping("/iata/{iata}")
    public AeroportoDTO getAeroportoByIata(@PathVariable String iata) throws IOException {
        Aeroporto aeroporto = aeroportoService.findAeroportoByIata(iata);
        return AeroportoMapper.toAeroportoDTO(aeroporto);
    }
}
