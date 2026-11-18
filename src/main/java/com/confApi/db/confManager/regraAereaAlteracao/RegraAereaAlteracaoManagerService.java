package com.confApi.db.confManager.regraAereaAlteracao;

import com.confApi.chatconfianca.client.ChatConfiancaManagerClient;
import com.confApi.db.confManager.regraAereaAlteracao.dto.RegraAereaAlteracaoConsultaRequest;
import com.confApi.db.confManager.regraAereaAlteracao.dto.RegraAereaAlteracaoConsultaResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class RegraAereaAlteracaoManagerService implements Serializable {

    private static final Logger LOG = Logger.getLogger(RegraAereaAlteracaoManagerService.class.getName());
    private static final String API_ACTION_CONSULTAR = "regrasAereas/alteracao/consultar";
    private static final String API_ACTION_SIMULAR = "regrasAereas/alteracao/simular";

    private final ChatConfiancaManagerClient manager;
    private final ObjectMapper mapper;

    public RegraAereaAlteracaoManagerService(ChatConfiancaManagerClient manager) {
        this.manager = manager;
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    }

    public RegraAereaAlteracaoConsultaResponse consultar(RegraAereaAlteracaoConsultaRequest request) {
        return executarConsulta(API_ACTION_CONSULTAR, request);
    }

    public RegraAereaAlteracaoConsultaResponse simular(RegraAereaAlteracaoConsultaRequest request) {
        request.setExigirRegraAprovada(true);
        return executarConsulta(API_ACTION_SIMULAR, request);
    }

    private RegraAereaAlteracaoConsultaResponse executarConsulta(String action, RegraAereaAlteracaoConsultaRequest request) {
        try {
            // Reutiliza a autenticacao do chat e sua renovacao unica em caso de HTTP 401.
            // O cliente tambem impede chamadas anonimas quando a autenticacao falha.
            String body = manager.post(action, request, String.class);
            if (body != null && !body.isBlank()) {
                RegraAereaAlteracaoConsultaResponse response =
                        mapper.readValue(body, RegraAereaAlteracaoConsultaResponse.class);
                if (response != null && response.getStatus() != null && !response.getStatus().isBlank()) {
                    return response;
                }
            }
            LOG.warning("Resposta invalida na consulta de regra aerea de alteracao/remarcacao.");
        } catch (Exception ex) {
            // Nao registrar credenciais, corpos de resposta ou dados da reserva.
            LOG.log(Level.WARNING, "Falha na consulta de regra aerea de alteracao/remarcacao: {0}",
                    ex.getClass().getSimpleName());
        }
        return fallback();
    }

    private RegraAereaAlteracaoConsultaResponse fallback() {
        RegraAereaAlteracaoConsultaResponse response = new RegraAereaAlteracaoConsultaResponse();
        response.setStatus("ERRO_CONSULTA");
        response.setMensagem("Nao foi possivel consultar regra de alteracao/remarcacao no manager.");
        return response;
    }
}
