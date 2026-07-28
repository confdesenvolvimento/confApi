package com.confApi.chatconfianca.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.confApi.chatconfianca.client.ChatConfiancaManagerClient;
import com.confApi.chatconfianca.dto.enums.DistribuicaoDepartamento;
import com.confApi.chatconfianca.dto.enums.PapelAtendente;
import com.confApi.chatconfianca.dto.enums.StatusAtendente;
import com.confApi.chatconfianca.dto.model.AtendenteStatus;
import com.confApi.chatconfianca.dto.model.ChatPerfil;
import com.confApi.chatconfianca.dto.model.ChatUsuarioPerfil;
import com.confApi.chatconfianca.dto.model.Departamento;
import com.confApi.chatconfianca.dto.model.DepartamentoAtendente;
import com.confApi.chatconfianca.dto.model.DepartamentoUnidade;
import com.confApi.chatconfianca.dto.model.ManagerAgencia;
import com.confApi.chatconfianca.dto.model.ManagerUnidade;
import com.confApi.chatconfianca.dto.model.ManagerUsuario;
import com.confApi.chatconfianca.dto.model.ParametroSistema;
import com.confApi.chatconfianca.dto.model.RefAgencia;
import com.confApi.chatconfianca.dto.model.RefUnidade;
import com.confApi.chatconfianca.dto.model.RefUsuario;
import com.confApi.chatconfianca.dto.model.RespostaRapida;
import com.confApi.chatconfianca.dto.model.SlaPolitica;
import com.confApi.chatconfianca.dto.request.DepartamentoUnidadeSincronizacaoRequest;
import com.confApi.chatconfianca.dto.request.DepartamentoAtendenteSincronizacaoRequest;
import com.confApi.chatconfianca.dto.request.SlaPoliticaSincronizacaoRequest;
import com.confApi.exception.RegraDeNegocioException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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

@Service
public class ChatConfiancaConfigService {
    private final ChatConfiancaManagerClient manager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatConfiancaConfigService(ChatConfiancaManagerClient manager) {
        this.manager = manager;
    }

    public List<Departamento> listarDepartamentos() {
        return manager.getList("chat-confianca/persistencia/departamentos",
                new ParameterizedTypeReference<List<Departamento>>() {
                });
    }

    public Departamento buscarDepartamento(Long id) {
        Departamento departamento = manager.get("chat-confianca/persistencia/departamentos/" + id, Departamento.class);
        if (departamento == null) {
            throw regra(404, "Departamento nao encontrado.");
        }
        return departamento;
    }

    public Departamento salvarDepartamento(Departamento departamento) {
        validarObrigatorio(departamento, "Informe o departamento.");
        if (isBlank(departamento.getNome())) {
            throw regra(400, "Informe o nome do departamento.");
        }
        validarHierarquiaDepartamento(departamento);
        if (isBlank(departamento.getCodigo())) {
            departamento.setCodigo(gerarCodigoDepartamento(departamento));
        } else {
            departamento.setCodigo(gerarCodigo(departamento.getCodigo()));
        }
        if (departamento.getAtivo() == null) {
            departamento.setAtivo(true);
        }
        return manager.post("chat-confianca/persistencia/departamentos", departamento, Departamento.class);
    }

    public void excluirDepartamento(Long id) {
        validarObrigatorio(id, "Informe o departamento.");
        manager.delete("chat-confianca/persistencia/departamentos/" + id);
    }

    public List<DepartamentoUnidade> listarDepartamentoUnidades() {
        return manager.getList("chat-confianca/persistencia/departamento-unidades",
                new ParameterizedTypeReference<List<DepartamentoUnidade>>() {
                });
    }

    public List<DepartamentoUnidade> listarDepartamentoUnidadesPorUnidade(Integer codgUnidade) {
        validarObrigatorio(codgUnidade, "Informe a unidade.");
        return listarDepartamentoUnidades().stream()
                .filter(item -> Objects.equals(item.getCodgUnidade(), codgUnidade))
                .collect(Collectors.toList());
    }

    public List<DepartamentoUnidade> listarDepartamentoUnidadesPorDepartamento(Long departamentoId) {
        validarObrigatorio(departamentoId, "Informe o departamento.");
        return listarDepartamentoUnidades().stream()
                .filter(item -> Objects.equals(item.getDepartamentoId(), departamentoId))
                .collect(Collectors.toList());
    }

