package com.confApi.chatconfianca.service;

import com.confApi.aereo.AereoClient;
import com.confApi.aereo.dto.ClasseSelecionada;
import com.confApi.aereo.dto.ConsultarLocalizadorRequest;
import com.confApi.aereo.dto.ConsultarLocalizadorResponse;
import com.confApi.aereo.dto.FamiliaPreco;
import com.confApi.aereo.dto.ParametrosPesquisa;
import com.confApi.aereo.dto.PassageiroTipoQtd;
import com.confApi.aereo.dto.PesquisaRequestDTO;
import com.confApi.aereo.dto.PesquisaResponse;
import com.confApi.aereo.dto.Preco;
import com.confApi.aereo.dto.Reserva;
import com.confApi.aereo.dto.Sistema;
import com.confApi.aereo.dto.TarifarRequest;
import com.confApi.aereo.dto.TarifarResponse;
import com.confApi.aereo.dto.Trecho;
import com.confApi.aereo.dto.ValorBase;
import com.confApi.aereo.eNums.Classe;
import com.confApi.aereo.eNums.Ordenacao;
import com.confApi.aereo.eNums.TipoBagagem;
import com.confApi.aereo.eNums.TipoConsulta;
import com.confApi.aereo.eNums.TipoPesquisa;
import com.confApi.aereo.eNums.TipoTarifa;
import com.confApi.chatconfianca.client.ChatConfiancaManagerClient;
import com.confApi.chatconfianca.dto.model.Conversa;
import com.confApi.chatconfianca.dto.model.ConversaEvento;
import com.confApi.chatconfianca.dto.model.SimulacaoRemarcacao;
import com.confApi.chatconfianca.dto.remarcacao.RemarcacaoRequest;
import com.confApi.chatconfianca.dto.remarcacao.RemarcacaoSimulacaoResponse;
import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.db.confManager.aeroporto.AeroportoService;
import com.confApi.db.confManager.regraAereaAlteracao.RegraAereaAlteracaoManagerService;
import com.confApi.db.confManager.regraAereaAlteracao.dto.RegraAereaAlteracaoCalculoResponse;
import com.confApi.db.confManager.regraAereaAlteracao.dto.RegraAereaAlteracaoConsultaRequest;
import com.confApi.db.confManager.regraAereaAlteracao.dto.RegraAereaAlteracaoConsultaResponse;
import com.confApi.exception.RegraDeNegocioException;
import com.confApi.hub.aereo.dto.Aeroporto;
import com.confApi.hub.aereo.dto.Bilhete;
import com.confApi.hub.aereo.dto.Companhia;
import com.confApi.hub.aereo.dto.Passageiro;
import com.confApi.hub.aereo.dto.TrechoReserva;
import com.confApi.hub.aereo.dto.Voo;
import com.confApi.model.IdentificacaoAgenciaModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Service
public class ChatConfiancaRemarcacaoService {
    private static final String SCHEMA = "chat.reschedule.v1";
    private static final String VALIDANDO = "VALIDANDO";
    private static final String AGUARDANDO_TRECHO = "AGUARDANDO_TRECHO";
    private static final String AGUARDANDO_CRITERIOS = "AGUARDANDO_CRITERIOS";
    private static final String PESQUISANDO = "PESQUISANDO";
    private static final String AGUARDANDO_OPCAO = "AGUARDANDO_OPCAO";
    private static final String CALCULANDO = "CALCULANDO";
    private static final String PREVIA_DISPONIVEL = "PREVIA_DISPONIVEL";
    private static final String ENCAMINHADO = "ENCAMINHADO";
    private static final String NAO_ELEGIVEL = "NAO_ELEGIVEL";
    private static final String ERRO = "ERRO";
    private static final String EXPIRADO = "EXPIRADO";
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int LIMITE_OPCOES = 5;

    private final ChatConfiancaManagerClient manager;
    private final ChatConfiancaService chatService;
    private final AereoClient aereoClient;
    private final AeroportoService aeroportoService;
    private final RegraAereaAlteracaoManagerService regraService;
    private final ObjectMapper mapper;

    public ChatConfiancaRemarcacaoService(ChatConfiancaManagerClient manager,
                                          ChatConfiancaService chatService,
                                          AereoClient aereoClient,
                                          AeroportoService aeroportoService,
                                          RegraAereaAlteracaoManagerService regraService,
                                          ObjectMapper mapper) {
        this.manager = manager;
        this.chatService = chatService;
        this.aereoClient = aereoClient;
        this.aeroportoService = aeroportoService;
        this.regraService = regraService;
        this.mapper = mapper;
    }

    public RemarcacaoSimulacaoResponse iniciar(RemarcacaoRequest.Iniciar request) {
        if (request == null || request.getConversaId() == null || request.getCodgUsuario() == null
                || vazio(request.getLocalizador())) {
            throw regra(400, "Informe a conversa, o usuario e o localizador da reserva.");
        }

        Conversa conversa = validarConversaSolicitante(request.getConversaId(), request.getCodgUsuario());
        SessaoChatResponse sessao = chatService.montarSessao(request.getCodgUsuario(), request.getCodgAgenciaSessao());

        SimulacaoRemarcacao simulacao = new SimulacaoRemarcacao();
        simulacao.setConversaId(conversa.getId());
        simulacao.setLocalizador(request.getLocalizador().trim().toUpperCase(Locale.ROOT));
        simulacao.setCodgUsuario(request.getCodgUsuario());
        simulacao.setCodgAgencia(sessao.getAgencia() == null ? conversa.getCodgAgencia() : sessao.getAgencia().getCodgAgencia());
        simulacao.setCodgUnidade(sessao.getUnidade() == null ? conversa.getCodgUnidade() : sessao.getUnidade().getCodgUnidade());
        simulacao.setStatus(VALIDANDO);
        simulacao.setExpiraEm(LocalDateTime.now().plusMinutes(30));
        simulacao = salvar(simulacao);
        registrarEvento(simulacao, "REMARCACAO_SIMULACAO_INICIADA",
                "Simulacao de alteracao iniciada para a reserva " + simulacao.getLocalizador() + ".", null);

        Reserva reserva = carregarReserva(simulacao, sessao);
        String impedimento = validarReserva(reserva);
        if (impedimento != null) {
            return bloquear(simulacao, impedimento);
        }

        List<Integer> indicesElegiveis = indicesTrechosElegiveis(reserva);
        if (indicesElegiveis.isEmpty()) {
            return bloquear(simulacao,
                    "Nao encontrei trecho futuro nacional, ativo e sem codeshare que possa ser simulado automaticamente.");
        }

        simulacao.setPassageirosJson(json(contagensPassageiros(reserva)));
        if (indicesElegiveis.size() == 1) {
            RemarcacaoSimulacaoResponse response = prepararTrecho(simulacao, reserva, indicesElegiveis.get(0));
            registrarCard(simulacao, response);
            return response;
        }

        simulacao.setStatus(AGUARDANDO_TRECHO);
        simulacao = salvar(simulacao);
        RemarcacaoSimulacaoResponse response = respostaBase(simulacao,
                "Qual trecho deseja alterar?",
                "Escolha um trecho futuro. Nesta primeira versao, simulamos um trecho por vez.");
        response.setTrechos(montarTrechos(reserva, indicesElegiveis, null));
        registrarEvento(simulacao, "REMARCACAO_AGUARDANDO_TRECHO",
                "Aguardando escolha do trecho da reserva.", null);
        registrarCard(simulacao, response);
        return response;
    }

