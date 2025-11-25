package com.confApi.db.confManager.hotel.model;

import com.confApi.db.confManager.historicoReserva.HistoricoReservaApi;
import com.confApi.db.confManager.hotelHospede.HotelHospede;
import com.confApi.db.confManager.hotelQuartoValor.HotelQuartoValor;
import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.db.confManager.reservaHotel.dto.ReservaHotel;
import com.confApi.db.enumeradores.EnumSexoHospedeHotel;
import com.confApi.hub.aereo.CartaoModel;
import com.confApi.hub.aereo.ParcelaCartaoModel;
import com.confApi.hub.aereo.RecebimentoModel;
import com.confApi.hub.hotel.dto.Hospedes;

import java.io.Serializable;
import java.util.ArrayList;

public class ReservaHotelMapper implements Serializable {

    public ReservaHotelModel mapToModel(ReservaHotel ra) {
        ReservaHotelModel reservaHotelSelecionado = new ReservaHotelModel();

        reservaHotelSelecionado.setUsuarioCriacao(ra.getCodgUsuarioCriacao().getNomeCompleto());
        reservaHotelSelecionado.setCodgUsuarioCriacao(ra.getCodgUsuarioCriacao());
        reservaHotelSelecionado.getCodgUsuarioCriacao().setAgencia(ra.getCodgAgencia());
        reservaHotelSelecionado.setNomeSistema(ra.getCodgSistema().getNomeSistema());
        reservaHotelSelecionado.setDataLimitePagamento(ra.getPrazoPagamentoCliente());
        reservaHotelSelecionado.setDataEmissao(ra.getDataEmissao());
        reservaHotelSelecionado.setCodgAgencia(ra.getCodgAgencia().getCodgAgencia());
        reservaHotelSelecionado.setDescricaMotivoCancelamento(ra.getDescricaoMotivoCancelamento());
        reservaHotelSelecionado.setCodgReservaHotelDB(ra.getCodgReservaHotel().longValue());
        if(reservaHotelSelecionado.getHotel()==null){
            reservaHotelSelecionado.setHotel(new HotelResponse());
        }
        reservaHotelSelecionado.getHotel().setEndereco(ra.getCodgHotel().getEndereco1());
        reservaHotelSelecionado.getHotel().setLatitude(Float.valueOf(ra.getCodgHotel().getLatitude()));
        reservaHotelSelecionado.getHotel().setLongitude(Float.valueOf(ra.getCodgHotel().getLongitude()));
        reservaHotelSelecionado.getHotel().setCheckin(ra.getCodgHotel().getCheckin());
        reservaHotelSelecionado.getHotel().setCheckout(ra.getCodgHotel().getCheckout());
        reservaHotelSelecionado.getHotel().setHorarioCafe(ra.getCodgHotel().getHorarioCafe());
        reservaHotelSelecionado.setStatusPagamento(ra.getStatusPagamento());
        reservaHotelSelecionado.setDescricaoRegraCancelamento(ra.getDescricaoRegraCancelamento());
        reservaHotelSelecionado.setFonte(ra.getFonte());

        if (reservaHotelSelecionado.getRecebimentos() == null) {
            reservaHotelSelecionado.setRecebimentos(new ArrayList());
        }
        for (Recebimento rec : ra.getRecebimentos()) {
            reservaHotelSelecionado.getRecebimentos().add(converterParaRecebimentoModel(rec));
        }

        if (ra.getDataCancelamento() != null) {
            reservaHotelSelecionado.setDataCancelamento(ra.getDataCancelamento());
            reservaHotelSelecionado.setIsExibirBtnCancelarReserva(false);
        }
        if (ra.getCodgHotel().getEstrela() != null) {
            reservaHotelSelecionado.getHotel().setCategoria(ra.getCodgHotel().getEstrela().intValue());
        }
        reservaHotelSelecionado.getHotel().getDetalhesHotel().addAll(ra.getCodgHotel().getHotelDetalhesList());
        reservaHotelSelecionado.getHotel().getImagens().addAll(ra.getCodgHotel().getHotelImagemList());

        for (HotelQuartoValor hqv : ra.getHotelQuartoValorList()) {
            for (HotelHospede hh : hqv.getHotelHospedeList()) {
                for (HotelAcomodacao ac : reservaHotelSelecionado.getHotel().getAcomodacoes()) {
                    ac.setQuantidadeAdultos(hqv.getQtdAdt());
                    ac.setQuantidadeCriancas(hqv.getQtdChd());

                    for (Hospedes ho : ac.getHospedes()) {
                       /* UtilDebug.sysError("Nome: " + hh.getHospedeNome() + " - " + ho.getNomePassageiro()
                                + " -- Sobrenome: " + hh.getHospedeSobrenome() + " - " + ho.getSobrenomePassageiro());*/

                        if (ho.getSobrenomePassageiro() == null || ho.getSobrenomePassageiro().trim().isEmpty()) {
                            // Verifica se o NomePassageiro contém nome e sobrenome juntos
                            String nomeCompletoHospede = hh.getHospedeNome() + " " + hh.getHospedeSobrenome();
                            if (ho.getNomePassageiro().equalsIgnoreCase(nomeCompletoHospede)) {
                                ho.setNomePassageiro(hh.getHospedeNome());
                                ho.setSobrenomePassageiro(hh.getHospedeSobrenome());
                            }
                        }

                        if (hh.getHospedeNome().equalsIgnoreCase(ho.getNomePassageiro())
                                && hh.getHospedeSobrenome().equalsIgnoreCase(ho.getSobrenomePassageiro())) {
                            ho.setIdade(hh.getHospedeIdade());
                            if (hh.getHospedeSexo() == EnumSexoHospedeHotel.Masculino.getIdSexo()) {
                                ho.setSexo(EnumSexoHospedeHotel.Masculino.getDescSexo());
                            } else if (hh.getHospedeSexo() == EnumSexoHospedeHotel.Feminino.getIdSexo()) {
                                ho.setSexo(EnumSexoHospedeHotel.Feminino.getDescSexo());
                            }

                        }
                    }
                }
            }
        }

        //Fazer aqui a verificacao da Tarifa NET HUB x BANCO
        for (HotelQuartoValor hqv : ra.getHotelQuartoValorList()) {
            for (HotelAcomodacao ac : reservaHotelSelecionado.getHotel().getAcomodacoes()) {
                if (hqv.getCodgFornecedorSistema().equalsIgnoreCase(ac.getCodgRoom())) { //Aqui tbm validar o codigo da Tarifa para tratar varios quartos
                    TarifaHotel th = new TarifaHotel();
                    th.setValorMarkupAplicado(hqv.getValorMkpAplicado());

                    th.setMediaDiaria(hqv.getValorDiariaMarkup());
                    th.setMoeda("BRL");
                    th.setPercentualMarkupAplicado(hqv.getPercMkpAplicado());
                    th.setPercentualTaxaExtra(0.0);
                    th.setPercentualTaxaIss(hqv.getPercTaxaIss());
                    th.setPercentualTaxaServico(hqv.getPercTaxaServico());
                    th.setValorTaxaIss(hqv.getValorTaxaIss());
                    th.setValorTaxaServico(hqv.getValorTaxaServico());
                    th.setValorTotalEstadiaComMarkup(hqv.getValorTotalEstadiaMarkup());
                    th.setValorTotalEstadiaComMarkupBrl(hqv.getValorTotalEstadiaMarkup());
                    th.setValorTotalEstadiaNet(hqv.getValorTotalEstadiaNet());
                    th.setTotalDiarias(hqv.getValorDiariaTotalMarkup());
                    ac.setTarifaHotel(th);
                }
            }
        }
        for (HotelAcomodacao ac : reservaHotelSelecionado.getHotel().getAcomodacoes()) {
            reservaHotelSelecionado.getTarifaTotalReserva().setMediaDiaria(reservaHotelSelecionado.getTarifaTotalReserva().getMediaDiaria() + ac.getTarifaHotel().getMediaDiaria());
            reservaHotelSelecionado.getTarifaTotalReserva().setTotalDiarias(reservaHotelSelecionado.getTarifaTotalReserva().getTotalDiarias() + ac.getTarifaHotel().getTotalDiarias());
            reservaHotelSelecionado.getTarifaTotalReserva().setValorTaxaIss(reservaHotelSelecionado.getTarifaTotalReserva().getValorTaxaIss() + ac.getTarifaHotel().getValorTaxaIss());
            reservaHotelSelecionado.getTarifaTotalReserva().setValorTaxaServico(reservaHotelSelecionado.getTarifaTotalReserva().getValorTaxaServico() + ac.getTarifaHotel().getValorTaxaServico());
            reservaHotelSelecionado.getTarifaTotalReserva().setValorTotalEstadiaComMarkupBrl(reservaHotelSelecionado.getTarifaTotalReserva().getValorTotalEstadiaComMarkupBrl() + ac.getTarifaHotel().getValorTotalEstadiaComMarkupBrl());
        }

        reservaHotelSelecionado.setHistorico(
                new HistoricoReservaApi().findByCodgReservaHotel(reservaHotelSelecionado.getCodgReservaHotelDB().intValue()));

        return reservaHotelSelecionado;
    }
    public RecebimentoModel converterParaRecebimentoModel(Recebimento recebimentoDB) {

        RecebimentoModel recebimentoModel = new RecebimentoModel();
        recebimentoModel.setCodgFormaPagamento(recebimentoDB.getCodgFormaPagto().getCodgFormaPagto());
        recebimentoModel.setValorPagamento(recebimentoDB.getValrRecebimento());
        recebimentoModel.setNomeFormaPagamento(recebimentoDB.getCodgFormaPagto().getNomeFormaPagto());
        recebimentoModel.setValorEntrada(recebimentoDB.getValrEntrada());
        recebimentoModel.setStatusRecebimento(recebimentoDB.getStatus());
        recebimentoModel.setDataRecebimento(recebimentoDB.getDataRecebimento());
        recebimentoModel.setCodgCodgRecebimento(recebimentoDB.getCodgRecebimento());
        if (recebimentoDB.getLink() != null) {
            recebimentoModel.setLink(recebimentoDB.getLink());
        }
        if (recebimentoDB.getNumrCartao() != null) {

            CartaoModel fpModel = new CartaoModel();
            fpModel.setNumeroCartao(mascararNumeroCartao(recebimentoDB.getNumrCartao()));
            fpModel.setTitularBandeira(recebimentoDB.getTitularCartao());
            fpModel.setCodgAutorizacao(recebimentoDB.getCodgAutCartao());
            fpModel.setQuantidadeParcelas(recebimentoDB.getQtdeParcela().toString());
            if (recebimentoDB.getAssinaturaEletronica() != null) {
                recebimentoModel.setAssinatura(recebimentoDB.getAssinaturaEletronica());
            }

            ParcelaCartaoModel parcela = new ParcelaCartaoModel();
            parcela.setValorPrimeiraParcela(recebimentoDB.getValrPrimeiraParcela());
            parcela.setValorDemaisParcelas(recebimentoDB.getValrDemaisParcela());
            fpModel.setParcelaSelecionada(parcela);
            recebimentoModel.setCartaoSelecionado(fpModel);
        }

        return recebimentoModel;
    }

    public String mascararNumeroCartao(String numeroCartao) {
        if (numeroCartao == null || numeroCartao.length() < 4) {
            throw new IllegalArgumentException("O número do cartão é inválido");
        }

        int length = numeroCartao.length();
        String ultimosQuatroDigitos = numeroCartao.substring(length - 4);
        String asteriscos = "*".repeat(length - 4);

        return asteriscos + ultimosQuatroDigitos;
    }
}
