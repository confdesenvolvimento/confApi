package com.confApi.chatconfianca.indicadores;

import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.chatconfianca.reservas.ChatReservaConsultaTexto;
import com.confApi.chatconfianca.reservas.ReservaConsultaEstado;
import com.confApi.chatconfianca.reservas.ReservaConsultaFiltros;
import com.confApi.chatconfianca.reservas.ReservaConsultaPagina;
import com.confApi.chatconfianca.service.ChatContextualAcessoService;
import com.confApi.chatgpt.dto.ChatMessageDTO;
import com.confApi.chatgpt.dto.ChatResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Consulta factual autorizada, sem LLM, cache compartilhado ou execucao de operacoes comerciais. */
@Service
public class ChatVendaAereaIndicadoresService {
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final ChatVendaAereaIndicadoresClient client;
    private final ChatContextualAcessoService acesso;
    private final ObjectMapper mapper;

    public ChatVendaAereaIndicadoresService(ChatVendaAereaIndicadoresClient client,
            ChatContextualAcessoService acesso, ObjectMapper mapper) {
        this.client = client; this.acesso = acesso; this.mapper = mapper;
    }

    public ChatResponseDTO consultar(VendaAereaIndicadoresFiltros recebidos, SessaoChatResponse sessao,
            Integer codgUsuario, Integer agenciaSolicitada, Long conversaId) {
        VendaAereaIndicadoresFiltros filtros;
        try { filtros = ChatVendaAereaIndicadoresClient.validarFiltros(recebidos); }
        catch (IllegalArgumentException ex) {
            return responder("Informe um período válido de até 366 dias para consultar as vendas aéreas, pela data de emissão da reserva.",
                    "CONSULTA_BLOQUEADA", null, conversaId, codgUsuario, null);
        }
        try {
            if (conversaId == null || conversaId <= 0 || codgUsuario == null || codgUsuario <= 0
                    || sessao == null || sessao.getAgencia() == null || sessao.getAgencia().getCodgAgencia() == null
                    || sessao.getAgencia().getCodgAgencia() <= 0) throw new IllegalArgumentException("Sessao ausente.");
            acesso.validarSessao(sessao, codgUsuario, agenciaSolicitada);
        } catch (RuntimeException ex) {
            return responder("Não consegui confirmar sua permissão para consultar as vendas desta agência. Nenhuma busca foi realizada.",
                    "CONSULTA_BLOQUEADA", null, conversaId, codgUsuario, null);
        }
        Integer agencia = sessao.getAgencia().getCodgAgencia();
        try {
            var resultado = client.consultar(agencia, codgUsuario, filtros);
            return responder(renderizar(resultado), "SEM_RESULTADO".equals(resultado.getStatus())
                    ? "SEM_RESULTADO" : "DADOS_CONSULTADOS", resultado, conversaId, codgUsuario, agencia);
        } catch (RuntimeException ex) {
            // Nunca registrar filtros, nomes, dados da reserva nem o corpo retornado pelo fornecedor.
            java.util.logging.Logger.getLogger(getClass().getName()).warning("VENDAS_AEREAS_CONSULTA_ERRO tipo=" + ex.getClass().getSimpleName());
            return responder("Não consegui concluir a consulta das vendas aéreas agora. Isso não significa que o resultado seja zero. Tente novamente ou solicite atendimento.",
                    "ERRO_INTEGRACAO", null, conversaId, codgUsuario, agencia);
        }
    }

