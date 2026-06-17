package com.confApi.wooba.sales;

import java.util.ArrayList;
import java.util.List;

public class WoobaReservedAirReservationPollingResult {

    private int encontradas;
    private int processadas;
    private int ignoradas;
    private int puladasCache;
    private int erros;
    private int criadas;
    private int atualizadas;
    private final List<String> uniqueIdsProcessados = new ArrayList<>();
    private final List<String> uniqueIdsComErro = new ArrayList<>();

    public int getEncontradas() {
        return encontradas;
    }

    public void setEncontradas(int encontradas) {
        this.encontradas = encontradas;
    }

    public int getProcessadas() {
        return processadas;
    }

    public void incrementarProcessadas(String uniqueId, WoobaAirReservationSyncResult syncResult) {
        this.processadas++;
        if (uniqueId != null) {
            this.uniqueIdsProcessados.add(uniqueId);
        }
        if (syncResult != null && syncResult.isCreated()) {
            this.criadas++;
        }
        if (syncResult != null && syncResult.isUpdated()) {
            this.atualizadas++;
        }
    }

    public int getIgnoradas() {
        return ignoradas;
    }

    public void incrementarIgnoradas() {
        this.ignoradas++;
    }

    public int getPuladasCache() {
        return puladasCache;
    }

    public void incrementarPuladasCache() {
        this.puladasCache++;
    }

    public int getErros() {
        return erros;
    }

    public void incrementarErros(String uniqueId) {
        this.erros++;
        if (uniqueId != null) {
            this.uniqueIdsComErro.add(uniqueId);
        }
    }

    public int getCriadas() {
        return criadas;
    }

    public int getAtualizadas() {
        return atualizadas;
    }

    public List<String> getUniqueIdsProcessados() {
        return uniqueIdsProcessados;
    }

    public List<String> getUniqueIdsComErro() {
        return uniqueIdsComErro;
    }
}
