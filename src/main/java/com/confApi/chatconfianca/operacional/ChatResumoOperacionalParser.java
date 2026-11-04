package com.confApi.chatconfianca.operacional;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.*;

/** Reconhecimento fechado para leitura; nunca converte um resumo em ordem comercial. */
public final class ChatResumoOperacionalParser {
    private ChatResumoOperacionalParser() { }
    public record Pedido(LocalDate inicio, LocalDate fim, String foco, String pergunta) { }

    public static Pedido interpretar(String texto, boolean continuidade, Clock clock) {
        String t = normalizar(texto);
        boolean explicito = t.matches(".*\\bresumo (?:operacional|da agencia|de hoje|de amanha)\\b.*")
                || t.matches("(?:o que (?:eu )?(?:preciso |devo )?acompanhar|quais (?:sao )?(?:as )?(?:minhas )?pendencias|minhas pendencias)(?: (?:operacionais|da agencia|de hoje|para hoje|hoje|de amanha|para amanha|amanha))*[?.!]*");
        boolean curto = continuidade && t.matches("(?:e |agora |mostre |quero ver |somente |apenas )?(?:os |as |o |a )?(?:amanha|hoje|embarques?|prazos?|hotel|hoteis|hospedagem|hospedagens|entradas?|saidas?|faturas?|financeiro|vencimentos?|resumo|tudo|proximos [0-9]+ dias)[?.!]*");
        if (!explicito && !curto) return null;
        if (t.matches(".*\\b(?:sem|exceto|menos|nao)\\b.*\\b(?:financeiro|faturas?|vencimentos?|hotel|hoteis|embarques?|prazos?|entradas?|saidas?)\\b.*")
                || t.matches(".*\\b(?:pagos?|pagas?|quitados?|quitadas?|cancelados?|canceladas?|reembolsados?|reembolsadas?|vencidas?|atrasadas?)\\b.*"))
            return erro("O resumo mostra registros operacionais cadastrados e faturas em aberto no período. Para excluir blocos ou consultar outros estados, informe o bloco e a consulta desejada separadamente; não vou inverter o filtro solicitado.");
        if (t.length() > 500 || t.matches(".*\\b(?:pague|pagar|emita|emitir|cancele|cancelar|confirme|confirmar|reembolse|reembolsar|remarque|transferir|encaminhe)\\b.*"))
            return erro("O resumo é somente para consulta. Peça a operação separadamente para validar a reserva e as confirmações necessárias.");
        if (t.matches(".*\\b(?:outra agencia|outras agencias|todas as agencias|agencia [0-9]+)\\b.*"))
            return erro("O resumo usa apenas a agência autenticada desta conversa. Para outra agência, selecione-a pelos controles autorizados do sistema.");
        if (t.matches(".*\\b(?:destino|origem|passageiro|cliente|localizador|companhia|cia|cidade)\\b.*"))
            return erro("O resumo operacional aceita período e bloco (embarques, prazos, hotel ou faturas). Para filtrar por passageiro, rota ou localizador, peça uma consulta de reservas separada.");
        boolean embarques = t.matches(".*\\bembarques?\\b.*"), prazos = t.matches(".*\\bprazos?\\b.*"),
                hospedagem = t.matches(".*\\b(?:hotel|hoteis|hospedagem|hospedagens|entradas?|saidas?)\\b.*"),
                faturas = t.matches(".*\\b(?:faturas?|vencimentos?|financeiro)\\b.*");
        if ((embarques ? 1 : 0) + (prazos ? 1 : 0) + (hospedagem ? 1 : 0) + (faturas ? 1 : 0) > 1)
            return erro("Posso mostrar todos os blocos com ‘resumo operacional’, ou um bloco por vez: embarques, prazos, hotel ou faturas. Qual prefere?");
        String foco = t.matches(".*\\bentradas?\\b.*") && t.matches(".*\\bsaidas?\\b.*") ? "HOTEL"
                : embarques ? "EMBARQUES" : prazos ? "PRAZOS"
                : t.matches(".*\\b(?:hotel|hoteis|hospedagem|hospedagens)\\b.*") ? "HOTEL"
                : t.matches(".*\\bentradas?\\b.*") ? "ENTRADAS"
                : t.matches(".*\\bsaidas?\\b.*") ? "SAIDAS" : faturas ? "FINANCEIRO" : "TODOS";
        LocalDate inicio = LocalDate.now(clock), fim = inicio;
        try {
            Matcher datas = Pattern.compile("(?<![0-9])(?:[0-9]{4}-[0-9]{2}-[0-9]{2}|[0-9]{2}/[0-9]{2}/[0-9]{4})(?![0-9])").matcher(t);
            List<LocalDate> encontradas = new ArrayList<>();
            while (datas.find()) {
                String d = datas.group();
                encontradas.add(d.contains("/") ? LocalDate.parse(d, DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)) : LocalDate.parse(d));
            }
            if (encontradas.size() > 2) return erroPeriodo();
            if (!encontradas.isEmpty()) {
                inicio = encontradas.get(0); fim = encontradas.get(encontradas.size()-1);
            } else {
                Matcher dias = Pattern.compile("\\bproximos ([0-9]+) dias\\b").matcher(t);
                if (dias.find()) {
                    int n = Integer.parseInt(dias.group(1));
                    if (n < 1 || n > 7) return erroPeriodo();
                    fim = inicio.plusDays(n-1);
                } else if (t.matches(".*\\bhoje e amanha\\b.*")) fim = inicio.plusDays(1);
                else if (t.matches(".*\\bhoje\\b.*") && t.matches(".*\\bamanha\\b.*")) return erroPeriodo();
                else if (t.matches(".*\\bamanha\\b.*")) inicio = fim = inicio.plusDays(1);
                else if (t.matches(".*\\b(?:ontem|semana|mes|ano|dias|horas|segunda|terca|quarta|quinta|sexta|sabado|domingo|[0-9]{1,2}/[0-9]{1,2}|[0-9]+-[0-9]+|dia [0-9]+|de [0-9]+)\\b.*")) return erroPeriodo();
            }
            if (inicio.getYear() < 1900 || fim.getYear() > 2100 || fim.isBefore(inicio) || ChronoUnit.DAYS.between(inicio, fim) > 6) return erroPeriodo();
            return new Pedido(inicio, fim, foco, null);
        } catch (RuntimeException ex) { return erroPeriodo(); }
    }

    public static String normalizar(String texto) {
        return texto == null ? "" : Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
    private static Pedido erro(String msg) { return new Pedido(null, null, "TODOS", msg); }
    private static Pedido erroPeriodo() {
        return erro("Informe hoje, amanhã, próximos 3 dias ou um intervalo de até 7 dias (dd/MM/aaaa). Os próximos dias incluem hoje; o resumo cobre as datas inteiras.");
    }
}
