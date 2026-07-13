package com.confApi.chatconfianca.service;

import com.confApi.chatconfianca.dto.enums.PapelAtendente;
import com.confApi.chatconfianca.dto.enums.StatusAtendente;
import com.confApi.chatconfianca.dto.model.AtendenteStatus;
import com.confApi.chatconfianca.dto.model.ChatPerfil;
import com.confApi.chatconfianca.dto.model.ChatUsuarioPerfil;
import com.confApi.chatconfianca.dto.model.Departamento;
import com.confApi.chatconfianca.dto.model.DepartamentoAtendente;
import com.confApi.chatconfianca.dto.model.DepartamentoUnidade;
import com.confApi.chatconfianca.dto.model.RefUsuario;
import com.confApi.chatconfianca.dto.model.RespostaRapida;
import com.confApi.chatconfianca.dto.model.SlaPolitica;
import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.exception.RegraDeNegocioException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ChatConfiancaGestaoUnidadeService {

    private static final Set<String> PERFIS_GERENCIAVEIS = Set.of(
            "ATENDENTE",
            "SUPERVISOR",
            "GESTOR",
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
        Integer unidadeAtendimento = departamentoUnidade.getCodgUnidade();
        RefUsuario usuario = usuarioInternoCompartilhavel(entity.getCodgUsuario());
        entity.setDepartamentoUnidadeId(departamentoUnidade.getId());
        if (entity.getPapel() == null) {
            entity.setPapel(PapelAtendente.ATENDENTE);
        }
        DepartamentoAtendente salvo = configService.salvarDepartamentoAtendente(entity);
        garantirPerfilUsuario(usuario.getCodgUsuario(), unidadeAtendimento, perfilPorPapel(entity.getPapel()));
        return salvo;
    }

    public void excluirAtendente(Integer codgUsuarioGestor, Long id) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        DepartamentoAtendente existente = configService.buscarDepartamentoAtendente(id);
        departamentoUnidadeDaUnidade(existente.getDepartamentoUnidadeId(), unidade);
        configService.excluirDepartamentoAtendente(id);
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
        return configService.listarUsuariosReferencia().stream()
                .filter(item -> usuarioInternoElegivel(item, unidade)
                || atendentesVinculados.contains(item.getCodgUsuario()))
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

    public List<ChatPerfil> listarPerfisGerenciaveis(Integer codgUsuarioGestor) {
        unidadeGestor(codgUsuarioGestor);
        return PERFIS_GERENCIAVEIS.stream()
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
        return configService.listarUsuarioPerfis().stream()
                .filter(item -> usuarios.contains(item.getCodgUsuario()))
                .filter(item -> perfis.contains(item.getPerfilId()))
                .filter(item -> isAdminGlobal(unidade) || item.getCodgUnidade() == null || Objects.equals(item.getCodgUnidade(), unidade))
                .collect(Collectors.toList());
    }

    public ChatUsuarioPerfil salvarUsuarioPerfil(Integer codgUsuarioGestor, ChatUsuarioPerfil entity) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        obrigatorio(entity, "Informe o perfil do usuario.");
        RefUsuario usuario = usuarioInternoDaUnidade(entity.getCodgUsuario(), unidade);
        ChatPerfil perfil = configService.buscarPerfil(entity.getPerfilId());
        if (!PERFIS_GERENCIAVEIS.contains(normalizarCodigo(perfil.getCodigo()))) {
            throw regra(403, "Perfil nao pode ser gerenciado pela unidade.");
        }
        if (isAdminGlobal(unidade) && entity.getCodgUnidade() == null) {
            entity.setCodgUnidade(usuario.getCodgUnidade());
        } else if (!isAdminGlobal(unidade)) {
            entity.setCodgUnidade(unidade);
        }
        if (entity.getAtivo() == null) {
            entity.setAtivo(true);
        }
        ChatUsuarioPerfil duplicado = buscarPerfilUsuarioExistente(entity.getCodgUsuario(), entity.getPerfilId(), unidade, entity.getId());
        if (duplicado != null) {
            entity.setId(duplicado.getId());
        }
        return configService.salvarUsuarioPerfil(entity);
    }

    public void excluirUsuarioPerfil(Integer codgUsuarioGestor, Long id) {
        Integer unidade = unidadeGestor(codgUsuarioGestor);
        ChatUsuarioPerfil existente = configService.buscarUsuarioPerfil(id);
        usuarioInternoDaUnidade(existente.getCodgUsuario(), unidade);
        ChatPerfil perfil = configService.buscarPerfil(existente.getPerfilId());
        if (!PERFIS_GERENCIAVEIS.contains(normalizarCodigo(perfil.getCodigo()))) {
            throw regra(403, "Perfil nao pode ser gerenciado pela unidade.");
        }
        if (!isAdminGlobal(unidade) && existente.getCodgUnidade() != null && !Objects.equals(existente.getCodgUnidade(), unidade)) {
            throw regra(403, "Perfil pertence a outra unidade.");
        }
        configService.excluirUsuarioPerfil(id);
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
        if (sessao.isAdmin() && (usuario == null || usuario.getCodgUnidade() == null)) {
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

    private void garantirPerfilUsuario(Integer codgUsuario, Integer codgUnidade, String codigoPerfil) {
        ChatPerfil perfil = perfilPorCodigoOuCriar(codigoPerfil);
        ChatUsuarioPerfil existente = buscarPerfilUsuarioExistente(codgUsuario, perfil.getId(), codgUnidade, null);
        ChatUsuarioPerfil entity = existente == null ? new ChatUsuarioPerfil() : existente;
        entity.setCodgUsuario(codgUsuario);
        entity.setPerfilId(perfil.getId());
        entity.setCodgUnidade(codgUnidade);
        entity.setAtivo(true);
        configService.salvarUsuarioPerfil(entity);
    }

    private ChatUsuarioPerfil buscarPerfilUsuarioExistente(Integer codgUsuario, Long perfilId, Integer codgUnidade, Long idIgnorado) {
        return configService.listarUsuarioPerfis().stream()
                .filter(item -> !Objects.equals(item.getId(), idIgnorado))
                .filter(item -> Objects.equals(item.getCodgUsuario(), codgUsuario))
                .filter(item -> Objects.equals(item.getPerfilId(), perfilId))
                .filter(item -> item.getCodgUnidade() == null || Objects.equals(item.getCodgUnidade(), codgUnidade))
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
            case "SUPERVISOR" -> "Supervisor do chat";
            case "GESTOR" -> "Gestor do chat";
            case "GESTOR_UNIDADE" -> "Gestor da unidade";
            default -> "Atendente do chat";
        };
    }

    private String normalizarCodigo(String codigo) {
        return codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
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
