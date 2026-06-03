package com.confApi.endPoints.clube.arquivoAnexo;

import com.confApi.db.clube.arquivoAnexo.ArquivoAnexo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clube/arquivo")
public class ArquivoAnexoController {

    @Autowired
    private ArquivoAnexoApi arquivoAnexoApi;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ArquivoAnexo arquivoAnexo) {
        return ResponseEntity.ok(arquivoAnexoApi.create(arquivoAnexo));
    }

    @GetMapping
    public List<ArquivoAnexo> getAll() {
        return arquivoAnexoApi.getAll();
    }

    @GetMapping("/filter")
    public List<ArquivoAnexo> findAll(@RequestBody ArquivoAnexo arquivoAnexo) {
        return arquivoAnexoApi.findAll(arquivoAnexo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody ArquivoAnexo arquivoAnexo) {
        return ResponseEntity.ok(arquivoAnexoApi.update(id, arquivoAnexo));
    }
}
