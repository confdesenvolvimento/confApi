package com.confApi.chatconfianca.operacional;

import com.confApi.chatconfianca.dto.model.Mensagem;
import com.confApi.chatconfianca.dto.request.PerguntarConfiaRequest;
import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.chatconfianca.service.ChatContextualAcessoService;
import com.confApi.chatgpt.dto.ChatMessageDTO;
import com.confApi.chatgpt.dto.ChatResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import static com.confApi.chatconfianca.operacional.ChatResumoOperacionalExecutor.Consulta;

/** Resumo factual compartilhado HUB/mobile. Nao chama LLM e nao executa operacoes comerciais. */
@Service
public class ChatResumoOperacionalService {
    private final ChatContextualAcessoService acesso;
    private final ChatResumoOperacionalAereoService aereo;
    private final ChatResumoOperacionalHotelService hotel;
    private final ChatResumoOperacionalFinanceiroService financeiro;
    private final ChatResumoOperacionalExecutor executor;
    private final ObjectMapper mapper;
    private Clock clock = Clock.system(ZoneId.of("America/Cuiaba"));
    @Value("${chat-confianca.resumo-operacional.enabled:false}")
    private boolean enabled = false;
    @Value("${chat-confianca.resumo-operacional.aereo-enabled:false}")
    private boolean aereoEnabled = false;
    @Value("${chat-confianca.resumo-operacional.hotel-enabled:false}")
    private boolean hotelEnabled = false;
    @Value("${chat-confianca.resumo-operacional.financeiro-enabled:false}")
    private boolean financeiroEnabled = false;

    public ChatResumoOperacionalService(ChatContextualAcessoService acesso, ChatResumoOperacionalAereoService aereo,
            ChatResumoOperacionalHotelService hotel, ChatResumoOperacionalFinanceiroService financeiro,
            ChatResumoOperacionalExecutor executor, ObjectMapper mapper) {
        this.acesso = acesso; this.aereo = aereo; this.hotel = hotel;
        this.financeiro = financeiro; this.executor = executor; this.mapper = mapper;
    }

    public boolean podeAtender(PerguntarConfiaRequest req, SessaoChatResponse sessao, List<Mensagem> historico) {
        return pedido(req, sessao, historico) != null;
    }

    public ChatResponseDTO tentar(PerguntarConfiaRequest req, SessaoChatResponse sessao, Long conversa, List<Mensagem> historico) {
        var p = pedido(req, sessao, historico);
        if (p == null) return null;
        if (!enabled) return resposta(req, sessao, conversa, p, "CAPACIDADE_INDISPONIVEL",
                "O resumo operacional ainda não está habilitado neste ambiente.", List.of());
        if (conversa == null || conversa < 1 || agencia(sessao) == null || req.getCodgUsuario() == null)
            return resposta(req, sessao, conversa, p, "CONSULTA_BLOQUEADA",
                    "Não consegui validar a agência e a conversa para preparar o resumo.", List.of());
        if (p.pergunta() != null) return resposta(req, sessao, conversa, p, "AGUARDANDO_DADOS", p.pergunta(), List.of());
        boolean algumaFonteHabilitada=switch(p.foco()) {
            case "EMBARQUES","PRAZOS" -> aereoEnabled;
            case "HOTEL","ENTRADAS","SAIDAS" -> hotelEnabled;
            case "FINANCEIRO" -> financeiroEnabled;
            default -> aereoEnabled || hotelEnabled || financeiroEnabled;
        };
        if (!algumaFonteHabilitada) return resposta(req,sessao,conversa,p,"CAPACIDADE_INDISPONIVEL",
                "As fontes necessárias para este resumo não estão habilitadas neste ambiente. Nenhuma ausência de pendência foi inferida.",List.of());
        try { acesso.validarSessao(sessao, req.getCodgUsuario(), req.getCodgAgenciaSessao()); }
        catch (RuntimeException ex) { return bloqueada(req, sessao, conversa, p); }
        List<Consulta> chamadas = new ArrayList<>();
        if (Set.of("TODOS", "EMBARQUES").contains(p.foco())) chamadas.add(new Consulta("EMBARQUES_AEREOS", "Embarques aéreos cadastrados",
                () -> aereo.consultarEmbarques(sessao, p.inicio(), p.fim())));
        if (Set.of("TODOS", "PRAZOS").contains(p.foco())) chamadas.add(new Consulta("PRAZOS_EMISSAO_AEREOS", "Prazos de emissão cadastrados",
                () -> aereo.consultarPrazos(sessao, p.inicio(), p.fim())));
        if (Set.of("TODOS", "HOTEL", "ENTRADAS").contains(p.foco())) chamadas.add(new Consulta("ENTRADAS_HOTEL", "Entradas de hotel cadastradas",
                () -> hotel.consultarEntradas(sessao, p.inicio(), p.fim())));
        if (Set.of("TODOS", "HOTEL", "SAIDAS").contains(p.foco())) chamadas.add(new Consulta("SAIDAS_HOTEL", "Saídas de hotel cadastradas",
                () -> hotel.consultarSaidas(sessao, p.inicio(), p.fim())));
        if (Set.of("TODOS", "FINANCEIRO").contains(p.foco())) chamadas.add(new Consulta("VENCIMENTOS_FATURAS", "Vencimentos de faturas",
                () -> financeiro.consultarVencimentos(sessao, p.inicio(), p.fim())));
        var fontes = executor.consultar(chamadas);
        // Revogacao/troca de vinculo durante IO descarta todo resultado, nao apenas a fonte financeira.
        try { acesso.validarSessao(sessao, req.getCodgUsuario(), req.getCodgAgenciaSessao()); }
        catch (RuntimeException ex) { return bloqueada(req, sessao, conversa, p); }
        boolean algum = fontes.stream().anyMatch(f -> f.status() == ChatResumoOperacionalFonte.Status.DISPONIVEL
                || f.status() == ChatResumoOperacionalFonte.Status.INCOMPLETO);
        String status = !algum ? "ERRO_INTEGRACAO" : fontes.stream().allMatch(f -> f.status() == ChatResumoOperacionalFonte.Status.DISPONIVEL)
                ? "DADOS_CONSULTADOS" : "DADOS_PARCIAIS";
        return resposta(req, sessao, conversa, p, status, renderizar(p, fontes), fontes);
    }

