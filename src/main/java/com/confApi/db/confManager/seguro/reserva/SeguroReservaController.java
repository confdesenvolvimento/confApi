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
    public List<SeguroReserva> findAllSeguroReservas () throws IOException {
        return seguroReservaService.findAllSeguroReservas();
    }

    @GetMapping("/findById/{id}")
    public Optional<SeguroReserva>findSeguroReservaById(@PathVariable Integer id) throws IOException{
        return seguroReservaService.findSeguroReservaById(id);
    }

    @PostMapping("/save")
    public SeguroReserva createSeguroReserva(@RequestBody SeguroReserva seguroReserva){
        return seguroReservaService.save(seguroReserva);
    }

    @PutMapping("/updateById/{id}")
    public ResponseEntity<SeguroReserva> updateSeguroReservaById(@RequestBody SeguroReserva seguroReserva,
                                                                 @PathVariable Integer id){

        if(!seguroReservaService.findSeguroReservaById(id).isPresent()){
            return ResponseEntity.notFound().build();
        }

        seguroReserva.setCodgReservaSeguro(id);

        return ResponseEntity.ok(seguroReservaService.save(seguroReserva));
    }

    @DeleteMapping("/deleteById/{id}")
    public ResponseEntity<Void>deleteSeguroReservaById(@PathVariable Integer id){

        if(!seguroReservaService.findSeguroReservaById(id).isPresent()){
            return ResponseEntity.notFound().build();
        }

        seguroReservaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
