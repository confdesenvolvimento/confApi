package com.confApi.chatconfianca.service;

import com.confApi.chatconfianca.client.ChatConfiancaManagerClient;
import com.confApi.chatconfianca.dto.reserva.ReservaAereaRecenteItem;
import com.confApi.chatconfianca.dto.reserva.ReservasAereasRecentesResponse;
import com.confApi.chatgpt.dto.ChatActionDTO;
import com.confApi.exception.RegraDeNegocioException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class ChatConfiancaReservaAereaService {
    public static final String SCHEMA_RESERVAS_RECENTES = "chat.reservas-recentes.v1";
    private static final int LIMITE_PADRAO = 10;
    private static final int LIMITE_MAXIMO = 50;
    private static final String CONSULTA_RECENTES =
            "chat-confianca/consultas/reservas-aereas/recentes";

    private final ChatConfiancaManagerClient manager;

    public ChatConfiancaReservaAereaService(ChatConfiancaManagerClient manager) {
        this.manager = manager;
    }

    public ReservasAereasRecentesResponse listarRecentes(Integer codgAgencia, Integer limit) {
        if (codgAgencia == null || codgAgencia <= 0) {
            throw new RegraDeNegocioException(400, "Informe a agencia para consultar as reservas.");
        }
        int limite = normalizarLimite(limit);
        List<ReservaAereaRecenteItem> itens = manager.getList(
                CONSULTA_RECENTES + "?codgAgencia=" + codgAgencia + "&limit=" + limite,
                new ParameterizedTypeReference<List<ReservaAereaRecenteItem>>() {
                });

        List<ReservaAereaRecenteItem> reservas = itens == null
                ? new ArrayList<>()
                : new ArrayList<>(itens);
        reservas.removeIf(Objects::isNull);
        reservas.forEach(this::preencherAcoes);

        ReservasAereasRecentesResponse response = new ReservasAereasRecentesResponse();
        response.setReservas(reservas);
        response.setQuantidade(reservas.size());
        response.setMensagem(reservas.isEmpty()
                ? "Nao foram encontradas reservas aereas recentes para esta agencia."
                : reservas.size() + (reservas.size() == 1
                ? " reserva recente encontrada."
                : " reservas recentes encontradas."));
        return response;
    }

    public List<ChatActionDTO> listarAcoes(ReservasAereasRecentesResponse response) {
        if (response == null || response.getReservas() == null) {
            return new ArrayList<>();
        }
        List<ChatActionDTO> actions = new ArrayList<>();
        for (ReservaAereaRecenteItem reserva : response.getReservas()) {
            if (reserva != null && reserva.getActions() != null) {
                actions.addAll(reserva.getActions());
            }
        }
        return actions;
    }

    private int normalizarLimite(Integer limit) {
        int valor = limit == null ? LIMITE_PADRAO : limit;
        if (valor < 1 || valor > LIMITE_MAXIMO) {
            throw new RegraDeNegocioException(400,
                    "O limite de reservas deve estar entre 1 e 50.");
        }
        return valor;
    }

    private void preencherAcoes(ReservaAereaRecenteItem reserva) {
        List<ChatActionDTO> actions = new ArrayList<>();
        if (reserva.getLocalizador() == null || reserva.getLocalizador().isBlank()) {
            reserva.setActions(actions);
            return;
        }

        String localizador = reserva.getLocalizador().trim().toUpperCase(Locale.ROOT);
        Integer reservaId = reserva.getReservaId();
        actions.add(new ChatActionDTO(
                "abrir_reserva",
                "Abrir reserva",
                "Abrir esta reserva aerea no sistema.",
                localizador,
                reservaId,
                false,
                false,
                false,
                "Abrir a reserva " + localizador + " no sistema."));
        if (reserva.isDisponivelSimulacao()) {
            actions.add(new ChatActionDTO(
                    "simular_remarcacao",
                    "Simular remarcacao",
                    "Escolher trecho e passageiros para calcular uma previa da alteracao.",
                    localizador,
                    reservaId,
                    false,
                    true,
                    false,
                    "Abra o seletor de reservas emitidas e use o localizador " + localizador
                            + " apenas para preencher a busca. Nao inicie a simulacao antes da minha selecao."));
        }

        if (reserva.isCancelamentoRequerValidacao()) {
            actions.add(new ChatActionDTO(
                    "preparar_cancelamento",
                    "Cancelar",
                    "Consultar regras e preparar a solicitacao de cancelamento.",
                    localizador,
                    reservaId,
                    true,
                    true,
                    true,
                    "Quero preparar o cancelamento da reserva " + localizador
                            + ". Consulte as regras antes. Nao execute a operacao ainda; "
                            + "explique os impactos e peca minha confirmacao explicita."));
        }

        actions.add(new ChatActionDTO(
                "consultar_regras",
                "Visualizar regras",
                "Consultar multas, reembolso, cancelamento e alteracao/remarcacao.",
                localizador,
                reservaId,
                false,
                true,
                false,
                "Consulte as regras, multas, reembolso, cancelamento e alteracao/remarcacao "
                        + "da reserva " + localizador + "."));

        reserva.setActions(actions);
    }
}