    private ChatResponseDTO bloqueada(PerguntarConfiaRequest r, SessaoChatResponse s, Long c, ChatResumoOperacionalParser.Pedido p) {
        return resposta(r, s, c, p, "CONSULTA_BLOQUEADA", "Não foi possível confirmar a permissão desta sessão. Nenhum dado operacional foi apresentado.", List.of());
    }

    private ChatResumoOperacionalParser.Pedido pedido(PerguntarConfiaRequest r, SessaoChatResponse s, List<Mensagem> h) {
        if (r == null) return null;
        boolean recente = ChatResumoOperacionalContexto.recente(h, r.getConversaId(), r.getCodgUsuario(), agencia(s), mapper);
        var p=ChatResumoOperacionalParser.interpretar(r.getMensagem(), recente, clock);
        var anterior=ChatResumoOperacionalContexto.recuperar(h,r.getConversaId(),r.getCodgUsuario(),agencia(s),mapper);
        String t=ChatResumoOperacionalParser.normalizar(r.getMensagem());
        if (p!=null && p.pergunta()==null && recente && anterior==null
                && !t.contains("resumo") && !t.contains("acompanhar") && !t.contains("pendencias"))
            return new ChatResumoOperacionalParser.Pedido(null,null,"TODOS",
                    "Confirme o bloco e o período no pedido completo, por exemplo: ‘resumo operacional de faturas de amanhã’. Não mantive um período da pergunta que precisava de esclarecimento.");
        // Pergunta explicita reinicia criterios. Seguimento curto muda apenas o criterio informado.
        if (p!=null && p.pergunta()==null && anterior!=null && !t.contains("resumo")
                && !t.contains("acompanhar") && !t.contains("pendencias")) {
            boolean mudouData=t.matches(".*\\b(?:hoje|amanha|proximos)\\b.*");
            p=new ChatResumoOperacionalParser.Pedido(mudouData?p.inicio():anterior.inicio(),
                    mudouData?p.fim():anterior.fim(),mudouData&&"TODOS".equals(p.foco())?anterior.foco():p.foco(),null);
        }
        return p;
    }
    private static Integer agencia(SessaoChatResponse s) {
        return s == null || s.getAgencia() == null ? null : s.getAgencia().getCodgAgencia();
    }

