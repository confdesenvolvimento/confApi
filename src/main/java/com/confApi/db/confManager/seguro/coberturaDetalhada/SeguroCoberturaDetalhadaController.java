package com.confApi.db.confManager.seguro.coberturaDetalhada;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("manager/seguro/coberturaDetalhada")
public class SeguroCoberturaDetalhadaController {

    @Autowired
    private SeguroCoberturaDetalhadaService seguroCoberturaDetalhadaService;

    @GetMapping("/findAll")
    public List<SeguroCoberturaDetalhada> findAllSeguroCoberturaDetalhadas() throws IOException {
        return seguroCoberturaDetalhadaService.findAll();
    }

    @GetMapping("/findById/{id}")
    public Optional<SeguroCoberturaDetalhada> findSeguroCoberturaDetalhadaById(@PathVariable Integer id) throws IOException{
        return seguroCoberturaDetalhadaService.findById(id);
    }

    @PostMapping("/save")
    public SeguroCoberturaDetalhada createSeguroCoberturaDetalhada(@RequestBody SeguroCoberturaDetalhada seguroCoberturaDetalhada){
        return seguroCoberturaDetalhadaService.save(seguroCoberturaDetalhada);
    }

    @PostMapping("/saveAll")
    public List<SeguroCoberturaDetalhada> createSeguroCoberturaDetalhadas(@RequestBody List<SeguroCoberturaDetalhada> seguroCoberturaDetalhadas){
        return seguroCoberturaDetalhadaService.saveAll(seguroCoberturaDetalhadas);
    }

    @PutMapping("/updateById/{id}")
    public ResponseEntity<SeguroCoberturaDetalhada> updateSeguroCoberturaDetalhadaById(@RequestBody SeguroCoberturaDetalhada seguroCoberturaDetalhada,
                                                                     @PathVariable Integer id){

        if(!seguroCoberturaDetalhadaService.findById(id).isPresent()){
            return ResponseEntity.noContent().build();
        }

        seguroCoberturaDetalhada.setCodgSeguroCoberturaDetalhada(id);
        return ResponseEntity.ok(seguroCoberturaDetalhadaService.save(seguroCoberturaDetalhada));
    }

    @DeleteMapping("/deleteById/{id}")
    public ResponseEntity<Void> deleteSeguroCoberturaDetalhadaById(@PathVariable Integer id){

        if(!seguroCoberturaDetalhadaService.findById(id).isPresent()){
            return ResponseEntity.noContent().build();
        }
        seguroCoberturaDetalhadaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
