package com.confApi.endPoints.voo;

import com.confApi.endPoints.aeroporto.AeroportoResponse;
import com.confApi.endPoints.assento.AssentoResponse;
import com.confApi.endPoints.bagagem.BagagemResponse;
import com.confApi.endPoints.companhiaAerea.CompanhiaResponse;
import com.confApi.hub.aereo.AssentoHub;
import com.confApi.hub.aereo.BagagemHub;
import com.confApi.hub.aereo.VooHub;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class VooResponse {
    private Integer id;
    private AeroportoResponse destino;
    private AeroportoResponse origem;
    private CompanhiaResponse ciaMandatoria;
    private CompanhiaResponse ciaOperadora;
    private Boolean bagagemInclusa;
    private Integer bagagemIndicador;
    private Double bagagemPeso;
    private Integer bagagemQuantidade;
    private String bagagemUnidadeDeMedida;
    private List<BagagemResponse> bagagens = new ArrayList<>();
    private Date dataPartida;
    private String horaPartida;
    private Date dataChegada;
    private String horaChegada;
    private String duracao;
    private String equipamento;
    private Integer qtdEscalas = 0;
    private String classe;
    private String numeroVoo;
    private String cabine;
    private Integer tipoSegmento = 0; // I= Ida, V=Volta
    private Boolean isConexao = false;
    private Boolean isReembolsavel = false;
    private List<AssentoResponse> assentos;
    private Boolean conexao;
    private String familia;
    private String familiaCodigo;
    private String localizadorCia;
    private String status;
    private Boolean surface;
    private String identificacaoDoVoo;
    private String descricaocss = "color_default";
    private String baseTarifaria;
    private Boolean isCodeShare = false;

    public VooResponse(VooHub vooHub) {
        this.id = vooHub.getId();
        this.destino = new AeroportoResponse(vooHub.getDestino());
        this.origem = new AeroportoResponse(vooHub.getOrigem());
        this.ciaMandatoria = new CompanhiaResponse(vooHub.getCiaMandatoria());
        this.ciaOperadora = vooHub.getCiaOperadora() != null ? new CompanhiaResponse(vooHub.getCiaOperadora()) : null;
        this.bagagemInclusa = vooHub.getBagagemInclusa();
        this.bagagemIndicador = vooHub.getBagagemIndicador();
        this.bagagemPeso = vooHub.getBagagemPeso();
        this.bagagemQuantidade = vooHub.getBagagemQuantidade();
        this.bagagemUnidadeDeMedida = vooHub.getBagagemUnidadeDeMedida();
        this.bagagens = new ArrayList<>();
        for (BagagemHub bagagemHub : vooHub.getBagagens()){
            this.bagagens.add(new BagagemResponse(bagagemHub));
        }
        this.dataPartida = vooHub.getDataPartida();
        this.horaPartida = vooHub.getHoraPartida();
        this.dataChegada = vooHub.getDataChegada();
        this.horaChegada = vooHub.getHoraChegada();
        this.duracao = vooHub.getDuracao();
        this.equipamento = vooHub.getEquipamento();
        this.qtdEscalas = vooHub.getQtdEscalas();
        this.classe = vooHub.getClasse();
        this.numeroVoo = vooHub.getNumeroVoo();
        this.cabine = vooHub.getCabine();
        this.tipoSegmento = vooHub.getTipoSegmento();
        this.isConexao = vooHub.getIsConexao();
        this.isReembolsavel = vooHub.getIsReembolsavel();
        this.assentos = new ArrayList<>();
        for (AssentoHub assentoHub : vooHub.getAssentos()){
            this.assentos.add(new AssentoResponse(assentoHub));
        }
        this.conexao = vooHub.getConexao();
        this.familia = vooHub.getFamilia();
        this.familiaCodigo = vooHub.getFamiliaCodigo();
        this.localizadorCia = vooHub.getLocalizadorCia();
        this.status = vooHub.getStatus();
        this.surface = vooHub.getSurface();
        this.identificacaoDoVoo = vooHub.getIdentificacaoDoVoo();
        this.descricaocss = null;
        this.baseTarifaria = null;
        this.isCodeShare = null;
    }
}
