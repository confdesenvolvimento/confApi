package com.confApi.aereo.reemissao;

import com.confApi.aereo.AereoClient;
import com.confApi.aereo.dto.*;
import com.confApi.aereo.eNums.*;
import com.confApi.chatconfianca.client.ChatConfiancaManagerClient;
import com.confApi.chatconfianca.service.ChatConfiancaService;
import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.db.confManager.aeroporto.AeroportoService;
import com.confApi.db.confManager.regraAereaAlteracao.RegraAereaAlteracaoManagerService;
import com.confApi.db.confManager.regraAereaAlteracao.dto.RegraAereaAlteracaoConsultaRequest;
import com.confApi.exception.RegraDeNegocioException;
import com.confApi.hub.aereo.dto.Aeroporto;
import com.confApi.hub.enumerador.TipoLimite;
import com.confApi.hub.limites.LimitesService;
import com.confApi.hub.limites.dto.LimiteCreditoRQ;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

/** Simulacao temporaria; a ultima etapa efetiva a reemissao e confere o resultado Azul. */
@Service
public class ReemissaoAzulService {
    private static final String BASE = "reservaAereo/reemissao-azul";
    private final Map<String, SessaoTemporaria> sessoes = new java.util.concurrent.ConcurrentHashMap<>();
    private static final class SessaoTemporaria {
        private volatile ReemissaoAzulRegistro registro;
        private SessaoTemporaria(ReemissaoAzulRegistro registro) { this.registro = registro; }
    }
    private final ChatConfiancaManagerClient manager;
    private final ChatConfiancaService usuarios;
    private final AereoClient aereo;
    private final ReemissaoAzulHubClient hub;
    private final RegraAereaAlteracaoManagerService regras;
    private final AeroportoService aeroportos;
    private final LimitesService limites;
    private final ObjectMapper mapper;
    private static final Set<String> REMOTAS = Set.of("DIVISAO_PENDENTE", "PREPARANDO", "SERVICOS", "CALCULANDO", "COMPARATIVO", "PAGAMENTO", "PROCESSANDO");
    public ReemissaoAzulService(ChatConfiancaManagerClient manager, ChatConfiancaService usuarios, AereoClient aereo,
            ReemissaoAzulHubClient hub, RegraAereaAlteracaoManagerService regras, AeroportoService aeroportos,
            LimitesService limites, ObjectMapper mapper) {
        this.manager = manager; this.usuarios = usuarios; this.aereo = aereo; this.hub = hub; this.regras = regras; this.aeroportos = aeroportos; this.limites = limites; this.mapper = mapper;
    }

    public ObjectNode iniciar(ObjectNode pedido) {
        SessaoChatResponse sessao = sessao(pedido);
        int reservaId = inteiro(pedido, "reservaId");
        JsonNode reserva = validarReserva(reservaId, sessao.getAgencia().getCodgAgencia());
        String loc = reserva.path("localizador").asText().trim().toUpperCase(Locale.ROOT);
        sessoes.entrySet().removeIf(e -> e.getValue().registro.getExpiraEm().isBefore(LocalDateTime.now())
                && !Set.of("DIVISAO_PENDENTE", "PROCESSANDO", "SINCRONIZANDO", "CONFERENCIA").contains(e.getValue().registro.getStatus()));
        for (SessaoTemporaria temporaria : sessoes.values()) {
            ReemissaoAzulRegistro candidata = temporaria.registro;
            if (!Objects.equals(candidata.getCodgUsuario(), sessao.getUsuario().getCodgUsuario())
                    || !Objects.equals(candidata.getReservaId(), reservaId)) continue;
            synchronized (temporaria) {
                ReemissaoAzulRegistro ativa = temporaria.registro;
                if (Objects.equals(ativa.getCodgUsuario(), sessao.getUsuario().getCodgUsuario())
                        && Objects.equals(ativa.getCodgAgencia(), sessao.getAgencia().getCodgAgencia())
                        && Objects.equals(ativa.getReservaId(), reservaId)
                        && !dados(ativa).hasNonNull("substituidaPor")
                        && !Set.of("CONCLUIDA", "NAO_CONCLUIDA", "CANCELADA", "EXPIRADA").contains(ativa.getStatus()))
                    return "DIVISAO_PENDENTE".equals(ativa.getStatus()) ? recuperarDivisao(ativa, sessao) : retomarSeSemConfirmacao(consultar(ativa), sessao);
            }
        }
        exigir(sessoes.size() < 2000, 503, "Ha muitas simulacoes em andamento. Tente novamente em instantes.");
        JsonNode disponibilidade = manager.get(BASE + "/" + reservaId + "/disponibilidade", JsonNode.class);
        exigir(disponibilidade != null && disponibilidade.path("disponivel").isBoolean(), 503, "Nao foi possivel consultar o historico desta reserva.");
        if (disponibilidade.path("divisoesPendentes").isArray() && !disponibilidade.path("divisoesPendentes").isEmpty())
            return abrirDivisaoPendente(sessao, reservaId, loc, disponibilidade.path("divisoesPendentes").get(0).asText());
        if (!disponibilidade.path("disponivel").asBoolean()) return retomarSeSemConfirmacao(abrirAcompanhamento(sessao, reservaId, loc, disponibilidade), sessao);
        return iniciarSimulacao(sessao, reservaId, loc, reserva, null);
    }

