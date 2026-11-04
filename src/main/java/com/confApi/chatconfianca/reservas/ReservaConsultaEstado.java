package com.confApi.chatconfianca.reservas;

import com.confApi.chatconfianca.dto.reserva.ReservaAereaRecenteItem;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/** Estado de consulta pertencente somente a um usuario, agencia e conversa. */
@Data
public class ReservaConsultaEstado {
    private Long conversa;
    private Long agencia;
    private Long usuario;
    private Long atualizadoEm;
    private ReservaConsultaFiltros filtros;
    private List<ReservaAereaRecenteItem> itens = new ArrayList<>();
    private Integer selecionadaId;
}
