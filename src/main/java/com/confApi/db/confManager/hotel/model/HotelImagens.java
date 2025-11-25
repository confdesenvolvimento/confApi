package com.confApi.db.confManager.hotel.model;

import java.io.Serializable;
import java.util.Objects;

public class HotelImagens implements Serializable {
    private Integer codgHotelImagem;
    private String urlImg;
    private String nome;
    private String descricao;

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.codgHotelImagem);
        hash = 89 * hash + Objects.hashCode(this.urlImg);
        hash = 89 * hash + Objects.hashCode(this.nome);
        hash = 89 * hash + Objects.hashCode(this.descricao);
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
        final HotelImagens other = (HotelImagens) obj;
        if (!Objects.equals(this.urlImg, other.urlImg)) {
            return false;
        }
        if (!Objects.equals(this.nome, other.nome)) {
            return false;
        }
        if (!Objects.equals(this.descricao, other.descricao)) {
            return false;
        }
        return Objects.equals(this.codgHotelImagem, other.codgHotelImagem);
    }



    public String getUrlImg() {
        return urlImg;
    }

    public void setUrlImg(String urlImg) {
        this.urlImg = urlImg;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Integer getCodgHotelImagem() {
        return codgHotelImagem;
    }

    public void setCodgHotelImagem(Integer codgHotelImagem) {
        this.codgHotelImagem = codgHotelImagem;
    }

    @Override
    public String toString() {
        return "HotelImagens{" + "codgHotelImagem=" + codgHotelImagem + ", urlImg=" + urlImg + ", nome=" + nome + ", descricao=" + descricao + '}';
    }



}

