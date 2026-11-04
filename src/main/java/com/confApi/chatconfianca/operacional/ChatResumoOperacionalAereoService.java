package com.confApi.chatconfianca.operacional;

import com.confApi.chatconfianca.client.ChatConfiancaManagerClient;
import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.exception.RegraDeNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import static com.confApi.chatconfianca.operacional.ChatResumoOperacionalFonte.Status.*;

/** Consulta somente dados locais do Manager, com sessao explicitamente revalidada pelo orquestrador. */
@Service
public class ChatResumoOperacionalAereoService {
    private static final String FONTE = "Cadastro de reservas aereas do Manager";
    private static final int LIMITE = 20;
    private final ChatConfiancaManagerClient manager;
    private final boolean enabled;

    public ChatResumoOperacionalAereoService(ChatConfiancaManagerClient manager,
            @Value("${chat-confianca.resumo-operacional.aereo-enabled:false}") boolean enabled) {
        this.manager = manager; this.enabled = enabled;
    }

    public ChatResumoOperacionalFonte consultarEmbarques(SessaoChatResponse sessao, LocalDate inicio, LocalDate fim) {
        return consultar(sessao, inicio, fim, "EMBARQUES");
    }
    public ChatResumoOperacionalFonte consultarPrazos(SessaoChatResponse sessao, LocalDate inicio, LocalDate fim) {
        return consultar(sessao, inicio, fim, "PRAZOS_EMISSAO");
    }

    private ChatResumoOperacionalFonte consultar(SessaoChatResponse sessao, LocalDate inicio, LocalDate fim, String tipo) {
        if (!enabled) return semDados(tipo, INDISPONIVEL, "Fonte aerea desabilitada; nao foi consultada.");
        if (sessao == null || sessao.getUsuario() == null || sessao.getUsuario().getCodgUsuario() == null
                || sessao.getUsuario().getCodgUsuario() <= 0 || sessao.getAgencia() == null
                || sessao.getAgencia().getCodgAgencia() == null || sessao.getAgencia().getCodgAgencia() <= 0
                || sessao.getUsuario().getCodgAgencia() != null
                    && !Objects.equals(sessao.getUsuario().getCodgAgencia(), sessao.getAgencia().getCodgAgencia()))
            return semDados(tipo, SEM_PERMISSAO, "Nao foi possivel confirmar a agencia da sessao.");
        if (inicio == null || fim == null || inicio.isAfter(fim) || inicio.getYear() < 1900 || fim.getYear() > 2100
                || ChronoUnit.DAYS.between(inicio, fim) > 6)
            return semDados(tipo, INDISPONIVEL, "Periodo operacional invalido; nenhuma consulta foi realizada.");
        try {
            Integer agencia = sessao.getAgencia().getCodgAgencia();
            // Todos os componentes sao numeros, datas ISO e enum fechado; nao ha URL ou SQL vindo do usuario/modelo.
            String path = "chat-confianca/consultas/operacional-aereo?codgAgencia=" + agencia
                    + "&tipo=" + tipo + "&inicio=" + inicio + "&fim=" + fim + "&limite=" + LIMITE;
            ChatResumoOperacionalAereoDados dados = manager.getEncoded(path, ChatResumoOperacionalAereoDados.class);
            if (dados == null) return semDados(tipo, INDISPONIVEL, "Fonte operacional aerea indisponivel; nao e ausencia de pendencias.");
            validar(dados, agencia, tipo, inicio, fim);
            List<ChatResumoOperacionalFonte.Item> itens = new ArrayList<>();
            for (ChatResumoOperacionalAereoDados.Item item : dados.getItens()) itens.add(converter(item, tipo));
            boolean limitado = dados.getTotal() > itens.size();
            String cobertura = "Cobertura restrita ao cadastro local do Manager, sem reservas exclusivas do Wooba ou de outras origens. "
                    + ("EMBARQUES".equals(tipo)
                    ? "Conta segmentos cadastrados de reservas emitidas, nao canceladas e com algum bilhete ativo; nao conta passageiros. "
                      + "Nao confirma voo no GDS, abertura ou realizacao de check-in. Registros sem data de voo preenchida nao entram. "
                    : "Conta reservas nao emitidas e nao canceladas com data_limite_emissao preenchida no periodo, sem limitar a data de criacao. "
                      + "O prazo cadastrado deve ser confirmado na reserva; nao autoriza nem realiza emissao. Reservas sem prazo preenchido nao entram. ")
                    + "Datas e horarios sao os cadastrados, com fuso da origem nao informado; podem incluir horarios ja passados no dia. "
                    + "A lista vazia nao comprova ausencia de pendencias da agencia."
                    + (limitado ? " Exibidos os primeiros " + LIMITE + " itens em ordem de data; ha outros registros no Manager." : "");
            return new ChatResumoOperacionalFonte(codigo(tipo), titulo(tipo), INCOMPLETO, FONTE,
                    cobertura, dados.getTotal(), List.copyOf(itens), limitado);
        } catch (RegraDeNegocioException ex) {
            return semDados(tipo, ex.getStatus() == 403 ? SEM_PERMISSAO : INDISPONIVEL,
                    ex.getStatus() == 403 ? "A integracao nao autorizou esta consulta aerea."
                            : "Falha na fonte aerea; nao e ausencia de pendencias.");
        } catch (RuntimeException ex) {
            // Nunca leva mensagem de excecao, token, URL, payload ou dados de outra agencia ao chat.
            return semDados(tipo, INDISPONIVEL, "Nao foi possivel concluir a consulta aerea; nao e ausencia de pendencias.");
        }
    }

