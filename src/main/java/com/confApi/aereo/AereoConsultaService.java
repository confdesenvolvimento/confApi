package com.confApi.aereo;

import com.confApi.aereo.dto.*;
import com.confApi.chatconfianca.client.ChatConfiancaManagerClient;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Coordena a leitura no fornecedor e o envio dos precos ao Manager. */
@Service
public class AereoConsultaService {
    private static final Logger LOG = LoggerFactory.getLogger(AereoConsultaService.class);
    private final AereoClient aereo;
    private final ChatConfiancaManagerClient manager;
    public AereoConsultaService(AereoClient aereo, ChatConfiancaManagerClient manager) { this.aereo = aereo; this.manager = manager; }

    public ConsultarLocalizadorResponse carregarReserva(ConsultarLocalizadorRequest req) {
        boolean azul = req != null && "Azul".equalsIgnoreCase(req.getSistema());
        ReservaAereo snapshot = null;
        if (azul) {
            try {
                String loc = req.getLocalizador() == null ? "" : req.getLocalizador().trim().toUpperCase(Locale.ROOT);
                exigir(loc.matches("[A-Z0-9]{6,10}"), "LOCALIZADOR_INVALIDO");
                snapshot = manager.get("reservaAereo/localizador/" + loc, ReservaAereo.class);
                exigir(snapshot != null && loc.equalsIgnoreCase(snapshot.getLocalizador()), "REFERENCIA_LOCAL_AUSENTE_OU_DIVERGENTE");
                // O contrato de consulta do Front usa codgAgencia para o codigo de backoffice.
                // O ID interno do Manager pertence a outro dominio de identificadores.
                String backoffice = snapshot.getCodgAgencia() == null ? null : snapshot.getCodgAgencia().getCodgSistemaBackOffice();
                exigir(backoffice != null && !backoffice.isBlank() && req.getAgencia() != null
                        && backoffice.trim().equals(req.getAgencia().getCodgAgencia()), "AGENCIA_BACKOFFICE_DIVERGENTE");
                exigir(snapshot.getCodgSistema() != null && ("Azul".equalsIgnoreCase(snapshot.getCodgSistema().getNomeSistema())
                        || "Azul".equalsIgnoreCase(snapshot.getCodgSistema().getSiglaSistema())), "SISTEMA_DIVERGENTE");
                exigir(Integer.valueOf(1).equals(snapshot.getStatus()) || Integer.valueOf(3).equals(snapshot.getStatus()), "STATUS_NAO_ELEGIVEL");
            } catch (RuntimeException ex) {
                LOG.warn("Consulta Azul sem referencia local para atualizar precos: tipo={}, motivo={}", ex.getClass().getSimpleName(),
                        ex instanceof ValidacaoConsultaException v ? v.codigo : "FALHA_LEITURA_MANAGER"); snapshot = null;
            }
        }
        ConsultarLocalizadorResponse resposta = aereo.carregarReserva(req);
        if (!azul || resposta == null) return resposta;
        // Nao confiar em metadados vindos do Hub: a ConfAPI confirma sua propria gravacao.
        resposta.setValoresAtualizados(false); resposta.setValoresSincronizados(false);
        if (snapshot == null) return resposta;
        try {
            exigir(resposta.getException() == null && resposta.getReservas() != null && resposta.getReservas().size() == 1);
            var fornecedor = resposta.getReservas().get(0);
            exigir(fornecedor != null && "Azul".equalsIgnoreCase(fornecedor.getSistema()));
            var pedido = ValoresConsultaAzul.preparar(fornecedor, snapshot);
            if (ValoresConsultaAzul.diverge(pedido)) {
                manager.put("reservaAereo/atualizarValoresFornecedor/" + snapshot.getCodgReservaAereo(), pedido);
                resposta.setValoresAtualizados(true);
            }
            resposta.setValoresSincronizados(true);
        } catch (RuntimeException ex) {
            LOG.warn("Atualizacao dos precos Azul pendente: reserva={}, tipo={}", snapshot.getCodgReservaAereo(), ex.getClass().getSimpleName());
        }
        return resposta;
    }
    private static void exigir(boolean c) { if (!c) throw new IllegalArgumentException("Reserva, agencia ou consulta divergente."); }
    private static void exigir(boolean c, String codigo) { if (!c) throw new ValidacaoConsultaException(codigo); }
    private static final class ValidacaoConsultaException extends IllegalArgumentException {
        private final String codigo;
        private ValidacaoConsultaException(String codigo) { super(codigo); this.codigo = codigo; }
    }
}