    public DepartamentoUnidade buscarDepartamentoUnidade(Long id) {
        DepartamentoUnidade entity = manager.get("chat-confianca/persistencia/departamento-unidades/" + id,
                DepartamentoUnidade.class);
        if (entity == null) {
            throw regra(404, "Departamento/unidade nao encontrado.");
        }
        return entity;
    }

    public DepartamentoUnidade salvarDepartamentoUnidade(DepartamentoUnidade entity) {
        validarObrigatorio(entity, "Informe o departamento/unidade.");
        validarObrigatorio(entity.getDepartamentoId(), "Informe o departamento.");
        validarObrigatorio(entity.getCodgUnidade(), "Informe a unidade.");
        validarDepartamentoOperacional(entity.getDepartamentoId());
        validarHorarioAtendimentoJson(entity);
        sincronizarUnidadeReferencia(entity.getCodgUnidade());
        aplicarPadroesDepartamentoUnidade(entity);
        return manager.post("chat-confianca/persistencia/departamento-unidades",
                entity, DepartamentoUnidade.class);
    }

    public List<DepartamentoUnidade> sincronizarDepartamentoUnidades(
            DepartamentoUnidadeSincronizacaoRequest request) {
        validarObrigatorio(request, "Informe os vinculos de unidade.");
        validarObrigatorio(request.getDepartamentoId(), "Informe o departamento.");
        validarDepartamentoOperacional(request.getDepartamentoId());

        Set<Integer> codigos = request.getCodigosUnidade() == null
                ? new LinkedHashSet<>()
                : request.getCodigosUnidade().stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Integer, RefUnidade> unidadesPorCodigo = listarUnidadesReferencia().stream()
                .filter(item -> item.getCodgUnidade() != null)
                .collect(Collectors.toMap(
                        RefUnidade::getCodgUnidade,
                        item -> item,
                        (atual, substituta) -> substituta,
                        LinkedHashMap::new));
        for (Integer codigo : codigos) {
            RefUnidade unidade = unidadesPorCodigo.get(codigo);
            if (unidade == null) {
                throw regra(400, "Unidade " + codigo + " nao encontrada no ConfiancaManager.");
            }
            completarUnidadeReferencia(unidade, codigo);
            manager.post("chat-confianca/persistencia/unidades", unidade, RefUnidade.class);
        }

        DepartamentoUnidade padrao = request.getConfiguracaoPadrao() == null
                ? new DepartamentoUnidade()
                : request.getConfiguracaoPadrao();
        padrao.setId(null);
        padrao.setDepartamentoId(request.getDepartamentoId());
        padrao.setCodgUnidade(null);
        validarHorarioAtendimentoJson(padrao);
        aplicarPadroesDepartamentoUnidade(padrao);
        request.setCodigosUnidade(new ArrayList<>(codigos));
        request.setConfiguracaoPadrao(padrao);

        return manager.postList(
                "chat-confianca/persistencia/departamento-unidades/sincronizar",
                request,
                new ParameterizedTypeReference<List<DepartamentoUnidade>>() {
                });
    }

    private void aplicarPadroesDepartamentoUnidade(DepartamentoUnidade entity) {
        if (entity.getPermiteChamadoAgencia() == null) {
            entity.setPermiteChamadoAgencia(true);
        }
        if (entity.getPermiteChamadoInterno() == null) {
            entity.setPermiteChamadoInterno(true);
        }
        if (entity.getExigeAssunto() == null) {
            entity.setExigeAssunto(false);
        }
        if (entity.getDistribuicao() == null) {
            entity.setDistribuicao(DistribuicaoDepartamento.MANUAL);
        }
        if (entity.getLimiteChatsPorAtendente() == null || entity.getLimiteChatsPorAtendente() <= 0) {
            entity.setLimiteChatsPorAtendente(3);
        }
        if (entity.getAtivo() == null) {
            entity.setAtivo(true);
        }
    }

    private void validarDepartamentoOperacional(Long departamentoId) {
        Departamento departamento = buscarDepartamento(departamentoId);
        if (Boolean.FALSE.equals(departamento.getAtivo())) {
            throw regra(400, "Departamento inativo nao pode receber unidades.");
        }
        if (possuiFilhosDepartamento(departamento.getId())) {
            throw regra(400, "Departamento agrupador nao recebe atendimento. Selecione um subdepartamento.");
        }
    }

