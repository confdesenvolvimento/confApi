package com.confApi.seguros;

import com.confApi.db.confManager.seguro.categoria.SeguroCategoriaService;
import com.confApi.db.confManager.seguro.cobertura.SeguroCobertura;
import com.confApi.db.confManager.seguro.cobertura.SeguroCoberturaService;
import com.confApi.db.confManager.seguro.coberturaDetalhada.SeguroCoberturaDetalhada;
import com.confApi.db.confManager.seguro.coberturaDetalhada.SeguroCoberturaDetalhadaService;
import com.confApi.db.confManager.seguro.reserva.SeguroReserva;
import com.confApi.db.confManager.seguro.reserva.SeguroReservaService;
import com.confApi.db.confManager.seguro.segurado.SeguroSegurado;
import com.confApi.db.confManager.seguro.segurado.SeguroSeguradoService;
import com.confApi.seguros.dto.SeguradoDTO;
import com.confApi.seguros.dto.SeguroCompraModel;
import com.confApi.seguros.mapper.SeguroCompraToManagerMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SegurosService {
    private final SegurosClient segurosClient;
    private final SeguroReservaService seguroReservaService;
    private final SeguroCoberturaService seguroCoberturaService;
    private final SeguroSeguradoService seguroSeguradoService;
    private final SeguroCategoriaService seguroCategoriaService;
    private final SeguroCoberturaDetalhadaService seguroCoberturaDetalhadaService;
    public SegurosService(SegurosClient segurosClient, SeguroReservaService seguroReservaService,
                          SeguroCoberturaService seguroCoberturaService,
                          SeguroSeguradoService seguroSeguradoService, SeguroCategoriaService seguroCategoriaService, SeguroCoberturaDetalhadaService seguroCoberturaDetalhadaService) {
        this.segurosClient = segurosClient;
        this.seguroReservaService = seguroReservaService;
        this.seguroCoberturaService = seguroCoberturaService;
        this.seguroSeguradoService = seguroSeguradoService;
        this.seguroCategoriaService = seguroCategoriaService;
        this.seguroCoberturaDetalhadaService = seguroCoberturaDetalhadaService;
    }

    public void pesquisar(SeguroCompraModel req) {

    }

    public void comprar(SeguroCompraModel req) {
        SeguroReserva reserva = SeguroCompraToManagerMapper.toReserva(req);
        SeguroReserva resp = seguroReservaService.save(reserva);

        SeguroCobertura seguroCoberturaResp = seguroCoberturaService.save(SeguroCompraToManagerMapper.toCobertura(req, resp));

        List<SeguroCoberturaDetalhada>  coberturaDetalhadas=  SeguroCompraToManagerMapper.toCoberturaDetalhada(req,seguroCoberturaResp);
        for (SeguroCoberturaDetalhada s : coberturaDetalhadas) {
            seguroCoberturaDetalhadaService.save(s);
        }

        List<SeguroSegurado> segurados = SeguroCompraToManagerMapper.toSegurados(req, seguroCoberturaResp);
        List<SeguroSegurado> seguradosSalvas = new ArrayList<>();
        for (SeguroSegurado s : segurados) {
            seguradosSalvas.add(seguroSeguradoService.save(s));

        }





        System.out.println("Compra codgSeguro: " + resp.getCodgReservaSeguro());


    }

}