    private ChatResponseDTO responder(String texto, String status, VendaAereaIndicadoresResultado r,
            Long conversa, Integer usuario, Integer agencia) {
        try {
            ObjectNode payload = mapper.createObjectNode();
            payload.put("schema", VendaAereaIndicadoresResultado.SCHEMA);
            payload.put("statusConsulta", status); payload.put("resultadoTarefa", status);
            if (r != null) {
                ObjectNode contexto = payload.putObject("contexto");
                contexto.put("conversa", conversa); contexto.put("agencia", agencia); contexto.put("usuario", usuario);
                contexto.put("atualizadoEm", System.currentTimeMillis());
                contexto.set("filtros", mapper.valueToTree(r.getFiltrosAplicados()));
                payload.set("totais", mapper.valueToTree(r.getTotais()));
                payload.set("totaisPorMoeda", mapper.valueToTree(r.getTotaisPorMoeda()));
                payload.set("grupos", mapper.valueToTree(r.getGrupos()));
                payload.put("gruposTruncados", r.isGruposTruncados());
                payload.put("page", r.getPage()); payload.put("size", r.getSize()); payload.put("hasNext", r.isHasNext());
                payload.put("totalPages", r.getTotalPages()); payload.put("criterioContagem", r.getCriterioContagem());
                payload.put("totalDetalhes", r.getTotalDetalhes());
                payload.put("criterioValor", r.getCriterioValor()); payload.put("reembolsosDisponiveis", false);
                payload.put("statusValor", r.getStatusValor()); payload.put("motivoValor", r.getMotivoValor());
                payload.put("composicaoValor", r.getComposicaoValor());
                payload.put("passageirosValorIncompleto", r.getPassageirosValorIncompleto());
                payload.set("limitacoes", mapper.valueToTree(r.getLimitacoes()));
            }
            var historico = new ArrayList<ChatMessageDTO>();
            historico.add(new ChatMessageDTO("system", mapper.writeValueAsString(payload)));
            // Toda consulta de indicadores encerra a selecao de uma lista de reservas anterior.
            // Uma nova referencia so nasce de uma pagina factual de emitidas com estes mesmos filtros.
            ObjectNode reservas = mapper.createObjectNode();
            reservas.put("schema", ReservaConsultaPagina.SCHEMA);
            if (r != null && r.getFiltrosAplicados().isDetalhar()) {
                var pagina = pagina(r);
                var estado = new ReservaConsultaEstado();
                estado.setConversa(conversa); estado.setAgencia(agencia.longValue()); estado.setUsuario(usuario.longValue());
                estado.setAtualizadoEm(System.currentTimeMillis()); estado.setFiltros(pagina.getFiltrosAplicados());
                estado.setItens(pagina.getReservas());
                if (pagina.getTotalReservas() == 1 && pagina.getReservas().size() == 1)
                    estado.setSelecionadaId(pagina.getReservas().get(0).getReservaId());
                reservas.put("statusConsulta", pagina.getTotalReservas() == 0 ? "SEM_RESULTADO" : "DADOS_CONSULTADOS");
                reservas.put("resultadoTarefa", reservas.path("statusConsulta").asText());
                reservas.set("contexto", mapper.valueToTree(estado));
                reservas.put("totalReservas", pagina.getTotalReservas()); reservas.put("totalLocalizadores", pagina.getTotalLocalizadores());
                reservas.put("page", pagina.getPage()); reservas.put("size", pagina.getSize());
                reservas.put("hasNext", pagina.isHasNext()); reservas.put("criterioContagem", pagina.getCriterioContagem());
            } else {
                reservas.put("statusConsulta", "CONSULTA_BLOQUEADA"); reservas.put("resultadoTarefa", "CONSULTA_BLOQUEADA");
            }
            historico.add(new ChatMessageDTO("system", mapper.writeValueAsString(reservas)));
            return new ChatResponseDTO(null, texto, List.of(), null, List.of("aereo.vendas_indicadores"), historico, List.of());
        } catch (Exception ex) { throw new IllegalStateException("Falha ao preparar resposta factual de vendas.", ex); }
    }

