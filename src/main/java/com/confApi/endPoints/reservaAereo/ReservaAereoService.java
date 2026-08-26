package com.confApi.endPoints.reservaAereo;

import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.hub.aereo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReservaAereoService {
    private static final String LOGIN_USUARIO_TECNICO = "api.confplus";

    @Autowired
    private ReservaAereoApi reservaAereoApi;

    public ReservaAereoResponse consultarLocalizador(ReservaAereoConsultarLocalizadorRequest obj) {
        ConsultarLocalizadorRequestHub consultarLocalizadorRequestHub = new ConsultarLocalizadorRequestHub(obj);
        ConsultarLocalizadorResponseHub pesquisaResponseHubList = reservaAereoApi.reservaAereoConsultaLocalizadorHub(consultarLocalizadorRequestHub);
        ReservaAereo pesquisaResponseDb = reservaAereoApi.reservaAereoConsultaLocalizadorDb(obj.getLocalizador());
        ReservaAereoResponse reservaAereoResponse = new ReservaAereoResponse(pesquisaResponseHubList, pesquisaResponseDb);

        return reservaAereoResponse;
    }

    public Boolean consultarGrupo(Integer codgReservaAereo) {
        return reservaAereoApi.consultarGrupo(codgReservaAereo);
    }

    public Boolean consultarPermissaoGrupo(Integer codgUsuarioSolicitante, String loginUsuarioSolicitante) {
        validarChamadaTecnica();
        if (codgUsuarioSolicitante == null
                || loginUsuarioSolicitante == null
                || loginUsuarioSolicitante.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário solicitante não informado.");
        }
        return reservaAereoApi.consultarPermissaoGrupo(
                codgUsuarioSolicitante, loginUsuarioSolicitante.trim());
    }

    public void atualizarGrupo(Integer codgReservaAereo, GrupoReservaAereoRequest request) {
        validarChamadaTecnica();
        if (request == null
                || request.getCodgUsuarioSolicitante() == null
                || request.getLoginUsuarioSolicitante() == null
                || request.getLoginUsuarioSolicitante().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário solicitante não informado.");
        }
        reservaAereoApi.atualizarGrupo(codgReservaAereo, request);
    }

    private void validarChamadaTecnica() {
        org.springframework.security.core.Authentication autenticacao =
                SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chamada não autenticada.");
        }
        if (autenticacao.getName() == null
                || !LOGIN_USUARIO_TECNICO.equalsIgnoreCase(autenticacao.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Aplicação não autorizada para esta operação.");
        }
    }
}
