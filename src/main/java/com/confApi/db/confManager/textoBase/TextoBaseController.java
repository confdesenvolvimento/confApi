package com.confApi.db.confManager.textoBase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("manager/textoBase")
public class TextoBaseController {

    @Autowired
    private TextoBaseService textoBaseService;
    @PostMapping("/save")
    public TextoBase createTextoBase(@RequestBody TextoBase textoBase){
        return textoBaseService.save(textoBase);
    }

    @GetMapping
    public List<TextoBase> findAllTextoBase(){
        return textoBaseService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TextoBase> findTextoBaseById(@PathVariable Integer id){
        Optional<TextoBase> textoBase = textoBaseService.findById(id);
        return textoBase.map(ResponseEntity::ok).orElseGet(()->ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<TextoBase> updateTextoBaseById(
            @RequestBody TextoBase textoBase,
            @PathVariable Integer id){
        if(!textoBaseService.findById(id).isPresent()){
            return ResponseEntity.noContent().build();
        }

        textoBase.setCodgTextoBase(id);
        return ResponseEntity.ok(textoBaseService.save(textoBase));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTextoBaseById(@PathVariable Integer id){

        if(!textoBaseService.findById(id).isPresent()){
            return ResponseEntity.noContent().build();
        }
        textoBaseService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
