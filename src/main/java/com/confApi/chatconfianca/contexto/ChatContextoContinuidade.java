package com.confApi.chatconfianca.contexto;

import com.confApi.chatconfianca.dto.model.Mensagem;
import com.confApi.chatconfianca.dto.request.PerguntarConfiaRequest;
import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.chatconfianca.reservas.ChatReservaConsultaParser;
import com.confApi.chatconfianca.service.ChatPedidoOperacional;
import com.confApi.chatgpt.util.LocalizadorAereo;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

/** Retoma apenas referencia persistida pelo servidor; cada consulta revalida o acesso. */
public final class ChatContextoContinuidade {
    private ChatContextoContinuidade() { }
    public static void completar(PerguntarConfiaRequest req, SessaoChatResponse sessao,
            List<Mensagem> historico, ObjectMapper mapper) {
        if (req == null || sessao == null || sessao.getAgencia() == null) return;
        var cliente = req.getContextoCliente();
        if (!ChatContextoClienteContrato.suportaCopiloto(cliente) || cliente.getRecurso() != null) return;
        String t = ChatReservaConsultaParser.normalizar(req.getMensagem());
        if (ChatPedidoOperacional.pedidoHumano(t) || LocalizadorAereo.extrair(req.getMensagem()) != null) return;
        if (t.matches(".*\\b(hotel|hoteis|seguro|carro|pacote|fatura|boleto|financeiro|outra|outro|nova|novo|pesquise|pesquisar)\\b.*")) return;
        boolean dados = t.matches(".*\\b(localizador|passageiros|trechos|horarios|situacao|bagagem)\\b.*");
        if (!dados && !ChatCopilotoRegrasService.perguntaRegras(t)) return;
        var ref = ChatAjudaContextualHistorico.recuperar(historico, req.getConversaId(),
                sessao.getAgencia().getCodgAgencia(), req.getCodgUsuario(), mapper);
        if (ref == null || !"RESERVA_AEREA".equals(ref.getTipo())) return;
        var contexto = new ChatContextoClienteDTO();
        contexto.setSchema(cliente.getSchema()); contexto.setCanal(cliente.getCanal());
        contexto.setTela("RESERVA_AEREA"); contexto.setRecurso(ref);
        contexto.setCapacidades(cliente.getCapacidades()); contexto.setCopilotoAereoVersao(cliente.getCopilotoAereoVersao());
        req.setContextoCliente(contexto);
    }
}