    public RemarcacaoSimulacaoResponse selecionarTrecho(Long id, RemarcacaoRequest.SelecionarTrecho request) {
        if (request == null || request.getCodgUsuario() == null || request.getTrechoIndice() == null) {
            throw regra(400, "Informe o usuario e o trecho.");
        }
        SimulacaoRemarcacao simulacao = buscarValidar(id, request.getCodgUsuario());
        Reserva reserva = carregarReserva(simulacao, montarSessao(simulacao));
        List<Integer> elegiveis = indicesTrechosElegiveis(reserva);
        if (!elegiveis.contains(request.getTrechoIndice())) {
            throw regra(400, "O trecho selecionado nao esta elegivel para simulacao.");
        }
        RemarcacaoSimulacaoResponse response = prepararTrecho(simulacao, reserva, request.getTrechoIndice());
        registrarCard(simulacao, response);
        return response;
    }

    public RemarcacaoSimulacaoResponse pesquisar(Long id, RemarcacaoRequest.Pesquisar request) {
        if (request == null || request.getCodgUsuario() == null || request.getData() == null) {
            throw regra(400, "Informe o usuario e a data desejada.");
        }
        SimulacaoRemarcacao simulacao = buscarValidar(id, request.getCodgUsuario());
        if (simulacao.getTrechoIndice() == null) {
            throw regra(409, "Selecione o trecho antes de pesquisar novos voos.");
        }
        validarDataPesquisa(request.getData());
        simulacao.setStatus(PESQUISANDO);
        simulacao.setCriteriosJson(json(request));
        simulacao = salvar(simulacao);

        SessaoChatResponse sessao = montarSessao(simulacao);
        Reserva reserva = carregarReserva(simulacao, sessao);
        TrechoReserva original = trecho(reserva, simulacao.getTrechoIndice());
        PesquisaRequestDTO pesquisa = montarPesquisa(simulacao, reserva, original, sessao, request);
        List<PesquisaResponse> retornos = aereoClient.pesquisarDisponibilidade(pesquisa);
        List<Trecho> opcoes = filtrarOpcoes(retornos, simulacao, request);

        if (opcoes.isEmpty()) {
            simulacao.setStatus(AGUARDANDO_CRITERIOS);
            simulacao.setResultadosJson(null);
            simulacao = salvar(simulacao);
            RemarcacaoSimulacaoResponse response = respostaCriterios(simulacao, original,
                    "Nao encontrei voos compativeis nessa data e periodo. Tente outra combinacao.");
            registrarEvento(simulacao, "REMARCACAO_PESQUISA_SEM_RESULTADO",
                    "Pesquisa sem opcoes compativeis.", json(request));
            registrarCard(simulacao, response);
            return response;
        }

        simulacao.setResultadosJson(json(opcoes));
        simulacao.setStatus(AGUARDANDO_OPCAO);
        simulacao = salvar(simulacao);
        RemarcacaoSimulacaoResponse response = respostaBase(simulacao,
                "Opcoes encontradas",
                "Escolha o voo e a familia tarifaria para calcular a previa.");
        response.setCriterios(montarCriterios(original, request.getData(), request.getPeriodo(),
                Boolean.TRUE.equals(request.getSomenteDireto())));
        response.setOpcoes(montarOpcoes(opcoes));
        registrarEvento(simulacao, "REMARCACAO_OPCOES_ENCONTRADAS",
                opcoes.size() + " opcoes apresentadas ao solicitante.", json(request));
        registrarCard(simulacao, response);
        return response;
    }

    public RemarcacaoSimulacaoResponse simular(Long id, RemarcacaoRequest.Simular request) {
        if (request == null || request.getCodgUsuario() == null || request.getOpcaoIndice() == null
                || request.getFamiliaIndice() == null) {
            throw regra(400, "Informe o usuario, o voo e a familia tarifaria.");
        }
        SimulacaoRemarcacao simulacao = buscarValidar(id, request.getCodgUsuario());
        List<Trecho> opcoes = lerOpcoes(simulacao.getResultadosJson());
        Trecho opcao = item(opcoes, request.getOpcaoIndice(), "Opcao de voo invalida ou expirada.");
        FamiliaPreco familia = item(opcao.getFamilias(), request.getFamiliaIndice(), "Familia tarifaria invalida.");

        simulacao.setStatus(CALCULANDO);
        simulacao = salvar(simulacao);
        SessaoChatResponse sessao = montarSessao(simulacao);
        Reserva reserva = carregarReserva(simulacao, sessao);
        TrechoReserva original = trecho(reserva, simulacao.getTrechoIndice());

        TarifarResponse tarifa = aereoClient.tarifar(montarTarifacao(opcao, familia, reserva));
        if (tarifa == null || tarifa.getException() != null || tarifa.getPreco() == null) {
            simulacao.setStatus(AGUARDANDO_OPCAO);
            simulacao = salvar(simulacao);
            RemarcacaoSimulacaoResponse response = respostaBase(simulacao,
                    "Nao foi possivel confirmar essa tarifa",
                    "A disponibilidade pode ter mudado. Escolha outra opcao ou refaca a pesquisa.");
            response.setOpcoes(montarOpcoes(opcoes));
            registrarEvento(simulacao, "REMARCACAO_TARIFACAO_INDISPONIVEL",
                    "Tarifa selecionada nao pode ser confirmada.", null);
            registrarCard(simulacao, response);
            return response;
        }

        BigDecimal novaTarifa = valorTarifa(tarifa.getPreco());
        BigDecimal novasTaxas = valorTaxas(tarifa.getPreco(), novaTarifa);
        if (novaTarifa == null) {
            return bloquear(simulacao,
                    "A companhia nao retornou o valor completo da nova tarifa. A equipe precisa cotar essa opcao manualmente.");
        }
        RegraAereaAlteracaoConsultaRequest regraRequest = montarRequestRegra(reserva, original, novaTarifa, novasTaxas);
        RegraAereaAlteracaoConsultaResponse regra = regraService.simular(regraRequest);
        boolean calculoIncompleto = regra != null && regra.getCalculo() != null
                && !Boolean.TRUE.equals(regra.getCalculo().getCalculoCompleto());
        if (!regraPermite(regra) || regra.getCalculo() == null || calculoIncompleto) {
            simulacao.setRegraSnapshotJson(json(regra));
            String motivo = calculoIncompleto
                    ? "A reserva nao retornou todos os valores originais necessarios para uma previa segura."
                    : regra == null || vazio(regra.getMensagem())
                            ? "Nao foi possivel homologar a regra para esta simulacao."
                            : regra.getMensagem();
            return bloquear(simulacao, motivo);
        }

        RemarcacaoSimulacaoResponse.OpcaoVoo opcaoView = montarOpcao(opcao, request.getOpcaoIndice());
        RemarcacaoSimulacaoResponse.Familia familiaView = montarFamilia(familia, request.getFamiliaIndice());
        RemarcacaoSimulacaoResponse.Previa previa = montarPrevia(regra, opcaoView, familiaView, simulacao);
        SelecaoPersistida selecao = new SelecaoPersistida();
        selecao.setOpcao(opcaoView);
        selecao.setFamilia(familiaView);

        simulacao.setOfertaSelecionadaJson(json(selecao));
        simulacao.setCalculoJson(json(previa));
        simulacao.setRegraId(regra.getRegra() == null ? null : regra.getRegra().getId());
        simulacao.setRegraSnapshotJson(json(regra));
        simulacao.setStatus(PREVIA_DISPONIVEL);
        simulacao = salvar(simulacao);

        RemarcacaoSimulacaoResponse response = respostaBase(simulacao,
                "Previa da alteracao",
                "Confira os valores estimados. Um atendente precisa validar a disponibilidade e concluir a remarcacao.");
        response.setPrevia(previa);
        response.setPermiteEncaminhar(true);
        Map<String, Object> dadosEvento = new LinkedHashMap<>();
        dadosEvento.put("totalEstimado", previa.getTotalEstimado());
        dadosEvento.put("companhia", simulacao.getCompanhiaIata());
        registrarEvento(simulacao, "REMARCACAO_PREVIA_GERADA",
                "Previa de remarcacao gerada para atendimento.", json(dadosEvento));
        registrarCard(simulacao, response);
        return response;
    }

