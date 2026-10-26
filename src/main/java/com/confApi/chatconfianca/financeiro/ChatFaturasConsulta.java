package com.confApi.chatconfianca.financeiro;

import com.confApi.chatgpt.dto.ChatMessageDTO;
import com.confApi.chatgpt.dto.ChatResponseDTO;
import com.confApi.chatgpt.dto.ConversationRequestDTO;
import com.confApi.db.confManager.faturas.FaturasService;
import com.confApi.db.confManager.faturas.dto.FaturaSicaRQ;
import com.confApi.db.confManager.faturas.dto.FaturaSicaRS;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Consulta somente leitura, isolada pela sessao e sem interpretacao financeira por LLM. */
public final class ChatFaturasConsulta {
    private static final int LIMITE = 20;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private final FaturasService faturasService;
    private final ObjectMapper mapper;

    public ChatFaturasConsulta(FaturasService faturasService, ObjectMapper mapper) {
        this.faturasService = Objects.requireNonNull(faturasService);
        this.mapper = Objects.requireNonNull(mapper);
    }

    public ChatResponseDTO responder(ConversationRequestDTO req, Map<String, String> filtros, boolean boletos) {
        Map<String, String> parametros = parametrosSeguros(filtros);
        Integer empresa = empresaDaSessao(req);
        if (empresa == null) return resposta(req, parametros, boletos, "CONSULTA_BLOQUEADA",
                "Não foi possível confirmar sua sessão e agência para consultar dados financeiros. Entre novamente no sistema e tente outra vez.", List.of(), 0, 0);
        ChatFaturasFiltros.Consulta consulta = ChatFaturasFiltros.validar(parametros, LocalDate.now());
        parametros = consulta.parametros();
        if (consulta.pergunta() != null) return resposta(req, parametros, boletos, "AGUARDANDO_DADOS",
                consulta.pergunta(), List.of(), 0, 0);
        FaturaSicaRQ request = consulta.request();
        request.setEmpfat(req.idErp());
        final List<FaturaSicaRS> recebidas;
        try {
            recebidas = faturasService.faturaSicaEstrita(request);
        } catch (RuntimeException ex) {
            return erro(req, parametros, boletos);
        }
        if (recebidas == null) return erro(req, parametros, boletos);
        // Verificar a lista inteira ANTES de filtrar ou limitar: nunca ocultar uma falha de isolamento.
        for (FaturaSicaRS fatura : recebidas) {
            if (fatura == null || fatura.getEmpfat() <= 0 || fatura.getEmpfat() != empresa) {
                return resposta(req, parametros, boletos, "CONSULTA_BLOQUEADA",
                        "A consulta financeira não pôde ser apresentada com segurança. Tente novamente pela área Financeiro do sistema ou procure o atendimento financeiro.", List.of(), 0, 0);
            }
        }
        List<Map<String, Object>> exibidas = new ArrayList<>();
        int excluidas = 0;
        for (FaturaSicaRS fatura : recebidas) {
            if (!compativel(fatura, request)) return erro(req, parametros, boletos);
            if (boletos && (fatura.isCancelado() || credito(fatura) || aFaturar(fatura))) {
                excluidas++;
                continue;
            }
            if (exibidas.size() < LIMITE) exibidas.add(registroSeguro(fatura));
        }
        String status = recebidas.isEmpty() ? "SEM_RESULTADO" : "DADOS_CONSULTADOS";
        String texto = renderizar(parametros, boletos, exibidas, recebidas.size(), excluidas);
        return resposta(req, parametros, boletos, status, texto, exibidas, recebidas.size(), excluidas);
    }

    private ChatResponseDTO erro(ConversationRequestDTO req, Map<String, String> parametros, boolean boletos) {
        return resposta(req, parametros, boletos, "ERRO_INTEGRACAO",
                "Não foi possível concluir a consulta financeira com dados válidos. Isso não significa que não existam faturas. Tente novamente ou consulte a área Financeiro do sistema.", List.of(), 0, 0);
    }