    public void excluirDepartamentoUnidade(Long id) {
        validarObrigatorio(id, "Informe o departamento/unidade.");
        manager.delete("chat-confianca/persistencia/departamento-unidades/" + id);
    }

    public List<DepartamentoAtendente> listarDepartamentoAtendentes() {
        return manager.getList("chat-confianca/persistencia/departamento-atendentes",
                new ParameterizedTypeReference<List<DepartamentoAtendente>>() {
                });
    }

    public List<DepartamentoAtendente> listarAtendentesDepartamento(Long departamentoUnidadeId) {
        validarObrigatorio(departamentoUnidadeId, "Informe o departamento/unidade.");
        return manager.getList("chat-confianca/consultas/departamento-unidades/" + departamentoUnidadeId + "/atendentes",
                new ParameterizedTypeReference<List<DepartamentoAtendente>>() {
                });
    }

    public List<DepartamentoAtendente> listarDepartamentosAtendente(Integer codgUsuario) {
        validarObrigatorio(codgUsuario, "Informe o usuario.");
        return manager.getList("chat-confianca/consultas/atendentes/" + codgUsuario + "/departamentos",
                new ParameterizedTypeReference<List<DepartamentoAtendente>>() {
                });
    }

    public DepartamentoAtendente buscarDepartamentoAtendente(Long id) {
        DepartamentoAtendente entity = manager.get("chat-confianca/persistencia/departamento-atendentes/" + id,
                DepartamentoAtendente.class);
        if (entity == null) {
            throw regra(404, "Vinculo do atendente nao encontrado.");
        }
        return entity;
    }

    public DepartamentoAtendente salvarDepartamentoAtendente(DepartamentoAtendente entity) {
        validarObrigatorio(entity, "Informe o vinculo do atendente.");
        validarObrigatorio(entity.getDepartamentoUnidadeId(), "Informe o departamento/unidade.");
        validarObrigatorio(entity.getCodgUsuario(), "Informe o usuario atendente.");
        sincronizarUsuarioReferencia(entity.getCodgUsuario());
        buscarDepartamentoUnidade(entity.getDepartamentoUnidadeId());
        if (entity.getPapel() == null) {
            entity.setPapel(PapelAtendente.ATENDENTE);
        }
        if (entity.getRecebeChamados() == null) {
            entity.setRecebeChamados(true);
        }
        if (entity.getPrioridadeDistribuicao() == null || entity.getPrioridadeDistribuicao() <= 0) {
            entity.setPrioridadeDistribuicao(1);
        }
        if (entity.getAtivo() == null) {
            entity.setAtivo(true);
        }
        return manager.post("chat-confianca/persistencia/departamento-atendentes",
                entity, DepartamentoAtendente.class);
    }

    public List<DepartamentoAtendente> sincronizarDepartamentoAtendente(
            DepartamentoAtendenteSincronizacaoRequest request) {
        validarObrigatorio(request, "Informe os vinculos do atendente.");
        return manager.postList(
                "chat-confianca/persistencia/departamento-atendentes/sincronizar",
                request,
                new ParameterizedTypeReference<List<DepartamentoAtendente>>() {
                });
    }

    public void excluirDepartamentoAtendente(Long id) {
        validarObrigatorio(id, "Informe o vinculo do atendente.");
        manager.delete("chat-confianca/persistencia/departamento-atendentes/" + id);
    }

    public List<ChatPerfil> listarPerfis() {
        return manager.getList("chat-confianca/persistencia/perfis",
                new ParameterizedTypeReference<List<ChatPerfil>>() {
                });
    }

    public ChatPerfil buscarPerfil(Long id) {
        ChatPerfil perfil = manager.get("chat-confianca/persistencia/perfis/" + id, ChatPerfil.class);
        if (perfil == null) {
            throw regra(404, "Perfil nao encontrado.");
        }
        return perfil;
    }

    public ChatPerfil salvarPerfil(ChatPerfil perfil) {
        validarObrigatorio(perfil, "Informe o perfil.");
        if (isBlank(perfil.getNome())) {
            throw regra(400, "Informe o nome do perfil.");
        }
        if (isBlank(perfil.getCodigo())) {
            perfil.setCodigo(gerarCodigo(perfil.getNome()));
        } else {
            perfil.setCodigo(gerarCodigo(perfil.getCodigo()));
        }
        if (perfil.getAtivo() == null) {
            perfil.setAtivo(true);
        }
        return manager.post("chat-confianca/persistencia/perfis", perfil, ChatPerfil.class);
    }

