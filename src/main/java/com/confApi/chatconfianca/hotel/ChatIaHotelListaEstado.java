package com.confApi.chatconfianca.hotel;
import java.util.*;
import lombok.Data;

/** Server-owned cursor and candidates. Not part of the model schema. */
@Data
public class ChatIaHotelListaEstado {
    private Map<String,String> filtros=new LinkedHashMap<>();
    private Map<String,Integer> opcoes=new LinkedHashMap<>();
    private String revisao, proximaData, opcaoSelecionada;
    private Integer proximoId;
    private boolean temMais;
    private int pagina;
    public ChatIaHotelListaEstado copiar(){var s=new ChatIaHotelListaEstado();s.setFiltros(new LinkedHashMap<>(filtros));s.setOpcoes(new LinkedHashMap<>(opcoes));s.setRevisao(revisao);s.setProximaData(proximaData);s.setProximoId(proximoId);s.setTemMais(temMais);s.setPagina(pagina);s.setOpcaoSelecionada(opcaoSelecionada);return s;}
}
