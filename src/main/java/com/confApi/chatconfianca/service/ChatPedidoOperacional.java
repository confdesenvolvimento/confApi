package com.confApi.chatconfianca.service;

import com.confApi.chatgpt.dto.ChatActionDTO;
import com.confApi.chatgpt.dto.ChatMessageDTO;
import com.confApi.chatgpt.dto.ChatResponseDTO;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/** Guardas de capacidade compartilhadas por V1/V2. Nao consulta dados nem concede permissoes. */
public final class ChatPedidoOperacional {
    private ChatPedidoOperacional() { }

    public record Orientacao(String intencao, String resultado, String texto, boolean humano) {
        public ChatResponseDTO resposta() {
            return new ChatResponseDTO(null, texto, List.of(), null, List.of(),
                    List.of(new ChatMessageDTO("system", "{\"schema\":\"chat.orientacao.v1\",\"resultadoTarefa\":\"" + resultado + "\"}")),
                    humano ? List.of(acaoHumana()) : List.of());
        }
    }

    public static Orientacao avaliar(String mensagem) {
        String t = normalizar(mensagem);
        if (t.isBlank()) return null;
        boolean financeiro = contem(t, "fatura|faturas|boleto|boletos|linha digitavel|codigo de barras");
        if (financeiro && (contem(t, "onde|qual menu|qual parte")
                || t.matches(".*\\bcomo\\b.*\\b(acesso|acessar|encontro|encontrar|vejo|visualizo|visualizar|entro|entrar|baixo|baixar|pego|obter|gero|gerar)\\b.*"))) {
            return new Orientacao("financeiro.orientacao_portal", "ORIENTACAO_FORNECIDA",
                    "No portal, acesse Financeiro → Faturas e boletos e selecione o documento. "
                    + "Se a opção não aparecer, peça ao administrador da agência ou ao suporte para verificar sua permissão. "
                    + "No aplicativo, se essa função não estiver disponível, use o portal com sua sessão autenticada. "
                    + "A disponibilidade de PDF, boleto ou linha digitável depende do documento e de seu fechamento; nenhuma consulta ou geração de arquivo foi executada nesta orientação.", false);
        }
        if (pedidoEmissao(t)) {
            return new Orientacao("aereo.emissao", "CAPACIDADE_INDISPONIVEL",
                    "Entendi que você deseja emitir uma reserva, não consultar faturas. "
                    + "Este chat não conclui a emissão nem pode afirmar bloqueio financeiro a partir de uma lista de documentos. "
                    + "Posso encaminhar sua solicitação ao atendimento: confirme o departamento desejado. Nenhuma emissão ou cobrança foi realizada.", true);
        }
        if (pedidoIndicadorVendas(t)) {
            return new Orientacao("vendas.indicadores", "CAPACIDADE_INDISPONIVEL",
                    "Você está pedindo um indicador de vendas por período. A lista das dez reservas recentes não representa a quantidade nem o valor total vendido ou emitido. "
                    + "O relatório consolidado ainda não está disponível neste chat. Consulte o relatório de vendas do sistema ou confirme o departamento para atendimento humano.", true);
        }
        if (contem(t, "multitrecho|multiplos trechos|tres trechos|3 trechos|varios trechos")) {
            return new Orientacao("aereo.multitrecho", "CAPACIDADE_INDISPONIVEL",
                    "Entendi que sua viagem tem múltiplos trechos. Não vou substituir esse itinerário por uma ida e volta simples. "
                    + "A pesquisa multitrecho ainda precisa ser feita no portal ou com o atendimento. Mantenha as cidades e as datas de cada trecho na solicitação e confirme o departamento se desejar encaminhar.", true);
        }
        if (contem(t, "linha digitavel|codigo de barras") && financeiro) {
            return new Orientacao("financeiro.documento_boleto", "CAPACIDADE_INDISPONIVEL",
                    "A linha digitável e o código de barras precisam vir do boleto real. Não vou criar ou estimar esses dados. "
                    + "No portal autenticado, acesse Financeiro → Faturas e boletos e abra o documento disponível. "
                    + "Se o boleto não estiver disponível ou precisar de atualização, confirme o atendimento financeiro. Nenhum boleto foi gerado ou pagamento realizado.", true);
        }
        if (contem(t, "alerta|alertas|monitor|monitores") && contem(t, "tarifa|tarifas|rota|rotas|voo|voos")
                && contem(t, "como|onde|criar|crie|cadastrar|cadastre|novo|nova|configurar|ativar")) {
            return new Orientacao("institucional.alerta_tarifas", "ORIENTACAO_FORNECIDA",
                    "Para cadastrar um monitor no portal, acesse Início → Alerta de Tarifas → +Novo Monitor e preencha os critérios da rota e do período. "
                    + "Não consultei sua lista de alertas e nenhum monitor foi criado, ativado ou alterado nesta conversa. "
                    + "Se a opção não aparecer, peça ao suporte para verificar a permissão de seu usuário.", false);
        }
        if (contem(t, "clube confianca|club confianca|treinamento|treinamentos|video tutorial|tutorial de pagamento")) {
            return new Orientacao("institucional.uso_produto", "SEM_CONHECIMENTO",
                    "Esse assunto faz parte dos serviços da Confiança. Ainda não tenho um procedimento vigente publicado para esta dúvida neste canal. "
                    + "Posso encaminhar ao suporte com o histórico para orientar o acesso ou indicar o treinamento correto, sem inventar um endereço de vídeo ou procedimento.", true);
        }
        if (contem(t, "pacote|pacotes") && contem(t, "personalizado|personalizada|roteiro sob medida")) {
            return new Orientacao("pacote.atendimento", "CAPACIDADE_INDISPONIVEL",
                    "Para esse pacote personalizado, a confirmação de ocupação, passageiros, roteiro e disponibilidade precisa do atendimento. "
                    + "As ofertas de cache não equivalem a uma cotação confirmada para o grupo. Posso encaminhar os critérios já enviados no histórico, sem pedir que você os repita em outro canal.", true);
        }
        return null;
    }

