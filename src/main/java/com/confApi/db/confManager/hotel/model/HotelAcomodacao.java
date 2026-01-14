package com.confApi.db.confManager.hotel.model;

import com.confApi.db.confManager.hotelHospede.HotelHospede;
import com.confApi.db.confManager.hotelQuartoValor.HotelQuartoValor;
import com.confApi.db.confManager.reservaHotel.dto.ReservaHotel;
import com.confApi.hub.hotel.dto.Hospedes;
import com.confApi.hub.hotel.dto.HotelPoliticaCancelamento;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@ToString
public class HotelAcomodacao implements Serializable {

    private String codgPlanoTarifa;
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
    private Date prazoPagamentoFornecedor;
    private Date prazoPagamentoCliente;
    private String descricaoRegraCancelamento;
    private String formaPagamento;
    private Integer qtdHospedes = 0;
    private Integer quantidadeAdultos = 0;
    private Integer quantidadeCriancas = 0;
    private Integer maximoHospedes;
    private Integer minimoHospedes;
    private Integer maximoCriancas;
    private Integer minimoCriancas;
    private List<Hospedes> hospedes = new ArrayList<>();
    private List<HotelRegrasCancelamento> regrasCancelamentoAcomodacao = new ArrayList<>();
    private List<HotelTaxasPoliticas> taxasPoliticas = new ArrayList<>();
    private List<HotelPoliticaCancelamento> politicaCancelamento = new ArrayList<>();

    private Integer idPesquisaQuartoCotacao;
    private String idUnico;

    private String sistema;

    public HotelAcomodacao() {
        // Gera um UUID único no momento da criação do objeto
        this.idUnico = UUID.randomUUID().toString();
    }

    public HotelAcomodacao(HotelQuartoValor hotelQuartoValor, ReservaHotel reservaHotel) {
        this.codgPlanoTarifa = null;
        this.codgRoom = null;
        this.siglaTipoQuarto = hotelQuartoValor.getSiglaTipoQuarto();
        this.nomeQuarto = hotelQuartoValor.getNomeQuarto();
        this.nomeQuartoExtenso = hotelQuartoValor.getNomeQuarto();
        this.nomeTipoQuarto = hotelQuartoValor.getSiglaTipoQuarto();
        this.descricaoTipoCama = null;
        this.regime = hotelQuartoValor.getDescricaoRegime();
        this.descricaoOferta = null;
        this.descricaoTipoDeTarifa = null;
        this.descricaoTipoAcomodacao = null;
        //valores das tarifas
        this.tarifaHotel = new TarifaHotel(hotelQuartoValor, reservaHotel);
        this.prazoPagamentoFornecedor = reservaHotel.getPrazoPagamentoFornecedor();
        this.prazoPagamentoCliente = reservaHotel.getPrazoPagamentoCliente();
        this.descricaoRegraCancelamento = descricaoRegraCancelamento;
        this.formaPagamento = null;
        this.maximoHospedes = null;
        this.minimoHospedes = null;
        this.maximoCriancas = null;
        this.minimoCriancas = null;
        for (HotelHospede hospede : hotelQuartoValor.getHotelHospedeList()) {
            Hospedes hosp = new Hospedes(hospede);
            this.quantidadeAdultos++;
            this.hospedes.add(hosp);
        }
        this.idPesquisaQuartoCotacao = null;
        this.idUnico = null;

    }

    // Getter para o ID único
    public String getIdUnico() {
        return idUnico;
    }



}

