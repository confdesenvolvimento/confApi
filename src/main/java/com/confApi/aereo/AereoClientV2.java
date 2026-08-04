package com.confApi.aereo;

import com.confApi.aereo.dto.*;
import com.confApi.aereo.eNums.EnumFormaPagamento;
import com.confApi.aereo.eNums.EnumStatusRecebimento;
import com.confApi.aereo.eNums.StatusReservaEnum;
import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.bilhete.BilheteAereo;
import com.confApi.db.confManager.passageiro.Passageiro;
import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.db.confManager.reservaAereo.ReservaAereoEmissaoDto;
import com.confApi.db.confManager.reservaPacote.ReservaPacote;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.endPoints.recebimento.CancelarResponse;
import com.confApi.endPoints.recebimento.RecebimentoApi;
import com.confApi.endPoints.usuario.UsuarioApi;
import com.confApi.hub.aereo.BilheteModel;
import com.confApi.hub.aereo.PassageiroModel;
import com.confApi.hub.aereo.RecebimentoModel;
import com.confApi.hub.aereo.ReservaAereoModel;
import com.confApi.notificacao.EnumTipoNotificacao;
import com.confApi.notificacao.NotificacaoConfig;
import com.confApi.notificacao.NotificacaoControle;
import com.confApi.util.JsonLogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class AereoClientV2 {

    private static final Logger LOG = Logger.getLogger(AereoClientV2.class.getName());
    private final RestTemplate restTemplate;

    @Autowired
    private ConfAppService confAppService;

    @Autowired
    private AereoService aereoService;

    @Autowired
    private UsuarioApi usuarioApi;

    @Autowired
    private RecebimentoApi recebimentoApi;

    public AereoClientV2(@Qualifier("hubRestTemplate") RestTemplate hubRestTemplate) {
        this.restTemplate = hubRestTemplate;
    }

    public BuscarFormasFinanciamentoResponse recuperarFormasFinanciamentoToken(BuscarFormasFinanciamentoRequest request) {
        return post(
                "Aéreo - Recuperar Formas de Financiamento Token",
                UrlConfig.URL_CONFIANCA_HUB + "api/aereo" + "/recuperarformasdefinanciamentoToken",
                request,
                BuscarFormasFinanciamentoResponse.class,
                new BuscarFormasFinanciamentoResponse()
        );
    }

    public List<PesquisaResponse> tarifarPesquisa(TarifarPesquisaRequest request) {
        return post(
                "Aéreo - Tarifar Pesquisa",
                UrlConfig.URL_CONFIANCA_HUB + "api/aereo" + "/tarifarpesquisa",
                request,
                new ParameterizedTypeReference<List<PesquisaResponse>>() {
                },
                new ArrayList<>()
        );
    }

    public RemoverAssentoResponse removerAssento(RemoverAssentoRequest request) {
        return post(
                "Aéreo - Remover Assento",
                UrlConfig.URL_CONFIANCA_HUB + "api/aereo" + "/removerassento",
                request,
                RemoverAssentoResponse.class,
                new RemoverAssentoResponse()
        );
    }

    public MarcarAssentoResponse marcarAssento(MarcarAssentoRequest request) {
        return post(
                "Aéreo - Marcar Assento",
                UrlConfig.URL_CONFIANCA_HUB + "api/aereo" + "/marcarassento",
                request,
                MarcarAssentoResponse.class,
                new MarcarAssentoResponse()
        );
    }

    public MapaAssentoResponse buscarMapaAssentos(MapaAssentoRequest request) {
        return post(
                "Aéreo - Buscar Mapa de Assentos",
                UrlConfig.URL_CONFIANCA_HUB + "api/aereo" + "/mapadeassentos",
                request,
                MapaAssentoResponse.class,
                new MapaAssentoResponse()
        );
    }

    public BuscarFormasFinanciamentoResponse recuperarFormasFinanciamento(BuscarFormasFinanciamentoRequest request) {
        return post(
                "Aéreo - Recuperar Formas de Financiamento",
                UrlConfig.URL_CONFIANCA_HUB + "api/aereo" + "/recuperarformasdefinanciamento",
                request,
                BuscarFormasFinanciamentoResponse.class,
                new BuscarFormasFinanciamentoResponse()
        );
    }

    public ConsultarEticketResponse cancelarBilhete(ReservaAereoModel reservaAereoModel) {
        ConsultarEticketResponse consultarEticketResponse = new ConsultarEticketResponse();
        if (reservaAereoModel.getPassageiros() == null) {
            return respostaCancelamentoBilheteComErro("A reserva não possui bilhetes para cancelar.");
        }

        boolean possuiBilhete = false;
        for (PassageiroModel passageiro : reservaAereoModel.getPassageiros()) {
            if (passageiro == null || passageiro.getBilhetes() == null) {
                continue;
            }

            for (BilheteModel bilhete : passageiro.getBilhetes()) {
                String numeroBilhete = bilhete != null ? bilhete.getNumeroBilhete() : null;
                if (numeroBilhete == null || numeroBilhete.trim().isEmpty()) {
                    return respostaCancelamentoBilheteComErro("Um dos bilhetes não possui número para cancelamento.");
                }

                possuiBilhete = true;
                try {
                    consultarEticketResponse = postConfirmado(
                        "Aéreo - Cancelar Bilhete",
                        UrlConfig.URL_CONFIANCA_HUB + "api/aereo" + "/cancelarbilhete",
                        new CancelarBilheteRequest(reservaAereoModel, bilhete),
                        ConsultarEticketResponse.class
                    );
                } catch (RuntimeException e) {
                    return respostaCancelamentoBilheteComErro("O HUB não confirmou o cancelamento do bilhete.");
                }

                if (consultarEticketResponse.getException() != null
                        || !"Cancelado".equalsIgnoreCase(consultarEticketResponse.getStatus())) {
                    return respostaCancelamentoBilheteComErro("O fornecedor não confirmou o cancelamento do bilhete.");
                }

                BilheteAereo bilheteAereo = new BilheteAereo(passageiro, bilhete);
                bilheteAereo.setDataCancelamento(new Date());
                bilheteAereo.setStatus(0);

                if (!putSemCorpoConfirmado(
                        "Aéreo - Cancelar Bilhete DB",
                        UrlConfig.URL_CONFIANCA_MANAGER + "bilheteAereo/cancelar/" + numeroBilhete,
                        bilheteAereo
                ) || !bilheteCanceladoNoManager(reservaAereoModel.getLocalizador(), numeroBilhete)) {
                    return respostaCancelamentoBilheteComErro(
                            "O fornecedor cancelou o bilhete, mas o ConfiancaManager não confirmou a atualização."
                    );
                }
            }
        }

        if (!possuiBilhete) {
            return respostaCancelamentoBilheteComErro("A reserva não possui bilhetes para cancelar.");
        }

        List<CancelarResponse> cancelarResponseList = new ArrayList<>();
        if (reservaAereoModel.getRecebimentos() != null) {
            for (RecebimentoModel recebimentoModel : reservaAereoModel.getRecebimentos()) {
                if (recebimentoModel.getStatusRecebimento() == 1) {
                    List<CancelarResponse> cancelarResponses = recebimentoApi.cancelar(recebimentoModel.getCodgRecebimento(), new Recebimento(recebimentoModel));
                    cancelarResponseList.addAll(cancelarResponses);
                }
            }
        }
        return consultarEticketResponse;
    }

    public Boolean cancelarReserva(ReservaAereoModel reservaAereoModel) {
        CancelarReservaResponse cancelarReservaResponse;
        try {
            cancelarReservaResponse = postConfirmado(
                    "Aéreo - Cancelar Reserva",
                    UrlConfig.URL_CONFIANCA_HUB + "api/aereo" + "/cancelar",
                    new CancelarReservaRequest(reservaAereoModel),
                    CancelarReservaResponse.class
            );
        } catch (RuntimeException e) {
            return false;
        }

        if (cancelarReservaResponse.getException() == null
                && "Cancelado".equalsIgnoreCase(cancelarReservaResponse.getStatus())) {
            String reservaAereoResponse = put(
                    "Aéreo - Cancelar Banco de Dados Reserva Aereo",
                    UrlConfig.URL_CONFIANCA_MANAGER + "reservaAereo/cancelar/" + reservaAereoModel.getCodgReservaAereoDB(),
                    new ReservaAereoCancelamentoDto(reservaAereoModel, usuarioApi.consultaUsuarioByLogin(reservaAereoModel.getUsuarioCancelamento())),
                    String.class,
                    ""
            );

            NotificacaoConfig notificacaoConfig =
                    new NotificacaoConfig(
                            "Localizador: " + reservaAereoModel.getLocalizador() + " criado.",
                            "Aéreo",
                            "Reserva Aérea", EnumTipoNotificacao.reservaAereo.getValor(),
                            usuarioApi.consultaUsuarioByLogin(reservaAereoModel.getUsuarioCancelamento())
                    );

            List<NotificacaoControle> notificacaoControleList = post(
                    "Aéreo - Reservar Notificação",
                    UrlConfig.URL_CONFIANCA_MANAGER + "notificacao?tipo=Exclusivo",
                    notificacaoConfig,
                    new ParameterizedTypeReference<List<NotificacaoControle>>() {
                    },
                    Collections.emptyList()
            );
            return true;
        }
        return false;
    }

    public ReservaAereoModel emitir(ReservaAereoModel reservaAerea, Boolean isLink) {
        ReservaAereoModel reservaAereaBase = new ReservaAereoModel(reservaAerea);
        Usuario usuarioEmissao = usuarioApi.consultaUsuarioByLogin(reservaAerea.getUsuarioCriacao());

        EmitirResponse emitirResponse;
        try {
            emitirResponse = postConfirmado(
                    "Aéreo - Emitir",
                    UrlConfig.URL_CONFIANCA_HUB + "api/aereo/emitir",
                    new EmitirRequest(reservaAerea),
                    EmitirResponse.class
            );
        } catch (RuntimeException e) {
            reservaAerea.setMsg("O HUB não confirmou a emissão da reserva.");
            return reservaAerea;
        }

        if (emitirResponse.getException() != null) {
            reservaAerea.setMsg(emitirResponse.getException().getMessage());
            return reservaAerea;
        }

        ConsultarLocalizadorResponse consultarLocalizadorResponse;
        try {
            consultarLocalizadorResponse = postConfirmado(
                    "Aéreo - Confirmar Emissão",
                    UrlConfig.URL_CONFIANCA_HUB + "api/aereo/consultar",
                    new ConsultarLocalizadorRequest(reservaAerea),
                    ConsultarLocalizadorResponse.class
            );
        } catch (RuntimeException e) {
            reservaAerea.setMsg("Não foi possível confirmar a emissão no fornecedor.");
            return reservaAerea;
        }

        reservaAerea = aereoService.convertToReservaAereoModel(consultarLocalizadorResponse, reservaAerea, true);
        if (!emissaoConfirmada(reservaAerea)) {
            reservaAerea.setMsg("O fornecedor não retornou bilhetes para esta emissão.");
            return reservaAerea;
        }

        put(
                "Aéreo - Atualizar Banco de Dados Reserva Aereo",
                UrlConfig.URL_CONFIANCA_MANAGER + "reservaAereo/emitir/"
                        + reservaAerea.getCodgReservaAereoDB(),
                new ReservaAereoEmissaoDto(reservaAerea, usuarioEmissao),
                EmitirResponse.class,
                new EmitirResponse()
        );
        ReservaAereo reservaDB = get(
                "Aéreo - Buscar Reserva Aereo Banco",
                UrlConfig.URL_CONFIANCA_MANAGER + "reservaAereo/localizador/" + reservaAerea.getLocalizador(),
                ReservaAereo.class,
                null
        );

        if (reservaDB != null) {
            aereoService.populaReservaFromDB(reservaDB, reservaAerea);
        }

        if (reservaAerea.getStatusReserva().equalsIgnoreCase(StatusReservaEnum.Emitida.getDescricao())) {
            if (reservaAerea.getPassageiros() != null) {
                for (PassageiroModel passageiroModel : reservaAerea.getPassageiros()) {

                    if (passageiroModel.getBilhetes() == null || passageiroModel.getBilhetes().isEmpty()) {
                        continue;
                    }

                    Passageiro passageiroDB = buscarPassageiroDB(reservaDB, passageiroModel);

                    if (passageiroDB == null) {
                        continue;
                    }

                    for (BilheteModel bilheteModel : passageiroModel.getBilhetes()) {

                        if (bilheteModel.getNumeroBilhete() == null
                                || bilheteModel.getNumeroBilhete().trim().isEmpty()) {
                            continue;
                        }

                        BilheteAereo bilheteExistente = buscarBilheteExistente(
                                passageiroDB,
                                bilheteModel.getNumeroBilhete()
                        );

                        if (bilheteExistente == null) {
                            post(
                                    "Aéreo - Gravar Bilhete Aéreo DB",
                                    UrlConfig.URL_CONFIANCA_MANAGER + "bilheteAereo",
                                    new BilheteAereo(passageiroDB, bilheteModel),
                                    Void.class,
                                    null
                            );
                        }
                    }
                }
            }

            NotificacaoConfig notificacaoConfig =
                    new NotificacaoConfig(
                            "Localizador: " + reservaAerea.getLocalizador() + " emitido com sucesso..",
                            "Aéreo",
                            "Emissão Aérea - " + reservaAerea.getLocalizador(),
                            EnumTipoNotificacao.reservaAereo.getValor(),
                            reservaAerea.getUsuarioCriacao2()
                    );

            List<NotificacaoControle> notificacaoControleList = post(
                    "Aéreo - Emitir Notificação",
                    UrlConfig.URL_CONFIANCA_MANAGER + "notificacao?tipo=Exclusivo",
                    notificacaoConfig,
                    new ParameterizedTypeReference<List<NotificacaoControle>>() {
                    },
                    Collections.emptyList()
            );

            if (isLink) {
                reservaAerea.getRecebimento().setStatusRecebimento(EnumStatusRecebimento.PAGO.getValor());
                reservaAerea.getRecebimento().setCodgFormaPagamento(EnumFormaPagamento.LINK_PAGAMENTO.getValor());
                reservaAerea.getRecebimento().getFormaDePagamento().setCodgFormaPagto(EnumFormaPagamento.LINK_PAGAMENTO.getValor());
                put(
                        "Aéreo - Atualizar Banco de Dados Recebimento Reserva Aereo",
                        UrlConfig.URL_CONFIANCA_MANAGER + "recebimento/" +
                                +reservaAerea.getCodgReservaAereoDB(),
                        new Recebimento(reservaAerea),
                        Void.class,
                        null
                );
            } else {
                Boolean autorizacao = false;
                post(
                        "Aéreo - Atualizar Banco de Dados Recebimento Reserva Aereo",
                        UrlConfig.URL_CONFIANCA_MANAGER + "recebimento/" + reservaAerea.getSistema() + "?autorizacao=" + autorizacao,
                        new Recebimento(reservaAerea),
                        Void.class,
                        null
                );
            }
        }

        reservaAerea.setMsg(null);
        return reservaAerea;
    }

    public ReservaAereoModel carregarReservaAerea(ReservaAereo reservaAerea) {
        ConsultarLocalizadorRequest consultarLocalizadorRequest = new ConsultarLocalizadorRequest(reservaAerea);
        ConsultarLocalizadorResponse consultarLocalizadorResponse = post(
                "Aéreo - Consultar Localizador",
                UrlConfig.URL_CONFIANCA_HUB + "api/aereo" + "/consultar",
                consultarLocalizadorRequest,
                ConsultarLocalizadorResponse.class,
                new ConsultarLocalizadorResponse()
        );

        ReservaAereo reservaDB = get(
                "Aéreo - Buscar Reserva Aereo Banco",
                UrlConfig.URL_CONFIANCA_MANAGER + "reservaAereo/localizador/" + reservaAerea.getLocalizador(),
                ReservaAereo.class,
                null
        );

        ReservaAereoModel reservaAereoModel = new ReservaAereoModel(consultarLocalizadorResponse, reservaDB);
        ReservaAereo reservaAereoUpdate = new ReservaAereo(reservaAereoModel);

        put(
                "Aéreo - Atualizar Reserva DB",
                UrlConfig.URL_CONFIANCA_MANAGER + "reservaAereo/" + reservaDB.getCodgReservaAereo(),
                reservaAereoUpdate,
                Void.class,
                null
        );

        return reservaAereoModel;
    }

    public ConsultarLocalizadorResponse carregarReservaAereaModel(ReservaAereoModel reservaAerea) {
        return post(
                "Aéreo - Consultar Localizador",
                UrlConfig.URL_CONFIANCA_HUB + "api/aereo" + "/consultar",
                new ConsultarLocalizadorRequest(reservaAerea),
                ConsultarLocalizadorResponse.class,
                new ConsultarLocalizadorResponse()
        );
    }

    public ReservarResponse reserva(PreReserva preReserva) {
        ReservarResponse reservarResponse = post(
                "Aéreo - Reservar",
                UrlConfig.URL_CONFIANCA_HUB + "api/aereo" + "/reservar",
                new ReservarRequest(preReserva),
                ReservarResponse.class,
                new ReservarResponse()
        );

        if (reservarResponse == null || reservarResponse.getReservas() == null) {
            return reservarResponse;
        }

        for (Reserva reserva : reservarResponse.getReservas()) {
            aereoService.populaReservaToReservaDB(reserva, preReserva);
            ReservaAereo reservaAereo = aereoService.convertToReservaAereo(reserva, preReserva);
            if (preReserva.getCodgPacote() != null) {
                reservaAereo.setCodgReservaPacote(new ReservaPacote(preReserva.getCodgPacote()));
            }

            ReservaAereo reservaAereoResponse = post(
                    "Aéreo - Gravar Banco de Dados Reserva Aereo",
                    UrlConfig.URL_CONFIANCA_MANAGER + "reservaAereo",
                    reservaAereo,
                    ReservaAereo.class,
                    new ReservaAereo()
            );

            NotificacaoConfig notificacaoConfig =
                    new NotificacaoConfig(
                            "Localizador: " + reserva.getLocalizador() + " criado.",
                            "Aéreo",
                            "Reserva Aérea", EnumTipoNotificacao.reservaAereo.getValor(),
                            new Usuario(preReserva.getUsuario())
                    );

            List<NotificacaoControle> notificacaoControleList = post(
                    "Aéreo - Reservar Notificação",
                    UrlConfig.URL_CONFIANCA_MANAGER + "notificacao?tipo=Exclusivo",
                    notificacaoConfig,
                    new ParameterizedTypeReference<List<NotificacaoControle>>() {
                    },
                    Collections.emptyList()
            );
        }
        return reservarResponse;
    }

    public PreReserva tarifar(PreReserva preReserva) {
        TarifarResponse response = postConfirmado(
                "Aéreo - Tarifar",
                UrlConfig.URL_CONFIANCA_HUB + "/api/aereo/tarifar",
                new TarifarRequest(preReserva),
                TarifarResponse.class
        );

        PreReserva tarifaConfirmada = aereoService.montarPreReservaTarifada(preReserva, response);
        if (tarifaConfirmada == null || tarifaConfirmada.getValorTotalGeral() == null
                || tarifaConfirmada.getValorTotalGeral() <= 0) {
            throw new IllegalStateException("O HUB não retornou um preço válido para a tarifa.");
        }
        return tarifaConfirmada;
    }

    public PesquisaResponse pesquisarDisponibilidade(PesquisaRequestDTOV2 pesquisaRequestDTO) {
        PesquisaResponse responseAjustado = new PesquisaResponse();
        List<PesquisaResponse> resposta = post(
                "Aéreo - Pesquisar Disponibilidade",
                UrlConfig.URL_CONFIANCA_HUB + "api/aereo" + "/pesquisa",
                pesquisaRequestDTO,
                new ParameterizedTypeReference<List<PesquisaResponse>>() {
                },
                Collections.emptyList()
        );

        for (PesquisaResponse p : resposta) {
            if (p != null) {

                if (p.getOrigem() != null) {
                    responseAjustado.setOrigem(p.getOrigem());
                }
                if (p.getDestino() != null) {
                    responseAjustado.setDestino(p.getDestino());
                }
                if (p.getViagensMultiplosTrechos() != null && !p.getViagensMultiplosTrechos().isEmpty()) {
                    if (responseAjustado.getViagensMultiplosTrechos() == null) {
                        responseAjustado.setViagensMultiplosTrechos(new ArrayList<>());
                    }
                    responseAjustado.getViagensMultiplosTrechos().addAll(p.getViagensMultiplosTrechos());
                }
                if (p.getTrechos1() != null && !p.getTrechos1().isEmpty()) {
                    if (responseAjustado.getTrechos1() == null) {
                        responseAjustado.setTrechos1(new ArrayList<>());
                    }
                    responseAjustado.getTrechos1().addAll(p.getTrechos1());

                }
                if (p.getTrechos2() != null && !p.getTrechos2().isEmpty()) {
                    if (responseAjustado.getTrechos2() == null) {
                        responseAjustado.setTrechos2(new ArrayList<>());
                    }
                    responseAjustado.getTrechos2().addAll(p.getTrechos2());

                }
            }
        }

        return responseAjustado;
    }

    private ConsultarEticketResponse respostaCancelamentoBilheteComErro(String mensagem) {
        ExceptionDetail erro = new ExceptionDetail();
        erro.setMessage(mensagem);

        ConsultarEticketResponse resposta = new ConsultarEticketResponse();
        resposta.setException(erro);
        return resposta;
    }

    private boolean emissaoConfirmada(ReservaAereoModel reservaAerea) {
        if (reservaAerea == null
                || !StatusReservaEnum.Emitida.getDescricao().equalsIgnoreCase(reservaAerea.getStatusReserva())
                || reservaAerea.getPassageiros() == null) {
            return false;
        }

        for (PassageiroModel passageiro : reservaAerea.getPassageiros()) {
            if (passageiro == null || passageiro.getBilhetes() == null) {
                continue;
            }
            for (BilheteModel bilhete : passageiro.getBilhetes()) {
                if (bilhete != null && bilhete.getNumeroBilhete() != null
                        && !bilhete.getNumeroBilhete().trim().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * O Manager responde 200 sem corpo ao cancelar um bilhete. O helper
     * genérico de PUT não consegue distinguir esse sucesso de uma falha,
     * porque espera um body não nulo. Para esta operação, o código HTTP é a
     * confirmação da gravação.
     */
    private <REQ> boolean putSemCorpoConfirmado(String operacao, String endpoint, REQ request) {
        long inicio = System.currentTimeMillis();
        try {
            ConfAppResp token = confAppService.token();
            HttpEntity<REQ> entity = new HttpEntity<>(request, defaultHeaders(token.getToken()));

            JsonLogUtil.logRequest(operacao, request);
            ResponseEntity<Void> response = restTemplate.exchange(endpoint, HttpMethod.PUT, entity, Void.class);
            logTempoExecucao(operacao, inicio);

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            tratarErro(operacao, endpoint, inicio, e);
            return false;
        }
    }

    private boolean bilheteCanceladoNoManager(String localizador, String numeroBilhete) {
        if (localizador == null || localizador.trim().isEmpty()) {
            return false;
        }

        ReservaAereo reserva = get(
                "Aéreo - Confirmar Bilhete Cancelado no DB",
                UrlConfig.URL_CONFIANCA_MANAGER + "reservaAereo/localizador/" + localizador,
                ReservaAereo.class,
                null
        );

        if (reserva == null || reserva.getPassageiros() == null) {
            return false;
        }

        for (Passageiro passageiro : reserva.getPassageiros()) {
            if (passageiro == null || passageiro.getBilhetes() == null) {
                continue;
            }
            for (BilheteAereo bilhete : passageiro.getBilhetes()) {
                if (bilhete != null
                        && equalsIgnoreCase(bilhete.getNumrBilhete(), numeroBilhete)
                        && Integer.valueOf(0).equals(bilhete.getStatus())) {
                    return true;
                }
            }
        }
        return false;
    }

    private <REQ, RES> RES postConfirmado(
            String operacao,
            String endpoint,
            REQ request,
            Class<RES> responseClass
    ) {
        long inicio = System.currentTimeMillis();
        try {
            ConfAppResp token = confAppService.token();
            HttpEntity<REQ> entity = new HttpEntity<>(request, defaultHeaders(token.getToken()));

            JsonLogUtil.logRequest(operacao, request);
            ResponseEntity<RES> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, responseClass);
            JsonLogUtil.logResponse(operacao, response.getBody());
            logTempoExecucao(operacao, inicio);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            throw new IllegalStateException(operacao + " retornou uma resposta vazia ou inválida.");
        } catch (Exception e) {
            tratarErro(operacao, endpoint, inicio, e);
            throw new IllegalStateException(operacao + " não foi confirmado pelo HUB.", e);
        }
    }

    private <REQ, RES> RES post(
            String operacao,
            String endpoint,
            REQ request,
            Class<RES> responseClass,
            RES retornoPadrao
    ) {
        long inicio = System.currentTimeMillis();

        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<REQ> entity = new HttpEntity<>(request, headers);

            JsonLogUtil.logRequest(operacao, request);

            ResponseEntity<RES> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    responseClass
            );

            JsonLogUtil.logResponse(operacao, response.getBody());

            logTempoExecucao(operacao, inicio);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            LOG.log(
                    Level.WARNING,
                    "{0} retornou status {1} sem corpo válido. URL: {2}",
                    new Object[]{operacao, response.getStatusCode(), endpoint}
            );

        } catch (Exception e) {
            tratarErro(operacao, endpoint, inicio, e);
        }

        return retornoPadrao;
    }

    private <REQ, RES> RES post(
            String operacao,
            String endpoint,
            REQ request,
            ParameterizedTypeReference<RES> responseType,
            RES retornoPadrao
    ) {
        long inicio = System.currentTimeMillis();

        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<REQ> entity = new HttpEntity<>(request, headers);

            JsonLogUtil.logRequest(operacao, request);

            ResponseEntity<RES> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    responseType
            );

            JsonLogUtil.logResponse(operacao, response.getBody());

            logTempoExecucao(operacao, inicio);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            LOG.log(
                    Level.WARNING,
                    "{0} retornou status {1} sem corpo válido. URL: {2}",
                    new Object[]{operacao, response.getStatusCode(), endpoint}
            );

        } catch (Exception e) {
            tratarErro(operacao, endpoint, inicio, e);
        }

        return retornoPadrao;
    }

    private <REQ, RES> RES put(
            String operacao,
            String endpoint,
            REQ request,
            Class<RES> responseClass,
            RES retornoPadrao
    ) {
        long inicio = System.currentTimeMillis();

        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<REQ> entity = new HttpEntity<>(request, headers);

            JsonLogUtil.logRequest(operacao, request);

            ResponseEntity<RES> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.PUT,
                    entity,
                    responseClass
            );

            JsonLogUtil.logResponse(operacao, response.getBody());

            logTempoExecucao(operacao, inicio);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            LOG.log(
                    Level.WARNING,
                    "{0} retornou status {1} sem corpo válido. URL: {2}",
                    new Object[]{operacao, response.getStatusCode(), endpoint}
            );

        } catch (Exception e) {
            tratarErro(operacao, endpoint, inicio, e);
        }

        return retornoPadrao;
    }

    private <RES> RES get(
            String operacao,
            String endpoint,
            Class<RES> responseClass,
            RES retornoPadrao
    ) {
        long inicio = System.currentTimeMillis();

        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<RES> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.GET,
                    entity,
                    responseClass
            );

            JsonLogUtil.logResponse(operacao, response.getBody());

            logTempoExecucao(operacao, inicio);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            LOG.log(
                    Level.WARNING,
                    "{0} retornou status {1} sem corpo válido. URL: {2}",
                    new Object[]{
                            operacao,
                            response.getStatusCode(),
                            endpoint
                    }
            );

        } catch (Exception e) {
            tratarErro(operacao, endpoint, inicio, e);
        }

        return retornoPadrao;
    }

    private HttpHeaders defaultHeaders(String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(bearerToken);
        return headers;
    }

    private void tratarErro(String operacao, String url, long inicio, Exception e) {
        logTempoExecucao(operacao, inicio);

        if (e instanceof ResourceAccessException) {
            tratarErroAcesso(operacao, url, e);
            return;
        }

        if (e instanceof RestClientResponseException) {
            tratarErroHttp(operacao, url, (RestClientResponseException) e);
            return;
        }

        LOG.log(
                Level.SEVERE,
                operacao + " - Erro inesperado ao consumir HUB. URL: " + url,
                e
        );
    }

    private void tratarErroAcesso(String operacao, String url, Exception e) {
        if (isTimeout(e)) {
            LOG.log(
                    Level.SEVERE,
                    operacao + " - Timeout ao consumir HUB. URL: " + url
                            + ". Verifique se o HUB está ativo, se a URL está correta, "
                            + "se o endpoint está demorando demais ou se o readTimeout do RestTemplate está baixo.",
                    e
            );
            return;
        }

        LOG.log(
                Level.SEVERE,
                operacao + " - Erro de conexão/acesso ao consumir HUB. URL: " + url,
                e
        );
    }

    private void tratarErroHttp(String operacao, String url, RestClientResponseException e) {
        LOG.log(
                Level.SEVERE,
                operacao + " - Erro HTTP ao consumir HUB. URL: " + url
                        + ", Status: " + e.getRawStatusCode()
        );
    }

    private boolean isTimeout(Throwable e) {
        Throwable causa = e;

        while (causa != null) {
            if (causa instanceof SocketTimeoutException) {
                return true;
            }

            causa = causa.getCause();
        }

        return false;
    }

    private void logTempoExecucao(String operacao, long inicio) {
        long fim = System.currentTimeMillis();
        long tempoMs = fim - inicio;

        LOG.log(
                Level.INFO,
                "{0} - Tempo de execução no HUB: {1} ms",
                new Object[]{operacao, tempoMs}
        );
    }

    private Passageiro buscarPassageiroDB(ReservaAereo reservaDB, PassageiroModel passageiroModel) {
        if (reservaDB == null || reservaDB.getPassageiros() == null || passageiroModel == null) {
            return null;
        }

        if (passageiroModel.getCodgPassageiroDb() != null) {
            for (Passageiro passageiroDB : reservaDB.getPassageiros()) {
                if (Objects.equals(
                        passageiroDB.getCodgPassageiro(),
                        passageiroModel.getCodgPassageiroDb()
                )) {
                    return passageiroDB;
                }
            }
        }

        for (Passageiro passageiroDB : reservaDB.getPassageiros()) {
            if (equalsIgnoreCase(passageiroDB.getNomePassageiro(), passageiroModel.getNome())
                    && equalsIgnoreCase(passageiroDB.getSobrenomePassageiro(), passageiroModel.getSobrenome())) {
                return passageiroDB;
            }
        }

        return null;
    }

    private BilheteAereo buscarBilheteExistente(Passageiro passageiroDB, String numeroBilhete) {
        if (passageiroDB == null || passageiroDB.getBilhetes() == null || numeroBilhete == null) {
            return null;
        }

        for (BilheteAereo bilheteDB : passageiroDB.getBilhetes()) {
            if (bilheteDB.getNumrBilhete() != null
                    && bilheteDB.getNumrBilhete().equals(numeroBilhete)) {
                return bilheteDB;
            }
        }

        return null;
    }

    private boolean equalsIgnoreCase(String valor1, String valor2) {
        if (valor1 == null || valor2 == null) {
            return false;
        }

        return valor1.trim().equalsIgnoreCase(valor2.trim());
    }
}
