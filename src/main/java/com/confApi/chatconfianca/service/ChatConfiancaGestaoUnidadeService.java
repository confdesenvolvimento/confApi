package com.confApi.chatconfianca.service;

import com.confApi.chatconfianca.dto.enums.PapelAtendente;
import com.confApi.chatconfianca.dto.enums.StatusAtendente;
import com.confApi.chatconfianca.dto.model.AtendenteStatus;
import com.confApi.chatconfianca.dto.model.ChatPerfil;
import com.confApi.chatconfianca.dto.model.ChatUsuarioPerfil;
import com.confApi.chatconfianca.dto.model.Departamento;
import com.confApi.chatconfianca.dto.model.DepartamentoAtendente;
import com.confApi.chatconfianca.dto.model.DepartamentoUnidade;
import com.confApi.chatconfianca.dto.model.RefUnidade;
import com.confApi.chatconfianca.dto.model.RefUsuario;
import com.confApi.chatconfianca.dto.model.RespostaRapida;
import com.confApi.chatconfianca.dto.model.SlaPolitica;
import com.confApi.chatconfianca.dto.request.DepartamentoAtendenteSincronizacaoRequest;
import com.confApi.chatconfianca.dto.request.DepartamentoUnidadeSincronizacaoRequest;
import com.confApi.chatconfianca.dto.request.SlaPoliticaSincronizacaoRequest;
import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.exception.RegraDeNegocioException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ChatConfiancaGestaoUnidadeService {

    private static final String PERFIL_ADMIN_CHAT = "ADMIN_CHAT";
    private static final Set<String> PERFIS_GERENCIAVEIS_UNIDADE = Set.of(
            "ATENDENTE",
            "SUPERVISOR",
            "GESTOR",
            "GESTOR_UNIDADE"
    );
    private static final Set<String> PERFIS_DERIVADOS_ATENDENTE = Set.of(
            "ATENDENTE",
            "SUPERVISOR",
            "GESTOR_UNIDADE"
    );

    private final ChatConfiancaConfigService configService;
    private final ChatConfiancaService chatService;

    public ChatConfiancaGestaoUnidadeService(ChatConfiancaConfigService configService,
                                             ChatConfiancaService chatService) {
        this.configService = configService;
        this.chatService = chatService;
    }

    public List<Departamento> listarDepartamentos(Integer codgUsuarioGestor) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        return configService.listarDepartamentos().stream()
                .filter(item -> isAdminGlobal(unidade)
                || (item.getId() != null && idsDepartamentosUnidade(unidade, true).contains(item.getId())))
                .sorted(Comparator.comparing(Departamento::getNome, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    public Departamento salvarDepartamento(Integer codgUsuarioGestor, Departamento departamento) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        obrigatorio(departamento, "Informe o departamento.");
        if (!isAdminGlobal(unidade)) {
            if (departamento.getId() != null && !departamentoVisivelNaUnidade(departamento.getId(), unidade)) {
                throw regra(403, "Departamento nao pertence a unidade do gestor.");
            }
            if (departamento.getDepartamentoPaiId() != null
                    && !departamentoVisivelNaUnidade(departamento.getDepartamentoPaiId(), unidade)) {
                throw regra(403, "Departamento pai nao pertence a unidade do gestor.");
            }
        }
        return configService.salvarDepartamento(departamento);
    }

    public void excluirDepartamento(Integer codgUsuarioGestor, Long id) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        obrigatorio(id, "Informe o departamento.");
        if (!departamentoVisivelNaUnidade(id, unidade)) {
            throw regra(403, "Departamento nao pertence a unidade do gestor.");
        }
        if (temFilho(id)) {
            throw regra(409, "Departamento possui subdepartamentos. Exclua ou mova os filhos primeiro.");
        }
        boolean usadoEmUnidade = configService.listarDepartamentoUnidades().stream()
                .anyMatch(item -> Objects.equals(item.getDepartamentoId(), id));
        if (isAdminGlobal(unidade) && usadoEmUnidade) {
            throw regra(409, "Departamento esta vinculado a uma ou mais unidades.");
        }
        boolean usadoForaUnidade = !isAdminGlobal(unidade) && configService.listarDepartamentoUnidades().stream()
                .anyMatch(item -> Objects.equals(item.getDepartamentoId(), id)
                && !Objects.equals(item.getCodgUnidade(), unidade));
        if (usadoForaUnidade) {
            throw regra(409, "Departamento tambem esta vinculado a outra unidade.");
        }
        configService.excluirDepartamento(id);
    }

    public List<DepartamentoUnidade> listarDepartamentoUnidades(Integer codgUsuarioGestor) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        return departamentoUnidadesDoEscopo(unidade).stream()
                .sorted(Comparator.comparing(this::nomeDepartamentoUnidade, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    public List<RefUnidade> listarUnidades(Integer codgUsuarioGestor) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        return configService.listarUnidadesReferencia().stream()
                .filter(item -> isAdminGlobal(unidade)
                || Objects.equals(item.getCodgUnidade(), unidade))
                .sorted(Comparator.comparing(
                        RefUnidade::getNomeUnidade,
                        Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    public List<DepartamentoUnidade> sincronizarDepartamentoUnidades(
            Integer codgUsuarioGestor,
            DepartamentoUnidadeSincronizacaoRequest request) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        if (!isAdminGlobal(unidade)) {
            throw regra(403, "Somente o administrador geral pode vincular varias unidades.");
        }
        obrigatorio(request, "Informe os vinculos de unidade.");
        obrigatorio(request.getDepartamentoId(), "Informe o departamento.");
        Departamento departamento = configService.buscarDepartamento(request.getDepartamentoId());
        if (temFilho(departamento.getId())) {
            throw regra(400, "Departamento agrupador nao recebe atendimento.");
        }

        Set<Integer> unidadesValidas = listarUnidades(codgUsuarioGestor).stream()
                .map(RefUnidade::getCodgUnidade)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<Integer> invalidas = request.getCodigosUnidade() == null
                ? new ArrayList<>()
                : request.getCodigosUnidade().stream()
                        .filter(Objects::nonNull)
                        .filter(item -> !unidadesValidas.contains(item))
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());
        if (!invalidas.isEmpty()) {
            throw regra(400, "Unidades fora do escopo de administracao: " + invalidas);
        }

        List<DepartamentoAtendente> equipeOrigem = new ArrayList<>();
        if (Boolean.TRUE.equals(request.getReplicarAtendentes())) {
            obrigatorio(
                    request.getDepartamentoUnidadeOrigemId(),
                    "Selecione a unidade modelo para replicar a equipe.");
            DepartamentoUnidade origem = configService.buscarDepartamentoUnidade(
                    request.getDepartamentoUnidadeOrigemId());
            if (!Objects.equals(origem.getDepartamentoId(), request.getDepartamentoId())) {
                throw regra(400, "Unidade modelo nao pertence ao departamento selecionado.");
            }
            equipeOrigem = configService.listarAtendentesDepartamento(origem.getId());
        }

        List<DepartamentoUnidade> sincronizados =
                configService.sincronizarDepartamentoUnidades(request);
        if (!equipeOrigem.isEmpty()) {
            for (DepartamentoUnidade vinculo : sincronizados) {
                if (Boolean.FALSE.equals(vinculo.getAtivo())) {
                    continue;
                }
                for (DepartamentoAtendente atendente : equipeOrigem) {
                    garantirPerfilUsuario(
                            atendente.getCodgUsuario(),
                            vinculo.getCodgUnidade(),
                            perfilPorPapel(atendente.getPapel()));
                }
            }
        }
        return sincronizados;
    }

    public DepartamentoUnidade salvarDepartamentoUnidade(Integer codgUsuarioGestor, DepartamentoUnidade entity) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        obrigatorio(entity, "Informe o departamento/unidade.");
        if (entity.getId() != null) {
            DepartamentoUnidade existente = configService.buscarDepartamentoUnidade(entity.getId());
            if (!isAdminGlobal(unidade) && !Objects.equals(existente.getCodgUnidade(), unidade)) {
                throw regra(403, "Departamento/unidade nao pertence a unidade do gestor.");
            }
        }
        if (!isAdminGlobal(unidade)
                && entity.getDepartamentoId() != null
                && !departamentoVisivelNaUnidade(entity.getDepartamentoId(), unidade)) {
            Departamento departamento = configService.buscarDepartamento(entity.getDepartamentoId());
            if (departamento.getDepartamentoPaiId() != null
                    && !departamentoVisivelNaUnidade(departamento.getDepartamentoPaiId(), unidade)) {
                throw regra(403, "Departamento nao pertence a unidade do gestor.");
            }
        }
        if (isAdminGlobal(unidade)) {
            obrigatorio(entity.getCodgUnidade(), "Informe a unidade.");
        } else {
            entity.setCodgUnidade(unidade);
        }
        return configService.salvarDepartamentoUnidade(entity);
    }

    public void excluirDepartamentoUnidade(Integer codgUsuarioGestor, Long id) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        DepartamentoUnidade existente = configService.buscarDepartamentoUnidade(id);
        if (!isAdminGlobal(unidade) && !Objects.equals(existente.getCodgUnidade(), unidade)) {
            throw regra(403, "Departamento/unidade nao pertence a unidade do gestor.");
        }
        configService.excluirDepartamentoUnidade(id);
    }

    public List<DepartamentoAtendente> listarAtendentes(Integer codgUsuarioGestor) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        Set<Long> duIds = idsDepartamentoUnidade(unidade);
        return configService.listarDepartamentoAtendentes().stream()
                .filter(item -> isAdminGlobal(unidade) || duIds.contains(item.getDepartamentoUnidadeId()))
                .sorted(Comparator.comparing(DepartamentoAtendente::getCodgUsuario, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
    }

    public DepartamentoAtendente salvarAtendente(Integer codgUsuarioGestor, DepartamentoAtendente entity) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        obrigatorio(entity, "Informe o atendente.");
        DepartamentoUnidade departamentoUnidade = departamentoUnidadeDaUnidade(entity.getDepartamentoUnidadeId(), unidade);
        RefUsuario usuario = usuarioInternoCompartilhavel(entity.getCodgUsuario());
        entity.setDepartamentoUnidadeId(departamentoUnidade.getId());
        if (entity.getPapel() == null) {
            entity.setPapel(PapelAtendente.ATENDENTE);
        }
        DepartamentoAtendente salvo = configService.salvarDepartamentoAtendente(entity);
        sincronizarPerfisAutomaticosUsuario(usuario.getCodgUsuario());
        return salvo;
    }

    public List<DepartamentoAtendente> sincronizarAtendente(
            Integer codgUsuarioGestor,
            DepartamentoAtendenteSincronizacaoRequest request) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        obrigatorio(request, "Informe os vinculos do atendente.");
        obrigatorio(request.getDepartamentoId(), "Informe o departamento.");
        obrigatorio(request.getCodgUsuario(), "Informe o usuario.");

        Departamento departamento = configService.buscarDepartamento(request.getDepartamentoId());
        if (temFilho(departamento.getId())) {
            throw regra(400, "Departamento agrupador nao recebe atendentes.");
        }
        RefUsuario usuario = usuarioInternoCompartilhavel(request.getCodgUsuario());

        List<DepartamentoUnidade> vinculosEscopo = departamentoUnidadesDoEscopo(unidade).stream()
                .filter(item -> item.getId() != null)
                .filter(item -> Objects.equals(item.getDepartamentoId(), request.getDepartamentoId()))
                .sorted(Comparator.comparing(
                        DepartamentoUnidade::getCodgUnidade,
                        Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
        Set<Long> idsEscopo = vinculosEscopo.stream()
                .map(DepartamentoUnidade::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> idsAtivos = vinculosEscopo.stream()
                .filter(item -> !Boolean.FALSE.equals(item.getAtivo()))
                .map(DepartamentoUnidade::getId)
                .collect(Collectors.toSet());
        Set<Long> idsSelecionados = request.getDepartamentoUnidadeIds() == null
                ? new LinkedHashSet<>()
                : request.getDepartamentoUnidadeIds().stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        if (idsEscopo.isEmpty()) {
            throw regra(400, "Departamento nao possui unidades disponiveis neste escopo.");
        }
        if (idsSelecionados.isEmpty()) {
            throw regra(400, "Selecione ao menos uma unidade para o atendente.");
        }
        List<Long> invalidos = idsSelecionados.stream()
                .filter(id -> !idsAtivos.contains(id))
                .sorted()
                .collect(Collectors.toList());
        if (!invalidos.isEmpty()) {
            throw regra(403, "Unidades inativas ou fora do escopo de administracao: " + invalidos);
        }

        request.setCodgUsuario(usuario.getCodgUsuario());
        request.setDepartamentoUnidadeIds(new ArrayList<>(idsSelecionados));
        request.setDepartamentoUnidadeIdsEscopo(new ArrayList<>(idsEscopo));
        if (request.getPapel() == null) {
            request.setPapel(PapelAtendente.ATENDENTE);
        }
        if (request.getRecebeChamados() == null) {
            request.setRecebeChamados(true);
        }
        if (request.getPrioridadeDistribuicao() == null
                || request.getPrioridadeDistribuicao() <= 0) {
            request.setPrioridadeDistribuicao(1);
        }
        if (request.getLimiteChatsSimultaneos() == null
                || request.getLimiteChatsSimultaneos() <= 0) {
            request.setLimiteChatsSimultaneos(3);
        }
        if (request.getAtivo() == null) {
            request.setAtivo(true);
        }

        List<DepartamentoAtendente> sincronizados =
                configService.sincronizarDepartamentoAtendente(request);
        sincronizarPerfisAutomaticosUsuario(usuario.getCodgUsuario());
        return sincronizados;
    }

    public void excluirAtendente(Integer codgUsuarioGestor, Long id) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        DepartamentoAtendente existente = configService.buscarDepartamentoAtendente(id);
        departamentoUnidadeDaUnidade(existente.getDepartamentoUnidadeId(), unidade);
        configService.excluirDepartamentoAtendente(id);
        sincronizarPerfisAutomaticosUsuario(existente.getCodgUsuario());
    }

    public List<AtendenteStatus> listarAtendenteStatus(Integer codgUsuarioGestor) {
        Set<Integer> atendentes = listarAtendentes(codgUsuarioGestor).stream()
                .map(DepartamentoAtendente::getCodgUsuario)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        Map<Integer, AtendenteStatus> existentes = configService.listarAtendenteStatus().stream()
                .filter(item -> item.getCodgUsuario() != null)
                .filter(item -> atendentes.contains(item.getCodgUsuario()))
                .collect(Collectors.toMap(AtendenteStatus::getCodgUsuario, item -> item, (a, b) -> a, LinkedHashMap::new));
        return atendentes.stream()
                .sorted()
                .map(codgUsuario -> {
                    AtendenteStatus status = existentes.get(codgUsuario);
                    if (status == null) {
                        status = new AtendenteStatus();
                        status.setCodgUsuario(codgUsuario);
                        status.setStatus(StatusAtendente.ONLINE);
                        status.setAtendimentosAtivos(0);
                    } else if (status.getStatus() == null) {
                        status.setStatus(StatusAtendente.ONLINE);
                    }
                    return status;
                })
                .collect(Collectors.toList());
    }

    public AtendenteStatus salvarAtendenteStatus(Integer codgUsuarioGestor, AtendenteStatus status) {
        obrigatorio(status, "Informe o status do atendente.");
        obrigatorio(status.getCodgUsuario(), "Informe o atendente.");
        boolean noEscopo = listarAtendentes(codgUsuarioGestor).stream()
                .map(DepartamentoAtendente::getCodgUsuario)
                .filter(Objects::nonNull)
                .anyMatch(status.getCodgUsuario()::equals);
        if (!noEscopo) {
            throw regra(403, "Atendente nao pertence ao escopo do gestor.");
        }
        if (status.getStatus() == null) {
            status.setStatus(StatusAtendente.ONLINE);
        }
        return configService.salvarAtendenteStatus(status);
    }

    public List<RefUsuario> listarUsuariosInternos(Integer codgUsuarioGestor) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        Set<Integer> atendentesVinculados = listarAtendentes(codgUsuarioGestor).stream()
                .map(DepartamentoAtendente::getCodgUsuario)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Integer> administradoresChat = isAdminGlobal(unidade)
                ? idsUsuariosAdminChatAtivos()
                : Set.of();
        return configService.listarUsuariosReferencia().stream()
                .filter(item -> usuarioInternoElegivel(item, unidade)
                || atendentesVinculados.contains(item.getCodgUsuario())
                || administradoresChat.contains(item.getCodgUsuario()))
                .sorted(Comparator.comparing(RefUsuario::getNomeCompleto, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    public RefUsuario buscarUsuarioInternoPorLogin(Integer codgUsuarioGestor, String login) {
        unidadeGestor(codgUsuarioGestor);
        RefUsuario usuario = configService.buscarUsuarioReferenciaPorLogin(login);
        if (!isUsuarioInternoCompartilhavel(usuario)) {
            throw regra(403, "Usuario precisa ser interno, ativo no chat e nao pode possuir agencia vinculada.");
        }
        return usuario;
    }

    public RefUsuario buscarUsuarioParaAcessoPorLogin(Integer codgUsuarioGestor, String login) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        RefUsuario usuario = configService.buscarUsuarioReferenciaPorLogin(login);
        boolean elegivel = isAdminGlobal(unidade)
                ? usuario != null && !Boolean.FALSE.equals(usuario.getAtivoChat())
                : usuarioInternoElegivel(usuario, unidade);
        if (!elegivel) {
            throw regra(403, "Usuario nao pode receber perfis de acesso neste escopo.");
        }
        return usuario;
    }

    public List<ChatPerfil> listarPerfisGerenciaveis(Integer codgUsuarioGestor) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        Set<String> codigos = new HashSet<>(PERFIS_GERENCIAVEIS_UNIDADE);
        if (isAdminGlobal(unidade)) {
            codigos.add(PERFIL_ADMIN_CHAT);
        }
        return codigos.stream()
                .map(this::perfilPorCodigoOuCriar)
                .sorted(Comparator.comparing(ChatPerfil::getCodigo, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    public List<ChatUsuarioPerfil> listarUsuarioPerfis(Integer codgUsuarioGestor) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        Set<Integer> usuarios = listarUsuariosInternos(codgUsuarioGestor).stream()
                .map(RefUsuario::getCodgUsuario)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> perfis = listarPerfisGerenciaveis(codgUsuarioGestor).stream()
                .map(ChatPerfil::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> perfisAdmin = idsPerfisAdminChat();
        return configService.listarUsuarioPerfis().stream()
                .filter(item -> usuarios.contains(item.getCodgUsuario())
                || (isAdminGlobal(unidade) && perfisAdmin.contains(item.getPerfilId())))
                .filter(item -> perfis.contains(item.getPerfilId()))
                .filter(item -> isAdminGlobal(unidade) || item.getCodgUnidade() == null || Objects.equals(item.getCodgUnidade(), unidade))
                .collect(Collectors.toList());
    }

    public ChatUsuarioPerfil salvarUsuarioPerfil(Integer codgUsuarioGestor, ChatUsuarioPerfil entity) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        obrigatorio(entity, "Informe o perfil do usuario.");
        ChatPerfil perfil = configService.buscarPerfil(entity.getPerfilId());
        String codigoPerfil = normalizarCodigo(perfil.getCodigo());
        validarPerfilGerenciavel(codigoPerfil, unidade);
        ChatUsuarioPerfil existenteEdicao = null;
        String codigoPerfilExistente = null;
        if (entity.getId() != null) {
            existenteEdicao = configService.buscarUsuarioPerfil(entity.getId());
            validarPerfilNaoAutomatico(existenteEdicao);
            ChatPerfil perfilExistente = configService.buscarPerfil(existenteEdicao.getPerfilId());
            codigoPerfilExistente = normalizarCodigo(perfilExistente.getCodigo());
            validarPerfilGerenciavel(codigoPerfilExistente, unidade);
        }

        RefUsuario usuario = PERFIL_ADMIN_CHAT.equals(codigoPerfil)
                ? usuarioAtivoChat(entity.getCodgUsuario())
                : usuarioInternoDaUnidade(entity.getCodgUsuario(), unidade);
        if (PERFIL_ADMIN_CHAT.equals(codigoPerfil)) {
            entity.setCodgUnidade(null);
        } else if (isAdminGlobal(unidade) && entity.getCodgUnidade() == null) {
            entity.setCodgUnidade(usuario.getCodgUnidade());
        } else if (!isAdminGlobal(unidade)) {
            entity.setCodgUnidade(unidade);
        }
        if (entity.getAtivo() == null) {
            entity.setAtivo(true);
        }
        entity.setAutomatico(false);
        ChatUsuarioPerfil duplicado = buscarPerfilUsuarioExistente(
                entity.getCodgUsuario(),
                entity.getPerfilId(),
                entity.getCodgUnidade(),
                entity.getId());
        if (duplicado != null) {
            if (Boolean.TRUE.equals(duplicado.getAutomatico())
                    && !Boolean.FALSE.equals(duplicado.getAtivo())) {
                throw regra(409,
                        "Este perfil e mantido pelo vinculo do atendente. Altere-o na aba Atendentes.");
            }
            entity.setId(duplicado.getId());
        }
        boolean removeAcessoAdmin = PERFIL_ADMIN_CHAT.equals(codigoPerfilExistente)
                && (!PERFIL_ADMIN_CHAT.equals(codigoPerfil) || Boolean.FALSE.equals(entity.getAtivo()));
        if (removeAcessoAdmin) {
            validarOutroAdministradorAtivo(existenteEdicao.getId());
        } else if (PERFIL_ADMIN_CHAT.equals(codigoPerfil) && Boolean.FALSE.equals(entity.getAtivo())) {
            validarOutroAdministradorAtivo(entity.getId());
        }
        return configService.salvarUsuarioPerfil(entity);
    }

    public void excluirUsuarioPerfil(Integer codgUsuarioGestor, Long id) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        ChatUsuarioPerfil existente = configService.buscarUsuarioPerfil(id);
        validarPerfilNaoAutomatico(existente);
        ChatPerfil perfil = configService.buscarPerfil(existente.getPerfilId());
        String codigoPerfil = normalizarCodigo(perfil.getCodigo());
        validarPerfilGerenciavel(codigoPerfil, unidade);
        if (PERFIL_ADMIN_CHAT.equals(codigoPerfil)) {
            usuarioAtivoChat(existente.getCodgUsuario());
            if (!Boolean.FALSE.equals(existente.getAtivo())) {
                validarOutroAdministradorAtivo(existente.getId());
            }
        } else {
            usuarioInternoDaUnidade(existente.getCodgUsuario(), unidade);
        }
        if (!isAdminGlobal(unidade) && existente.getCodgUnidade() != null && !Objects.equals(existente.getCodgUnidade(), unidade)) {
            throw regra(403, "Perfil pertence a outra unidade.");
        }
        configService.excluirUsuarioPerfil(id);
    }

    private void validarPerfilNaoAutomatico(ChatUsuarioPerfil perfilUsuario) {
        if (perfilUsuario != null && Boolean.TRUE.equals(perfilUsuario.getAutomatico())) {
            throw regra(409,
                    "Perfil automatico: altere o papel ou as unidades do usuario na aba Atendentes.");
        }
    }

    public List<RespostaRapida> listarRespostasRapidas(Integer codgUsuarioGestor) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        return configService.listarRespostasRapidas(null, unidade, false);
    }

    public RespostaRapida salvarRespostaRapida(Integer codgUsuarioGestor, RespostaRapida entity) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        obrigatorio(entity, "Informe a resposta rapida.");
        if (!isAdminGlobal(unidade)
                && entity.getDepartamentoId() != null
                && !departamentoVisivelNaUnidade(entity.getDepartamentoId(), unidade)) {
            throw regra(403, "Departamento nao pertence a unidade do gestor.");
        }
        if (!isAdminGlobal(unidade)) {
            entity.setCodgUnidade(unidade);
        }
        if (entity.getCriadoPorCodgUsuario() == null) {
            entity.setCriadoPorCodgUsuario(codgUsuarioGestor);
        }
        return configService.salvarRespostaRapida(entity);
    }

    public void excluirRespostaRapida(Integer codgUsuarioGestor, Long id) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        RespostaRapida existente = configService.buscarRespostaRapida(id);
        if (!isAdminGlobal(unidade) && existente.getCodgUnidade() != null && !Objects.equals(existente.getCodgUnidade(), unidade)) {
            throw regra(403, "Resposta rapida pertence a outra unidade.");
        }
        if (!isAdminGlobal(unidade)
                && existente.getDepartamentoId() != null
                && !departamentoVisivelNaUnidade(existente.getDepartamentoId(), unidade)) {
            throw regra(403, "Resposta rapida pertence a outra unidade.");
        }
        configService.excluirRespostaRapida(id);
    }

    public List<SlaPolitica> listarSlaPoliticas(Integer codgUsuarioGestor) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        Set<Long> duIds = idsDepartamentoUnidade(unidade);
        return configService.listarSlaPoliticas().stream()
                .filter(item -> isAdminGlobal(unidade) || duIds.contains(item.getDepartamentoUnidadeId()))
                .collect(Collectors.toList());
    }

    public SlaPolitica salvarSlaPolitica(Integer codgUsuarioGestor, SlaPolitica politica) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        departamentoUnidadeDaUnidade(politica == null ? null : politica.getDepartamentoUnidadeId(), unidade);
        return configService.salvarSlaPolitica(politica);
    }

    public List<SlaPolitica> sincronizarSlaPoliticas(
            Integer codgUsuarioGestor,
            SlaPoliticaSincronizacaoRequest request) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        obrigatorio(request, "Informe a politica de SLA.");
        obrigatorio(request.getDepartamentoId(), "Informe o departamento.");
        obrigatorio(request.getPrioridade(), "Informe a prioridade.");

        Departamento departamento = configService.buscarDepartamento(request.getDepartamentoId());
        if (departamento == null || departamento.getId() == null) {
            throw regra(404, "Departamento nao encontrado.");
        }
        if (Boolean.FALSE.equals(departamento.getAtivo())) {
            throw regra(400, "Departamento inativo nao pode receber politica de SLA.");
        }
        if (temFilho(departamento.getId())) {
            throw regra(400, "Departamento agrupador nao recebe politica de SLA.");
        }
        List<Long> idsEscopo = departamentoUnidadesDoEscopo(unidade).stream()
                .filter(item -> Objects.equals(
                item.getDepartamentoId(),
                request.getDepartamentoId()))
                .filter(item -> !Boolean.FALSE.equals(item.getAtivo()))
                .map(DepartamentoUnidade::getId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        if (idsEscopo.isEmpty()) {
            throw regra(400, "Departamento nao possui unidades ativas neste escopo.");
        }
        request.setDepartamentoUnidadeIdsEscopo(idsEscopo);
        if (request.getAtivo() == null) {
            request.setAtivo(true);
        }
        return configService.sincronizarSlaPoliticas(request);
    }

    public void excluirSlaPolitica(Integer codgUsuarioGestor, Long id) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        SlaPolitica existente = configService.listarSlaPoliticas().stream()
                .filter(item -> Objects.equals(item.getId(), id))
                .findFirst()
                .orElseThrow(() -> regra(404, "Politica de SLA nao encontrada."));
        departamentoUnidadeDaUnidade(existente.getDepartamentoUnidadeId(), unidade);
        configService.excluirSlaPolitica(id);
    }

    private Integer unidadeGestor(Integer codgUsuarioGestor) {
        obrigatorio(codgUsuarioGestor, "Informe o gestor.");
        SessaoChatResponse sessao = chatService.montarSessao(codgUsuarioGestor);
        if (sessao == null || (!sessao.isGestor() && !sessao.isAdmin())) {
            throw regra(403, "Acesso restrito a gestores do chat.");
        }
        RefUsuario usuario = sessao.getUsuario();
        if (sessao.isAdmin()) {
            return null;
        }
        if (usuario == null || usuario.getCodgUnidade() == null) {
            throw regra(403, "Gestor nao possui unidade interna vinculada ao chat.");
        }
        if (!sessao.isAdmin() && usuario.getCodgAgencia() != null) {
            throw regra(403, "Gestao do chat restrita a usuarios internos da unidade.");
        }
        return usuario.getCodgUnidade();
    }

    private boolean isAdminGlobal(Integer codgUnidade) {
        return codgUnidade == null;
    }

    private List<DepartamentoUnidade> departamentoUnidadesDoEscopo(Integer codgUnidade) {
        if (isAdminGlobal(codgUnidade)) {
            return configService.listarDepartamentoUnidades();
        }
        return configService.listarDepartamentoUnidadesPorUnidade(codgUnidade);
    }

    private Set<Long> idsDepartamentoUnidade(Integer codgUnidade) {
        return departamentoUnidadesDoEscopo(codgUnidade).stream()
                .map(DepartamentoUnidade::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<Long> idsDepartamentosUnidade(Integer codgUnidade, boolean incluirPais) {
        if (isAdminGlobal(codgUnidade)) {
            return configService.listarDepartamentos().stream()
                    .map(Departamento::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }
        Set<Long> ids = departamentoUnidadesDoEscopo(codgUnidade).stream()
                .map(DepartamentoUnidade::getDepartamentoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!incluirPais) {
            return ids;
        }
        Set<Long> visitados = new HashSet<>(ids);
        boolean alterou = true;
        while (alterou) {
            alterou = false;
            List<Long> atuais = new ArrayList<>(visitados);
            for (Long id : atuais) {
                Departamento departamento = buscarDepartamentoOuNull(id);
                if (departamento != null && departamento.getDepartamentoPaiId() != null
                        && visitados.add(departamento.getDepartamentoPaiId())) {
                    alterou = true;
                }
            }
        }
        return visitados;
    }

    private boolean departamentoVisivelNaUnidade(Long departamentoId, Integer codgUnidade) {
        return departamentoId != null
                && (isAdminGlobal(codgUnidade) || idsDepartamentosUnidade(codgUnidade, true).contains(departamentoId));
    }

    private DepartamentoUnidade departamentoUnidadeDaUnidade(Long departamentoUnidadeId, Integer codgUnidade) {
        obrigatorio(departamentoUnidadeId, "Informe o departamento/unidade.");
        DepartamentoUnidade departamentoUnidade = configService.buscarDepartamentoUnidade(departamentoUnidadeId);
        if (!isAdminGlobal(codgUnidade) && !Objects.equals(departamentoUnidade.getCodgUnidade(), codgUnidade)) {
            throw regra(403, "Departamento/unidade nao pertence a unidade do gestor.");
        }
        return departamentoUnidade;
    }

    private RefUsuario usuarioInternoDaUnidade(Integer codgUsuario, Integer codgUnidade) {
        obrigatorio(codgUsuario, "Informe o usuario.");
        RefUsuario usuario = buscarUsuarioReferencia(codgUsuario);
        if (!usuarioInternoElegivel(usuario, codgUnidade)) {
            throw regra(403, "Usuario nao e interno da unidade do gestor ou possui agencia vinculada.");
        }
        return usuario;
    }

    private RefUsuario usuarioInternoCompartilhavel(Integer codgUsuario) {
        obrigatorio(codgUsuario, "Informe o usuario.");
        RefUsuario usuario = buscarUsuarioReferencia(codgUsuario);
        if (!isUsuarioInternoCompartilhavel(usuario)) {
            throw regra(403, "Usuario precisa ser interno, ativo no chat e nao pode possuir agencia vinculada.");
        }
        return usuario;
    }

    private RefUsuario usuarioAtivoChat(Integer codgUsuario) {
        obrigatorio(codgUsuario, "Informe o usuario.");
        RefUsuario usuario = buscarUsuarioReferencia(codgUsuario);
        if (usuario == null || Boolean.FALSE.equals(usuario.getAtivoChat())) {
            throw regra(403, "Usuario precisa estar ativo no chat.");
        }
        return usuario;
    }

    private RefUsuario buscarUsuarioReferencia(Integer codgUsuario) {
        return configService.listarUsuariosReferencia().stream()
                .filter(item -> Objects.equals(item.getCodgUsuario(), codgUsuario))
                .findFirst()
                .orElseThrow(() -> regra(404, "Usuario nao encontrado na referencia do chat. Busque pelo login primeiro."));
    }

    private boolean usuarioInternoElegivel(RefUsuario usuario, Integer codgUnidade) {
        return isUsuarioInternoCompartilhavel(usuario)
                && (isAdminGlobal(codgUnidade) || Objects.equals(usuario.getCodgUnidade(), codgUnidade));
    }

    private boolean isUsuarioInternoCompartilhavel(RefUsuario usuario) {
        return usuario != null
                && usuario.getCodgAgencia() == null
                && !Boolean.FALSE.equals(usuario.getAtivoChat());
    }

    private void sincronizarPerfisAutomaticosUsuario(Integer codgUsuario) {
        if (codgUsuario == null) {
            return;
        }
        List<DepartamentoUnidade> departamentoUnidades = configService.listarDepartamentoUnidades();
        Map<Long, DepartamentoUnidade> departamentoUnidadePorId =
                (departamentoUnidades == null ? List.<DepartamentoUnidade>of() : departamentoUnidades).stream()
                        .filter(Objects::nonNull)
                        .filter(item -> item.getId() != null)
                        .collect(Collectors.toMap(
                                DepartamentoUnidade::getId,
                                item -> item,
                                (primeiro, ignorado) -> primeiro,
                                LinkedHashMap::new));

        List<DepartamentoAtendente> vinculos = configService.listarDepartamentoAtendentes();
        Map<Integer, Set<String>> perfisNecessarios = new LinkedHashMap<>();
        (vinculos == null ? List.<DepartamentoAtendente>of() : vinculos).stream()
                .filter(Objects::nonNull)
                .filter(item -> Objects.equals(item.getCodgUsuario(), codgUsuario))
                .filter(item -> !Boolean.FALSE.equals(item.getAtivo()))
                .forEach(item -> {
                    DepartamentoUnidade departamentoUnidade =
                            departamentoUnidadePorId.get(item.getDepartamentoUnidadeId());
                    if (departamentoUnidade == null || departamentoUnidade.getCodgUnidade() == null) {
                        return;
                    }
                    perfisNecessarios
                            .computeIfAbsent(
                                    departamentoUnidade.getCodgUnidade(),
                                    ignorado -> new LinkedHashSet<>())
                            .add(perfilPorPapel(item.getPapel()));
                });

        perfisNecessarios.forEach((codgUnidade, codigos) ->
                codigos.forEach(codigo ->
                        garantirPerfilUsuario(codgUsuario, codgUnidade, codigo)));

        List<ChatPerfil> perfis = configService.listarPerfis();
        Map<Long, String> codigoPerfilPorId =
                (perfis == null ? List.<ChatPerfil>of() : perfis).stream()
                        .filter(Objects::nonNull)
                        .filter(item -> item.getId() != null)
                        .collect(Collectors.toMap(
                                ChatPerfil::getId,
                                item -> normalizarCodigo(item.getCodigo()),
                                (primeiro, ignorado) -> primeiro));
        List<ChatUsuarioPerfil> perfisUsuario = configService.listarUsuarioPerfis();
        (perfisUsuario == null ? List.<ChatUsuarioPerfil>of() : perfisUsuario).stream()
                .filter(Objects::nonNull)
                .filter(item -> Objects.equals(item.getCodgUsuario(), codgUsuario))
                .filter(item -> Boolean.TRUE.equals(item.getAutomatico()))
                .filter(item -> !Boolean.FALSE.equals(item.getAtivo()))
                .filter(item -> {
                    String codigo = codigoPerfilPorId.get(item.getPerfilId());
                    return PERFIS_DERIVADOS_ATENDENTE.contains(codigo)
                            && !perfisNecessarios
                                    .getOrDefault(item.getCodgUnidade(), Set.of())
                                    .contains(codigo);
                })
                .forEach(item -> {
                    item.setAtivo(false);
                    configService.salvarUsuarioPerfil(item);
                });
    }

    private void garantirPerfilUsuario(Integer codgUsuario, Integer codgUnidade, String codigoPerfil) {
        ChatPerfil perfil = perfilPorCodigoOuCriar(codigoPerfil);
        List<ChatUsuarioPerfil> perfisUsuario = configService.listarUsuarioPerfis();
        List<ChatUsuarioPerfil> existentes =
                (perfisUsuario == null ? List.<ChatUsuarioPerfil>of() : perfisUsuario).stream()
                        .filter(Objects::nonNull)
                        .filter(item -> Objects.equals(item.getCodgUsuario(), codgUsuario))
                        .filter(item -> Objects.equals(item.getPerfilId(), perfil.getId()))
                        .filter(item -> Objects.equals(item.getCodgUnidade(), codgUnidade))
                        .collect(Collectors.toList());
        boolean perfilManual = existentes.stream()
                .anyMatch(item -> !Boolean.TRUE.equals(item.getAutomatico()));
        if (perfilManual) {
            return;
        }
        ChatUsuarioPerfil existenteAutomatico = existentes.stream()
                .filter(item -> Boolean.TRUE.equals(item.getAutomatico()))
                .findFirst()
                .orElse(null);
        ChatUsuarioPerfil entity =
                existenteAutomatico == null ? new ChatUsuarioPerfil() : existenteAutomatico;
        entity.setCodgUsuario(codgUsuario);
        entity.setPerfilId(perfil.getId());
        entity.setCodgUnidade(codgUnidade);
        entity.setAtivo(true);
        entity.setAutomatico(true);
        configService.salvarUsuarioPerfil(entity);
    }

    private ChatUsuarioPerfil buscarPerfilUsuarioExistente(Integer codgUsuario, Long perfilId, Integer codgUnidade, Long idIgnorado) {
        return configService.listarUsuarioPerfis().stream()
                .filter(item -> !Objects.equals(item.getId(), idIgnorado))
                .filter(item -> Objects.equals(item.getCodgUsuario(), codgUsuario))
                .filter(item -> Objects.equals(item.getPerfilId(), perfilId))
                .filter(item -> Objects.equals(item.getCodgUnidade(), codgUnidade))
                .findFirst()
                .orElse(null);
    }

    private ChatPerfil perfilPorCodigoOuCriar(String codigoPerfil) {
        String codigo = normalizarCodigo(codigoPerfil);
        return configService.listarPerfis().stream()
                .filter(item -> codigo.equals(normalizarCodigo(item.getCodigo())))
                .findFirst()
                .orElseGet(() -> {
                    ChatPerfil perfil = new ChatPerfil();
                    perfil.setCodigo(codigo);
                    perfil.setNome(nomePerfil(codigo));
                    perfil.setDescricao("Perfil criado automaticamente pela gestao do chat.");
                    perfil.setAtivo(true);
                    return configService.salvarPerfil(perfil);
                });
    }

    private String perfilPorPapel(PapelAtendente papel) {
        if (papel == PapelAtendente.SUPERVISOR) {
            return "SUPERVISOR";
        }
        if (papel == PapelAtendente.GESTOR) {
            return "GESTOR_UNIDADE";
        }
        return "ATENDENTE";
    }

    private String nomePerfil(String codigo) {
        return switch (codigo) {
            case PERFIL_ADMIN_CHAT -> "Administrador geral do chat";
            case "SUPERVISOR" -> "Supervisor do chat";
            case "GESTOR" -> "Gestor do chat";
            case "GESTOR_UNIDADE" -> "Gestor da unidade";
            default -> "Atendente do chat";
        };
    }

    private String normalizarCodigo(String codigo) {
        return codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
    }

    private void validarPerfilGerenciavel(String codigoPerfil, Integer codgUnidade) {
        if (PERFIL_ADMIN_CHAT.equals(codigoPerfil)) {
            if (!isAdminGlobal(codgUnidade)) {
                throw regra(403, "Somente um administrador geral pode conceder ou remover este perfil.");
            }
            return;
        }
        if (!PERFIS_GERENCIAVEIS_UNIDADE.contains(codigoPerfil)) {
            throw regra(403, "Perfil nao pode ser gerenciado pela unidade.");
        }
    }

    private Set<Long> idsPerfisAdminChat() {
        return configService.listarPerfis().stream()
                .filter(Objects::nonNull)
                .filter(item -> PERFIL_ADMIN_CHAT.equals(normalizarCodigo(item.getCodigo())))
                .map(ChatPerfil::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<Integer> idsUsuariosAdminChatAtivos() {
        Set<Long> perfisAdmin = idsPerfisAdminChat();
        if (perfisAdmin.isEmpty()) {
            return Set.of();
        }
        return configService.listarUsuarioPerfis().stream()
                .filter(Objects::nonNull)
                .filter(item -> !Boolean.FALSE.equals(item.getAtivo()))
                .filter(item -> perfisAdmin.contains(item.getPerfilId()))
                .map(ChatUsuarioPerfil::getCodgUsuario)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private void validarOutroAdministradorAtivo(Long idIgnorado) {
        Set<Long> perfisAdmin = idsPerfisAdminChat();
        boolean existeOutro = configService.listarUsuarioPerfis().stream()
                .filter(Objects::nonNull)
                .filter(item -> !Objects.equals(item.getId(), idIgnorado))
                .filter(item -> !Boolean.FALSE.equals(item.getAtivo()))
                .anyMatch(item -> perfisAdmin.contains(item.getPerfilId()));
        if (!existeOutro) {
            throw regra(409, "O ultimo administrador geral do chat nao pode ser removido ou inativado.");
        }
    }

    private boolean temFilho(Long departamentoId) {
        return configService.listarDepartamentos().stream()
                .anyMatch(item -> Objects.equals(item.getDepartamentoPaiId(), departamentoId));
    }

    private Departamento buscarDepartamentoOuNull(Long id) {
        try {
            return configService.buscarDepartamento(id);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String nomeDepartamentoUnidade(DepartamentoUnidade departamentoUnidade) {
        if (departamentoUnidade == null) {
            return "";
        }
        if (departamentoUnidade.getNomeExibicao() != null && !departamentoUnidade.getNomeExibicao().isBlank()) {
            return departamentoUnidade.getNomeExibicao();
        }
        Departamento departamento = buscarDepartamentoOuNull(departamentoUnidade.getDepartamentoId());
        return departamento == null ? "" : departamento.getNome();
    }

    private void obrigatorio(Object valor, String mensagem) {
        if (valor == null) {
            throw regra(400, mensagem);
        }
    }

    private RegraDeNegocioException regra(int status, String mensagem) {
        return new RegraDeNegocioException(status, mensagem);
    }
}
