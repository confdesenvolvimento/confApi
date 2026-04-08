package com.confApi.endPoints.novidade;

import com.confApi.db.confManager.novidade.Novidade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/v1/novidade")
public class NovidadeController {

    @Autowired
    private NovidadeService novidadeService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Novidade> insertNotificacaoControle(
            @RequestPart("novidade") Novidade obj,
            @RequestPart(value = "arquivo", required = false) MultipartFile arquivo) throws Exception {

        System.out.println("NovidadeController.insertNotificacaoControle: " + obj);

        Novidade novidade = novidadeService.insert(obj, arquivo);
        return ResponseEntity.ok().body(novidade);
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<Novidade> update(@RequestBody @Valid Novidade obj, @PathVariable Integer id) throws Exception {
        return ResponseEntity.ok().body(novidadeService.update(obj, id));
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<Novidade> fingById(@PathVariable Integer id) throws Exception {
        return ResponseEntity.ok().body(novidadeService.consultarNovidade(id));
    }

    @GetMapping
    public ResponseEntity<List<Novidade>> findAll() throws Exception {
        System.out.println("findAll");
        return ResponseEntity.ok().body(novidadeService.consultarNovidadeGeral());
    }
}

