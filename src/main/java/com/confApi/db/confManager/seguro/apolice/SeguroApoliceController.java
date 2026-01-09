package com.confApi.db.confManager.seguro.apolice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("manager/seguro/apolice")
public class SeguroApoliceController {

    @Autowired
    private SeguroApoliceService seguroApoliceService;

    @GetMapping("/findAll")
    public List<SeguroApolice> findAllSeguroApolices(){
        return seguroApoliceService.findAll();
    }

    @GetMapping("/findById/{id}")
    public ResponseEntity<SeguroApolice> findSeguroApoliceById(@PathVariable Integer id){
        Optional<SeguroApolice> seguroApolice = seguroApoliceService.findById(id);
        return seguroApolice.map(ResponseEntity::ok).orElseGet(()->ResponseEntity.notFound().build());
    }

    @GetMapping("/findByCodgSeguroSegurado/{codgSeguroSegurado}")
    public ResponseEntity<SeguroApolice>findByCodgSeguroSegurado(@PathVariable Integer codgSeguroSegurado){
        Optional<SeguroApolice>seguroApolice = seguroApoliceService.findBySeguroSeguradoCodgSeguroSegurado(codgSeguroSegurado);
        return seguroApolice.map(ResponseEntity::ok).orElseGet(()->ResponseEntity.notFound().build());
    }

    @GetMapping("/findByCodgSeguroCobertura/{codgSeguroCobertura}")
    public ResponseEntity<SeguroApolice>findByCodgSeguroCobertura(@PathVariable Integer codgSeguroCobertura){
        Optional<SeguroApolice>seguroApolice = seguroApoliceService.findBySeguroCoberturaCodgSeguroCobertura(codgSeguroCobertura);
        return seguroApolice.map(ResponseEntity::ok).orElseGet(()->ResponseEntity.notFound().build());
    }

    @PostMapping("/save")
    public SeguroApolice createSeguroApolice(@RequestBody SeguroApolice seguroApolice){
        return seguroApoliceService.save(seguroApolice);
    }

    @PutMapping("/updateById/{id}")
    public ResponseEntity<SeguroApolice> updateSeguroApoliceById(@RequestBody SeguroApolice seguroApolice,
                                                                   @PathVariable Integer id){

        if(!seguroApoliceService.findById(id).isPresent()){
            return ResponseEntity.noContent().build();
        }

        seguroApolice.setCodgSeguroApolice(id);
        return ResponseEntity.ok(seguroApoliceService.save(seguroApolice));
    }

    @DeleteMapping("/deleteById/{id}")
    public ResponseEntity<Void> deleteSeguroApoliceById(@PathVariable Integer id){

        if(!seguroApoliceService.findById(id).isPresent()){
            return ResponseEntity.noContent().build();
        }
        seguroApoliceService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