    private static void validar(ChatResumoOperacionalAereoDados d, Integer agencia, String tipo, LocalDate inicio, LocalDate fim) {
        if (!"chat.operacional-aereo.v1".equals(d.getSchema()) || !agencia.equals(d.getCodgAgencia())
                || !tipo.equals(d.getTipo()) || !inicio.equals(d.getInicio()) || !fim.equals(d.getFim())
                || d.getLimite() != LIMITE || d.getTotal() == null || d.getTotal() < 0 || !"NAO_INFORMADO".equals(d.getFusoFonte())
                || d.getItens() == null || d.getItens().size() != Math.min(d.getTotal(), LIMITE))
            throw new IllegalStateException("Resposta operacional invalida.");
        Set<String> ids = new HashSet<>();
        for (ChatResumoOperacionalAereoDados.Item i : d.getItens()) {
            if (i == null || i.getReservaId() == null || i.getReservaId() <= 0 || i.getDataHoraLocal() == null
                    || i.getDataHoraLocal().toLocalDate().isBefore(inicio) || i.getDataHoraLocal().toLocalDate().isAfter(fim)
                    || "EMBARQUES".equals(tipo) && (i.getVooId() == null || i.getVooId() <= 0)
                    || "PRAZOS_EMISSAO".equals(tipo) && i.getVooId() != null
                    || !ids.add(i.getReservaId() + ":" + ("EMBARQUES".equals(tipo) ? i.getVooId() : "prazo")))
                throw new IllegalStateException("Registro operacional invalido.");
            texto(i.getLocalizador(), 45); texto(i.getCompanhia(), 30); texto(i.getNumeroVoo(), 20);
            texto(i.getOrigem(), 20); texto(i.getDestino(), 20); texto(i.getStatusCadastral(), 100);
        }
    }

    private static ChatResumoOperacionalFonte.Item converter(ChatResumoOperacionalAereoDados.Item i, String tipo) {
        String pnr = texto(i.getLocalizador(), 45);
        String referencia = pnr == null ? "Reserva " + i.getReservaId() : "Localizador " + pnr;
        String companhia = texto(i.getCompanhia(), 30);
        String descricao = referencia + (companhia == null ? "" : " — " + companhia);
        boolean embarque = "EMBARQUES".equals(tipo);
        if (embarque) {
            String origem = texto(i.getOrigem(), 20), destino = texto(i.getDestino(), 20), voo = texto(i.getNumeroVoo(), 20);
            descricao += " — " + (origem == null ? "Origem nao informada" : origem) + " → "
                    + (destino == null ? "Destino nao informado" : destino) + (voo == null ? "" : " — voo " + voo);
            String status = texto(i.getStatusCadastral(), 100);
            if (status != null) descricao += ". Status cadastral do voo: " + status;
        } else descricao += ". Reserva nao emitida; confirme o prazo cadastrado antes de qualquer operacao.";
        String codigo = embarque ? "aereo-embarque-" + i.getReservaId() + "-" + i.getVooId() : "aereo-prazo-" + i.getReservaId();
        // O copiloto recebe PNR somente quando cabe no contrato seguro dos consumidores existentes.
        String pnrAcao = pnr != null && pnr.matches("[A-Za-z0-9]{5,30}") ? pnr.toUpperCase(Locale.ROOT) : null;
        return new ChatResumoOperacionalFonte.Item(codigo, embarque ? "Embarque cadastrado" : "Prazo de emissao cadastrado",
                descricao, i.getDataHoraLocal().toLocalDate(), i.getDataHoraLocal().toLocalTime(),
                i.getReservaId().longValue(), pnrAcao, "AEREO");
    }

    private static String texto(String raw, int max) {
        if (raw == null || raw.isBlank()) return null;
        String texto = raw.strip();
        if (texto.length() > max || texto.codePoints().anyMatch(Character::isISOControl)
                || texto.indexOf('<') >= 0 || texto.indexOf('>') >= 0)
            throw new IllegalStateException("Campo operacional invalido.");
        return texto;
    }
    private static String codigo(String tipo) { return "EMBARQUES".equals(tipo) ? "EMBARQUES_AEREOS" : "PRAZOS_EMISSAO_AEREOS"; }
    private static String titulo(String tipo) { return "EMBARQUES".equals(tipo) ? "Embarques aereos cadastrados" : "Prazos de emissao cadastrados"; }
    private static ChatResumoOperacionalFonte semDados(String tipo, ChatResumoOperacionalFonte.Status status, String cobertura) {
        return new ChatResumoOperacionalFonte(codigo(tipo), titulo(tipo), status, FONTE, cobertura, null, List.of(), false);
    }
}
