package com.confApi.hoteis.model.reserva;

import com.confApi.db.confManager.hotel.model.HotelAcomodacao;
import com.confApi.hub.hotel.dto.Hospedes;
import com.confApi.hub.hotel.dto.HotelPoliticaCancelamento;
import com.confApi.hub.hotel.dto.HotelTaxasPoliticas;
import com.confApi.hub.hotel.dto.TarifaHotel;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class HotelAcomodacaoFront {    private String codgPlanoTarifa;
    private String codgRoom;
    private String siglaTipoQuarto;
    private String nomeQuarto;
    private String nomeQuartoExtenso;
    private Boolean isSelecionado = false;
    private String nomeTipoQuarto;
    private String descricaoTipoCama;
    private String regime;
    private String descricaoOferta;
    private String descricaoTipoDeTarifa;
    private String descricaoTipoAcomodacao;
    private TarifaHotel tarifaHotel;
    private Integer vagasDisponiveis = 0;
    private Boolean isPrePagamento = false;
    private Boolean isNaoReembolsavel = false;
    private String formaPagamento;
    private Integer maximoHospedes = 0;
    private Integer minimoHospedes = 0;
    private Integer maximoCriancas = 0;
    private Integer minimoCriancas = 0;
    private List<Hospedes> hospedes;
    private List<HotelTaxasPoliticas> taxasPoliticas = new ArrayList<>();
    private List<HotelPoliticaCancelamento> politicaCancelamento = new ArrayList<>();

    private String sistema;

    public HotelAcomodacaoFront(HotelAcomodacao hotelAcomodacao) {
        this.codgPlanoTarifa = hotelAcomodacao.getCodgPlanoTarifa();
        this.codgRoom = hotelAcomodacao.getCodgRoom();
        this.siglaTipoQuarto = hotelAcomodacao.getSiglaTipoQuarto();
        this.nomeQuarto = hotelAcomodacao.getNomeQuarto();
        this.nomeQuartoExtenso = hotelAcomodacao.getNomeQuartoExtenso();
        this.isSelecionado = hotelAcomodacao.getIsSelecionado();
        this.nomeTipoQuarto = hotelAcomodacao.getNomeTipoQuarto();
        this.descricaoTipoCama = hotelAcomodacao.getDescricaoTipoCama();
        this.regime = hotelAcomodacao.getRegime();
        this.descricaoOferta = hotelAcomodacao.getDescricaoOferta();
        this.descricaoTipoDeTarifa = hotelAcomodacao.getDescricaoTipoDeTarifa();
        this.descricaoTipoAcomodacao = hotelAcomodacao.getDescricaoTipoAcomodacao();
        this.tarifaHotel = new TarifaHotel(hotelAcomodacao.getTarifaHotel());
        this.vagasDisponiveis = hotelAcomodacao.getVagasDisponiveis();
        this.isPrePagamento = hotelAcomodacao.getIsPrePagamento();
        this.isNaoReembolsavel = hotelAcomodacao.getIsNaoReembolsavel();
        this.formaPagamento = hotelAcomodacao.getFormaPagamento();
        this.maximoHospedes = hotelAcomodacao.getMaximoHospedes();
        this.minimoHospedes = hotelAcomodacao.getMinimoHospedes();
        this.maximoCriancas = hotelAcomodacao.getMaximoCriancas();
        this.minimoCriancas = hotelAcomodacao.getMinimoCriancas();
        this.hospedes = hotelAcomodacao.getHospedes();
        this.taxasPoliticas = new ArrayList<>();
        for(com.confApi.db.confManager.hotel.model.HotelTaxasPoliticas hotelTaxasPoliticas : hotelAcomodacao.getTaxasPoliticas()){
            HotelTaxasPoliticas hotelTaxasPoliticas1 = new HotelTaxasPoliticas(hotelTaxasPoliticas);
            this.taxasPoliticas.add(hotelTaxasPoliticas1);
        }
        this.politicaCancelamento = hotelAcomodacao.getPoliticaCancelamento();
        this.sistema = hotelAcomodacao.getSistema();
    }
}
