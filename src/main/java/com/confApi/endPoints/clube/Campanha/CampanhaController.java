package com.confApi.endPoints.clube.Campanha;

import com.confApi.db.clube.campanha.Campanha;
import com.confApi.db.clube.campanha.CampanhaService;
import com.confApi.db.clube.campanha.dto.CampanhasAgrupadasDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clube/campanha")
public class CampanhaController {

    @Autowired
    private CampanhaService campanhaService;


    @GetMapping
    public List<Campanha> getAll() {
        return campanhaService.getAll();
    }

    @GetMapping("/ativas")
    public List<Campanha> getAllAtivas() {
        return campanhaService.getAllAtivas();
    }

    @GetMapping("/ativas-status-1")
    public List<Campanha> getAllAtivasStatus1() {
        return campanhaService.getAllAtivasStatus1();
    }
/*
    @GetMapping("/campanhas-ativas-hoje")
    public List<Campanha> getCampanhasAtivasHoje() {
        return campanhaService.getCampanhasAtivasHoje();
    }*/

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(campanhaService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody Campanha campanha) {
        return ResponseEntity.ok(campanhaService.update(id, campanha));
    }

    @GetMapping("/campanhas-agrupadas")
    public CampanhasAgrupadasDTO getCampanhasAgrupadas(@RequestParam String loginUsuario) {
        return campanhaService.getCampanhasAgrupadas(loginUsuario);
    }

}
