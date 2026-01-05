package com.confApi.db.confManager.seguro.reserva;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
