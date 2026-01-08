package com.confApi.db.confManager.seguro.reserva;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("manager/seguro/reserva")
public class SeguroReservaController {

    @Autowired
    private SeguroReservaService seguroReservaService;

    @GetMapping("/findAll")
    public List<SeguroReserva> findAllSeguroReservas() throws IOException {
        return seguroReservaService.findAll();
    }

    @GetMapping("/findById/{id}")
    public ResponseEntity<SeguroReserva> findSeguroReservaById(@PathVariable Integer id) throws IOException{
        Optional<SeguroReserva>seguroReserva = seguroReservaService.findById(id);
        return seguroReserva.map(ResponseEntity::ok).orElseGet(()->ResponseEntity.notFound().build());
    }
    
    @GetMapping("/findByLocalizador/{localizador}")
    public ResponseEntity<SeguroReserva> findByLocalizador(@PathVariable String localizador){
        Optional<SeguroReserva>seguroReserva = seguroReservaService.findByLocalizador(localizador);
        return seguroReserva.map(ResponseEntity::ok).orElseGet(()->ResponseEntity.notFound().build());
    }

    @PostMapping("/save")
    public SeguroReserva createSeguroReserva(@RequestBody SeguroReserva seguroReserva){
        return seguroReservaService.save(seguroReserva);
    }

    @PutMapping("/updateById/{id}")
    public ResponseEntity<SeguroReserva> updateSeguroReservaById(@RequestBody SeguroReserva seguroReserva,
                                                                 @PathVariable Integer id){

        if(!seguroReservaService.findById(id).isPresent()){
            return ResponseEntity.notFound().build();
        }

        seguroReserva.setCodgReservaSeguro(id);

        return ResponseEntity.ok(seguroReservaService.save(seguroReserva));
    }

    @DeleteMapping("/deleteById/{id}")
    public ResponseEntity<Void> deleteSeguroReservaById(@PathVariable Integer id){

        if(!seguroReservaService.findById(id).isPresent()){
            return ResponseEntity.notFound().build();
        }

        seguroReservaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
