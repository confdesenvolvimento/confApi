package com.confApi.db.confManager.seguro.cobertura;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("manager/seguro/cobertura")
public class SeguroCoberturaController {

    @Autowired
    private SeguroCoberturaService seguroCoberturaService;

    @GetMapping("/findAll")
    public List<SeguroCobertura> findAllSeguroCoberturas (){
        return seguroCoberturaService.findAll();
    }

    @GetMapping("/findById/{id}")
    public ResponseEntity<SeguroCobertura>findCoberturaById(@PathVariable Integer id){
        Optional<SeguroCobertura> cobertura = seguroCoberturaService.findById(id);
        return cobertura.map(ResponseEntity::ok).orElseGet(()->ResponseEntity.notFound().build());
    }

    @GetMapping("/findByCodgReservaSeguro/{codgReservaSeguro}")
    public ResponseEntity<SeguroCobertura>findByCodgReservaSeguro(@PathVariable Integer codgReservaSeguro){
        Optional<SeguroCobertura> cobertura = seguroCoberturaService.findBySeguroReservaCodgReservaSeguro(codgReservaSeguro);
        return cobertura.map(ResponseEntity::ok).orElseGet(()->ResponseEntity.notFound().build());
    }

    @PostMapping("/save")
    public SeguroCobertura createSeguroCobertura(@RequestBody SeguroCobertura seguroCobertura){
        return seguroCoberturaService.save(seguroCobertura);
    }

    @PutMapping("/updateById/{id}")
    public ResponseEntity<SeguroCobertura> updateSeguroCoberturaById(@RequestBody SeguroCobertura seguroCobertura,
                                                                 @PathVariable Integer id){

        if(!seguroCoberturaService.findById(id).isPresent()){
            return ResponseEntity.noContent().build();
        }

        seguroCobertura.setCodgSeguroCobertura(id);
        return ResponseEntity.ok(seguroCoberturaService.save(seguroCobertura));
    }

    @DeleteMapping("/deleteById/{id}")
    public ResponseEntity<Void> deleteSeguroCoberturaById(@PathVariable Integer id){

        if(!seguroCoberturaService.findById(id).isPresent()){
            return ResponseEntity.noContent().build();
        }
        seguroCoberturaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
