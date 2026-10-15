package com.confApi.db.confManager.rodoviario.dto;
import lombok.Data;
import java.util.*;
@Data
public class ReservaRodoviarioDetalheDTO {
    private ReservaRodoviarioDTO reserva;
    private List<RodoviarioTrechoDTO> trechos = new ArrayList<>();
    private List<RodoviarioPassageiroDTO> passageiros = new ArrayList<>();
    private List<RodoviarioAssentoDTO> assentos = new ArrayList<>();
    private List<RodoviarioBilheteDTO> bilhetes = new ArrayList<>();
    private RodoviarioValorDTO valores;
    private List<Map<String,Object>> recebimentos = new ArrayList<>();
}
