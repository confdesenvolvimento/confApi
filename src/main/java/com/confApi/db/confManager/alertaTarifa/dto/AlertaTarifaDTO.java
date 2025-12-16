package com.confApi.db.confManager.alertaTarifa.dto;

import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.hub.aereo.dto.Aeroporto;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Data
public class AlertaTarifaDTO implements Serializable {

    private Integer id;
    private Usuario usuario;
    private Agencia agencia;
    private com.confApi.db.confManager.aeroporto.Aeroporto origem;
    private com.confApi.db.confManager.aeroporto.Aeroporto destino;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date periodoInicio;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date periodoFinal;
    private Double valorMaximo;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date validade;
    private boolean flagEnviarEmail = true;
    private boolean flagEnviarNotificacao = true;
    private Integer status;
    private Integer codgUsuario;
    private Date minDateDataVolta;
    private Date maxValidade;
    private Date minValidade;
    private Date maxDate = null;
    private List<AlertaTarifaLogDTO> logs;
    private Double menorValorEncontrado;
    private Double maiorValorEncontrado;
    private String observacao;

    public AlertaTarifaDTO() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AlertaTarifaDTO that)) return false;
        return isFlagEnviarEmail() == that.isFlagEnviarEmail() && isFlagEnviarNotificacao() == that.isFlagEnviarNotificacao() && Objects.equals(getId(), that.getId()) && Objects.equals(getUsuario(), that.getUsuario()) && Objects.equals(getAgencia(), that.getAgencia()) && Objects.equals(getOrigem(), that.getOrigem()) && Objects.equals(getDestino(), that.getDestino()) && Objects.equals(getPeriodoInicio(), that.getPeriodoInicio()) && Objects.equals(getPeriodoFinal(), that.getPeriodoFinal()) && Objects.equals(getValorMaximo(), that.getValorMaximo()) && Objects.equals(getValidade(), that.getValidade()) && Objects.equals(getStatus(), that.getStatus()) && Objects.equals(getCodgUsuario(), that.getCodgUsuario()) && Objects.equals(getMinDateDataVolta(), that.getMinDateDataVolta()) && Objects.equals(getMaxValidade(), that.getMaxValidade()) && Objects.equals(getMinValidade(), that.getMinValidade()) && Objects.equals(getMaxDate(), that.getMaxDate()) && Objects.equals(getLogs(), that.getLogs()) && Objects.equals(getMenorValorEncontrado(), that.getMenorValorEncontrado()) && Objects.equals(getMaiorValorEncontrado(), that.getMaiorValorEncontrado()) && Objects.equals(getObservacao(), that.getObservacao());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getUsuario(), getAgencia(), getOrigem(), getDestino(), getPeriodoInicio(), getPeriodoFinal(), getValorMaximo(), getValidade(), isFlagEnviarEmail(), isFlagEnviarNotificacao(), getStatus(), getCodgUsuario(), getMinDateDataVolta(), getMaxValidade(), getMinValidade(), getMaxDate(), getLogs(), getMenorValorEncontrado(), getMaiorValorEncontrado(), getObservacao());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        //sb.append("📢ALERTA DE TARIFA Nº ").append(id != null ? id : "N/I").append("\n");

        if (origem != null && destino != null) {
            sb.append(" Rota: ")
                    .append(origem.getIataAeroporto()).append(" → ").append(destino.getIataAeroporto()).append(" (")
                    .append(origem.getNomeAeroporto()).append(" → ").append(destino.getNomeAeroporto()).append(")\n");
        }

        sb.append("Período: ")
                .append(formatarData(periodoInicio)).append(" até ").append(formatarData(periodoFinal)).append("\n");

       // sb.append("📆 Validade do alerta: ").append(formatarData(validade)).append("\n");

        if (valorMaximo != null) {
            sb.append(" Menor valor encontrado: R$ ").append(String.format("%.2f", valorMaximo)).append("\n");
        }
        if (menorValorEncontrado != null || maiorValorEncontrado != null) {
            sb.append("Faixa de valores encontrados: ");
            if (menorValorEncontrado != null) {
                sb.append("mín R$ ").append(String.format("%.2f", menorValorEncontrado)).append(" ");
            }
            if (maiorValorEncontrado != null) {
                sb.append("| máx R$ ").append(String.format("%.2f", maiorValorEncontrado));
            }
            sb.append("\n");
        }

       // sb.append("🧾 Status: ").append(obterStatusDescricao(status)).append("\n");

        if (observacao != null && !observacao.isBlank()) {
            sb.append("🗒️ Observação: ").append(observacao).append("\n");
        }

        // Exibe os logs detalhados

            if (logs != null && !logs.isEmpty()) {
                sb.append(" Tarifas Encontradas:\n");
                for (AlertaTarifaLogDTO log : logs) {
                    sb.append("      ").append(log.toString().replace("\n", "\n      ")).append("\n");
                    sb.append("\n");
                }
            } else {
                sb.append("   ⚠️ Nenhuma Tarifa encontrada até o momento.\n");
            }
            sb.append("-----------------------------------------------------\n");
        return sb.toString();
    }

    // Método auxiliar para formatar datas
    private String formatarData(Date data) {
        if (data == null) return "N/I";
        return new java.text.SimpleDateFormat("dd/MM/yyyy").format(data);
    }

    // Método auxiliar para descrever o status do alerta
    private String obterStatusDescricao(Integer status) {
        if (status == null) return "Desconhecido";
        return switch (status) {
            case 0 -> "Inativo";
            case 1 -> "Ativo";
            case 2 -> "Expirado";
            default -> "Desconhecido (" + status + ")";
        };
    }
}
