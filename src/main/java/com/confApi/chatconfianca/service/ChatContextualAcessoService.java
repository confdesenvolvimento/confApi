package com.confApi.chatconfianca.service;

import com.confApi.chatconfianca.client.ChatConfiancaManagerClient;
import com.confApi.chatconfianca.dto.model.DepartamentoAtendente;
import com.confApi.chatconfianca.dto.model.DepartamentoUnidade;
import com.confApi.chatconfianca.dto.model.RefAgencia;
import com.confApi.chatconfianca.dto.model.RefUnidade;
import com.confApi.chatconfianca.dto.model.RefUsuario;
import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.exception.RegraDeNegocioException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

/** Guarda adicional fail-closed dos novos fluxos; nao modifica a autorizacao legada. */
@Service
public class ChatContextualAcessoService {
    private final ChatConfiancaManagerClient manager;
    private final ChatConfiancaConfigService config;

    public ChatContextualAcessoService(ChatConfiancaManagerClient manager, ChatConfiancaConfigService configService) {
        this.manager = manager;
        this.config = configService;
    }

    public void validarSessao(SessaoChatResponse sessao, Integer codgUsuario, Integer agenciaSolicitada) {
        try {
            if (sessao == null || !positivo(codgUsuario) || sessao.getUsuario() == null
                    || sessao.getAgencia() == null || sessao.getUnidade() == null) throw negado();
            RefUsuario informado = sessao.getUsuario();
            RefAgencia agenciaInformada = sessao.getAgencia();
            RefUnidade unidadeInformada = sessao.getUnidade();
            if (!Objects.equals(codgUsuario, informado.getCodgUsuario())
                    || !positivo(agenciaInformada.getCodgAgencia()) || !positivo(unidadeInformada.getCodgUnidade())
                    || !Boolean.TRUE.equals(informado.getAtivoChat())
                    || !Boolean.TRUE.equals(agenciaInformada.getAtivoChat())
                    || !Boolean.TRUE.equals(unidadeInformada.getAtivoChat())
                    || !Objects.equals(agenciaInformada.getCodgUnidade(), unidadeInformada.getCodgUnidade())
                    || agenciaSolicitada != null && !Objects.equals(agenciaSolicitada, agenciaInformada.getCodgAgencia()))
                throw negado();

            RefUsuario usuario = config.buscarUsuarioReferencia(codgUsuario);
            RefAgencia agencia = manager.get("chat-confianca/consultas/agencias/" + agenciaInformada.getCodgAgencia(), RefAgencia.class);
            RefUnidade unidade = manager.get("chat-confianca/consultas/unidades/" + unidadeInformada.getCodgUnidade(), RefUnidade.class);
            if (usuario == null || agencia == null || unidade == null
                    || !Boolean.TRUE.equals(usuario.getAtivoChat()) || !Boolean.TRUE.equals(agencia.getAtivoChat())
                    || !Boolean.TRUE.equals(unidade.getAtivoChat()) || !Objects.equals(usuario.getCodgUsuario(), codgUsuario)
                    || !Objects.equals(usuario.getCodgAgencia(), informado.getCodgAgencia())
                    || !Objects.equals(usuario.getCodgUnidade(), informado.getCodgUnidade())
                    || !Objects.equals(agencia.getCodgAgencia(), agenciaInformada.getCodgAgencia())
                    || !Objects.equals(agencia.getCodgSistemaBackoffice(), agenciaInformada.getCodgSistemaBackoffice())
                    || !Objects.equals(unidade.getCodgUnidade(), unidadeInformada.getCodgUnidade())
                    || !Objects.equals(agencia.getCodgUnidade(), unidade.getCodgUnidade())) throw negado();

            if (usuario.getCodgAgencia() != null) {
                if (!Objects.equals(usuario.getCodgAgencia(), agencia.getCodgAgencia())) throw negado();
                return;
            }
            if (positivo(usuario.getCodgUnidade()) && Objects.equals(usuario.getCodgUnidade(), unidade.getCodgUnidade())) return;
            List<String> globais = manager.getList("chat-confianca/consultas/usuarios/" + codgUsuario + "/perfis",
                    new ParameterizedTypeReference<List<String>>() { });
            if (globais != null && globais.stream().filter(Objects::nonNull)
                    .map(x -> x.trim().toUpperCase(Locale.ROOT)).anyMatch(x -> "ADMIN".equals(x) || "ADMIN_CHAT".equals(x))) return;

            List<DepartamentoAtendente> vinculos = manager.getList("chat-confianca/consultas/atendentes/" + codgUsuario + "/departamentos",
                    new ParameterizedTypeReference<List<DepartamentoAtendente>>() { });
            List<DepartamentoUnidade> departamentos = config.listarDepartamentoUnidadesPorUnidade(unidade.getCodgUnidade());
            if (vinculos != null && departamentos != null) for (DepartamentoAtendente vinculo : vinculos) {
                if (vinculo == null || !Objects.equals(codgUsuario, vinculo.getCodgUsuario())
                        || !Boolean.TRUE.equals(vinculo.getAtivo()) || vinculo.getDepartamentoUnidadeId() == null) continue;
                for (DepartamentoUnidade departamento : departamentos)
                    if (departamento != null && Boolean.TRUE.equals(departamento.getAtivo())
                            && Objects.equals(unidade.getCodgUnidade(), departamento.getCodgUnidade())
                            && Objects.equals(vinculo.getDepartamentoUnidadeId(), departamento.getId())) return;
            }
            throw negado();
        } catch (RuntimeException ex) {
            // Erro de integracao, referencia ausente e falta de permissao nunca concedem acesso.
            throw negado();
        }
    }

    private static boolean positivo(Integer id) { return id != null && id > 0; }
    private static RegraDeNegocioException negado() {
        return new RegraDeNegocioException(403, "Nao foi possivel confirmar a permissao da sessao para este recurso do assistente.");
    }
}
