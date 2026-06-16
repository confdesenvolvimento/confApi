package com.confApi.wooba.sales;

import com.confApi.db.confManager.reservaAereo.ReservaAereo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WoobaAirReservationSyncResult {

    private String action;
    private String reason;
    private ReservaAereo reserva;
    private boolean created;
    private boolean updated;
    private boolean issuedNotificationSent;
    private boolean createdNotificationSent;
    private List<String> bilhetesGravados = new ArrayList<>();
    private List<String> bilhetesAtualizados = new ArrayList<>();
    private int pagamentosGravados;
    private int pagamentosAtualizados;

    public static WoobaAirReservationSyncResult ignored(String reason, ReservaAereo reserva) {
        WoobaAirReservationSyncResult result = new WoobaAirReservationSyncResult();
        result.setAction("IGNORED");
        result.setReason(reason);
        result.setReserva(reserva);
        return result;
    }

    public static WoobaAirReservationSyncResult processed(ReservaAereo reserva) {
        WoobaAirReservationSyncResult result = new WoobaAirReservationSyncResult();
        result.setAction("PROCESSED");
        result.setReserva(reserva);
        return result;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public ReservaAereo getReserva() {
        return reserva;
    }

    public void setReserva(ReservaAereo reserva) {
        this.reserva = reserva;
    }

    public boolean isCreated() {
        return created;
    }

    public void setCreated(boolean created) {
        this.created = created;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }

    public boolean isIssuedNotificationSent() {
        return issuedNotificationSent;
    }

    public void setIssuedNotificationSent(boolean issuedNotificationSent) {
        this.issuedNotificationSent = issuedNotificationSent;
    }

    public boolean isCreatedNotificationSent() {
        return createdNotificationSent;
    }

    public void setCreatedNotificationSent(boolean createdNotificationSent) {
        this.createdNotificationSent = createdNotificationSent;
    }

    public List<String> getBilhetesGravados() {
        return Collections.unmodifiableList(bilhetesGravados);
    }

    public void setBilhetesGravados(List<String> bilhetesGravados) {
        this.bilhetesGravados = bilhetesGravados == null ? new ArrayList<>() : new ArrayList<>(bilhetesGravados);
    }

    public List<String> getBilhetesAtualizados() {
        return Collections.unmodifiableList(bilhetesAtualizados);
    }

    public void setBilhetesAtualizados(List<String> bilhetesAtualizados) {
        this.bilhetesAtualizados = bilhetesAtualizados == null ? new ArrayList<>() : new ArrayList<>(bilhetesAtualizados);
    }

    public int getPagamentosGravados() {
        return pagamentosGravados;
    }

    public void setPagamentosGravados(int pagamentosGravados) {
        this.pagamentosGravados = pagamentosGravados;
    }

    public int getPagamentosAtualizados() {
        return pagamentosAtualizados;
    }

    public void setPagamentosAtualizados(int pagamentosAtualizados) {
        this.pagamentosAtualizados = pagamentosAtualizados;
    }
}
