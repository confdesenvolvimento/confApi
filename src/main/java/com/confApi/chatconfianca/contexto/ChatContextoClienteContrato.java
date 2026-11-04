package com.confApi.chatconfianca.contexto;

import com.confApi.chatgpt.dto.ChatActionDestinoDTO;
import com.confApi.exception.RegraDeNegocioException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Valida formato e limites. Nunca decide autorizacao nem executa comandos. */
public final class ChatContextoClienteContrato {
    private static final Set<String> CANAIS = Set.of("WEB", "APP");
    private static final Set<String> TELAS = Set.of("CHAT", "RESERVA_AEREA", "FATURAS", "FATURA_DETALHE");
    private static final Set<String> CAPACIDADES = Set.of("ABRIR_RESERVA", "ABRIR_FATURA", "BAIXAR_DOCUMENTO");
    private static final Set<String> TIPOS = Set.of("RESERVA_AEREA", "FATURA");
    private static final Set<String> PRODUTOS = Set.of("AEREO", "TERRESTRE");
    private static final Set<String> DOCUMENTOS = Set.of("FATURA_PDF", "BOLETO_PDF", "CONCILIACAO_CSV");
    private ChatContextoClienteContrato() { }

    public static ChatContextoClienteDTO validar(ChatContextoClienteDTO entrada) {
        if (entrada == null) return null;
        if (!"chat.contexto-cliente.v1".equals(entrada.getSchema())) throw invalido();
        ChatContextoClienteDTO out = new ChatContextoClienteDTO();
        out.setCanal(codigo(entrada.getCanal(), CANAIS));
        out.setTela(codigo(entrada.getTela(), TELAS));
        out.setRecurso(recurso(entrada.getRecurso()));
        compativel(out.getTela(), out.getRecurso());
        if (entrada.getCapacidades() != null && entrada.getCapacidades().size() > 8) throw invalido();
        LinkedHashSet<String> capacidades = new LinkedHashSet<>();
        if (entrada.getCapacidades() != null) for (String valor : entrada.getCapacidades())
            capacidades.add(codigo(valor, CAPACIDADES));
        out.setCapacidades(new ArrayList<>(capacidades));
        if (entrada.getCopilotoAereoVersao()!=null
                && (entrada.getCopilotoAereoVersao()<1 || entrada.getCopilotoAereoVersao()>100)) throw invalido();
        out.setCopilotoAereoVersao(entrada.getCopilotoAereoVersao());
        return out;
    }

    public static boolean suporta(ChatContextoClienteDTO contexto, String capacidade) {
        return contexto != null && capacidade != null && CAPACIDADES.contains(capacidade) && contexto.getCapacidades() != null
                && contexto.getCapacidades().contains(capacidade);
    }

    public static boolean suportaCopiloto(ChatContextoClienteDTO contexto) {
        return contexto!=null && Integer.valueOf(1).equals(contexto.getCopilotoAereoVersao())
                && suporta(contexto,"ABRIR_RESERVA");
    }

    public static ChatActionDestinoDTO validarDestino(ChatActionDestinoDTO entrada) {
        if (entrada == null) return null;
        ChatActionDestinoDTO out = new ChatActionDestinoDTO();
        out.setTela(codigo(entrada.getTela(), TELAS));
        out.setRecurso(recurso(entrada.getRecurso()));
        compativel(out.getTela(), out.getRecurso());
        if (("RESERVA_AEREA".equals(out.getTela()) || "FATURA_DETALHE".equals(out.getTela()))
                && out.getRecurso() == null) throw invalido();
        if (entrada.getDocumento() != null) {
            out.setDocumento(codigo(entrada.getDocumento(), DOCUMENTOS));
            if (out.getRecurso() == null || !"FATURA".equals(out.getRecurso().getTipo())) throw invalido();
        }
        return out;
    }

    private static ChatRecursoReferenciaDTO recurso(ChatRecursoReferenciaDTO entrada) {
        if (entrada == null) return null;
        ChatRecursoReferenciaDTO out = new ChatRecursoReferenciaDTO();
        out.setTipo(codigo(entrada.getTipo(), TIPOS));
        String id = entrada.getId();
        if (id == null || !id.matches("[1-9][0-9]{0,9}")) throw invalido();
        try { if (Long.parseLong(id) > Integer.MAX_VALUE) throw invalido(); }
        catch (NumberFormatException ex) { throw invalido(); }
        out.setId(id);
        if (entrada.getProduto() != null) {
            out.setProduto(codigo(entrada.getProduto(), PRODUTOS));
            if (!"FATURA".equals(out.getTipo())) throw invalido();
        }
        if (entrada.getDataEmissao() != null) {
            String data = entrada.getDataEmissao();
            if (!"FATURA".equals(out.getTipo()) || !data.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw invalido();
            try {
                LocalDate valor = LocalDate.parse(data, DateTimeFormatter.ISO_LOCAL_DATE);
                if (valor.getYear() < 1900 || valor.getYear() > 2100) throw invalido();
                out.setDataEmissao(valor.toString());
            } catch (DateTimeParseException ex) { throw invalido(); }
        }
        return out;
    }

    private static void compativel(String tela, ChatRecursoReferenciaDTO recurso) {
        if (recurso == null) return;
        if ("CHAT".equals(tela)) throw invalido();
        if ("RESERVA_AEREA".equals(tela) != "RESERVA_AEREA".equals(recurso.getTipo())) throw invalido();
    }

    private static String codigo(String valor, Set<String> permitidos) {
        if (valor == null || valor.length() > 40) throw invalido();
        String normalizado = valor.trim().toUpperCase(Locale.ROOT);
        if (!permitidos.contains(normalizado)) throw invalido();
        return normalizado;
    }

    private static RegraDeNegocioException invalido() {
        return new RegraDeNegocioException(400, "O contexto de tela informado nao e valido. Atualize a tela e tente novamente.");
    }
}