    public RemarcacaoSimulacaoResponse encaminhar(Long id, RemarcacaoRequest.Encaminhar request) {
        if (request == null || request.getCodgUsuario() == null) {
            throw regra(400, "Informe o usuario.");
        }
        SimulacaoRemarcacao simulacao = buscarValidar(id, request.getCodgUsuario());
        chatService.encaminharConversaParaAtendente(simulacao.getConversaId(), request.getCodgUsuario(),
                "Cliente solicitou concluir a remarcacao da reserva " + simulacao.getLocalizador()
                        + " com base na simulacao " + simulacao.getId() + ".");
        simulacao.setStatus(ENCAMINHADO);
        simulacao = salvar(simulacao);
        RemarcacaoSimulacaoResponse response = respostaBase(simulacao,
                "Solicitacao encaminhada",
                "A equipe recebeu a reserva, o voo escolhido, a regra e a previa calculada.");
        response.setPermiteEncaminhar(false);

        String resumo = montarResumoEncaminhamento(simulacao);
        Map<String, Object> contexto = new LinkedHashMap<>();
        contexto.put("schema", SCHEMA);
        contexto.put("remarcacao", response);
        contexto.put("handoffSchema", "chat.reschedule.handoff.v1");
        contexto.put("simulacaoId", simulacao.getId());
        contexto.put("localizador", simulacao.getLocalizador());
        contexto.put("oferta", vazio(simulacao.getOfertaSelecionadaJson())
                ? null : ler(simulacao.getOfertaSelecionadaJson(), SelecaoPersistida.class));
        contexto.put("previa", vazio(simulacao.getCalculoJson())
                ? null : ler(simulacao.getCalculoJson(), RemarcacaoSimulacaoResponse.Previa.class));
        chatService.registrarMensagemSistema(simulacao.getConversaId(), resumo, json(contexto));
        registrarEvento(simulacao, "REMARCACAO_ENCAMINHADA",
                "Simulacao encaminhada para conclusao com atendente.", simulacao.getCalculoJson());
        return response;
    }

    private String montarResumoEncaminhamento(SimulacaoRemarcacao simulacao) {
        StringBuilder resumo = new StringBuilder("Simulacao de remarcacao #")
                .append(simulacao.getId())
                .append(" | Reserva ").append(simulacao.getLocalizador());
        if (!vazio(simulacao.getOrigem()) && !vazio(simulacao.getDestino())) {
            resumo.append(" | Trecho ").append(simulacao.getOrigem()).append("-").append(simulacao.getDestino());
        }
        if (!vazio(simulacao.getOfertaSelecionadaJson())) {
            SelecaoPersistida selecao = ler(simulacao.getOfertaSelecionadaJson(), SelecaoPersistida.class);
            if (selecao.getOpcao() != null) {
                resumo.append(" | Voo ").append(selecao.getOpcao().getNumerosVoos())
                        .append(" em ").append(selecao.getOpcao().getDataPartida())
                        .append(" ").append(selecao.getOpcao().getHoraPartida());
            }
            if (selecao.getFamilia() != null) {
                resumo.append(" | Familia ").append(selecao.getFamilia().getNome());
            }
        }
        if (!vazio(simulacao.getCalculoJson())) {
            RemarcacaoSimulacaoResponse.Previa previa = ler(
                    simulacao.getCalculoJson(), RemarcacaoSimulacaoResponse.Previa.class);
            if (previa.getTotalEstimado() != null) {
                resumo.append(" | Total estimado ")
                        .append(primeiro(previa.getMoeda(), "BRL"))
                        .append(" ").append(previa.getTotalEstimado());
            }
        }
        resumo.append(". Validar novamente disponibilidade, regra e valores antes de concluir.");
        return resumo.toString();
    }

    public RemarcacaoSimulacaoResponse consultar(Long id, Integer codgUsuario) {
        SimulacaoRemarcacao simulacao = buscarValidar(id, codgUsuario);
        RemarcacaoSimulacaoResponse response = respostaBase(simulacao,
                tituloStatus(simulacao.getStatus()), mensagemStatus(simulacao));
        if (!vazio(simulacao.getCriteriosJson())) {
            RemarcacaoRequest.Pesquisar criterios = ler(simulacao.getCriteriosJson(), RemarcacaoRequest.Pesquisar.class);
            response.setCriterios(new RemarcacaoSimulacaoResponse.Criterios());
            response.getCriterios().setOrigem(simulacao.getOrigem());
            response.getCriterios().setDestino(simulacao.getDestino());
            response.getCriterios().setCompanhia(simulacao.getCompanhiaIata());
            response.getCriterios().setDataSugerida(criterios.getData());
            response.getCriterios().setPeriodo(criterios.getPeriodo());
            response.getCriterios().setSomenteDireto(Boolean.TRUE.equals(criterios.getSomenteDireto()));
        }
        if (!vazio(simulacao.getResultadosJson())) {
            response.setOpcoes(montarOpcoes(lerOpcoes(simulacao.getResultadosJson())));
        }
        if (!vazio(simulacao.getCalculoJson())) {
            response.setPrevia(ler(simulacao.getCalculoJson(), RemarcacaoSimulacaoResponse.Previa.class));
        }
        response.setPermiteEncaminhar(PREVIA_DISPONIVEL.equals(simulacao.getStatus())
                || NAO_ELEGIVEL.equals(simulacao.getStatus()));
        return response;
    }

    private RemarcacaoSimulacaoResponse prepararTrecho(SimulacaoRemarcacao simulacao,
                                                        Reserva reserva,
                                                        Integer indice) {
        TrechoReserva trecho = trecho(reserva, indice);
        simulacao.setTrechoIndice(indice);
        simulacao.setOrigem(iata(trecho.getOrigem()));
        simulacao.setDestino(iata(trecho.getDestino()));
        simulacao.setCompanhiaIata(companhiaTrecho(trecho));
        simulacao.setTrechoOriginalJson(json(trecho));

        RegraAereaAlteracaoConsultaResponse regra = regraService.simular(
                montarRequestRegra(reserva, trecho, null, null));
        simulacao.setRegraSnapshotJson(json(regra));
        simulacao.setRegraId(regra == null || regra.getRegra() == null ? null : regra.getRegra().getId());
        if (!regraPermite(regra)) {
            String motivo = regra == null || vazio(regra.getMensagem())
                    ? "Nao foi possivel confirmar uma regra aprovada para esse trecho."
                    : regra.getMensagem();
            return bloquear(simulacao, motivo, false);
        }

        simulacao.setStatus(AGUARDANDO_CRITERIOS);
        simulacao.setMotivoBloqueio(null);
        salvar(simulacao);
        Map<String, Object> dadosEvento = new LinkedHashMap<>();
        dadosEvento.put("trechoIndice", indice);
        dadosEvento.put("regraId", simulacao.getRegraId());
        registrarEvento(simulacao, "REMARCACAO_TRECHO_SELECIONADO",
                "Trecho " + simulacao.getOrigem() + " - " + simulacao.getDestino() + " selecionado.",
                json(dadosEvento));
        return respostaCriterios(simulacao, trecho,
                "A regra permite simular a alteracao. Informe a nova data e, se desejar, um periodo.");
    }

    private RemarcacaoSimulacaoResponse bloquear(SimulacaoRemarcacao simulacao, String motivo) {
        return bloquear(simulacao, motivo, true);
    }

