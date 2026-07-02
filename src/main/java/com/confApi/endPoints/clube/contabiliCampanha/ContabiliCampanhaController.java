package com.confApi.endPoints.clube.contabiliCampanha;

import com.confApi.db.clube.contabiliCampanha.ContabiliCampanha;
import com.confApi.db.clube.contabiliCampanha.ContabiliCampanhaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clube/contabilCampanha")
public class ContabiliCampanhaController {

    @Autowired
    private ContabiliCampanhaService contabiliCampanhaService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(contabiliCampanhaService.getAll());
    }

    @GetMapping("/params")
    public List<ContabiliCampanha> findAll(@RequestBody ContabiliCampanha contabiliCampanha) {
        return contabiliCampanhaService.findAll(contabiliCampanha);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ContabiliCampanha contabiliCampanha) {
        return ResponseEntity.ok(contabiliCampanhaService.create(contabiliCampanha));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody ContabiliCampanha contabiliCampanha) {
        return ResponseEntity.ok(contabiliCampanhaService.update(id, contabiliCampanha));
    }

    @GetMapping("/ranking")
    public List<ContabiliCampanha> getRanking(@RequestParam Integer codgCampanha) {
        System.out.println("ranking "+codgCampanha);
        System.out.println("r : "+contabiliCampanhaService.getRanking(codgCampanha));
        return contabiliCampanhaService.getRanking(codgCampanha);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        return ResponseEntity.ok(contabiliCampanhaService.delete(id));
    }

    @DeleteMapping("/deleteIdCampanhaAll/{id}")
    public ResponseEntity<?> deleteIdCampanhaAll(@PathVariable Integer id) {
        return ResponseEntity.ok(contabiliCampanhaService.deleteIdCampanhaAll(id));
    }

    @GetMapping("/relatorio/{campanhaId}")
    public List<?> relatorio(@PathVariable Integer campanhaId) {
        return contabiliCampanhaService.relatorio(campanhaId);
    }
}
