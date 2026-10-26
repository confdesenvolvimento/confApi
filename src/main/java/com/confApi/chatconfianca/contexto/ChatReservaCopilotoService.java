package com.confApi.chatconfianca.contexto;

import com.confApi.chatconfianca.dto.reserva.ReservaAereaRecenteItem;
import com.confApi.chatgpt.dto.ChatActionDTO;
import com.confApi.chatgpt.dto.ChatActionDestinoDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Sugestoes deterministicas; nao consulta, nao executa e nao confere autorizacao. */
@Service
public class ChatReservaCopilotoService {
    @Value("${chat-confianca.copiloto-proximas-acoes.enabled:true}")
    private boolean enabled = true;

    public boolean habilitado() { return enabled; }

    /** O caller fornece somente um registro recem-consultado e autorizado da agencia. */
    public List<ChatActionDTO> acoes(ReservaAereaRecenteItem item, ChatContextoClienteDTO contexto) {
        if (!enabled || item == null || item.getReservaId() == null || item.getReservaId() <= 0
                || item.getLocalizador() == null || !item.getLocalizador().trim().matches("[A-Za-z0-9]{5,30}")) return List.of();
        if (!ChatContextoClienteContrato.suporta(contexto, "ABRIR_RESERVA")) return List.of();
        String localizador = item.getLocalizador().trim().toUpperCase(Locale.ROOT);
        List<ChatActionDTO> acoes = new ArrayList<>(4);
        acoes.add(acao(item, localizador, "abrir_reserva", "Abrir reserva",
                "Abrir o registro consultado no sistema. Nenhuma operacao sera concluida pelo chat.", false, false));
        if (!ChatContextoClienteContrato.suportaCopiloto(contexto)) return List.copyOf(acoes);
        acoes.add(acao(item, localizador, "consultar_regras", "Consultar regras",
                "Consultar as regras desta reserva antes de decidir sobre alteracao, cancelamento ou reembolso.", false, true));
        if (item.isDisponivelSimulacao() && Integer.valueOf(3).equals(item.getStatus()) && item.getDataCancelamento() == null) {
            acoes.add(acao(item, localizador, "simular_remarcacao", "Simular remarcacao",
                    "Abrir o seletor da reserva. Trechos, passageiros e regras serao revalidados antes da previa; nao altera nem cobra.", false, true));
        }
        acoes.add(acao(item, localizador, "FALAR_ATENDENTE", "Falar com atendente",
                "Confirmar ou escolher o departamento antes de encaminhar a conversa para atendimento humano.", true, false));
        return List.copyOf(acoes);
    }

    private static ChatActionDTO acao(ReservaAereaRecenteItem item, String localizador, String codigo,
            String titulo, String descricao, boolean confirmar, boolean regras) {
        ChatRecursoReferenciaDTO recurso = new ChatRecursoReferenciaDTO();
        recurso.setTipo("RESERVA_AEREA");
        recurso.setId(Integer.toString(item.getReservaId()));
        ChatActionDestinoDTO destino = new ChatActionDestinoDTO();
        destino.setTela("RESERVA_AEREA");
        destino.setRecurso(recurso);
        // Nunca usa texto do modelo como prompt, URL, nome de metodo ou executor.
        return new ChatActionDTO(codigo, titulo, descricao, localizador, item.getReservaId(),
                confirmar, regras, false, null, ChatContextoClienteContrato.validarDestino(destino));
    }
}
