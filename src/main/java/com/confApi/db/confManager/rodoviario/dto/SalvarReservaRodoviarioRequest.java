package com.confApi.db.confManager.rodoviario.dto;
import lombok.Data;
import java.util.*;
@Data
public class SalvarReservaRodoviarioRequest {
    private String chaveIdempotencia;
    private ReservaRodoviarioDTO reserva;
    private List<TrechoCompleto> trechos = new ArrayList<>();
    private RodoviarioValorDTO valores;
    private List<Integer> recebimentoIds = new ArrayList<>();
    @Data public static class TrechoCompleto {
        private RodoviarioTrechoDTO trecho;
        private List<PassageiroCompleto> passageiros = new ArrayList<>();
    }
    @Data public static class PassageiroCompleto {
        private RodoviarioPassageiroDTO passageiro;
        private RodoviarioAssentoDTO assento;
        private List<RodoviarioBilheteDTO> bilhetes = new ArrayList<>();
    }
}