    public void excluirPerfil(Long id) {
        validarObrigatorio(id, "Informe o perfil.");
        manager.delete("chat-confianca/persistencia/perfis/" + id);
    }

    public List<ChatUsuarioPerfil> listarUsuarioPerfis() {
        return manager.getList("chat-confianca/persistencia/usuario-perfis",
                new ParameterizedTypeReference<List<ChatUsuarioPerfil>>() {
                });
    }

    public List<ChatUsuarioPerfil> listarPerfisUsuario(Integer codgUsuario) {
        validarObrigatorio(codgUsuario, "Informe o usuario.");
        return listarUsuarioPerfis().stream()
                .filter(item -> Objects.equals(item.getCodgUsuario(), codgUsuario))
                .collect(Collectors.toList());
    }

    public ChatUsuarioPerfil buscarUsuarioPerfil(Long id) {
        ChatUsuarioPerfil entity = manager.get("chat-confianca/persistencia/usuario-perfis/" + id,
                ChatUsuarioPerfil.class);
        if (entity == null) {
            throw regra(404, "Perfil do usuario nao encontrado.");
        }
        return entity;
    }

    public ChatUsuarioPerfil salvarUsuarioPerfil(ChatUsuarioPerfil entity) {
        validarObrigatorio(entity, "Informe o perfil do usuario.");
        validarObrigatorio(entity.getCodgUsuario(), "Informe o usuario.");
        validarObrigatorio(entity.getPerfilId(), "Informe o perfil.");
        buscarPerfil(entity.getPerfilId());
        if (entity.getAtivo() == null) {
            entity.setAtivo(true);
        }
        if (entity.getAutomatico() == null) {
            entity.setAutomatico(false);
        }
        return manager.post("chat-confianca/persistencia/usuario-perfis", entity, ChatUsuarioPerfil.class);
    }

    public void excluirUsuarioPerfil(Long id) {
        validarObrigatorio(id, "Informe o perfil do usuario.");
        manager.delete("chat-confianca/persistencia/usuario-perfis/" + id);
    }

