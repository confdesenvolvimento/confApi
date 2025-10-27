package com.confApi.db.confManager.db.wooba.checkin.dto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class CheckinRQ  {

    private String dataForm;
    private String dataTo;
    private String idErp;

    public CheckinRQ(String idErp, Integer tipoConsulta) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        if (tipoConsulta == 1) {
            this.dataForm = dateFormat.format(new Date());
            this.dataTo = dateFormat.format(new Date());
            this.idErp = idErp;
        } else if (tipoConsulta == 2) {
            //this.dataForm = adicionar3Dias();
            this.dataForm = dateFormat.format(new Date());
            this.dataTo = calcularDataApos3Dias(dataForm);
            this.idErp = idErp;
        }
    }

    public static String adicionar3Dias() {
        try {
            // Converte a string de data para um objeto Date
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            Date dataConvertida = new Date();
            // Cria um objeto de calendário com a data convertida
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(dataConvertida);
            // Adiciona 3 dias ao calendário
            calendar.add(Calendar.DAY_OF_MONTH, 3);
            // Obtém a nova data após adicionar 3 dias
            Date novaData = calendar.getTime();
            // Formata a nova data como uma string no formato "yyyy-MM-dd"
            String novaDataFormatada = dateFormat.format(novaData);
            return novaDataFormatada;
        } catch (Exception e) {
            // Lida com qualquer exceção de formatação de data
            e.printStackTrace();
            return null;
        }
    }

    public static String calcularDataApos3Dias(String dataBaseString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        try {
            // Converte a String para Date
            Date dataBase = dateFormat.parse(dataBaseString);
            // Cria um objeto Calendar e configura a dataBase
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(dataBase);
            // Adiciona 3 dias ao calendário
            calendar.add(Calendar.DAY_OF_MONTH, 3);
            // Obtém a nova data após 3 dias
            Date novaData = calendar.getTime();
            // Converte a novaData para String
            return dateFormat.format(novaData);
        } catch (ParseException e) {
            // Trate a exceção de parse aqui, se necessário
            e.printStackTrace();
            return null;
        }
    }

    public String getDataForm() {
        return dataForm;
    }

    public void setDataForm(String dataForm) {
        this.dataForm = dataForm;
    }

    public String getDataTo() {
        return dataTo;
    }

    public void setDataTo(String dataTo) {
        this.dataTo = dataTo;
    }

    public String getIdErp() {
        return idErp;
    }

    public void setIdErp(String idErp) {
        this.idErp = idErp;
    }

}
