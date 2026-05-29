package com.confApi.aereo.dto;

import com.confApi.hub.aereo.dto.Aeroporto;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.Date;
@Data
public class RegraTarifaria {
    @JsonAlias({"Id", "id"})
    private Integer id;
    @JsonAlias({"BaseTarifaria", "baseTarifaria"})
    private String baseTarifaria;
    @JsonAlias({"DataChegada", "dataChegada"})
    private Date dataChegada;
    @JsonAlias({"DataSaida", "dataSaida"})
    private Date dataSaida;
    @JsonAlias({"Destino", "destino"})
    private Aeroporto destino;
    @JsonAlias({"HoraChegada", "horaChegada"})
    private String horaChegada;
    @JsonAlias({"HoraSaida", "horaSaida"})
    private String horaSaida;
    @JsonAlias({"Numero", "numero"})
    private String numero;
    @JsonAlias({"Origem", "origem"})
    private Aeroporto origem;
}
