package com.confApi.db.confManager.reservaAereo;

import java.util.List;

/** Contrato de persistencia do Manager, sem pagamento ou chamadas ao fornecedor. */
public record ReservaAereoValoresRQ(String localizador, Double total, List<ValorPassageiro> passageiros,
        List<ValorPassageiro> anteriores) {
    public record ValorPassageiro(Integer codgPassageiro, Double tarifa, Double embarque,
            Double du, Double rav, Double rc, Double assento, Double total, Double tarifaNet) { }
}