    private RemarcacaoSimulacaoResponse bloquear(SimulacaoRemarcacao simulacao, String motivo, boolean registrarCard) {
        simulacao.setStatus(NAO_ELEGIVEL);
        simulacao.setMotivoBloqueio(motivo);
        simulacao = salvar(simulacao);
        registrarEvento(simulacao, "REMARCACAO_NAO_ELEGIVEL", motivo, simulacao.getRegraSnapshotJson());
        RemarcacaoSimulacaoResponse response = respostaBase(simulacao,
                "Simulacao precisa de analise humana", motivo);
        response.setPermiteEncaminhar(true);
        if (registrarCard) {
            registrarCard(simulacao, response);
        }
        return response;
    }

    private String validarReserva(Reserva reserva) {
        if (reserva == null) return "Nao foi possivel carregar a reserva informada.";
        if (normalizar(reserva.getStatus()).contains("cancel")) return "A reserva esta cancelada.";
        if (reserva.getDataEmissao() == null && !possuiBilhetes(reserva)) return "A reserva ainda nao esta emitida.";
        if (!todosPassageirosComBilheteAtivo(reserva)) {
            return "Nem todos os passageiros possuem bilhete ativo. A equipe precisa analisar o caso.";
        }
        if (reserva.getViagens() == null || reserva.getViagens().isEmpty()) return "A reserva nao retornou trechos.";
        return null;
    }

    private List<Integer> indicesTrechosElegiveis(Reserva reserva) {
        List<Integer> resultado = new ArrayList<>();
        Set<String> nacionais = aeroportoService.findIatasAeroportosNacionais();
        if (nacionais == null || nacionais.isEmpty() || reserva == null || reserva.getViagens() == null) {
            return resultado;
        }
        for (int i = 0; i < reserva.getViagens().size(); i++) {
            TrechoReserva trecho = reserva.getViagens().get(i);
            if (trecho == null || trecho.getVoos() == null || trecho.getVoos().isEmpty()) continue;
            String origem = iata(trecho.getOrigem());
            String destino = iata(trecho.getDestino());
            if (!nacionais.contains(origem) || !nacionais.contains(destino)) continue;
            if (!trechoFuturo(trecho) || !trechoStatusElegivel(trecho)
                    || trechoCodeshare(trecho) || vazio(companhiaTrecho(trecho))) continue;
            resultado.add(i);
        }
        return resultado;
    }

    private boolean trechoStatusElegivel(TrechoReserva trecho) {
        for (Voo voo : trecho.getVoos()) {
            String status = normalizar(voo == null ? null : voo.getStatus());
            if (status.contains("cancel") || status.contains("voado") || status.contains("flown")
                    || status.contains("utilizado") || status.contains("reembols")) {
                return false;
            }
        }
        return true;
    }

    private boolean trechoFuturo(TrechoReserva trecho) {
        Voo primeiro = primeiroVoo(trecho);
        return primeiro != null && dataHora(primeiro, true).isAfter(LocalDateTime.now());
    }

    private boolean trechoCodeshare(TrechoReserva trecho) {
        String companhia = companhiaTrecho(trecho);
        for (Voo voo : trecho.getVoos()) {
            if (Boolean.TRUE.equals(voo.getIsCodeShare())) return true;
            String mandatoria = voo.getCiaMandatoria() == null ? null : voo.getCiaMandatoria().getCodigoIata();
            String operadora = voo.getCiaOperadora() == null ? null : voo.getCiaOperadora().getCodigoIata();
            if (!vazio(mandatoria) && !vazio(operadora) && !mandatoria.equalsIgnoreCase(operadora)) return true;
            if (!vazio(mandatoria) && !vazio(companhia) && !mandatoria.equalsIgnoreCase(companhia)) return true;
        }
        return false;
    }

    private Reserva carregarReserva(SimulacaoRemarcacao simulacao, SessaoChatResponse sessao) {
        ConsultarLocalizadorResponse response = aereoClient.carregarReserva(
                montarConsultaLocalizador(simulacao, sessao));
        if (response == null || response.getException() != null || response.getReservas() == null) {
            return null;
        }
        return response.getReservas().stream()
                .filter(item -> item != null && simulacao.getLocalizador().equalsIgnoreCase(item.getLocalizador()))
                .findFirst()
                .orElse(response.getReservas().stream().filter(java.util.Objects::nonNull).findFirst().orElse(null));
    }

    private ConsultarLocalizadorRequest montarConsultaLocalizador(SimulacaoRemarcacao simulacao,
                                                                   SessaoChatResponse sessao) {
        ConsultarLocalizadorRequest request = new ConsultarLocalizadorRequest();
        request.setSistema("Wooba");
        request.setLocalizador(simulacao.getLocalizador());
        com.confApi.aereo.dto.Agencia agencia = new com.confApi.aereo.dto.Agencia();
        agencia.setCodgAgencia(simulacao.getCodgAgencia() == null ? null : String.valueOf(simulacao.getCodgAgencia()));
        agencia.setNome(sessao.getAgencia() == null ? null : sessao.getAgencia().getNomeAgencia());
        agencia.setUnidade(sessao.getUnidade() == null ? null : sessao.getUnidade().getNomeUnidade());
        agencia.setCodgSistemaBackoffice(sessao.getAgencia() == null ? null : sessao.getAgencia().getCodgSistemaBackoffice());
        request.setAgencia(agencia);

        IdentificacaoAgenciaModel identificacao = new IdentificacaoAgenciaModel();
        identificacao.setCodgAgencia(simulacao.getCodgAgencia());
        identificacao.setCodgUnidade(simulacao.getCodgUnidade());
        identificacao.setCodgUsuario(simulacao.getCodgUsuario());
        identificacao.setCodgErp(inteiro(sessao.getAgencia() == null ? null : sessao.getAgencia().getCodgSistemaBackoffice()));
        request.setIdentificacaoAgenciaModel(identificacao);
        return request;
    }

