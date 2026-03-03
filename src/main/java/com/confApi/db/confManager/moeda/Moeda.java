package com.confApi.db.confManager.moeda;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;
@Data
public class Moeda implements Serializable {

    private Integer codgMoeda;
    private String nome;
    private String sigla;

}