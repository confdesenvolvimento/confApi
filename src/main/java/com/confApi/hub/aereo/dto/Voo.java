package com.confApi.hub.aereo.dto;

import com.confApi.db.confManager.assentoAereo.Assento;
import com.confApi.endPoints.assento.AssentoResponse;
import com.confApi.endPoints.bagagem.BagagemResponse;
import com.confApi.endPoints.voo.VooResponse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class Voo implements Serializable{

    private Integer id;
    private Aeroporto destino;
    private Aeroporto origem;
    private Companhia ciaMandatoria = new Companhia();
    private Companhia ciaOperadora;
    private Boolean bagagemInclusa;
    private Integer bagagemIndicador;
    private Double bagagemPeso;
    private Integer bagagemQuantidade;
    private String bagagemUnidadeDeMedida;
    private List<Bagagem> bagagens = new ArrayList<>();
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
    private List<Assento> assentos;
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

    public Voo(VooResponse vooResponse) {
        this.id = vooResponse.getId();
        this.destino = new Aeroporto(vooResponse.getDestino());
        this.origem = new Aeroporto(vooResponse.getOrigem());
        this.ciaMandatoria = new Companhia(vooResponse.getCiaMandatoria());
        this.ciaOperadora = vooResponse.getCiaOperadora() != null ? new Companhia(vooResponse.getCiaOperadora()) : null;
        this.bagagemInclusa = vooResponse.getBagagemInclusa();
        this.bagagemIndicador = vooResponse.getBagagemIndicador();
        this.bagagemPeso = vooResponse.getBagagemPeso();
        this.bagagemQuantidade = vooResponse.getBagagemQuantidade();
        this.bagagemUnidadeDeMedida = vooResponse.getBagagemUnidadeDeMedida();
        this.bagagens = new ArrayList<>();
        for(BagagemResponse bagagem:vooResponse.getBagagens()){
            this.bagagens.add(new Bagagem(bagagem));
        }
        this.dataPartida = vooResponse.getDataPartida();
        this.horaPartida = vooResponse.getHoraPartida();
        this.dataChegada = vooResponse.getDataChegada();
        this.horaChegada = vooResponse.getHoraChegada();
        this.duracao = vooResponse.getDuracao();
        this.equipamento = vooResponse.getEquipamento();
        this.qtdEscalas = vooResponse.getQtdEscalas();
        this.classe = vooResponse.getClasse();
        this.numeroVoo = vooResponse.getNumeroVoo();
        this.cabine = vooResponse.getCabine();
        this.tipoSegmento = vooResponse.getTipoSegmento();
        this.isConexao = vooResponse.getIsConexao();
        this.isReembolsavel = vooResponse.getIsReembolsavel();
        this.assentos = new ArrayList<>();
        for(AssentoResponse assento:vooResponse.getAssentos()){
            this.assentos.add(new Assento(assento));
        }
        this.conexao = vooResponse.getConexao();
        this.familia = vooResponse.getFamilia();
        this.familiaCodigo = vooResponse.getFamiliaCodigo();
        this.localizadorCia = vooResponse.getLocalizadorCia();
        this.status = vooResponse.getStatus();
        this.surface = vooResponse.getSurface();
        this.identificacaoDoVoo = vooResponse.getIdentificacaoDoVoo();
        this.descricaocss = vooResponse.getDescricaocss();
        this.baseTarifaria = vooResponse.getBaseTarifaria();
        this.isCodeShare = vooResponse.getIsCodeShare();
    }

    public String getBaseTarifaria() {
        return baseTarifaria;
    }

    public void setBaseTarifaria(String baseTarifaria) {
        this.baseTarifaria = baseTarifaria;
    }



    public String getDescricaocss() {
        return descricaocss;
    }

    public void setDescricaocss(String descricaocss) {
        this.descricaocss = descricaocss;
    }



    public Boolean getIsConexao() {
        return isConexao;
    }

    public void setIsConexao(Boolean isConexao) {
        this.isConexao = isConexao;
    }

    public Boolean getIsReembolsavel() {
        return isReembolsavel;
    }

    public void setIsReembolsavel(Boolean isReembolsavel) {
        this.isReembolsavel = isReembolsavel;
    }

    public String getIdentificacaoDoVoo() {
        return identificacaoDoVoo;
    }

    public void setIdentificacaoDoVoo(String identificacaoDoVoo) {
        this.identificacaoDoVoo = identificacaoDoVoo;
    }

    public Boolean getIsCodeShare() {
        return isCodeShare;
    }

    public void setIsCodeShare(Boolean isCodeShare) {
        this.isCodeShare = isCodeShare;
    }




    public Voo() {
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.id);
        hash = 97 * hash + Objects.hashCode(this.destino);
        hash = 97 * hash + Objects.hashCode(this.origem);
        hash = 97 * hash + Objects.hashCode(this.ciaMandatoria);
        hash = 97 * hash + Objects.hashCode(this.ciaOperadora);
        hash = 97 * hash + Objects.hashCode(this.bagagemInclusa);
        hash = 97 * hash + Objects.hashCode(this.bagagemIndicador);
        hash = 97 * hash + Objects.hashCode(this.bagagemPeso);
        hash = 97 * hash + Objects.hashCode(this.bagagemQuantidade);
        hash = 97 * hash + Objects.hashCode(this.bagagemUnidadeDeMedida);
        hash = 97 * hash + Objects.hashCode(this.dataPartida);
        hash = 97 * hash + Objects.hashCode(this.horaPartida);
        hash = 97 * hash + Objects.hashCode(this.dataChegada);
        hash = 97 * hash + Objects.hashCode(this.horaChegada);
        hash = 97 * hash + Objects.hashCode(this.duracao);
        hash = 97 * hash + Objects.hashCode(this.equipamento);
        hash = 97 * hash + Objects.hashCode(this.qtdEscalas);
        hash = 97 * hash + Objects.hashCode(this.classe);
        hash = 97 * hash + Objects.hashCode(this.numeroVoo);
        hash = 97 * hash + Objects.hashCode(this.cabine);
        hash = 97 * hash + Objects.hashCode(this.tipoSegmento);
        hash = 97 * hash + Objects.hashCode(this.isConexao);
        hash = 97 * hash + Objects.hashCode(this.isReembolsavel);
        hash = 97 * hash + Objects.hashCode(this.assentos);
        hash = 97 * hash + Objects.hashCode(this.conexao);
        hash = 97 * hash + Objects.hashCode(this.familia);
        hash = 97 * hash + Objects.hashCode(this.familiaCodigo);
        hash = 97 * hash + Objects.hashCode(this.localizadorCia);
        hash = 97 * hash + Objects.hashCode(this.status);
        hash = 97 * hash + Objects.hashCode(this.surface);
        hash = 97 * hash + Objects.hashCode(this.identificacaoDoVoo);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Voo other = (Voo) obj;
        if (!Objects.equals(this.bagagemUnidadeDeMedida, other.bagagemUnidadeDeMedida)) {
            return false;
        }
        if (!Objects.equals(this.horaPartida, other.horaPartida)) {
            return false;
        }
        if (!Objects.equals(this.horaChegada, other.horaChegada)) {
            return false;
        }
        if (!Objects.equals(this.duracao, other.duracao)) {
            return false;
        }
        if (!Objects.equals(this.equipamento, other.equipamento)) {
            return false;
        }
        if (!Objects.equals(this.classe, other.classe)) {
            return false;
        }
        if (!Objects.equals(this.numeroVoo, other.numeroVoo)) {
            return false;
        }
        if (!Objects.equals(this.cabine, other.cabine)) {
            return false;
        }
        if (!Objects.equals(this.familia, other.familia)) {
            return false;
        }
        if (!Objects.equals(this.familiaCodigo, other.familiaCodigo)) {
            return false;
        }
        if (!Objects.equals(this.localizadorCia, other.localizadorCia)) {
            return false;
        }
        if (!Objects.equals(this.status, other.status)) {
            return false;
        }
        if (!Objects.equals(this.identificacaoDoVoo, other.identificacaoDoVoo)) {
            return false;
        }
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.destino, other.destino)) {
            return false;
        }
        if (!Objects.equals(this.origem, other.origem)) {
            return false;
        }
        if (!Objects.equals(this.ciaMandatoria, other.ciaMandatoria)) {
            return false;
        }
        if (!Objects.equals(this.ciaOperadora, other.ciaOperadora)) {
            return false;
        }
        if (!Objects.equals(this.bagagemInclusa, other.bagagemInclusa)) {
            return false;
        }
        if (!Objects.equals(this.bagagemIndicador, other.bagagemIndicador)) {
            return false;
        }
        if (!Objects.equals(this.bagagemPeso, other.bagagemPeso)) {
            return false;
        }
        if (!Objects.equals(this.bagagemQuantidade, other.bagagemQuantidade)) {
            return false;
        }
        if (!Objects.equals(this.dataPartida, other.dataPartida)) {
            return false;
        }
        if (!Objects.equals(this.dataChegada, other.dataChegada)) {
            return false;
        }
        if (!Objects.equals(this.qtdEscalas, other.qtdEscalas)) {
            return false;
        }
        if (!Objects.equals(this.tipoSegmento, other.tipoSegmento)) {
            return false;
        }
        if (!Objects.equals(this.isConexao, other.isConexao)) {
            return false;
        }
        if (!Objects.equals(this.isReembolsavel, other.isReembolsavel)) {
            return false;
        }
        if (!Objects.equals(this.assentos, other.assentos)) {
            return false;
        }
        if (!Objects.equals(this.conexao, other.conexao)) {
            return false;
        }
        return Objects.equals(this.surface, other.surface);
    }

    public Voo(Integer id, Aeroporto destino, Aeroporto origem, Companhia ciaMandatoria,
               Companhia ciaOperadora, Boolean bagagemInclusa, Integer bagagemIndicador,
               Double bagagemPeso, Integer bagagemQuantidade, String bagagemUnidadeDeMedida,
               Date dataPartida, String horaPartida, Date dataChegada, String horaChegada,
               String duracao, String equipamento, Integer qtdEscalas, String classe, String numeroVoo,
               String cabine, Integer tipoSegmento, Boolean isConexao, Boolean isReembolsavel,
               List<Assento> assentos, Boolean conexao, String familia, String familiaCodigo,
               String localizadorCia, String status, Boolean surface) {
        this.id = id;
        this.destino = destino;
        this.origem = origem;
        this.ciaMandatoria = ciaMandatoria;
        this.ciaOperadora = ciaOperadora;
        this.bagagemInclusa = bagagemInclusa;
        this.bagagemIndicador = bagagemIndicador;
        this.bagagemPeso = bagagemPeso;
        this.bagagemQuantidade = bagagemQuantidade;
        this.bagagemUnidadeDeMedida = bagagemUnidadeDeMedida;
        this.dataPartida = dataPartida;
        this.horaPartida = horaPartida;
        this.dataChegada = dataChegada;
        this.horaChegada = horaChegada;
        this.duracao = duracao;
        this.equipamento = equipamento;
        this.qtdEscalas = qtdEscalas;
        this.classe = classe;
        this.numeroVoo = numeroVoo;
        this.cabine = cabine;
        this.tipoSegmento = tipoSegmento;
        this.isConexao = isConexao;
        this.isReembolsavel = isReembolsavel;
        this.assentos = assentos;
        this.conexao = conexao;
        this.familia = familia;
        this.familiaCodigo = familiaCodigo;
        this.localizadorCia = localizadorCia;
        this.status = status;
        this.surface = surface;
    }

    public Voo(Integer id, Aeroporto destino, Aeroporto origem, Companhia ciaOperadora,
               Boolean bagagemInclusa, Integer bagagemIndicador, Double bagagemPeso,
               Integer bagagemQuantidade, String bagagemUnidadeDeMedida, Date dataPartida,
               String horaPartida, Date dataChegada, String horaChegada, String duracao,
               String equipamento, String classe, String numeroVoo, String cabine,
               List<Assento> assentos, Boolean conexao, String familia, String familiaCodigo,
               String localizadorCia, String status, Boolean surface, String identificacaoDoVoo,
               String baseTarifaria) {
        this.id = id;
        this.destino = destino;
        this.origem = origem;
        this.ciaOperadora = ciaOperadora;
        this.bagagemInclusa = bagagemInclusa;
        this.bagagemIndicador = bagagemIndicador;
        this.bagagemPeso = bagagemPeso;
        this.bagagemQuantidade = bagagemQuantidade;
        this.bagagemUnidadeDeMedida = bagagemUnidadeDeMedida;
        this.dataPartida = dataPartida;
        this.horaPartida = horaPartida;
        this.dataChegada = dataChegada;
        this.horaChegada = horaChegada;
        this.duracao = duracao;
        this.equipamento = equipamento;
        this.classe = classe;
        this.numeroVoo = numeroVoo;
        this.cabine = cabine;
        this.assentos = assentos;
        this.conexao = conexao;
        this.familia = familia;
        this.familiaCodigo = familiaCodigo;
        this.localizadorCia = localizadorCia;
        this.status = status;
        this.surface = surface;
        this.identificacaoDoVoo = identificacaoDoVoo;
        this.baseTarifaria = baseTarifaria;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Aeroporto getDestino() {
        return destino;
    }

    public void setDestino(Aeroporto destino) {
        this.destino = destino;
    }

    public Aeroporto getOrigem() {
        return origem;
    }

    public void setOrigem(Aeroporto origem) {
        this.origem = origem;
    }

    public Companhia getCiaMandatoria() {
        return ciaMandatoria;
    }

    public void setCiaMandatoria(Companhia ciaMandatoria) {
        this.ciaMandatoria = ciaMandatoria;
    }

    public Companhia getCiaOperadora() {
        return ciaOperadora;
    }

    public void setCiaOperadora(Companhia ciaOperadora) {
        this.ciaOperadora = ciaOperadora;
    }

    public Boolean getBagagemInclusa() {
        return bagagemInclusa;
    }

    public void setBagagemInclusa(Boolean bagagemInclusa) {
        this.bagagemInclusa = bagagemInclusa;
    }

    public Integer getBagagemIndicador() {
        return bagagemIndicador;
    }

    public void setBagagemIndicador(Integer bagagemIndicador) {
        this.bagagemIndicador = bagagemIndicador;
    }

    public Double getBagagemPeso() {
        return bagagemPeso;
    }

    public void setBagagemPeso(Double bagagemPeso) {
        this.bagagemPeso = bagagemPeso;
    }

    public Integer getBagagemQuantidade() {
        return bagagemQuantidade;
    }

    public void setBagagemQuantidade(Integer bagagemQuantidade) {
        this.bagagemQuantidade = bagagemQuantidade;
    }

    public String getBagagemUnidadeDeMedida() {
        return bagagemUnidadeDeMedida;
    }

    public void setBagagemUnidadeDeMedida(String bagagemUnidadeDeMedida) {
        this.bagagemUnidadeDeMedida = bagagemUnidadeDeMedida;
    }

    public Date getDataPartida() {
        return dataPartida;
    }

    public void setDataPartida(Date dataPartida) {
        this.dataPartida = dataPartida;
    }

    public String getHoraPartida() {
        return horaPartida;
    }

    public void setHoraPartida(String horaPartida) {
        this.horaPartida = horaPartida;
    }

    public Date getDataChegada() {
        return dataChegada;
    }

    public void setDataChegada(Date dataChegada) {
        this.dataChegada = dataChegada;
    }

    public String getHoraChegada() {
        return horaChegada;
    }

    public void setHoraChegada(String horaChegada) {
        this.horaChegada = horaChegada;
    }

    public String getDuracao() {
        return duracao;
    }

    public void setDuracao(String duracao) {
        this.duracao = duracao;
    }

    public String getEquipamento() {
        return equipamento;
    }

    public void setEquipamento(String equipamento) {
        this.equipamento = equipamento;
    }

    public Integer getQtdEscalas() {
        return qtdEscalas;
    }

    public void setQtdEscalas(Integer qtdEscalas) {
        this.qtdEscalas = qtdEscalas;
    }

    public String getClasse() {
        return classe;
    }

    public void setClasse(String classe) {
        this.classe = classe;
    }

    public String getNumeroVoo() {
        return numeroVoo;
    }

    public void setNumeroVoo(String numeroVoo) {
        this.numeroVoo = numeroVoo;
    }

    public String getCabine() {
        return cabine;
    }

    public void setCabine(String cabine) {
        this.cabine = cabine;
    }

    public Integer getTipoSegmento() {
        return tipoSegmento;
    }

    public void setTipoSegmento(Integer tipoSegmento) {
        this.tipoSegmento = tipoSegmento;
    }

    public Boolean getConexao() {
        return isConexao;
    }

    public void setConexao(Boolean conexao) {
        isConexao = conexao;
    }

    public String getFamilia() {
        return familia;
    }

    public void setFamilia(String familia) {
        this.familia = familia;
    }

    public String getFamiliaCodigo() {
        return familiaCodigo;
    }

    public void setFamiliaCodigo(String familiaCodigo) {
        this.familiaCodigo = familiaCodigo;
    }

    public String getLocalizadorCia() {
        return localizadorCia;
    }

    public void setLocalizadorCia(String localizadorCia) {
        this.localizadorCia = localizadorCia;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getSurface() {
        return surface;
    }

    public void setSurface(Boolean surface) {
        this.surface = surface;
    }

    public Boolean getReembolsavel() {
        return isReembolsavel;
    }

    public void setReembolsavel(Boolean reembolsavel) {
        isReembolsavel = reembolsavel;
    }

    public List<Assento> getAssentos() {
        return assentos;
    }

    public void setAssentos(List<Assento> assentos) {
        this.assentos = assentos;
    }

    public List<Bagagem> getBagagens() {
        return bagagens;
    }

    public void setBagagens(List<Bagagem> bagagens) {
        this.bagagens = bagagens;
    }



    @Override
    public String toString() {
        return "Voo{" +
                "id=" + id +
                ", destino=" + destino +
                ", origem=" + origem +
                ", ciaMandatoria=" + ciaMandatoria +
                ", ciaOperadora=" + ciaOperadora +
                ", bagagemInclusa=" + bagagemInclusa +
                ", bagagemIndicador=" + bagagemIndicador +
                ", bagagemPeso=" + bagagemPeso +
                ", bagagemQuantidade=" + bagagemQuantidade +
                ", bagagemUnidadeDeMedida='" + bagagemUnidadeDeMedida + '\'' +
                ", dataPartida=" + dataPartida +
                ", horaPartida='" + horaPartida + '\'' +
                ", dataChegada=" + dataChegada +
                ", horaChegada='" + horaChegada + '\'' +
                ", duracao='" + duracao + '\'' +
                ", equipamento='" + equipamento + '\'' +
                ", qtdEscalas=" + qtdEscalas +
                ", classe='" + classe + '\'' +
                ", numeroVoo='" + numeroVoo + '\'' +
                ", cabine='" + cabine + '\'' +
                ", tipoSegmento=" + tipoSegmento +
                ", isConexao=" + isConexao +
                ", isReembolsavel=" + isReembolsavel +
                ", assentos=" + assentos +
                ", conexao=" + conexao +
                ", familia='" + familia + '\'' +
                ", familiaCodigo='" + familiaCodigo + '\'' +
                ", localizadorCia='" + localizadorCia + '\'' +
                ", status='" + status + '\'' +
                ", surface=" + surface +
                '}';
    }
}
