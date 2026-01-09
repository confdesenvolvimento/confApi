package com.confApi.db.confManager.seguro.categoria;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("manager/seguro/categoria")
public class SeguroCategoriaController {

    @Autowired
    private SeguroCategoriaService seguroCategoriaService;

    @GetMapping("/findAll")
    public List<SeguroCategoria> findAllSeguroCoberturas(){
        return seguroCategoriaService.findAll();
    }

    @GetMapping("/findById/{id}")
    public ResponseEntity<SeguroCategoria> findSeguroCategoriaById(@PathVariable Integer id){
        Optional<SeguroCategoria>seguroCategoria = seguroCategoriaService.findById(id);
        return seguroCategoria.map(ResponseEntity::ok).orElseGet(()->ResponseEntity.notFound().build());
    }

    @GetMapping("/findByCodgSeguroCobertura/{codgSeguroCobertura}")
    public ResponseEntity<SeguroCategoria>findByCodgSeguroCobertura(@PathVariable Integer codgSeguroCobertura){
        Optional<SeguroCategoria>seguroCategoria = seguroCategoriaService.findBySeguroCoberturaCodgSeguroCobertura(codgSeguroCobertura);
        return seguroCategoria.map(ResponseEntity::ok).orElseGet(()->ResponseEntity.notFound().build());
    }

    @PostMapping("/save")
    public SeguroCategoria createSeguroCategoria(@RequestBody SeguroCategoria seguroCategoria){
        return seguroCategoriaService.save(seguroCategoria);
    }

    @PostMapping("/saveAll")
    public List<SeguroCategoria> seguroCategorias(@RequestBody List<SeguroCategoria> seguroCategorias){
        return seguroCategoriaService.saveAll(seguroCategorias);
    }

    @PutMapping("/updateById/{id}")
    public ResponseEntity<SeguroCategoria> updateSeguroCategoriaById(@RequestBody SeguroCategoria seguroCategoria,
                                                                     @PathVariable Integer id){

        if(!seguroCategoriaService.findById(id).isPresent()){
            return ResponseEntity.noContent().build();
        }

        seguroCategoria.setCodgSeguroCategoria(id);
        return ResponseEntity.ok(seguroCategoriaService.save(seguroCategoria));
    }

    @DeleteMapping("/deleteById/{id}")
    public ResponseEntity<Void> deleteSeguroCategoriaById(@PathVariable Integer id){

        if(!seguroCategoriaService.findById(id).isPresent()){
            return ResponseEntity.noContent().build();
        }
        seguroCategoriaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
