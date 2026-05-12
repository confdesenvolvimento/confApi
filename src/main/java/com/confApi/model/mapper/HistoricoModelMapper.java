package com.confApi.model.mapper;



import com.confApi.db.confManager.historicoReserva.dto.HistoricoReserva;
import com.confApi.model.HistoricoReservaModel;

import java.util.ArrayList;
import java.util.List;

public class HistoricoModelMapper {

    private HistoricoModelMapper() {
        // Classe utilitária
    }

    public static HistoricoReservaModel toModel(HistoricoReserva historico) {
        HistoricoReservaModel model = new HistoricoReservaModel();

        if (historico == null) {
            return model;
        }

        model.setCodgHistoricoReserva(historico.getCodgHistoricoReserva());
        model.setCodgReservaAereo(historico.getCodgReservaAereo());
        model.setDataHoraTransacao(historico.getDataHoraTransacao());
        model.setDescricao(historico.getDescricao());
        model.setIpAcesso(historico.getIpAcesso());
        model.setFlagInterno(historico.getFlagInterno());
        model.setCodgUsuario(historico.getCodgUsuario());
        model.setCodgReservaHotel(historico.getCodgReservaHotel());
        model.setCodgReservaPacote(historico.getCodgReservaPacote());
        model.setCodgReservaSeguro(historico.getCodgReservaSeguro());

        return model;
    }

    public static HistoricoReserva toDTO(HistoricoReservaModel model) {
        HistoricoReserva historico = new HistoricoReserva();

        if (model == null) {
            return historico;
        }

        historico.setCodgHistoricoReserva(model.getCodgHistoricoReserva());
        historico.setCodgReservaAereo(model.getCodgReservaAereo());
        historico.setDataHoraTransacao(model.getDataHoraTransacao());
        historico.setDescricao(model.getDescricao());
        historico.setIpAcesso(model.getIpAcesso());
        historico.setFlagInterno(model.getFlagInterno());
        historico.setCodgUsuario(model.getCodgUsuario());
        historico.setCodgReservaHotel(model.getCodgReservaHotel());
        historico.setCodgReservaPacote(model.getCodgReservaPacote());
        historico.setCodgReservaSeguro(model.getCodgReservaSeguro());

        return historico;
    }

    public static List<HistoricoReservaModel> toModelList(List<HistoricoReserva> historicos) {
        List<HistoricoReservaModel> models = new ArrayList<>();

        if (historicos == null || historicos.isEmpty()) {
            return models;
        }

        for (HistoricoReserva historico : historicos) {
            if (historico == null) {
                continue;
            }

            models.add(toModel(historico));
        }

        return models;
    }

    public static List<HistoricoReserva> toDTOList(List<HistoricoReservaModel> models) {
        List<HistoricoReserva> historicos = new ArrayList<>();

        if (models == null || models.isEmpty()) {
            return historicos;
        }

        for (HistoricoReservaModel model : models) {
            if (model == null) {
                continue;
            }

            historicos.add(toDTO(model));
        }

        return historicos;
    }
}