    private PesquisaRequestDTO montarPesquisa(SimulacaoRemarcacao simulacao,
                                               Reserva reserva,
                                               TrechoReserva original,
                                               SessaoChatResponse sessao,
                                               RemarcacaoRequest.Pesquisar criterios) {
        PesquisaRequestDTO request = new PesquisaRequestDTO();
        request.setAgencia(simulacao.getCodgAgencia() == null ? null : String.valueOf(simulacao.getCodgAgencia()));
        request.setUnidade(simulacao.getCodgUnidade() == null ? null : String.valueOf(simulacao.getCodgUnidade()));
        request.setTipoPesquisa(TipoPesquisa.ONEWAY);
        request.setTipoConsulta(TipoConsulta.NACIONAL);
        request.setAeroportoOrigem(new Aeroporto(simulacao.getOrigem(), descricao(original.getOrigem())));
        request.setAeroportoDestino(new Aeroporto(simulacao.getDestino(), descricao(original.getDestino())));
        request.setDataIda(Date.from(criterios.getData().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        Map<String, Integer> passageiros = contagensPassageiros(reserva);
        request.setQtdADT(passageiros.getOrDefault("ADT", 0));
        request.setQtdCHD(passageiros.getOrDefault("CHD", 0));
        request.setQtdINF(passageiros.getOrDefault("INF", 0));
        request.setPesquisa(new ParametrosPesquisa(Classe.ECONOMICA, Ordenacao.VALOR,
                TipoBagagem.TODAS_AS_OPCOES, TipoTarifa.OW));
        request.setSistemas(Collections.singletonList(new Sistema(0, "WO", 0)));
        Companhia companhia = new Companhia();
        companhia.setId(idCompanhia(original));
        companhia.setCodigoIata(simulacao.getCompanhiaIata());
        companhia.setDescricao(simulacao.getCompanhiaIata());
        request.setCompanhias(Collections.singletonList(companhia));
        IdentificacaoAgenciaModel identificacao = new IdentificacaoAgenciaModel();
        identificacao.setCodgAgencia(simulacao.getCodgAgencia());
        identificacao.setCodgUnidade(simulacao.getCodgUnidade());
        identificacao.setCodgUsuario(simulacao.getCodgUsuario());
        identificacao.setCodgErp(inteiro(sessao.getAgencia() == null ? null : sessao.getAgencia().getCodgSistemaBackoffice()));
        request.setIdentificacaoAgenciaModel(identificacao);
        return request;
    }

    private List<Trecho> filtrarOpcoes(List<PesquisaResponse> respostas,
                                       SimulacaoRemarcacao simulacao,
                                       RemarcacaoRequest.Pesquisar criterios) {
        if (respostas == null) return new ArrayList<>();
        List<Trecho> resultado = new ArrayList<>();
        Set<String> chaves = new HashSet<>();
        for (PesquisaResponse resposta : respostas) {
            if (resposta == null || resposta.getTrechos1() == null) continue;
            for (Trecho trecho : resposta.getTrechos1()) {
                if (!opcaoCompativel(trecho, simulacao, criterios)) continue;
                String chave = numerosVoos(trecho) + "|" + horaPrimeiroVoo(trecho) + "|" + trecho.getSistema();
                if (chaves.add(chave)) resultado.add(trecho);
            }
        }
        resultado.sort((a, b) -> valorMenor(a).compareTo(valorMenor(b)));
        return resultado.stream().limit(LIMITE_OPCOES).collect(Collectors.toList());
    }

    private boolean opcaoCompativel(Trecho trecho,
                                     SimulacaoRemarcacao simulacao,
                                     RemarcacaoRequest.Pesquisar criterios) {
        if (trecho == null || trecho.getVoos() == null || trecho.getVoos().isEmpty()
                || trecho.getFamilias() == null || trecho.getFamilias().isEmpty()) return false;
        if (!simulacao.getOrigem().equalsIgnoreCase(iata(trecho.getOrigem()))
                || !simulacao.getDestino().equalsIgnoreCase(iata(trecho.getDestino()))) return false;
        if (!simulacao.getCompanhiaIata().equalsIgnoreCase(companhiaTrecho(trecho))) return false;
        if (Boolean.TRUE.equals(criterios.getSomenteDireto())
                && (trecho.getVoos().size() > 1 || (trecho.getNumeroParadas() != null && trecho.getNumeroParadas() > 0))) return false;
        for (Voo voo : trecho.getVoos()) {
            if (Boolean.TRUE.equals(voo.getIsCodeShare())) return false;
            String iata = voo.getCiaMandatoria() == null ? null : voo.getCiaMandatoria().getCodigoIata();
            if (!vazio(iata) && !simulacao.getCompanhiaIata().equalsIgnoreCase(iata)) return false;
        }
        return periodoCompativel(horaPrimeiroVoo(trecho), criterios.getPeriodo());
    }

    private TarifarRequest montarTarifacao(Trecho trecho, FamiliaPreco familia, Reserva reserva) {
        TarifarRequest request = new TarifarRequest();
        request.setSistema(trecho.getSistema());
        request.setAgencia(null);
        request.setIdentificacaoViagem(trecho.getIdentificacaoDaViagem());
        request.setClasses(new ArrayList<>());
        for (Voo voo : trecho.getVoos()) {
            ClasseSelecionada classe = new ClasseSelecionada();
            classe.setBaseTarifaria(familia.getBaseTarifaria());
            classe.setClasse(familia.getClasse());
            classe.setFamilia(familia.getFamilia() == null ? null : familia.getFamilia().getCodgFamilia());
            classe.setNumero(voo.getNumeroVoo());
            classe.setIdentificacaoDeVoo(voo.getIdentificacaoDoVoo());
            classe.setSistema(trecho.getSistema());
            classe.setTrecho(1);
            request.getClasses().add(classe);
        }
        request.setPassageiroTipoQtds(new ArrayList<>());
        contagensPassageiros(reserva).forEach((tipo, quantidade) -> {
            if (quantidade > 0) {
                PassageiroTipoQtd item = new PassageiroTipoQtd();
                item.setTipo(tipo);
                item.setQuantidade(quantidade);
                request.getPassageiroTipoQtds().add(item);
            }
        });
        return request;
    }

    private RegraAereaAlteracaoConsultaRequest montarRequestRegra(Reserva reserva,
                                                                  TrechoReserva trecho,
                                                                  BigDecimal novaTarifa,
                                                                  BigDecimal novasTaxas) {
        Voo voo = primeiroVoo(trecho);
        RegraAereaAlteracaoConsultaRequest request = new RegraAereaAlteracaoConsultaRequest();
        request.setCompanhia(companhiaTrecho(trecho));
        request.setMercado("NACIONAL");
        request.setFamiliaTarifaria(voo == null ? null : primeiro(voo.getFamilia(), voo.getFamiliaCodigo()));
        request.setCodigoTarifario(voo == null ? null : primeiro(voo.getFamiliaCodigo(), voo.getBaseTarifaria()));
        request.setClasseReserva(voo == null ? null : primeiro(voo.getClasse(), primeiraLetra(voo.getBaseTarifaria())));
        request.setTipoEvento("REMARCACAO");
        request.setMomento("ANTES_EMBARQUE");
        request.setValorTarifa(valorTarifaOriginal(reserva));
        request.setValorTaxas(valorTaxasOriginais(reserva));
        request.setValorNovaTarifa(novaTarifa);
        request.setValorNovasTaxas(novasTaxas);
        request.setTaxaServico(BigDecimal.ZERO);
        request.setValorTotalReserva(decimal(reserva.getValorReserva() == null ? null : reserva.getValorReserva().getValor()));
        request.setQuantidadePassageiros(Math.max(1, reserva.getPassageiros() == null ? 0 : reserva.getPassageiros().size()));
        request.setQuantidadeTrechos(1);
        request.setExigirRegraAprovada(true);
        request.setValidadeMaximaDias(90);
        return request;
    }

    private RemarcacaoSimulacaoResponse.Previa montarPrevia(RegraAereaAlteracaoConsultaResponse regra,
                                                             RemarcacaoSimulacaoResponse.OpcaoVoo opcao,
                                                             RemarcacaoSimulacaoResponse.Familia familia,
                                                             SimulacaoRemarcacao simulacao) {
        RegraAereaAlteracaoCalculoResponse calculo = regra.getCalculo();
        RemarcacaoSimulacaoResponse.Previa previa = new RemarcacaoSimulacaoResponse.Previa();
        previa.setVoo(opcao);
        previa.setFamilia(familia);
        previa.setMoeda(vazio(calculo.getMoeda()) ? "BRL" : calculo.getMoeda());
        previa.setTarifaOriginal(calculo.getValorTarifaBase());
        previa.setNovaTarifa(calculo.getValorNovaTarifa());
        previa.setMulta(zero(calculo.getValorMulta()));
        previa.setDiferencaTarifaria(zero(calculo.getDiferencaTarifaria()));
        previa.setDiferencaTaxas(zero(calculo.getDiferencaTaxas()));
        previa.setTaxaServico(zero(calculo.getTaxaServico()));
        previa.setTotalEstimado(calculo.getTotalPrevisto());
        previa.setCalculoCompleto(Boolean.TRUE.equals(calculo.getCalculoCompleto()));
        previa.setRegraResumo(primeiro(regra.getMensagem(), calculo.getResumo()));
        previa.setAviso("Esta e uma estimativa, nao uma alteracao concluida. Valores e assentos dependem de nova validacao pela companhia.");
        LocalDateTime validadeTarifa = LocalDateTime.now().plusMinutes(15);
        previa.setValidoAte(simulacao.getExpiraEm() != null && simulacao.getExpiraEm().isBefore(validadeTarifa)
                ? simulacao.getExpiraEm() : validadeTarifa);
        return previa;
    }

    private RemarcacaoSimulacaoResponse respostaCriterios(SimulacaoRemarcacao simulacao,
                                                           TrechoReserva trecho,
                                                           String mensagem) {
        RemarcacaoSimulacaoResponse response = respostaBase(simulacao,
                "Quando deseja viajar?", mensagem);
        response.setCriterios(montarCriterios(trecho, dataOriginal(trecho), "QUALQUER", false));
        return response;
    }

    private RemarcacaoSimulacaoResponse.Criterios montarCriterios(TrechoReserva trecho,
                                                                   LocalDate data,
                                                                   String periodo,
                                                                   boolean direto) {
        RemarcacaoSimulacaoResponse.Criterios criterios = new RemarcacaoSimulacaoResponse.Criterios();
        criterios.setOrigem(iata(trecho.getOrigem()));
        criterios.setDestino(iata(trecho.getDestino()));
        criterios.setCompanhia(companhiaTrecho(trecho));
        criterios.setDataMinima(LocalDate.now());
        criterios.setDataMaxima(LocalDate.now().plusDays(330));
        criterios.setDataSugerida(data.isBefore(LocalDate.now()) ? LocalDate.now() : data);
        criterios.setPeriodo(vazio(periodo) ? "QUALQUER" : periodo.toUpperCase(Locale.ROOT));
        criterios.setSomenteDireto(direto);
        criterios.setObservacao("A rota e a companhia sao mantidas conforme a reserva original.");
        return criterios;
    }

    private List<RemarcacaoSimulacaoResponse.Trecho> montarTrechos(Reserva reserva,
                                                                   List<Integer> indices,
                                                                   Integer selecionado) {
        List<RemarcacaoSimulacaoResponse.Trecho> resultado = new ArrayList<>();
        for (Integer indice : indices) {
            TrechoReserva item = trecho(reserva, indice);
            Voo primeiro = primeiroVoo(item);
            Voo ultimo = item.getVoos().get(item.getVoos().size() - 1);
            RemarcacaoSimulacaoResponse.Trecho dto = new RemarcacaoSimulacaoResponse.Trecho();
            dto.setIndice(indice);
            dto.setOrigem(iata(item.getOrigem()));
            dto.setDestino(iata(item.getDestino()));
            dto.setCompanhia(companhiaTrecho(item));
            dto.setDataPartida(formatarData(primeiro.getDataPartida()));
            dto.setHoraPartida(primeiro.getHoraPartida());
            dto.setDataChegada(formatarData(ultimo.getDataChegada()));
            dto.setHoraChegada(ultimo.getHoraChegada());
            dto.setNumeroVoos(numerosVoos(item));
            dto.setSelecionado(indice.equals(selecionado));
            resultado.add(dto);
        }
        return resultado;
    }

    private List<RemarcacaoSimulacaoResponse.OpcaoVoo> montarOpcoes(List<Trecho> opcoes) {
        List<RemarcacaoSimulacaoResponse.OpcaoVoo> resultado = new ArrayList<>();
        for (int i = 0; i < opcoes.size(); i++) resultado.add(montarOpcao(opcoes.get(i), i));
        return resultado;
    }

    private RemarcacaoSimulacaoResponse.OpcaoVoo montarOpcao(Trecho trecho, Integer indice) {
        Voo primeiro = trecho.getVoos().get(0);
        Voo ultimo = trecho.getVoos().get(trecho.getVoos().size() - 1);
        RemarcacaoSimulacaoResponse.OpcaoVoo dto = new RemarcacaoSimulacaoResponse.OpcaoVoo();
        dto.setIndice(indice);
        dto.setSistema(trecho.getSistema());
        dto.setCompanhia(companhiaTrecho(trecho));
        dto.setOrigem(iata(trecho.getOrigem()));
        dto.setDestino(iata(trecho.getDestino()));
        dto.setDataPartida(formatarData(primeiro.getDataPartida()));
        dto.setHoraPartida(primeiro.getHoraPartida());
        dto.setDataChegada(formatarData(ultimo.getDataChegada()));
        dto.setHoraChegada(ultimo.getHoraChegada());
        dto.setDuracao(primeiro(trecho.getTempoDeDuracao(), primeiro.getDuracao()));
        dto.setParadas(trecho.getNumeroParadas() == null ? Math.max(0, trecho.getVoos().size() - 1) : trecho.getNumeroParadas());
        dto.setNumerosVoos(numerosVoos(trecho));
        dto.setMenorValor(valorMenor(trecho));
        for (int i = 0; i < trecho.getFamilias().size(); i++) {
            dto.getFamilias().add(montarFamilia(trecho.getFamilias().get(i), i));
        }
        return dto;
    }

    private RemarcacaoSimulacaoResponse.Familia montarFamilia(FamiliaPreco familia, Integer indice) {
        RemarcacaoSimulacaoResponse.Familia dto = new RemarcacaoSimulacaoResponse.Familia();
        dto.setIndice(indice);
        dto.setNome(familia.getFamilia() == null ? primeiro(familia.getTipo(), "Tarifa")
                : primeiro(familia.getFamilia().getDescricaoFamilia(), familia.getFamilia().getCodgFamilia()));
        dto.setCabine(familia.getCabine());
        dto.setClasse(familia.getClasse());
        dto.setBagagem(descricaoBagagem(familia));
        dto.setValor(valorFamilia(familia));
        return dto;
    }

    private RemarcacaoSimulacaoResponse respostaBase(SimulacaoRemarcacao simulacao,
                                                      String titulo,
                                                      String mensagem) {
        RemarcacaoSimulacaoResponse response = new RemarcacaoSimulacaoResponse();
        response.setId(simulacao.getId());
        response.setConversaId(simulacao.getConversaId());
        response.setStatus(simulacao.getStatus());
        response.setLocalizador(simulacao.getLocalizador());
        response.setCompanhiaIata(simulacao.getCompanhiaIata());
        response.setTitulo(titulo);
        response.setMensagem(mensagem);
        response.setExpiraEm(simulacao.getExpiraEm());
        return response;
    }

    private void registrarCard(SimulacaoRemarcacao simulacao, RemarcacaoSimulacaoResponse response) {
        Map<String, Object> conteudo = new LinkedHashMap<>();
        conteudo.put("schema", SCHEMA);
        conteudo.put("remarcacao", response);
        chatService.registrarMensagemBot(simulacao.getConversaId(), response.getMensagem(), json(conteudo));
    }

    private void registrarEvento(SimulacaoRemarcacao simulacao,
                                  String tipo,
                                  String descricao,
                                  String dadosJson) {
        ConversaEvento evento = new ConversaEvento();
        evento.setConversaId(simulacao.getConversaId());
        evento.setTipoEvento(tipo);
        evento.setCodgUsuario(simulacao.getCodgUsuario());
        evento.setDescricao(descricao);
        evento.setDadosJson(dadosJson);
        manager.post("chat-confianca/persistencia/conversa-eventos", evento, ConversaEvento.class);
    }

    private SimulacaoRemarcacao salvar(SimulacaoRemarcacao simulacao) {
        return manager.post("chat-confianca/persistencia/simulacoes-remarcacao", simulacao, SimulacaoRemarcacao.class);
    }

    private SimulacaoRemarcacao buscarValidar(Long id, Integer codgUsuario) {
        if (id == null || codgUsuario == null) throw regra(400, "Informe a simulacao e o usuario.");
        SimulacaoRemarcacao simulacao = manager.get(
                "chat-confianca/persistencia/simulacoes-remarcacao/" + id, SimulacaoRemarcacao.class);
        if (simulacao == null) throw regra(404, "Simulacao nao encontrada.");
        if (!codgUsuario.equals(simulacao.getCodgUsuario())) throw regra(403, "Usuario nao pertence a simulacao.");
        validarConversaSolicitante(simulacao.getConversaId(), codgUsuario);
        if (EXPIRADO.equals(simulacao.getStatus())
                || (simulacao.getExpiraEm() != null && simulacao.getExpiraEm().isBefore(LocalDateTime.now()))) {
            throw regra(409, "A simulacao expirou. Inicie uma nova pesquisa para obter valores atuais.");
        }
        return simulacao;
    }

    private Conversa validarConversaSolicitante(Long conversaId, Integer codgUsuario) {
        Conversa conversa = chatService.buscarConversa(conversaId);
        if (conversa == null) throw regra(404, "Conversa nao encontrada.");
        if (!codgUsuario.equals(conversa.getSolicitanteCodgUsuario())) {
            throw regra(403, "Usuario nao e o solicitante da conversa.");
        }
        return conversa;
    }

    private SessaoChatResponse montarSessao(SimulacaoRemarcacao simulacao) {
        return chatService.montarSessao(simulacao.getCodgUsuario(), simulacao.getCodgAgencia());
    }

    private TrechoReserva trecho(Reserva reserva, Integer indice) {
        return item(reserva == null ? null : reserva.getViagens(), indice, "Trecho nao encontrado na reserva atual.");
    }

    private <T> T item(List<T> lista, Integer indice, String mensagem) {
        if (lista == null || indice == null || indice < 0 || indice >= lista.size() || lista.get(indice) == null) {
            throw regra(400, mensagem);
        }
        return lista.get(indice);
    }

    private boolean regraPermite(RegraAereaAlteracaoConsultaResponse response) {
        return response != null && response.getRegra() != null
                && Boolean.TRUE.equals(response.getRegra().getPermiteAlteracao())
                && response.getStatus() != null && response.getStatus().startsWith("PERMITIDA");
    }

    private boolean possuiBilhetes(Reserva reserva) {
        return reserva.getPassageiros() != null && reserva.getPassageiros().stream()
                .filter(java.util.Objects::nonNull)
                .anyMatch(p -> p.getBilhetes() != null && !p.getBilhetes().isEmpty());
    }

    private boolean todosPassageirosComBilheteAtivo(Reserva reserva) {
        if (reserva.getPassageiros() == null || reserva.getPassageiros().isEmpty()) return false;
        for (Passageiro passageiro : reserva.getPassageiros()) {
            if (passageiro == null || passageiro.getBilhetes() == null || passageiro.getBilhetes().isEmpty()) return false;
            boolean ativo = false;
            for (Bilhete bilhete : passageiro.getBilhetes()) {
                if (bilhete == null || vazio(bilhete.getNumero())) continue;
                String status = normalizar(bilhete.getStatus());
                if (!status.contains("cancel") && !status.contains("reembols")) ativo = true;
            }
            if (!ativo) return false;
        }
        return true;
    }

    private Map<String, Integer> contagensPassageiros(Reserva reserva) {
        Map<String, Integer> resultado = new LinkedHashMap<>();
        resultado.put("ADT", 0);
        resultado.put("CHD", 0);
        resultado.put("INF", 0);
        if (reserva != null && reserva.getPassageiros() != null) {
            for (Passageiro passageiro : reserva.getPassageiros()) {
                String faixa = normalizar(passageiro == null ? null : passageiro.getFaixaEtaria());
                String tipo = faixa.contains("inf") || faixa.contains("bebe") ? "INF"
                        : faixa.contains("chd") || faixa.contains("crianc") ? "CHD" : "ADT";
                resultado.put(tipo, resultado.get(tipo) + 1);
            }
        }
        if (resultado.values().stream().mapToInt(Integer::intValue).sum() == 0) resultado.put("ADT", 1);
        return resultado;
    }

    private BigDecimal valorTarifaOriginal(Reserva reserva) {
        ValorBase base = reserva.getValorReserva() == null ? null : reserva.getValorReserva().getValorBase();
        return ratear(decimal(base == null ? null : base.getTarifa()), reserva);
    }

    private BigDecimal valorTaxasOriginais(Reserva reserva) {
        ValorBase base = reserva.getValorReserva() == null ? null : reserva.getValorReserva().getValorBase();
        if (base == null) return BigDecimal.ZERO;
        BigDecimal taxas = zero(decimal(base.getTaxaEmbarque()))
                .add(zero(decimal(base.getTaxaDU())))
                .add(zero(decimal(base.getRAV())))
                .add(zero(decimal(base.getRC())))
                .add(zero(decimal(base.getTaxaAssento())));
        return ratear(taxas, reserva);
    }

    private BigDecimal ratear(BigDecimal valor, Reserva reserva) {
        if (valor == null) return null;
        int trechos = reserva.getViagens() == null || reserva.getViagens().isEmpty() ? 1 : reserva.getViagens().size();
        return valor.divide(BigDecimal.valueOf(trechos), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal valorTarifa(Preco preco) {
        return primeiroDecimal(preco.getTotalTarifa(), preco.getTarifa());
    }

    private BigDecimal valorTaxas(Preco preco, BigDecimal tarifa) {
        BigDecimal total = primeiroDecimal(preco.getTotalGeral(), preco.getTotal());
        if (total != null && tarifa != null) {
            BigDecimal diferenca = total.subtract(tarifa);
            return diferenca.signum() < 0 ? BigDecimal.ZERO : diferenca.setScale(2, RoundingMode.HALF_UP);
        }
        return zero(decimal(preco.getTotalTaxaEmbarque()))
                .add(zero(decimal(preco.getTotalTaxaServico())))
                .add(zero(decimal(preco.getTotalTaxaDeCombustivel())))
                .add(zero(decimal(preco.getTotalTaxaAssento())))
                .add(zero(decimal(preco.getTotalTaxaBagagem())));
    }

    private BigDecimal valorMenor(Trecho trecho) {
        if (trecho == null || trecho.getFamilias() == null) return BigDecimal.valueOf(Long.MAX_VALUE);
        return trecho.getFamilias().stream().map(this::valorFamilia).filter(java.util.Objects::nonNull)
                .min(BigDecimal::compareTo).orElse(BigDecimal.valueOf(Long.MAX_VALUE));
    }

    private BigDecimal valorFamilia(FamiliaPreco familia) {
        if (familia == null || familia.getPreco() == null) return null;
        return primeiroDecimal(familia.getPreco().getTotalGeral(), familia.getPreco().getTotal(),
                familia.getPreco().getTotalTarifa(), familia.getPreco().getTarifa());
    }

    private String descricaoBagagem(FamiliaPreco familia) {
        if (!Boolean.TRUE.equals(familia.getBagagemInclusa())
                && (familia.getBagagemQuantidade() == null || familia.getBagagemQuantidade() < 1)) return "Sem bagagem despachada";
        if (familia.getBagagemQuantidade() != null && familia.getBagagemQuantidade() > 0) {
            return familia.getBagagemQuantidade() + " bagagem(ns)"
                    + (familia.getBagagemPeso() == null ? "" : " de ate " + familia.getBagagemPeso().intValue() + " kg");
        }
        return "Bagagem incluida";
    }

    private String companhiaTrecho(TrechoReserva trecho) {
        if (trecho == null) return null;
        if (trecho.getCompanhia() != null && !vazio(trecho.getCompanhia().getCodigoIata())) {
            return trecho.getCompanhia().getCodigoIata().toUpperCase(Locale.ROOT);
        }
        Voo voo = primeiroVoo(trecho);
        return voo == null || voo.getCiaMandatoria() == null ? null : voo.getCiaMandatoria().getCodigoIata();
    }

    private String companhiaTrecho(Trecho trecho) {
        if (trecho == null) return null;
        if (trecho.getCompanhia() != null && !vazio(trecho.getCompanhia().getCodigoIata())) {
            return trecho.getCompanhia().getCodigoIata().toUpperCase(Locale.ROOT);
        }
        if (trecho.getVoos() == null || trecho.getVoos().isEmpty()) return null;
        Voo voo = trecho.getVoos().get(0);
        return voo.getCiaMandatoria() == null ? null : voo.getCiaMandatoria().getCodigoIata();
    }

    private Integer idCompanhia(TrechoReserva trecho) {
        if (trecho.getCompanhia() != null && trecho.getCompanhia().getId() != null) return trecho.getCompanhia().getId();
        Voo voo = primeiroVoo(trecho);
        return voo == null || voo.getCiaMandatoria() == null ? 0 : voo.getCiaMandatoria().getId();
    }

    private Voo primeiroVoo(TrechoReserva trecho) {
        return trecho == null || trecho.getVoos() == null || trecho.getVoos().isEmpty() ? null : trecho.getVoos().get(0);
    }

    private String numerosVoos(TrechoReserva trecho) {
        return trecho == null || trecho.getVoos() == null ? "" : trecho.getVoos().stream()
                .map(Voo::getNumeroVoo).filter(v -> !vazio(v)).collect(Collectors.joining(" / "));
    }

    private String numerosVoos(Trecho trecho) {
        return trecho == null || trecho.getVoos() == null ? "" : trecho.getVoos().stream()
                .map(Voo::getNumeroVoo).filter(v -> !vazio(v)).collect(Collectors.joining(" / "));
    }

    private String horaPrimeiroVoo(Trecho trecho) {
        return trecho == null || trecho.getVoos() == null || trecho.getVoos().isEmpty()
                ? null : trecho.getVoos().get(0).getHoraPartida();
    }

    private boolean periodoCompativel(String hora, String periodo) {
        if (vazio(periodo) || "QUALQUER".equalsIgnoreCase(periodo)) return true;
        int valor = horaInt(hora);
        return switch (periodo.toUpperCase(Locale.ROOT)) {
            case "MANHA" -> valor >= 5 && valor < 12;
            case "TARDE" -> valor >= 12 && valor < 18;
            case "NOITE" -> valor >= 18 || valor < 5;
            default -> true;
        };
    }

    private int horaInt(String valor) {
        return horaLocal(valor).getHour();
    }

    private LocalDateTime dataHora(Voo voo, boolean partida) {
        Date data = partida ? voo.getDataPartida() : voo.getDataChegada();
        String hora = partida ? voo.getHoraPartida() : voo.getHoraChegada();
        LocalDate dia = data == null ? LocalDate.now() : Instant.ofEpochMilli(data.getTime())
                .atZone(ZoneId.systemDefault()).toLocalDate();
        return LocalDateTime.of(dia, horaLocal(hora));
    }

    private LocalTime horaLocal(String valor) {
        if (vazio(valor)) return LocalTime.MIDNIGHT;
        String digitos = valor.replaceAll("[^0-9]", "");
        try {
            if (digitos.length() >= 4) return LocalTime.of(Integer.parseInt(digitos.substring(0, 2)), Integer.parseInt(digitos.substring(2, 4)));
            if (valor.contains(":")) {
                String[] partes = valor.split(":");
                return LocalTime.of(Integer.parseInt(partes[0]), Integer.parseInt(partes[1].replaceAll("[^0-9]", "")));
            }
        } catch (DateTimeParseException | NumberFormatException ignored) {
        }
        return LocalTime.MIDNIGHT;
    }

    private LocalDate dataOriginal(TrechoReserva trecho) {
        Voo voo = primeiroVoo(trecho);
        return voo == null || voo.getDataPartida() == null ? LocalDate.now()
                : Instant.ofEpochMilli(voo.getDataPartida().getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private void validarDataPesquisa(LocalDate data) {
        if (data.isBefore(LocalDate.now())) throw regra(400, "A data da pesquisa nao pode estar no passado.");
        if (data.isAfter(LocalDate.now().plusDays(330))) throw regra(400, "A data ultrapassa a janela de pesquisa de 330 dias.");
    }

    private String iata(Aeroporto aeroporto) {
        return aeroporto == null || aeroporto.getCodigoIata() == null ? null : aeroporto.getCodigoIata().toUpperCase(Locale.ROOT);
    }

    private String descricao(Aeroporto aeroporto) {
        return aeroporto == null ? null : aeroporto.getDescricao();
    }

    private String formatarData(Date data) {
        return data == null ? null : Instant.ofEpochMilli(data.getTime()).atZone(ZoneId.systemDefault()).toLocalDate().format(DATA_BR);
    }

    private BigDecimal primeiroDecimal(Double... valores) {
        if (valores == null) return null;
        for (Double valor : valores) if (valor != null && valor >= 0) return decimal(valor);
        return null;
    }

    private BigDecimal decimal(Double valor) {
        return valor == null ? null : BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal zero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private String primeiro(String... valores) {
        if (valores != null) for (String valor : valores) if (!vazio(valor)) return valor;
        return null;
    }

    private String primeiraLetra(String valor) {
        return vazio(valor) ? null : valor.substring(0, 1);
    }

    private Integer inteiro(String valor) {
        if (vazio(valor)) return null;
        try {
            return Integer.valueOf(valor.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizar(String valor) {
        return valor == null ? "" : java.text.Normalizer.normalize(valor, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").trim().toLowerCase(Locale.ROOT);
    }

    private boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }

    private String json(Object valor) {
        try {
            return valor == null ? null : mapper.writeValueAsString(valor);
        } catch (JsonProcessingException ex) {
            throw regra(500, "Nao foi possivel registrar os dados da simulacao.");
        }
    }

    private <T> T ler(String json, Class<T> tipo) {
        try {
            return mapper.readValue(json, tipo);
        } catch (JsonProcessingException ex) {
            throw regra(500, "Os dados salvos da simulacao estao invalidos.");
        }
    }

    private List<Trecho> lerOpcoes(String json) {
        if (vazio(json)) throw regra(409, "A pesquisa expirou ou nao possui opcoes.");
        try {
            return mapper.readValue(json, new TypeReference<List<Trecho>>() { });
        } catch (JsonProcessingException ex) {
            throw regra(500, "Nao foi possivel recuperar as opcoes pesquisadas.");
        }
    }

    private String tituloStatus(String status) {
        if (PREVIA_DISPONIVEL.equals(status)) return "Previa da alteracao";
        if (AGUARDANDO_OPCAO.equals(status)) return "Escolha um voo";
        if (AGUARDANDO_CRITERIOS.equals(status)) return "Quando deseja viajar?";
        if (NAO_ELEGIVEL.equals(status)) return "Simulacao precisa de analise humana";
        if (ENCAMINHADO.equals(status)) return "Solicitacao encaminhada";
        return "Simulacao de alteracao";
    }

    private String mensagemStatus(SimulacaoRemarcacao simulacao) {
        if (!vazio(simulacao.getMotivoBloqueio())) return simulacao.getMotivoBloqueio();
        if (PREVIA_DISPONIVEL.equals(simulacao.getStatus())) return "Confira os valores estimados.";
        if (ENCAMINHADO.equals(simulacao.getStatus())) return "A equipe recebeu o contexto da simulacao.";
        return "Continue a simulacao pelo ultimo card enviado na conversa.";
    }

    private RegraDeNegocioException regra(int status, String mensagem) {
        return new RegraDeNegocioException(status, mensagem);
    }

    @Data
    private static class SelecaoPersistida {
        private RemarcacaoSimulacaoResponse.OpcaoVoo opcao;
        private RemarcacaoSimulacaoResponse.Familia familia;
    }
}