    private ChatResponseDTO resposta(ConversationRequestDTO req, Map<String, String> parametros, boolean boletos,
            String status, String texto, List<Map<String, Object>> faturas, int total, int excluidas) {
        Map<String, Object> contexto = new LinkedHashMap<>();
        contexto.put("agencia", req == null ? null : req.codgAgencia());
        contexto.put("usuario", req == null ? null : req.codgUsuario());
        contexto.put("atualizadoEm", System.currentTimeMillis());
        contexto.put("parametros", parametros);
        contexto.put("boletos", boletos);
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("schema", "chat.faturas.v1");
        dados.put("statusConsulta", status);
        dados.put("filtros", parametros);
        dados.put("faturas", faturas);
        dados.put("totalRetornado", total);
        dados.put("excluidasDaListaBoletos", excluidas);
        dados.put("contexto", contexto);
        String json;
        try {
            json = mapper.writeValueAsString(dados);
        } catch (JsonProcessingException | RuntimeException ex) {
            texto = "Não foi possível preparar o resultado da consulta financeira. Tente novamente pela área Financeiro do sistema.";
            json = "{\"schema\":\"chat.faturas.v1\",\"statusConsulta\":\"ERRO_INTEGRACAO\",\"faturas\":[]}";
        }
        return new ChatResponseDTO(null, texto, List.of(), null, List.of(boletos ? "boletos" : "faturas"),
                List.of(new ChatMessageDTO("system", json)), List.of());
    }

    private static Integer empresaDaSessao(ConversationRequestDTO req) {
        if (req == null || req.codgAgencia() == null || req.codgAgencia() <= 0
                || req.codgUsuario() == null || req.codgUsuario() <= 0
                || req.idErp() == null || !req.idErp().matches("\\d{1,10}")) return null;
        try {
            int empresa = Integer.parseInt(req.idErp());
            return empresa > 0 ? empresa : null;
        } catch (NumberFormatException ex) { return null; }
    }

    private static Map<String, String> parametrosSeguros(Map<String, String> filtros) {
        Map<String, String> seguros = new LinkedHashMap<>();
        if (filtros != null) filtros.forEach((chave, valor) -> {
            if (chave != null && ChatFaturasFiltros.PARAMETROS.contains(chave) && valor != null) seguros.put(chave, valor);
        });
        return seguros;
    }

    private static boolean compativel(FaturaSicaRS f, FaturaSicaRQ rq) {
        if (!Double.isFinite(f.getValor()) || f.getNumfat() < 0 || (f.getNumfat() == 0 && !aFaturar(f))) return false;
        if (rq.getInvoiceNumber() != null && rq.getInvoiceNumber() != f.getNumfat()) return false;
        String produto = produto(f);
        if (!"TODOS".equals(rq.getInvoiceType()) && !rq.getInvoiceType().equals(produto)) return false;
        String pagamento = normalizar(f.getPago());
        if (!"sim".equals(pagamento) && !"nao".equals(pagamento)) return false;
        if ("PAGO".equals(rq.getPagamento()) && !"sim".equals(pagamento)) return false;
        if ("ABERTO".equals(rq.getPagamento()) && !"nao".equals(pagamento)) return false;
        LocalDate emissao = data(f.getDataFatura()), vencimento = data(f.getDataVen());
        if (informada(f.getDataFatura()) && emissao == null || informada(f.getDataVen()) && vencimento == null) return false;
        LocalDate referencia = "DATA_EMISSAO".equals(rq.getTipoData()) ? emissao : vencimento;
        LocalDate inicio = data(rq.getDataInicio()), fim = data(rq.getDataFim());
        return referencia != null && inicio != null && fim != null && !referencia.isBefore(inicio) && !referencia.isAfter(fim);
    }

    private static Map<String, Object> registroSeguro(FaturaSicaRS f) {
        Map<String, Object> registro = new LinkedHashMap<>();
        registro.put("numero", f.getNumfat());
        registro.put("emissao", formatarData(f.getDataFatura()));
        registro.put("vencimento", formatarData(f.getDataVen()));
        registro.put("valor", BigDecimal.valueOf(f.getValor()));
        registro.put("produto", produto(f));
        registro.put("situacao", situacao(f));
        registro.put("pagamento", "sim".equals(normalizar(f.getPago())) ? "PAGA" : "EM_ABERTO");
        return registro;
    }

