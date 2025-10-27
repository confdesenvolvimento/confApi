package com.confApi.db.confManager.faturas.dto;

import com.confApi.db.confManager.faturas.dto.model.*;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
@Data
public class DetalharFaturaRS implements Serializable {

    private static final long serialVersionUID = 1L;
    //    @JsonProperty("bilhetes")
    private List<Bilhete> bilhetes = new ArrayList<>();
    private List<ReembolsoFatura> reembolsos = new ArrayList<>();;
    private List<Ocorrencia> ocorrencias = new ArrayList<>();
    private List<NotaFiscal> notaFiscal = new ArrayList<>();
    private List<Hotel> hoteis = new ArrayList<>();
    private List<Carro> carros = new ArrayList<>();
    private List<Rodoviario> rodoviarios = new ArrayList<>();
    private List<Seguro> seguros = new ArrayList<>();
    //    private List<Object> servicos = new ArrayList<>();
    private Cabecalho cabecalho = new Cabecalho();

}
