package com.confApi.db.confManager.hotel.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

public class ReservasHotelExibicao implements Serializable {

    private String unidade;
    private String agencia;
    private String localizador;
    private String identificacaoHotelSistema;
    private String codgUsuario;
    private String codgAgencia;
    private String nomeHotel;
    private Integer sistema;
    private String nomeFornecedor;
    private String prazoPagamentoCliente;
    private Integer numeroDeQuartos;
    private Integer qtdeAdultos;
    private Integer qtdeCriancas;
    private Integer qtdeBebes;
    private Integer quantidadeDiarias;
    private Integer pago;
    private Integer status;
    private String checkIn;
    private String checkOut;
    private Double valorDiaria;
    private Double totalDeDiarias;
    private Double totalDeDiariasNet;
    private Double diariaNet;
    private Double vlrMkpPercentual;
    private Double taxas;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date criadaEm;
    private String limiteParaEmissao;
    private String fonte;
    private String hospede;

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 61 * hash + Objects.hashCode(this.unidade);
        hash = 61 * hash + Objects.hashCode(this.agencia);
        hash = 61 * hash + Objects.hashCode(this.localizador);
        hash = 61 * hash + Objects.hashCode(this.identificacaoHotelSistema);
        hash = 61 * hash + Objects.hashCode(this.codgUsuario);
        hash = 61 * hash + Objects.hashCode(this.codgAgencia);
        hash = 61 * hash + Objects.hashCode(this.nomeHotel);
        hash = 61 * hash + Objects.hashCode(this.sistema);
        hash = 61 * hash + Objects.hashCode(this.nomeFornecedor);
        hash = 61 * hash + Objects.hashCode(this.prazoPagamentoCliente);
        hash = 61 * hash + Objects.hashCode(this.numeroDeQuartos);
        hash = 61 * hash + Objects.hashCode(this.qtdeAdultos);
        hash = 61 * hash + Objects.hashCode(this.qtdeCriancas);
        hash = 61 * hash + Objects.hashCode(this.qtdeBebes);
        hash = 61 * hash + Objects.hashCode(this.quantidadeDiarias);
        hash = 61 * hash + Objects.hashCode(this.pago);
        hash = 61 * hash + Objects.hashCode(this.status);
        hash = 61 * hash + Objects.hashCode(this.checkIn);
        hash = 61 * hash + Objects.hashCode(this.checkOut);
        hash = 61 * hash + Objects.hashCode(this.valorDiaria);
        hash = 61 * hash + Objects.hashCode(this.totalDeDiarias);
        hash = 61 * hash + Objects.hashCode(this.totalDeDiariasNet);
        hash = 61 * hash + Objects.hashCode(this.diariaNet);
        hash = 61 * hash + Objects.hashCode(this.vlrMkpPercentual);
        hash = 61 * hash + Objects.hashCode(this.taxas);
        hash = 61 * hash + Objects.hashCode(this.criadaEm);
        hash = 61 * hash + Objects.hashCode(this.limiteParaEmissao);
        hash = 61 * hash + Objects.hashCode(this.fonte);
        hash = 61 * hash + Objects.hashCode(this.hospede);
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
        final ReservasHotelExibicao other = (ReservasHotelExibicao) obj;
        if (!Objects.equals(this.unidade, other.unidade)) {
            return false;
        }
        if (!Objects.equals(this.agencia, other.agencia)) {
            return false;
        }
        if (!Objects.equals(this.localizador, other.localizador)) {
            return false;
        }
        if (!Objects.equals(this.identificacaoHotelSistema, other.identificacaoHotelSistema)) {
            return false;
        }
        if (!Objects.equals(this.codgUsuario, other.codgUsuario)) {
            return false;
        }
        if (!Objects.equals(this.codgAgencia, other.codgAgencia)) {
            return false;
        }
        if (!Objects.equals(this.nomeHotel, other.nomeHotel)) {
            return false;
        }
        if (!Objects.equals(this.nomeFornecedor, other.nomeFornecedor)) {
            return false;
        }
        if (!Objects.equals(this.prazoPagamentoCliente, other.prazoPagamentoCliente)) {
            return false;
        }
        if (!Objects.equals(this.checkIn, other.checkIn)) {
            return false;
        }
        if (!Objects.equals(this.checkOut, other.checkOut)) {
            return false;
        }
        if (!Objects.equals(this.limiteParaEmissao, other.limiteParaEmissao)) {
            return false;
        }
        if (!Objects.equals(this.fonte, other.fonte)) {
            return false;
        }
        if (!Objects.equals(this.hospede, other.hospede)) {
            return false;
        }
        if (!Objects.equals(this.sistema, other.sistema)) {
            return false;
        }
        if (!Objects.equals(this.numeroDeQuartos, other.numeroDeQuartos)) {
            return false;
        }
        if (!Objects.equals(this.qtdeAdultos, other.qtdeAdultos)) {
            return false;
        }
        if (!Objects.equals(this.qtdeCriancas, other.qtdeCriancas)) {
            return false;
        }
        if (!Objects.equals(this.qtdeBebes, other.qtdeBebes)) {
            return false;
        }
        if (!Objects.equals(this.quantidadeDiarias, other.quantidadeDiarias)) {
            return false;
        }
        if (!Objects.equals(this.pago, other.pago)) {
            return false;
        }
        if (!Objects.equals(this.status, other.status)) {
            return false;
        }
        if (!Objects.equals(this.valorDiaria, other.valorDiaria)) {
            return false;
        }
        if (!Objects.equals(this.totalDeDiarias, other.totalDeDiarias)) {
            return false;
        }
        if (!Objects.equals(this.totalDeDiariasNet, other.totalDeDiariasNet)) {
            return false;
        }
        if (!Objects.equals(this.diariaNet, other.diariaNet)) {
            return false;
        }
        if (!Objects.equals(this.vlrMkpPercentual, other.vlrMkpPercentual)) {
            return false;
        }
        if (!Objects.equals(this.taxas, other.taxas)) {
            return false;
        }
        return Objects.equals(this.criadaEm, other.criadaEm);
    }




    public ReservasHotelExibicao() {
    }

    public String getNomeFornecedor() {
        return nomeFornecedor;
    }

    public void setNomeFornecedor(String nomeFornecedor) {
        this.nomeFornecedor = nomeFornecedor;
    }

    public String getUnidade() {
        return unidade;
    }

    public void setUnidade(String unidade) {
        this.unidade = unidade;
    }

    public String getAgencia() {
        return agencia;
    }

    public void setAgencia(String agencia) {
        this.agencia = agencia;
    }

    public String getLocalizador() {
        return localizador;
    }

    public void setLocalizador(String localizador) {
        this.localizador = localizador;
    }

    public String getIdentificacaoHotelSistema() {
        return identificacaoHotelSistema;
    }

    public void setIdentificacaoHotelSistema(String identificacaoHotelSistema) {
        this.identificacaoHotelSistema = identificacaoHotelSistema;
    }

    public String getCodgUsuario() {
        return codgUsuario;
    }

    public void setCodgUsuario(String codgUsuario) {
        this.codgUsuario = codgUsuario;
    }

    public String getCodgAgencia() {
        return codgAgencia;
    }

    public void setCodgAgencia(String codgAgencia) {
        this.codgAgencia = codgAgencia;
    }

    public String getNomeHotel() {
        return nomeHotel;
    }

    public void setNomeHotel(String nomeHotel) {
        this.nomeHotel = nomeHotel;
    }

    public Integer getSistema() {
        return sistema;
    }

    public void setSistema(Integer sistema) {
        this.sistema = sistema;
    }

    public Integer getNumeroDeQuartos() {
        return numeroDeQuartos;
    }

    public void setNumeroDeQuartos(Integer numeroDeQuartos) {
        this.numeroDeQuartos = numeroDeQuartos;
    }

    public Integer getQtdeAdultos() {
        return qtdeAdultos;
    }

    public void setQtdeAdultos(Integer qtdeAdultos) {
        this.qtdeAdultos = qtdeAdultos;
    }

    public Integer getQtdeCriancas() {
        return qtdeCriancas;
    }

    public void setQtdeCriancas(Integer qtdeCriancas) {
        this.qtdeCriancas = qtdeCriancas;
    }

    public Integer getQtdeBebes() {
        return qtdeBebes;
    }

    public void setQtdeBebes(Integer qtdeBebes) {
        this.qtdeBebes = qtdeBebes;
    }

    public Integer getQuantidadeDiarias() {
        return quantidadeDiarias;
    }

    public void setQuantidadeDiarias(Integer quantidadeDiarias) {
        this.quantidadeDiarias = quantidadeDiarias;
    }

    public Integer getPago() {
        return pago;
    }

    public void setPago(Integer pago) {
        this.pago = pago;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(String checkIn) {
        this.checkIn = checkIn;
    }

    public String getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(String checkOut) {
        this.checkOut = checkOut;
    }

    public Double getValorDiaria() {
        return valorDiaria;
    }

    public void setValorDiaria(Double valorDiaria) {
        this.valorDiaria = valorDiaria;
    }

    public Double getTotalDeDiarias() {
        return totalDeDiarias;
    }

    public void setTotalDeDiarias(Double totalDeDiarias) {
        this.totalDeDiarias = totalDeDiarias;
    }

    public Double getTotalDeDiariasNet() {
        return totalDeDiariasNet;
    }

    public void setTotalDeDiariasNet(Double totalDeDiariasNet) {
        this.totalDeDiariasNet = totalDeDiariasNet;
    }

    public Double getDiariaNet() {
        return diariaNet;
    }

    public void setDiariaNet(Double diariaNet) {
        this.diariaNet = diariaNet;
    }

    public Double getVlrMkpPercentual() {
        return vlrMkpPercentual;
    }

    public void setVlrMkpPercentual(Double vlrMkpPercentual) {
        this.vlrMkpPercentual = vlrMkpPercentual;
    }

    public Double getTaxas() {
        return taxas;
    }

    public void setTaxas(Double taxas) {
        this.taxas = taxas;
    }

    public String getPrazoPagamentoCliente() {
        return prazoPagamentoCliente;
    }

    public void setPrazoPagamentoCliente(String prazoPagamentoCliente) {
        this.prazoPagamentoCliente = prazoPagamentoCliente;
    }

    public Date getCriadaEm() {
        return criadaEm;
    }

    public void setCriadaEm(Date criadaEm) {
        this.criadaEm = criadaEm;
    }

    public String getLimiteParaEmissao() {
        return limiteParaEmissao;
    }

    public void setLimiteParaEmissao(String limiteParaEmissao) {
        this.limiteParaEmissao = limiteParaEmissao;
    }

    public String getHospede() {
        return hospede;
    }

    public void setHospede(String hospede) {
        this.hospede = hospede;
    }

    public String getFonte() {
        return fonte;
    }

    public void setFonte(String fonte) {
        this.fonte = fonte;
    }

}