    private static String renderizar(Map<String, String> p, boolean boletos, List<Map<String, Object>> lista, int total, int excluidas) {
        StringBuilder texto = new StringBuilder(total == 0
                ? "A consulta foi concluída e não encontrou faturas para estes filtros."
                : "A consulta retornou " + total + (total == 1 ? " registro financeiro." : " registros financeiros."));
        texto.append("\n\nFiltros: ").append(rotuloPagamento(p.get("faturaPagamento")))
                .append("; ").append(rotuloProduto(p.get("faturaProduto"))).append("; ")
                .append("DATA_EMISSAO".equals(p.get("faturaTipoData")) ? "emissão" : "vencimento")
                .append(" de ").append(formatarData(p.get("faturaInicio"))).append(" a ").append(formatarData(p.get("faturaFim"))).append('.');
        if (p.containsKey("faturaNumero")) texto.append(" Número da fatura: ").append(p.get("faturaNumero")).append('.');
        if (boletos && excluidas > 0) texto.append("\n\n").append(excluidas)
                .append(" registro(s) de crédito, à faturar ou cancelados foram excluídos da lista de boletos. Isso não significa ausência de registros financeiros.");
        if (!lista.isEmpty()) {
            NumberFormat dinheiro = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
            for (Map<String, Object> f : lista) {
                texto.append("\n\n").append(((Number) f.get("numero")).intValue() == 0 ? "Ainda não emitida" : "Fatura " + f.get("numero"))
                        .append(" — ").append(f.get("situacao"))
                        .append("\nProduto: ").append(rotuloProduto(String.valueOf(f.get("produto"))))
                        .append(" | Valor: ").append(dinheiro.format(f.get("valor")))
                        .append("\nEmissão: ").append(f.get("emissao"))
                        .append("\nVencimento: ").append(f.get("vencimento"));
            }
        }
        if (total - excluidas > LIMITE) texto.append("\n\nExibindo os primeiros 20 registros de ").append(total - excluidas)
                .append(". Informe um período menor ou o número da fatura para restringir a consulta.");
        if (total > 0) texto.append("\n\nOs valores são de documentos financeiros e não representam o total de vendas ou o total pago. Registros de crédito, cancelados e à faturar têm naturezas diferentes.");
        String documento = p.getOrDefault("faturaDocumento", boletos ? "BOLETO" : null);
        if (documento != null) texto.append("\n\nPara ").append(switch (documento) {
            case "CSV" -> "a conciliação CSV";
            case "BOLETO" -> "consultar ou baixar o boleto";
            default -> "o PDF da fatura";
        }).append(", acesse a área Financeiro, na tela de faturas, com sua sessão autenticada. A disponibilidade do documento deve ser verificada nessa tela; esta consulta não gerou um arquivo nem um comprovante de pagamento.");
        return texto.toString();
    }

    private static String situacao(FaturaSicaRS f) {
        if (f.isCancelado()) return "Cancelada";
        if (credito(f)) return "Crédito";
        if (aFaturar(f)) return "À faturar (não emitida)";
        if ("sim".equals(normalizar(f.getPago()))) return "Paga";
        return "sim".equals(normalizar(f.getFatVenc())) ? "Em aberto (vencida)" : "Em aberto";
    }
    private static boolean credito(FaturaSicaRS f) { return normalizar(f.getSituacao()).contains("credito"); }
    private static boolean aFaturar(FaturaSicaRS f) { return normalizar(f.getSituacao()).contains("a faturar"); }
    private static String produto(FaturaSicaRS f) {
        return switch (normalizar(f.getInvoiceType())) {
            case "aereo", "aerea" -> "AEREO";
            case "terrestre" -> "TERRESTRE";
            default -> "NAO_INFORMADO";
        };
    }
    private static String rotuloProduto(String produto) {
        return switch (produto == null ? "" : produto) {
            case "AEREO" -> "Aéreo";
            case "TERRESTRE" -> "Terrestre";
            case "TODOS" -> "todos os produtos";
            default -> "Não informado";
        };
    }
    private static String rotuloPagamento(String pagamento) {
        return "PAGO".equals(pagamento) ? "pagas" : "AMBOS".equals(pagamento) ? "pagas e em aberto" : "em aberto";
    }
    private static String normalizar(String texto) {
        return Normalizer.normalize(texto == null ? "" : texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
    private static boolean informada(String valor) { return valor != null && !valor.isBlank(); }
    private static LocalDate data(String valor) {
        if (!informada(valor)) return null;
        try { return LocalDate.parse(valor.trim(), valor.contains("/") ? DATA : DateTimeFormatter.ISO_LOCAL_DATE); }
        catch (DateTimeParseException ex) { return null; }
    }
    private static String formatarData(String valor) {
        LocalDate data = data(valor);
        return data == null ? "Não informada" : DATA.format(data);
    }
}
