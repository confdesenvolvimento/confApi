package com.confApi.db.confManager.seguro.apolice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("manager/seguro/apolice")
public class SeguroApoliceController {

    @Autowired
    private SeguroApoliceService seguroApoliceService;

    @GetMapping("/findAll")
    public List<SeguroApolice> findAllSeguroApolices() throws IOException {
        return seguroApoliceService.findAll();
    }

    @GetMapping("/findById/{id}")
    public Optional<SeguroApolice> findSeguroApoliceById(@PathVariable Integer id) throws IOException{
        return seguroApoliceService.findById(id);
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
