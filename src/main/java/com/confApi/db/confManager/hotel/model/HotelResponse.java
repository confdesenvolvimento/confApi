package com.confApi.db.confManager.hotel.model;

import com.confApi.db.confManager.hotelQuartoValor.HotelQuartoValor;
import com.confApi.db.confManager.reservaHotel.dto.ReservaHotel;
import com.confApi.hub.hotel.dto.HotelPoliticaCancelamento;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class HotelResponse implements Serializable {

    private String codigoHotelSistema;
    private Integer categoria;
    private String descricaoCategoria;
    private String classeSistema;
    private String nomeSistema;
    private String codigoSistema;
    private String nome;
    private String codigoIataCidade;
    private Integer quantidadeAdultos = 0;
    private Integer quantidadeCriancas = 0;
    private String descricao = "";
    private String descricaoExibicao;
    private String descricaoLocalizacao;
    private String descricaoQuartos;
    private String descricaoAreasLazer;
    private String descricaoRefeicoes;
    private String descricaoInstalacoes;
    private String descricaoFornecedor;
    private List<String> informacoesEspeciais;
    private String endereco;
    private String estado;
    private String bairro;
    private String descricaoCategoriaCurta;
    private String cidade;
    private String codigoCidade;
    private String pais;
    private String localizacao;
    private String urlImagem;
    private String telefone;
    private String fax;
    private String email;
    private String cep;
    private String cnpj;
    private String moeda;
    private Double valorCambio;
    private Integer quantidadeNoites;
    private Boolean visivel = true;
    private Boolean recomendado = false;
    private Boolean reservar = false;
    private Boolean exibirMapa = true;
    private Boolean exibirDetalhesHotel = true;
    private Boolean exibirColunaQuartoDisponivel = false;
    private Boolean cobrarTaxaIr = false;
    private Date dataEntrada;
    private Date dataSaida;
    private Date prazoPagamentoCliente;
    private Date prazoPagamentoFornecedor;
    private Float latitude = 0f;
    private Float longitude = 0f;
    private String checkin;
    private String checkout;
    private String horarioCafe;
    private PontoInteresse pontoSelecionado;
    private PontoInteresse distanciaAeroporto;
    private TarifaHotel tarifasHotel;
    private Integer exibirQtdAcomodacoes = 0;
    private Boolean isExibirMaisQtdAcomocoes = false;
    private List<HotelAcomodacao> acomodacoes = new ArrayList<>();
    private List<QuartoPesquisa> quartoPesquisa = new ArrayList<>();
    private List<HotelTaxasPoliticas> taxasPoliticas = new ArrayList<>();
    private List<HotelServicoHotel> servicos = new ArrayList<>();
    private List<HotelDetalhesHotel> detalhesHotel = new ArrayList<>();
    private List<HotelImagens> imagens = new ArrayList<>();
    private List<HotelPoliticaCancelamento> politicaCancelamento = new ArrayList<>();

    private boolean selected = false;// para cotacao de hotel.

    private String searchToken;

    public void atualizaValoresTotalQuartoSelecionado(HotelAcomodacao ac, String qtPesquisa) {
       // UtilDebug.sysError("Chamou atualizar");
        tarifasHotel = new TarifaHotel();
        for (QuartoPesquisa quartoPesquisa1 : quartoPesquisa) {
            if (quartoPesquisa1.getId().equalsIgnoreCase(qtPesquisa)) {
                quartoPesquisa1.selecionaAcomodacao(ac);
            }

        }

        for (QuartoPesquisa quartoPesquisa1 : quartoPesquisa) {

            tarifasHotel.calcularTotais(quartoPesquisa1.getAcomodacaoSelecionada().getTarifaHotel());
        }

    }

    public void atualizaValoresTotalQuartoSelecionado() {
       // UtilDebug.sysError("Chamou atualizar");
        tarifasHotel = new TarifaHotel();
        for (QuartoPesquisa quartoPesquisa1 : quartoPesquisa) {
            tarifasHotel.calcularTotais(quartoPesquisa1.getAcomodacaoSelecionada().getTarifaHotel());
        }

    }

    public void ajustarValoresTotalQuartoSelecionadoChange(TarifaHotel tarifa) {
        tarifasHotel.calcularTotais(tarifa);

    }

    public void mostrarMaisAcomodacoes() {
        isExibirMaisQtdAcomocoes = false;
        // exibirQtdAcomodacoes = acomodacoes.size();
    }

    public void mostrarMenosAcomodacoes() {
        isExibirMaisQtdAcomocoes = true;
        // exibirQtdAcomodacoes = 3;
    }

    public void ocultarAcomodacoes() {
        if (isExibirMaisQtdAcomocoes) {
            isExibirMaisQtdAcomocoes = true;
        } else {
            isExibirMaisQtdAcomocoes = false;
        }
    }

    public HotelResponse(ReservaHotel reservaHotel) {
        this.codigoHotelSistema = null;
        this.categoria = (int) Math.round(reservaHotel.getCodgHotel().getEstrela());
        this.descricaoCategoria = null;
        this.classeSistema = reservaHotel.getCodgSistema().getNomeSistema();
        this.nomeSistema = reservaHotel.getCodgSistema().getNomeSistema();
        this.codigoSistema = (reservaHotel.getCodgSistema() != null && reservaHotel.getCodgSistema().getCodgSistema() != null)
                ? reservaHotel.getCodgSistema().getCodgSistema().toString()
                : null;
        this.nome = reservaHotel.getCodgHotel().getNomeHotel();
        this.codigoIataCidade = null;
        this.descricaoExibicao = null;
        this.descricaoLocalizacao = null;
        this.descricaoQuartos = null;
        this.descricaoAreasLazer = null;
        this.descricaoRefeicoes = null;
        this.descricaoInstalacoes = null;
        this.descricaoFornecedor = null;
        this.informacoesEspeciais = null;
        this.endereco = reservaHotel.getCodgHotel().getEndereco1();
        this.estado = null;
        this.bairro = null;
        this.descricaoCategoriaCurta = null;
        this.cidade = reservaHotel.getCodgHotel().getCodgCidade().getNomeCidade();
        this.codigoCidade = null;
        this.pais = null;
        this.localizacao = null;
        this.urlImagem = reservaHotel.getCodgHotel().getUrlImagemHotel();
        this.telefone = reservaHotel.getCodgHotel().getTelefone();
        this.fax = reservaHotel.getCodgHotel().getFax();
        this.email = reservaHotel.getCodgHotel().getEmail();
        this.cep = null;
        this.cnpj = null;
        this.moeda = null;
        this.valorCambio = null;
        this.quantidadeNoites = reservaHotel.getQuantidadeDiarias();
        this.dataEntrada = reservaHotel.getDataEntrada();
        this.dataSaida = reservaHotel.getDataSaida();
        this.prazoPagamentoCliente = reservaHotel.getPrazoPagamentoCliente();
        this.prazoPagamentoFornecedor = reservaHotel.getPrazoPagamentoFornecedor();
        this.checkin = reservaHotel.getCodgHotel().getCheckin();
        this.checkout = reservaHotel.getCodgHotel().getCheckout();
        this.horarioCafe = reservaHotel.getCodgHotel().getHorarioCafe();
        this.pontoSelecionado = null;
        this.distanciaAeroporto = null;
        this.tarifasHotel = new TarifaHotel(reservaHotel.getHotelQuartoValorList().get(0), reservaHotel);

        for(HotelQuartoValor hotelQuartoValor : reservaHotel.getHotelQuartoValorList()){
            HotelAcomodacao hotelAcomodacao = new HotelAcomodacao(hotelQuartoValor, reservaHotel);
            this.acomodacoes.add(hotelAcomodacao);
        }

        this.latitude = Float.parseFloat(reservaHotel.getCodgHotel().getLatitude());
        this.longitude = Float.parseFloat(reservaHotel.getCodgHotel().getLongitude());

    }

    public HotelResponse() {
    }

}

