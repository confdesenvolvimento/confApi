package com.confApi.chatconfianca.service;

import com.confApi.chatconfianca.client.ChatConfiancaManagerClient;
import com.confApi.chatconfianca.dto.enums.DistribuicaoDepartamento;
import com.confApi.chatconfianca.dto.enums.OrigemConversa;
import com.confApi.chatconfianca.dto.enums.PapelAtendente;
import com.confApi.chatconfianca.dto.enums.PapelParticipante;
import com.confApi.chatconfianca.dto.enums.PrioridadeConversa;
import com.confApi.chatconfianca.dto.enums.RemetenteTipo;
import com.confApi.chatconfianca.dto.enums.StatusConversa;
import com.confApi.chatconfianca.dto.enums.StatusFila;
import com.confApi.chatconfianca.dto.enums.StatusAtendente;
import com.confApi.chatconfianca.dto.enums.StatusMensagem;
import com.confApi.chatconfianca.dto.request.TransferirConversaRequest;
import com.confApi.chatconfianca.dto.request.AdicionarTagConversaRequest;
import com.confApi.chatconfianca.dto.model.Tag;
import com.confApi.chatconfianca.dto.model.SlaPolitica;
import com.confApi.chatconfianca.dto.model.SlaConversaResumo;
import com.confApi.chatconfianca.dto.model.DashboardGrupoResumo;
import com.confApi.chatconfianca.dto.model.DashboardAtendimentoResumo;
import com.confApi.chatconfianca.dto.model.ConversaTransferencia;
import com.confApi.chatconfianca.dto.model.ConversaTag;
import com.confApi.chatconfianca.dto.enums.StatusTransferencia;
import com.confApi.chatconfianca.dto.enums.TipoMensagem;
import com.confApi.chatconfianca.dto.enums.VisibilidadeMensagem;
import com.confApi.chatconfianca.dto.model.AtendimentoAvaliacao;
import com.confApi.chatconfianca.dto.model.AtendenteStatus;
import com.confApi.chatconfianca.dto.model.Conversa;
import com.confApi.chatconfianca.dto.model.ConversaEvento;
import com.confApi.chatconfianca.dto.model.ConversaParticipante;
import com.confApi.chatconfianca.dto.model.Departamento;
import com.confApi.chatconfianca.dto.model.DepartamentoAtendente;
import com.confApi.chatconfianca.dto.model.DepartamentoUnidade;
import com.confApi.chatconfianca.dto.model.FilaAtendimento;
import com.confApi.chatconfianca.dto.model.Mensagem;
import com.confApi.chatconfianca.dto.model.MensagemAnexo;
import com.confApi.chatconfianca.dto.model.MensagemLeitura;
import com.confApi.chatconfianca.dto.model.MensagensNaoLidasResumo;
import com.confApi.chatconfianca.dto.model.RefAgencia;
import com.confApi.chatconfianca.dto.model.RefUnidade;
import com.confApi.chatconfianca.dto.model.RefUsuario;
import com.confApi.chatconfianca.dto.model.RespostaRapida;
import com.confApi.chatconfianca.dto.model.VwConversaResumo;
import com.confApi.chatconfianca.dto.model.VwFilaAtendimento;
import com.confApi.chatconfianca.dto.request.AbrirConversaRequest;
import com.confApi.chatconfianca.dto.request.AssumirAtendimentoRequest;
import com.confApi.chatconfianca.dto.request.AvaliarAtendimentoRequest;
import com.confApi.chatconfianca.dto.request.EncerrarConversaRequest;
import com.confApi.chatconfianca.dto.request.EnviarAnexoRequest;
import com.confApi.chatconfianca.dto.request.EnviarMensagemRequest;
import com.confApi.chatconfianca.dto.request.RegistrarLeituraRequest;
import com.confApi.chatconfianca.dto.response.AnexoDownloadResponse;
import com.confApi.chatconfianca.dto.response.DepartamentoAtendimentoOpcao;
import com.confApi.chatconfianca.dto.response.ChatNotificacaoResumoResponse;
import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.exception.RegraDeNegocioException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class ChatConfiancaService {
    private static final Logger LOGGER = Logger.getLogger(ChatConfiancaService.class.getName());
    private static final DateTimeFormatter PROTOCOLO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final long MAX_ANEXO_BYTES = 10L * 1024L * 1024L;
    private static final int MINUTOS_AUTO_ENCERRAMENTO_CONFIA_PADRAO = 120;
    private static final String CODIGO_DEPARTAMENTO_CONFIA_GERAL = "CONFIA_GERAL";
    private static final String NOME_DEPARTAMENTO_CONFIA_GERAL = "ConfIA Geral";

    private final ChatConfiancaManagerClient manager;
    private final ChatConfiancaConfigService configService;

    public ChatConfiancaService(ChatConfiancaManagerClient manager, ChatConfiancaConfigService configService) {
        this.manager = manager;
        this.configService = configService;
    }

    public SessaoChatResponse montarSessao(Integer codgUsuario) {
        return montarSessao(codgUsuario, null);
    }

    public SessaoChatResponse montarSessao(Integer codgUsuario, Integer codgAgenciaSessao) {
        validarObrigatorio(codgUsuario, "Informe o usuario.");
        configService.sincronizarUsuarioReferencia(codgUsuario);

        RefUsuario usuario = buscarUsuarioOuFalhar(codgUsuario);
        if (!Boolean.TRUE.equals(usuario.getAtivoChat())) {
            throw regra(403, "Usuario inativo para o chat.");
        }

        RefAgencia agencia = null;
        if (usuario.getCodgAgencia() != null) {
            agencia = buscarOuSincronizarAgencia(usuario.getCodgAgencia());
            validarAgenciaAtiva(agencia, usuario.getCodgAgencia(), 403);
        } else if (codgAgenciaSessao != null) {
            agencia = buscarOuSincronizarAgencia(codgAgenciaSessao);
            validarAgenciaAtiva(agencia, codgAgenciaSessao, 403);
        }

        Integer codgUnidade = agencia != null ? agencia.getCodgUnidade() : usuario.getCodgUnidade();
        RefUnidade unidade = null;
        if (codgUnidade != null) {
            unidade = buscarUnidadeOuFalhar(codgUnidade);
            validarUnidadeAtiva(unidade, codgUnidade, 403);
        }

        List<String> perfis = new ArrayList<>(listarPerfis(codgUsuario, codgUnidade));
        List<DepartamentoAtendente> vinculosAtendente = listarDepartamentosAtendenteSeguro(codgUsuario);
        adicionarPerfisPorVinculo(perfis, vinculosAtendente, codgUnidade);

        SessaoChatResponse response = new SessaoChatResponse();
        response.setUsuario(usuario);
        response.setAgencia(agencia);
        response.setUnidade(unidade);
        response.setPerfis(perfis);
        response.setAtendente(temPerfil(perfis, "ATENDENTE", "SUPERVISOR", "GESTOR", "GESTOR_UNIDADE", "ADMIN_CHAT"));
        response.setGestor(temPerfil(perfis, "GESTOR", "GESTOR_UNIDADE", "SUPERVISOR"));
        response.setAdmin(temPerfil(perfis, "ADMIN", "ADMIN_CHAT"));
        return response;
    }

    public List<DepartamentoUnidade> listarDepartamentosDisponiveis(Integer codgAgencia) {
        validarObrigatorio(codgAgencia, "Informe a agencia.");
        RefAgencia agencia = buscarOuSincronizarAgencia(codgAgencia);
        validarAgenciaAtiva(agencia, codgAgencia, 404);
        RefUnidade unidade = buscarUnidadeOuFalhar(agencia.getCodgUnidade());
        validarUnidadeAtiva(unidade, agencia.getCodgUnidade(), 404);
        return manager.getList(
                "chat-confianca/consultas/departamentos-disponiveis/agencia/" + codgAgencia,
                new ParameterizedTypeReference<List<DepartamentoUnidade>>() {
                }
        );
    }
    public List<DepartamentoUnidade> listarDepartamentosDisponiveisPorUsuario(Integer codgUsuario) {
        return listarDepartamentosDisponiveisPorUsuario(codgUsuario, null);
    }

    public List<DepartamentoUnidade> listarDepartamentosDisponiveisPorUsuario(Integer codgUsuario, Integer codgAgenciaSessao) {
        SessaoChatResponse sessao = montarSessao(codgUsuario, codgAgenciaSessao);
        List<DepartamentoUnidade> departamentos = listarDepartamentosSessao(sessao);
        Integer codgUnidade = unidadeSessao(sessao);
        if ((departamentos == null || departamentos.isEmpty()) && codgUnidade != null) {
            DepartamentoUnidade fallback = garantirDepartamentoConfiaGeral(codgUnidade);
            departamentos = fallback == null ? new ArrayList<>() : List.of(fallback);
        }
        if (departamentos == null || departamentos.isEmpty()) {
            throw regra(404, "Usuario " + codgUsuario + " nao possui unidade/agencia ativa vinculada para o chat.");
        }
        return departamentos;
    }

    public List<DepartamentoAtendimentoOpcao> listarOpcoesAtendimentoUsuario(Integer codgUsuario, Integer codgAgenciaSessao) {
        SessaoChatResponse sessao = montarSessao(codgUsuario, codgAgenciaSessao);
        List<DepartamentoUnidade> departamentos = listarDepartamentosSessao(sessao);
        Integer codgUnidade = unidadeSessao(sessao);
        if ((departamentos == null || departamentos.isEmpty()) && codgUnidade != null) {
            garantirDepartamentoConfiaGeral(codgUnidade);
            departamentos = new ArrayList<>();
        }
        if (departamentos == null || departamentos.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> idsConfiaGeral = idsDepartamentoConfiaGeral();
        return departamentos.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null)
                .filter(item -> !idsConfiaGeral.contains(item.getDepartamentoId()))
                .sorted(Comparator.comparing(this::nomeOpcaoDepartamento, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(this::montarOpcaoAtendimento)
                .collect(Collectors.toList());
    }

    private List<DepartamentoUnidade> listarDepartamentosSessao(SessaoChatResponse sessao) {
        if (sessao == null) {
            return new ArrayList<>();
        }
        RefAgencia agencia = sessao.getAgencia();
        if (agencia != null && agencia.getCodgAgencia() != null) {
            return listarDepartamentosDisponiveis(agencia.getCodgAgencia());
        }
        Integer codgUnidade = unidadeSessao(sessao);
        if (codgUnidade == null) {
            return new ArrayList<>();
        }
        return configService.listarDepartamentoUnidadesPorUnidade(codgUnidade).stream()
                .filter(item -> !Boolean.FALSE.equals(item.getAtivo()))
                .collect(Collectors.toList());
    }

    private DepartamentoAtendimentoOpcao montarOpcaoAtendimento(DepartamentoUnidade departamentoUnidade) {
        boolean ativo = departamentoUnidade != null && !Boolean.FALSE.equals(departamentoUnidade.getAtivo());
        boolean possuiAtendente = ativo && possuiAtendenteHumano(departamentoUnidade);
        DepartamentoAtendimentoOpcao opcao = new DepartamentoAtendimentoOpcao();
        opcao.setDepartamentoUnidadeId(departamentoUnidade.getId());
        opcao.setDepartamentoId(departamentoUnidade.getDepartamentoId());
        opcao.setCodgUnidade(departamentoUnidade.getCodgUnidade());
        opcao.setNomeExibicao(nomeOpcaoDepartamento(departamentoUnidade));
        opcao.setMensagemAbertura(departamentoUnidade.getMensagemAbertura());
        opcao.setAtivo(ativo);
        opcao.setPossuiAtendente(possuiAtendente);
        opcao.setPermiteHumano(possuiAtendente);
        opcao.setSomenteConfia(!possuiAtendente);
        if (!ativo) {
            opcao.setMotivoIndisponibilidade("Departamento indisponivel no momento.");
        } else if (!possuiAtendente) {
            opcao.setMotivoIndisponibilidade("Sem atendente humano vinculado para esta unidade.");
        }
        return opcao;
    }

    private boolean possuiAtendenteHumano(DepartamentoUnidade departamentoUnidade) {
        if (departamentoUnidade == null || departamentoUnidade.getId() == null) {
            return false;
        }
        try {
            List<DepartamentoAtendente> atendentes = configService.listarAtendentesDepartamento(departamentoUnidade.getId());
            return atendentes != null && atendentes.stream()
                    .filter(Objects::nonNull)
                    .filter(item -> !Boolean.FALSE.equals(item.getAtivo()))
                    .filter(item -> !Boolean.FALSE.equals(item.getRecebeChamados()))
                    .anyMatch(item -> item.getCodgUsuario() != null);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private DepartamentoUnidade garantirDepartamentoConfiaGeral(Integer codgUnidade) {
        if (codgUnidade == null) {
            return null;
        }
        Departamento departamento = buscarOuCriarDepartamentoConfiaGeral();
        return configService.listarDepartamentoUnidadesPorUnidade(codgUnidade).stream()
                .filter(item -> Objects.equals(item.getDepartamentoId(), departamento.getId()))
                .findFirst()
                .orElseGet(() -> criarDepartamentoUnidadeConfiaGeral(codgUnidade, departamento));
    }

    private Departamento buscarOuCriarDepartamentoConfiaGeral() {
        return configService.listarDepartamentos().stream()
                .filter(item -> CODIGO_DEPARTAMENTO_CONFIA_GERAL.equalsIgnoreCase(item.getCodigo()))
                .findFirst()
                .orElseGet(() -> {
                    Departamento departamento = new Departamento();
                    departamento.setNome(NOME_DEPARTAMENTO_CONFIA_GERAL);
                    departamento.setCodigo(CODIGO_DEPARTAMENTO_CONFIA_GERAL);
                    departamento.setDescricao("Departamento tecnico para conversas atendidas somente pela ConfIA.");
                    departamento.setCorHex("#2563eb");
                    departamento.setIcone("pi pi-sparkles");
                    departamento.setAtivo(true);
                    return configService.salvarDepartamento(departamento);
                });
    }

    private DepartamentoUnidade criarDepartamentoUnidadeConfiaGeral(Integer codgUnidade, Departamento departamento) {
        DepartamentoUnidade departamentoUnidade = new DepartamentoUnidade();
        departamentoUnidade.setDepartamentoId(departamento.getId());
        departamentoUnidade.setCodgUnidade(codgUnidade);
        departamentoUnidade.setNomeExibicao(NOME_DEPARTAMENTO_CONFIA_GERAL);
        departamentoUnidade.setMensagemAbertura("Atendimento inicial pela ConfIA.");
        departamentoUnidade.setMensagemForaHorario("A ConfIA continua disponivel para orientacoes iniciais.");
        departamentoUnidade.setPermiteChamadoAgencia(true);
        departamentoUnidade.setPermiteChamadoInterno(true);
        departamentoUnidade.setExigeAssunto(false);
        departamentoUnidade.setDistribuicao(DistribuicaoDepartamento.MANUAL);
        departamentoUnidade.setLimiteChatsPorAtendente(3);
        departamentoUnidade.setAtivo(true);
        return configService.salvarDepartamentoUnidade(departamentoUnidade);
    }

    private Set<Long> idsDepartamentoConfiaGeral() {
        return configService.listarDepartamentos().stream()
                .filter(Objects::nonNull)
                .filter(item -> CODIGO_DEPARTAMENTO_CONFIA_GERAL.equalsIgnoreCase(item.getCodigo()))
                .map(Departamento::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private String nomeOpcaoDepartamento(DepartamentoUnidade departamentoUnidade) {
        if (departamentoUnidade == null) {
            return "Atendimento";
        }
        return isBlank(departamentoUnidade.getNomeExibicao())
                ? "Atendimento"
                : departamentoUnidade.getNomeExibicao();
    }
    public Conversa abrirConversa(AbrirConversaRequest request) {
        validarObrigatorio(request, "Informe os dados da conversa.");
        validarObrigatorio(request.getCodgUsuario(), "Informe o usuario.");
        validarObrigatorio(request.getDepartamentoUnidadeId(), "Informe o departamento.");

        SessaoChatResponse sessao = montarSessao(request.getCodgUsuario(), request.getCodgAgenciaSessao());
        RefUsuario usuario = sessao.getUsuario();
        RefAgencia agencia = sessao.getAgencia();
        if (agencia == null || agencia.getCodgAgencia() == null) {
            throw regra(400, "Usuario nao esta vinculado a uma agencia.");
        }

        DepartamentoUnidade departamentoUnidade = listarDepartamentosDisponiveis(agencia.getCodgAgencia())
                .stream()
                .filter(item -> request.getDepartamentoUnidadeId().equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> regra(400, "Departamento indisponivel para a unidade da agencia."));

        if (!possuiAtendenteHumano(departamentoUnidade)) {
            throw regra(400, "Este departamento esta disponivel somente pela ConfIA no momento.");
        }

        if (Boolean.TRUE.equals(departamentoUnidade.getExigeAssunto()) && isBlank(request.getAssunto())) {
            throw regra(400, "Informe o assunto.");
        }

        LocalDateTime agora = LocalDateTime.now();
        PrioridadeConversa prioridade = request.getPrioridade() == null
                ? PrioridadeConversa.NORMAL
                : request.getPrioridade();

        Conversa conversa = new Conversa();
        conversa.setUuid(UUID.randomUUID().toString());
        conversa.setProtocolo(gerarProtocolo(agora));
        conversa.setDepartamentoUnidadeId(departamentoUnidade.getId());
        conversa.setCodgUnidade(agencia.getCodgUnidade());
        conversa.setCodgAgencia(agencia.getCodgAgencia());
        conversa.setSolicitanteCodgUsuario(usuario.getCodgUsuario());
        conversa.setAssunto(normalizarAssunto(request.getAssunto()));
        conversa.setDescricaoInicial(request.getDescricaoInicial());
        conversa.setOrigem(OrigemConversa.WEB);
        conversa.setStatus(StatusConversa.AGUARDANDO_ATENDENTE);
        conversa.setPrioridade(prioridade);
        conversa.setUltimoEventoEm(agora);
        conversa = manager.post("chat-confianca/persistencia/conversas", conversa, Conversa.class);

        ConversaParticipante solicitante = new ConversaParticipante();
        solicitante.setConversaId(conversa.getId());
        solicitante.setCodgUsuario(usuario.getCodgUsuario());
        solicitante.setPapel(PapelParticipante.SOLICITANTE);
        solicitante.setAtivo(true);
        solicitante.setSilenciado(false);
        manager.post("chat-confianca/persistencia/conversa-participantes", solicitante, ConversaParticipante.class);

        FilaAtendimento fila = new FilaAtendimento();
        fila.setConversaId(conversa.getId());
        fila.setDepartamentoUnidadeId(departamentoUnidade.getId());
        fila.setCodgUnidade(agencia.getCodgUnidade());
        fila.setCodgAgencia(agencia.getCodgAgencia());
        fila.setSolicitanteCodgUsuario(usuario.getCodgUsuario());
        fila.setStatus(StatusFila.AGUARDANDO);
        fila.setPrioridade(prioridade);
        fila.setPosicao(calcularProximaPosicao(departamentoUnidade.getId()));
        fila = manager.post("chat-confianca/persistencia/filas", fila, FilaAtendimento.class);

        registrarEvento(conversa.getId(), "CONVERSA_ABERTA", usuario.getCodgUsuario(),
                "Conversa aberta pelo solicitante.");

        if (!isBlank(request.getDescricaoInicial())) {
            persistirMensagem(conversa, usuario.getCodgUsuario(), request.getDescricaoInicial(), false);
        }

        conversa = distribuirAutomaticamenteSePossivel(conversa, fila, departamentoUnidade, agora);

        return conversa;
    }

    public Conversa iniciarConversaAssistida(Integer codgUsuario, Long departamentoUnidadeId, String assunto,
                                             String descricaoInicial, PrioridadeConversa prioridade,
                                             String metadadosJson) {
        return iniciarConversaAssistida(codgUsuario, departamentoUnidadeId, assunto,
                descricaoInicial, prioridade, metadadosJson, null);
    }

    public Conversa iniciarConversaAssistida(Integer codgUsuario, Long departamentoUnidadeId, String assunto,
                                             String descricaoInicial, PrioridadeConversa prioridade,
                                             String metadadosJson, Integer codgAgenciaSessao) {
        validarObrigatorio(codgUsuario, "Informe o usuario.");
        validarObrigatorio(departamentoUnidadeId, "Informe o departamento.");

        SessaoChatResponse sessao = montarSessao(codgUsuario, codgAgenciaSessao);
        RefUsuario usuario = sessao.getUsuario();
        RefAgencia agencia = sessao.getAgencia();
        if (agencia == null || agencia.getCodgAgencia() == null) {
            throw regra(400, "Usuario nao esta vinculado a uma agencia.");
        }

        DepartamentoUnidade departamentoUnidade = listarDepartamentosDisponiveis(agencia.getCodgAgencia())
                .stream()
                .filter(item -> departamentoUnidadeId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> regra(400, "Departamento indisponivel para a unidade da agencia."));

        LocalDateTime agora = LocalDateTime.now();
        PrioridadeConversa prioridadeNormalizada = prioridade == null ? PrioridadeConversa.NORMAL : prioridade;

        Conversa conversa = new Conversa();
        conversa.setUuid(UUID.randomUUID().toString());
        conversa.setProtocolo(gerarProtocolo(agora));
        conversa.setDepartamentoUnidadeId(departamentoUnidade.getId());
        conversa.setCodgUnidade(agencia.getCodgUnidade());
        conversa.setCodgAgencia(agencia.getCodgAgencia());
        conversa.setSolicitanteCodgUsuario(usuario.getCodgUsuario());
        conversa.setAssunto(normalizarAssunto(isBlank(assunto) ? departamentoUnidade.getNomeExibicao() : assunto));
        conversa.setDescricaoInicial(descricaoInicial);
        conversa.setOrigem(OrigemConversa.WEB);
        conversa.setStatus(StatusConversa.AGUARDANDO_SOLICITANTE);
        conversa.setPrioridade(prioridadeNormalizada);
        conversa.setMetadadosJson(metadadosJson);
        conversa.setUltimoEventoEm(agora);
        conversa = manager.post("chat-confianca/persistencia/conversas", conversa, Conversa.class);

        ConversaParticipante solicitante = new ConversaParticipante();
        solicitante.setConversaId(conversa.getId());
        solicitante.setCodgUsuario(usuario.getCodgUsuario());
        solicitante.setPapel(PapelParticipante.SOLICITANTE);
        solicitante.setAtivo(true);
        solicitante.setSilenciado(false);
        manager.post("chat-confianca/persistencia/conversa-participantes", solicitante, ConversaParticipante.class);

        registrarEvento(conversa.getId(), "CONFIA_CONVERSA_INICIADA", usuario.getCodgUsuario(),
                "Conversa iniciada com a ConfIA.");
        return conversa;
    }

    public Mensagem registrarMensagemUsuarioAssistida(Long conversaId, Integer codgUsuario, String conteudo) {
        EnviarMensagemRequest request = new EnviarMensagemRequest();
        request.setConversaId(conversaId);
        request.setCodgUsuario(codgUsuario);
        request.setConteudo(conteudo);
        request.setInterna(false);
        request.setGestor(false);
        return enviarMensagem(request);
    }

    public Mensagem registrarMensagemBot(Long conversaId, String conteudo, String conteudoJson) {
        validarObrigatorio(conversaId, "Informe a conversa.");
        if (isBlank(conteudo)) {
            conteudo = "Nao consegui gerar uma resposta agora. Posso encaminhar para um atendente humano.";
        }
        Conversa conversa = buscarConversaOuFalhar(conversaId);
        if (!aceitaMensagem(conversa.getStatus())) {
            throw regra(409, "Conversa nao aceita novas mensagens.");
        }
        Mensagem mensagem = persistirMensagem(conversa, null, conteudo, false, TipoMensagem.TEXTO,
                conteudoJson, RemetenteTipo.BOT);
        conversa.setUltimoEventoEm(LocalDateTime.now());
        manager.post("chat-confianca/persistencia/conversas", conversa, Conversa.class);
        registrarEvento(conversa.getId(), "CONFIA_RESPONDEU", null, "ConfIA respondeu ao solicitante.");
        return mensagem;
    }

    public Mensagem registrarMensagemSistema(Long conversaId, String conteudo, String conteudoJson) {
        validarObrigatorio(conversaId, "Informe a conversa.");
        validarTextoObrigatorio(conteudo, "Informe a mensagem do sistema.");
        Conversa conversa = buscarConversaOuFalhar(conversaId);
        if (!aceitaMensagem(conversa.getStatus())) {
            throw regra(409, "Conversa nao aceita novas mensagens.");
        }
        Mensagem mensagem = persistirMensagem(conversa, null, conteudo.trim(), false,
                TipoMensagem.SISTEMA, conteudoJson, RemetenteTipo.SISTEMA);
        conversa.setUltimoEventoEm(LocalDateTime.now());
        manager.post("chat-confianca/persistencia/conversas", conversa, Conversa.class);
        registrarEvento(conversa.getId(), "REMARCACAO_CONTEXTO_GERADO", null,
                "Resumo da simulacao preparado para a equipe.");
        return mensagem;
    }

    public Conversa encaminharConversaParaAtendente(Long conversaId, Integer codgUsuario, String motivo) {
        validarObrigatorio(conversaId, "Informe a conversa.");
        validarObrigatorio(codgUsuario, "Informe o usuario.");

        Conversa conversa = buscarConversaOuFalhar(conversaId);
        validarSolicitacaoAtendimentoHumano(conversa, codgUsuario);
        if (conversa.getStatus() == StatusConversa.EM_ATENDIMENTO
                || conversa.getStatus() == StatusConversa.AGUARDANDO_ATENDENTE) {
            return conversa;
        }
        if (!aceitaMensagem(conversa.getStatus())) {
            throw regra(409, "Conversa nao aceita encaminhamento para atendimento humano.");
        }

        DepartamentoUnidade departamentoUnidade = manager.get(
                "chat-confianca/persistencia/departamento-unidades/" + conversa.getDepartamentoUnidadeId(),
                DepartamentoUnidade.class
        );
        if (departamentoUnidade == null) {
            throw regra(404, "Departamento da conversa nao encontrado.");
        }
        if (!possuiAtendenteHumano(departamentoUnidade)) {
            throw regra(400, "Este departamento nao possui atendente humano disponivel no momento.");
        }

        return encaminharConversaParaAtendente(
                conversa, departamentoUnidade, codgUsuario, motivo, false);
    }

    /**
     * Direciona o handoff de remarcacao para o unico departamento ativo da
     * unidade explicitamente configurado para receber remarcacoes aereas.
     *
     * <p>A configuracao e a existencia de atendente humano sao integralmente
     * validadas antes da primeira escrita. Assim, configuracoes ausentes ou
     * ambiguas nao alteram conversa, fila ou historico.</p>
     */
    public Conversa encaminharConversaParaDepartamentoRemarcacao(
            Long conversaId,
            Integer codgUsuario,
            String motivo) {
        validarObrigatorio(conversaId, "Informe a conversa.");
        validarObrigatorio(codgUsuario, "Informe o usuario.");

        Conversa conversa = buscarConversaOuFalhar(conversaId);
        validarSolicitacaoAtendimentoHumano(conversa, codgUsuario);
        if (!aceitaMensagem(conversa.getStatus())) {
            throw regra(409, "Conversa nao aceita encaminhamento para atendimento humano.");
        }
        if (conversa.getCodgUnidade() == null) {
            throw regra(409,
                    "A conversa nao possui unidade definida para localizar o departamento de remarcacao aerea.");
        }

        List<DepartamentoUnidade> configurados =
                configService.listarDepartamentoUnidadesPorUnidade(conversa.getCodgUnidade());
        List<DepartamentoUnidade> destinos = (configurados == null
                ? List.<DepartamentoUnidade>of()
                : configurados).stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null)
                .filter(item -> Objects.equals(item.getCodgUnidade(), conversa.getCodgUnidade()))
                .filter(item -> Boolean.TRUE.equals(item.getAtivo()))
                .filter(item -> Boolean.TRUE.equals(item.getRecebeRemarcacaoAerea()))
                .collect(Collectors.toList());

        if (destinos.isEmpty()) {
            throw regra(409,
                    "A unidade da conversa nao possui departamento ativo configurado "
                            + "para receber remarcacao aerea.");
        }
        if (destinos.size() > 1) {
            throw regra(409,
                    "A unidade da conversa possui mais de um departamento ativo configurado "
                            + "para receber remarcacao aerea. Mantenha apenas um destino.");
        }

        DepartamentoUnidade destino = destinos.get(0);
        if (!possuiAtendenteHumano(destino)) {
            throw regra(409,
                    "O departamento configurado para remarcacao aerea nao possui "
                            + "atendente humano ativo para receber chamados.");
        }

        boolean mudouDepartamento =
                !Objects.equals(conversa.getDepartamentoUnidadeId(), destino.getId());
        if (!mudouDepartamento
                && (conversa.getStatus() == StatusConversa.EM_ATENDIMENTO
                || conversa.getStatus() == StatusConversa.AGUARDANDO_ATENDENTE)) {
            return conversa;
        }

        return encaminharConversaParaAtendente(
                conversa, destino, codgUsuario, motivo, mudouDepartamento);
    }

    private void validarSolicitacaoAtendimentoHumano(Conversa conversa, Integer codgUsuario) {
        boolean acessoGestor = ehGestorOuAdmin(codgUsuario, conversa.getCodgUnidade());
        if (!acessoGestor && !usuarioParticipa(conversa.getId(), codgUsuario)) {
            throw regra(403, "Usuario nao participa da conversa.");
        }
    }

    private Conversa encaminharConversaParaAtendente(
            Conversa conversa,
            DepartamentoUnidade departamentoUnidade,
            Integer codgUsuario,
            String motivo,
            boolean registrarTransferencia) {
        LocalDateTime agora = LocalDateTime.now();
        Long departamentoOrigemId = conversa.getDepartamentoUnidadeId();
        Integer atendenteOrigem = conversa.getAtendenteResponsavelCodgUsuario();
        FilaAtendimento fila = buscarFilaPorConversa(conversa.getId());
        if (fila == null) {
            fila = new FilaAtendimento();
            fila.setConversaId(conversa.getId());
        }
        fila.setDepartamentoUnidadeId(departamentoUnidade.getId());
        fila.setCodgUnidade(departamentoUnidade.getCodgUnidade() == null
                ? conversa.getCodgUnidade()
                : departamentoUnidade.getCodgUnidade());
        fila.setCodgAgencia(conversa.getCodgAgencia());
        fila.setSolicitanteCodgUsuario(conversa.getSolicitanteCodgUsuario());
        fila.setPrioridade(conversa.getPrioridade());
        fila.setPosicao(calcularProximaPosicao(departamentoUnidade.getId()));
        fila.setStatus(StatusFila.AGUARDANDO);
        fila.setAtendenteDestinoCodgUsuario(null);
        fila.setChamadoEm(null);
        fila.setSaiuEm(null);
        fila.setMotivoSaida(null);
        fila = manager.post("chat-confianca/persistencia/filas", fila, FilaAtendimento.class);

        conversa.setDepartamentoUnidadeId(departamentoUnidade.getId());
        if (departamentoUnidade.getCodgUnidade() != null) {
            conversa.setCodgUnidade(departamentoUnidade.getCodgUnidade());
        }
        if (registrarTransferencia) {
            conversa.setAtendenteResponsavelCodgUsuario(null);
        }
        conversa.setStatus(StatusConversa.AGUARDANDO_ATENDENTE);
        conversa.setUltimoEventoEm(agora);
        conversa = manager.post("chat-confianca/persistencia/conversas", conversa, Conversa.class);

        String descricao = isBlank(motivo)
                ? "Atendimento humano solicitado."
                : motivo.trim();
        if (registrarTransferencia) {
            registrarTransferenciaAutomaticaRemarcacao(
                    conversa,
                    departamentoOrigemId,
                    atendenteOrigem,
                    departamentoUnidade.getId(),
                    descricao,
                    agora);
            try {
                atualizarCargaAtendente(atendenteOrigem);
            } catch (RuntimeException ex) {
                LOGGER.log(Level.WARNING,
                        "Conversa " + conversa.getId()
                                + " foi direcionada para remarcacao, mas a carga do atendente anterior "
                                + "nao foi atualizada.",
                        ex);
            }
        }
        registrarEvento(conversa.getId(), "ATENDIMENTO_HUMANO_SOLICITADO", codgUsuario, descricao);
        persistirResumoConfiaParaAtendimento(conversa, departamentoUnidade, descricao);
        persistirMensagem(conversa, null,
                "Certo, vou encaminhar voce para um atendente humano. A equipe recebera o contexto desta conversa.",
                false, TipoMensagem.SISTEMA, null, RemetenteTipo.SISTEMA);

        return distribuirAutomaticamenteSePossivel(conversa, fila, departamentoUnidade, agora);
    }

    private void registrarTransferenciaAutomaticaRemarcacao(
            Conversa conversa,
            Long departamentoOrigemId,
            Integer atendenteOrigem,
            Long departamentoDestinoId,
            String motivo,
            LocalDateTime agora) {
        try {
            ConversaTransferencia transferencia = new ConversaTransferencia();
            transferencia.setConversaId(conversa.getId());
            transferencia.setDepartamentoUnidadeOrigemId(departamentoOrigemId);
            transferencia.setDepartamentoUnidadeDestinoId(departamentoDestinoId);
            transferencia.setAtendenteOrigemCodgUsuario(atendenteOrigem);
            transferencia.setAtendenteDestinoCodgUsuario(null);
            transferencia.setMotivo(motivo);
            transferencia.setStatus(StatusTransferencia.ACEITA);
            transferencia.setRespondidoEm(agora);
            manager.post(
                    "chat-confianca/persistencia/conversa-transferencias",
                    transferencia,
                    ConversaTransferencia.class);
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING,
                    "Conversa " + conversa.getId()
                            + " foi direcionada para remarcacao, mas o historico de transferencia nao foi salvo.",
                    ex);
        }
    }

    private void persistirResumoConfiaParaAtendimento(Conversa conversa,
                                                       DepartamentoUnidade departamentoUnidade,
                                                       String motivoEncaminhamento) {
        if (conversa == null || !metadadosOrigemConfia(conversa.getMetadadosJson())) {
            return;
        }
        String resumo = montarResumoConfiaParaAtendente(conversa, departamentoUnidade, motivoEncaminhamento);
        if (isBlank(resumo)) {
            return;
        }
        persistirMensagem(conversa, null, resumo, true, TipoMensagem.SISTEMA, null, RemetenteTipo.SISTEMA);
        registrarEvento(conversa.getId(), "CONFIA_RESUMO_ATENDENTE", null,
                "Resumo da ConfIA gerado para atendimento humano.");
    }

    private String montarResumoConfiaParaAtendente(Conversa conversa,
                                                   DepartamentoUnidade departamentoUnidade,
                                                   String motivoEncaminhamento) {
        List<Mensagem> historico = listarMensagensPublicasConversa(conversa.getId()).stream()
                .filter(item -> item.getRemetenteTipo() == RemetenteTipo.USUARIO
                || item.getRemetenteTipo() == RemetenteTipo.BOT)
                .collect(Collectors.toList());

        Mensagem primeiraCliente = historico.stream()
                .filter(item -> item.getRemetenteTipo() == RemetenteTipo.USUARIO)
                .findFirst()
                .orElse(null);
        Mensagem ultimaCliente = historico.stream()
                .filter(item -> item.getRemetenteTipo() == RemetenteTipo.USUARIO)
                .reduce((anterior, atual) -> atual)
                .orElse(null);
        Mensagem ultimaConfia = historico.stream()
                .filter(item -> item.getRemetenteTipo() == RemetenteTipo.BOT)
                .reduce((anterior, atual) -> atual)
                .orElse(null);

        StringBuilder resumo = new StringBuilder();
        resumo.append("Resumo ConfIA para atendimento humano\n");
        adicionarLinhaResumo(resumo, "Protocolo", conversa.getProtocolo());
        adicionarLinhaResumo(resumo, "Departamento", departamentoUnidade == null ? null : departamentoUnidade.getNomeExibicao());
        adicionarLinhaResumo(resumo, "Assunto", conversa.getAssunto());
        adicionarLinhaResumo(resumo, "Motivo do encaminhamento", motivoEncaminhamento);
        adicionarLinhaResumo(resumo, "Mensagem inicial do cliente", conteudoMensagemResumo(primeiraCliente, 280));
        adicionarLinhaResumo(resumo, "Ultima mensagem do cliente", conteudoMensagemResumo(ultimaCliente, 280));
        adicionarLinhaResumo(resumo, "Ultima resposta da ConfIA", conteudoMensagemResumo(ultimaConfia, 420));

        if (!historico.isEmpty()) {
            resumo.append("\nHistorico recente:\n");
            int inicio = Math.max(0, historico.size() - 8);
            for (Mensagem mensagem : historico.subList(inicio, historico.size())) {
                resumo.append("- ")
                        .append(labelResumoMensagem(mensagem))
                        .append(": ")
                        .append(conteudoMensagemResumo(mensagem, 360))
                        .append("\n");
            }
        }
        resumo.append("\nOrientacao: validar o que a ConfIA ja respondeu e continuar no mesmo contexto.");
        return limitarTextoResumo(resumo.toString().trim(), 3800);
    }

    private List<Mensagem> listarMensagensPublicasConversa(Long conversaId) {
        List<Mensagem> mensagens = manager.getList(
                "chat-confianca/consultas/conversas/" + conversaId + "/mensagens",
                new ParameterizedTypeReference<List<Mensagem>>() {
                }
        );
        if (mensagens == null || mensagens.isEmpty()) {
            return new ArrayList<>();
        }
        return mensagens.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getStatus() != StatusMensagem.EXCLUIDA)
                .filter(item -> item.getVisibilidade() == null || item.getVisibilidade() == VisibilidadeMensagem.PUBLICA)
                .filter(item -> !isBlank(item.getConteudo()))
                .sorted(Comparator.comparing(Mensagem::getEnviadaEm, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    private void adicionarLinhaResumo(StringBuilder resumo, String label, String valor) {
        if (!isBlank(valor)) {
            resumo.append("- ").append(label).append(": ").append(limparTextoResumo(valor)).append("\n");
        }
    }

    private String labelResumoMensagem(Mensagem mensagem) {
        if (mensagem == null || mensagem.getRemetenteTipo() == null) {
            return "Mensagem";
        }
        if (mensagem.getRemetenteTipo() == RemetenteTipo.BOT) {
            return "ConfIA";
        }
        if (mensagem.getRemetenteTipo() == RemetenteTipo.SISTEMA) {
            return "Sistema";
        }
        return "Cliente";
    }

    private String conteudoMensagemResumo(Mensagem mensagem, int limite) {
        return mensagem == null ? null : limitarTextoResumo(limparTextoResumo(mensagem.getConteudo()), limite);
    }

    private String limparTextoResumo(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.replace('\r', ' ')
                .replace('\n', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String limitarTextoResumo(String texto, int limite) {
        if (texto == null || limite <= 0 || texto.length() <= limite) {
            return texto;
        }
        return texto.substring(0, Math.max(0, limite - 3)).trim() + "...";
    }
    public Conversa buscarConversa(Long conversaId) {
        return buscarConversaOuFalhar(conversaId);
    }

    public Conversa buscarConversa(Long conversaId, Integer codgUsuario, boolean gestor) {
        validarObrigatorio(codgUsuario, "Informe o usuario.");
        Conversa conversa = buscarConversaOuFalhar(conversaId);
        validarAcessoConversa(conversa, codgUsuario, gestor, "Usuario nao participa da conversa.");
        return conversa;
    }

    public List<Mensagem> listarMensagens(Long conversaId, Integer codgUsuario, boolean podeVerInternas, boolean gestor) {
        validarObrigatorio(codgUsuario, "Informe o usuario.");
        Conversa conversa = buscarConversaOuFalhar(conversaId);
        boolean acessoGestor = gestor && ehGestorOuAdmin(codgUsuario, conversa.getCodgUnidade());

        if (!acessoGestor && !usuarioParticipa(conversaId, codgUsuario)) {
            throw regra(403, "Usuario nao participa da conversa.");
        }

        boolean acessoInternas = podeVerInternas
                && (acessoGestor || Objects.equals(codgUsuario, conversa.getAtendenteResponsavelCodgUsuario()));

        List<Mensagem> resultado = manager.getList(
                "chat-confianca/consultas/conversas/" + conversaId + "/mensagens",
                new ParameterizedTypeReference<List<Mensagem>>() {
                }
        ).stream()
                .filter(item -> item.getStatus() != StatusMensagem.EXCLUIDA)
                .filter(item -> acessoInternas || item.getVisibilidade() == VisibilidadeMensagem.PUBLICA)
                .sorted(Comparator.comparing(Mensagem::getEnviadaEm, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Mensagem::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
        preencherAnexos(conversaId, resultado);
        return resultado;
    }

    public Mensagem enviarMensagem(EnviarMensagemRequest request) {
        validarObrigatorio(request, "Informe os dados da mensagem.");
        validarObrigatorio(request.getConversaId(), "Informe a conversa.");
        validarObrigatorio(request.getCodgUsuario(), "Informe o usuario.");
        if (isBlank(request.getConteudo())) {
            throw regra(400, "Informe a mensagem.");
        }

        Conversa conversa = buscarConversaOuFalhar(request.getConversaId());
        if (!aceitaMensagem(conversa.getStatus())) {
            throw regra(409, "Conversa nao aceita novas mensagens.");
        }

        boolean acessoGestor = Boolean.TRUE.equals(request.getGestor())
                && ehGestorOuAdmin(request.getCodgUsuario(), conversa.getCodgUnidade());
        if (!acessoGestor && !usuarioParticipa(request.getConversaId(), request.getCodgUsuario())) {
            throw regra(403, "Usuario nao participa da conversa.");
        }

        boolean interna = Boolean.TRUE.equals(request.getInterna());
        if (interna && !acessoGestor
                && !Objects.equals(request.getCodgUsuario(), conversa.getAtendenteResponsavelCodgUsuario())) {
            throw regra(403, "Mensagem interna restrita a atendentes e gestores.");
        }

        Mensagem mensagem = persistirMensagem(conversa, request.getCodgUsuario(), request.getConteudo(), interna);
        conversa.setUltimoEventoEm(LocalDateTime.now());
        manager.post("chat-confianca/persistencia/conversas", conversa, Conversa.class);
        registrarEvento(conversa.getId(), interna ? "MENSAGEM_INTERNA" : "MENSAGEM_ENVIADA",
                request.getCodgUsuario(), interna ? "Mensagem interna enviada." : "Mensagem enviada.");
        return mensagem;
    }
    public Mensagem enviarAnexo(EnviarAnexoRequest request) {
        validarObrigatorio(request, "Informe os dados do anexo.");
        validarObrigatorio(request.getConversaId(), "Informe a conversa.");
        validarObrigatorio(request.getCodgUsuario(), "Informe o usuario.");
        if (isBlank(request.getNomeArquivo())) {
            throw regra(400, "Informe o nome do arquivo.");
        }
        if (isBlank(request.getConteudoBase64())) {
            throw regra(400, "Informe o arquivo.");
        }

        Conversa conversa = buscarConversaOuFalhar(request.getConversaId());
        if (!aceitaMensagem(conversa.getStatus())) {
            throw regra(409, "Conversa nao aceita novos anexos.");
        }

        boolean acessoGestor = Boolean.TRUE.equals(request.getGestor())
                && ehGestorOuAdmin(request.getCodgUsuario(), conversa.getCodgUnidade());
        if (!acessoGestor && !usuarioParticipa(request.getConversaId(), request.getCodgUsuario())) {
            throw regra(403, "Usuario nao participa da conversa.");
        }

        boolean interna = Boolean.TRUE.equals(request.getInterna());
        if (interna && !acessoGestor
                && !Objects.equals(request.getCodgUsuario(), conversa.getAtendenteResponsavelCodgUsuario())) {
            throw regra(403, "Anexo interno restrito a atendentes e gestores.");
        }

        byte[] conteudo = decodificarBase64(request.getConteudoBase64());
        if (conteudo.length == 0) {
            throw regra(400, "Arquivo vazio.");
        }
        if (conteudo.length > MAX_ANEXO_BYTES) {
            throw regra(400, "Arquivo maior que 10MB.");
        }

        String nomeOriginal = normalizarNomeArquivo(request.getNomeArquivo());
        String mimeType = normalizarMimeType(request.getMimeType(), nomeOriginal);
        TipoMensagem tipo = mimeType.startsWith("image/") ? TipoMensagem.IMAGEM : TipoMensagem.ARQUIVO;
        Mensagem mensagem = persistirMensagem(conversa, request.getCodgUsuario(), nomeOriginal, interna, tipo, null);
        MensagemAnexo anexo = armazenarAnexo(conversa, mensagem, nomeOriginal, mimeType, conteudo);
        mensagem.getAnexos().add(anexo);

        conversa.setUltimoEventoEm(LocalDateTime.now());
        manager.post("chat-confianca/persistencia/conversas", conversa, Conversa.class);
        registrarEvento(conversa.getId(), interna ? "ANEXO_INTERNO" : "ANEXO_ENVIADO",
                request.getCodgUsuario(), "Anexo enviado: " + nomeOriginal);
        return mensagem;
    }

    public AnexoDownloadResponse baixarAnexo(Long anexoId, Integer codgUsuario, boolean gestor) {
        validarObrigatorio(anexoId, "Informe o anexo.");
        validarObrigatorio(codgUsuario, "Informe o usuario.");

        MensagemAnexo anexo = manager.get(
                "chat-confianca/persistencia/mensagem-anexos/" + anexoId,
                MensagemAnexo.class
        );
        if (anexo == null) {
            throw regra(404, "Anexo nao encontrado.");
        }

        Mensagem mensagem = manager.get(
                "chat-confianca/persistencia/mensagens/" + anexo.getMensagemId(),
                Mensagem.class
        );
        if (mensagem == null) {
            throw regra(404, "Mensagem do anexo nao encontrada.");
        }

        Conversa conversa = buscarConversaOuFalhar(mensagem.getConversaId());
        boolean acessoGestor = gestor && ehGestorOuAdmin(codgUsuario, conversa.getCodgUnidade());
        if (!acessoGestor && !usuarioParticipa(conversa.getId(), codgUsuario)) {
            throw regra(403, "Usuario nao participa da conversa.");
        }
        if (mensagem.getVisibilidade() == VisibilidadeMensagem.INTERNA
                && !acessoGestor
                && !Objects.equals(codgUsuario, conversa.getAtendenteResponsavelCodgUsuario())) {
            throw regra(403, "Anexo interno restrito a atendentes e gestores.");
        }

        Path arquivo = Paths.get(anexo.getCaminhoStorage());
        if (!Files.exists(arquivo) || !Files.isRegularFile(arquivo)) {
            throw regra(404, "Arquivo do anexo nao encontrado no storage.");
        }

        try {
            AnexoDownloadResponse response = new AnexoDownloadResponse();
            response.setNomeArquivo(anexo.getNomeOriginal());
            response.setMimeType(anexo.getMimeType());
            response.setConteudo(Files.readAllBytes(arquivo));
            return response;
        } catch (Exception ex) {
            throw regra(500, "Nao foi possivel ler o anexo.");
        }
    }

    public List<VwFilaAtendimento> listarFilaParaAtendente(Integer codgUsuario, boolean gestor) {
        validarObrigatorio(codgUsuario, "Informe o atendente.");
        SessaoChatResponse sessao = montarSessao(codgUsuario);
        if (gestor && sessao.isAdmin()) {
            return ordenarFila(listarViewsFilaAbertas());
        }
        if (gestor && sessao.isGestor()) {
            Integer codgUnidade = unidadeGestaoSessao(sessao);
            Set<Long> conversasUnidade = listarHistoricoUnidade(codgUnidade).stream()
                    .map(VwConversaResumo::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            return ordenarFila(listarViewsFilaAbertas().stream()
                    .filter(item -> item.getConversaId() != null
                    && conversasUnidade.contains(item.getConversaId()))
                    .collect(Collectors.toList()));
        }

        Set<Long> filasPermitidas = listarDepartamentosAtendente(codgUsuario).stream()
                .filter(item -> Boolean.TRUE.equals(item.getAtivo()))
                .filter(item -> Boolean.TRUE.equals(item.getRecebeChamados()))
                .flatMap(item -> listarFilasDepartamento(item.getDepartamentoUnidadeId()).stream())
                .filter(this::filaAberta)
                .map(FilaAtendimento::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (filasPermitidas.isEmpty()) {
            return new ArrayList<>();
        }

        return ordenarFila(listarViewsFilaAbertas().stream()
                .filter(item -> filasPermitidas.contains(item.getId()))
                .collect(Collectors.toList()));
    }

    public Conversa redistribuirFila(Long filaId, Integer codgUsuarioGestor) {
        validarObrigatorio(filaId, "Informe a fila.");
        validarObrigatorio(codgUsuarioGestor, "Informe o gestor.");
        FilaAtendimento fila = manager.get(
                "chat-confianca/persistencia/filas/" + filaId,
                FilaAtendimento.class
        );
        if (fila == null) {
            throw regra(404, "Registro de fila nao encontrado.");
        }
        if (!filaAberta(fila)) {
            throw regra(409, "Atendimento ja saiu da fila.");
        }
        Conversa conversa = buscarConversaOuFalhar(fila.getConversaId());
        if (!ehGestorOuAdmin(codgUsuarioGestor, conversa.getCodgUnidade())) {
            throw regra(403, "Acesso restrito a gestores do chat.");
        }
        DepartamentoUnidade departamentoUnidade = manager.get(
                "chat-confianca/persistencia/departamento-unidades/" + fila.getDepartamentoUnidadeId(),
                DepartamentoUnidade.class
        );
        if (departamentoUnidade == null || !Boolean.TRUE.equals(departamentoUnidade.getAtivo())) {
            throw regra(404, "Departamento/unidade da fila nao encontrado ou inativo.");
        }
        DepartamentoAtendente candidato = selecionarAtendenteDistribuicao(departamentoUnidade);
        if (candidato == null) {
            throw regra(409, "Nenhum atendente online disponivel para redistribuir.");
        }
        return assumirFila(fila, conversa, candidato.getCodgUsuario(), "Redistribuido pelo gestor", LocalDateTime.now());
    }

    public Conversa assumirAtendimento(AssumirAtendimentoRequest request) {
        validarObrigatorio(request, "Informe os dados do atendimento.");
        validarObrigatorio(request.getFilaId(), "Informe a fila.");
        validarObrigatorio(request.getCodgAtendente(), "Informe o atendente.");

        FilaAtendimento fila = manager.get(
                "chat-confianca/persistencia/filas/" + request.getFilaId(),
                FilaAtendimento.class
        );
        if (fila == null) {
            throw regra(404, "Registro de fila nao encontrado.");
        }
        if (!filaAberta(fila)) {
            throw regra(409, "Atendimento ja saiu da fila.");
        }

        Conversa conversa = buscarConversaOuFalhar(fila.getConversaId());
        boolean acessoGestor = Boolean.TRUE.equals(request.getGestor())
                && ehGestorOuAdmin(request.getCodgAtendente(), conversa.getCodgUnidade());
        DepartamentoAtendente vinculo = buscarVinculo(fila.getDepartamentoUnidadeId(), request.getCodgAtendente());
        if (!acessoGestor && vinculo == null) {
            throw regra(403, "Atendente nao esta vinculado ao departamento/unidade.");
        }
        validarLimite(request.getCodgAtendente(), vinculo);
        validarStatusAtendenteDisponivel(request.getCodgAtendente());

        LocalDateTime agora = LocalDateTime.now();
        return assumirFila(fila, conversa, request.getCodgAtendente(), "Assumido pelo atendente", agora);
    }

    public Conversa encerrarConversa(EncerrarConversaRequest request) {
        validarObrigatorio(request, "Informe os dados de encerramento.");
        validarObrigatorio(request.getConversaId(), "Informe a conversa.");
        validarObrigatorio(request.getCodgUsuario(), "Informe o usuario.");
        validarTextoObrigatorio(request.getMotivo(), "Informe o motivo do encerramento.");

        Conversa conversa = buscarConversaOuFalhar(request.getConversaId());
        boolean acessoGestor = Boolean.TRUE.equals(request.getGestor())
                && ehGestorOuAdmin(request.getCodgUsuario(), conversa.getCodgUnidade());
        if (!acessoGestor && !usuarioParticipa(request.getConversaId(), request.getCodgUsuario())) {
            throw regra(403, "Usuario nao participa da conversa.");
        }
        if (conversa.getStatus() == StatusConversa.ENCERRADA || conversa.getStatus() == StatusConversa.CANCELADA) {
            return conversa;
        }
        String motivoEncerramento = montarMotivoEncerramento(request);

        LocalDateTime agora = LocalDateTime.now();
        conversa.setStatus(StatusConversa.ENCERRADA);
        conversa.setEncerradoEm(agora);
        conversa.setEncerradoPorCodgUsuario(request.getCodgUsuario());
        conversa.setMotivoEncerramento(motivoEncerramento);
        conversa.setUltimoEventoEm(agora);
        conversa = manager.post("chat-confianca/persistencia/conversas", conversa, Conversa.class);

        FilaAtendimento fila = buscarFilaPorConversa(request.getConversaId());
        if (fila != null && fila.getSaiuEm() == null) {
            fila.setStatus(StatusFila.CANCELADO);
            fila.setSaiuEm(agora);
            fila.setMotivoSaida(motivoEncerramento);
            manager.post("chat-confianca/persistencia/filas", fila, FilaAtendimento.class);
        }

        registrarEvento(request.getConversaId(), "CONVERSA_ENCERRADA", request.getCodgUsuario(), motivoEncerramento);
        atualizarCargaAtendente(conversa.getAtendenteResponsavelCodgUsuario());
        return conversa;
    }

    public int encerrarConversasIaInativas(int minutosInatividade) {
        int minutos = minutosInatividade <= 0 ? MINUTOS_AUTO_ENCERRAMENTO_CONFIA_PADRAO : minutosInatividade;
        LocalDateTime limite = LocalDateTime.now().minusMinutes(minutos);
        List<Conversa> conversas = manager.getList(
                "chat-confianca/persistencia/conversas",
                new ParameterizedTypeReference<List<Conversa>>() {
                }
        );
        if (conversas == null || conversas.isEmpty()) {
            return 0;
        }

        int encerradas = 0;
        for (Conversa conversa : conversas) {
            if (!elegivelAutoEncerramentoConfia(conversa, limite)) {
                continue;
            }
            EncerrarConversaRequest request = new EncerrarConversaRequest();
            request.setConversaId(conversa.getId());
            request.setCodgUsuario(conversa.getSolicitanteCodgUsuario());
            request.setCategoria("RESOLVIDA_IA_AUTO");
            request.setMotivo("Encerrada automaticamente por inatividade apos atendimento da ConfIA.");
            request.setGestor(false);
            try {
                encerrarConversa(request);
                encerradas++;
            } catch (RuntimeException ex) {
                LOGGER.log(Level.WARNING, "Nao foi possivel autoencerrar conversa IA " + conversa.getId(), ex);
            }
        }
        return encerradas;
    }

    public AtendimentoAvaliacao buscarAvaliacaoAtendimento(Long conversaId, Integer codgUsuario, boolean gestor) {
        validarObrigatorio(conversaId, "Informe a conversa.");
        validarObrigatorio(codgUsuario, "Informe o usuario.");
        Conversa conversa = buscarConversaOuFalhar(conversaId);
        validarAcessoConversa(conversa, codgUsuario, gestor, "Usuario nao participa da conversa.");

        if (gestor && ehGestorOuAdmin(codgUsuario, conversa.getCodgUnidade())) {
            List<AtendimentoAvaliacao> avaliacoes = manager.getList(
                    "chat-confianca/consultas/conversas/" + conversaId + "/avaliacoes",
                    new ParameterizedTypeReference<List<AtendimentoAvaliacao>>() {
                    }
            );
            return avaliacoes.isEmpty() ? null : avaliacoes.get(0);
        }

        return manager.get(
                "chat-confianca/consultas/conversas/" + conversaId + "/avaliacoes/" + codgUsuario,
                AtendimentoAvaliacao.class
        );
    }

    public AtendimentoAvaliacao avaliarAtendimento(AvaliarAtendimentoRequest request) {
        validarObrigatorio(request, "Informe os dados da avaliacao.");
        validarObrigatorio(request.getConversaId(), "Informe a conversa.");
        validarObrigatorio(request.getCodgUsuarioAvaliador(), "Informe o usuario avaliador.");
        if (request.getNota() == null || request.getNota() < 1 || request.getNota() > 5) {
            throw regra(400, "A nota deve estar entre 1 e 5.");
        }

        Conversa conversa = buscarConversaOuFalhar(request.getConversaId());
        if (conversa.getStatus() != StatusConversa.ENCERRADA) {
            throw regra(409, "Somente conversas encerradas podem ser avaliadas.");
        }
        if (!Objects.equals(conversa.getSolicitanteCodgUsuario(), request.getCodgUsuarioAvaliador())) {
            throw regra(403, "Somente o solicitante pode avaliar o atendimento.");
        }

        if (buscarAvaliacaoAtendimento(request.getConversaId(), request.getCodgUsuarioAvaliador(), false) != null) {
            throw regra(409, "Esta conversa ja foi avaliada por este usuario.");
        }

        AtendimentoAvaliacao avaliacao = new AtendimentoAvaliacao();
        avaliacao.setConversaId(request.getConversaId());
        avaliacao.setCodgUsuarioAvaliador(request.getCodgUsuarioAvaliador());
        avaliacao.setNota(request.getNota());
        avaliacao.setComentario(normalizarComentarioAvaliacao(request.getComentario()));
        avaliacao = manager.post("chat-confianca/persistencia/atendimento-avaliacoes",
                avaliacao, AtendimentoAvaliacao.class);
        registrarEvento(request.getConversaId(), "CONVERSA_AVALIADA", request.getCodgUsuarioAvaliador(),
                "Atendimento avaliado.");
        return avaliacao;
    }

    public int registrarLeitura(RegistrarLeituraRequest request) {
        validarObrigatorio(request, "Informe os dados da leitura.");
        validarObrigatorio(request.getConversaId(), "Informe a conversa.");
        validarObrigatorio(request.getCodgUsuario(), "Informe o usuario.");

        Conversa conversa = buscarConversaOuFalhar(request.getConversaId());
        boolean acessoGestor = Boolean.TRUE.equals(request.getGestor())
                && ehGestorOuAdmin(request.getCodgUsuario(), conversa.getCodgUnidade());
        if (!acessoGestor && !usuarioParticipa(request.getConversaId(), request.getCodgUsuario())) {
            throw regra(403, "Usuario nao participa da conversa.");
        }

        boolean incluirInternas = acessoGestor
                || Objects.equals(
                        request.getCodgUsuario(),
                        conversa.getAtendenteResponsavelCodgUsuario());
        Integer atualizadas = manager.post(
                "chat-confianca/persistencia/mensagem-leituras/conversas/"
                        + request.getConversaId() + "/usuarios/" + request.getCodgUsuario()
                        + "?incluirInternas=" + incluirInternas,
                null,
                Integer.class
        );
        return atualizadas == null ? 0 : atualizadas;
    }

    public List<VwConversaResumo> listarHistoricoSolicitante(Integer codgUsuario) {
        validarObrigatorio(codgUsuario, "Informe o usuario.");
        return manager.getList(
                "chat-confianca/consultas/resumos/solicitante/" + codgUsuario,
                new ParameterizedTypeReference<List<VwConversaResumo>>() {
                }
        );
    }

    public List<VwConversaResumo> listarHistoricoAtendente(Integer codgUsuario) {
        validarObrigatorio(codgUsuario, "Informe o atendente.");
        return manager.getList(
                "chat-confianca/consultas/resumos/atendente/" + codgUsuario,
                new ParameterizedTypeReference<List<VwConversaResumo>>() {
                }
        );
    }

    public ChatNotificacaoResumoResponse resumirNotificacoes(Integer codgUsuario) {
        validarObrigatorio(codgUsuario, "Informe o usuario.");
        SessaoChatResponse sessao = montarSessao(codgUsuario);

        MensagensNaoLidasResumo usuario = manager.get(
                "chat-confianca/consultas/resumos/nao-lidas/solicitante/" + codgUsuario,
                MensagensNaoLidasResumo.class);
        if (usuario == null) {
            usuario = new MensagensNaoLidasResumo();
        }

        boolean equipe = sessao.isAtendente() || sessao.isGestor() || sessao.isAdmin();
        MensagensNaoLidasResumo atendente = new MensagensNaoLidasResumo();
        long filasAguardando = 0L;
        if (equipe) {
            MensagensNaoLidasResumo resumoAtendente = manager.get(
                    "chat-confianca/consultas/resumos/nao-lidas/atendente/" + codgUsuario,
                    MensagensNaoLidasResumo.class);
            if (resumoAtendente != null) {
                atendente = resumoAtendente;
            }
            boolean acessoGestao = sessao.isGestor() || sessao.isAdmin();
            filasAguardando = listarFilaParaAtendente(codgUsuario, acessoGestao).stream()
                    .map(VwFilaAtendimento::getConversaId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();
        }

        ChatNotificacaoResumoResponse response = new ChatNotificacaoResumoResponse();
        response.setCodgUsuario(codgUsuario);
        response.setAtendente(sessao.isAtendente());
        response.setGestor(sessao.isGestor());
        response.setAdmin(sessao.isAdmin());
        response.setFilasAguardando(filasAguardando);
        response.setConversasAtendenteNaoLidas(atendente.getConversasNaoLidas());
        response.setMensagensAtendenteNaoLidas(atendente.getMensagensNaoLidas());
        response.setConversaAtendenteDestaqueId(atendente.getConversaDestaqueId());
        response.setConversasUsuarioNaoLidas(usuario.getConversasNaoLidas());
        response.setMensagensUsuarioNaoLidas(usuario.getMensagensNaoLidas());
        response.setConversaUsuarioDestaqueId(usuario.getConversaDestaqueId());
        response.setTotalPendencias(filasAguardando
                + atendente.getConversasNaoLidas()
                + usuario.getConversasNaoLidas());
        response.setAtualizadoEm(LocalDateTime.now());
        return response;
    }

    public List<VwConversaResumo> listarHistoricoUnidade(Integer codgUnidade) {
        validarObrigatorio(codgUnidade, "Informe a unidade.");
        return manager.getList(
                "chat-confianca/consultas/resumos/unidade/" + codgUnidade,
                new ParameterizedTypeReference<List<VwConversaResumo>>() {
                }
        );
    }

    public List<VwConversaResumo> listarHistoricoUnidade(Integer codgUnidade, Integer codgUsuario) {
        validarObrigatorio(codgUsuario, "Informe o usuario.");
        if (!ehGestorOuAdmin(codgUsuario, codgUnidade)) {
            throw regra(403, "Historico restrito a gestores da unidade.");
        }
        return listarHistoricoUnidade(codgUnidade);
    }

    public List<VwConversaResumo> buscarHistorico(Integer codgUsuario,
                                                   boolean gestor,
                                                   String termo,
                                                   StatusConversa status,
                                                   PrioridadeConversa prioridade,
                                                   Integer codgUnidade,
                                                   Integer codgAgencia,
                                                   Integer codgSolicitante,
                                                   Integer codgAtendente,
                                                   Long departamentoUnidadeId,
                                                   LocalDateTime dataInicio,
                                                   LocalDateTime dataFim,
                                                   Integer limite) {
        validarObrigatorio(codgUsuario, "Informe o usuario.");
        SessaoChatResponse sessao = montarSessao(codgUsuario);
        Integer unidadeConsulta = codgUnidade;
        Integer solicitanteConsulta = codgSolicitante;
        Integer atendenteConsulta = codgAtendente;
        boolean acessoGestao = gestor && sessao != null && (sessao.isGestor() || sessao.isAdmin());

        if (acessoGestao) {
            if (!adminGlobalChat(sessao)) {
                Integer unidadeSessao = unidadeGestaoSessao(sessao);
                if (unidadeSessao == null) {
                    throw regra(403, "Usuario sem unidade para consultar historico do chat.");
                }
                if (unidadeConsulta != null && !Objects.equals(unidadeConsulta, unidadeSessao)) {
                    throw regra(403, "Historico restrito a unidade do gestor.");
                }
                unidadeConsulta = unidadeSessao;
            }
        } else if (sessao != null && sessao.isAtendente()) {
            atendenteConsulta = codgUsuario;
        } else {
            solicitanteConsulta = codgUsuario;
        }

        List<String> params = new ArrayList<>();
        adicionarParametro(params, "termo", termo);
        adicionarParametro(params, "status", status);
        adicionarParametro(params, "prioridade", prioridade);
        adicionarParametro(params, "codgUnidade", unidadeConsulta);
        adicionarParametro(params, "codgAgencia", codgAgencia);
        adicionarParametro(params, "codgSolicitante", solicitanteConsulta);
        adicionarParametro(params, "codgAtendente", atendenteConsulta);
        adicionarParametro(params, "departamentoUnidadeId", departamentoUnidadeId);
        adicionarParametro(params, "dataInicio", dataInicio);
        adicionarParametro(params, "dataFim", dataFim);
        adicionarParametro(params, "limite", limite == null ? 200 : limite);
        adicionarParametro(params, "codgUsuarioLeitura", codgUsuario);

        String path = "chat-confianca/consultas/resumos/historico";
        if (!params.isEmpty()) {
            path += "?" + String.join("&", params);
        }
        return manager.getList(path, new ParameterizedTypeReference<List<VwConversaResumo>>() {
        });
    }

    public List<ConversaEvento> listarEventos(Long conversaId, Integer codgUsuario, boolean gestor) {
        validarObrigatorio(conversaId, "Informe a conversa.");
        validarObrigatorio(codgUsuario, "Informe o usuario.");
        Conversa conversa = buscarConversaOuFalhar(conversaId);
        validarAcessoConversa(conversa, codgUsuario, gestor, "Usuario nao participa da conversa.");
        return manager.getList(
                "chat-confianca/consultas/conversas/" + conversaId + "/eventos",
                new ParameterizedTypeReference<List<ConversaEvento>>() {
                }
        );
    }


    public List<Tag> listarTagsAtivas() {
        return manager.getList(
                "chat-confianca/persistencia/tags",
                new ParameterizedTypeReference<List<Tag>>() {
                }
        ).stream()
                .filter(item -> item != null && Boolean.TRUE.equals(item.getAtivo()))
                .sorted(Comparator.comparing(Tag::getNome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .collect(Collectors.toList());
    }

    public List<Tag> listarTagsAtivas(Integer codgUsuario) {
        validarAcessoAtendimento(codgUsuario);
        return listarTagsAtivas();
    }

    public List<RespostaRapida> listarRespostasRapidasAtendente(Integer codgUsuario,
                                                                Long departamentoId,
                                                                Integer codgUnidade) {
        SessaoChatResponse sessao = validarAcessoAtendimento(codgUsuario);
        Integer unidade = resolverUnidadeOperacional(sessao, codgUsuario, codgUnidade);
        if (departamentoId != null && unidade != null
                && !departamentoDisponivelNaUnidade(departamentoId, unidade)) {
            throw regra(403, "Departamento nao pertence a unidade informada.");
        }
        return configService.listarRespostasRapidas(departamentoId, unidade, true);
    }

    public RespostaRapida salvarRespostaRapidaAtendente(Integer codgUsuario,
                                                         RespostaRapida respostaRapida) {
        validarObrigatorio(respostaRapida, "Informe a resposta rapida.");
        SessaoChatResponse sessao = validarAcessoAtendimento(codgUsuario);
        Integer unidade = resolverUnidadeOperacional(
                sessao,
                codgUsuario,
                respostaRapida.getCodgUnidade());
        if (unidade == null && !sessao.isAdmin()) {
            throw regra(403, "Informe a unidade da resposta rapida.");
        }
        if (respostaRapida.getDepartamentoId() != null && unidade != null
                && !departamentoDisponivelNaUnidade(respostaRapida.getDepartamentoId(), unidade)) {
            throw regra(403, "Departamento nao pertence a unidade informada.");
        }
        respostaRapida.setCodgUnidade(unidade);
        respostaRapida.setCriadoPorCodgUsuario(codgUsuario);
        return configService.salvarRespostaRapida(respostaRapida);
    }

    public AtendenteStatus buscarAtendenteStatus(Integer codgUsuarioSolicitante,
                                                  Integer codgAtendente) {
        validarAcessoProprioAtendente(codgUsuarioSolicitante, codgAtendente);
        return configService.buscarAtendenteStatus(codgAtendente);
    }

    public AtendenteStatus salvarAtendenteStatus(Integer codgUsuarioSolicitante,
                                                  AtendenteStatus status) {
        validarObrigatorio(status, "Informe o status do atendente.");
        validarAcessoProprioAtendente(codgUsuarioSolicitante, status.getCodgUsuario());
        status.setCodgUsuario(codgUsuarioSolicitante);
        return configService.salvarAtendenteStatus(status);
    }

    public List<Tag> listarTagsConversa(Long conversaId, Integer codgUsuario, boolean gestor) {
        validarObrigatorio(conversaId, "Informe a conversa.");
        validarObrigatorio(codgUsuario, "Informe o usuario.");
        Conversa conversa = buscarConversaOuFalhar(conversaId);
        validarAcessoConversa(conversa, codgUsuario, gestor, "Usuario nao participa da conversa.");

        Set<Long> tagIds = manager.getList(
                "chat-confianca/persistencia/conversa-tags",
                new ParameterizedTypeReference<List<ConversaTag>>() {
                }
        ).stream()
                .filter(item -> Objects.equals(item.getConversaId(), conversaId))
                .map(ConversaTag::getTagId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (tagIds.isEmpty()) {
            return new ArrayList<>();
        }

        return listarTagsAtivas().stream()
                .filter(item -> tagIds.contains(item.getId()))
                .collect(Collectors.toList());
    }

    public Tag adicionarTagConversa(AdicionarTagConversaRequest request) {
        validarObrigatorio(request, "Informe os dados da tag.");
        validarObrigatorio(request.getConversaId(), "Informe a conversa.");
        validarObrigatorio(request.getCodgUsuario(), "Informe o usuario.");

        Conversa conversa = buscarConversaOuFalhar(request.getConversaId());
        validarAcessoConversa(conversa, request.getCodgUsuario(), Boolean.TRUE.equals(request.getGestor()),
                "Usuario nao participa da conversa.");

        Tag tag = request.getTagId() == null ? null
                : manager.get("chat-confianca/persistencia/tags/" + request.getTagId(), Tag.class);
        if (tag == null) {
            String nome = normalizarTag(request.getNome());
            if (isBlank(nome)) {
                throw regra(400, "Informe o nome da tag.");
            }
            tag = listarTagsAtivas().stream()
                    .filter(item -> item.getNome() != null && item.getNome().equalsIgnoreCase(nome))
                    .findFirst()
                    .orElse(null);
            if (tag == null) {
                tag = new Tag();
                tag.setNome(nome);
                tag.setCorHex(normalizarCorTag(request.getCorHex()));
                tag.setAtivo(true);
                tag = manager.post("chat-confianca/persistencia/tags", tag, Tag.class);
            }
        }
        if (tag == null || tag.getId() == null) {
            throw regra(404, "Tag nao encontrada.");
        }

        ConversaTag existente = manager.get(
                "chat-confianca/persistencia/conversa-tags/" + request.getConversaId() + "/" + tag.getId(),
                ConversaTag.class
        );
        if (existente == null) {
            ConversaTag vinculo = new ConversaTag();
            vinculo.setConversaId(request.getConversaId());
            vinculo.setTagId(tag.getId());
            vinculo.setCriadoPorCodgUsuario(request.getCodgUsuario());
            manager.post("chat-confianca/persistencia/conversa-tags", vinculo, ConversaTag.class);
            registrarEvento(request.getConversaId(), "TAG_ADICIONADA", request.getCodgUsuario(), tag.getNome());
        }
        return tag;
    }

    public void removerTagConversa(AdicionarTagConversaRequest request) {
        validarObrigatorio(request, "Informe os dados da tag.");
        validarObrigatorio(request.getConversaId(), "Informe a conversa.");
        validarObrigatorio(request.getTagId(), "Informe a tag.");
        validarObrigatorio(request.getCodgUsuario(), "Informe o usuario.");

        Conversa conversa = buscarConversaOuFalhar(request.getConversaId());
        validarAcessoConversa(conversa, request.getCodgUsuario(), Boolean.TRUE.equals(request.getGestor()),
                "Usuario nao participa da conversa.");
        manager.delete("chat-confianca/persistencia/conversa-tags/" + request.getConversaId() + "/" + request.getTagId());
        registrarEvento(request.getConversaId(), "TAG_REMOVIDA", request.getCodgUsuario(), "Tag removida.");
    }

    public Conversa transferirConversa(TransferirConversaRequest request) {
        validarObrigatorio(request, "Informe os dados da transferencia.");
        validarObrigatorio(request.getConversaId(), "Informe a conversa.");
        validarObrigatorio(request.getCodgUsuario(), "Informe o usuario.");
        validarObrigatorio(request.getDepartamentoUnidadeDestinoId(), "Informe o departamento de destino.");
        validarTextoObrigatorio(request.getMotivo(), "Informe o motivo da transferencia.");

        Conversa conversa = buscarConversaOuFalhar(request.getConversaId());
        if (!aceitaMensagem(conversa.getStatus())) {
            throw regra(409, "Conversa nao aceita transferencia.");
        }

        boolean acessoGestor = Boolean.TRUE.equals(request.getGestor())
                && ehGestorOuAdmin(request.getCodgUsuario(), conversa.getCodgUnidade());
        if (!acessoGestor && !Objects.equals(conversa.getAtendenteResponsavelCodgUsuario(), request.getCodgUsuario())) {
            throw regra(403, "Somente o atendente responsavel ou gestor pode transferir.");
        }

        DepartamentoUnidade destino = manager.get(
                "chat-confianca/persistencia/departamento-unidades/" + request.getDepartamentoUnidadeDestinoId(),
                DepartamentoUnidade.class
        );
        if (destino == null || !Boolean.TRUE.equals(destino.getAtivo())) {
            throw regra(404, "Departamento de destino nao encontrado ou inativo.");
        }
        if (Objects.equals(destino.getId(), conversa.getDepartamentoUnidadeId())
                && Objects.equals(request.getCodgAtendenteDestino(), conversa.getAtendenteResponsavelCodgUsuario())) {
            throw regra(409, "A conversa ja esta neste destino.");
        }
        if (request.getCodgAtendenteDestino() != null) {
            DepartamentoAtendente vinculoDestino = buscarVinculoObrigatorio(destino.getId(), request.getCodgAtendenteDestino());
            validarLimite(request.getCodgAtendenteDestino(), vinculoDestino);
            validarStatusAtendenteDisponivel(request.getCodgAtendenteDestino());
        }

        LocalDateTime agora = LocalDateTime.now();
        ConversaTransferencia transferencia = new ConversaTransferencia();
        transferencia.setConversaId(conversa.getId());
        transferencia.setDepartamentoUnidadeOrigemId(conversa.getDepartamentoUnidadeId());
        transferencia.setDepartamentoUnidadeDestinoId(destino.getId());
        transferencia.setAtendenteOrigemCodgUsuario(conversa.getAtendenteResponsavelCodgUsuario());
        transferencia.setAtendenteDestinoCodgUsuario(request.getCodgAtendenteDestino());
        transferencia.setMotivo(request.getMotivo());
        transferencia.setStatus(StatusTransferencia.ACEITA);
        transferencia.setRespondidoEm(agora);
        manager.post("chat-confianca/persistencia/conversa-transferencias", transferencia, ConversaTransferencia.class);

        conversa.setDepartamentoUnidadeId(destino.getId());
        if (destino.getCodgUnidade() != null) {
            conversa.setCodgUnidade(destino.getCodgUnidade());
        }
        conversa.setAtendenteResponsavelCodgUsuario(request.getCodgAtendenteDestino());
        conversa.setStatus(request.getCodgAtendenteDestino() == null
                ? StatusConversa.TRANSFERIDA
                : StatusConversa.EM_ATENDIMENTO);
        conversa.setUltimoEventoEm(agora);
        conversa = manager.post("chat-confianca/persistencia/conversas", conversa, Conversa.class);

        FilaAtendimento fila = buscarFilaPorConversa(conversa.getId());
        if (request.getCodgAtendenteDestino() == null) {
            if (fila == null) {
                fila = new FilaAtendimento();
                fila.setConversaId(conversa.getId());
            }
            fila.setDepartamentoUnidadeId(destino.getId());
            fila.setCodgUnidade(conversa.getCodgUnidade());
            fila.setCodgAgencia(conversa.getCodgAgencia());
            fila.setSolicitanteCodgUsuario(conversa.getSolicitanteCodgUsuario());
            fila.setStatus(StatusFila.AGUARDANDO);
            fila.setPrioridade(conversa.getPrioridade());
            fila.setPosicao(calcularProximaPosicao(destino.getId()));
            fila.setAtendenteDestinoCodgUsuario(null);
            fila.setChamadoEm(null);
            fila.setSaiuEm(null);
            fila.setMotivoSaida(null);
            fila = manager.post("chat-confianca/persistencia/filas", fila, FilaAtendimento.class);
        } else {
            adicionarParticipanteSeNecessario(conversa.getId(), request.getCodgAtendenteDestino());
            if (fila != null) {
                fila.setDepartamentoUnidadeId(destino.getId());
                fila.setStatus(StatusFila.EM_ATENDIMENTO);
                fila.setAtendenteDestinoCodgUsuario(request.getCodgAtendenteDestino());
                fila.setChamadoEm(agora);
                fila.setSaiuEm(agora);
                fila.setMotivoSaida("Transferido diretamente");
                manager.post("chat-confianca/persistencia/filas", fila, FilaAtendimento.class);
            }
        }

        String destinoNome = isBlank(destino.getNomeExibicao()) ? "novo departamento" : destino.getNomeExibicao();
        String descricaoTransferencia = "Atendimento transferido para " + destinoNome + ". Motivo: "
                + request.getMotivo().trim();
        persistirMensagem(conversa, request.getCodgUsuario(), descricaoTransferencia, false,
                TipoMensagem.TRANSFERENCIA, null);
        registrarEvento(conversa.getId(), "CONVERSA_TRANSFERIDA", request.getCodgUsuario(), descricaoTransferencia);

        atualizarCargaAtendente(transferencia.getAtendenteOrigemCodgUsuario());
        atualizarCargaAtendente(request.getCodgAtendenteDestino());
        if (request.getCodgAtendenteDestino() == null) {
            conversa = distribuirAutomaticamenteSePossivel(conversa, fila, destino, agora);
        }

        return conversa;
    }

    public SlaConversaResumo calcularSlaConversa(Long conversaId, Integer codgUsuario, boolean gestor) {
        validarObrigatorio(conversaId, "Informe a conversa.");
        validarObrigatorio(codgUsuario, "Informe o usuario.");
        Conversa conversa = buscarConversaOuFalhar(conversaId);
        validarAcessoConversa(conversa, codgUsuario, gestor, "Usuario nao participa da conversa.");
        return montarSla(conversa, LocalDateTime.now());
    }

    public DashboardAtendimentoResumo dashboardUnidade(Integer codgUnidade, Integer codgUsuario) {
        validarObrigatorio(codgUnidade, "Informe a unidade.");
        validarObrigatorio(codgUsuario, "Informe o usuario.");
        if (!ehGestorOuAdmin(codgUsuario, codgUnidade)) {
            throw regra(403, "Acesso restrito a gestores do chat.");
        }

        List<VwConversaResumo> historico = listarHistoricoUnidade(codgUnidade);
        DashboardAtendimentoResumo dashboard = new DashboardAtendimentoResumo();
        dashboard.setCodgUnidade(codgUnidade);
        dashboard.setAtualizadoEm(LocalDateTime.now());
        dashboard.setTotalConversas(0L);
        dashboard.setAbertas(0L);
        dashboard.setAguardandoAtendente(0L);
        dashboard.setEmAtendimento(0L);
        dashboard.setAguardandoCliente(0L);
        dashboard.setTransferidas(0L);
        dashboard.setEncerradas(0L);
        dashboard.setUrgentes(0L);
        dashboard.setSlaAlerta(0L);
        dashboard.setSlaViolado(0L);
        dashboard.setTotalMensagens(0L);

        Map<String, DashboardGrupoResumo> porDepartamento = new LinkedHashMap<>();
        Map<String, DashboardGrupoResumo> porAtendente = new LinkedHashMap<>();
        LocalDateTime agora = LocalDateTime.now();

        for (VwConversaResumo item : historico) {
            dashboard.setTotalConversas(dashboard.getTotalConversas() + 1);
            dashboard.setTotalMensagens(dashboard.getTotalMensagens() + (item.getTotalMensagens() == null ? 0 : item.getTotalMensagens()));
            if (item.getPrioridade() == PrioridadeConversa.URGENTE) {
                dashboard.setUrgentes(dashboard.getUrgentes() + 1);
            }
            if (item.getStatus() == StatusConversa.AGUARDANDO_ATENDENTE) {
                dashboard.setAguardandoAtendente(dashboard.getAguardandoAtendente() + 1);
            } else if (item.getStatus() == StatusConversa.EM_ATENDIMENTO) {
                dashboard.setEmAtendimento(dashboard.getEmAtendimento() + 1);
            } else if (item.getStatus() == StatusConversa.AGUARDANDO_SOLICITANTE) {
                dashboard.setAguardandoCliente(dashboard.getAguardandoCliente() + 1);
            } else if (item.getStatus() == StatusConversa.TRANSFERIDA) {
                dashboard.setTransferidas(dashboard.getTransferidas() + 1);
            } else if (item.getStatus() == StatusConversa.ENCERRADA
                    || item.getStatus() == StatusConversa.RESOLVIDA
                    || item.getStatus() == StatusConversa.CANCELADA) {
                dashboard.setEncerradas(dashboard.getEncerradas() + 1);
            }

            boolean aberta = conversaAberta(item.getStatus());
            boolean slaViolado = false;
            boolean slaAlerta = false;
            if (aberta) {
                dashboard.setAbertas(dashboard.getAbertas() + 1);
                Conversa conversa = manager.get("chat-confianca/consultas/conversas/" + item.getId(), Conversa.class);
                if (conversa != null) {
                    SlaConversaResumo sla = montarSla(conversa, agora);
                    slaViolado = Boolean.TRUE.equals(sla.getResolucaoViolada()) || Boolean.TRUE.equals(sla.getPrimeiraRespostaViolada());
                    slaAlerta = !slaViolado && Boolean.TRUE.equals(sla.getEmAlerta());
                    if (slaViolado) {
                        dashboard.setSlaViolado(dashboard.getSlaViolado() + 1);
                    } else if (slaAlerta) {
                        dashboard.setSlaAlerta(dashboard.getSlaAlerta() + 1);
                    }
                }
            }

            atualizarGrupo(porDepartamento, item.getDepartamentoNome(), item, aberta, slaAlerta, slaViolado);
            atualizarGrupo(porAtendente, item.getAtendenteNome(), item, aberta, slaAlerta, slaViolado);
        }

        dashboard.setPorDepartamento(new ArrayList<>(porDepartamento.values()));
        dashboard.setPorAtendente(new ArrayList<>(porAtendente.values()));
        preencherIndicadoresConfia(dashboard, codgUnidade);
        return dashboard;
    }

    private void preencherIndicadoresConfia(DashboardAtendimentoResumo dashboard, Integer codgUnidade) {
        inicializarIndicadoresConfia(dashboard);
        try {
            List<Conversa> conversas = manager.getList(
                    "chat-confianca/persistencia/conversas",
                    new ParameterizedTypeReference<List<Conversa>>() {
                    }
            );
            if (conversas == null || conversas.isEmpty()) {
                return;
            }

            List<Conversa> conversasConfia = conversas.stream()
                    .filter(Objects::nonNull)
                    .filter(item -> Objects.equals(item.getCodgUnidade(), codgUnidade))
                    .filter(item -> item.getId() != null)
                    .filter(item -> metadadosOrigemConfia(item.getMetadadosJson()))
                    .collect(Collectors.toList());

            dashboard.setConfiaTotal((long) conversasConfia.size());
            if (conversasConfia.isEmpty()) {
                return;
            }

            Set<Long> idsConfia = conversasConfia.stream()
                    .map(Conversa::getId)
                    .collect(Collectors.toSet());
            Set<Long> idsResolvidasIa = new HashSet<>();

            for (Conversa conversa : conversasConfia) {
                if (conversaResolvidaPelaConfia(conversa)) {
                    dashboard.setConfiaResolvidas(dashboard.getConfiaResolvidas() + 1);
                    idsResolvidasIa.add(conversa.getId());
                    if (conversaAutoEncerradaPelaConfia(conversa)) {
                        dashboard.setConfiaAutoEncerradas(dashboard.getConfiaAutoEncerradas() + 1);
                    }
                } else if (conversaConfiaEncaminhadaHumano(conversa)) {
                    dashboard.setConfiaEncaminhadasHumano(dashboard.getConfiaEncaminhadasHumano() + 1);
                } else if (conversaConfiaEmAndamento(conversa)) {
                    dashboard.setConfiaEmAndamento(dashboard.getConfiaEmAndamento() + 1);
                }
            }

            preencherNotaMediaConfia(dashboard, idsResolvidasIa);
            dashboard.setConfiaMotivosEncaminhamento(montarMotivosEncaminhamentoConfia(idsConfia));
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "Nao foi possivel calcular indicadores da ConfIA.", ex);
            inicializarIndicadoresConfia(dashboard);
        }
    }

    private void inicializarIndicadoresConfia(DashboardAtendimentoResumo dashboard) {
        dashboard.setConfiaTotal(0L);
        dashboard.setConfiaResolvidas(0L);
        dashboard.setConfiaAutoEncerradas(0L);
        dashboard.setConfiaEncaminhadasHumano(0L);
        dashboard.setConfiaEmAndamento(0L);
        dashboard.setConfiaAvaliacoes(0L);
        dashboard.setConfiaNotaMedia(null);
        dashboard.setConfiaMotivosEncaminhamento(new ArrayList<>());
    }

    private boolean conversaResolvidaPelaConfia(Conversa conversa) {
        if (conversa == null || conversa.getStatus() != StatusConversa.ENCERRADA) {
            return false;
        }
        String motivo = conversa.getMotivoEncerramento();
        return !isBlank(motivo) && motivo.trim().startsWith("RESOLVIDA_IA");
    }

    private boolean conversaAutoEncerradaPelaConfia(Conversa conversa) {
        String motivo = conversa == null ? null : conversa.getMotivoEncerramento();
        return !isBlank(motivo) && motivo.trim().startsWith("RESOLVIDA_IA_AUTO");
    }

    private boolean conversaConfiaEncaminhadaHumano(Conversa conversa) {
        if (conversa == null || conversaResolvidaPelaConfia(conversa)) {
            return false;
        }
        if (conversa.getAtendenteResponsavelCodgUsuario() != null) {
            return true;
        }
        return conversa.getStatus() == StatusConversa.AGUARDANDO_ATENDENTE
                || conversa.getStatus() == StatusConversa.EM_ATENDIMENTO
                || conversa.getStatus() == StatusConversa.TRANSFERIDA
                || conversa.getStatus() == StatusConversa.RESOLVIDA
                || conversa.getStatus() == StatusConversa.ENCERRADA
                || conversa.getStatus() == StatusConversa.CANCELADA;
    }

    private boolean conversaConfiaEmAndamento(Conversa conversa) {
        return conversa != null
                && conversa.getAtendenteResponsavelCodgUsuario() == null
                && (conversa.getStatus() == StatusConversa.NOVA
                || conversa.getStatus() == StatusConversa.AGUARDANDO_SOLICITANTE);
    }

    private void preencherNotaMediaConfia(DashboardAtendimentoResumo dashboard, Set<Long> idsResolvidasIa) {
        if (idsResolvidasIa == null || idsResolvidasIa.isEmpty()) {
            return;
        }
        List<AtendimentoAvaliacao> avaliacoes = manager.getList(
                "chat-confianca/persistencia/atendimento-avaliacoes",
                new ParameterizedTypeReference<List<AtendimentoAvaliacao>>() {
                }
        );
        if (avaliacoes == null || avaliacoes.isEmpty()) {
            return;
        }
        List<Integer> notas = avaliacoes.stream()
                .filter(Objects::nonNull)
                .filter(item -> idsResolvidasIa.contains(item.getConversaId()))
                .map(AtendimentoAvaliacao::getNota)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        dashboard.setConfiaAvaliacoes((long) notas.size());
        if (!notas.isEmpty()) {
            double media = notas.stream().mapToInt(Integer::intValue).average().orElse(0D);
            dashboard.setConfiaNotaMedia(media);
        }
    }

    private List<DashboardGrupoResumo> montarMotivosEncaminhamentoConfia(Set<Long> idsConfia) {
        if (idsConfia == null || idsConfia.isEmpty()) {
            return new ArrayList<>();
        }
        List<ConversaEvento> eventos = manager.getList(
                "chat-confianca/persistencia/conversa-eventos",
                new ParameterizedTypeReference<List<ConversaEvento>>() {
                }
        );
        if (eventos == null || eventos.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, Long> motivos = new LinkedHashMap<>();
        eventos.stream()
                .filter(Objects::nonNull)
                .filter(item -> idsConfia.contains(item.getConversaId()))
                .filter(item -> "ATENDIMENTO_HUMANO_SOLICITADO".equalsIgnoreCase(item.getTipoEvento()))
                .map(this::motivoEncaminhamentoConfia)
                .forEach(motivo -> motivos.merge(motivo, 1L, Long::sum));

        return motivos.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    DashboardGrupoResumo grupo = new DashboardGrupoResumo();
                    grupo.setNome(entry.getKey());
                    grupo.setTotal(entry.getValue());
                    grupo.setAbertos(0L);
                    grupo.setUrgentes(0L);
                    grupo.setSlaAlerta(0L);
                    grupo.setSlaViolado(0L);
                    return grupo;
                })
                .collect(Collectors.toList());
    }

    private String motivoEncaminhamentoConfia(ConversaEvento evento) {
        String descricao = evento == null ? null : evento.getDescricao();
        if (isBlank(descricao)) {
            return "Solicitacao de atendimento humano";
        }
        String motivo = limparTextoResumo(descricao);
        return limitarTextoResumo(motivo, 120);
    }

    private String normalizarComentarioAvaliacao(String comentario) {
        if (comentario == null) {
            return null;
        }
        String normalizado = comentario.trim();
        return normalizado.length() > 1000 ? normalizado.substring(0, 1000) : normalizado;
    }

    private void validarAcessoConversa(Conversa conversa, Integer codgUsuario, boolean gestor, String mensagem) {
        boolean acessoGestor = gestor && ehGestorOuAdmin(codgUsuario, conversa.getCodgUnidade());
        if (!acessoGestor && !usuarioParticipa(conversa.getId(), codgUsuario)) {
            throw regra(403, mensagem);
        }
    }

    private SessaoChatResponse validarAcessoAtendimento(Integer codgUsuario) {
        validarObrigatorio(codgUsuario, "Informe o usuario.");
        SessaoChatResponse sessao = montarSessao(codgUsuario);
        if (sessao == null || (!sessao.isAtendente() && !sessao.isGestor() && !sessao.isAdmin())) {
            throw regra(403, "Acesso restrito a equipe de atendimento do chat.");
        }
        return sessao;
    }

    private void validarAcessoProprioAtendente(Integer codgUsuarioSolicitante,
                                               Integer codgAtendente) {
        validarObrigatorio(codgAtendente, "Informe o atendente.");
        if (!Objects.equals(codgUsuarioSolicitante, codgAtendente)) {
            throw regra(403, "O atendente somente pode alterar o proprio status.");
        }
        validarAcessoAtendimento(codgUsuarioSolicitante);
    }

    private Integer resolverUnidadeOperacional(SessaoChatResponse sessao,
                                               Integer codgUsuario,
                                               Integer codgUnidadeSolicitada) {
        if (sessao.isAdmin()) {
            return codgUnidadeSolicitada;
        }
        if (sessao.isGestor()) {
            Integer unidadeGestor = unidadeGestaoSessao(sessao);
            if (codgUnidadeSolicitada != null
                    && !Objects.equals(codgUnidadeSolicitada, unidadeGestor)) {
                throw regra(403, "Operacao restrita a unidade do gestor.");
            }
            return unidadeGestor;
        }

        Integer unidadeSessao = unidadeSessao(sessao);
        Integer unidade = codgUnidadeSolicitada == null ? unidadeSessao : codgUnidadeSolicitada;
        if (unidade == null) {
            throw regra(403, "Atendente sem unidade operacional para o chat.");
        }
        if (!Objects.equals(unidade, unidadeSessao)
                && !atendentePossuiVinculoNaUnidade(codgUsuario, unidade)) {
            throw regra(403, "Atendente nao possui vinculo com a unidade informada.");
        }
        return unidade;
    }

    private boolean atendentePossuiVinculoNaUnidade(Integer codgUsuario, Integer codgUnidade) {
        Set<Long> idsUnidade = idsDepartamentoUnidade(codgUnidade);
        return listarDepartamentosAtendenteSeguro(codgUsuario).stream()
                .filter(Objects::nonNull)
                .filter(item -> !Boolean.FALSE.equals(item.getAtivo()))
                .map(DepartamentoAtendente::getDepartamentoUnidadeId)
                .anyMatch(idsUnidade::contains);
    }

    private boolean departamentoDisponivelNaUnidade(Long departamentoId, Integer codgUnidade) {
        return configService.listarDepartamentoUnidadesPorUnidade(codgUnidade).stream()
                .filter(Objects::nonNull)
                .filter(item -> !Boolean.FALSE.equals(item.getAtivo()))
                .anyMatch(item -> Objects.equals(item.getDepartamentoId(), departamentoId));
    }

    private boolean adminGlobalChat(SessaoChatResponse sessao) {
        return sessao != null && sessao.isAdmin();
    }

    private Integer unidadeSessao(SessaoChatResponse sessao) {
        if (sessao == null) {
            return null;
        }
        if (sessao.getUnidade() != null && sessao.getUnidade().getCodgUnidade() != null) {
            return sessao.getUnidade().getCodgUnidade();
        }
        return sessao.getUsuario() == null ? null : sessao.getUsuario().getCodgUnidade();
    }

    private Integer unidadeGestaoSessao(SessaoChatResponse sessao) {
        if (sessao == null || sessao.getUsuario() == null
                || sessao.getUsuario().getCodgAgencia() != null
                || sessao.getUsuario().getCodgUnidade() == null) {
            throw regra(403, "Gestao do chat restrita a usuario interno com unidade vinculada.");
        }
        return sessao.getUsuario().getCodgUnidade();
    }

    private void adicionarParametro(List<String> params, String nome, Object valor) {
        if (valor == null) {
            return;
        }
        String texto = String.valueOf(valor);
        if (texto.isBlank()) {
            return;
        }
        params.add(nome + "=" + URLEncoder.encode(texto, StandardCharsets.UTF_8));
    }

    private String normalizarTag(String nome) {
        if (nome == null) {
            return null;
        }
        String normalizado = nome.trim();
        return normalizado.length() > 80 ? normalizado.substring(0, 80) : normalizado;
    }

    private String normalizarCorTag(String corHex) {
        if (isBlank(corHex)) {
            return "#4d6fd8";
        }
        String cor = corHex.trim();
        if (!cor.startsWith("#")) {
            cor = "#" + cor;
        }
        return cor.matches("#[0-9a-fA-F]{6}") ? cor : "#4d6fd8";
    }

    private SlaConversaResumo montarSla(Conversa conversa, LocalDateTime agora) {
        SlaPolitica politica = buscarPoliticaSla(conversa);
        int primeiraResposta = politica != null && politica.getPrimeiraRespostaMinutos() != null
                ? politica.getPrimeiraRespostaMinutos()
                : primeiraRespostaPadrao(conversa.getPrioridade());
        int resolucao = politica != null && politica.getResolucaoMinutos() != null
                ? politica.getResolucaoMinutos()
                : resolucaoPadrao(conversa.getPrioridade());
        int alerta = politica != null && politica.getAlertaAntesMinutos() != null
                ? politica.getAlertaAntesMinutos()
                : Math.max(5, resolucao / 5);

        LocalDateTime inicio = conversa.getCriadoEm() == null ? conversa.getUltimoEventoEm() : conversa.getCriadoEm();
        if (inicio == null) {
            inicio = agora;
        }
        LocalDateTime fimResolucao = conversa.getEncerradoEm() == null ? agora : conversa.getEncerradoEm();
        LocalDateTime fimPrimeiraResposta = conversa.getPrimeiraRespostaEm() == null ? agora : conversa.getPrimeiraRespostaEm();

        int minutosPrimeira = minutosEntre(inicio, fimPrimeiraResposta);
        int minutosResolucao = minutosEntre(inicio, fimResolucao);
        int restantes = resolucao - minutosResolucao;
        boolean primeiraPendente = conversa.getPrimeiraRespostaEm() == null && conversaAberta(conversa.getStatus());
        boolean primeiraViolada = minutosPrimeira > primeiraResposta && (primeiraPendente || conversa.getPrimeiraRespostaEm() != null);
        boolean resolucaoViolada = minutosResolucao > resolucao;
        boolean emAlerta = !resolucaoViolada && conversaAberta(conversa.getStatus()) && restantes <= alerta;

        SlaConversaResumo resumo = new SlaConversaResumo();
        resumo.setConversaId(conversa.getId());
        resumo.setPrimeiraRespostaMinutos(primeiraResposta);
        resumo.setResolucaoMinutos(resolucao);
        resumo.setAlertaAntesMinutos(alerta);
        resumo.setMinutosPrimeiraResposta(minutosPrimeira);
        resumo.setMinutosResolucao(minutosResolucao);
        resumo.setMinutosRestantes(restantes);
        resumo.setPercentualResolucao(resolucao <= 0 ? 100 : Math.max(0, Math.min(100, (minutosResolucao * 100) / resolucao)));
        resumo.setPrimeiraRespostaPendente(primeiraPendente);
        resumo.setPrimeiraRespostaViolada(primeiraViolada);
        resumo.setResolucaoViolada(resolucaoViolada);
        resumo.setEmAlerta(emAlerta);
        if (!conversaAberta(conversa.getStatus())) {
            resumo.setStatus("ENCERRADA");
            resumo.setLabel("Encerrada");
        } else if (resolucaoViolada || primeiraViolada) {
            resumo.setStatus("VIOLADO");
            resumo.setLabel("SLA estourado");
        } else if (emAlerta) {
            resumo.setStatus("ALERTA");
            resumo.setLabel("SLA em alerta");
        } else {
            resumo.setStatus("OK");
            resumo.setLabel("SLA no prazo");
        }
        resumo.setDetalhe("Primeira resposta: " + minutosPrimeira + "/" + primeiraResposta
                + " min | Resolucao: " + minutosResolucao + "/" + resolucao + " min");
        return resumo;
    }

    private SlaPolitica buscarPoliticaSla(Conversa conversa) {
        return manager.getList(
                "chat-confianca/persistencia/sla-politicas",
                new ParameterizedTypeReference<List<SlaPolitica>>() {
                }
        ).stream()
                .filter(item -> item != null && Boolean.TRUE.equals(item.getAtivo()))
                .filter(item -> Objects.equals(item.getDepartamentoUnidadeId(), conversa.getDepartamentoUnidadeId()))
                .filter(item -> item.getPrioridade() == conversa.getPrioridade())
                .findFirst()
                .orElse(null);
    }

    private int primeiraRespostaPadrao(PrioridadeConversa prioridade) {
        if (prioridade == PrioridadeConversa.URGENTE) {
            return 5;
        }
        if (prioridade == PrioridadeConversa.ALTA) {
            return 15;
        }
        if (prioridade == PrioridadeConversa.BAIXA) {
            return 60;
        }
        return 30;
    }

    private int resolucaoPadrao(PrioridadeConversa prioridade) {
        if (prioridade == PrioridadeConversa.URGENTE) {
            return 60;
        }
        if (prioridade == PrioridadeConversa.ALTA) {
            return 120;
        }
        if (prioridade == PrioridadeConversa.BAIXA) {
            return 480;
        }
        return 240;
    }

    private int minutosEntre(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null) {
            return 0;
        }
        return (int) Math.max(0, Duration.between(inicio, fim).toMinutes());
    }

    private boolean conversaAberta(StatusConversa status) {
        return status == StatusConversa.NOVA
                || status == StatusConversa.AGUARDANDO_ATENDENTE
                || status == StatusConversa.EM_ATENDIMENTO
                || status == StatusConversa.AGUARDANDO_SOLICITANTE
                || status == StatusConversa.TRANSFERIDA;
    }

    private void atualizarGrupo(Map<String, DashboardGrupoResumo> grupos, String nome, VwConversaResumo item,
                                boolean aberta, boolean slaAlerta, boolean slaViolado) {
        String chave = isBlank(nome) ? "Sem responsavel" : nome.trim();
        DashboardGrupoResumo grupo = grupos.computeIfAbsent(chave, key -> {
            DashboardGrupoResumo novo = new DashboardGrupoResumo();
            novo.setNome(key);
            novo.setTotal(0L);
            novo.setAbertos(0L);
            novo.setUrgentes(0L);
            novo.setSlaAlerta(0L);
            novo.setSlaViolado(0L);
            return novo;
        });
        grupo.setTotal(grupo.getTotal() + 1);
        if (aberta) {
            grupo.setAbertos(grupo.getAbertos() + 1);
        }
        if (item.getPrioridade() == PrioridadeConversa.URGENTE) {
            grupo.setUrgentes(grupo.getUrgentes() + 1);
        }
        if (slaAlerta) {
            grupo.setSlaAlerta(grupo.getSlaAlerta() + 1);
        }
        if (slaViolado) {
            grupo.setSlaViolado(grupo.getSlaViolado() + 1);
        }
    }
    private void preencherAnexos(Long conversaId, List<Mensagem> mensagens) {
        if (mensagens == null || mensagens.isEmpty()) {
            return;
        }
        List<MensagemAnexo> anexos = manager.getList(
                "chat-confianca/consultas/conversas/" + conversaId + "/mensagem-anexos",
                new ParameterizedTypeReference<List<MensagemAnexo>>() {
                }
        );
        if (anexos == null || anexos.isEmpty()) {
            return;
        }
        Map<Long, List<MensagemAnexo>> anexosPorMensagem = anexos.stream()
                .filter(item -> item.getMensagemId() != null)
                .collect(Collectors.groupingBy(MensagemAnexo::getMensagemId));
        mensagens.forEach(mensagem -> mensagem.setAnexos(
                new ArrayList<>(anexosPorMensagem.getOrDefault(mensagem.getId(), new ArrayList<>()))));
    }
    private RefUsuario buscarUsuarioOuFalhar(Integer codgUsuario) {
        RefUsuario usuario = manager.get("chat-confianca/consultas/usuarios/" + codgUsuario, RefUsuario.class);
        if (usuario == null) {
            throw regra(404, "Usuario nao encontrado no chat.");
        }
        return usuario;
    }

    private RefAgencia buscarOuSincronizarAgencia(Integer codgAgencia) {
        RefAgencia agencia = manager.get("chat-confianca/consultas/agencias/" + codgAgencia, RefAgencia.class);
        if (agencia == null
                || !Boolean.TRUE.equals(agencia.getAtivoChat())
                || unidadeReferenciaAusente(agencia.getCodgUnidade())) {
            return configService.sincronizarAgenciaReferencia(codgAgencia);
        }
        return agencia;
    }

    private RefUnidade buscarUnidadeOuFalhar(Integer codgUnidade) {
        RefUnidade unidade = manager.get("chat-confianca/consultas/unidades/" + codgUnidade, RefUnidade.class);
        if (unidade == null) {
            throw regra(404, "Unidade " + codgUnidade + " nao encontrada em ref_unidade para o chat.");
        }
        return unidade;
    }

    private void validarAgenciaAtiva(RefAgencia agencia, Integer codgAgencia, int statusErro) {
        if (agencia == null) {
            throw regra(statusErro, "Agencia " + codgAgencia + " nao encontrada em ref_agencia nem no ConfiancaManager.");
        }
        if (agencia.getCodgUnidade() == null) {
            throw regra(statusErro, "Agencia " + agencia.getCodgAgencia() + " nao possui unidade vinculada para o chat.");
        }
        if (!Boolean.TRUE.equals(agencia.getAtivoChat())) {
            throw regra(statusErro, "Agencia " + agencia.getCodgAgencia()
                    + " inativa para o chat. status=" + agencia.getStatus()
                    + ", ativoChat=" + agencia.getAtivoChat() + ".");
        }
    }

    private void validarUnidadeAtiva(RefUnidade unidade, Integer codgUnidade, int statusErro) {
        if (unidade == null) {
            throw regra(statusErro, "Unidade " + codgUnidade + " nao encontrada em ref_unidade para o chat.");
        }
        if (!Boolean.TRUE.equals(unidade.getAtivoChat())) {
            throw regra(statusErro, "Unidade " + codgUnidade
                    + " inativa para o chat. status=" + unidade.getStatus()
                    + ", ativoChat=" + unidade.getAtivoChat() + ".");
        }
    }

    private boolean unidadeReferenciaAusente(Integer codgUnidade) {
        if (codgUnidade == null) {
            return true;
        }
        RefUnidade unidade = manager.get("chat-confianca/consultas/unidades/" + codgUnidade, RefUnidade.class);
        return unidade == null;
    }
    private Conversa buscarConversaOuFalhar(Long conversaId) {
        validarObrigatorio(conversaId, "Informe a conversa.");
        Conversa conversa = manager.get("chat-confianca/consultas/conversas/" + conversaId, Conversa.class);
        if (conversa == null) {
            throw regra(404, "Conversa nao encontrada.");
        }
        return conversa;
    }

    private Mensagem persistirMensagem(Conversa conversa, Integer remetente, String conteudo, boolean interna) {
        return persistirMensagem(conversa, remetente, conteudo, interna, TipoMensagem.TEXTO, null);
    }

    private Mensagem persistirMensagem(Conversa conversa, Integer remetente, String conteudo, boolean interna,
                                       TipoMensagem tipo, String conteudoJson) {
        return persistirMensagem(conversa, remetente, conteudo, interna, tipo, conteudoJson, RemetenteTipo.USUARIO);
    }

    private Mensagem persistirMensagem(Conversa conversa, Integer remetente, String conteudo, boolean interna,
                                       TipoMensagem tipo, String conteudoJson, RemetenteTipo remetenteTipo) {
        Mensagem mensagem = new Mensagem();
        mensagem.setConversaId(conversa.getId());
        mensagem.setRemetenteCodgUsuario(remetente);
        mensagem.setRemetenteTipo(remetenteTipo == null ? RemetenteTipo.USUARIO : remetenteTipo);
        mensagem.setTipo(tipo == null ? TipoMensagem.TEXTO : tipo);
        mensagem.setVisibilidade(interna ? VisibilidadeMensagem.INTERNA : VisibilidadeMensagem.PUBLICA);
        mensagem.setConteudo(conteudo);
        mensagem.setConteudoJson(conteudoJson);
        mensagem.setStatus(StatusMensagem.ENVIADA);
        mensagem = manager.post("chat-confianca/persistencia/mensagens", mensagem, Mensagem.class);

        if (remetente != null) {
            MensagemLeitura leitura = new MensagemLeitura();
            leitura.setMensagemId(mensagem.getId());
            leitura.setCodgUsuario(remetente);
            leitura.setEntregueEm(LocalDateTime.now());
            leitura.setLidaEm(LocalDateTime.now());
            manager.post("chat-confianca/persistencia/mensagem-leituras", leitura, MensagemLeitura.class);
        }
        return mensagem;
    }

    private MensagemAnexo armazenarAnexo(Conversa conversa, Mensagem mensagem, String nomeOriginal,
                                         String mimeType, byte[] conteudo) {
        try {
            Path pastaConversa = Paths.get(storageRoot(), String.valueOf(conversa.getId())).toAbsolutePath().normalize();
            Files.createDirectories(pastaConversa);
            String nomeArmazenado = mensagem.getId() + "-" + UUID.randomUUID() + "-" + nomeOriginal;
            Path arquivo = pastaConversa.resolve(nomeArmazenado).normalize();
            if (!arquivo.startsWith(pastaConversa)) {
                throw regra(400, "Nome de arquivo invalido.");
            }
            Files.write(arquivo, conteudo, StandardOpenOption.CREATE_NEW);

            MensagemAnexo anexo = new MensagemAnexo();
            anexo.setMensagemId(mensagem.getId());
            anexo.setNomeOriginal(nomeOriginal);
            anexo.setNomeArmazenado(nomeArmazenado);
            anexo.setCaminhoStorage(arquivo.toString());
            anexo.setMimeType(mimeType);
            anexo.setTamanhoBytes((long) conteudo.length);
            anexo.setHashSha256(sha256(conteudo));
            anexo = manager.post("chat-confianca/persistencia/mensagem-anexos", anexo, MensagemAnexo.class);
            anexo.setUrlPublica("v1/chat-confianca/anexos/" + anexo.getId() + "/download");
            return manager.post("chat-confianca/persistencia/mensagem-anexos", anexo, MensagemAnexo.class);
        } catch (RegraDeNegocioException ex) {
            throw ex;
        } catch (Exception ex) {
            throw regra(500, "Nao foi possivel armazenar o anexo.");
        }
    }
    private void registrarEvento(Long conversaId, String tipo, Integer codgUsuario, String descricao) {
        ConversaEvento evento = new ConversaEvento();
        evento.setConversaId(conversaId);
        evento.setTipoEvento(tipo);
        evento.setCodgUsuario(codgUsuario);
        evento.setDescricao(descricao);
        manager.post("chat-confianca/persistencia/conversa-eventos", evento, ConversaEvento.class);
    }

    private Integer calcularProximaPosicao(Long departamentoUnidadeId) {
        long total = listarFilasDepartamento(departamentoUnidadeId).stream()
                .filter(this::filaAberta)
                .count();
        return (int) total + 1;
    }

    private Conversa distribuirAutomaticamenteSePossivel(Conversa conversa, FilaAtendimento fila,
                                                        DepartamentoUnidade departamentoUnidade,
                                                        LocalDateTime agora) {
        if (conversa == null || fila == null || departamentoUnidade == null
                || departamentoUnidade.getDistribuicao() == null
                || departamentoUnidade.getDistribuicao() == DistribuicaoDepartamento.MANUAL) {
            return conversa;
        }
        DepartamentoAtendente candidato = selecionarAtendenteDistribuicao(departamentoUnidade);
        if (candidato == null) {
            registrarEvento(conversa.getId(), "DISTRIBUICAO_AGUARDANDO", conversa.getSolicitanteCodgUsuario(),
                    "Sem atendente online disponivel para distribuicao automatica.");
            return conversa;
        }
        return assumirFila(fila, conversa, candidato.getCodgUsuario(), "Distribuicao automatica", agora);
    }

    private DepartamentoAtendente selecionarAtendenteDistribuicao(DepartamentoUnidade departamentoUnidade) {
        List<DepartamentoAtendente> candidatos = configService.listarAtendentesDepartamento(departamentoUnidade.getId())
                .stream()
                .filter(item -> item.getCodgUsuario() != null)
                .filter(item -> Boolean.TRUE.equals(item.getAtivo()))
                .filter(item -> Boolean.TRUE.equals(item.getRecebeChamados()))
                .filter(item -> atendenteOnline(item.getCodgUsuario()))
                .filter(item -> !limiteAtingido(item.getCodgUsuario(), limiteEfetivo(item, departamentoUnidade)))
                .collect(Collectors.toList());
        if (candidatos.isEmpty()) {
            return null;
        }

        DistribuicaoDepartamento distribuicao = departamentoUnidade.getDistribuicao();
        if (distribuicao == DistribuicaoDepartamento.ROUND_ROBIN) {
            return candidatos.stream()
                    .min(Comparator.comparingInt((DepartamentoAtendente item) -> prioridadeDistribuicao(item))
                            .thenComparing((DepartamentoAtendente item) -> ultimaAtividadeAtendente(item.getCodgUsuario()),
                                    Comparator.nullsFirst(Comparator.naturalOrder()))
                            .thenComparing(DepartamentoAtendente::getCodgUsuario, Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElse(null);
        }

        return candidatos.stream()
                .min(Comparator.comparingLong((DepartamentoAtendente item) -> contarAtendimentosAtivos(item.getCodgUsuario()))
                        .thenComparingInt((DepartamentoAtendente item) -> prioridadeDistribuicao(item))
                        .thenComparing(DepartamentoAtendente::getCodgUsuario, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private Conversa assumirFila(FilaAtendimento fila, Conversa conversa, Integer codgAtendente,
                                String motivoSaida, LocalDateTime agora) {
        fila.setStatus(StatusFila.EM_ATENDIMENTO);
        fila.setAtendenteDestinoCodgUsuario(codgAtendente);
        fila.setChamadoEm(agora);
        fila.setSaiuEm(agora);
        fila.setMotivoSaida(motivoSaida);
        manager.post("chat-confianca/persistencia/filas", fila, FilaAtendimento.class);

        conversa.setAtendenteResponsavelCodgUsuario(codgAtendente);
        conversa.setStatus(StatusConversa.EM_ATENDIMENTO);
        conversa.setUltimoEventoEm(agora);
        if (conversa.getIniciadoEm() == null) {
            conversa.setIniciadoEm(agora);
        }
        if (conversa.getPrimeiraRespostaEm() == null) {
            conversa.setPrimeiraRespostaEm(agora);
        }
        conversa = manager.post("chat-confianca/persistencia/conversas", conversa, Conversa.class);

        adicionarParticipanteSeNecessario(conversa.getId(), codgAtendente);
        registrarEvento(conversa.getId(), "ATENDIMENTO_ASSUMIDO", codgAtendente, motivoSaida + ".");
        atualizarCargaAtendente(codgAtendente);
        return conversa;
    }

    private boolean atendenteOnline(Integer codgAtendente) {
        AtendenteStatus status = buscarStatusAtendenteSeguro(codgAtendente);
        return status.getStatus() == null || status.getStatus() == StatusAtendente.ONLINE;
    }

    private void validarStatusAtendenteDisponivel(Integer codgAtendente) {
        if (!atendenteOnline(codgAtendente)) {
            AtendenteStatus status = buscarStatusAtendenteSeguro(codgAtendente);
            throw regra(409, "Atendente esta " + statusAtendenteLabel(status.getStatus())
                    + " e nao pode receber novos atendimentos.");
        }
    }

    private AtendenteStatus buscarStatusAtendenteSeguro(Integer codgAtendente) {
        try {
            return configService.buscarAtendenteStatus(codgAtendente);
        } catch (RuntimeException ex) {
            AtendenteStatus status = new AtendenteStatus();
            status.setCodgUsuario(codgAtendente);
            status.setStatus(StatusAtendente.ONLINE);
            status.setAtendimentosAtivos(0);
            return status;
        }
    }

    private void atualizarCargaAtendente(Integer codgAtendente) {
        if (codgAtendente == null) {
            return;
        }
        AtendenteStatus status = buscarStatusAtendenteSeguro(codgAtendente);
        status.setCodgUsuario(codgAtendente);
        status.setAtendimentosAtivos((int) Math.min(Integer.MAX_VALUE, contarAtendimentosAtivos(codgAtendente)));
        configService.salvarAtendenteStatus(status);
    }

    private long contarAtendimentosAtivos(Integer codgAtendente) {
        return listarHistoricoAtendente(codgAtendente).stream()
                .filter(item -> item.getStatus() == StatusConversa.EM_ATENDIMENTO
                        || item.getStatus() == StatusConversa.AGUARDANDO_SOLICITANTE)
                .count();
    }

    private Integer limiteEfetivo(DepartamentoAtendente vinculo, DepartamentoUnidade departamentoUnidade) {
        if (vinculo != null && vinculo.getLimiteChatsSimultaneos() != null
                && vinculo.getLimiteChatsSimultaneos() > 0) {
            return vinculo.getLimiteChatsSimultaneos();
        }
        if (departamentoUnidade != null && departamentoUnidade.getLimiteChatsPorAtendente() != null
                && departamentoUnidade.getLimiteChatsPorAtendente() > 0) {
            return departamentoUnidade.getLimiteChatsPorAtendente();
        }
        return null;
    }

    private boolean limiteAtingido(Integer codgAtendente, Integer limite) {
        return limite != null && limite > 0 && contarAtendimentosAtivos(codgAtendente) >= limite;
    }

    private int prioridadeDistribuicao(DepartamentoAtendente atendente) {
        return atendente == null || atendente.getPrioridadeDistribuicao() == null
                ? 1
                : atendente.getPrioridadeDistribuicao();
    }

    private LocalDateTime ultimaAtividadeAtendente(Integer codgAtendente) {
        return buscarStatusAtendenteSeguro(codgAtendente).getUltimaAtividadeEm();
    }
    private List<FilaAtendimento> listarFilasDepartamento(Long departamentoUnidadeId) {
        return manager.getList(
                "chat-confianca/consultas/departamento-unidades/" + departamentoUnidadeId + "/filas",
                new ParameterizedTypeReference<List<FilaAtendimento>>() {
                }
        );
    }

    private List<DepartamentoAtendente> listarDepartamentosAtendente(Integer codgUsuario) {
        return manager.getList(
                "chat-confianca/consultas/atendentes/" + codgUsuario + "/departamentos",
                new ParameterizedTypeReference<List<DepartamentoAtendente>>() {
                }
        );
    }

    private DepartamentoAtendente buscarVinculo(Long departamentoUnidadeId, Integer codgAtendente) {
        return listarDepartamentosAtendente(codgAtendente).stream()
                .filter(item -> Objects.equals(item.getDepartamentoUnidadeId(), departamentoUnidadeId))
                .filter(item -> Boolean.TRUE.equals(item.getAtivo()))
                .filter(item -> Boolean.TRUE.equals(item.getRecebeChamados()))
                .findFirst()
                .orElse(null);
    }

    private DepartamentoAtendente buscarVinculoObrigatorio(Long departamentoUnidadeId, Integer codgAtendente) {
        DepartamentoAtendente vinculo = buscarVinculo(departamentoUnidadeId, codgAtendente);
        if (vinculo == null) {
            throw regra(403, "Atendente nao esta vinculado ao departamento/unidade de destino.");
        }
        return vinculo;
    }

    private void validarLimite(Integer codgAtendente, DepartamentoAtendente vinculo) {
        Integer limite = vinculo == null ? null : vinculo.getLimiteChatsSimultaneos();
        if (limite == null || limite <= 0) {
            return;
        }
        if (limiteAtingido(codgAtendente, limite)) {
            throw regra(409, "Limite de chats simultaneos atingido.");
        }
    }

    private void adicionarParticipanteSeNecessario(Long conversaId, Integer codgAtendente) {
        if (usuarioParticipa(conversaId, codgAtendente)) {
            return;
        }

        ConversaParticipante participante = new ConversaParticipante();
        participante.setConversaId(conversaId);
        participante.setCodgUsuario(codgAtendente);
        participante.setPapel(PapelParticipante.ATENDENTE);
        participante.setAtivo(true);
        participante.setSilenciado(false);
        manager.post("chat-confianca/persistencia/conversa-participantes", participante, ConversaParticipante.class);
    }

    private boolean usuarioParticipa(Long conversaId, Integer codgUsuario) {
        Boolean participa = manager.get(
                "chat-confianca/consultas/conversas/" + conversaId + "/participantes/" + codgUsuario + "/exists",
                Boolean.class
        );
        return Boolean.TRUE.equals(participa);
    }

    private FilaAtendimento buscarFilaPorConversa(Long conversaId) {
        return manager.get("chat-confianca/consultas/conversas/" + conversaId + "/fila", FilaAtendimento.class);
    }

    private List<VwFilaAtendimento> listarViewsFilaAbertas() {
        List<VwFilaAtendimento> result = new ArrayList<>();
        result.addAll(listarViewsFilaPorStatus(StatusFila.AGUARDANDO));
        result.addAll(listarViewsFilaPorStatus(StatusFila.CHAMANDO));

        Set<Long> ids = new HashSet<>();
        return result.stream()
                .filter(item -> item.getId() == null || ids.add(item.getId()))
                .collect(Collectors.toList());
    }

    private List<VwFilaAtendimento> listarViewsFilaPorStatus(StatusFila status) {
        return manager.getList(
                "chat-confianca/consultas/fila/status/" + status.name(),
                new ParameterizedTypeReference<List<VwFilaAtendimento>>() {
                }
        );
    }

    private List<VwFilaAtendimento> ordenarFila(List<VwFilaAtendimento> fila) {
        return fila.stream()
                .sorted(Comparator.comparingInt((VwFilaAtendimento item) -> prioridadeOrdem(item.getPrioridade()))
                        .reversed()
                        .thenComparing(VwFilaAtendimento::getEntrouEm, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    private boolean filaAberta(FilaAtendimento fila) {
        return fila != null && (fila.getStatus() == StatusFila.AGUARDANDO || fila.getStatus() == StatusFila.CHAMANDO);
    }

    private boolean aceitaMensagem(StatusConversa status) {
        return status == StatusConversa.NOVA
                || status == StatusConversa.AGUARDANDO_ATENDENTE
                || status == StatusConversa.TRANSFERIDA
                || status == StatusConversa.EM_ATENDIMENTO
                || status == StatusConversa.AGUARDANDO_SOLICITANTE;
    }

    private boolean elegivelAutoEncerramentoConfia(Conversa conversa, LocalDateTime limite) {
        if (conversa == null || conversa.getId() == null || conversa.getSolicitanteCodgUsuario() == null) {
            return false;
        }
        if (conversa.getAtendenteResponsavelCodgUsuario() != null) {
            return false;
        }
        if (conversa.getStatus() != StatusConversa.NOVA
                && conversa.getStatus() != StatusConversa.AGUARDANDO_SOLICITANTE) {
            return false;
        }
        if (!metadadosOrigemConfia(conversa.getMetadadosJson())) {
            return false;
        }
        LocalDateTime referencia = conversa.getUltimoEventoEm();
        if (referencia == null) {
            referencia = conversa.getAtualizadoEm() == null ? conversa.getCriadoEm() : conversa.getAtualizadoEm();
        }
        return referencia != null && referencia.isBefore(limite);
    }

    private boolean metadadosOrigemConfia(String metadadosJson) {
        return !isBlank(metadadosJson)
                && metadadosJson.toUpperCase(Locale.ROOT).contains("CONFIA");
    }

    private String montarMotivoEncerramento(EncerrarConversaRequest request) {
        String motivo = request.getMotivo().trim();
        if (isBlank(request.getCategoria())) {
            return motivo;
        }
        return request.getCategoria().trim() + " - " + motivo;
    }

    private void validarTextoObrigatorio(String valor, String mensagem) {
        if (isBlank(valor)) {
            throw regra(400, mensagem);
        }
    }

    private boolean ehGestorOuAdmin(Integer codgUsuario, Integer codgUnidade) {
        SessaoChatResponse sessao = montarSessao(codgUsuario);
        if (sessao.isAdmin()) {
            return true;
        }
        RefUsuario usuario = sessao.getUsuario();
        if (usuario == null || usuario.getCodgAgencia() != null
                || usuario.getCodgUnidade() == null
                || codgUnidade == null
                || !Objects.equals(usuario.getCodgUnidade(), codgUnidade)) {
            return false;
        }
        List<String> perfis = new ArrayList<>(listarPerfis(codgUsuario, codgUnidade));
        List<DepartamentoAtendente> vinculosAtendente = listarDepartamentosAtendenteSeguro(codgUsuario);
        adicionarPerfisPorVinculo(perfis, vinculosAtendente, codgUnidade);
        return temPerfil(perfis, "GESTOR", "GESTOR_UNIDADE", "SUPERVISOR");
    }

    private List<String> listarPerfis(Integer codgUsuario, Integer codgUnidade) {
        String path = "chat-confianca/consultas/usuarios/" + codgUsuario + "/perfis";
        if (codgUnidade != null) {
            path += "?codgUnidade=" + codgUnidade;
        }
        List<String> perfis = manager.getList(path, new ParameterizedTypeReference<List<String>>() {
        });
        return perfis == null ? new ArrayList<>() : perfis;
    }

    private List<DepartamentoAtendente> listarDepartamentosAtendenteSeguro(Integer codgUsuario) {
        try {
            List<DepartamentoAtendente> vinculos = listarDepartamentosAtendente(codgUsuario);
            return vinculos == null ? new ArrayList<>() : vinculos;
        } catch (RuntimeException ex) {
            return new ArrayList<>();
        }
    }

    private void adicionarPerfisPorVinculo(List<String> perfis,
                                           List<DepartamentoAtendente> vinculos,
                                           Integer codgUnidade) {
        if (vinculos == null || vinculos.isEmpty()) {
            return;
        }
        Set<Long> vinculosDaUnidade = idsDepartamentoUnidade(codgUnidade);
        vinculos.stream()
                .filter(Objects::nonNull)
                .filter(item -> !Boolean.FALSE.equals(item.getAtivo()))
                .forEach(item -> {
                    adicionarPerfil(perfis, "ATENDENTE");
                    PapelAtendente papel = item.getPapel();
                    if (!vinculosDaUnidade.contains(item.getDepartamentoUnidadeId())) {
                        return;
                    }
                    if (papel == PapelAtendente.GESTOR) {
                        adicionarPerfil(perfis, "GESTOR");
                    } else if (papel == PapelAtendente.SUPERVISOR) {
                        adicionarPerfil(perfis, "SUPERVISOR");
                    }
                });
    }

    private Set<Long> idsDepartamentoUnidade(Integer codgUnidade) {
        if (codgUnidade == null) {
            return Set.of();
        }
        try {
            return configService.listarDepartamentoUnidadesPorUnidade(codgUnidade).stream()
                    .filter(Objects::nonNull)
                    .filter(item -> !Boolean.FALSE.equals(item.getAtivo()))
                    .map(DepartamentoUnidade::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING,
                    "Nao foi possivel validar os vinculos do atendente na unidade " + codgUnidade + ".", ex);
            return Set.of();
        }
    }

    private void adicionarPerfil(List<String> perfis, String perfil) {
        if (perfil == null) {
            return;
        }
        if (perfis.stream()
                .filter(Objects::nonNull)
                .map(item -> item.trim().toUpperCase(Locale.ROOT))
                .noneMatch(perfil::equals)) {
            perfis.add(perfil);
        }
    }

    private boolean temPerfil(List<String> perfis, String... esperados) {
        if (perfis == null || perfis.isEmpty()) {
            return false;
        }
        Set<String> normalized = perfis.stream()
                .filter(Objects::nonNull)
                .map(item -> item.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        for (String esperado : esperados) {
            if (normalized.contains(esperado)) {
                return true;
            }
        }
        return false;
    }

    private String gerarProtocolo(LocalDateTime agora) {
        String sufixo = UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        return "CHAT-" + agora.format(PROTOCOLO_FORMAT) + "-" + sufixo;
    }

    private String normalizarAssunto(String assunto) {
        if (isBlank(assunto)) {
            return "Atendimento";
        }
        return assunto.length() <= 255 ? assunto : assunto.substring(0, 255);
    }

    private int prioridadeOrdem(PrioridadeConversa prioridade) {
        return prioridade == null ? 0 : prioridade.ordinal();
    }

    private byte[] decodificarBase64(String conteudoBase64) {
        try {
            return Base64.getDecoder().decode(conteudoBase64);
        } catch (IllegalArgumentException ex) {
            throw regra(400, "Arquivo invalido.");
        }
    }

    private String normalizarNomeArquivo(String nomeArquivo) {
        String nome = nomeArquivo == null ? "anexo" : nomeArquivo.trim();
        nome = nome.replace("\\", "_").replace("/", "_").replace(":", "_").replace("\"", "_");
        nome = nome.replaceAll("[\\r\\n\\t]", "_");
        if (nome.isBlank()) {
            return "anexo";
        }
        return nome.length() <= 180 ? nome : nome.substring(nome.length() - 180);
    }

    private String normalizarMimeType(String mimeType, String nomeArquivo) {
        if (!isBlank(mimeType)) {
            return mimeType.length() <= 120 ? mimeType : mimeType.substring(0, 120);
        }
        String nome = nomeArquivo == null ? "" : nomeArquivo.toLowerCase(Locale.ROOT);
        if (nome.endsWith(".png")) {
            return "image/png";
        }
        if (nome.endsWith(".jpg") || nome.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (nome.endsWith(".gif")) {
            return "image/gif";
        }
        if (nome.endsWith(".webp")) {
            return "image/webp";
        }
        if (nome.endsWith(".pdf")) {
            return "application/pdf";
        }
        return "application/octet-stream";
    }

    private String storageRoot() {
        String configurado = System.getProperty("chat.confianca.storage");
        if (isBlank(configurado)) {
            configurado = System.getenv("CHAT_CONFIANCA_STORAGE");
        }
        if (isBlank(configurado)) {
            return Paths.get(System.getProperty("java.io.tmpdir"), "chat-confianca", "anexos").toString();
        }
        return configurado;
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
    }
    private void validarObrigatorio(Object valor, String mensagem) {
        if (valor == null) {
            throw regra(400, mensagem);
        }
    }

    private boolean isBlank(String valor) {
        return valor == null || valor.isBlank();
    }

    private String statusAtendenteLabel(StatusAtendente status) {
        if (status == null) {
            return "online";
        }
        switch (status) {
            case OFFLINE:
                return "offline";
            case OCUPADO:
                return "ocupado";
            case AUSENTE:
                return "ausente";
            case INVISIVEL:
                return "invisivel";
            case ONLINE:
            default:
                return "online";
        }
    }

    private RegraDeNegocioException regra(int status, String mensagem) {
        return new RegraDeNegocioException(status, mensagem);
    }
}

