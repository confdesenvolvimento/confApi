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
import com.confApi.seguros.dto.FiltroReservaSeguro;
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

    public SeguroReserva comprar(SeguroCompraModel req) {
        SeguroReserva seguroReserva = new SeguroReserva(req);
        SeguroReserva resp = seguroReservaService.save(seguroReserva);
        return resp;
    }

    public SeguroReserva atualizarReserva(SeguroReserva seguroReserva) {
        SeguroReserva resp = seguroReservaService.atualizarReservaSeguro(seguroReserva);
        return resp;
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
        dto.setCodgUsuario(reserva.getCodgUsuarioCriacao().getCodgUsuario());
        dto.setLoginUsuario(reserva.getCodgUsuarioCriacao().getLoginUsuario());
        dto.setNomeUsuario(reserva.getCodgUsuarioCriacao().getNomeCompleto());

        dto.setCodgAgencia(reserva.getCodgAgencia().getCodgAgencia());
        dto.setNomeAgencia(reserva.getCodgAgencia().getNomeAgencia());

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

    public List<SeguroReserva> carregarReservas(FiltroReservaSeguro filtroReservaSeguro) {
        return seguroReservaService.findFiltro(filtroReservaSeguro);
    }

    public void cancelarReserva(CancelamentoRequestDTO cancelamentoRequest){
        seguroReservaService.cancelarReservaSeguro(cancelamentoRequest);
    }



}