    static String renderizar(VendaAereaIndicadoresResultado r) {
        var f = r.getFiltrosAplicados(); var t = r.getTotais();
        var b = new StringBuilder("Vendas aéreas da sua agência — emissão da reserva de ")
                .append(DIA.format(f.getInicio())).append(" a ").append(DIA.format(f.getFim())).append('.');
        filtro(b, "Origem", f.getOrigem()); filtro(b, "Destino", f.getDestino());
        filtro(b, "Companhia", f.getCompanhia()); filtro(b, "Passageiro", f.getNomePassageiro());
        b.append("\n\nReservas emitidas e ainda ativas: ").append(t.getReservasEmitidas())
                .append(".\nLocalizadores distintos dessas reservas: ").append(t.getLocalizadoresEmitidos())
                .append(".\nRegistros de passageiros nessas reservas: ").append(t.getPassageirosEmitidos())
                .append(".\nBilhetes ativos: ").append(t.getBilhetesAtivos()).append('.');
        b.append("\n\nSeparadamente, na mesma base de emissão: ").append(t.getReservasCanceladas())
                .append(" reserva(s) atualmente cancelada(s), ").append(t.getBilhetesCancelados())
                .append(" bilhete(s) com cancelamento registrado, ").append(t.getReservasOutrasSituacoes())
                .append(" reserva(s) e ").append(t.getBilhetesOutrasSituacoes()).append(" bilhete(s) em outras situações.");
        if (!r.getGrupos().isEmpty()) {
            b.append("\n\nPor companhia da reserva:");
            for (var grupo : r.getGrupos()) {
                b.append("\n- ").append(seguro(grupo.getDescricao())).append(": ")
                    .append(grupo.getTotais().getReservasEmitidas()).append(" reserva(s) emitida(s) ativa(s), ")
                    .append(grupo.getTotais().getLocalizadoresEmitidos()).append(" localizador(es), ")
                    .append(grupo.getTotais().getPassageirosEmitidos()).append(" registro(s) de passageiro e ")
                    .append(grupo.getTotais().getBilhetesAtivos()).append(" bilhete(s) ativo(s).");
                if ("COMPLETO".equals(r.getStatusValor()) && VendaAereaIndicadoresResultado.COMPOSICAO.equals(r.getComposicaoValor())
                        && "COMPLETO".equals(grupo.getStatusValor()) && VendaAereaIndicadoresResultado.COMPOSICAO.equals(grupo.getComposicaoValor()))
                    for (var m : grupo.getTotaisPorMoeda()) b.append(" Valor de vendas: ").append(valor(m)).append('.');
            }
            if (r.isGruposTruncados()) b.append("\nA lista de companhias foi limitada; os totais gerais continuam considerando todos os resultados.");
            if ("COMPLETO".equals(r.getStatusValor()) && !r.getTotaisPorMoeda().isEmpty())
                b.append("\nPor arredondamento de exibição, a soma dos grupos pode diferir em centavos do total calculado na precisão original.");
        }
        b.append("\n\n").append(textoValores(r));
        b.append("\nCritério: data de emissão cadastrada na reserva e situação atual, não um histórico de cada emissão ou reemissão. Passageiros são registros por reserva, não pessoas únicas. Totais calculados sobre todos os resultados, não só a página exibida.");
        if (f.isDetalhar()) b.append("\n\nDetalhamento das reservas emitidas e ainda ativas:\n")
                .append(ChatReservaConsultaTexto.renderizar(pagina(r), "DETALHES"));
        else b.append("\nVocê pode pedir para listar essas vendas, separar por companhia ou procurar um passageiro nesses resultados.");
        return b.toString();
    }

