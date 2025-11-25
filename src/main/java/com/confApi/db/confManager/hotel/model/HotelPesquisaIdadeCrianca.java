package com.confApi.db.confManager.hotel.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class HotelPesquisaIdadeCrianca implements Serializable {
    private int id = 0;
    private Integer idadeCrianca = 0;

}