    public static boolean pedidoCondicaoComercial(String texto) {
        String t = normalizar(texto);
        return contem(t, "parcelamento|parcelar|parcelas|quantas vezes|quantas vezes posso|formas de pagamento|forma de pagamento")
                && !contem(t, "limite de credito|meu limite|minhas faturas|simulacao de remarcacao");
    }

    public static Orientacao condicaoSemFonte() {
        return new Orientacao("financeiro.condicoes_comerciais", "SEM_CONHECIMENTO",
                "Não tenho uma condição comercial vigente e confirmada para informar parcelas, juros ou formas de pagamento dessa companhia neste canal. "
                + "Essas condições podem variar por companhia, base e modalidade. Posso encaminhar ao atendimento para confirmar, sem presumir um número de parcelas ou autorização de pagamento.", true);
    }

    public static boolean pedidoHumano(String texto) {
        String t = normalizar(texto);
        return contem(t, "atendente|atendimento humano|pessoa do atendimento|falar com uma pessoa")
                && !t.matches(".*\\b(nao|sem)\\b.{0,20}\\b(atendente|atendimento humano)\\b.*");
    }

    private static boolean pedidoEmissao(String t) {
        return contem(t, "emitir|emita") && contem(t, "reserva|bilhete|passagem|localizador")
                && !contem(t, "boleto|fatura|faturas|data de emissao");
    }

    private static boolean pedidoIndicadorVendas(String t) {
        boolean assunto = contem(t, "vendas?|vendeu|vendid[oa]s?|emiti|emitimos|emitid[oa]s?|faturamento");
        boolean indicador = contem(t, "quanto|quantos|quantas|total|valor total|quantidade|relatorio|indicador|ultima semana|ultimo mes|no ano");
        return assunto && indicador && !contem(t, "fatura|faturas|boleto|boletos|regra tarifaria");
    }

    public static ChatActionDTO acaoHumana() {
        return new ChatActionDTO("FALAR_ATENDENTE", "Falar com atendimento", "Confirme ou escolha o departamento antes de encaminhar.",
                null, null, true, false, false, "Quero falar com um atendente.");
    }

    static String normalizar(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private static boolean contem(String texto, String alternativas) {
        return java.util.regex.Pattern.compile("\\b(?:" + alternativas + ")\\b").matcher(texto).find();
    }
}
