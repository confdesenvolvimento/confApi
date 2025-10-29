package com.confApi.db.wooba.checkin.dto.ia;

import lombok.Data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Data
public class CheckinIAResponse {


    private String descricao = "Lista com as reservas com embarques proximos, para informar aos passageiros PAX para fazerem o check-in";
    private List<ReservaCheckInIA> reservaCheckInIA;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Descrição: ").append(descricao).append("\n");

        if (reservaCheckInIA != null && !reservaCheckInIA.isEmpty()) {
            sb.append("Reservas com embarques nas proximas 72 horas (Check-in):\n");
            for (ReservaCheckInIA reserva : reservaCheckInIA) {
                //sb.append("  - ID: ").append(reserva.getId()).append("\n")
                sb.append("    Trecho: ").append(reserva.getTrecho()).append("\n")
                        //.append("    Agência: ").append(reserva.getNomeAgencia()).append("\n")
                        .append("    Localizador: ").append(reserva.getLocalizadorCompanhia()).append("\n")
                        .append("    Reserva: ").append(reserva.getLocalizadorCompanhia()).append("\n")
                        .append("    Número do Bilhete: ").append(reserva.getNumeroDoBilhete()).append("\n")
                        .append("    Companhia: ").append(reserva.getNomeCia()).append("\n")
                        .append("    Nome Passageiro PAX: ").append(reserva.getPassageiro().replace("\\", "/")).append("\n")
                        // .append("    Passageiro: ").append(reserva.getPassageiro()).append("\n")
                        .append("    Data Embarque: ").append(ajustaData(reserva.getData())).append("\n")
                        .append("    Trechos:\n");

                if (reserva.getTrechosMultiplaConexao() != null && !reserva.getTrechosMultiplaConexao().isEmpty()) {
                    for (TrechoCheckInIA trecho : reserva.getTrechosMultiplaConexao()) {
                        sb.append("      * De: ").append(trecho.getDe())
                                .append(" para ").append(trecho.getPara()).append("\n")
                                .append("        Companhia: ").append(trecho.getCompanhia()).append("\n")
                                .append("        Duração do voo: ").append(trecho.getDuracaoVoo()).append("\n")
                                .append("        Voo: ").append(trecho.getVoo()).append("\n")
                                .append("        Horário: ").append(trecho.getHora()).append("\n")
                                .append("        Data do voo: ").append(ajustaData(trecho.getData())).append("\n");
                    }
                } else {
                    sb.append("      (Voo sem conexão)\n");
                }

                sb.append("\n");
            }
        } else {
            sb.append("Nenhuma reserva encontrada.\n");
        }

        return sb.toString();
    }

    private String gerarCommandLink(String url) {
        if (url == null || url.isEmpty()) {
            return "Sem link disponível";
        }
        return "<h:commandLink action=\"#{chatBean.redirecionarParaLink('" + url + "')}\" value=\"Clique aqui para fazer o Check-in\" />";
    }

    public String formatLink(String message) {
        // Procura padrões de Markdown e substitui por HTML
        return message.replaceAll("\\[([^\\]]+)\\]\\(([^\\)]+)\\)", "<a href=\"$2\" target=\"_blank\">$1</a>");
    }

    public String ajustaData(Long dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        // Converter o timestamp para Date
        Date date = new Date(dateStr);

        // Formatar a data
        String formattedDate = sdf.format(date);

        return formattedDate;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public List<ReservaCheckInIA> getReservaCheckInIA() {
        return reservaCheckInIA;
    }

    public void setReservaCheckInIA(List<ReservaCheckInIA> reservaCheckInIA) {
        this.reservaCheckInIA = reservaCheckInIA;
    }

}
