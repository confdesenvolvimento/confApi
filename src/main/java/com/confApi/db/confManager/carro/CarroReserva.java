package com.confApi.db.confManager.carro;

import com.confApi.carros.dto.CarroBookingHub;
import com.confApi.carros.dto.CarroBookingLojaHub;
import com.confApi.carros.dto.RegrasCancelamento;
import com.confApi.carros.dto.ReservarCarroResponseDTO;
import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.db.confManager.sistema.Sistema;
import com.confApi.db.confManager.usuario.Usuario;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@NoArgsConstructor
public class CarroReserva {

    private Usuario usuario;
    private String localizadorSistema;
    private String localizadorLocadora;
    private String voucher;
    private Sistema sistema;
    private Agencia agencia;
    private Integer statusReserva;
    private Integer statusPagamentoFornecedor;
    private Integer statusPagamentoCliente;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataEmissao;
    private LocalDateTime prazoEmissaoCliente;
    private LocalDateTime dataCancelamento;
    private Usuario usuarioCancelamento;
    private Double valorTotalReservaNet;
    private Double valorTotalReservaMarkup;
    private LocalDateTime prazoCancelamento;
    private String observacaoInterna;
    private String observacaoPublica;
    private LocalDateTime dataRetirada;
    private LocalDateTime dataRetorno;
    private String horaRetirada;
    private String horaRetorno;
    private String descricaoMotivoCancelamento;
    private Double valorMultaCancelamento;

    private String codgLojaRetirada;
    private String cidadeRetirada;
    private String descricaoLojaRetirada;

    private String codgLojaDevolucao;
    private String cidadeDevolucao;
    private String descricaoLojaDevolucao;

    private String descricaoEnderecoRetirada;
    private String descricaoEnderecoDevolucao;

    private String fonte;

    private List<Recebimento> recebimentos = new ArrayList<>();

    public CarroReserva(ReservarCarroResponseDTO obj) {
        CarroBookingHub reserva = obj != null ? obj.getReservaCarro() : null;
        RegrasCancelamento regras = obj != null ? obj.getRegrasCancelamento() : null;

        CarroBookingLojaHub lojaRetirada = buscarLoja(reserva, true);
        CarroBookingLojaHub lojaDevolucao = buscarLoja(reserva, false);

        this.usuario = null;
        this.localizadorSistema = reserva != null ? reserva.getBookingID() : null;
        this.localizadorLocadora = reserva != null ? reserva.getBookingID() : null;
        this.voucher = reserva != null ? reserva.getVoucher() : null;
        this.sistema = null;
        this.agencia = null;

        this.statusReserva = reserva != null ? mapStatus(reserva.getBookingStatus()) : null;
        this.statusPagamentoFornecedor = reserva != null ? mapStatus(reserva.getPagamentoStatus()) : null;

        this.statusPagamentoCliente = null;

        LocalDateTime agora = LocalDateTime.now();
        this.dataCriacao = agora;
        this.dataEmissao = agora;

        this.prazoEmissaoCliente = null;
        this.dataCancelamento = null;
        this.usuarioCancelamento = null;

        this.valorTotalReservaNet = reserva != null ? reserva.getValorTotalPagamentoEquivalente() : null;
        this.valorTotalReservaMarkup = null;

        this.prazoCancelamento = null;

        this.observacaoInterna = obj != null ? obj.getRestrictionOfTermsAndConditions() : null;
        this.observacaoPublica = obj != null ? obj.getLocalPaymentAlert() : null;

        this.dataRetirada = reserva != null
                ? parseLocalDateTime(reserva.getDataRetirada(), reserva.getHoraRetirada())
                : null;

        this.dataRetorno = reserva != null
                ? parseLocalDateTime(reserva.getDataRetorno(), reserva.getHoraRetorno())
                : null;

        this.horaRetirada = reserva != null ? reserva.getHoraRetirada() : null;
        this.horaRetorno = reserva != null ? reserva.getHoraRetorno() : null;

        this.descricaoMotivoCancelamento = null;
        this.valorMultaCancelamento = regras != null ? regras.getValorCancelamento() : null;

        this.codgLojaRetirada = lojaRetirada != null ? lojaRetirada.getCodigo() : null;
        this.codgLojaDevolucao = lojaDevolucao != null ? lojaDevolucao.getCodigo() : null;

        this.cidadeRetirada = lojaRetirada != null ? lojaRetirada.getCidade() : null;
        this.cidadeDevolucao = lojaDevolucao != null ? lojaDevolucao.getCidade() : null;

        this.descricaoLojaRetirada = lojaRetirada != null ? lojaRetirada.getHorasAbertas() : null;
        this.descricaoLojaDevolucao = lojaDevolucao != null ? lojaDevolucao.getHorasAbertas() : null;

        this.descricaoEnderecoRetirada = lojaRetirada != null ? lojaRetirada.getEndereco() : null;
        this.descricaoEnderecoDevolucao = lojaDevolucao != null ? lojaDevolucao.getEndereco() : null;
    }

    private static CarroBookingLojaHub buscarLoja(CarroBookingHub reserva, boolean lojaRetirada) {
        if (reserva == null || reserva.getLojas() == null) {
            return null;
        }

        for (CarroBookingLojaHub loja : reserva.getLojas()) {
            if (loja != null && Boolean.valueOf(lojaRetirada).equals(loja.getLojaRetirada())) {
                return loja;
            }
        }

        return null;
    }

    private static LocalDateTime parseLocalDateTime(String data, String hora) {
        if (data == null || data.trim().isEmpty()) {
            return null;
        }

        String dataTratada = data.trim();

        if (dataTratada.contains("T")) {
            try {
                return LocalDateTime.parse(dataTratada);
            } catch (DateTimeParseException ignored) {
            }
        }

        String horaTratada = hora == null || hora.trim().isEmpty()
                ? "00:00"
                : hora.trim();

        if (horaTratada.length() > 8) {
            horaTratada = horaTratada.substring(0, 8);
        }

        String dataHora = dataTratada + " " + horaTratada;

        List<DateTimeFormatter> formatters = Arrays.asList(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(dataHora, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        throw new IllegalArgumentException("Data/hora inválida: " + dataHora);
    }

    private static Integer mapStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }

        String statusTratado = status.trim();

        if ("Confirmed".equalsIgnoreCase(statusTratado)
                || "Confirmado".equalsIgnoreCase(statusTratado)) {
            return 1;
        }

        if ("Cancelled".equalsIgnoreCase(statusTratado)
                || "Canceled".equalsIgnoreCase(statusTratado)
                || "Cancelado".equalsIgnoreCase(statusTratado)) {
            return 2;
        }

        return null;
    }
}
