package com.confApi.seguros;

import com.confApi.db.confManager.seguro.categoria.SeguroCategoriaService;
import com.confApi.db.confManager.seguro.cobertura.SeguroCobertura;
import com.confApi.db.confManager.seguro.cobertura.SeguroCoberturaService;
import com.confApi.db.confManager.seguro.coberturaDetalhada.SeguroCoberturaDetalhada;
import com.confApi.db.confManager.seguro.coberturaDetalhada.SeguroCoberturaDetalhadaService;
import com.confApi.db.confManager.seguro.reserva.DTO.CancelamentoRequestDTO;
import com.confApi.db.confManager.seguro.reserva.SeguroReserva;
import com.confApi.db.confManager.seguro.reserva.SeguroReservaService;
import com.confApi.db.confManager.seguro.segurado.SeguroSegurado;
import com.confApi.db.confManager.seguro.segurado.SeguroSeguradoService;
import com.confApi.seguros.dto.SeguradoDTO;
import com.confApi.seguros.dto.SeguroCompraModel;
import com.confApi.seguros.dto.SeguroReservaDTO;
import com.confApi.seguros.mapper.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    public void comprar(SeguroCompraModel req, List<SeguroReservaDTO> seguroReservaDTOList) {
        SeguroReserva reserva = SeguroCompraToManagerMapper.toReserva(req, seguroReservaDTOList);
        SeguroReserva resp = seguroReservaService.save(reserva);

        SeguroCobertura seguroCoberturaResp = seguroCoberturaService.save(SeguroCompraToManagerMapper.toCobertura(req, resp));

        List<SeguroCoberturaDetalhada> coberturaDetalhadas = SeguroCompraToManagerMapper.toCoberturaDetalhada(req, seguroCoberturaResp);
        for (SeguroCoberturaDetalhada s : coberturaDetalhadas) {
            seguroCoberturaDetalhadaService.save(s);
        }

        List<SeguroSegurado> segurados = SeguroCompraToManagerMapper.toSegurados(req, seguroCoberturaResp, seguroReservaDTOList);
        List<SeguroSegurado> seguradosSalvas = new ArrayList<>();
        for (SeguroSegurado s : segurados) {
            seguradosSalvas.add(seguroSeguradoService.save(s));
        }
    }

    public SeguroReservaDTO carregarReserva(String localizador) {
        Optional<SeguroReserva> optionalSeguroReserva = seguroReservaService.findByLocalizador(localizador);
        SeguroReserva reserva = optionalSeguroReserva.get();
        SeguroReservaDTO dto = new SeguroReservaDTO();

        dto.setCodgReservaSeguro(reserva.getCodgReservaSeguro());
        dto.setLocalizador(reserva.getLocalizador());
        dto.setStatus(reserva.getStatus());
        dto.setStatusPagamentoCliente(reserva.getStatusPagamentoCliente());
        dto.setDataCriacao(reserva.getDataCriacao());
        dto.setDataEmissao(reserva.getDataEmissao());
        dto.setCodgUsuario(reserva.getUsuario().getCodgUsuario());
        dto.setLoginUsuario(reserva.getUsuario().getLoginUsuario());
        dto.setNomeUsuario(reserva.getUsuario().getNomeCompleto());

        dto.setCodgAgencia(reserva.getAgencia().getCodgAgencia());
        dto.setNomeAgencia(reserva.getAgencia().getNomeAgencia());

        dto.setDataInicioCobertura(reserva.getDataInicioCobertura());
        dto.setDataFinalCobertura(reserva.getDataFinalCobertura());

        dto.setValorTotalReservaNet(reserva.getValorTotalReservaNet());
        dto.setValorTotalReservaMarkup(reserva.getValorTotalReservaMarkup());

// Contato emergência (se você salvou na reserva ou em outra tabela)
        dto.setContatoEmergencia(
                ContatoEmergenciaMapper.toDTO(
                        reserva.getContatoNome(),
                        reserva.getContatoTelefone(),
                        reserva.getContatoEmail()
                )
        );

        Optional<SeguroCobertura> cobertura = seguroCoberturaService.findBySeguroReservaCodgReservaSeguro(reserva.getCodgReservaSeguro());
        dto.setCobertura(SeguroCoberturaToPlanoMapper.toDTO(cobertura.get(), null));

        List<SeguroSegurado> listOptionalSegurados = seguroSeguradoService.findBySeguroCoberturaCodgSeguroCobertura(cobertura.get().getCodgSeguroCobertura());

        // Segurados
        dto.setSegurados(
                SeguradoMapper.toDTOList(listOptionalSegurados)
        );

        List<SeguroCoberturaDetalhada> opt = seguroCoberturaDetalhadaService.findBySeguroCoberturaCodgSeguroCobertura(cobertura.get().getCodgSeguroCobertura());
        
        dto.getCobertura().getCoberturasDetalhes().addAll(CoberturaSeguroMapper.toDTOList(opt));
        return dto;
    }

    public void cancelarReserva(CancelamentoRequestDTO cancelamentoRequest){
        seguroReservaService.cancelarReservaSeguro(cancelamentoRequest);
    }



}
