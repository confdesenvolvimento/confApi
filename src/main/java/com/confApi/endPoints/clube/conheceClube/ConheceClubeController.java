package com.confApi.endPoints.clube.conheceClube;

import com.confApi.db.clube.conhecaClube.ConhecaClube;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clube/conhecaClube")
public class ConheceClubeController {

    @Autowired
    private ConheceClubeApi conheceClubeApi;

    @GetMapping("/consulta/{id}")
    public ResponseEntity<?> consulta(@PathVariable Integer id) {
        return ResponseEntity.ok(conheceClubeApi.consulta(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody ConhecaClube conhecaClube) {
        return ResponseEntity.ok(conheceClubeApi.update(id, conhecaClube));
    }
}