    public List<RespostaRapida> listarRespostasRapidas(Long departamentoId, Integer codgUnidade, Boolean somenteAtivas) {
        return manager.getList("chat-confianca/persistencia/respostas-rapidas",
                new ParameterizedTypeReference<List<RespostaRapida>>() {
                }).stream()
                .filter(item -> departamentoId == null || Objects.equals(item.getDepartamentoId(), departamentoId))
                .filter(item -> codgUnidade == null || item.getCodgUnidade() == null
                        || Objects.equals(item.getCodgUnidade(), codgUnidade))
                .filter(item -> !Boolean.TRUE.equals(somenteAtivas) || Boolean.TRUE.equals(item.getAtivo()))
                .sorted(Comparator.comparing(RespostaRapida::getTitulo, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    public RespostaRapida buscarRespostaRapida(Long id) {
        RespostaRapida entity = manager.get("chat-confianca/persistencia/respostas-rapidas/" + id,
                RespostaRapida.class);
        if (entity == null) {
            throw regra(404, "Resposta rapida nao encontrada.");
        }
        return entity;
    }

    public RespostaRapida salvarRespostaRapida(RespostaRapida entity) {
        validarObrigatorio(entity, "Informe a resposta rapida.");
        validarObrigatorio(entity.getDepartamentoId(), "Informe o departamento.");
        if (isBlank(entity.getTitulo())) {
            throw regra(400, "Informe o titulo da resposta rapida.");
        }
        if (isBlank(entity.getTexto())) {
            throw regra(400, "Informe o texto da resposta rapida.");
        }
        Departamento departamento = buscarDepartamento(entity.getDepartamentoId());
        if (possuiFilhosDepartamento(departamento.getId())) {
            throw regra(400, "Departamento agrupador nao recebe atendimento. Selecione um subdepartamento.");
        }
        if (entity.getAtivo() == null) {
            entity.setAtivo(true);
        }
        return manager.post("chat-confianca/persistencia/respostas-rapidas", entity, RespostaRapida.class);
    }

    public void excluirRespostaRapida(Long id) {
        validarObrigatorio(id, "Informe a resposta rapida.");
        manager.delete("chat-confianca/persistencia/respostas-rapidas/" + id);
    }

    public List<AtendenteStatus> listarAtendenteStatus() {
        return manager.getList("chat-confianca/persistencia/atendente-status",
                new ParameterizedTypeReference<List<AtendenteStatus>>() {
                });
    }

    public AtendenteStatus buscarAtendenteStatus(Integer codgUsuario) {
        AtendenteStatus status = manager.get("chat-confianca/persistencia/atendente-status/" + codgUsuario,
                AtendenteStatus.class);
        if (status == null) {
            throw regra(404, "Status do atendente nao encontrado.");
        }
        return status;
    }

    public AtendenteStatus salvarAtendenteStatus(AtendenteStatus status) {
        validarObrigatorio(status, "Informe o status do atendente.");
        validarObrigatorio(status.getCodgUsuario(), "Informe o usuario.");
        if (status.getStatus() == null) {
            status.setStatus(StatusAtendente.ONLINE);
        }
        if (status.getAtendimentosAtivos() == null) {
            status.setAtendimentosAtivos(0);
        }
        status.setUltimaAtividadeEm(LocalDateTime.now());
        return manager.post("chat-confianca/persistencia/atendente-status", status, AtendenteStatus.class);
    }

    public void excluirAtendenteStatus(Integer codgUsuario) {
        validarObrigatorio(codgUsuario, "Informe o usuario.");
        manager.delete("chat-confianca/persistencia/atendente-status/" + codgUsuario);
    }

    public List<ParametroSistema> listarParametros() {
        return manager.getList("chat-confianca/persistencia/parametros",
                new ParameterizedTypeReference<List<ParametroSistema>>() {
                });
    }

    public ParametroSistema salvarParametro(ParametroSistema parametro) {
        validarObrigatorio(parametro, "Informe o parametro.");
        if (isBlank(parametro.getChave())) {
            throw regra(400, "Informe a chave do parametro.");
        }
        return manager.post("chat-confianca/persistencia/parametros", parametro, ParametroSistema.class);
    }

    public void excluirParametro(String chave) {
        validarObrigatorio(chave, "Informe a chave do parametro.");
        manager.delete("chat-confianca/persistencia/parametros/" + chave);
    }

    public List<SlaPolitica> listarSlaPoliticas() {
        return manager.getList("chat-confianca/persistencia/sla-politicas",
                new ParameterizedTypeReference<List<SlaPolitica>>() {
                });
    }

    public SlaPolitica salvarSlaPolitica(SlaPolitica politica) {
        validarObrigatorio(politica, "Informe a politica de SLA.");
        validarObrigatorio(politica.getDepartamentoUnidadeId(), "Informe o departamento/unidade.");
        buscarDepartamentoUnidade(politica.getDepartamentoUnidadeId());
        if (politica.getAtivo() == null) {
            politica.setAtivo(true);
        }
        return manager.post("chat-confianca/persistencia/sla-politicas", politica, SlaPolitica.class);
    }

    public List<SlaPolitica> sincronizarSlaPoliticas(
            SlaPoliticaSincronizacaoRequest request) {
        validarObrigatorio(request, "Informe a politica de SLA.");
        return manager.postList(
                "chat-confianca/persistencia/sla-politicas/sincronizar",
                request,
                new ParameterizedTypeReference<List<SlaPolitica>>() {
                });
    }

    public void excluirSlaPolitica(Long id) {
        validarObrigatorio(id, "Informe a politica de SLA.");
        manager.delete("chat-confianca/persistencia/sla-politicas/" + id);
    }

    public RefUsuario buscarUsuarioReferenciaPorLogin(String login) {
        validarObrigatorio(login, "Informe o login do usuario.");
        return sincronizarUsuarioReferenciaPorLogin(login);
    }
    public List<RefUsuario> listarUsuariosReferencia() {
        return manager.getList("chat-confianca/persistencia/usuarios",
                new ParameterizedTypeReference<List<RefUsuario>>() {
                });
    }

    public List<RefAgencia> listarAgenciasReferencia() {
        return manager.getList("chat-confianca/persistencia/agencias",
                new ParameterizedTypeReference<List<RefAgencia>>() {
                });
    }

    public RefAgencia sincronizarAgenciaReferencia(Integer codgAgencia) {
        validarObrigatorio(codgAgencia, "Informe a agencia.");
        RefAgencia existente = manager.get("chat-confianca/persistencia/agencias/" + codgAgencia, RefAgencia.class);
        ManagerAgencia agencia = buscarAgenciaManagerPorCodigo(codgAgencia);
        if (agencia == null) {
            if (existente != null) {
                garantirUnidadeReferencia(existente.getCodgUnidade());
                return existente;
            }
            throw regra(404, "Agencia nao encontrada no ConfiancaManager.");
        }
        return salvarAgenciaReferencia(agencia);
    }
    public List<RefUnidade> listarUnidadesReferencia() {
        Map<Integer, RefUnidade> unidades = manager.getList(
                        "chat-confianca/persistencia/unidades",
                        new ParameterizedTypeReference<List<RefUnidade>>() {
                        }).stream()
                .filter(item -> item.getCodgUnidade() != null)
                .collect(Collectors.toMap(
                        RefUnidade::getCodgUnidade,
                        item -> item,
                        (atual, ignorada) -> atual,
                        LinkedHashMap::new));
        manager.getList("unidade",
                        new ParameterizedTypeReference<List<ManagerUnidade>>() {
                        }).stream()
                .map(this::toRefUnidade)
                .filter(item -> item.getCodgUnidade() != null)
                .forEach(item -> unidades.put(item.getCodgUnidade(), item));
        return unidades.values().stream()
                .sorted(Comparator.comparing(RefUnidade::getNomeUnidade, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    private RefUnidade toRefUnidade(ManagerUnidade unidade) {
        RefUnidade ref = new RefUnidade();
        ref.setCodgUnidade(unidade.getCodgUnidade());
        ref.setNomeUnidade(unidade.getNomeUnidade());
        ref.setCodgSistemaBackoffice(unidade.getCodgSistemaBackOffice());
        ref.setStatus(unidade.getStatus());
        ref.setIdWoobaUnidade(unidade.getIdWoobaUnidade());
        ref.setAtivoChat(unidade.getStatus() == null || Objects.equals(unidade.getStatus(), 1));
        return ref;
    }



    public RefUsuario sincronizarUsuarioReferencia(Integer codgUsuario) {
        RefUsuario existente = manager.get("chat-confianca/persistencia/usuarios/" + codgUsuario, RefUsuario.class);
        ManagerUsuario usuario = buscarUsuarioManagerPorCodigo(codgUsuario);
        if (usuario == null) {
            if (existente != null) {
                return existente;
            }
            throw regra(404, "Usuario nao encontrado no ConfiancaManager.");
        }
        return salvarUsuarioReferencia(usuario);
    }

    private RefUsuario sincronizarUsuarioReferenciaPorLogin(String login) {
        ManagerUsuario usuario = buscarUsuarioManagerPorLogin(login);
        if (usuario == null) {
            throw regra(404, "Usuario nao encontrado no ConfiancaManager para o login informado.");
        }
        return salvarUsuarioReferencia(usuario);
    }

    private ManagerUsuario buscarUsuarioManagerPorCodigo(Integer codgUsuario) {
        if (codgUsuario == null) {
            return null;
        }
        return consultarUsuariosManager("codgUsuario=" + codgUsuario).stream()
                .filter(item -> Objects.equals(item.getCodgUsuario(), codgUsuario))
                .findFirst()
                .orElse(null);
    }

    private ManagerUsuario buscarUsuarioManagerPorLogin(String login) {
        if (isBlank(login)) {
            return null;
        }
        String loginNormalizado = login.trim();
        String encoded = URLEncoder.encode(loginNormalizado, StandardCharsets.UTF_8);
        return consultarUsuariosManager("loginUsuario=" + encoded).stream()
                .filter(item -> item.getLoginUsuario() != null
                        && item.getLoginUsuario().equalsIgnoreCase(loginNormalizado))
                .findFirst()
                .orElse(null);
    }

    private List<ManagerUsuario> consultarUsuariosManager(String query) {
        return manager.getList("usuario?" + query,
                new ParameterizedTypeReference<List<ManagerUsuario>>() {
                });
    }

    private ManagerAgencia buscarAgenciaManagerPorCodigo(Integer codgAgencia) {
        if (codgAgencia == null) {
            return null;
        }
        return consultarAgenciasManager("codgAgencia=" + codgAgencia).stream()
                .filter(item -> Objects.equals(item.getCodgAgencia(), codgAgencia))
                .findFirst()
                .orElse(null);
    }

    private List<ManagerAgencia> consultarAgenciasManager(String query) {
        return manager.getList("agencia?" + query,
                new ParameterizedTypeReference<List<ManagerAgencia>>() {
                });
    }
    private RefUsuario salvarUsuarioReferencia(ManagerUsuario usuario) {
        RefUsuario ref = toRefUsuario(usuario);
        sincronizarDependenciasUsuario(usuario, ref);
        return manager.post("chat-confianca/persistencia/usuarios", ref, RefUsuario.class);
    }

    private void sincronizarDependenciasUsuario(ManagerUsuario usuario, RefUsuario ref) {
        ManagerUnidade unidade = unidadeUsuario(usuario);
        if (unidade != null && unidade.getCodgUnidade() != null) {
            salvarUnidadeReferencia(unidade);
        } else if (ref.getCodgUnidade() != null) {
            sincronizarUnidadeReferencia(ref.getCodgUnidade());
        }

        ManagerAgencia agencia = usuario.getAgencia();
        if (agencia != null && agencia.getCodgAgencia() != null) {
            RefAgencia refAgencia;
            if (ref.getCodgUnidade() != null) {
                refAgencia = toRefAgencia(agencia, ref.getCodgUnidade());
                manager.post("chat-confianca/persistencia/agencias", refAgencia, RefAgencia.class);
            } else {
                refAgencia = sincronizarAgenciaReferencia(agencia.getCodgAgencia());
                ref.setCodgUnidade(refAgencia.getCodgUnidade());
            }
            ref.setCodgAgencia(refAgencia.getCodgAgencia());
        } else {
            ref.setCodgAgencia(null);
        }
    }

    private RefUsuario toRefUsuario(ManagerUsuario usuario) {
        RefUsuario ref = new RefUsuario();
        ref.setCodgUsuario(usuario.getCodgUsuario());
        ref.setNomeCompleto(isBlank(usuario.getNomeCompleto())
                ? usuario.getLoginUsuario()
                : usuario.getNomeCompleto());
        if (isBlank(ref.getNomeCompleto())) {
            ref.setNomeCompleto("Usuario " + usuario.getCodgUsuario());
        }
        ref.setLoginUsuario(usuario.getLoginUsuario());
        ref.setEmail(usuario.getEmail());
        ref.setCpf(usuario.getCpf());
        ref.setTelefone(usuario.getTelefone());
        ref.setCelular(usuario.getCelular());
        ref.setTipoUsuario(usuario.getTipoUsuario());
        ref.setAdministradorAgencia(usuario.getAdministradorAgencia());
        ref.setStatus(usuario.getStatus());
        ref.setAtivoChat(usuario.getStatus() == null || Objects.equals(usuario.getStatus(), 1));
        ManagerUnidade unidade = unidadeUsuario(usuario);
        ref.setCodgUnidade(unidade == null ? null : unidade.getCodgUnidade());
        return ref;
    }

    private ManagerUnidade unidadeUsuario(ManagerUsuario usuario) {
        if (usuario.getUnidade() != null && usuario.getUnidade().getCodgUnidade() != null) {
            return usuario.getUnidade();
        }
        if (usuario.getAgencia() != null && usuario.getAgencia().getCodgUnidade() != null
                && usuario.getAgencia().getCodgUnidade().getCodgUnidade() != null) {
            return usuario.getAgencia().getCodgUnidade();
        }
        return null;
    }

    private RefAgencia salvarAgenciaReferencia(ManagerAgencia agencia) {
        ManagerUnidade unidade = agencia.getCodgUnidade();
        if (unidade == null || unidade.getCodgUnidade() == null) {
            throw regra(400, "Agencia nao possui unidade vinculada no ConfiancaManager.");
        }
        salvarUnidadeReferencia(unidade);
        RefAgencia ref = toRefAgencia(agencia, unidade.getCodgUnidade());
        return manager.post("chat-confianca/persistencia/agencias", ref, RefAgencia.class);
    }
    private void garantirUnidadeReferencia(Integer codgUnidade) {
        if (codgUnidade == null) {
            return;
        }
        RefUnidade existente = manager.get("chat-confianca/consultas/unidades/" + codgUnidade, RefUnidade.class);
        if (existente == null) {
            sincronizarUnidadeReferencia(codgUnidade);
        }
    }

    private RefAgencia toRefAgencia(ManagerAgencia agencia, Integer codgUnidade) {
        RefAgencia ref = new RefAgencia();
        ref.setCodgAgencia(agencia.getCodgAgencia());
        ref.setCodgUnidade(codgUnidade);
        ref.setNomeAgencia(isBlank(agencia.getNomeAgencia())
                ? "Agencia " + agencia.getCodgAgencia()
                : agencia.getNomeAgencia());
        ref.setCnpj(agencia.getCnpj());
        ref.setCodgSistemaBackoffice(agencia.getCodgSistemaBackOffice());
        ref.setStatus(agencia.getStatus());
        ref.setIdWoobaAgencia(agencia.getIdWoobaAgencia());
        ref.setAtivoChat(agencia.getStatus() == null || Objects.equals(agencia.getStatus(), 1));
        return ref;
    }

    private void salvarUnidadeReferencia(ManagerUnidade unidade) {
        RefUnidade ref = toRefUnidade(unidade);
        completarUnidadeReferencia(ref, unidade.getCodgUnidade());
        manager.post("chat-confianca/persistencia/unidades", ref, RefUnidade.class);
    }

    private void completarUnidadeReferencia(RefUnidade unidade, Integer codgUnidade) {
        if (isBlank(unidade.getNomeUnidade())) {
            unidade.setNomeUnidade("Unidade " + codgUnidade);
        }
        if (unidade.getAtivoChat() == null) {
            unidade.setAtivoChat(true);
        }
    }
    private void validarHorarioAtendimentoJson(DepartamentoUnidade entity) {
        String horarioJson = entity.getHorarioAtendimentoJson();
        if (isBlank(horarioJson)) {
            entity.setHorarioAtendimentoJson(null);
            return;
        }
        try {
            objectMapper.readTree(horarioJson);
            entity.setHorarioAtendimentoJson(horarioJson.trim());
        } catch (JsonProcessingException ex) {
            throw regra(400, "Horario JSON deve ser um JSON valido. Deixe em branco ou informe um objeto/array JSON.");
        }
    }

    private void sincronizarUnidadeReferencia(Integer codgUnidade) {
        RefUnidade unidade = buscarUnidadeReferencia(codgUnidade);
        completarUnidadeReferencia(unidade, codgUnidade);
        manager.post("chat-confianca/persistencia/unidades", unidade, RefUnidade.class);
    }

    private RefUnidade buscarUnidadeReferencia(Integer codgUnidade) {
        return listarUnidadesReferencia().stream()
                .filter(item -> Objects.equals(item.getCodgUnidade(), codgUnidade))
                .findFirst()
                .orElseThrow(() -> regra(400, "Unidade nao encontrada no ConfiancaManager."));
    }
    private String gerarCodigoDepartamento(Departamento departamento) {
        String codigo = gerarCodigo(departamento.getNome());
        if (departamento.getDepartamentoPaiId() == null) {
            return codigo;
        }
        Departamento pai = buscarDepartamento(departamento.getDepartamentoPaiId());
        return gerarCodigo(pai.getCodigo() + "_" + codigo);
    }

    private void validarHierarquiaDepartamento(Departamento departamento) {
        Long departamentoPaiId = departamento.getDepartamentoPaiId();
        if (departamentoPaiId == null) {
            return;
        }
        if (Objects.equals(departamento.getId(), departamentoPaiId)) {
            throw regra(400, "Departamento nao pode ser pai dele mesmo.");
        }

        Departamento pai = buscarDepartamento(departamentoPaiId);
        Set<Long> visitados = new HashSet<>();
        while (pai.getDepartamentoPaiId() != null) {
            Long proximoPaiId = pai.getDepartamentoPaiId();
            if (Objects.equals(departamento.getId(), proximoPaiId)) {
                throw regra(400, "Hierarquia de departamento geraria ciclo.");
            }
            if (!visitados.add(proximoPaiId)) {
                throw regra(400, "Hierarquia de departamento invalida.");
            }
            pai = buscarDepartamento(proximoPaiId);
        }
    }

    private boolean possuiFilhosDepartamento(Long departamentoId) {
        if (departamentoId == null) {
            return false;
        }
        return listarDepartamentos().stream()
                .anyMatch(item -> Objects.equals(item.getDepartamentoPaiId(), departamentoId));
    }
    private String gerarCodigo(String value) {
        return value.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private void validarObrigatorio(Object valor, String mensagem) {
        if (valor == null) {
            throw regra(400, mensagem);
        }
    }

    private boolean isBlank(String valor) {
        return valor == null || valor.isBlank();
    }

    private RegraDeNegocioException regra(int status, String mensagem) {
        return new RegraDeNegocioException(status, mensagem);
    }
}