    private ObjectNode iniciarSimulacao(SessaoChatResponse sessao, int reservaId, String loc, JsonNode reserva, JsonNode retomada) {
        ObjectNode consulta = mapper.createObjectNode().put("localizador", loc);
        JsonNode resposta = hub.executar("consultar", consulta);
        exigir(resposta != null && "CONSULTADA".equals(resposta.path("status").asText()), 409, mensagem(resposta));
        ObjectNode dados = mapper.createObjectNode(); dados.set("original", resposta.get("original"));
        exigir("BRL".equals(dados.path("original").path("moeda").asText()), 409, "A reemissao automatica exige reserva em reais (BRL). Solicite analise ao atendimento para outra moeda.");
        ((ObjectNode) dados.get("original")).put("valorVenda", totalVenda(reserva));
        ArrayNode trechos = (ArrayNode) dados.path("original").path("trechos");
        exigir(trechos.size() >= 1 && trechos.size() <= 2, 409, "O itinerario precisa ter um ou dois trechos para este fluxo.");
        ArrayNode politicas = dados.putArray("politicas");
        boolean possuiFuturo = false;
        for (JsonNode trecho : trechos) {
            boolean futuro = LocalDateTime.parse(trecho.path("partida").asText()).isAfter(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));
            ((ObjectNode) trecho).put("elegivel", futuro); possuiFuturo |= futuro;
            if (futuro) politicas.add(consultarRegra(trecho, reserva));
            else politicas.addObject().put("trechoIndice", trecho.path("indice").asInt()).put("mensagem", "Trecho utilizado ou em no-show: indisponivel para reemissao automatica.");
        }
        exigir(possuiFuturo, 409, "Nenhum trecho futuro elegivel. Solicite analise ao atendimento.");
        dados.put("politicaVersao", UUID.randomUUID().toString());
        dados.put("mensagem", "A reemissao inclui todos os passageiros dos trechos escolhidos. Confira as regras e aceite para continuar.");
        dados.set("selecionados", mapper.createArrayNode()); dados.set("escolhas", mapper.createArrayNode());
        dados.set("eventos", mapper.createArrayNode());
        dados.put("envioFinalRegistrado", false).put("confirmadaAzul", false);
        if (retomada != null && retomada.path("divisaoConcluida").asBoolean()) {
            dados.put("passageirosConfirmados", true).put("divisaoConcluida", true);
            dados.set("reservaOrigemId", retomada.get("reservaOrigemId")); dados.set("localizadorOrigem", retomada.get("localizadorOrigem"));
            dados.put("avisoRetomada", "A separacao dos passageiros foi concluida. Esta simulacao usa somente os passageiros do novo localizador. Confira a politica e escolha os novos voos.");
        } else if (retomada != null) {
            dados.put("reiniciadaSemConfirmacao", true);
            dados.set("conferenciaAnterior", retomada.path("consultaFornecedor"));
            dados.put("avisoRetomada", "A tentativa anterior nao teve confirmacao de reemissao. Iniciamos uma nova simulacao: confira a politica, escolha os voos e aprove novamente os valores. Qualquer pendencia anterior sera conferida antes de novo pagamento.");
            if (preparoSemEnvioFinanceiro(retomada)) dados.put("simulacaoAnteriorId", retomada.path("id").asText());
            else if (retomada.path("operacoesPendentes").isArray()) dados.set("tentativasAnteriores", retomada.get("operacoesPendentes"));
            else dados.putArray("tentativasAnteriores").add(retomada.path("id").asText());
        }
        ReemissaoAzulRegistro r = new ReemissaoAzulRegistro(); r.setId(UUID.randomUUID().toString()); r.setLocalizador(loc);
        r.setCodgUsuario(sessao.getUsuario().getCodgUsuario()); r.setCodgAgencia(sessao.getAgencia().getCodgAgencia()); r.setReservaId(reservaId);
        r.setExpiraEm(LocalDateTime.now().plusMinutes(30));
        return visualizar(salvar(r, dados, dados.path("original").path("passageiros").size() > 1 && !dados.path("passageirosConfirmados").asBoolean() ? "PASSAGEIROS" : "POLITICA"));
    }

    public ObjectNode executar(String id, String acao, ObjectNode pedido) {
        SessaoChatResponse sessao = sessao(pedido);
        SessaoTemporaria temporaria = sessoes.get(UUID.fromString(id).toString());
        exigir(temporaria != null, 409, "A simulacao expirou. Reabra a reserva; confira o historico se a reemissao ja foi enviada.");
        synchronized (temporaria) {
            ReemissaoAzulRegistro r = mapper.convertValue(temporaria.registro, ReemissaoAzulRegistro.class);
            return executar(r, acao, pedido, sessao);
        }
    }

    private ObjectNode executar(ReemissaoAzulRegistro r, String acao, ObjectNode pedido, SessaoChatResponse sessao) {
        exigir(r != null && Objects.equals(r.getCodgUsuario(), sessao.getUsuario().getCodgUsuario()) && Objects.equals(r.getCodgAgencia(), sessao.getAgencia().getCodgAgencia()), 403, "Solicitacao nao pertence ao usuario e agencia atuais.");
        if ("consultar".equals(acao) && "DIVISAO_PENDENTE".equals(r.getStatus())) return recuperarDivisao(r, sessao);
        if ("consultar".equals(acao)) return retomarSeSemConfirmacao(consultar(r), sessao);
        exigir(!dados(r).path("somenteAcompanhamento").asBoolean(), 409, "Este acompanhamento permite apenas consultar a tentativa anterior na Azul.");
        exigir(pedido.path("versao").canConvertToInt() && r.getVersao().equals(pedido.get("versao").intValue()), 409, "A solicitacao mudou. Atualize o andamento antes de continuar.");
        exigir(r.getExpiraEm().isAfter(LocalDateTime.now()), 409, "A solicitacao expirou. Atualize o andamento.");
        ObjectNode dados = dados(r);
        dados.remove("falhaRecuperavel");
        if ("trocar-passageiros".equals(acao)) { etapa(r, "POLITICA", "DIVISAO_CONFIRMACAO"); return visualizar(salvar(r, dados, "PASSAGEIROS")); }
        if ("passageiros".equals(acao)) {
            etapa(r, "PASSAGEIROS");
            JsonNode selecao = pedido.path("passageiroKeys"); Set<String> keys = new LinkedHashSet<>();
            exigir(selecao.isArray() && !selecao.isEmpty(), 400, "Selecione quem deseja reemitir.");
            Set<String> disponiveis = new HashSet<>(); dados.path("original").path("passageiros").forEach(p -> disponiveis.add(p.path("chave").asText()));
            for (JsonNode key : selecao) exigir(key.isTextual() && disponiveis.contains(key.asText()) && keys.add(key.asText()), 400, "Selecao de passageiros invalida.");
            dados.set("passageirosSelecionados", selecao.deepCopy());
            if (keys.size() == disponiveis.size()) {
                dados.put("passageirosConfirmados", true); dados.remove("planoDivisao");
                return visualizar(salvar(r, dados, "POLITICA"));
            }
            ObjectNode consulta = pedidoHub(r); consulta.set("originalEsperada", dados.get("original")); consulta.set("passageiroKeys", selecao);
            JsonNode validacao = hub.executar("divisao/validar", consulta);
            exigir(validacao != null && "VALIDADA".equals(validacao.path("status").asText()), 409, mensagem(validacao));
            ObjectNode plano = mapper.createObjectNode(); plano.set("originalEsperada", dados.get("original")); plano.set("passageiroKeys", selecao);
            JsonNode retorno = managerDivisao(r, "planejar", plano);
            exigir(retorno != null && retorno.hasNonNull("assinatura"), 503, "Nao foi possivel conferir os valores da separacao.");
            dados.set("planoDivisao", retorno); return visualizar(salvar(r, dados, "DIVISAO_CONFIRMACAO"));
        }
        if ("confirmar-divisao".equals(acao)) {
            etapa(r, "DIVISAO_CONFIRMACAO"); exigir(pedido.path("aceite").asBoolean(), 400, "Confirme a separacao dos passageiros em outra reserva.");
            ObjectNode consulta = pedidoHub(r); consulta.set("originalEsperada", dados.get("original")); consulta.set("passageiroKeys", dados.get("passageirosSelecionados"));
            JsonNode validacao = hub.executar("divisao/validar", consulta);
            exigir(validacao != null && "VALIDADA".equals(validacao.path("status").asText()), 409, mensagem(validacao));
            ObjectNode autorizacao = consulta.deepCopy(); autorizacao.put("aceite", true); autorizacao.set("plano", dados.get("planoDivisao"));
            JsonNode autorizada = managerDivisao(r, "autorizar", autorizacao);
            exigir(autorizada != null && autorizada.path("autorizada").isBoolean(), 503, "Autorizacao da divisao nao confirmada. Atualize o andamento.");
            r = salvar(r, dados, "DIVISAO_PENDENTE");
            JsonNode resultado;
            try { resultado = hub.executar(autorizada.path("autorizada").asBoolean() ? "divisao/dividir" : "divisao/recuperar", consulta); }
            catch (RuntimeException ex) { resultado = null; }
            return concluirDivisao(r, sessao, resultado);
        }
        if ("aceitar".equals(acao)) {
            etapa(r, "POLITICA"); exigir(pedido.path("aceite").asBoolean() && dados.path("politicaVersao").asText().equals(pedido.path("politicaVersao").asText()), 400, "Aceite a politica exibida para continuar.");
            dados.put("aceiteEm", Instant.now().toString()); return visualizar(salvar(r, dados, "TRECHOS"));
        }
        if ("trechos".equals(acao)) {
            etapa(r, "TRECHOS"); ArrayNode indices = mapper.createArrayNode();
            for (JsonNode i : pedido.path("indices")) {
                exigir(i.isIntegralNumber() && i.intValue() >= 0 && i.intValue() < dados.path("original").path("trechos").size(), 400, "Trecho invalido.");
                exigir(dados.path("original").path("trechos").get(i.intValue()).path("elegivel").asBoolean(), 409, "O trecho escolhido ja embarcou ou esta em no-show.");
                boolean duplicado = false; for (JsonNode anterior : indices) if (anterior.equals(i)) duplicado = true;
                exigir(!duplicado, 400, "Trecho duplicado."); indices.add(i);
            }
            exigir(indices.size() > 0 && indices.size() <= 2, 400, "Selecione ida, volta ou ida e volta.");
            List<Integer> ordenados = new ArrayList<>(); indices.forEach(i -> ordenados.add(i.intValue())); Collections.sort(ordenados);
            for (JsonNode politica : dados.path("politicas")) {
                if (politica.path("regra").path("exigeRemarcacaoConjunta").asBoolean()) exigir(ordenados.size() == dados.path("original").path("trechos").size(), 409, "A politica exige a troca de ida e volta juntas.");
            }
            dados.set("selecionados", mapper.valueToTree(ordenados)); dados.putArray("escolhas"); dados.put("ordem", 0);
            return visualizar(salvar(r, dados, "PESQUISA"));
        }
        if ("pesquisar".equals(acao)) {
            etapa(r, "PESQUISA", "RESULTADOS");
            validarReserva(r.getReservaId(), r.getCodgAgencia());
            JsonNode original = trechoAtual(dados);
            LocalDate data = LocalDate.parse(pedido.path("data").asText());
            exigir(!data.isBefore(LocalDate.now(ZoneId.of("America/Sao_Paulo"))) && !data.isAfter(LocalDate.now().plusYears(1)), 400, "Selecione uma data valida para a viagem.");
            validarCronologia(dados, data, null);
            PesquisaRequestDTO busca = new PesquisaRequestDTO(); busca.setAgencia(r.getCodgAgencia().toString()); busca.setUnidade(sessao.getUnidade().getCodgUnidade().toString());
            busca.setTipoPesquisa(TipoPesquisa.ONEWAY); busca.setTipoConsulta("NACIONAL".equals(dados.path("politicas").get(original.path("indice").asInt()).path("mercado").asText()) ? TipoConsulta.NACIONAL : TipoConsulta.INTERNACIONAL);
            busca.setAeroportoOrigem(new Aeroporto(original.path("origem").asText(), "")); busca.setAeroportoDestino(new Aeroporto(original.path("destino").asText(), ""));
            busca.setDataIda(java.sql.Date.valueOf(data)); busca.setSistemas(List.of(new Sistema(0, "Azul", 0)));
            com.confApi.model.IdentificacaoAgenciaModel identificacao = new com.confApi.model.IdentificacaoAgenciaModel(); identificacao.setCodgAgencia(r.getCodgAgencia()); identificacao.setCodgUsuario(r.getCodgUsuario()); identificacao.setCodgUnidade(sessao.getUnidade().getCodgUnidade()); busca.setIdentificacaoAgenciaModel(identificacao);
            busca.setPesquisa(new ParametrosPesquisa(Classe.ECONOMICA, Ordenacao.DURACAO, TipoBagagem.TODAS_AS_OPCOES, TipoTarifa.RT));
            int adt = 0, chd = 0, inf = 0;
            for (JsonNode pax : dados.path("original").path("passageiros")) {
                switch (pax.path("tipo").asText()) { case "ADT" -> adt++; case "CHD" -> chd++; case "INF" -> inf++; default -> throw erro(409, "Tipo de passageiro desconhecido."); }
                if (pax.path("bebeAssociado").asBoolean()) inf++;
            }
            busca.setQtdADT(adt); busca.setQtdCHD(chd); busca.setQtdINF(inf);
            List<PesquisaResponse> resposta = aereo.pesquisarDisponibilidadeEstrita(busca);
            exigir(resposta != null, 503, "Pesquisa indisponivel. Tente novamente.");
            ArrayNode opcoes = mapper.createArrayNode();
            for (PesquisaResponse lote : resposta) {
                if (lote == null || lote.getException() != null || lote.getTrechos1() == null) continue;
                for (Trecho trecho : lote.getTrechos1()) {
                    if (!"Azul".equalsIgnoreCase(trecho.getSistema()) || trecho.getVoos() == null || trecho.getVoos().isEmpty() || trecho.getFamilias() == null || trecho.getFamilias().isEmpty()) continue;
                    ObjectNode opcao = mapper.valueToTree(trecho);
                    if (!rotaCompativel(original, opcao)) continue;
                    if (vooJaReservado(dados, opcao.path("identificacaoDaViagem").asText())) continue;
                    if (!dataVoo(opcao.path("voos").get(0), true).toLocalDate().equals(data)) continue;
                    // Entrega o mesmo horario local para tela, validacao cronologica e Manager.
                    for (JsonNode voo : opcao.path("voos")) {
                        LocalDateTime partida = dataVoo(voo, true), chegada = dataVoo(voo, false);
                        ((ObjectNode) voo).put("dataPartida", partida.toString()).put("dataChegada", chegada.toString());
                    }
                    opcao.put("indice", opcoes.size()); opcoes.add(opcao);
                }
            }
            dados.set("opcoes", opcoes); dados.put("dataPesquisa", data.toString());
            dados.put("mensagem", opcoes.isEmpty() ? "Nenhum voo alternativo disponivel para esta data. Escolha outra data para pesquisar." : "Escolha o voo e a familia tarifaria.");
            return visualizar(salvar(r, dados, "RESULTADOS"));
        }
        if ("selecionar".equals(acao)) {
            etapa(r, "RESULTADOS"); JsonNode voo = item(dados.path("opcoes"), inteiro(pedido, "opcao")); JsonNode familia = item(voo.path("familias"), inteiro(pedido, "familia"));
            exigir(!voo.path("identificacaoDaViagem").asText().isBlank() && !familia.path("identificacaoDeVoo").asText().isBlank(), 409, "Oferta sem identificacao da Azul.");
            validarNovoVoo(dados, voo.path("identificacaoDaViagem").asText());
            ObjectNode escolha = mapper.createObjectNode(); escolha.set("original", trechoAtual(dados)); escolha.set("voo", voo); escolha.set("familia", familia); escolha.put("data", dados.path("dataPesquisa").asText());
            validarCronologia(dados, LocalDate.parse(escolha.path("data").asText()), escolha);
            ((ArrayNode) dados.get("escolhas")).add(escolha); dados.remove("opcoes");
            if (dados.path("escolhas").size() < dados.path("selecionados").size()) {
                dados.put("ordem", dados.path("ordem").asInt() + 1);
                // Conserva a ordem das etapas ao selecionar mais de um trecho.
                r = salvar(r, dados, "SELECAO"); return visualizar(salvar(r, dados, "PESQUISA"));
            }
            return visualizar(salvar(r, dados, "SELECAO"));
        }
        if ("confirmar-voos".equals(acao)) { etapa(r, "SELECAO"); return visualizar(salvar(r, dados, "SERVICOS_OPCAO")); }
        if ("preparar".equals(acao)) {
            etapa(r, "SERVICOS_OPCAO"); JsonNode reservaAtual = validarReserva(r.getReservaId(), r.getCodgAgencia());
            reconciliarAntesDaCotacao(r, dados, reservaAtual);
            for (JsonNode escolha : dados.path("escolhas")) {
                ObjectNode regraAtual = consultarRegra(escolha.path("original"), reservaAtual);
                JsonNode aceita = dados.path("politicas").get(escolha.path("original").path("indice").asInt()).path("regra");
                exigir(regraAtual.path("regra").path("id").asText().equals(aceita.path("id").asText()) && regraAtual.path("regra").path("dataAtualizacao").asText().equals(aceita.path("dataAtualizacao").asText()), 409, "A politica foi atualizada. Encerre esta solicitacao e aceite as novas condicoes antes de continuar.");
            }
            ObjectNode consulta = pedidoHub(r); consulta.set("pedido", montarPedido(dados, r.getLocalizador())); consulta.put("buscarServicos", pedido.path("adicionarServicos").asBoolean());
            consulta.set("originalEsperada", dados.get("original"));
            r = salvar(r, dados, "PREPARANDO");
            return chamadaRemota(r, dados, "preparar", consulta, "SERVICOS", "servicosFornecedor");
        }
        if ("cotar".equals(acao)) {
            etapa(r, "SERVICOS"); ObjectNode consulta = pedidoHub(r); consulta.set("servicos", pedido.path("servicos").isArray() ? pedido.get("servicos") : mapper.createArrayNode());
            for (JsonNode servico : consulta.path("servicos")) exigir(servico.path("indice").isIntegralNumber() && servico.path("quantidade").isIntegralNumber() && servico.path("quantidade").asInt() > 0, 400, "Selecione quantidades inteiras para os servicos.");
            dados.set("servicosSelecionados", consulta.get("servicos")); r = salvar(r, dados, "CALCULANDO");
            return chamadaRemota(r, dados, "cotar", consulta, "COMPARATIVO", "cotacao");
        }
        if ("aprovar".equals(acao)) {
            etapa(r, "COMPARATIVO"); exigir(pedido.path("aceite").asBoolean(), 400, "Confirme os valores para continuar.");
            dados.put("valorAprovado", dados.path("cotacao").path("valorAdicional").decimalValue()); dados.put("valorAprovadoEm", Instant.now().toString());
            dados.put("faturadoDisponivel", faturado(sessao, dados.path("valorAprovado").decimalValue()));
            return visualizar(salvar(r, dados, "PAGAMENTO"));
        }
        if ("concluir".equals(acao)) {
            etapa(r, "PAGAMENTO"); JsonNode reservaAtual = validarReserva(r.getReservaId(), r.getCodgAgencia());
            exigir(totalVenda(reservaAtual).compareTo(dados.path("original").path("valorVenda").decimalValue()) == 0,
                    409, "Os valores da reserva mudaram desde o aceite. Confira a reserva antes de concluir a reemissao.");
            BigDecimal valor = dados.path("valorAprovado").decimalValue();
            JsonNode pagamento = pedido.path("pagamento");
            int forma = pagamento.path("formaDePagamento").asInt();
            if (valor.signum() > 0) {
                exigir(forma == 1 || forma == 2, 400, "Selecione uma forma de pagamento disponivel.");
                if (forma == 1) exigir(faturado(sessao, valor), 409, "Limite faturado indisponivel ou insuficiente.");
                if (forma == 2) exigir("VI".equals(pagamento.path("bandeiraCartao").asText()) && pagamento.path("numeroCartao").asText().matches("[0-9]{13,19}") && pagamento.path("codigoDeSeguranca").asText().matches("[0-9]{3}") && !pagamento.path("titularNome").asText().isBlank() && pagamento.path("validade").asText().matches("(0[1-9]|1[0-2])/[0-9]{2}([0-9]{2})?") && pagamento.path("parcelas").isIntegralNumber() && pagamento.path("parcelas").asInt() >= 1 && pagamento.path("parcelas").asInt() <= 12, 400, "Confira os dados do cartao Visa.");
            }
            ObjectNode consulta = pedidoHub(r); consulta.put("valorAutorizado", valor); consulta.set("pagamento", pagamento);
            // A reserva original ja foi conferida no preparo, na mesma sessao da troca.
            // Nao abrir outra autenticacao Azul entre a cotacao e o checkout.
            // A solicitacao definitiva e registrada somente nesta ultima etapa.
            JsonNode disponibilidadeFinal = manager.get(BASE + "/" + r.getReservaId() + "/disponibilidade", JsonNode.class);
            exigir(disponibilidadeFinal != null && disponibilidadeFinal.path("disponivel").asBoolean(), 409,
                    "Existe uma tentativa anterior sem reconciliacao financeira. Reabra a reemissao para conferir os pagamentos na Azul antes de uma nova cotacao.");
            JsonNode autorizacao;
            try { autorizacao = manager.post(BASE + "/" + r.getReservaId() + "/solicitar", r, JsonNode.class); }
            catch (RegraDeNegocioException ex) {
                if (ex.getStatus() == 409) throw erro(409, "Outra tentativa desta reserva esta pendente no historico. Atualize o andamento antes de enviar novo pagamento.");
                throw ex;
            }
            exigir(autorizacao != null && autorizacao.path("autorizada").asBoolean(), 409,
                    "Nao foi possivel confirmar o envio da reemissao. Confira o historico antes de tentar novamente.");
            dados.put("envioFinalRegistrado", true);
            // Dados financeiros trafegam apenas nesta requisicao, sem integrar a sessao nem o historico.
            dados.put("formaPagamento", forma); dados.put("parcelas", forma == 2 ? pagamento.path("parcelas").asInt(1) : 1); r = salvar(r, dados, "PROCESSANDO");
            return chamadaRemota(r, dados, "concluir", consulta, "CONCLUIDA", "confirmacao");
        }
        if ("voltar".equals(acao)) {
            String destino = switch (r.getStatus()) { case "DIVISAO_CONFIRMACAO" -> "PASSAGEIROS"; case "PESQUISA" -> "TRECHOS"; case "RESULTADOS", "SELECAO" -> "PESQUISA"; case "SERVICOS_OPCAO" -> "SELECAO"; case "PAGAMENTO" -> "COMPARATIVO"; default -> throw erro(409, "Esta etapa nao permite voltar. Consulte o andamento."); };
            if ("PESQUISA".equals(destino) || "TRECHOS".equals(destino)) { dados.putArray("escolhas"); dados.put("ordem", 0); dados.remove("opcoes"); }
            return visualizar(salvar(r, dados, destino));
        }
        if ("cancelar".equals(acao)) { exigir(!REMOTAS.contains(r.getStatus()), 409, "Operacao ja iniciada na Azul. Consulte o atendimento para conferencia."); return visualizar(salvar(r, dados, "CANCELADA")); }
        throw erro(400, "Acao desconhecida.");
    }

    private JsonNode managerDivisao(ReemissaoAzulRegistro r, String acao, ObjectNode dados) {
        ReemissaoAzulRegistro contexto = mapper.convertValue(r, ReemissaoAzulRegistro.class); contexto.setDadosJson(dados.toString());
        return manager.post(BASE + "/" + r.getReservaId() + "/divisao/" + acao, contexto, JsonNode.class);
    }
    private ObjectNode abrirDivisaoPendente(SessaoChatResponse sessao, int reservaId, String loc, String id) {
        UUID.fromString(id); SessaoTemporaria existente = sessoes.get(id);
        exigir(existente == null || Objects.equals(existente.registro.getCodgUsuario(), sessao.getUsuario().getCodgUsuario()), 409, "A divisao esta sendo acompanhada por outro usuario da agencia.");
        ReemissaoAzulRegistro r = new ReemissaoAzulRegistro(); r.setId(id); r.setReservaId(reservaId); r.setLocalizador(loc);
        r.setCodgUsuario(sessao.getUsuario().getCodgUsuario()); r.setCodgAgencia(sessao.getAgencia().getCodgAgencia()); r.setExpiraEm(LocalDateTime.now().plusMinutes(30));
        ObjectNode dados = mapper.createObjectNode().put("envioFinalRegistrado", false).put("confirmadaAzul", false);
        registrarAcompanhamento(r, dados, "DIVISAO_PENDENTE"); return recuperarDivisao(r, sessao);
    }
    private ObjectNode recuperarDivisao(ReemissaoAzulRegistro r, SessaoChatResponse sessao) {
        ObjectNode dados = dados(r);
        try {
            JsonNode estado = managerDivisao(r, "consultar", mapper.createObjectNode());
            exigir(estado != null, 503, "Nao foi possivel consultar a divisao registrada.");
            if ("CANCELADA".equals(estado.path("status").asText())) {
                JsonNode reserva = validarReserva(r.getReservaId(), r.getCodgAgencia());
                ObjectNode nova = iniciarSimulacao(sessao, r.getReservaId(), r.getLocalizador(), reserva, null);
                dados.put("substituidaPor", nova.path("id").asText()); registrarAcompanhamento(r, dados, "CANCELADA"); return nova;
            }
            if ("CONCLUIDA".equals(estado.path("status").asText())) return abrirReservaDividida(r, sessao, estado.path("reservaId").asInt());
            JsonNode plano = estado.path("plano"); exigir(plano.hasNonNull("originalEsperada") && plano.hasNonNull("passageiroKeys"), 503, "Plano da divisao incompleto.");
            dados.set("original", plano.get("originalEsperada")); dados.set("planoDivisao", plano);
            registrarAcompanhamento(r, dados, "DIVISAO_PENDENTE");
            ObjectNode consulta = pedidoHub(r); consulta.set("originalEsperada", plano.get("originalEsperada")); consulta.set("passageiroKeys", plano.get("passageiroKeys"));
            return concluirDivisao(r, sessao, hub.executar("divisao/recuperar", consulta));
        } catch (RuntimeException ex) {
            dados.put("mensagem", "A separacao dos passageiros esta pendente de conferencia. Atualize o andamento; nenhuma nova divisao sera enviada.");
            return visualizar(registrarAcompanhamento(r, dados, "DIVISAO_PENDENTE"));
        }
    }
    private ObjectNode concluirDivisao(ReemissaoAzulRegistro r, SessaoChatResponse sessao, JsonNode resultado) {
        ObjectNode dados = dados(r);
        if (resultado == null || !"DIVIDIDA".equals(resultado.path("status").asText())) {
            if (resultado != null && "ERRO".equals(resultado.path("status").asText()) && resultado.path("divisaoEnviada").isBoolean()
                    && !resultado.path("divisaoEnviada").asBoolean() && r.getId().equals(resultado.path("operacaoId").asText())) {
                ObjectNode falha = mapper.createObjectNode(); falha.set("falha", resultado);
                JsonNode encerrada = managerDivisao(r, "encerrar-sem-envio", falha);
                exigir(encerrada != null && "CANCELADA".equals(encerrada.path("status").asText()), 503, "Encerramento da divisao sem envio nao confirmado.");
                JsonNode reserva = validarReserva(r.getReservaId(), r.getCodgAgencia());
                ObjectNode nova = iniciarSimulacao(sessao, r.getReservaId(), r.getLocalizador(), reserva, null);
                dados.put("substituidaPor", nova.path("id").asText()); registrarAcompanhamento(r, dados, "CANCELADA"); return nova;
            }
            dados.put("mensagem", resultado == null ? "A resposta da separacao nao foi confirmada. Atualize o andamento sem reenviar a divisao." : mensagem(resultado));
            return visualizar(registrarAcompanhamento(r, dados, "DIVISAO_PENDENTE"));
        }
        try {
            ObjectNode sincronizar = mapper.createObjectNode(); sincronizar.set("divisao", resultado);
            JsonNode sincronizada = managerDivisao(r, "sincronizar", sincronizar);
            exigir(sincronizada != null && "CONCLUIDA".equals(sincronizada.path("status").asText()) && sincronizada.path("reservaId").isIntegralNumber(), 503, "Registro da divisao pendente.");
            return abrirReservaDividida(r, sessao, sincronizada.path("reservaId").asInt());
        } catch (RuntimeException ex) {
            dados.put("mensagem", "A Azul confirmou a separacao. O registro local esta pendente; Atualizar andamento repete somente a conferencia e a sincronizacao.");
            return visualizar(registrarAcompanhamento(r, dados, "DIVISAO_PENDENTE"));
        }
    }
    private ObjectNode abrirReservaDividida(ReemissaoAzulRegistro anterior, SessaoChatResponse sessao, int destino) {
        exigir(destino > 0 && destino != anterior.getReservaId(), 409, "Destino da divisao invalido.");
        JsonNode reserva = validarReserva(destino, sessao.getAgencia().getCodgAgencia());
        ObjectNode retomada = mapper.createObjectNode().put("divisaoConcluida", true).put("reservaOrigemId", anterior.getReservaId()).put("localizadorOrigem", anterior.getLocalizador());
        ObjectNode nova = iniciarSimulacao(sessao, destino, reserva.path("localizador").asText(), reserva, retomada);
        ObjectNode dados = dados(anterior); dados.put("substituidaPor", nova.path("id").asText()); registrarAcompanhamento(anterior, dados, "CONCLUIDA");
        return nova;
    }

    private ObjectNode chamadaRemota(ReemissaoAzulRegistro r, ObjectNode dados, String acao, ObjectNode pedido, String sucesso, String campo) {
        JsonNode resposta;
        try { resposta = hub.executar(acao, pedido); }
        catch (RuntimeException ex) { dados.put("mensagem", "A comunicacao foi interrompida. A operacao precisa de conferencia antes de qualquer nova tentativa."); return visualizar(salvar(r, dados, "CONFERENCIA")); }
        String esperado = switch (acao) { case "preparar" -> "SERVICOS"; case "cotar" -> "COTADA"; default -> "CONCLUIDA"; };
        boolean valido = resposta != null && esperado.equals(resposta.path("status").asText());
        if (valido && "cotar".equals(acao)) valido = resposta.path("valorAdicional").isNumber() && resposta.path("valorAdicional").decimalValue().signum() >= 0 && resposta.path("valorOriginal").isNumber() && dados.path("original").path("moeda").asText().equals(resposta.path("moeda").asText());
        if (valido && Set.of("concluir", "consultar-resultado").contains(acao)) valido = resposta.path("valorPago").isNumber() && resposta.path("valorPago").decimalValue().compareTo(dados.path("valorAprovado").decimalValue()) == 0 && r.getLocalizador().equals(resposta.path("original").path("localizador").asText());
        if (!valido) {
            dados.put("mensagem", mensagem(resposta));
            registrarFalhaAzul(dados, resposta);
            if (Set.of("concluir", "consultar-resultado").contains(acao) && semEfetivacaoConfirmada(resposta)) {
                dados.set("falhaConclusao", resposta);
                ReemissaoAzulRegistro encerramento = mapper.convertValue(r, ReemissaoAzulRegistro.class);
                encerramento.setDadosJson(dados.toString());
                try {
                    JsonNode encerrada = manager.post(BASE + "/" + r.getReservaId() + "/encerrar-sem-efetivacao", encerramento, JsonNode.class);
                    exigir(encerrada != null && encerrada.path("encerrada").asBoolean(), 503, "Encerramento da tentativa nao confirmado.");
                } catch (RuntimeException ex) {
                    dados.put("mensagem", "A reemissao nao foi efetivada na Azul. O registro do encerramento esta pendente; atualize o andamento antes de tentar novamente.");
                    return visualizar(salvar(r, dados, "CONFERENCIA"));
                }
                return visualizar(salvar(r, dados, "NAO_CONCLUIDA"));
            }
            boolean recuperavel = resposta != null && "ERRO".equals(resposta.path("status").asText())
                    && resposta.path("requerConferencia").isBoolean() && !resposta.path("requerConferencia").asBoolean()
                    && resposta.path("falhaRecuperavel").asBoolean();
            if (recuperavel && Set.of("preparar", "cotar").contains(acao)) {
                dados.put("falhaRecuperavel", true);
                return visualizar(salvar(r, dados, "preparar".equals(acao) ? "SELECAO" : "SERVICOS"));
            }
            return visualizar(salvar(r, dados, "CONFERENCIA"));
        }
        if ("cotar".equals(acao) && dados.path("original").path("valorVenda").isNumber()) {
            ObjectNode cotacao = resposta.deepCopy(); cotacao.set("valorOriginalFornecedor", resposta.get("valorOriginal")); cotacao.set("valorOriginal", dados.path("original").get("valorVenda")); resposta = cotacao;
        }
        dados.set(campo, resposta);
        if (resposta.has("expiraEm")) r.setExpiraEm(LocalDateTime.ofInstant(Instant.parse(resposta.path("expiraEm").asText()), ZoneId.systemDefault()));
        if ("CONCLUIDA".equals(sucesso)) {
            dados.put("confirmadaAzul", true);
            dados.put("mensagem", "Reemissao confirmada pela Azul. Atualizando os registros da reserva e do pagamento.");
            return sincronizar(salvar(r, dados, "SINCRONIZANDO"));
        }
        return visualizar(salvar(r, dados, sucesso));
    }
    private ObjectNode sincronizar(ReemissaoAzulRegistro r) {
        ObjectNode dados = dados(r);
        try {
            ReemissaoAzulRegistro contexto = mapper.convertValue(r, ReemissaoAzulRegistro.class);
            ObjectNode dadosManager = dados.deepCopy();
            for (JsonNode escolha : dadosManager.path("escolhas")) for (JsonNode segmento : escolha.path("voo").path("voos")) {
                LocalDateTime partida = dataVoo(segmento, true), chegada = dataVoo(segmento, false);
                ((ObjectNode) segmento).put("dataPartida", partida.toString()).put("horaPartida", partida.toLocalTime().toString());
                ((ObjectNode) segmento).put("dataChegada", chegada.toString()).put("horaChegada", chegada.toLocalTime().toString());
            }
            contexto.setDadosJson(dadosManager.toString());
            JsonNode resposta = manager.post(BASE + "/" + r.getReservaId() + "/sincronizar", contexto, JsonNode.class);
            exigir(resposta != null && resposta.path("sincronizada").asBoolean(), 503, "Atualizacao local pendente.");
        } catch (RuntimeException ex) { ObjectNode v = visualizar(r); v.put("mensagem", "A Azul confirmou a reemissao. A atualizacao dos registros locais esta pendente; use Atualizar andamento para concluir, sem repetir a cobranca."); return v; }
        dados.put("mensagem", "Reemissao confirmada pela Azul. Reserva e pagamento registrados.");
        return visualizar(salvar(r, dados, "CONCLUIDA"));
    }
    private ObjectNode consultar(ReemissaoAzulRegistro r) {
        ObjectNode dadosAtuais = dados(r);
        SessaoTemporaria substituta = sessoes.get(dadosAtuais.path("substituidaPor").asText());
        if (substituta != null) return visualizar(substituta.registro);
        if ("DIVISAO_PENDENTE".equals(r.getStatus())) return visualizar(r);
        if (dadosAtuais.path("somenteAcompanhamento").asBoolean()) return conferirAcompanhamento(r);
        if ("SINCRONIZANDO".equals(r.getStatus())) return sincronizar(r);
        if ("CONFERENCIA".equals(r.getStatus()) && preparoSemEnvioFinanceiro(dadosAtuais)) {
            JsonNode consulta = atualizarConsultaFornecedor(r, dadosAtuais);
            if (consulta == null) dadosAtuais.put("mensagem", dadosAtuais.path("erroConsultaFornecedor").asText());
            else if (!confirmacaoCorrespondeAConsulta(dadosAtuais.path("original"), consulta.path("reserva")))
                dadosAtuais.put("mensagem", "A reserva consultada na Azul difere da simulacao interrompida ou possui saldo pendente. Confira a reserva antes de iniciar outra tentativa.");
            return visualizar(registrarAcompanhamento(r, dadosAtuais, "CONFERENCIA"));
        }
        if ("CONFERENCIA".equals(r.getStatus()) && dadosAtuais.path("envioFinalRegistrado").asBoolean()) {
            atualizarConsultaFornecedor(r, dadosAtuais);
            return chamadaRemota(r, dadosAtuais, "consultar-resultado", pedidoHub(r), "CONCLUIDA", "confirmacao");
        }
        if (r.getExpiraEm().isBefore(LocalDateTime.now()) && !Set.of("CONCLUIDA", "NAO_CONCLUIDA", "CONFERENCIA", "CANCELADA", "EXPIRADA").contains(r.getStatus())) {
            ObjectNode dados = dados(r); dados.put("mensagem", REMOTAS.contains(r.getStatus()) ? "Solicite a conferencia da operacao com a Azul antes de reiniciar." : "A solicitacao expirou. Inicie uma nova solicitacao.");
            r = salvar(r, dados, REMOTAS.contains(r.getStatus()) ? "CONFERENCIA" : "EXPIRADA");
        }
        return visualizar(r);
    }

    private ObjectNode abrirAcompanhamento(SessaoChatResponse sessao, int reservaId, String localizador, JsonNode disponibilidade) {
        ObjectNode dados = mapper.createObjectNode().put("somenteAcompanhamento", true).put("envioFinalRegistrado", true).put("confirmadaAzul", false);
        ArrayNode referencias = dados.putArray("operacoesPendentes");
        for (JsonNode referencia : disponibilidade.path("operacoesPendentes")) {
            try { referencias.add(UUID.fromString(referencia.asText()).toString()); }
            catch (RuntimeException referenciaAntiga) { /* A consulta ao fornecedor continua disponivel. */ }
        }
        if (!referencias.isEmpty()) dados.put("operacaoAnteriorId", referencias.get(0).asText());
        ReemissaoAzulRegistro r = new ReemissaoAzulRegistro(); r.setId(UUID.randomUUID().toString()); r.setLocalizador(localizador);
        r.setReservaId(reservaId); r.setCodgUsuario(sessao.getUsuario().getCodgUsuario()); r.setCodgAgencia(sessao.getAgencia().getCodgAgencia());
        r.setExpiraEm(LocalDateTime.now().plusMinutes(30));
        registrarAcompanhamento(r, dados, "CONFERENCIA");
        return conferirAcompanhamento(r);
    }

    private void reconciliarAntesDaCotacao(ReemissaoAzulRegistro r, ObjectNode dados, JsonNode reservaAtual) {
        JsonNode disponibilidade = manager.get(BASE + "/" + r.getReservaId() + "/disponibilidade", JsonNode.class);
        exigir(disponibilidade != null && disponibilidade.path("disponivel").isBoolean(), 503, "Nao foi possivel consultar as tentativas anteriores.");
        if (disponibilidade.path("disponivel").asBoolean()) return;
        JsonNode pendentes = disponibilidade.path("tentativasPendentes");
        exigir(pendentes.isArray() && !pendentes.isEmpty(), 409,
                "A tentativa anterior precisa de data e referencia para conferir os pagamentos na Azul. Atualize o Manager e reabra a reserva.");
        for (JsonNode pendente : pendentes) {
            String id = pendente.path("operacaoId").asText();
            try { UUID.fromString(id); Instant.parse(pendente.path("enviadaEm").asText()); }
            catch (RuntimeException ex) { throw erro(409, "Referencia ou data da tentativa anterior indisponivel para reconciliacao."); }
            ObjectNode pedido = mapper.createObjectNode().put("localizador", r.getLocalizador()).put("operacaoId", id)
                    .put("tentativaEnviadaEm", pendente.path("enviadaEm").asText()).put("valorOriginalBanco", totalVenda(reservaAtual));
            pedido.set("originalEsperada", dados.get("original"));
            JsonNode prova = hub.executar("reconciliar-anterior", pedido);
            exigir(prova != null && "RECONCILIADA".equals(prova.path("status").asText())
                    && "Azul".equals(prova.path("fonte").asText()) && id.equals(prova.path("operacaoId").asText())
                    && r.getLocalizador().equals(prova.path("localizador").asText()) && prova.path("semPagamentoAdicionalRegistrado").asBoolean(),
                    409, prova != null && "CONCLUIDA".equals(prova.path("status").asText())
                            ? "A Azul ja confirmou a tentativa anterior. Reabra a reserva para conferir o itinerario antes de outra operacao." : mensagem(prova));
            ObjectNode comprovacao = mapper.createObjectNode(); comprovacao.set("reconciliacao", prova);
            comprovacao.putObject("original").put("valorVenda", totalVenda(reservaAtual));
            ReemissaoAzulRegistro contexto = mapper.convertValue(r, ReemissaoAzulRegistro.class);
            contexto.setId(id); contexto.setStatus("CONFERENCIA"); contexto.setDadosJson(comprovacao.toString());
            JsonNode registrada = manager.post(BASE + "/" + r.getReservaId() + "/reconciliar", contexto, JsonNode.class);
            exigir(registrada != null && registrada.path("reconciliada").asBoolean(), 409, "A conferencia financeira precisa ser registrada antes da nova cotacao.");
        }
        JsonNode atualizada = manager.get(BASE + "/" + r.getReservaId() + "/disponibilidade", JsonNode.class);
        exigir(atualizada != null && atualizada.path("disponivel").asBoolean(), 409, "Ainda existe uma tentativa pendente para esta reserva. Atualize o acompanhamento.");
        dados.put("reconciliacaoAnteriorConfirmada", true);
        dados.put("avisoRetomada", "A tentativa anterior foi reconciliada com a reserva e os pagamentos registrados na Azul. Confira a nova cotacao antes de confirmar a reemissao.");
    }

    private boolean preparoSemEnvioFinanceiro(JsonNode estado) {
        if (!estado.path("envioFinalRegistrado").isBoolean() || estado.path("envioFinalRegistrado").asBoolean()
                || estado.path("confirmadaAzul").asBoolean()) return false;
        for (String campo : List.of("pagamentoEnviado", "pagamentoConfirmado", "checkoutEnviado"))
            if (!estado.path(campo).isBoolean() || estado.path(campo).asBoolean()) return false;
        return true;
    }
    private ObjectNode retomarSeSemConfirmacao(ObjectNode estado, SessaoChatResponse sessao) {
        boolean preparoInterrompido = !estado.path("somenteAcompanhamento").asBoolean() && preparoSemEnvioFinanceiro(estado);
        if ((!estado.path("somenteAcompanhamento").asBoolean() && !estado.path("envioFinalRegistrado").asBoolean() && !preparoInterrompido)
                || !Set.of("CONFERENCIA", "NAO_CONCLUIDA").contains(estado.path("status").asText())
                || estado.path("confirmadaAzul").asBoolean() || !estado.path("consultaAtualizada").asBoolean()) return estado;
        SessaoTemporaria substituta = sessoes.get(estado.path("substituidaPor").asText());
        if (substituta != null) return visualizar(substituta.registro);
        JsonNode fornecedor = estado.path("consultaFornecedor").path("reserva");
        if (preparoInterrompido && !confirmacaoCorrespondeAConsulta(estado.path("original"), fornecedor)) return estado;
        if (!"Confirmed".equals(fornecedor.path("situacao").asText()) || !fornecedor.path("saldoPendente").isNumber()
                || fornecedor.path("saldoPendente").decimalValue().signum() != 0) return estado;
        int reservaId = estado.path("reservaId").asInt();
        JsonNode reserva = validarReserva(reservaId, sessao.getAgencia().getCodgAgencia());
        // Reinicia apenas a simulacao; o bloqueio de um resultado financeiro incerto continua na confirmacao final.
        ObjectNode nova = iniciarSimulacao(sessao, reservaId, estado.path("localizador").asText(), reserva, estado);
        SessaoTemporaria anterior = sessoes.get(estado.path("id").asText());
        if (anterior != null) {
            ObjectNode contextoAnterior = dados(anterior.registro); contextoAnterior.put("substituidaPor", nova.path("id").asText());
            registrarAcompanhamento(anterior.registro, contextoAnterior, anterior.registro.getStatus());
        }
        return nova;
    }

    private ObjectNode conferirAcompanhamento(ReemissaoAzulRegistro r) {
        ObjectNode dados = dados(r); dados.put("confirmadaAzul", false);
        JsonNode consulta = atualizarConsultaFornecedor(r, dados);
        if (consulta == null) {
            dados.put("mensagem", dados.path("erroConsultaFornecedor").asText());
            return visualizar(registrarAcompanhamento(r, dados, "CONFERENCIA"));
        }
        JsonNode resultado = null;
        if (dados.hasNonNull("operacaoAnteriorId")) {
            ObjectNode pedido = mapper.createObjectNode().put("localizador", r.getLocalizador()).put("operacaoId", dados.path("operacaoAnteriorId").asText());
            try { resultado = hub.executar("consultar-resultado", pedido); }
            catch (RuntimeException ex) { /* A reserva consultada continua visivel, sem atribuir sucesso a tentativa. */ }
        }
        if (resultado != null && "CONCLUIDA".equals(resultado.path("status").asText())
                && "Azul".equals(resultado.path("fonteConfirmacao").asText())
                && dados.path("operacaoAnteriorId").asText().equals(resultado.path("operacaoId").asText())
                && resultado.path("valorPago").isNumber() && resultado.path("valorPago").decimalValue().signum() >= 0
                && confirmacaoCorrespondeAConsulta(resultado.path("original"), consulta.path("reserva"))) {
            dados.put("confirmadaAzul", true); dados.set("confirmacao", resultado);
            dados.put("mensagem", "A Azul confirmou a reemissao desta operacao e o itinerario foi conferido na consulta atual. A atualizacao dos registros locais esta pendente de conferencia.");
            return visualizar(registrarAcompanhamento(r, dados, "CONFIRMADA_FORNECEDOR"));
        }
        if (dados.hasNonNull("operacaoAnteriorId") && semEfetivacaoConfirmada(resultado)) {
            dados.set("falhaConclusao", resultado);
            ReemissaoAzulRegistro encerramento = mapper.convertValue(r, ReemissaoAzulRegistro.class);
            encerramento.setId(dados.path("operacaoAnteriorId").asText()); encerramento.setStatus("CONFERENCIA"); encerramento.setDadosJson(dados.toString());
            try {
                JsonNode encerrada = manager.post(BASE + "/" + r.getReservaId() + "/encerrar-sem-efetivacao", encerramento, JsonNode.class);
                if (encerrada != null && encerrada.path("encerrada").asBoolean()) {
                    dados.put("somenteAcompanhamento", false).put("mensagem", mensagem(resultado));
                    return visualizar(registrarAcompanhamento(r, dados, "NAO_CONCLUIDA"));
                }
            } catch (RuntimeException ex) { /* O bloqueio permanece ate o encerramento ser registrado. */ }
        }
        dados.put("mensagem", "A Azul retornou a reserva atual, mas nao foi possivel comprovar a reemissao da tentativa anterior. Confira o itinerario consultado abaixo.");
        return visualizar(registrarAcompanhamento(r, dados, "CONFERENCIA"));
    }

    private JsonNode atualizarConsultaFornecedor(ReemissaoAzulRegistro r, ObjectNode dados) {
        dados.put("consultaAtualizada", false); dados.remove("erroConsultaFornecedor");
        JsonNode consulta;
        try { consulta = hub.executar("conferir", mapper.createObjectNode().put("localizador", r.getLocalizador())); }
        catch (RuntimeException ex) { consulta = null; }
        if (consulta == null || !"CONFERIDA".equals(consulta.path("status").asText()) || !"Azul".equals(consulta.path("fonte").asText())
                || !r.getLocalizador().equals(consulta.path("reserva").path("localizador").asText())
                || !consulta.path("reserva").path("trechos").isArray() || !consulta.path("reserva").path("passageiros").isArray()
                || !consulta.path("reserva").path("valor").isNumber() || !consulta.path("reserva").path("saldoPendente").isNumber()) {
            dados.put("erroConsultaFornecedor", "Nao foi possivel atualizar a consulta na Azul. A conclusao da reemissao permanece sem comprovacao."); return null;
        }
        dados.set("consultaFornecedor", consulta); dados.put("consultaAtualizada", true);
        if (dados.path("somenteAcompanhamento").asBoolean()) dados.putObject("original").put("moeda", consulta.path("reserva").path("moeda").asText("BRL"));
        return consulta;
    }

    private boolean confirmacaoCorrespondeAConsulta(JsonNode confirmada, JsonNode atual) {
        if (!"Confirmed".equals(atual.path("situacao").asText()) || !atual.path("saldoPendente").isNumber()
                || atual.path("saldoPendente").decimalValue().signum() != 0
                || !confirmada.path("localizador").equals(atual.path("localizador")) || !confirmada.path("moeda").equals(atual.path("moeda"))
                || !confirmada.path("valor").isNumber() || !atual.path("valor").isNumber()
                || confirmada.path("valor").decimalValue().compareTo(atual.path("valor").decimalValue()) != 0) return false;
        if (!confirmada.path("trechos").isArray() || confirmada.path("trechos").isEmpty()
                || confirmada.path("trechos").size() != atual.path("trechos").size()) return false;
        Map<String, JsonNode> trechos = new HashMap<>();
        for (JsonNode t : atual.path("trechos")) if (trechos.put(t.path("chave").asText(), t) != null) return false;
        for (JsonNode t : confirmada.path("trechos")) {
            JsonNode a = trechos.remove(t.path("chave").asText()); if (a == null) return false;
            for (var campo : Map.of("origem", "origin", "destino", "destination", "partida", "departure", "chegada", "arrival", "familia", "familia").entrySet())
                if (!t.path(campo.getKey()).equals(a.path(campo.getValue()))) return false;
        }
        if (!confirmada.path("passageiros").isArray() || confirmada.path("passageiros").isEmpty()
                || confirmada.path("passageiros").size() != atual.path("passageiros").size()) return false;
        Map<String, JsonNode> passageiros = new HashMap<>();
        for (JsonNode p : atual.path("passageiros")) if (passageiros.put(p.path("chave").asText(), p) != null) return false;
        for (JsonNode p : confirmada.path("passageiros")) {
            JsonNode a = passageiros.remove(p.path("chave").asText()); if (a == null) return false;
            for (String campo : List.of("tipo", "bebeAssociado")) if (!p.path(campo).equals(a.path(campo))) return false;
        }
        return true;
    }

    private ReemissaoAzulRegistro registrarAcompanhamento(ReemissaoAzulRegistro r, ObjectNode dados, String status) {
        r.setStatus(status); r.setDadosJson(dados.toString()); r.setVersao(r.getVersao() == null ? 0 : r.getVersao() + 1);
        r.setAtualizadoEm(LocalDateTime.now());
        sessoes.compute(r.getId(), (id, atual) -> { if (atual == null) return new SessaoTemporaria(r); atual.registro = r; return atual; });
        return r;
    }
    private JsonNode validarReserva(int id, int agencia) {
        JsonNode reserva = manager.get("reservaAereo/" + id, JsonNode.class);
        exigir(reserva != null && reserva.path("codgAgencia").path("codgAgencia").asInt(-1) == agencia, 403, "Reserva nao pertence a agencia atual.");
        exigir(reserva.path("status").asInt() == 3 && ("AZUL".equalsIgnoreCase(reserva.path("codgSistema").path("nomeSistema").asText()) || "AZUL".equalsIgnoreCase(reserva.path("codgSistema").path("siglaSistema").asText())), 409, "Reemissao disponivel somente para reservas emitidas pelo fornecedor Azul.");
        return reserva;
    }
    private BigDecimal totalVenda(JsonNode reserva) {
        JsonNode passageiros = reserva.path("passageiros");
        exigir(passageiros.isArray() && !passageiros.isEmpty(), 409, "Reserva sem valores de venda cadastrados por passageiro.");
        BigDecimal total = BigDecimal.ZERO;
        for (JsonNode passageiro : passageiros) {
            JsonNode valores = passageiro.path("reservaValores");
            exigir(valores.isArray() && valores.size() == 1 && valores.get(0).isObject(), 409, "Cadastro de valores por passageiro incompleto ou ambiguo.");
            for (String componente : List.of("valorTarifa", "valorTaxaEmbarque", "valorDu", "valorRc", "valorRav", "valorTaxaCombustivel", "valorAssento")) {
                JsonNode v = valores.get(0).path(componente);
                boolean obrigatorio = Set.of("valorTarifa", "valorTaxaEmbarque", "valorDu").contains(componente);
                exigir(!obrigatorio && (v.isMissingNode() || v.isNull()) || v.isNumber() && v.decimalValue().signum() >= 0, 409, "Componente de venda ausente ou invalido: " + componente);
                if (v.isNumber()) total = total.add(v.decimalValue());
            }
        }
        return total;
    }
    private ObjectNode consultarRegra(JsonNode trecho, JsonNode reserva) {
        RegraAereaAlteracaoConsultaRequest rq = new RegraAereaAlteracaoConsultaRequest(); rq.setCompanhia("AD");
        Set<String> nacionais = aeroportos.findIatasAeroportosNacionais();
        exigir(nacionais != null && !nacionais.isEmpty(), 503, "Nao foi possivel validar o mercado da reserva. Tente novamente.");
        String mercado = nacionais.contains(trecho.path("origem").asText()) && nacionais.contains(trecho.path("destino").asText()) ? "NACIONAL" : "INTERNACIONAL";
        rq.setMercado(mercado); rq.setFamiliaTarifaria(trecho.path("familia").asText()); rq.setCodigoTarifario(trecho.path("baseTarifaria").asText(trecho.path("familia").asText())); rq.setClasseReserva(trecho.path("classe").asText(null)); rq.setTipoEvento("REMARCACAO"); rq.setMomento("ANTES_EMBARQUE"); rq.setExigirRegraAprovada(true); rq.setValidadeMaximaDias(90);
        if (reserva.hasNonNull("dataEmissao")) rq.setDataEmissaoReserva(mapper.convertValue(reserva.get("dataEmissao"), Date.class));
        var regra = regras.consultar(rq);
        if (regra != null && Set.of("NAO_ENCONTRADO", "REGRA_NAO_HOMOLOGADA").contains(Objects.toString(regra.getStatus(), ""))) {
            // Continuidade temporaria enquanto o link da regra nao foi disponibilizado.
            // Nao representa uma regra aprovada nem determina multas ou isencoes.
            ObjectNode politica = mapper.createObjectNode().put("status", "PENDENTE_LINK").put("pendenteLink", true)
                    .put("mercado", mercado).put("trechoIndice", trecho.path("indice").asInt())
                    .put("mensagem", "Multas e diferencas tarifarias serao confirmadas pela Azul no comparativo, antes da confirmacao final.");
            politica.putObject("regra").put("tituloUsuario", "Link da regra pendente")
                    .put("descricaoUsuario", "O link com a regra de reemissao sera disponibilizado posteriormente. Por enquanto, voce pode continuar a simulacao.");
            return politica;
        }
        exigir(regra != null && regra.getStatus() != null && regra.getStatus().startsWith("PERMITIDA") && regra.getRegra() != null && Boolean.TRUE.equals(regra.getRegra().getPermiteAlteracao()), 409, regra == null ? "Politica de reemissao indisponivel." : regra.getMensagem());
        ObjectNode politica = mapper.valueToTree(regra); politica.put("mercado", mercado); politica.put("trechoIndice", trecho.path("indice").asInt()); return politica;
    }
    private boolean faturado(SessaoChatResponse sessao, BigDecimal valor) {
        try {
            String erp = sessao.getAgencia().getCodgSistemaBackoffice(); if (erp == null || erp.isBlank()) return false;
            var rq = new LimiteCreditoRQ(erp); var status = limites.checkLimiteApi(rq);
            if (status == null || (status.getStatusCode() != 0 && status.getStatusCode() != 200)) return false;
            var disponibilidade = limites.consultaLimiteApi(rq);
            if (disponibilidade == null || !Boolean.TRUE.equals(disponibilidade.getConsultaConfirmada()) || disponibilidade.getLimiteCredito() == null) return false;
            return disponibilidade.getLimiteCredito().stream().filter(Objects::nonNull).filter(l -> TipoLimite.FATURADO.equals(l.getTipoLimite())).anyMatch(l -> dinheiro(l.getTotalDisponivel()).compareTo(valor) >= 0);
        } catch (RuntimeException ex) { return false; }
    }
    private static BigDecimal dinheiro(String texto) { if (texto == null) return BigDecimal.ZERO; String n = texto.replace("R$", "").replace(" ", ""); if (n.contains(",")) n = n.replace(".", "").replace(',', '.'); return new BigDecimal(n); }
    private SessaoChatResponse sessao(ObjectNode pedido) { var s = usuarios.montarSessao(inteiro(pedido, "codgUsuario"), inteiro(pedido, "codgAgenciaSessao")); exigir(s != null && s.getUsuario() != null && s.getAgencia() != null && s.getUnidade() != null, 403, "Sessao de usuario invalida."); return s; }
    private ObjectNode montarPedido(ObjectNode dados, String loc) {
        ObjectNode pedido = mapper.createObjectNode().put("sistema", "Azul").put("localizador", loc).put("moeda", dados.path("original").path("moeda").asText());
        ArrayNode remover = pedido.putArray("trechosRemover"), novos = pedido.putArray("novosTrechos");
        for (JsonNode escolha : dados.path("escolhas")) {
            String chave = escolha.path("voo").path("identificacaoDaViagem").asText();
            exigir(!vooJaReservado(dados, chave), 409, "O voo selecionado e o mesmo da reserva. Escolha outro voo ou outra data.");
            for (JsonNode anterior : novos) exigir(!chave.equals(anterior.path("journeyKey").asText()), 409, "O mesmo voo foi selecionado mais de uma vez.");
            remover.add(escolha.path("original").path("chave").asText());
            novos.addObject().put("journeyKey", escolha.path("voo").path("identificacaoDaViagem").asText()).put("fareKey", escolha.path("familia").path("identificacaoDeVoo").asText()).put("origem", escolha.path("original").path("origem").asText()).put("destino", escolha.path("original").path("destino").asText()).put("data", escolha.path("data").asText());
        }
        exigir(remover.size() == dados.path("selecionados").size(), 409, "Selecione todos os novos voos antes de continuar."); return pedido;
    }
    private void validarCronologia(ObjectNode dados, LocalDate data, JsonNode escolha) {
        int indice = trechoAtual(dados).path("indice").asInt();
        JsonNode ida = null, volta = null;
        for (JsonNode e : dados.path("escolhas")) if (e.path("original").path("indice").asInt() == 0) ida = e;
        if (indice == 1 && ida != null) exigir(!data.isBefore(LocalDate.parse(ida.path("data").asText())), 400, "A volta deve ocorrer apos a ida.");
        JsonNode original = dados.path("original").path("trechos");
        if (indice == 0 && original.size() > 1 && dados.path("selecionados").size() == 1) exigir(!data.isAfter(LocalDateTime.parse(original.get(1).path("partida").asText()).toLocalDate()), 400, "A nova ida deve ocorrer antes da volta preservada.");
        if (indice == 1 && dados.path("selecionados").size() == 1) exigir(!data.isBefore(LocalDateTime.parse(original.get(0).path("chegada").asText()).toLocalDate()), 400, "A nova volta deve ocorrer depois da ida preservada.");
        if (escolha != null) {
            Map<Integer, JsonNode> escolhidos = new HashMap<>();
            dados.path("escolhas").forEach(e -> escolhidos.put(e.path("original").path("indice").asInt(), e)); escolhidos.put(indice, escolha);
            if (original.size() == 2) {
                JsonNode novaIda = escolhidos.get(0), novaVolta = escolhidos.get(1);
                LocalDateTime chegadaIda = novaIda == null ? LocalDateTime.parse(original.get(0).path("chegada").asText()) : dataVoo(novaIda.path("voo").path("voos").get(novaIda.path("voo").path("voos").size() - 1), false);
                LocalDateTime partidaVolta = novaVolta == null ? LocalDateTime.parse(original.get(1).path("partida").asText()) : dataVoo(novaVolta.path("voo").path("voos").get(0), true);
                // Na troca conjunta, a volta original sera substituida; a validacao completa ocorre ao seleciona-la.
                boolean voltaPendente = dados.path("selecionados").size() == 2 && novaVolta == null;
                exigir(voltaPendente || !partidaVolta.isBefore(chegadaIda), 400, "A volta nao pode partir antes da chegada da ida, inclusive no mesmo dia.");
            }
        }
    }
    private boolean vooJaReservado(ObjectNode dados, String chave) {
        for (JsonNode trecho : dados.path("original").path("trechos")) if (chave.equals(trecho.path("chave").asText())) return true;
        return false;
    }
    private void validarNovoVoo(ObjectNode dados, String chave) {
        exigir(!vooJaReservado(dados, chave), 409, "O voo selecionado e o mesmo da reserva. Escolha outro voo ou outra data.");
        for (JsonNode escolha : dados.path("escolhas")) exigir(!chave.equals(escolha.path("voo").path("identificacaoDaViagem").asText()),
                409, "O mesmo voo foi selecionado mais de uma vez. Confira os trechos escolhidos.");
    }
    private boolean rotaCompativel(JsonNode original, JsonNode opcao) {
        String origem = original.path("origem").asText(), destino = original.path("destino").asText();
        if (!origem.equals(opcao.path("origem").path("codigoIata").asText())
                || !destino.equals(opcao.path("destino").path("codigoIata").asText())) return false;
        JsonNode voos = opcao.path("voos");
        if (!voos.isArray() || voos.isEmpty() || !origem.equals(voos.get(0).path("origem").path("codigoIata").asText())
                || !destino.equals(voos.get(voos.size() - 1).path("destino").path("codigoIata").asText())) return false;
        for (int i = 1; i < voos.size(); i++) {
            String conexao = voos.get(i - 1).path("destino").path("codigoIata").asText();
            if (conexao.isBlank() || !conexao.equals(voos.get(i).path("origem").path("codigoIata").asText())) return false;
        }
        return true;
    }
    LocalDateTime dataVoo(JsonNode voo, boolean partida) {
        exigir(voo != null, 409, "Opcao sem os horarios do voo.");
        JsonNode valor = voo.path(partida ? "dataPartida" : "dataChegada");
        String hora = voo.path(partida ? "horaPartida" : "horaChegada").asText("").trim();
        try {
            LocalDateTime dt;
            if (valor.isIntegralNumber()) {
                dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(valor.longValue()), ZoneId.of("America/Sao_Paulo"));
            } else {
                String texto = valor.asText("").trim();
                if (texto.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
                    // Uma data sem horario so e valida quando o fornecedor informa a hora separadamente.
                    dt = LocalDate.parse(texto).atTime(LocalTime.parse(hora));
                } else {
                    String comOffset = texto.replaceFirst("([+-][0-9]{2})([0-9]{2})$", "$1:$2");
                    try {
                        dt = OffsetDateTime.parse(comOffset).atZoneSameInstant(ZoneId.of("America/Sao_Paulo")).toLocalDateTime();
                    } catch (java.time.format.DateTimeParseException semFuso) {
                        dt = LocalDateTime.parse(texto);
                    }
                }
            }
            if (!hora.isBlank()) dt = LocalDateTime.of(dt.toLocalDate(), LocalTime.parse(hora));
            return dt;
        } catch (RuntimeException ex) { throw erro(409, "Horario do voo indisponivel. Refaca a pesquisa."); }
    }
    private ReemissaoAzulRegistro salvar(ReemissaoAzulRegistro r, ObjectNode dados, String status) {
        ((ArrayNode) dados.withArray("eventos")).addObject().put("status", status).put("em", Instant.now().toString());
        ObjectNode atividade = mapper.createObjectNode().put("id", r.getId()).put("reservaId", r.getReservaId())
                .put("codgUsuario", r.getCodgUsuario()).put("codgAgencia", r.getCodgAgencia()).put("status", status)
                .put("descricao", descricaoAtividade(status, dados));
        JsonNode gravada = manager.post(BASE + "/" + r.getReservaId() + "/atividades", atividade, JsonNode.class);
        exigir(gravada != null && gravada.path("registrada").asBoolean(), 503, "Nao foi possivel registrar a atividade no historico da reserva.");
        r.setStatus(status); r.setDadosJson(dados.toString());
        r.setVersao(r.getVersao() == null ? 0 : r.getVersao() + 1);
        r.setAtualizadoEm(LocalDateTime.now());
        sessoes.compute(r.getId(), (id, atual) -> {
            if (atual == null) return new SessaoTemporaria(r);
            atual.registro = r; return atual;
        });
        return r;
    }
    private String descricaoAtividade(String status, ObjectNode dados) {
        if (dados.path("falhaRecuperavel").asBoolean()) {
            String motivo = dados.path("mensagem").asText();
            return "Validacao recusada antes de efetivar a reemissao: " + (motivo.length() > 750 ? motivo.substring(0, 750) : motivo);
        }
        return switch (status) {
            case "PASSAGEIROS" -> "Selecao dos passageiros para reemissao apresentada.";
            case "DIVISAO_CONFIRMACAO" -> "Separacao dos passageiros e valores locais conferidos; aguardando aceite da divisao.";
            case "DIVISAO_PENDENTE" -> "Divisao autorizada; resultado acompanhado sem repeticao automatica.";
            case "POLITICA" -> dados.path("reiniciadaSemConfirmacao").asBoolean()
                    ? "Simulacao reiniciada apos consulta na Azul sem confirmacao da tentativa anterior. Novo aceite e nova aprovacao de valores obrigatorios."
                    : politicaPendente(dados)
                    ? "Simulacao aberta; link da regra pendente e continuidade disponivel."
                    : "Simulacao de alteracao aberta; politica apresentada.";
            case "TRECHOS" -> politicaPendente(dados)
                    ? "Usuario ciente de que o link da regra sera disponibilizado; continuidade da simulacao autorizada."
                    : "Politica de alteracao aceita pelo usuario.";
            case "PESQUISA" -> "Trechos selecionados para pesquisa de novos voos.";
            case "RESULTADOS" -> "Pesquisa de voos realizada para " + dados.path("dataPesquisa").asText() + ".";
            case "SELECAO" -> "Novos voos selecionados: " + resumoEscolhas(dados);
            case "SERVICOS_OPCAO", "PREPARANDO", "SERVICOS" -> "Preparacao da cotacao e escolha de servicos adicionais.";
            case "CALCULANDO", "COMPARATIVO" -> "Comparativo calculado. Adicional: BRL " + dados.path("cotacao").path("valorAdicional").asText("a confirmar") + ".";
            case "PAGAMENTO" -> "Comparativo aprovado pelo usuario. Adicional: BRL " + dados.path("valorAprovado").asText() + ".";
            case "PROCESSANDO" -> "Confirmacao final em processamento na Azul.";
            case "SINCRONIZANDO" -> "Azul confirmou a alteracao; atualizacao da reserva em andamento.";
            case "CONCLUIDA" -> "Atualizacao da reserva concluida apos confirmacao da Azul.";
            case "NAO_CONCLUIDA" -> "Reemissao nao efetivada. Nenhum pagamento ou checkout enviado; reserva original conferida na Azul. " + dados.path("mensagem").asText();
            case "CONFERENCIA" -> "Operacao pendente de conferencia. " + dados.path("mensagem").asText("Verifique a reserva antes de qualquer nova tentativa.");
            case "CANCELADA" -> "Simulacao encerrada pelo usuario.";
            case "EXPIRADA" -> "Simulacao expirada.";
            default -> "Etapa de alteracao atualizada.";
        };
    }
    private String resumoEscolhas(ObjectNode dados) {
        List<String> escolhas = new ArrayList<>();
        for (JsonNode escolha : dados.path("escolhas")) {
            JsonNode original = escolha.path("original");
            List<String> voos = new ArrayList<>();
            escolha.path("voo").path("voos").forEach(v -> voos.add(v.path("numeroVoo").asText()));
            escolhas.add(original.path("origem").asText() + "-" + original.path("destino").asText()
                    + " em " + escolha.path("data").asText() + ", voos " + String.join("/", voos)
                    + ", familia " + escolha.path("familia").path("familia").path("descricaoFamilia").asText());
        }
        String resumo = String.join("; ", escolhas);
        return resumo.length() > 800 ? resumo.substring(0, 800) + "..." : resumo;
    }
    private boolean politicaPendente(ObjectNode dados) {
        for (JsonNode politica : dados.path("politicas")) if (politica.path("pendenteLink").asBoolean()) return true;
        return false;
    }
    private void registrarFalhaAzul(ObjectNode dados, JsonNode resposta) {
        if (resposta == null) return;
        for (String campo : List.of("etapaAzul", "codigoFalha", "httpStatusAzul", "pagamentoEnviado", "pagamentoConfirmado", "checkoutEnviado")) {
            JsonNode valor = resposta.path(campo);
            if (valor.isBoolean() || valor.isIntegralNumber() || (valor.isTextual() && valor.asText().matches("[A-Z_]{1,50}"))) dados.set(campo, valor);
        }
    }
    private boolean semEfetivacaoConfirmada(JsonNode resposta) {
        return resposta != null && "ERRO".equals(resposta.path("status").asText())
                && resposta.path("semEfetivacaoConfirmada").asBoolean() && resposta.path("reservaOriginalConferida").asBoolean()
                && resposta.path("requerConferencia").isBoolean() && !resposta.path("requerConferencia").asBoolean()
                && resposta.path("pagamentoEnviado").isBoolean() && !resposta.path("pagamentoEnviado").asBoolean()
                && resposta.path("pagamentoConfirmado").isBoolean() && !resposta.path("pagamentoConfirmado").asBoolean()
                && resposta.path("checkoutEnviado").isBoolean() && !resposta.path("checkoutEnviado").asBoolean();
    }
    private ObjectNode visualizar(ReemissaoAzulRegistro r) { ObjectNode v = dados(r).deepCopy(); v.put("id", r.getId()).put("versao", r.getVersao()).put("status", r.getStatus()).put("localizador", r.getLocalizador()).put("reservaId", r.getReservaId()).put("expiraEm", r.getExpiraEm().toString()); return v; }
    private ObjectNode dados(ReemissaoAzulRegistro r) { try { return (ObjectNode) mapper.readTree(r.getDadosJson()); } catch (Exception ex) { throw erro(500, "Registro da solicitacao indisponivel."); } }
    private ObjectNode pedidoHub(ReemissaoAzulRegistro r) { return mapper.createObjectNode().put("localizador", r.getLocalizador()).put("operacaoId", r.getId()); }
    private JsonNode trechoAtual(ObjectNode d) { return d.path("original").path("trechos").get(d.path("selecionados").get(d.path("ordem").asInt()).intValue()); }
    private JsonNode item(JsonNode lista, int indice) { exigir(lista.isArray() && indice >= 0 && indice < lista.size(), 400, "Selecao invalida ou expirada."); return lista.get(indice); }
    private static int inteiro(JsonNode n, String key) { exigir(n.path(key).isIntegralNumber() && n.path(key).canConvertToInt(), 400, "Campo obrigatorio: " + key); return n.path(key).intValue(); }
    private static void etapa(ReemissaoAzulRegistro r, String... status) { exigir(Arrays.asList(status).contains(r.getStatus()), 409, "Etapa indisponivel. Atualize o andamento da solicitacao."); }
    private static String mensagem(JsonNode n) { return n == null ? "Fornecedor indisponivel. Consulte o andamento da solicitacao." : n.path("mensagem").asText("Nao foi possivel validar a operacao com a Azul."); }
    private static void exigir(boolean condicao, int status, String mensagem) { if (!condicao) throw erro(status, mensagem); }
    private static RegraDeNegocioException erro(int status, String mensagem) { return new RegraDeNegocioException(status, mensagem); }
}
