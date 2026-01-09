package com.confApi.db.confManager.seguro.segurado;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("manager/seguro/segurado")
public class SeguroSeguradoController {

    @Autowired
    private SeguroSeguradoService seguroSeguradoService;
    @PostMapping("/save")
    public SeguroSegurado createSeguroSegurado(@RequestBody SeguroSegurado seguroSegurado){
        return seguroSeguradoService.save(seguroSegurado);
    }
    @GetMapping("/findAll")
    public List<SeguroSegurado> findAllSeguroSeguradoDetalhadas(){
        return seguroSeguradoService.findAll();
    }

    @GetMapping("/findById/{id}")
    public ResponseEntity<SeguroSegurado> findSeguradoById(@PathVariable Integer id){
        Optional<SeguroSegurado> segurado = seguroSeguradoService.findById(id);
        return segurado.map(ResponseEntity::ok).orElseGet(()->ResponseEntity.notFound().build());
    }

    @GetMapping("/findByCodgSeguroCobertura/{codgSeguroCobertura}")
    public ResponseEntity<SeguroSegurado>findByCodgSeguroCobertura(@PathVariable Integer codgSeguroCobertura){
        Optional<SeguroSegurado>seguroSegurado = seguroSeguradoService.findBySeguroSeguradoCodgSeguroCobertura(codgSeguroCobertura);
        return seguroSegurado.map(ResponseEntity::ok).orElseGet(()->ResponseEntity.notFound().build());
    }

    @PostMapping("/saveAll")
    public List<SeguroSegurado> createSeguroSegurados(@RequestBody List<SeguroSegurado> seguroSegurados){
        return seguroSeguradoService.saveAll(seguroSegurados);
    }

    @PutMapping("/updateById/{id}")
    public ResponseEntity<SeguroSegurado> updateSeguroSeguradoById(@RequestBody SeguroSegurado seguroSegurado,
                                                                                       @PathVariable Integer id){

        if(!seguroSeguradoService.findById(id).isPresent()){
            return ResponseEntity.noContent().build();
        }

        seguroSegurado.setCodgSeguroSegurado(id);
        return ResponseEntity.ok(seguroSeguradoService.save(seguroSegurado));
    }

    @DeleteMapping("/deleteById/{id}")
    public ResponseEntity<Void> deleteSeguroSeguradoById(@PathVariable Integer id){

        if(!seguroSeguradoService.findById(id).isPresent()){
            return ResponseEntity.noContent().build();
        }
        seguroSeguradoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
