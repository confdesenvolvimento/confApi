package com.confApi.db.confManager.cambio;

import com.confApi.db.confManager.moeda.Moeda;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
@Data
public class Cambio implements Serializable {

    private Integer codgCambio;

    private Date data;

    private Double valorCotacao;

    private Moeda moeda;

    private Moeda moedaPara;
}