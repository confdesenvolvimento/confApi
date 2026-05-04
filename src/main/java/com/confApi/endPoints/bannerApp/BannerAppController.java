package com.confApi.endPoints.bannerApp;

import com.confApi.db.confManager.bannerApp.BannerApp;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/v1/bannerapp")
public class BannerAppController {

    @Autowired
    private BannerAppService bannerAppService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BannerApp> insertBannerAppControle(
            @RequestPart("bannerApp") BannerApp obj,
            @RequestPart(value = "arquivo", required = false) MultipartFile arquivo) throws Exception {

        BannerApp bannerApp = bannerAppService.insert(obj, arquivo);
        return ResponseEntity.ok().body(bannerApp);
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<BannerApp> update(@RequestBody @Valid BannerApp obj, @PathVariable Integer id) throws Exception {
        return ResponseEntity.ok().body(bannerAppService.update(obj, id));
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<BannerApp> fingById(@PathVariable Integer id) throws Exception {
        return ResponseEntity.ok().body(bannerAppService.consultarBannerApp(id));
    }

    @GetMapping
    public ResponseEntity<List<BannerApp>> findAll() throws Exception {
        return ResponseEntity.ok().body(bannerAppService.consultarBannerAppGeral());
    }

    @GetMapping(value = "/ativos")
    public ResponseEntity<List<BannerApp>> findAllAtivos() throws Exception {
        return ResponseEntity.ok().body(bannerAppService.consultarBannerAppAtivos());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) throws JsonProcessingException {
        String erro = bannerAppService.delete(id);
        if (erro != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
        }
        return ResponseEntity.noContent().build();
    }
}

