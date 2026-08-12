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
import com.confApi.aereo.dto.PrecoTipo;
import com.confApi.aereo.dto.Reserva;
import com.confApi.aereo.dto.Sistema;
import com.confApi.aereo.dto.TarifarRequest;
import com.confApi.aereo.dto.TarifarResponse;
import com.confApi.aereo.dto.Trecho;
import com.confApi.aereo.dto.ValorBase;
import com.confApi.aereo.dto.ValorPassageiro;
import com.confApi.aereo.eNums.Classe;
import com.confApi.aereo.eNums.Ordenacao;
import com.confApi.aereo.eNums.TipoBagagem;
import com.confApi.aereo.eNums.TipoConsulta;
import com.confApi.aereo.eNums.TipoPesquisa;
import com.confApi.aereo.eNums.TipoTarifa;
import com.confApi.chatconfianca.client.ChatConfiancaManagerClient;
import com.confApi.chatconfianca.dto.enums.StatusConversa;
import com.confApi.chatconfianca.dto.model.Conversa;
import com.confApi.chatconfianca.dto.model.ConversaEvento;
import com.confApi.chatconfianca.dto.model.SimulacaoRemarcacao;
import com.confApi.chatconfianca.dto.remarcacao.RemarcacaoRequest;
import com.confApi.chatconfianca.dto.remarcacao.RemarcacaoSimulacaoResponse;
import com.confApi.chatconfianca.dto.remarcacao.ReservasEmitidasRemarcacaoResponse;
import com.confApi.chatconfianca.dto.request.AdicionarTagConversaRequest;
import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.db.confManager.aeroporto.AeroportoService;
import com.confApi.db.confManager.regraAereaAlteracao.RegraAereaAlteracaoManagerService;
import com.confApi.db.confManager.regraAereaAlteracao.dto.RegraAereaAlteracaoCalculoResponse;
import com.confApi.db.confManager.regraAereaAlteracao.dto.RegraAereaAlteracaoConsultaRequest;
import com.confApi.db.confManager.regraAereaAlteracao.dto.RegraAereaAlteracaoConsultaResponse;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.db.confManager.reservaValor.ReservaValor;
import com.confApi.endPoints.reservaAereo.ReservaAereoApi;
import com.confApi.exception.RegraDeNegocioException;
import com.confApi.hub.aereo.dto.Aeroporto;
import com.confApi.hub.aereo.dto.Bilhete;
import com.confApi.hub.aereo.dto.Companhia;
import com.confApi.hub.aereo.dto.Passageiro;
import com.confApi.hub.aereo.dto.TrechoReserva;
import com.confApi.hub.aereo.dto.Voo;
import com.confApi.hub.enumerador.TipoLimite;
import com.confApi.hub.limites.LimitesService;
import com.confApi.hub.limites.dto.Disponibilidade;
import com.confApi.hub.limites.dto.LimiteCredito;
import com.confApi.hub.limites.dto.LimiteCreditoRQ;
import com.confApi.hub.limites.dto.StatusResponse;
import com.confApi.model.IdentificacaoAgenciaModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class ChatConfiancaRemarcacaoService {
    private static final Logger LOG = Logger.getLogger(ChatConfiancaRemarcacaoService.class.getName());
    private static final String SCHEMA = "chat.reschedule.v1";
    private static final String VALIDANDO = "VALIDANDO";
    private static final String AGUARDANDO_TRECHO = "AGUARDANDO_TRECHO";
    private static final String AGUARDANDO_PASSAGEIROS = "AGUARDANDO_PASSAGEIROS";
    private static final String AGUARDANDO_CRITERIOS = "AGUARDANDO_CRITERIOS";
    private static final String PESQUISANDO = "PESQUISANDO";
    private static final String AGUARDANDO_OPCAO = "AGUARDANDO_OPCAO";
    private static final String CALCULANDO = "CALCULANDO";
    private static final String PREVIA_DISPONIVEL = "PREVIA_DISPONIVEL";
    private static final String ENCAMINHADO = "ENCAMINHADO";
    private static final String NAO_ELEGIVEL = "NAO_ELEGIVEL";
    private static final String ERRO = "ERRO";
    private static final String EXPIRADO = "EXPIRADO";
    private static final String CANCELADO = "CANCELADO";
    private static final String ESCOPO_TODOS = "TODOS";
    private static final String ESCOPO_INDIVIDUAL = "INDIVIDUAL";
    private static final int PAGAMENTO_FATURA = 1;
    private static final int PAGAMENTO_CARTAO = 2;
    private static final BigDecimal LIMITE_DU_DIFERENCA_TARIFA = new BigDecimal("400.00");
    private static final BigDecimal DU_MINIMA_REMARCACAO = new BigDecimal("40.00");
    private static final BigDecimal PERCENTUAL_DU_ACIMA_LIMITE = new BigDecimal("0.10");
    private static final String PAGAMENTO_AGUARDANDO_PREFERENCIA = "AGUARDANDO_PREFERENCIA";
    private static final String PAGAMENTO_PREFERENCIA_REGISTRADA = "PREFERENCIA_REGISTRADA";
    private static final String PAGAMENTO_PREFERENCIA_SUJEITA_VALIDACAO =
            "PREFERENCIA_REGISTRADA_SUJEITA_VALIDACAO";
    private static final String PAGAMENTO_NAO_APLICAVEL = "NAO_APLICAVEL";
    private static final String FORMA_DISPONIVEL = "DISPONIVEL";
    private static final String FORMA_INDISPONIVEL = "INDISPONIVEL";
    private static final String FORMA_SUJEITA_VALIDACAO = "SUJEITA_VALIDACAO";
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int LIMITE_OPCOES = 5;
    private static final int STATUS_RESERVA_EMITIDA = 3;
    private static final int PAGINA_PADRAO_RESERVAS = 0;
    private static final int TAMANHO_PADRAO_RESERVAS = 10;
    private static final int TAMANHO_MAXIMO_RESERVAS = 50;
    private static final String CAMINHO_RESERVAS_EMITIDAS =
            "chat-confianca/consultas/remarcacoes/reservas-emitidas";
    private static final Set<String> COMPANHIAS_SUPORTADAS_REMARCACAO =
            Set.of("G3", "LA", "JJ", "AD");
    private static final Set<String> STATUS_FINAIS_CONSULTAVEIS = Set.of(
            ENCAMINHADO, NAO_ELEGIVEL, ERRO, EXPIRADO, CANCELADO);

    private final ChatConfiancaManagerClient manager;
    private final ChatConfiancaService chatService;
    private final AereoClient aereoClient;
    private final AeroportoService aeroportoService;
    private final RegraAereaAlteracaoManagerService regraService;
    private final LimitesService limitesService;
    private final ObjectMapper mapper;
    private final ReservaAereoApi reservaAereoApi;

    /**
     * Mantem o rateio dos valores originais por trecho disponivel para testes
     * com fornecedores. Por padrao, a simulacao usa o valor integral da reserva.
     */
    @Value("${chat-confianca.remarcacao.ratear-valores-originais-por-trecho:false}")
    private boolean ratearValoresOriginaisPorTrecho;

    public ChatConfiancaRemarcacaoService(ChatConfiancaManagerClient manager,
                                          ChatConfiancaService chatService,
                                          AereoClient aereoClient,
                                          AeroportoService aeroportoService,
                                          RegraAereaAlteracaoManagerService regraService,
                                          LimitesService limitesService,
                                          ObjectMapper mapper,
                                          ReservaAereoApi reservaAereoApi) {
        this.manager = manager;
        this.chatService = chatService;
        this.aereoClient = aereoClient;
        this.aeroportoService = aeroportoService;
        this.regraService = regraService;
        this.limitesService = limitesService;
        this.mapper = mapper;
        this.reservaAereoApi = reservaAereoApi;
    }

    public ReservasEmitidasRemarcacaoResponse listarReservasEmitidas(
            Long conversaId,
            Integer codgUsuario,
            String busca,
            LocalDate dataEmissaoInicio,
            LocalDate dataEmissaoFim,
            Integer page,
            Integer size) {
        Conversa conversa = validarConversaSolicitante(conversaId, codgUsuario);
        validarConversaConfiaAtiva(conversa);
        validarSessaoConversa(conversa, codgUsuario);
        validarPeriodoEmissao(dataEmissaoInicio, dataEmissaoFim);

        int pagina = normalizarPagina(page);
        int tamanho = normalizarTamanhoPagina(size);
        return consultarReservasEmitidasManager(
                conversa.getCodgAgencia(),
                null,
                busca,
                dataEmissaoInicio,
                dataEmissaoFim,
                pagina,
                tamanho);
    }

    public RemarcacaoSimulacaoResponse iniciar(RemarcacaoRequest.Iniciar request) {
        if (request == null || request.getConversaId() == null || request.getCodgUsuario() == null
                || vazio(request.getLocalizador())) {
            throw regra(400, "Informe a conversa, o usuario e o localizador da reserva.");
        }

        Conversa conversa = validarConversaSolicitante(request.getConversaId(), request.getCodgUsuario());
        validarConversaConfiaAtiva(conversa);
        SessaoChatResponse sessao = validarSessaoConversa(conversa, request.getCodgUsuario());
        String localizador = request.getLocalizador().trim().toUpperCase(Locale.ROOT);
        ReservasEmitidasRemarcacaoResponse.Item reservaSelecionada = resolverReservaSelecionada(
                conversa.getCodgAgencia(), request.getReservaId(), localizador);

        SimulacaoRemarcacao simulacao = new SimulacaoRemarcacao();
        simulacao.setConversaId(conversa.getId());
        simulacao.setReservaAereoId(reservaSelecionada.getReservaId());
        simulacao.setLocalizador(reservaSelecionada.getLocalizador().trim().toUpperCase(Locale.ROOT));
        simulacao.setCodgUsuario(request.getCodgUsuario());
        simulacao.setCodgAgencia(conversa.getCodgAgencia());
        Integer codgUnidade = conversa.getCodgUnidade();
        if (codgUnidade == null && sessao.getUnidade() != null) {
            codgUnidade = sessao.getUnidade().getCodgUnidade();
        }
        simulacao.setCodgUnidade(codgUnidade);
        simulacao.setCompanhiaIata(reservaSelecionada.getCompanhiaIata());
        simulacao.setStatus(VALIDANDO);
        simulacao.setExpiraEm(LocalDateTime.now().plusMinutes(30));
        simulacao = salvar(simulacao);
        registrarEvento(simulacao, "REMARCACAO_SIMULACAO_INICIADA",
                "Simulacao de alteracao iniciada para a reserva " + simulacao.getLocalizador() + ".", null);

        Reserva reserva = carregarReserva(simulacao, sessao, reservaSelecionada);
        String impedimento = validarReserva(reserva);
        if (impedimento != null) {
            return bloquear(simulacao, impedimento);
        }

        List<Integer> indicesElegiveis = indicesTrechosElegiveis(reserva);
        if (indicesElegiveis.isEmpty()) {
            return bloquear(simulacao,
                    "Nao encontrei trecho futuro nacional, ativo e sem codeshare que possa ser simulado automaticamente.");
        }

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

    public RemarcacaoSimulacaoResponse selecionarPassageiros(
            Long id,
            RemarcacaoRequest.SelecionarPassageiros request) {
        if (request == null || request.getCodgUsuario() == null || vazio(request.getEscopo())) {
            throw regra(400, "Informe o usuario e quem deseja remarcar.");
        }
        SimulacaoRemarcacao simulacao = buscarValidar(id, request.getCodgUsuario());
        if (simulacao.getTrechoIndice() == null) {
            throw regra(409, "Selecione o trecho antes dos passageiros.");
        }

        Reserva reserva = carregarReserva(simulacao, montarSessao(simulacao));
        TrechoReserva trecho = trecho(reserva, simulacao.getTrechoIndice());
        List<RemarcacaoSimulacaoResponse.Passageiro> passageiros = montarPassageiros(reserva, trecho);
        String escopo = request.getEscopo().trim().toUpperCase(Locale.ROOT);
        List<Integer> indicesSelecionados = new ArrayList<>();

        if (ESCOPO_TODOS.equals(escopo)) {
            if (!permiteSelecionarTodos(passageiros)) {
                throw regra(409,
                        "A selecao de todos nao esta disponivel. Verifique os bilhetes e o adulto responsavel pelo bebe.");
            }
            passageiros.forEach(item -> indicesSelecionados.add(item.getIndice()));
        } else if (ESCOPO_INDIVIDUAL.equals(escopo)) {
            if (request.getPassageiroIndice() == null) {
                throw regra(400, "Selecione o passageiro.");
            }
            RemarcacaoSimulacaoResponse.Passageiro passageiro = passageiros.stream()
                    .filter(item -> request.getPassageiroIndice().equals(item.getIndice()))
                    .findFirst()
                    .orElseThrow(() -> regra(400, "Passageiro nao pertence a reserva atual."));
            if (!passageiro.isElegivel()) {
                throw regra(409, primeiro(passageiro.getMotivoInelegibilidade(),
                        "Passageiro nao elegivel para este trecho."));
            }
            if ("INF".equals(passageiro.getTipo())) {
                throw regra(409, "Bebe deve ser remarcado com o adulto responsavel. Selecione todos ou fale com um atendente.");
            }
            indicesSelecionados.add(passageiro.getIndice());
        } else {
            throw regra(400, "Escolha um passageiro ou todos.");
        }

        salvarSelecaoPassageiros(simulacao, escopo, indicesSelecionados, passageiros);
        limparResultadosPosteriores(simulacao);
        simulacao.setStatus(AGUARDANDO_CRITERIOS);
        simulacao.setMotivoBloqueio(null);
        simulacao = salvar(simulacao);

        marcarPassageirosSelecionados(passageiros, indicesSelecionados);
        Map<String, Object> dadosEvento = new LinkedHashMap<>();
        dadosEvento.put("escopo", escopo);
        dadosEvento.put("passageiros", indicesSelecionados);
        registrarEvento(simulacao, "REMARCACAO_PASSAGEIROS_SELECIONADOS",
                ESCOPO_TODOS.equals(escopo)
                        ? "Todos os passageiros foram selecionados."
                        : "Passageiro selecionado para a simulacao.",
                json(dadosEvento));

        RemarcacaoSimulacaoResponse response = respostaCriterios(simulacao, trecho,
                "Passageiros confirmados. Informe a nova data e, se desejar, um periodo.");
        response.setPassageiros(passageiros);
        response.setPermiteSelecionarTodos(permiteSelecionarTodos(passageiros));
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
        SessaoChatResponse sessao = montarSessao(simulacao);
        Reserva reserva = carregarReserva(simulacao, sessao);
        List<Passageiro> passageirosSelecionados = passageirosSelecionados(reserva, simulacao);
        if (passageirosSelecionados.isEmpty()) {
            throw regra(409, "Selecione quem deseja remarcar antes de pesquisar novos voos.");
        }
        TrechoReserva original = trecho(reserva, simulacao.getTrechoIndice());
        simulacao.setStatus(PESQUISANDO);
        simulacao.setCriteriosJson(json(request));
        simulacao = salvar(simulacao);
        PesquisaRequestDTO pesquisa = montarPesquisa(
                simulacao, original, sessao, request, passageirosSelecionados);
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

        SessaoChatResponse sessao = montarSessao(simulacao);
        Reserva reserva = carregarReserva(simulacao, sessao);
        TrechoReserva original = trecho(reserva, simulacao.getTrechoIndice());
        List<Passageiro> passageirosSelecionados = passageirosSelecionados(reserva, simulacao);
        if (passageirosSelecionados.isEmpty()) {
            throw regra(409, "Selecione quem deseja remarcar antes de calcular a previa.");
        }
        simulacao.setOfertaSelecionadaJson(null);
        simulacao.setCalculoJson(null);
        limparPreferenciaPagamento(simulacao);
        simulacao.setStatus(CALCULANDO);
        simulacao = salvar(simulacao);

        TarifarResponse tarifa = aereoClient.tarifar(
                montarTarifacao(opcao, familia, passageirosSelecionados));
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

        if (!possuiTarifaParaTodos(tarifa.getPreco(), passageirosSelecionados)) {
            return bloquear(simulacao,
                    "A companhia nao retornou o valor completo para todos os tipos de passageiro selecionados. "
                            + "A equipe precisa cotar essa opcao manualmente.");
        }

        RemarcacaoSimulacaoResponse.OpcaoVoo opcaoView = montarOpcao(opcao, request.getOpcaoIndice());
        RemarcacaoSimulacaoResponse.Familia familiaView = montarFamilia(familia, request.getFamiliaIndice());
        CalculoPassageiros calculoPassageiros = montarPreviaPassageiros(
                reserva, original, tarifa.getPreco(), passageirosSelecionados,
                opcaoView, familiaView, simulacao);
        if (!calculoPassageiros.isPermitido()) {
            simulacao.setRegraSnapshotJson(json(calculoPassageiros.getRegras()));
            return bloquear(simulacao, calculoPassageiros.getMotivo());
        }
        RemarcacaoSimulacaoResponse.Previa previa = calculoPassageiros.getPrevia();
        SelecaoPersistida selecao = new SelecaoPersistida();
        selecao.setOpcao(opcaoView);
        selecao.setFamilia(familiaView);

        simulacao.setOfertaSelecionadaJson(json(selecao));
        simulacao.setCalculoJson(json(previa));
        simulacao.setRegraId(calculoPassageiros.getRegraId());
        simulacao.setRegraSnapshotJson(json(calculoPassageiros.getRegras()));
        limparPreferenciaPagamento(simulacao);
        simulacao.setPagamentoStatus(exigeFormaPagamento(previa)
                ? PAGAMENTO_AGUARDANDO_PREFERENCIA
                : PAGAMENTO_NAO_APLICAVEL);
        simulacao.setStatus(PREVIA_DISPONIVEL);
        simulacao = salvar(simulacao);

        RemarcacaoSimulacaoResponse response = respostaBase(simulacao,
                "Previa da alteracao",
                "Confira os valores estimados. Um atendente precisa validar a disponibilidade e concluir a remarcacao.");
        response.setPrevia(previa);
        preencherPagamento(response, simulacao, previa, true);
        Map<String, Object> dadosEvento = new LinkedHashMap<>();
        dadosEvento.put("totalEstimado", previa.getTotalEstimado());
        dadosEvento.put("companhia", simulacao.getCompanhiaIata());
        registrarEvento(simulacao, "REMARCACAO_PREVIA_GERADA",
                "Previa de remarcacao gerada para atendimento.", json(dadosEvento));
        registrarCard(simulacao, response);
        return response;
    }

    public RemarcacaoSimulacaoResponse selecionarFormaPagamento(
            Long id,
            RemarcacaoRequest.SelecionarFormaPagamento request) {
        if (request == null || request.getCodgUsuario() == null || request.getCodigo() == null) {
            throw regra(400, "Informe o usuario e a forma de pagamento.");
        }
        SimulacaoRemarcacao simulacao = buscarValidar(id, request.getCodgUsuario());
        RemarcacaoSimulacaoResponse.Previa previa = validarPreviaDisponivel(simulacao);
        if (!exigeFormaPagamento(previa)) {
            throw regra(409, "Esta previa nao possui diferenca a pagar.");
        }

        List<RemarcacaoSimulacaoResponse.FormaPagamento> formas =
                montarFormasPagamento(simulacao, totalPrevia(previa));
        RemarcacaoSimulacaoResponse.FormaPagamento forma = formas.stream()
                .filter(item -> request.getCodigo().equals(item.getCodigo()))
                .findFirst()
                .orElseThrow(() -> regra(400, "Forma de pagamento invalida."));
        if (!forma.isDisponivel()) {
            throw regra(409, primeiro(forma.getMensagem(),
                    "A forma de pagamento selecionada nao esta disponivel."));
        }

        LocalDateTime agora = LocalDateTime.now();
        simulacao.setFormaPagamentoCodigo(forma.getCodigo());
        simulacao.setFormaPagamentoDescricao(forma.getDescricao());
        simulacao.setPagamentoStatus(FORMA_SUJEITA_VALIDACAO.equals(forma.getStatus())
                ? PAGAMENTO_PREFERENCIA_SUJEITA_VALIDACAO
                : PAGAMENTO_PREFERENCIA_REGISTRADA);
        simulacao.setPagamentoSelecionadoEm(agora);
        simulacao = salvar(simulacao);

        RemarcacaoSimulacaoResponse.FormaPagamento selecionada = formaSelecionada(simulacao);
        RemarcacaoSimulacaoResponse response = respostaBase(simulacao,
                "Preferencia de pagamento registrada",
                "Nenhuma cobranca foi realizada. O atendente confirmara o valor e a forma de pagamento.");
        response.setPrevia(previa);
        response.setExigeFormaPagamento(true);
        response.setFormasPagamento(formas);
        response.setFormaPagamentoSelecionada(selecionada);
        response.setPermiteEncaminhar(chatService.departamentoRemarcacaoDisponivel(simulacao.getConversaId()));

        Map<String, Object> dadosEvento = new LinkedHashMap<>();
        dadosEvento.put("codigo", forma.getCodigo());
        dadosEvento.put("chave", forma.getChave());
        dadosEvento.put("descricao", forma.getDescricao());
        dadosEvento.put("statusDisponibilidade", forma.getStatus());
        dadosEvento.put("selecionadaEm", agora);
        registrarEvento(simulacao, "REMARCACAO_FORMA_PAGAMENTO_SELECIONADA",
                "Preferencia de pagamento registrada: " + forma.getDescricao() + ".",
                json(dadosEvento));
        registrarCard(simulacao, response);
        return response;
    }

    public RemarcacaoSimulacaoResponse encaminhar(Long id, RemarcacaoRequest.Encaminhar request) {
        if (request == null || request.getCodgUsuario() == null) {
            throw regra(400, "Informe o usuario.");
        }
        SimulacaoRemarcacao simulacao = buscarValidar(id, request.getCodgUsuario());
        RemarcacaoSimulacaoResponse.Previa previa = null;
        if (PREVIA_DISPONIVEL.equals(simulacao.getStatus())) {
            previa = validarPreviaDisponivel(simulacao);
            if (exigeFormaPagamento(previa) && !possuiPreferenciaPagamento(simulacao)) {
                throw regra(409,
                        "Escolha como prefere pagar a diferenca antes de falar com o atendente.");
            }
        } else if (!NAO_ELEGIVEL.equals(simulacao.getStatus())) {
            throw regra(409,
                    "Conclua a previa da remarcacao antes de falar com o atendente.");
        }

        chatService.encaminharConversaParaDepartamentoRemarcacao(
                simulacao.getConversaId(), request.getCodgUsuario(),
                "Cliente solicitou concluir a remarcacao da reserva " + simulacao.getLocalizador()
                        + " com base na simulacao " + simulacao.getId() + ".");
        RemarcacaoSimulacaoResponse response = respostaBase(simulacao,
                "Solicitacao encaminhada",
                "A equipe recebeu a reserva, o voo escolhido, a regra e a previa calculada.");
        response.setStatus(ENCAMINHADO);
        response.setPrevia(previa);
        response.setExigeFormaPagamento(exigeFormaPagamento(previa));
        response.setFormaPagamentoSelecionada(formaSelecionada(simulacao));
        preencherTrechoOriginal(response, simulacao);
        response.setPermiteEncaminhar(false);

        String resumo = montarResumoEncaminhamento(simulacao);
        Map<String, Object> contexto = new LinkedHashMap<>();
        contexto.put("schema", SCHEMA);
        contexto.put("remarcacao", response);
        contexto.put("handoffSchema", "chat.reschedule.handoff.v2");
        contexto.put("simulacaoId", simulacao.getId());
        contexto.put("localizador", simulacao.getLocalizador());
        contexto.put("oferta", vazio(simulacao.getOfertaSelecionadaJson())
                ? null : ler(simulacao.getOfertaSelecionadaJson(), SelecaoPersistida.class));
        contexto.put("previa", previa);
        contexto.put("pagamento", formaSelecionada(simulacao));
        chatService.registrarMensagemSistema(simulacao.getConversaId(), resumo, json(contexto));

        String statusAnterior = simulacao.getStatus();
        try {
            simulacao.setStatus(ENCAMINHADO);
            simulacao = salvar(simulacao);
        } catch (RuntimeException ex) {
            simulacao.setStatus(statusAnterior);
            throw ex;
        }
        adicionarTagRemarcacao(simulacao, request.getCodgUsuario());
        registrarEventoNaoBloqueante(simulacao, "REMARCACAO_ENCAMINHADA",
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
            if (previa.getPassageiros() != null && !previa.getPassageiros().isEmpty()) {
                resumo.append(" | Passageiro(s) ")
                        .append(previa.getPassageiros().stream()
                                .map(RemarcacaoSimulacaoResponse.PreviaPassageiro::getNome)
                                .filter(nome -> !vazio(nome))
                                .collect(Collectors.joining(", ")));
            }
            BigDecimal total = previa.getTotalSelecionado() == null
                    ? previa.getTotalEstimado() : previa.getTotalSelecionado();
            if (total != null) {
                resumo.append(" | Total estimado ")
                        .append(primeiro(previa.getMoeda(), "BRL"))
                        .append(" ").append(total);
            }
        }
        RemarcacaoSimulacaoResponse.FormaPagamento pagamento = formaSelecionada(simulacao);
        if (pagamento != null) {
            resumo.append(" | Pagamento preferido ").append(pagamento.getDescricao());
        }
        resumo.append(". Validar novamente disponibilidade, regra e valores antes de concluir.");
        return resumo.toString();
    }

    public RemarcacaoSimulacaoResponse consultar(Long id, Integer codgUsuario) {
        SimulacaoRemarcacao simulacao = buscarValidarParaConsulta(id, codgUsuario);
        RemarcacaoSimulacaoResponse response = respostaBase(simulacao,
                tituloStatus(simulacao.getStatus()), mensagemStatus(simulacao));
        if (AGUARDANDO_PASSAGEIROS.equals(simulacao.getStatus())
                || AGUARDANDO_CRITERIOS.equals(simulacao.getStatus())) {
            PassageirosPersistidos persistidos = selecaoPassageiros(simulacao);
            List<RemarcacaoSimulacaoResponse.Passageiro> passageiros = persistidos.getPassageiros();
            if (passageiros == null || passageiros.isEmpty()) {
                Reserva reserva = carregarReserva(simulacao, montarSessao(simulacao));
                if (reserva != null && simulacao.getTrechoIndice() != null) {
                    passageiros = montarPassageiros(
                            reserva, trecho(reserva, simulacao.getTrechoIndice()));
                }
            }
            if (passageiros != null) {
                marcarPassageirosSelecionados(passageiros, persistidos.getIndices());
                response.setPassageiros(passageiros);
                response.setPermiteSelecionarTodos(permiteSelecionarTodos(passageiros));
            }
        }
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
        if (AGUARDANDO_CRITERIOS.equals(simulacao.getStatus())) {
            preencherTrechoOriginal(response, simulacao);
        }
        if (!vazio(simulacao.getResultadosJson())) {
            response.setOpcoes(montarOpcoes(lerOpcoes(simulacao.getResultadosJson())));
        }
        RemarcacaoSimulacaoResponse.Previa previa = null;
        if (!vazio(simulacao.getCalculoJson())) {
            previa = ler(simulacao.getCalculoJson(), RemarcacaoSimulacaoResponse.Previa.class);
            response.setPrevia(previa);
        }
        if (PREVIA_DISPONIVEL.equals(simulacao.getStatus())) {
            preencherPagamento(response, simulacao, previa, true);
        } else {
            response.setExigeFormaPagamento(false);
            response.setFormaPagamentoSelecionada(formaSelecionada(simulacao));
        response.setPermiteEncaminhar(NAO_ELEGIVEL.equals(simulacao.getStatus())
                && chatService.departamentoRemarcacaoDisponivel(simulacao.getConversaId()));
        }
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

        Map<String, Object> dadosEvento = new LinkedHashMap<>();
        dadosEvento.put("trechoIndice", indice);
        dadosEvento.put("regraId", simulacao.getRegraId());
        registrarEvento(simulacao, "REMARCACAO_TRECHO_SELECIONADO",
                "Trecho " + simulacao.getOrigem() + " - " + simulacao.getDestino() + " selecionado.",
                json(dadosEvento));
        return prepararPassageiros(simulacao, reserva, trecho);
    }

    private RemarcacaoSimulacaoResponse prepararPassageiros(
            SimulacaoRemarcacao simulacao,
            Reserva reserva,
            TrechoReserva trecho) {
        List<RemarcacaoSimulacaoResponse.Passageiro> passageiros = montarPassageiros(reserva, trecho);
        List<RemarcacaoSimulacaoResponse.Passageiro> elegiveis = passageiros.stream()
                .filter(RemarcacaoSimulacaoResponse.Passageiro::isElegivel)
                .collect(Collectors.toList());
        if (elegiveis.isEmpty()) {
            return bloquear(simulacao,
                    "Nenhum passageiro possui bilhete ativo para o trecho selecionado.", false);
        }

        limparResultadosPosteriores(simulacao);
        simulacao.setMotivoBloqueio(null);
        boolean somenteUmPassageiro = passageiros.size() == 1 && elegiveis.size() == 1;
        if (somenteUmPassageiro) {
            if ("INF".equals(elegiveis.get(0).getTipo())) {
                return bloquear(simulacao,
                        "Bebe deve ser remarcado com o adulto responsavel. A equipe precisa analisar a reserva.",
                        false);
            }
            List<Integer> indices = List.of(elegiveis.get(0).getIndice());
            salvarSelecaoPassageiros(simulacao, ESCOPO_INDIVIDUAL, indices, passageiros);
            marcarPassageirosSelecionados(passageiros, indices);
            simulacao.setStatus(AGUARDANDO_CRITERIOS);
            simulacao = salvar(simulacao);
            RemarcacaoSimulacaoResponse response = respostaCriterios(simulacao, trecho,
                    "Passageiro confirmado. Informe a nova data e, se desejar, um periodo.");
            response.setPassageiros(passageiros);
            response.setPermiteSelecionarTodos(true);
            return response;
        }

        salvarSelecaoPassageiros(simulacao, null, List.of(), passageiros);
        simulacao.setStatus(AGUARDANDO_PASSAGEIROS);
        simulacao = salvar(simulacao);
        RemarcacaoSimulacaoResponse response = respostaBase(simulacao,
                "Quem deseja remarcar?",
                "Escolha um passageiro ou todos. Passageiros indisponiveis precisam de atendimento humano.");
        response.setPassageiros(passageiros);
        response.setPermiteSelecionarTodos(permiteSelecionarTodos(passageiros));
        registrarEvento(simulacao, "REMARCACAO_AGUARDANDO_PASSAGEIROS",
                "Aguardando escolha dos passageiros para o trecho.", null);
        return response;
    }

    private List<RemarcacaoSimulacaoResponse.Passageiro> montarPassageiros(
            Reserva reserva,
            TrechoReserva trecho) {
        List<RemarcacaoSimulacaoResponse.Passageiro> resultado = new ArrayList<>();
        if (reserva == null || reserva.getPassageiros() == null) {
            return resultado;
        }
        for (int indice = 0; indice < reserva.getPassageiros().size(); indice++) {
            Passageiro origem = reserva.getPassageiros().get(indice);
            RemarcacaoSimulacaoResponse.Passageiro dto = new RemarcacaoSimulacaoResponse.Passageiro();
            dto.setIndice(indice);
            dto.setIdentificador(identificadorPassageiro(origem, indice));
            dto.setNome(nomePassageiro(origem, indice));
            dto.setTipo(tipoPassageiro(origem));
            String impedimento = impedimentoPassageiroTrecho(origem, trecho);
            dto.setElegivel(impedimento == null);
            dto.setMotivoInelegibilidade(impedimento);
            resultado.add(dto);
        }
        return resultado;
    }

    private String impedimentoPassageiroTrecho(Passageiro passageiro, TrechoReserva trecho) {
        if (passageiro == null || passageiro.getBilhetes() == null || passageiro.getBilhetes().isEmpty()) {
            return "Passageiro sem bilhete emitido.";
        }
        boolean possuiBilheteAtivo = false;
        for (Bilhete bilhete : passageiro.getBilhetes()) {
            if (!bilheteAtivo(bilhete)) {
                continue;
            }
            possuiBilheteAtivo = true;
            if (bilheteContemplaTrecho(bilhete, trecho)) {
                return null;
            }
        }
        return possuiBilheteAtivo
                ? "Bilhete nao contempla o trecho selecionado."
                : "Bilhete cancelado ou reembolsado.";
    }

    private boolean bilheteAtivo(Bilhete bilhete) {
        if (bilhete == null || vazio(bilhete.getNumero())) {
            return false;
        }
        String status = normalizar(bilhete.getStatus());
        return !status.contains("cancel")
                && !status.contains("reembols")
                && !status.contains("utilizado")
                && !status.contains("voado")
                && !status.contains("flown");
    }

    private boolean bilheteContemplaTrecho(Bilhete bilhete, TrechoReserva trecho) {
        if (bilhete.getVoos() == null || bilhete.getVoos().isEmpty()) {
            return true;
        }
        if (trecho == null || trecho.getVoos() == null || trecho.getVoos().isEmpty()) {
            return false;
        }
        for (Voo vooBilhete : bilhete.getVoos()) {
            for (Voo vooTrecho : trecho.getVoos()) {
                if (mesmoVoo(vooBilhete, vooTrecho)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean mesmoVoo(Voo primeiro, Voo segundo) {
        if (primeiro == null || segundo == null
                || vazio(primeiro.getNumeroVoo()) || vazio(segundo.getNumeroVoo())) {
            return false;
        }
        boolean mesmoNumero = primeiro.getNumeroVoo().replaceAll("[^0-9A-Za-z]", "")
                .equalsIgnoreCase(segundo.getNumeroVoo().replaceAll("[^0-9A-Za-z]", ""));
        if (!mesmoNumero) return false;
        if (primeiro.getDataPartida() == null || segundo.getDataPartida() == null) return true;
        LocalDate dataPrimeiro = Instant.ofEpochMilli(primeiro.getDataPartida().getTime())
                .atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate dataSegundo = Instant.ofEpochMilli(segundo.getDataPartida().getTime())
                .atZone(ZoneId.systemDefault()).toLocalDate();
        return dataPrimeiro.equals(dataSegundo);
    }

    private String identificadorPassageiro(Passageiro passageiro, int indice) {
        if (passageiro != null && !vazio(passageiro.getIdPassageiro())) {
            return passageiro.getIdPassageiro();
        }
        if (passageiro != null && passageiro.getBilhetes() != null) {
            for (Bilhete bilhete : passageiro.getBilhetes()) {
                if (bilhete != null && !vazio(bilhete.getPaxRef())) {
                    return bilhete.getPaxRef();
                }
            }
        }
        return "PAX-" + indice;
    }

    private String nomePassageiro(Passageiro passageiro, int indice) {
        if (passageiro == null) {
            return "Passageiro " + (indice + 1);
        }
        StringJoiner nome = new StringJoiner(" ");
        if (!vazio(passageiro.getNome())) nome.add(passageiro.getNome().trim());
        if (!vazio(passageiro.getNomeDoMeio())) nome.add(passageiro.getNomeDoMeio().trim());
        if (!vazio(passageiro.getSobrenome())) nome.add(passageiro.getSobrenome().trim());
        String resultado = nome.toString().trim();
        return vazio(resultado) ? "Passageiro " + (indice + 1) : resultado;
    }

    private String tipoPassageiro(Passageiro passageiro) {
        String faixa = normalizar(passageiro == null ? null : passageiro.getFaixaEtaria());
        if (faixa.contains("inf") || faixa.contains("bebe")) return "INF";
        if (faixa.contains("chd") || faixa.contains("crianc")) return "CHD";
        return "ADT";
    }

    private void salvarSelecaoPassageiros(
            SimulacaoRemarcacao simulacao,
            String escopo,
            List<Integer> indices,
            List<RemarcacaoSimulacaoResponse.Passageiro> passageiros) {
        PassageirosPersistidos persistidos = new PassageirosPersistidos();
        persistidos.setEscopo(escopo);
        persistidos.setIndices(indices == null ? new ArrayList<>() : new ArrayList<>(indices));
        persistidos.setPassageiros(passageiros == null ? new ArrayList<>() : new ArrayList<>(passageiros));
        simulacao.setPassageirosJson(json(persistidos));
    }

    private PassageirosPersistidos selecaoPassageiros(SimulacaoRemarcacao simulacao) {
        if (simulacao == null || vazio(simulacao.getPassageirosJson())) {
            return new PassageirosPersistidos();
        }
        try {
            return mapper.readValue(simulacao.getPassageirosJson(), PassageirosPersistidos.class);
        } catch (JsonProcessingException ex) {
            return new PassageirosPersistidos();
        }
    }

    private List<Passageiro> passageirosSelecionados(Reserva reserva, SimulacaoRemarcacao simulacao) {
        PassageirosPersistidos persistidos = selecaoPassageiros(simulacao);
        List<Passageiro> resultado = new ArrayList<>();
        if (reserva == null || reserva.getPassageiros() == null || persistidos.getIndices() == null) {
            return resultado;
        }
        for (Integer indice : persistidos.getIndices()) {
            if (indice != null && indice >= 0 && indice < reserva.getPassageiros().size()) {
                Passageiro passageiro = reserva.getPassageiros().get(indice);
                if (passageiro != null) resultado.add(passageiro);
            }
        }
        return resultado;
    }

    private void marcarPassageirosSelecionados(
            List<RemarcacaoSimulacaoResponse.Passageiro> passageiros,
            List<Integer> indices) {
        Set<Integer> selecionados = indices == null ? Set.of() : new HashSet<>(indices);
        passageiros.forEach(item -> item.setSelecionado(selecionados.contains(item.getIndice())));
    }

    private boolean permiteSelecionarTodos(
            List<RemarcacaoSimulacaoResponse.Passageiro> passageiros) {
        if (passageiros == null || passageiros.isEmpty()
                || passageiros.stream().anyMatch(item -> !item.isElegivel())) {
            return false;
        }
        boolean possuiBebe = passageiros.stream().anyMatch(item -> "INF".equals(item.getTipo()));
        boolean possuiAdulto = passageiros.stream().anyMatch(item -> "ADT".equals(item.getTipo()));
        return !possuiBebe || possuiAdulto;
    }

    private void limparResultadosPosteriores(SimulacaoRemarcacao simulacao) {
        simulacao.setCriteriosJson(null);
        simulacao.setResultadosJson(null);
        simulacao.setOfertaSelecionadaJson(null);
        simulacao.setCalculoJson(null);
        limparPreferenciaPagamento(simulacao);
    }

    private RemarcacaoSimulacaoResponse bloquear(SimulacaoRemarcacao simulacao, String motivo) {
        return bloquear(simulacao, motivo, true);
    }

    private RemarcacaoSimulacaoResponse bloquear(SimulacaoRemarcacao simulacao, String motivo, boolean registrarCard) {
        limparPreferenciaPagamento(simulacao);
        simulacao.setPagamentoStatus(PAGAMENTO_NAO_APLICAVEL);
        simulacao.setStatus(NAO_ELEGIVEL);
        simulacao.setMotivoBloqueio(motivo);
        simulacao = salvar(simulacao);
        registrarEvento(simulacao, "REMARCACAO_NAO_ELEGIVEL", motivo, simulacao.getRegraSnapshotJson());
        RemarcacaoSimulacaoResponse response = respostaBase(simulacao,
                "Simulacao precisa de analise humana", motivo);
        response.setPermiteEncaminhar(chatService.departamentoRemarcacaoDisponivel(simulacao.getConversaId()));
        if (registrarCard) {
            registrarCard(simulacao, response);
        }
        return response;
    }

    private String validarReserva(Reserva reserva) {
        if (reserva == null) return "Nao foi possivel carregar a reserva informada.";
        if (normalizar(reserva.getStatus()).contains("cancel")) return "A reserva esta cancelada.";
        if (reserva.getDataEmissao() == null && !possuiBilhetes(reserva)) return "A reserva ainda nao esta emitida.";
        if (reserva.getPassageiros() == null || reserva.getPassageiros().isEmpty()) {
            return "A reserva nao retornou passageiros.";
        }
        if (!possuiBilhetes(reserva)) return "A reserva nao possui bilhetes emitidos.";
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

    private ReservasEmitidasRemarcacaoResponse.Item resolverReservaSelecionada(
            Integer codgAgencia,
            Integer reservaId,
            String localizador) {
        if (codgAgencia == null) {
            throw regra(409, "A conversa nao possui agencia vinculada.");
        }
        if (vazio(localizador)) {
            throw regra(400, "Informe o localizador exato da reserva selecionada.");
        }
        String localizadorExato = localizador.trim().toUpperCase(Locale.ROOT);

        if (reservaId != null) {
            ReservasEmitidasRemarcacaoResponse response = consultarReservasEmitidasManager(
                    codgAgencia,
                    reservaId,
                    null,
                    null,
                    null,
                    PAGINA_PADRAO_RESERVAS,
                    TAMANHO_PADRAO_RESERVAS);
            List<ReservasEmitidasRemarcacaoResponse.Item> itens = itensReserva(response).stream()
                    .filter(item -> item != null && reservaId.equals(item.getReservaId()))
                    .collect(Collectors.toList());
            if (itens.isEmpty()) {
                throw regra(404, "Reserva emitida nao encontrada para a agencia desta conversa.");
            }
            if (itens.size() != 1) {
                throw regra(409, "A selecao da reserva ficou ambigua. Atualize a lista e selecione novamente.");
            }
            return validarReservaSelecionada(itens.get(0), localizadorExato);
        }

        ReservasEmitidasRemarcacaoResponse response = consultarReservasEmitidasManager(
                codgAgencia,
                null,
                localizadorExato,
                null,
                null,
                PAGINA_PADRAO_RESERVAS,
                TAMANHO_MAXIMO_RESERVAS);
        if (response.getTotalElements() != null
                && response.getTotalElements() > itensReserva(response).size()) {
            throw regra(409,
                    "A busca retornou muitas reservas. Selecione a reserva pelo identificador exibido.");
        }
        List<ReservasEmitidasRemarcacaoResponse.Item> exatas = itensReserva(response).stream()
                .filter(java.util.Objects::nonNull)
                .filter(item -> !vazio(item.getLocalizador()))
                .filter(item -> localizadorExato.equalsIgnoreCase(item.getLocalizador().trim()))
                .collect(Collectors.toList());
        if (exatas.isEmpty()) {
            throw regra(404, "Reserva emitida nao encontrada para a agencia desta conversa.");
        }
        if (exatas.size() != 1) {
            throw regra(409,
                    "Existe mais de uma reserva com este localizador. Selecione uma opcao da lista.");
        }
        return validarReservaSelecionada(exatas.get(0), localizadorExato);
    }

    private ReservasEmitidasRemarcacaoResponse.Item validarReservaSelecionada(
            ReservasEmitidasRemarcacaoResponse.Item item,
            String localizadorExato) {
        if (item == null || item.getReservaId() == null
                || vazio(item.getLocalizador())
                || !localizadorExato.equalsIgnoreCase(item.getLocalizador().trim())) {
            throw regra(409, "A reserva selecionada nao corresponde ao localizador informado.");
        }
        if (!Integer.valueOf(STATUS_RESERVA_EMITIDA).equals(item.getStatus())
                || item.getDataEmissao() == null) {
            throw regra(409, "A reserva selecionada nao esta emitida.");
        }
        if (!companhiaSuportadaRemarcacao(item.getCompanhiaIata())) {
            throw regra(409,
                    "A simulacao automatica esta disponivel somente para as companhias G3, LA, JJ e AD.");
        }
        if (!item.isDisponivelSimulacao()
                || item.getQuantidadeBilhetesAtivos() == null
                || item.getQuantidadeBilhetesAtivos() < 1) {
            throw regra(409, primeiro(
                    item.getMotivoIndisponibilidade(),
                    "A reserva selecionada nao esta disponivel para simulacao."));
        }
        return item;
    }

    private ReservasEmitidasRemarcacaoResponse consultarReservasEmitidasManager(
            Integer codgAgencia,
            Integer reservaId,
            String busca,
            LocalDate dataEmissaoInicio,
            LocalDate dataEmissaoFim,
            int page,
            int size) {
        if (codgAgencia == null) {
            throw regra(409, "A conversa nao possui agencia vinculada.");
        }
        StringBuilder path = new StringBuilder(CAMINHO_RESERVAS_EMITIDAS);
        adicionarParametroConsulta(path, "codgAgencia", codgAgencia);
        adicionarParametroConsulta(path, "reservaId", reservaId);
        adicionarParametroConsulta(path, "busca", normalizarBusca(busca));
        adicionarParametroConsulta(path, "dataEmissaoInicio", dataEmissaoInicio);
        adicionarParametroConsulta(path, "dataEmissaoFim", dataEmissaoFim);
        adicionarParametroConsulta(path, "page", page);
        adicionarParametroConsulta(path, "size", size);

        ReservasEmitidasRemarcacaoResponse response = manager.get(
                path.toString(), ReservasEmitidasRemarcacaoResponse.class);
        if (response == null) {
            response = new ReservasEmitidasRemarcacaoResponse();
            response.setPage(page);
            response.setSize(size);
            response.setTotalElements(0L);
            response.setTotalPages(0);
            response.setHasNext(false);
        } else if (response.getItems() == null) {
            response.setItems(new ArrayList<>());
        }
        return response;
    }

    private List<ReservasEmitidasRemarcacaoResponse.Item> itensReserva(
            ReservasEmitidasRemarcacaoResponse response) {
        return response == null || response.getItems() == null
                ? Collections.emptyList() : response.getItems();
    }

    private void adicionarParametroConsulta(StringBuilder path, String nome, Object valor) {
        if (valor == null) {
            return;
        }
        path.append(path.indexOf("?") < 0 ? '?' : '&')
                .append(nome)
                .append('=')
                .append(URLEncoder.encode(String.valueOf(valor), StandardCharsets.UTF_8));
    }

    private String normalizarBusca(String busca) {
        if (vazio(busca)) {
            return null;
        }
        String normalizada = busca.trim();
        if (normalizada.length() > 120) {
            throw regra(400, "A busca deve possuir no maximo 120 caracteres.");
        }
        return normalizada;
    }

    private void validarPeriodoEmissao(LocalDate inicio, LocalDate fim) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw regra(400, "A data inicial de emissao nao pode ser posterior a data final.");
        }
    }

    private int normalizarPagina(Integer page) {
        int valor = page == null ? PAGINA_PADRAO_RESERVAS : page;
        if (valor < 0) {
            throw regra(400, "A pagina nao pode ser negativa.");
        }
        return valor;
    }

    private int normalizarTamanhoPagina(Integer size) {
        int valor = size == null ? TAMANHO_PADRAO_RESERVAS : size;
        if (valor < 1 || valor > TAMANHO_MAXIMO_RESERVAS) {
            throw regra(400, "O tamanho da pagina deve estar entre 1 e 50.");
        }
        return valor;
    }

    private Reserva carregarReserva(SimulacaoRemarcacao simulacao, SessaoChatResponse sessao) {
        ReservasEmitidasRemarcacaoResponse.Item reservaSelecionada = null;
        if (simulacao.getReservaAereoId() != null) {
            reservaSelecionada = resolverReservaSelecionada(
                    simulacao.getCodgAgencia(),
                    simulacao.getReservaAereoId(),
                    simulacao.getLocalizador());
        }
        return carregarReserva(simulacao, sessao, reservaSelecionada);
    }

    private Reserva carregarReserva(
            SimulacaoRemarcacao simulacao,
            SessaoChatResponse sessao,
            ReservasEmitidasRemarcacaoResponse.Item reservaSelecionada) {
        ConsultarLocalizadorResponse response = aereoClient.carregarReserva(
                montarConsultaLocalizador(simulacao, sessao, reservaSelecionada));
        if (response == null || response.getException() != null || response.getReservas() == null) {
            return null;
        }

        List<Reserva> correspondenciasExatas = response.getReservas().stream()
                .filter(item -> item != null && simulacao.getLocalizador().equalsIgnoreCase(item.getLocalizador()))
                .collect(Collectors.toList());
        if (correspondenciasExatas.isEmpty()) {
            return null;
        }
        if (correspondenciasExatas.size() == 1) {
            Reserva unica = correspondenciasExatas.get(0);
            if (reservaSelecionada == null
                    || correspondeCompanhia(unica, reservaSelecionada.getCompanhiaIata())) {
                return unica;
            }
            throw regra(409,
                    "A reserva carregada nao corresponde a companhia da opcao selecionada.");
        }

        if (reservaSelecionada != null) {
            List<Reserva> desambiguadas = correspondenciasExatas.stream()
                    .filter(item -> correspondeCompanhia(item, reservaSelecionada.getCompanhiaIata()))
                    .collect(Collectors.toList());
            if (desambiguadas.size() == 1) {
                return desambiguadas.get(0);
            }
        }

        throw regra(409,
                "Existe mais de uma reserva com este localizador. Selecione a reserva pelo identificador exibido.");
    }

    private ConsultarLocalizadorRequest montarConsultaLocalizador(SimulacaoRemarcacao simulacao,
                                                                   SessaoChatResponse sessao,
                                                                   ReservasEmitidasRemarcacaoResponse.Item reservaSelecionada) {
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

    private boolean correspondeCompanhia(Reserva reserva, String companhiaEsperada) {
        if (vazio(companhiaEsperada) || reserva == null || reserva.getViagens() == null) {
            return false;
        }
        String companhiaCanonicaEsperada = companhiaCanonica(companhiaEsperada);
        return reserva.getViagens().stream()
                .filter(java.util.Objects::nonNull)
                .map(this::companhiaTrecho)
                .map(this::companhiaCanonica)
                .anyMatch(companhiaCanonicaEsperada::equals);
    }

    private boolean companhiaSuportadaRemarcacao(String companhiaIata) {
        return !vazio(companhiaIata)
                && COMPANHIAS_SUPORTADAS_REMARCACAO.contains(
                companhiaIata.trim().toUpperCase(Locale.ROOT));
    }

    private String companhiaCanonica(String companhiaIata) {
        if (vazio(companhiaIata)) {
            return "";
        }
        String codigo = companhiaIata.trim().toUpperCase(Locale.ROOT);
        return "JJ".equals(codigo) || "LA".equals(codigo) ? "LATAM" : codigo;
    }

    private PesquisaRequestDTO montarPesquisa(SimulacaoRemarcacao simulacao,
                                               TrechoReserva original,
                                               SessaoChatResponse sessao,
                                               RemarcacaoRequest.Pesquisar criterios,
                                               List<Passageiro> passageirosSelecionados) {
        PesquisaRequestDTO request = new PesquisaRequestDTO();
        request.setAgencia(simulacao.getCodgAgencia() == null ? null : String.valueOf(simulacao.getCodgAgencia()));
        request.setUnidade(simulacao.getCodgUnidade() == null ? null : String.valueOf(simulacao.getCodgUnidade()));
        request.setTipoPesquisa(TipoPesquisa.ONEWAY);
        request.setTipoConsulta(TipoConsulta.NACIONAL);
        request.setAeroportoOrigem(new Aeroporto(simulacao.getOrigem(), descricao(original.getOrigem())));
        request.setAeroportoDestino(new Aeroporto(simulacao.getDestino(), descricao(original.getDestino())));
        request.setDataIda(Date.from(criterios.getData().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        Map<String, Integer> passageiros = contagensPassageiros(passageirosSelecionados);
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

    private TarifarRequest montarTarifacao(
            Trecho trecho,
            FamiliaPreco familia,
            List<Passageiro> passageirosSelecionados) {
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
        contagensPassageiros(passageirosSelecionados).forEach((tipo, quantidade) -> {
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
        return montarRequestRegra(
                reserva,
                trecho,
                valorTarifaOriginal(reserva),
                valorTaxasOriginais(reserva),
                novaTarifa,
                novasTaxas,
                BigDecimal.ZERO,
                decimal(reserva.getValorReserva() == null ? null : reserva.getValorReserva().getValor()),
                Math.max(1, reserva.getPassageiros() == null ? 0 : reserva.getPassageiros().size()));
    }

    private RegraAereaAlteracaoConsultaRequest montarRequestRegra(
            Reserva reserva,
            TrechoReserva trecho,
            BigDecimal tarifaOriginal,
            BigDecimal taxasOriginais,
            BigDecimal novaTarifa,
            BigDecimal novasTaxas,
            BigDecimal taxaServico,
            BigDecimal totalOriginal,
            int quantidadePassageiros) {
        Voo voo = primeiroVoo(trecho);
        RegraAereaAlteracaoConsultaRequest request = new RegraAereaAlteracaoConsultaRequest();
        request.setCompanhia(companhiaTrecho(trecho));
        request.setMercado("NACIONAL");
        request.setFamiliaTarifaria(voo == null ? null : primeiro(voo.getFamilia(), voo.getFamiliaCodigo()));
        request.setCodigoTarifario(voo == null ? null : primeiro(voo.getFamiliaCodigo(), voo.getBaseTarifaria()));
        request.setClasseReserva(voo == null ? null : primeiro(voo.getClasse(), primeiraLetra(voo.getBaseTarifaria())));
        request.setTipoEvento("REMARCACAO");
        request.setMomento("ANTES_EMBARQUE");
        request.setValorTarifa(tarifaOriginal);
        request.setValorTaxas(taxasOriginais);
        request.setValorNovaTarifa(novaTarifa);
        request.setValorNovasTaxas(novasTaxas);
        request.setTaxaServico(zero(taxaServico));
        request.setValorTotalReserva(totalOriginal);
        request.setDataEmissaoReserva(reserva == null ? null : reserva.getDataEmissao());
        request.setQuantidadePassageiros(Math.max(1, quantidadePassageiros));
        request.setQuantidadeTrechos(1);
        request.setExigirRegraAprovada(true);
        request.setValidadeMaximaDias(90);
        return request;
    }

    private CalculoPassageiros montarPreviaPassageiros(
            Reserva reserva,
            TrechoReserva trecho,
            Preco preco,
            List<Passageiro> passageirosSelecionados,
            RemarcacaoSimulacaoResponse.OpcaoVoo opcao,
            RemarcacaoSimulacaoResponse.Familia familia,
            SimulacaoRemarcacao simulacao) {
        CalculoPassageiros resultado = new CalculoPassageiros();
        RemarcacaoSimulacaoResponse.Previa previa = new RemarcacaoSimulacaoResponse.Previa();
        previa.setVoo(opcao);
        previa.setFamilia(familia);
        previa.setMoeda(vazio(preco.getMoeda()) ? "BRL" : preco.getMoeda());
        previa.setTarifaOriginal(BigDecimal.ZERO);
        previa.setNovaTarifa(BigDecimal.ZERO);
        previa.setMulta(BigDecimal.ZERO);
        previa.setDiferencaTarifaria(BigDecimal.ZERO);
        previa.setDiferencaTaxas(BigDecimal.ZERO);
        previa.setTaxaServico(BigDecimal.ZERO);
        previa.setTotalEstimado(BigDecimal.ZERO);
        previa.setTotalSelecionado(BigDecimal.ZERO);
        previa.setCalculoCompleto(true);

        boolean houveRateioOriginal = false;
        String resumoRegra = null;
        String familiaOriginal = familiaOriginal(trecho);
        Map<Integer, List<BigDecimal>> taxasEmbarquePorTrecho =
                carregarTaxasEmbarquePorTrecho(reserva);
        Map<String, Integer> quantidadesPorTipo = contagensPassageiros(passageirosSelecionados);
        NormalizacaoPreco normalizacaoPreco = identificarNormalizacaoPreco(preco, passageirosSelecionados);
        for (Passageiro passageiro : passageirosSelecionados) {
            int indice = reserva.getPassageiros().indexOf(passageiro);
            ValoresPassageiro originais = valoresOriginaisPassageiro(reserva, passageiro, indice);
            houveRateioOriginal = houveRateioOriginal || originais.isRateado();
            String tipoPassageiro = tipoPassageiro(passageiro);
            int quantidadeTipo = quantidadesPorTipo.getOrDefault(tipoPassageiro, 1);
            PrecoTipo novoPreco = precoTipo(preco, tipoPassageiro);
            BigDecimal novaTarifa = normalizacaoPreco.porPassageiro(
                    decimal(novoPreco == null ? null : novoPreco.getValorTarifa()),
                    quantidadeTipo, ComponentePreco.TARIFA);
            BigDecimal novaTaxaEmbarqueTrecho = novasTaxasPassageiro(
                    novoPreco, quantidadeTipo, normalizacaoPreco);
            BigDecimal novaTaxaEmbarque = novaTaxaEmbarqueComTrechos(
                    novaTaxaEmbarqueTrecho,
                    originais,
                    taxasEmbarquePorTrecho.get(indice),
                    simulacao.getTrechoIndice(),
                    reserva.getViagens() == null ? 0 : reserva.getViagens().size());
            // O valor enviado ao Manager representa somente o acréscimo de DU/taxas
            // de serviço. A tarifa original já pode conter DU, RC e RAV.
            BigDecimal taxaServicoNova = novaTaxaServicoPassageiro(
                    novoPreco, quantidadeTipo, normalizacaoPreco);
            BigDecimal taxaServicoOriginal = taxaServicoOriginal(originais);
            BigDecimal diferencaTarifariaParaDu = diferencaTarifariaParaDu(
                    originais.getTarifa(), novaTarifa);
            BigDecimal diferencaTaxaServico = calcularDuRemarcacao(
                    diferencaTarifariaParaDu,
                    taxaServicoNova,
                    taxaServicoOriginal);

            RegraAereaAlteracaoConsultaRequest regraRequest = montarRequestRegra(
                    reserva,
                    trecho,
                    originais.getTarifa(),
                    originais.getTaxaEmbarque(),
                    novaTarifa,
                    novaTaxaEmbarque,
                    diferencaTaxaServico,
                    originais.getTotal(),
                    1);
            RegraAereaAlteracaoConsultaResponse regra = regraService.simular(regraRequest);
            resultado.getRegras().add(regra);
            RegraAereaAlteracaoCalculoResponse calculo = regra == null ? null : regra.getCalculo();
            boolean calculoIncompleto = calculo == null
                    || !Boolean.TRUE.equals(calculo.getCalculoCompleto());
            if (!regraPermite(regra) || calculoIncompleto) {
                String nome = nomePassageiro(passageiro, indice < 0 ? 0 : indice);
                String motivo = calculoIncompleto
                        ? "Nao foi possivel obter todos os valores necessarios para calcular " + nome + "."
                        : regra == null || vazio(regra.getMensagem())
                                ? "Nao foi possivel homologar a regra para " + nome + "."
                                : regra.getMensagem();
                resultado.setMotivo(motivo);
                return resultado;
            }
            if (resultado.getRegraId() == null && regra.getRegra() != null) {
                resultado.setRegraId(regra.getRegra().getId());
            }
            resumoRegra = primeiro(resumoRegra, regra.getMensagem(), calculo.getResumo());

            RemarcacaoSimulacaoResponse.PreviaPassageiro item =
                    new RemarcacaoSimulacaoResponse.PreviaPassageiro();
            item.setIndice(indice);
            item.setIdentificador(identificadorPassageiro(passageiro, indice < 0 ? 0 : indice));
            item.setNome(nomePassageiro(passageiro, indice < 0 ? 0 : indice));
            item.setTipo(tipoPassageiro(passageiro));
            item.setFamiliaOriginal(familiaOriginal);
            item.setTarifaOriginal(zero(calculo.getValorTarifaBase()));
            item.setNovaTarifa(zero(calculo.getValorNovaTarifa()));
            item.setTaxaEmbarqueOriginal(zero(calculo.getValorTaxasBase()));
            item.setNovaTaxaEmbarque(zero(calculo.getValorNovasTaxas()));
            item.setTaxaServicoOriginal(taxaServicoOriginal);
            item.setNovaTaxaServico(taxaServicoNova);
            item.setMulta(zero(calculo.getValorMulta()));
            item.setMultaIsentaPorAntecedencia(Boolean.TRUE.equals(calculo.getMultaIsentaPorAntecedencia()));
            item.setLimiteHorasIsencaoMulta(calculo.getLimiteHorasIsencaoMulta());
            item.setDiferencaTarifaria(zero(calculo.getDiferencaTarifaria()));
            item.setDiferencaTaxaEmbarque(diferencaNaoNegativa(
                    calculo.getValorNovasTaxas(), calculo.getValorTaxasBase()));
            item.setTaxaDu(zero(calculo.getTaxaServico()));
            item.setTotalEstimado(zero(calculo.getTotalPrevisto()));
            item.setCalculoCompleto(true);
            previa.getPassageiros().add(item);

            previa.setTarifaOriginal(somar(previa.getTarifaOriginal(), item.getTarifaOriginal()));
            previa.setNovaTarifa(somar(previa.getNovaTarifa(), item.getNovaTarifa()));
            previa.setMulta(somar(previa.getMulta(), item.getMulta()));
            previa.setDiferencaTarifaria(somar(
                    previa.getDiferencaTarifaria(), item.getDiferencaTarifaria()));
            previa.setDiferencaTaxas(somar(
                    previa.getDiferencaTaxas(), item.getDiferencaTaxaEmbarque()));
            previa.setTaxaServico(somar(previa.getTaxaServico(), item.getTaxaDu()));
            previa.setTotalEstimado(somar(previa.getTotalEstimado(), item.getTotalEstimado()));
        }

        previa.setTotalSelecionado(previa.getTotalEstimado());
        previa.setRegraResumo(resumoRegra);
        previa.setAviso(houveRateioOriginal
                ? "Estimativa por passageiro com valores originais rateados entre os trechos. "
                        + "A companhia e o atendente devem validar valores e assentos antes da conclusao."
                : "Esta e uma estimativa por passageiro, nao uma alteracao concluida. "
                        + "Valores e assentos dependem de nova validacao pela companhia.");
        LocalDateTime validadeTarifa = LocalDateTime.now().plusMinutes(15);
        previa.setValidoAte(simulacao.getExpiraEm() != null && simulacao.getExpiraEm().isBefore(validadeTarifa)
                ? simulacao.getExpiraEm() : validadeTarifa);
        resultado.setPrevia(previa);
        resultado.setPermitido(true);
        return resultado;
    }

    private RemarcacaoSimulacaoResponse respostaCriterios(SimulacaoRemarcacao simulacao,
                                                           TrechoReserva trecho,
                                                           String mensagem) {
        RemarcacaoSimulacaoResponse response = respostaBase(simulacao,
                "Quando deseja viajar?", mensagem);
        response.setCriterios(montarCriterios(trecho, dataOriginal(trecho), "QUALQUER", false));
        // Mantem no card os dados reais de cada voo do trecho original, inclusive conexoes.
        response.setTrechos(List.of(montarTrecho(trecho, simulacao.getTrechoIndice(), true)));
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
            resultado.add(montarTrecho(item, indice, indice.equals(selecionado)));
        }
        return resultado;
    }

    private RemarcacaoSimulacaoResponse.Trecho montarTrecho(
            TrechoReserva item,
            Integer indice,
            boolean selecionado) {
        if (item == null || item.getVoos() == null || item.getVoos().isEmpty()) {
            throw regra(409, "O trecho original nao possui dados de voo.");
        }
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
        for (Voo voo : item.getVoos()) {
            dto.getVoos().add(montarVoo(voo, dto.getCompanhia()));
        }
        dto.setSelecionado(selecionado);
        return dto;
    }

    private String companhiaVoo(Voo voo, String fallback) {
        if (voo != null && voo.getCiaMandatoria() != null
                && !vazio(voo.getCiaMandatoria().getCodigoIata())) {
            return voo.getCiaMandatoria().getCodigoIata().toUpperCase(Locale.ROOT);
        }
        if (voo != null && voo.getCiaOperadora() != null
                && !vazio(voo.getCiaOperadora().getCodigoIata())) {
            return voo.getCiaOperadora().getCodigoIata().toUpperCase(Locale.ROOT);
        }
        return fallback;
    }

    private RemarcacaoSimulacaoResponse.Voo montarVoo(Voo voo, String companhiaFallback) {
        RemarcacaoSimulacaoResponse.Voo dto = new RemarcacaoSimulacaoResponse.Voo();
        dto.setCompanhia(companhiaVoo(voo, companhiaFallback));
        dto.setNumero(voo == null ? null : voo.getNumeroVoo());
        dto.setOrigem(voo == null ? null : iata(voo.getOrigem()));
        dto.setDestino(voo == null ? null : iata(voo.getDestino()));
        dto.setDataPartida(voo == null ? null : formatarData(voo.getDataPartida()));
        dto.setHoraPartida(voo == null ? null : voo.getHoraPartida());
        dto.setDataChegada(voo == null ? null : formatarData(voo.getDataChegada()));
        dto.setHoraChegada(voo == null ? null : voo.getHoraChegada());
        dto.setDuracao(voo == null ? null : voo.getDuracao());
        dto.setEquipamento(voo == null ? null : voo.getEquipamento());
        return dto;
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
        for (Voo voo : trecho.getVoos()) {
            dto.getVoos().add(montarVoo(voo, dto.getCompanhia()));
        }
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
        response.setMotivoBloqueio(simulacao.getMotivoBloqueio());
        response.setExpiraEm(simulacao.getExpiraEm());
        return response;
    }

    private RemarcacaoSimulacaoResponse.Previa validarPreviaDisponivel(
            SimulacaoRemarcacao simulacao) {
        if (simulacao == null || !PREVIA_DISPONIVEL.equals(simulacao.getStatus())) {
            throw regra(409, "A previa nao esta disponivel para esta acao.");
        }
        if (vazio(simulacao.getCalculoJson())) {
            throw regra(409, "A previa nao possui valores calculados.");
        }
        RemarcacaoSimulacaoResponse.Previa previa =
                ler(simulacao.getCalculoJson(), RemarcacaoSimulacaoResponse.Previa.class);
        if (previa.getValidoAte() == null || !previa.getValidoAte().isAfter(LocalDateTime.now())) {
            throw regra(409,
                    "A previa de valores expirou. Refaca a pesquisa antes de continuar.");
        }
        return previa;
    }

    private void preencherPagamento(
            RemarcacaoSimulacaoResponse response,
            SimulacaoRemarcacao simulacao,
            RemarcacaoSimulacaoResponse.Previa previa,
            boolean consultarFatura) {
        boolean exige = exigeFormaPagamento(previa);
        response.setExigeFormaPagamento(exige);
        response.setFormaPagamentoSelecionada(formaSelecionada(simulacao));
        if (exige && consultarFatura) {
            response.setFormasPagamento(montarFormasPagamento(simulacao, totalPrevia(previa)));
        }
        response.setPermiteEncaminhar((!exige || possuiPreferenciaPagamento(simulacao))
                && chatService.departamentoRemarcacaoDisponivel(simulacao.getConversaId()));
    }

    private void preencherTrechoOriginal(
            RemarcacaoSimulacaoResponse response,
            SimulacaoRemarcacao simulacao) {
        if (response == null || simulacao == null || vazio(simulacao.getTrechoOriginalJson())) {
            return;
        }
        try {
            TrechoReserva original = ler(simulacao.getTrechoOriginalJson(), TrechoReserva.class);
            response.setTrechos(List.of(
                    montarTrecho(original, simulacao.getTrechoIndice(), true)));
        } catch (Exception ex) {
            LOG.log(Level.WARNING,
                    "Nao foi possivel reconstruir o trecho original no handoff da simulacao "
                            + simulacao.getId() + ". O encaminhamento sera mantido.",
                    ex);
        }
    }

    private List<RemarcacaoSimulacaoResponse.FormaPagamento> montarFormasPagamento(
            SimulacaoRemarcacao simulacao,
            BigDecimal total) {
        List<RemarcacaoSimulacaoResponse.FormaPagamento> formas = new ArrayList<>();
        formas.add(avaliarFatura(simulacao, total));

        RemarcacaoSimulacaoResponse.FormaPagamento cartao =
                novaFormaPagamento(PAGAMENTO_CARTAO, "CARTAO", "Cartao");
        cartao.setDisponivel(true);
        cartao.setStatus(FORMA_DISPONIVEL);
        cartao.setMensagem("Pagamento orientado pelo atendente.");
        formas.add(cartao);
        return formas;
    }

    private RemarcacaoSimulacaoResponse.FormaPagamento avaliarFatura(
            SimulacaoRemarcacao simulacao,
            BigDecimal total) {
        RemarcacaoSimulacaoResponse.FormaPagamento fatura =
                novaFormaPagamento(PAGAMENTO_FATURA, "FATURA", "Faturado");
        String codgErp;
        try {
            SessaoChatResponse sessao = montarSessao(simulacao);
            codgErp = sessao == null || sessao.getAgencia() == null
                    ? null : sessao.getAgencia().getCodgSistemaBackoffice();
        } catch (Exception ex) {
            LOG.log(Level.WARNING,
                    "Nao foi possivel carregar a agencia para validar a fatura da simulacao "
                            + simulacao.getId(), ex);
            return faturaSujeitaValidacao(fatura);
        }
        if (vazio(codgErp)) {
            return faturaSujeitaValidacao(fatura);
        }

        try {
            LimiteCreditoRQ request = new LimiteCreditoRQ(codgErp);
            StatusResponse status = limitesService.checkLimiteApi(request);
            if (status == null || (status.getStatusCode() != 0 && status.getStatusCode() != 200)) {
                return faturaSujeitaValidacao(fatura);
            }

            Disponibilidade disponibilidade = limitesService.consultaLimiteApi(request);
            if (disponibilidade == null
                    || !Boolean.TRUE.equals(disponibilidade.getConsultaConfirmada())
                    || disponibilidade.getLimiteCredito() == null) {
                return faturaSujeitaValidacao(fatura);
            }

            List<LimiteCredito> limitesFaturados = disponibilidade.getLimiteCredito().stream()
                    .filter(java.util.Objects::nonNull)
                    .filter(item -> TipoLimite.FATURADO.equals(item.getTipoLimite()))
                    .collect(Collectors.toList());
            if (limitesFaturados.isEmpty()) {
                fatura.setDisponivel(false);
                fatura.setStatus(FORMA_INDISPONIVEL);
                fatura.setMensagem("Sem limite faturado disponivel.");
                return fatura;
            }

            List<BigDecimal> valores = limitesFaturados.stream()
                    .map(LimiteCredito::getTotalDisponivel)
                    .map(this::valorMonetario)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
            if (valores.isEmpty()) {
                return faturaSujeitaValidacao(fatura);
            }
            BigDecimal disponivel = valores.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            if (disponivel.compareTo(zero(total)) >= 0) {
                fatura.setDisponivel(true);
                fatura.setStatus(FORMA_DISPONIVEL);
                fatura.setMensagem("Limite disponivel para o valor.");
            } else {
                fatura.setDisponivel(false);
                fatura.setStatus(FORMA_INDISPONIVEL);
                fatura.setMensagem("Limite insuficiente para o valor.");
            }
            return fatura;
        } catch (Exception ex) {
            LOG.log(Level.WARNING,
                    "Nao foi possivel confirmar o limite faturado da simulacao " + simulacao.getId(),
                    ex);
            return faturaSujeitaValidacao(fatura);
        }
    }

    private RemarcacaoSimulacaoResponse.FormaPagamento faturaSujeitaValidacao(
            RemarcacaoSimulacaoResponse.FormaPagamento fatura) {
        fatura.setDisponivel(true);
        fatura.setStatus(FORMA_SUJEITA_VALIDACAO);
        fatura.setMensagem("Sujeito a validacao do atendente.");
        return fatura;
    }

    private RemarcacaoSimulacaoResponse.FormaPagamento novaFormaPagamento(
            int codigo,
            String chave,
            String descricao) {
        RemarcacaoSimulacaoResponse.FormaPagamento forma =
                new RemarcacaoSimulacaoResponse.FormaPagamento();
        forma.setCodigo(codigo);
        forma.setChave(chave);
        forma.setDescricao(descricao);
        return forma;
    }

    private RemarcacaoSimulacaoResponse.FormaPagamento formaSelecionada(
            SimulacaoRemarcacao simulacao) {
        if (!possuiPreferenciaPagamento(simulacao)) {
            return null;
        }
        int codigo = simulacao.getFormaPagamentoCodigo();
        RemarcacaoSimulacaoResponse.FormaPagamento forma = novaFormaPagamento(
                codigo,
                codigo == PAGAMENTO_FATURA ? "FATURA" : "CARTAO",
                primeiro(simulacao.getFormaPagamentoDescricao(),
                        codigo == PAGAMENTO_FATURA ? "Faturado" : "Cartao"));
        forma.setDisponivel(true);
        forma.setStatus(simulacao.getPagamentoStatus());
        forma.setMensagem(PAGAMENTO_PREFERENCIA_SUJEITA_VALIDACAO.equals(simulacao.getPagamentoStatus())
                ? "Sujeito a validacao do atendente."
                : "Registrado para confirmacao do atendente.");
        forma.setSelecionadaEm(simulacao.getPagamentoSelecionadoEm());
        return forma;
    }

    private boolean possuiPreferenciaPagamento(SimulacaoRemarcacao simulacao) {
        if (simulacao == null
                || vazio(simulacao.getPagamentoStatus())
                || !simulacao.getPagamentoStatus().startsWith(PAGAMENTO_PREFERENCIA_REGISTRADA)) {
            return false;
        }
        return Integer.valueOf(PAGAMENTO_FATURA).equals(simulacao.getFormaPagamentoCodigo())
                || Integer.valueOf(PAGAMENTO_CARTAO).equals(simulacao.getFormaPagamentoCodigo());
    }

    private void limparPreferenciaPagamento(SimulacaoRemarcacao simulacao) {
        simulacao.setFormaPagamentoCodigo(null);
        simulacao.setFormaPagamentoDescricao(null);
        simulacao.setPagamentoStatus(null);
        simulacao.setPagamentoSelecionadoEm(null);
    }

    private boolean exigeFormaPagamento(RemarcacaoSimulacaoResponse.Previa previa) {
        return previa != null && totalPrevia(previa).compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal totalPrevia(RemarcacaoSimulacaoResponse.Previa previa) {
        if (previa == null) {
            return BigDecimal.ZERO;
        }
        return zero(previa.getTotalSelecionado() == null
                ? previa.getTotalEstimado() : previa.getTotalSelecionado());
    }

    private BigDecimal valorMonetario(String valor) {
        if (vazio(valor)) {
            return null;
        }
        String normalizado = valor.trim()
                .replace("R$", "")
                .replace("\u00A0", "")
                .replace(" ", "");
        if (normalizado.contains(",")) {
            normalizado = normalizado.replace(".", "").replace(",", ".");
        }
        try {
            return new BigDecimal(normalizado).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void adicionarTagRemarcacao(SimulacaoRemarcacao simulacao, Integer codgUsuario) {
        try {
            AdicionarTagConversaRequest request = new AdicionarTagConversaRequest();
            request.setConversaId(simulacao.getConversaId());
            request.setCodgUsuario(codgUsuario);
            request.setNome("Remarca\u00e7\u00e3o a\u00e9rea");
            request.setCorHex("#FF6B00");
            request.setGestor(false);
            chatService.adicionarTagConversa(request);
        } catch (Exception ex) {
            LOG.log(Level.WARNING,
                    "Handoff concluido, mas nao foi possivel adicionar a tag de remarcacao a conversa "
                            + simulacao.getConversaId(), ex);
            try {
                registrarEvento(simulacao, "REMARCACAO_TAG_NAO_APLICADA",
                        "Nao foi possivel adicionar a tag Remarcacao aerea; o encaminhamento foi mantido.",
                        null);
            } catch (Exception eventoEx) {
                LOG.log(Level.WARNING,
                        "Nao foi possivel registrar a falha ao adicionar a tag de remarcacao.",
                        eventoEx);
            }
        }
    }

    private void registrarEventoNaoBloqueante(
            SimulacaoRemarcacao simulacao,
            String tipo,
            String descricao,
            String dadosJson) {
        try {
            registrarEvento(simulacao, tipo, descricao, dadosJson);
        } catch (Exception ex) {
            LOG.log(Level.WARNING,
                    "Handoff concluido, mas nao foi possivel registrar o evento " + tipo
                            + " da simulacao " + simulacao.getId() + ".",
                    ex);
        }
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
        return buscarValidar(id, codgUsuario, true);
    }

    private SimulacaoRemarcacao buscarValidarParaConsulta(Long id, Integer codgUsuario) {
        return buscarValidar(id, codgUsuario, false);
    }

    private SimulacaoRemarcacao buscarValidar(Long id, Integer codgUsuario, boolean mutacao) {
        if (id == null || codgUsuario == null) throw regra(400, "Informe a simulacao e o usuario.");
        SimulacaoRemarcacao simulacao = manager.get(
                "chat-confianca/persistencia/simulacoes-remarcacao/" + id, SimulacaoRemarcacao.class);
        if (simulacao == null) throw regra(404, "Simulacao nao encontrada.");
        if (!codgUsuario.equals(simulacao.getCodgUsuario())) throw regra(403, "Usuario nao pertence a simulacao.");
        Conversa conversa = validarConversaSolicitante(simulacao.getConversaId(), codgUsuario);
        if (mutacao || !STATUS_FINAIS_CONSULTAVEIS.contains(simulacao.getStatus())) {
            validarConversaConfiaAtiva(conversa);
            validarSessaoConversa(conversa, codgUsuario);
            if (!java.util.Objects.equals(conversa.getCodgAgencia(), simulacao.getCodgAgencia())) {
                throw regra(409, "A agencia da simulacao nao corresponde mais a agencia da conversa.");
            }
        }
        if (EXPIRADO.equals(simulacao.getStatus())
                || (simulacao.getExpiraEm() != null && simulacao.getExpiraEm().isBefore(LocalDateTime.now()))) {
            throw regra(409, "A simulacao expirou. Inicie uma nova pesquisa para obter valores atuais.");
        }
        return simulacao;
    }

    private Conversa validarConversaSolicitante(Long conversaId, Integer codgUsuario) {
        if (conversaId == null || codgUsuario == null) {
            throw regra(400, "Informe a conversa e o usuario.");
        }
        Conversa conversa = chatService.buscarConversa(conversaId, codgUsuario, false);
        if (conversa == null) throw regra(404, "Conversa nao encontrada.");
        if (!codgUsuario.equals(conversa.getSolicitanteCodgUsuario())) {
            throw regra(403, "Usuario nao e o solicitante da conversa.");
        }
        return conversa;
    }

    private void validarConversaConfiaAtiva(Conversa conversa) {
        boolean statusAtivo = conversa != null
                && (conversa.getStatus() == StatusConversa.NOVA
                || conversa.getStatus() == StatusConversa.AGUARDANDO_SOLICITANTE);
        if (!statusAtivo || conversa.getAtendenteResponsavelCodgUsuario() != null
                || !metadadosOrigemConfia(conversa.getMetadadosJson())) {
            throw regra(409,
                    "A simulacao de remarcacao somente pode continuar em uma conversa ativa com a ConfIA.");
        }
    }

    private boolean metadadosOrigemConfia(String metadadosJson) {
        if (vazio(metadadosJson)) {
            return false;
        }
        try {
            JsonNode metadados = mapper.readTree(metadadosJson);
            JsonNode origem = metadados == null ? null : metadados.get("origem");
            return origem != null && origem.isTextual()
                    && "CONFIA".equalsIgnoreCase(origem.asText().trim());
        } catch (JsonProcessingException exception) {
            return false;
        }
    }

    private SessaoChatResponse validarSessaoConversa(Conversa conversa, Integer codgUsuario) {
        if (conversa == null || conversa.getCodgAgencia() == null) {
            throw regra(409, "A conversa nao possui agencia vinculada.");
        }
        SessaoChatResponse sessao = chatService.montarSessao(codgUsuario, conversa.getCodgAgencia());
        Integer agenciaSessao = sessao == null || sessao.getAgencia() == null
                ? null : sessao.getAgencia().getCodgAgencia();
        if (!conversa.getCodgAgencia().equals(agenciaSessao)) {
            throw regra(403, "Usuario nao pertence a agencia da conversa.");
        }
        return sessao;
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

    private Map<String, Integer> contagensPassageiros(List<Passageiro> passageiros) {
        Map<String, Integer> resultado = new LinkedHashMap<>();
        resultado.put("ADT", 0);
        resultado.put("CHD", 0);
        resultado.put("INF", 0);
        if (passageiros != null) {
            for (Passageiro passageiro : passageiros) {
                String tipo = tipoPassageiro(passageiro);
                resultado.put(tipo, resultado.getOrDefault(tipo, 0) + 1);
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
        if (!ratearValoresOriginaisPorTrecho) return valor;
        int trechos = reserva.getViagens() == null || reserva.getViagens().isEmpty() ? 1 : reserva.getViagens().size();
        return valor.divide(BigDecimal.valueOf(trechos), 2, RoundingMode.HALF_UP);
    }

    private boolean possuiTarifaParaTodos(Preco preco, List<Passageiro> passageiros) {
        if (preco == null || passageiros == null || passageiros.isEmpty()) return false;
        for (Passageiro passageiro : passageiros) {
            PrecoTipo tipo = precoTipo(preco, tipoPassageiro(passageiro));
            if (tipo == null || tipo.getValorTarifa() == null) return false;
        }
        return true;
    }

    private PrecoTipo precoTipo(Preco preco, String tipo) {
        if (preco == null) return null;
        if ("INF".equals(tipo)) return preco.getPrecoBebe();
        if ("CHD".equals(tipo)) return preco.getPrecoCrianca();
        return preco.getPrecoAdulto();
    }

    /**
     * A tarifacao normalmente retorna os valores unitarios de cada tipo de
     * passageiro. Alguns fornecedores, porem, retornam o total do tipo (por
     * exemplo, R$ 400 para dois ADT de R$ 200). Identificamos esse caso pelos
     * totais informados na propria resposta antes de montar a previa.
     */
    private NormalizacaoPreco identificarNormalizacaoPreco(
            Preco preco,
            List<Passageiro> passageiros) {
        return new NormalizacaoPreco(
                componenteAgrupado(preco, passageiros, ComponentePreco.TARIFA),
                componenteAgrupado(preco, passageiros, ComponentePreco.TAXA_EMBARQUE),
                componenteAgrupado(preco, passageiros, ComponentePreco.TAXA_COMBUSTIVEL),
                componenteAgrupado(preco, passageiros, ComponentePreco.TAXA_ASSENTO),
                componenteAgrupado(preco, passageiros, ComponentePreco.TAXA_BAGAGEM),
                componenteAgrupado(preco, passageiros, ComponentePreco.TAXA_MENOR),
                componenteAgrupado(preco, passageiros, ComponentePreco.TAXA_SERVICO));
    }

    private boolean componenteAgrupado(
            Preco preco,
            List<Passageiro> passageiros,
            ComponentePreco componente) {
        BigDecimal totalInformado = totalComponente(preco, componente);
        if (totalInformado == null || totalInformado.signum() <= 0) {
            return false;
        }

        Map<String, Integer> quantidades = contagensPassageiros(passageiros);
        BigDecimal totalPorTipo = BigDecimal.ZERO;
        BigDecimal totalPorPassageiro = BigDecimal.ZERO;
        boolean encontrouValor = false;
        for (Map.Entry<String, Integer> entrada : quantidades.entrySet()) {
            int quantidade = entrada.getValue() == null ? 0 : entrada.getValue();
            if (quantidade <= 0) continue;
            PrecoTipo precoTipo = precoTipo(preco, entrada.getKey());
            BigDecimal valor = valorComponente(precoTipo, componente);
            if (valor == null) continue;
            encontrouValor = encontrouValor || valor.signum() > 0;
            totalPorTipo = totalPorTipo.add(valor);
            totalPorPassageiro = totalPorPassageiro.add(
                    valor.multiply(BigDecimal.valueOf(quantidade)));
        }
        if (!encontrouValor) return false;

        BigDecimal diferencaAgrupada = totalInformado.subtract(totalPorTipo).abs();
        BigDecimal diferencaUnitarios = totalInformado.subtract(totalPorPassageiro).abs();
        // So divide quando o total informado confirma o comportamento
        // agrupado. Se o fornecedor nao informar o total, preservamos o
        // contrato padrao (valor unitario) para evitar uma divisao indevida.
        return diferencaAgrupada.compareTo(diferencaUnitarios) < 0
                && toleranciaTotal(totalInformado, totalPorTipo);
    }

    private BigDecimal totalComponente(Preco preco, ComponentePreco componente) {
        if (preco == null) return null;
        switch (componente) {
            case TARIFA:
                return primeiroDecimal(preco.getTotalTarifa(), preco.getTarifa());
            case TAXA_EMBARQUE:
                return decimal(preco.getTotalTaxaEmbarque());
            case TAXA_COMBUSTIVEL:
                return decimal(preco.getTotalTaxaDeCombustivel());
            case TAXA_ASSENTO:
                return decimal(preco.getTotalTaxaAssento());
            case TAXA_BAGAGEM:
                return decimal(preco.getTotalTaxaBagagem());
            case TAXA_MENOR:
                return decimal(preco.getTotalTaxaMenorDesacompanhado());
            case TAXA_SERVICO:
                return decimal(preco.getTotalTaxaServico());
            default:
                return null;
        }
    }

    private BigDecimal valorComponente(PrecoTipo preco, ComponentePreco componente) {
        if (preco == null) return null;
        switch (componente) {
            case TARIFA:
                return decimal(preco.getValorTarifa());
            case TAXA_EMBARQUE:
                return decimal(preco.getValorTaxaEmbarque());
            case TAXA_COMBUSTIVEL:
                return decimal(preco.getValorTaxaCombustivel());
            case TAXA_ASSENTO:
                return decimal(preco.getValorTaxaAssento());
            case TAXA_BAGAGEM:
                return decimal(preco.getValorTaxaBagagem());
            case TAXA_MENOR:
                return decimal(preco.getValorTaxaMenorDesacompanhado());
            case TAXA_SERVICO:
                return novaTaxaServicoPassageiro(preco);
            default:
                return null;
        }
    }

    private boolean toleranciaTotal(BigDecimal informado, BigDecimal calculado) {
        BigDecimal tolerancia = new BigDecimal("0.05");
        BigDecimal percentual = informado.abs().multiply(new BigDecimal("0.005"));
        if (percentual.compareTo(tolerancia) > 0) tolerancia = percentual;
        return informado.subtract(calculado).abs().compareTo(tolerancia) <= 0;
    }

    private BigDecimal novasTaxasPassageiro(PrecoTipo preco) {
        if (preco == null) return null;
        return zero(decimal(preco.getValorTaxaEmbarque()))
                .add(zero(decimal(preco.getValorTaxaCombustivel())))
                .add(zero(decimal(preco.getValorTaxaAssento())))
                .add(zero(decimal(preco.getValorTaxaBagagem())))
                .add(zero(decimal(preco.getValorTaxaMenorDesacompanhado())));
    }

    private BigDecimal novasTaxasPassageiro(
            PrecoTipo preco,
            int quantidade,
            NormalizacaoPreco normalizacao) {
        if (preco == null) return null;
        return zero(normalizacao.porPassageiro(
                        decimal(preco.getValorTaxaEmbarque()), quantidade,
                        ComponentePreco.TAXA_EMBARQUE))
                .add(zero(normalizacao.porPassageiro(
                        decimal(preco.getValorTaxaCombustivel()), quantidade,
                        ComponentePreco.TAXA_COMBUSTIVEL)))
                .add(zero(normalizacao.porPassageiro(
                        decimal(preco.getValorTaxaAssento()), quantidade,
                        ComponentePreco.TAXA_ASSENTO)))
                .add(zero(normalizacao.porPassageiro(
                        decimal(preco.getValorTaxaBagagem()), quantidade,
                        ComponentePreco.TAXA_BAGAGEM)))
                .add(zero(normalizacao.porPassageiro(
                        decimal(preco.getValorTaxaMenorDesacompanhado()), quantidade,
                        ComponentePreco.TAXA_MENOR)));
    }

    private BigDecimal novaTaxaEmbarqueComTrechos(
            BigDecimal novaTaxaTrecho,
            ValoresPassageiro originais,
            List<BigDecimal> taxasPorTrecho,
            Integer trechoSelecionado,
            int quantidadeTrechos) {
        BigDecimal nova = zero(novaTaxaTrecho);
        if (quantidadeTrechos <= 1 || originais == null) {
            return nova;
        }
        if (ratearValoresOriginaisPorTrecho) {
            // Modo de teste: preserva o comportamento legado, sem recompor
            // os trechos a partir do valor integral da reserva.
            return nova;
        }

        BigDecimal originalTotal = zero(originais.getTaxaEmbarqueIntegral());
        if (originalTotal.signum() == 0) {
            originalTotal = zero(originais.getTaxaEmbarque());
        }

        BigDecimal originalTrecho = null;
        if (taxasPorTrecho != null
                && trechoSelecionado != null
                && trechoSelecionado >= 0
                && trechoSelecionado < taxasPorTrecho.size()) {
            originalTrecho = zero(taxasPorTrecho.get(trechoSelecionado));
        }

        // Reservas antigas podem nao possuir os valores separados por trecho.
        // Nesse caso, a melhor aproximacao disponivel e distribuir apenas a taxa
        // original para identificar o valor que permanece no trecho nao alterado.
        if (originalTrecho == null) {
            originalTrecho = originalTotal.divide(
                    BigDecimal.valueOf(quantidadeTrechos), 2, RoundingMode.HALF_UP);
        }

        BigDecimal originalNaoAlterado = originalTotal.subtract(originalTrecho);
        if (originalNaoAlterado.signum() < 0) {
            originalNaoAlterado = BigDecimal.ZERO;
        }
        return nova.add(originalNaoAlterado).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<Integer, List<BigDecimal>> carregarTaxasEmbarquePorTrecho(Reserva reserva) {
        Map<Integer, List<BigDecimal>> resultado = new HashMap<>();
        if (reserva == null
                || vazio(reserva.getLocalizador())
                || reserva.getViagens() == null
                || reserva.getViagens().size() <= 1
                || reserva.getPassageiros() == null
                || reservaAereoApi == null) {
            return resultado;
        }

        try {
            ReservaAereo reservaDb = reservaAereoApi
                    .reservaAereoConsultaLocalizadorDb(reserva.getLocalizador());
            if (reservaDb == null || reservaDb.getPassageiros() == null) {
                return resultado;
            }

            int quantidadeTrechos = reserva.getViagens().size();
            for (int indice = 0; indice < reserva.getPassageiros().size(); indice++) {
                Passageiro passageiro = reserva.getPassageiros().get(indice);
                com.confApi.db.confManager.passageiro.Passageiro passageiroDb =
                        localizarPassageiroDb(reservaDb, passageiro, indice);
                if (passageiroDb == null
                        || passageiroDb.getReservaValores() == null
                        || passageiroDb.getReservaValores().size() != quantidadeTrechos) {
                    continue;
                }

                List<BigDecimal> taxas = new ArrayList<>();
                for (ReservaValor valor : passageiroDb.getReservaValores()) {
                    taxas.add(decimal(valor == null ? null : valor.getValorTaxaEmbarque()));
                }
                resultado.put(indice, taxas);
            }
        } catch (Exception ex) {
            LOG.log(Level.FINE,
                    "Nao foi possivel carregar taxas de embarque por trecho para a remarcacao.", ex);
        }
        return resultado;
    }

    private com.confApi.db.confManager.passageiro.Passageiro localizarPassageiroDb(
            ReservaAereo reservaDb,
            Passageiro passageiro,
            int indice) {
        if (reservaDb == null || reservaDb.getPassageiros() == null) {
            return null;
        }

        String cpf = somenteDigitos(passageiro == null ? null : passageiro.getCpf());
        if (!vazio(cpf)) {
            com.confApi.db.confManager.passageiro.Passageiro porCpf = reservaDb.getPassageiros().stream()
                    .filter(java.util.Objects::nonNull)
                    .filter(item -> cpf.equals(somenteDigitos(item.getCpf())))
                    .findFirst()
                    .orElse(null);
            if (porCpf != null) {
                return porCpf;
            }
        }

        String nome = normalizar(nomePassageiro(passageiro, Math.max(0, indice)));
        return reservaDb.getPassageiros().stream()
                .filter(java.util.Objects::nonNull)
                .filter(item -> nome.equals(normalizar(
                        (item.getNomePassageiro() == null ? "" : item.getNomePassageiro())
                                + " "
                                + (item.getSobrenomePassageiro() == null ? "" : item.getSobrenomePassageiro()))))
                .findFirst()
                .orElse(null);
    }

    private BigDecimal novaTaxaServicoPassageiro(PrecoTipo preco) {
        if (preco == null) return BigDecimal.ZERO;
        return zero(decimal(preco.getValorTaxaServico()))
                .add(zero(decimal(preco.getValorFee())))
                .add(zero(decimal(preco.getValorRav())));
    }

    private BigDecimal novaTaxaServicoPassageiro(
            PrecoTipo preco,
            int quantidade,
            NormalizacaoPreco normalizacao) {
        return normalizacao == null
                ? novaTaxaServicoPassageiro(preco)
                : normalizacao.porPassageiro(
                        novaTaxaServicoPassageiro(preco), quantidade,
                        ComponentePreco.TAXA_SERVICO);
    }

    private BigDecimal diferencaTarifariaParaDu(BigDecimal tarifaOriginal, BigDecimal novaTarifa) {
        if (tarifaOriginal == null || novaTarifa == null) {
            return null;
        }
        BigDecimal diferenca = novaTarifa.subtract(tarifaOriginal);
        return diferenca.signum() < 0 ? BigDecimal.ZERO : diferenca.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularDuRemarcacao(BigDecimal diferencaTarifaria,
                                            BigDecimal taxaServicoNova,
                                            BigDecimal taxaServicoOriginal) {
        if (diferencaTarifaria == null) {
            return diferencaNaoNegativa(taxaServicoNova, taxaServicoOriginal);
        }
        if (diferencaTarifaria.compareTo(LIMITE_DU_DIFERENCA_TARIFA) <= 0) {
            return DU_MINIMA_REMARCACAO;
        }
        return diferencaTarifaria.multiply(PERCENTUAL_DU_ACIMA_LIMITE)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal taxaServicoOriginal(ValoresPassageiro valores) {
        if (valores == null) return BigDecimal.ZERO;
        return zero(valores.getTaxaDu())
                .add(zero(valores.getTaxaRc()))
                .add(zero(valores.getRav()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal diferencaNaoNegativa(BigDecimal novo, BigDecimal original) {
        BigDecimal diferenca = zero(novo).subtract(zero(original));
        return diferenca.signum() > 0
                ? diferenca.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }

    private ValoresPassageiro valoresOriginaisPassageiro(
            Reserva reserva,
            Passageiro passageiro,
            int indice) {
        ValoresPassageiro resultado = new ValoresPassageiro();
        ValorBase base = reserva.getValorReserva() == null
                ? null : reserva.getValorReserva().getValorBase();
        ValorPassageiro valorPassageiro = localizarValorPassageiro(base, passageiro, indice);
        if (valorPassageiro != null) {
            resultado.setTaxaEmbarqueIntegral(
                    zero(decimal(valorPassageiro.getTaxaEmbarque())));
            resultado.setTarifa(ratear(decimal(valorPassageiro.getTarifa()), reserva));
            resultado.setTaxaEmbarque(ratear(
                    zero(decimal(valorPassageiro.getTaxaEmbarque())), reserva));
            resultado.setTaxaDu(ratear(decimal(valorPassageiro.getTaxaDU()), reserva));
            resultado.setTaxaRc(ratear(decimal(valorPassageiro.getRC()), reserva));
            resultado.setRav(ratear(decimal(valorPassageiro.getRAV()), reserva));
            resultado.setTotal(ratear(decimal(valorPassageiro.getTotal()), reserva));
            if (resultado.getTotal() == null && resultado.getTarifa() != null) {
                resultado.setTotal(somar(resultado.getTarifa(), resultado.getTaxaEmbarque()));
            }
            return resultado;
        }

        int quantidadePassageiros = Math.max(
                1, reserva.getPassageiros() == null ? 0 : reserva.getPassageiros().size());
        resultado.setTaxaEmbarqueIntegral(
                zero(decimal(base == null ? null : base.getTaxaEmbarque()))
                        .divide(BigDecimal.valueOf(quantidadePassageiros), 2, RoundingMode.HALF_UP));
        resultado.setTarifa(ratearPorPassageiro(
                decimal(base == null ? null : base.getTarifa()), reserva, quantidadePassageiros));
        resultado.setTaxaEmbarque(ratearPorPassageiro(
                decimal(base == null ? null : base.getTaxaEmbarque()), reserva, quantidadePassageiros));
        resultado.setTaxaDu(ratearPorPassageiro(
                decimal(base == null ? null : base.getTaxaDU()), reserva, quantidadePassageiros));
        resultado.setTaxaRc(ratearPorPassageiro(
                decimal(base == null ? null : base.getRC()), reserva, quantidadePassageiros));
        resultado.setRav(ratearPorPassageiro(
                decimal(base == null ? null : base.getRAV()), reserva, quantidadePassageiros));
        resultado.setTotal(ratearPorPassageiro(
                decimal(base == null ? null : base.getTotal()), reserva, quantidadePassageiros));
        resultado.setRateado(true);
        return resultado;
    }

    private ValorPassageiro localizarValorPassageiro(
            ValorBase base,
            Passageiro passageiro,
            int indice) {
        if (base == null || base.getValorPassageiroList() == null
                || base.getValorPassageiroList().isEmpty()) {
            return null;
        }
        String cpf = somenteDigitos(passageiro == null ? null : passageiro.getCpf());
        if (!vazio(cpf)) {
            ValorPassageiro porCpf = base.getValorPassageiroList().stream()
                    .filter(java.util.Objects::nonNull)
                    .filter(item -> cpf.equals(somenteDigitos(item.getCpf())))
                    .findFirst()
                    .orElse(null);
            if (porCpf != null) return porCpf;
        }

        String nome = normalizar(nomePassageiro(passageiro, Math.max(0, indice)));
        ValorPassageiro porNome = base.getValorPassageiroList().stream()
                .filter(java.util.Objects::nonNull)
                .filter(item -> !vazio(item.getNomePassageiro())
                        && nome.equals(normalizar(item.getNomePassageiro())))
                .findFirst()
                .orElse(null);
        if (porNome != null) return porNome;
        return indice >= 0 && indice < base.getValorPassageiroList().size()
                ? base.getValorPassageiroList().get(indice) : null;
    }

    private BigDecimal ratearPorPassageiro(BigDecimal valor, Reserva reserva, int passageiros) {
        if (valor == null) return null;
        return ratear(valor, reserva)
                .divide(BigDecimal.valueOf(Math.max(1, passageiros)), 2, RoundingMode.HALF_UP);
    }

    private String familiaOriginal(TrechoReserva trecho) {
        Voo voo = primeiroVoo(trecho);
        if (voo == null) return null;
        return primeiro(voo.getFamilia(), voo.getFamiliaCodigo(), voo.getBaseTarifaria());
    }

    private BigDecimal somar(BigDecimal primeiro, BigDecimal segundo) {
        return zero(primeiro).add(zero(segundo)).setScale(2, RoundingMode.HALF_UP);
    }

    private String somenteDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("[^0-9]", "");
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
        if (AGUARDANDO_PASSAGEIROS.equals(status)) return "Quem deseja remarcar?";
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

    private enum ComponentePreco {
        TARIFA,
        TAXA_EMBARQUE,
        TAXA_COMBUSTIVEL,
        TAXA_ASSENTO,
        TAXA_BAGAGEM,
        TAXA_MENOR,
        TAXA_SERVICO
    }

    private static class NormalizacaoPreco {
        private final boolean tarifaAgrupada;
        private final boolean taxaEmbarqueAgrupada;
        private final boolean taxaCombustivelAgrupada;
        private final boolean taxaAssentoAgrupada;
        private final boolean taxaBagagemAgrupada;
        private final boolean taxaMenorAgrupada;
        private final boolean taxaServicoAgrupada;

        private NormalizacaoPreco(
                boolean tarifaAgrupada,
                boolean taxaEmbarqueAgrupada,
                boolean taxaCombustivelAgrupada,
                boolean taxaAssentoAgrupada,
                boolean taxaBagagemAgrupada,
                boolean taxaMenorAgrupada,
                boolean taxaServicoAgrupada) {
            this.tarifaAgrupada = tarifaAgrupada;
            this.taxaEmbarqueAgrupada = taxaEmbarqueAgrupada;
            this.taxaCombustivelAgrupada = taxaCombustivelAgrupada;
            this.taxaAssentoAgrupada = taxaAssentoAgrupada;
            this.taxaBagagemAgrupada = taxaBagagemAgrupada;
            this.taxaMenorAgrupada = taxaMenorAgrupada;
            this.taxaServicoAgrupada = taxaServicoAgrupada;
        }

        private BigDecimal porPassageiro(
                BigDecimal valor,
                int quantidade,
                ComponentePreco componente) {
            if (valor == null || quantidade <= 1 || !agrupado(componente)) {
                return valor;
            }
            return valor.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);
        }

        private boolean agrupado(ComponentePreco componente) {
            switch (componente) {
                case TARIFA:
                    return tarifaAgrupada;
                case TAXA_EMBARQUE:
                    return taxaEmbarqueAgrupada;
                case TAXA_COMBUSTIVEL:
                    return taxaCombustivelAgrupada;
                case TAXA_ASSENTO:
                    return taxaAssentoAgrupada;
                case TAXA_BAGAGEM:
                    return taxaBagagemAgrupada;
                case TAXA_MENOR:
                    return taxaMenorAgrupada;
                case TAXA_SERVICO:
                    return taxaServicoAgrupada;
                default:
                    return false;
            }
        }
    }

    @Data
    private static class SelecaoPersistida {
        private RemarcacaoSimulacaoResponse.OpcaoVoo opcao;
        private RemarcacaoSimulacaoResponse.Familia familia;
    }

    @Data
    private static class PassageirosPersistidos {
        private String escopo;
        private List<Integer> indices = new ArrayList<>();
        private List<RemarcacaoSimulacaoResponse.Passageiro> passageiros = new ArrayList<>();
    }

    @Data
    private static class ValoresPassageiro {
        private BigDecimal tarifa;
        private BigDecimal taxaEmbarque;
        private BigDecimal taxaEmbarqueIntegral;
        private BigDecimal taxaDu;
        private BigDecimal taxaRc;
        private BigDecimal rav;
        private BigDecimal total;
        private boolean rateado;
    }

    @Data
    private static class CalculoPassageiros {
        private boolean permitido;
        private String motivo;
        private Long regraId;
        private RemarcacaoSimulacaoResponse.Previa previa;
        private List<RegraAereaAlteracaoConsultaResponse> regras = new ArrayList<>();
    }
}
