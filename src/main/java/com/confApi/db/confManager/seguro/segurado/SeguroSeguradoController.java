package com.confApi.db.confManager.seguro.segurado;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("manager/seguro/segurado")
public class SeguroSeguradoController {

    @Autowired
    private SeguroSeguradoService seguroSeguradoService;

    @GetMapping("/findAll")
    public List<SeguroSegurado> findAllSeguroSeguradoDetalhadas() throws IOException {
        return seguroSeguradoService.findAll();
    }

    @GetMapping("/findById/{id}")
    public Optional<SeguroSegurado> findSeguroSeguradoById(@PathVariable Integer id) throws IOException{
        return seguroSeguradoService.findById(id);
    }

    @PostMapping("/save")
    public SeguroSegurado createSeguroSegurado(@RequestBody SeguroSegurado seguroSegurado){
        return seguroSeguradoService.save(seguroSegurado);
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