    private static ReservaConsultaPagina pagina(VendaAereaIndicadoresResultado r) {
        var f = r.getFiltrosAplicados(); var filtros = new ReservaConsultaFiltros();
        filtros.setInicio(f.getInicio()); filtros.setFim(f.getFim()); filtros.setOrigem(f.getOrigem());
        filtros.setDestino(f.getDestino()); filtros.setCompanhia(f.getCompanhia()); filtros.setNomePassageiro(f.getNomePassageiro());
        filtros.setSituacao("EMITIDAS"); filtros.setTipoData("EMISSAO"); filtros.setPage(r.getPage()); filtros.setSize(r.getSize());
        var p = new ReservaConsultaPagina(); p.setSchema(ReservaConsultaPagina.SCHEMA);
        p.setCodgAgencia(r.getCodgAgencia()); p.setFiltrosAplicados(filtros); p.setReservas(r.getReservas());
        p.setTotalReservas(r.getTotais().getReservasEmitidas()); p.setTotalLocalizadores(r.getTotais().getLocalizadoresEmitidos());
        p.setStatus(p.getTotalReservas() == 0 ? "SEM_RESULTADO" : "OK");
        p.setPage(r.getPage()); p.setSize(r.getSize()); p.setTotalPages(r.getTotalPages()); p.setHasNext(r.isHasNext());
        p.setCriterioContagem(r.getCriterioContagem()); return p;
    }

    private static void filtro(StringBuilder b, String nome, String valor) {
        if (valor != null) b.append("\n").append(nome).append(": ").append(seguro(valor)).append('.');
    }
    private static String textoValores(VendaAereaIndicadoresResultado r) {
        if ("COMPLETO".equals(r.getStatusValor()) && VendaAereaIndicadoresResultado.COMPOSICAO.equals(r.getComposicaoValor())) {
            var b = new StringBuilder("Valor de vendas (composição do dashboard):");
            if (r.getTotais().getReservasEmitidas() == 0) b.append(" não há reservas emitidas ativas neste recorte. Nenhuma moeda foi presumida.");
            else for (var m : r.getTotaisPorMoeda()) b.append("\n- ").append(valor(m));
            b.append("\nComposição: tarifa + taxa de embarque + DU + RAV + RC + combustível. Moedas apresentadas separadamente, sem conversão; não representa lucro, reembolso ou valor líquido.");
            if (r.getTotaisPorMoeda().stream().anyMatch(m -> m.getValorBrutoEmitido() != null && m.getValorBrutoEmitido().signum() < 0))
                b.append("\nHá valor negativo cadastrado; isso não confirma reembolso ou saldo líquido.");
            return b.toString();
        }
        if ("INCOMPLETO".equals(r.getStatusValor())) return "Não foi possível confirmar o valor total: existem dados de valores ou moedas incompletos ou ambíguos. Nenhuma soma parcial foi apresentada como total. As quantidades acima continuam disponíveis.";
        if ("INDISPONIVEL_SEM_PERMISSAO".equals(r.getStatusValor())) return "Seu perfil não possui a permissão do painel de análise de vendas para consultar valores. Mostrei apenas as quantidades, sem dados monetários.";
        if ("NAO_SOLICITADO".equals(r.getMotivoValor())) return "A consulta retornou somente quantidades; valores monetários não foram solicitados. Não estimei faturamento ou valor líquido.";
        return "Valores monetários não estão disponíveis agora. Não apresentei valor zero nem estimei faturamento, reembolsos ou valor líquido.";
    }

    private static String valor(VendaAereaIndicadoresResultado.MedidaValor m) {
        if (m == null || !m.isCompleto() || m.getValorBrutoEmitido() == null) throw new IllegalArgumentException("Valor nao comprovado.");
        var nf = java.text.NumberFormat.getNumberInstance(new java.util.Locale("pt", "BR"));
        nf.setMinimumFractionDigits(2); nf.setMaximumFractionDigits(2); nf.setRoundingMode(java.math.RoundingMode.HALF_UP);
        return m.getMoeda() + " " + nf.format(m.getValorBrutoEmitido());
    }
    private static String seguro(String valor) {
        return valor == null ? "" : valor.replaceAll("[\\p{Cntrl}]", " ").replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;").replace("[", "\\[").replace("]", "\\]")
                .replace("*", "\\*").replace("`", "\\`");
    }
}
