package com.confApi.db.confManager.seguro.coberturaDetalhada;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("manager/seguro/coberturaDetalhada")
public class SeguroCoberturaDetalhadaController {

    @Autowired
    private SeguroCoberturaDetalhadaService seguroCoberturaDetalhadaService;

    @GetMapping("/findAll")
    public List<SeguroCoberturaDetalhada> findAllSeguroCoberturaDetalhadas(){
        return seguroCoberturaDetalhadaService.findAll();
    }

    @GetMapping("/findById/{id}")
    public ResponseEntity<SeguroCoberturaDetalhada> findSeguroCoberturaDetalhadaById(@PathVariable Integer id){
        Optional<SeguroCoberturaDetalhada>seguroCoberturaDetalhada = seguroCoberturaDetalhadaService.findById(id);
        return seguroCoberturaDetalhada.map(ResponseEntity::ok).orElseGet(()->ResponseEntity.notFound().build());
    }

    @GetMapping("/findByCodgSeguroCobertura/{codgSeguroCobertura}")
    public ResponseEntity<SeguroCoberturaDetalhada>findByCodgSeguroCobertura(@PathVariable Integer codgSeguroCobertura){
        Optional<SeguroCoberturaDetalhada>seguroCoberturaDetalhada = seguroCoberturaDetalhadaService.findBySeguroCoberturaCodgSeguroCobertura(codgSeguroCobertura);
        return seguroCoberturaDetalhada.map(ResponseEntity::ok).orElseGet(()->ResponseEntity.notFound().build());
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