    private String renderizar(ChatResumoOperacionalParser.Pedido p, List<ChatResumoOperacionalFonte> fontes) {
        DateTimeFormatter d = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String periodo = p.inicio().equals(p.fim()) ? d.format(p.inicio()) : d.format(p.inicio()) + " a " + d.format(p.fim());
        StringBuilder b = new StringBuilder("Resumo operacional — ").append(periodo)
                .append(". Datas inteiras; referência de ‘hoje’: America/Cuiaba.\n")
                .append("Consulta sob demanda, não uma lista completa de obrigações. Aéreo e hotel: cadastro Manager, sem consolidar Wooba nem revalidar fornecedor. Horários aéreos são os cadastrados, sem fuso confirmado; podem incluir horários já passados.\n");
        for (var f : fontes) {
            b.append("\n").append(limpar(f.titulo(), 100)).append(":\n");
            if (f.status() == ChatResumoOperacionalFonte.Status.INDISPONIVEL || f.status() == ChatResumoOperacionalFonte.Status.SEM_PERMISSAO) {
                b.append(f.status() == ChatResumoOperacionalFonte.Status.SEM_PERMISSAO ? "Consulta não autorizada. " : "Consulta indisponível. ");
            } else {
                if (f.totalConhecido() != null) b.append("Registros nesta fonte/período: ").append(f.totalConhecido()).append(". ");
                else b.append("Registros elegíveis retornados: ").append(f.itens().size()).append(f.limitado() ? " ou mais. " : ". ");
                if (f.itens().isEmpty()) b.append("Nenhum registro retornado; isso não garante ausência de pendências.\n");
                for (var i : f.itens().stream().limit(3).toList()) {
                    b.append("- ");
                    if (i.data() != null) b.append(d.format(i.data())).append(" ");
                    if (i.hora() != null) b.append(i.hora().format(DateTimeFormatter.ofPattern("HH:mm"))).append(" (horário cadastrado) — ");
                    b.append(limpar(i.titulo(), 140));
                    if (i.descricao() != null && !i.descricao().isBlank()) b.append(" — ").append(limpar(i.descricao(), 180));
                    b.append("\n");
                }
                if (f.limitado() || f.itens().size() > 3) b.append("Exibição limitada; consulte a listagem do produto para os demais itens.\n");
            }
            b.append(limpar(f.cobertura(), 1000)).append("\n");
        }
        return b.append("\nPara conferir um item, informe o produto e o localizador. Também pode pedir ‘resumo operacional de amanhã’ ou ‘resumo operacional dos próximos 3 dias’. Nenhuma emissão, pagamento, check-in ou transferência foi executada.").toString();
    }

    static String limpar(String texto, int max) {
        if (texto == null) return "";
        String s = texto.replaceAll("[\\p{Cntrl}\\u202a-\\u202e\\u2066-\\u2069]", " ").replaceAll("\\s+", " ").trim();
        return s.substring(0, Math.min(max, s.length()));
    }

    private ChatResponseDTO resposta(PerguntarConfiaRequest req, SessaoChatResponse sessao, Long conversa,
            ChatResumoOperacionalParser.Pedido p, String status, String texto, List<ChatResumoOperacionalFonte> fontes) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("schema", ChatResumoOperacionalContexto.SCHEMA);
        payload.put("statusConsulta", status); payload.put("resultadoTarefa", status);
        ObjectNode contexto = payload.putObject("contexto");
        contexto.put("conversa", conversa); contexto.put("usuario", req.getCodgUsuario()); contexto.put("agencia", agencia(sessao));
        contexto.put("atualizadoEm", System.currentTimeMillis()); contexto.put("foco", p.foco());
        if (p.inicio() != null) contexto.put("inicio", p.inicio().toString());
        if (p.fim() != null) contexto.put("fim", p.fim().toString());
        // Logs permitem diagnosticar cobertura e timeout, sem duplicar passageiros/faturas/totais no contexto.
        var estados = payload.putArray("fontes");
        for (var f : fontes) {
            var e = estados.addObject(); e.put("codigo", f.codigo()); e.put("status", f.status().name());
            e.put("fonte", f.fonte()); e.put("limitado", f.limitado() || f.itens().size() > 3);
        }
        var barreira = mapper.createObjectNode(); barreira.put("schema", "chat.reservas-consulta.v1");
        barreira.put("statusConsulta", "CONSULTA_BLOQUEADA"); barreira.put("resultadoTarefa", status);
        return new ChatResponseDTO(null, texto, List.of(), null, List.of("agencia.resumo_operacional"),
                List.of(new ChatMessageDTO("system", payload.toString()), new ChatMessageDTO("system", barreira.toString())), List.of());
    }
}
