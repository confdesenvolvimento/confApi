package com.confApi.rodoviario;

import com.confApi.db.confManager.rodoviario.RodoviarioManagerClient;
import com.confApi.db.confManager.rodoviario.dto.*;
import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.hub.rodoviario.RodoviarioHubClient;
import com.confApi.recebimento.RecebimentoService;
import com.confApi.rodoviario.dto.RodoviarioCompraModel;
import com.confApi.rodoviario.dto.RodoviarioConsultaRequest;
import com.confApi.rodoviario.dto.RodoviarioReservaOperacaoResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

import static com.confApi.hub.rodoviario.RodoviarioHubClient.Operacao.*;

/**
 * Orquestracao equivalente a carro: fornecedor, consulta completa, Manager e detalhe salvo.
 */
@Service
@RequiredArgsConstructor
public class RodoviarioReservaService {
    private static final Logger log = LoggerFactory.getLogger(RodoviarioReservaService.class);
    private final RodoviarioHubClient hub;
    private final RodoviarioManagerClient manager;
    private final RodoviarioReservaMapper mapper;
    private final ObjectMapper json;
    private final RecebimentoService recebimentoService;
    private final RodoviarioHttpClient http;
    private final Object[] locks = java.util.stream.IntStream.range(0, 64).mapToObj(i -> new Object()).toArray();

    public RodoviarioReservaOperacaoResponse reservar(RodoviarioCompraModel req, String usuario) {
        validar(req, usuario, true);
        synchronized (lock(req)) {
            return comprar(req, usuario, true, false);
        }
    }

    public RodoviarioReservaOperacaoResponse emitir(RodoviarioCompraModel req, String usuario) {
        validar(req, usuario, false);
        synchronized (lock(req)) {
            return comprar(req, usuario, false, false);
        }
    }

    public RodoviarioReservaOperacaoResponse reconciliar(RodoviarioCompraModel req, String usuario) {
        boolean reserva = req != null && req.getReservaRodoviario() != null;
        validar(req, usuario, reserva);
        synchronized (lock(req)) {
            return comprar(req, usuario, reserva, true);
        }
    }

    private Object lock(RodoviarioCompraModel req) {
        return locks[Math.floorMod(req.getDadosManager().getChaveIdempotencia().hashCode(), locks.length)];
    }

    private void validar(RodoviarioCompraModel req, String usuario, boolean reservar) {
        exigir(req != null && req.getEmissao() != null && req.getEmissao().isObject(), 400, "Informe emissao conforme o checkout rodoviario do Hub.");
        exigir(!req.getEmissao().path("email").asText().isBlank() && req.getEmissao().path("pagamento").isObject(), 400, "Informe email e pagamento da emissao.");
        exigir(!reservar || (req.getReservaRodoviario() != null && req.getReservaRodoviario().isObject()), 400, "Informe reservaRodoviario conforme o booking do Hub.");
        exigir(reservar || !req.getEmissao().path("codigoReserva").asText().isBlank(), 400, "Informe codigoReserva para emitir o booking existente.");
        String sistema = req.getEmissao().path("sistema").asText("QueroPassagem");
        exigir(!reservar || sistema.equalsIgnoreCase(req.getReservaRodoviario().path("sistema").asText("QueroPassagem")), 400, "Booking e emissao devem usar o mesmo sistema.");
        mapper.validar(req.getDadosManager());

        exigir(req.getRecebimento() != null, 400, "Informe o recebimento rodoviário.");
        var recebimento = req.getRecebimento();
        Integer forma = recebimento.getCodgFormaPagamento();
        exigir((Integer.valueOf(1).equals(forma) || Integer.valueOf(2).equals(forma))
               && recebimento.getValorPagamento() != null
               && Double.isFinite(recebimento.getValorPagamento())
               && recebimento.getValorPagamento() > 0
               && BigDecimal.valueOf(recebimento.getValorPagamento())
                          .stripTrailingZeros().scale() <= 2
               && BigDecimal.valueOf(recebimento.getValorPagamento())
                          .compareTo(new BigDecimal("9999999999999.99")) <= 0
               && recebimento.getFormaDePagamento() != null
               && Objects.equals(recebimento.getFormaDePagamento().getCodgFormaPagto(), forma),
                400, "Informe forma de pagamento e valor válido para o recebimento rodoviário.");
        exigir("preApproved".equalsIgnoreCase(req.getEmissao().path("pagamento").path("metodo").asText())
               && req.getEmissao().path("pagamento").path("parcelas").asInt() == 1,
                400, "O checkout rodoviário deve usar preApproved; o cartão é processado pela Rede.");
        exigir(req.getRecebimento().getCodgRecebimento() == null,
                400, "Não envie recebimento já processado para uma nova compra.");

        // Ainda nao ha markup. Os valores da pesquisa sao uma expectativa
        // de NET, substituida pelo total do booking antes de criar o recebimento.
        BigDecimal totalEsperado = BigDecimal.valueOf(recebimento.getValorPagamento());
        var valores = req.getDadosManager().getValores();
        exigir(valores.getValorTotalNet() == null || valores.getValorTotalNet().compareTo(totalEsperado) == 0,
                400, "O total NET informado deve corresponder ao valor esperado para o recebimento.");
        exigir(valores.getValorTotalMkp() == null || valores.getValorTotalMkp().compareTo(totalEsperado) == 0,
                400, "Sem markup, o total MKP deve ser igual ao NET esperado para o recebimento.");
        valores.setValorTotalNet(totalEsperado.setScale(2));
        valores.setValorTotalMkp(totalEsperado.setScale(2));
    }

    private RodoviarioReservaOperacaoResponse comprar(RodoviarioCompraModel req, String usuario, boolean reservar, boolean somenteConsultar) {
        var resposta = new RodoviarioReservaOperacaoResponse();
        var dadosManager = req.getDadosManager();
        for (int indice = 0; indice < dadosManager.getTrechos().size(); indice++) {
            var trecho = dadosManager.getTrechos().get(indice).getTrecho();
            log.info("Rodoviario request dadosManager requestId={} codgSistema={} trecho={} codgCidadeOrigem={} codgCidadeDestino={}",
                    MDC.get(RodoviarioRequestLoggingFilter.REQUEST_ID), dadosManager.getReserva().getCodgSistema(),
                    indice + 1, trecho.getCodgCidadeOrigem(), trecho.getCodgCidadeDestino());
        }
        // Consulta a referência antes de cobrar; uma compra existente nunca
        // pode disparar nova cobrança por uma tentativa de sincronização.
        String referencia = referencia(req.getDadosManager());
        ObjectNode emissao = req.getEmissao().deepCopy();
        emissao.put("referenciaExterna", referencia);
        ObjectNode consulta = json.createObjectNode().put("sistema", emissao.path("sistema").asText("QueroPassagem"))
                .put("email", emissao.path("email").asText()).put("referenciaExterna", referencia);
        boolean efeito = false, solicitandoReserva = false, acessandoManager = false;
        Recebimento recebimentoCriado = null;
        try {
            JsonNode compra = recuperar(consulta);
            if (compra == null) {
                exigir(!somenteConsultar, 409, "Compra nao localizada por referencia. Confira o fornecedor antes de tentar emitir novamente.");
                JsonNode booking;
                if (reservar) {
                    efeito = true;
                    solicitandoReserva = true;
                    booking = hub.post(RodoviarioHubClient.Operacao.reservar, req.getReservaRodoviario());
                    String codigo = booking.path("dados").path("codigoFornecedor").asText();
                    exigir(codigo.matches("[A-Za-z0-9_-]+"), 502, "Booking retornado pelo Hub sem identificador valido.");
                    emissao.put("codigoReserva", codigo);
                    resposta.setCodigoReservaTemporaria(codigo);
                    solicitandoReserva = false;
                } else {
                    booking = operacaoBooking("consultarreserva", emissao, false);
                    exigir(emissao.path("codigoReserva").asText().equals(
                                    booking.path("dados").path("codigoFornecedor").asText()),
                            502, "Consulta do Hub retornou outro booking.");
                }
                BigDecimal totalBooking;
                try {
                    totalBooking = totalNetBooking(booking.path("dados"));
                } catch (IllegalArgumentException ex) {
                    return recusarAntesDoRecebimento(resposta, emissao, reservar,
                            "PRECO_NAO_CONFIRMADO", "O fornecedor nao confirmou o valor total da viagem. "
                                                    + "Nenhum recebimento ou emissao foi solicitado. Atualize a pesquisa.");
                }
                BigDecimal totalEsperado = BigDecimal.valueOf(req.getRecebimento().getValorPagamento());
                log.info("Rodoviario preco booking requestId={} totalPesquisa={} totalBookingNet={}",
                        MDC.get(RodoviarioRequestLoggingFilter.REQUEST_ID), totalEsperado, totalBooking);
                if (totalBooking.compareTo(totalEsperado) != 0) {
                    log.info("Rodoviario adotando preco do fornecedor requestId={} totalPesquisa={} totalAplicado={}",
                            MDC.get(RodoviarioRequestLoggingFilter.REQUEST_ID), totalEsperado, totalBooking);
                }
                // O total confirmado no booking passa a valer para a reserva
                // e para o recebimento, antes de solicitar a cobranca ao Manager.
                double totalRecebimento = totalBooking.doubleValue();
                req.getRecebimento().setValorPagamento(totalRecebimento);
                req.getRecebimento().setValorEntrada(totalRecebimento);
                if (req.getRecebimento().getCartaoSelecionado() != null) {
                    req.getRecebimento().getCartaoSelecionado().setValor(totalRecebimento);
                }
                req.getDadosManager().getValores().setValorTotalNet(totalBooking);
                req.getDadosManager().getValores().setValorTotalMkp(totalBooking);
                // Mesmo fluxo do carro: o Manager cobra pela Rede (ou registra
                // o faturado), salva o TID e devolve o ID do recebimento.
                // O valor ja foi atualizado pelo booking; ainda nao houve checkout.
                // Marcar efeito antes do POST: timeout pode ter ocorrido depois
                // de a Rede ter processado a transação.
                efeito = true;
                recebimentoCriado = recebimentoService.criarRecebimento(req);
                exigir(recebimentoCriado != null
                       && recebimentoCriado.getCodgRecebimento() != null
                       && Integer.valueOf(1).equals(recebimentoCriado.getStatus()),
                        502, "O recebimento não foi confirmado no Manager. Concilie antes de repetir.");
                exigir(recebimentoCriado.getValrRecebimento() != null
                       && Double.isFinite(recebimentoCriado.getValrRecebimento())
                       && BigDecimal.valueOf(recebimentoCriado.getValrRecebimento()).compareTo(
                                BigDecimal.valueOf(req.getRecebimento().getValorPagamento())) == 0,
                        502, "O valor confirmado no recebimento diverge do valor solicitado.");
                if (Integer.valueOf(2).equals(req.getRecebimento().getCodgFormaPagamento())) {
                    exigir(recebimentoCriado.getCodgTransacao() != null
                           && !recebimentoCriado.getCodgTransacao().isBlank(),
                            502, "A Rede não retornou um TID persistido para o recebimento.");
                } else {
                    exigir(recebimentoCriado.getCodgTransacao() == null,
                            502, "O recebimento faturado retornou um TID inesperado.");
                }
                req.getRecebimento().setCodgRecebimento(recebimentoCriado.getCodgRecebimento());
                efeito = true;
                var checkout = hub.post(RodoviarioHubClient.Operacao.emitir, emissao);
                resposta.setFornecedor(checkout);
                String codigo = mapper.codigoCompra(checkout.path("dados"));
                consulta.remove("referenciaExterna");
                consulta.put("codigoCompra", Long.parseLong(codigo));
                compra = hub.post(consultar, consulta);
            }
            resposta.setFornecedor(compra);
            var dados = compra.path("dados");
            String codigo = mapper.codigoCompra(dados);
            acessandoManager = true;
            var existente = porLocalizador(codigo, req.getDadosManager().getReserva().getCodgSistema(), usuario);
            if (existente == null) {
                if (recebimentoCriado == null) {
                    return parcial(resposta, "Compra localizada no fornecedor, mas não há recebimento identificado "
                                             + "para vinculá-la. Concilie a cobrança antes de gravar a reserva.");
                }
                var destinos = destinosPorCidade(req.getDadosManager().getReserva().getCodgSistema(), ids(req.getDadosManager()), usuario);
                var payload = mapper.mapearCidades(req.getDadosManager(), dados, destinos);
                BigDecimal totalFinal = payload.getValores().getValorTotalMkp();
                BigDecimal totalEsperado = req.getDadosManager().getValores().getValorTotalNet();
                BigDecimal totalRecebido = BigDecimal.valueOf(recebimentoCriado.getValrRecebimento());
                BigDecimal totalFornecedor = payload.getValores().getValorTotalNet();
                if (totalFinal == null || totalFornecedor == null
                    || totalFinal.compareTo(totalFornecedor) != 0
                    || totalFinal.compareTo(totalEsperado) != 0
                    || totalFinal.compareTo(totalRecebido) != 0) {
                    log.warn("Rodoviario divergencia de valores requestId={} codgRecebimento={} "
                             + "totalBookingNet={} totalRecebido={} totalFornecedorNet={} totalManagerMkp={}",
                            MDC.get(RodoviarioRequestLoggingFilter.REQUEST_ID),
                            recebimentoCriado.getCodgRecebimento(),
                            totalEsperado, totalRecebido,
                            totalFornecedor, totalFinal);
                    resposta.setCodigo("VALOR_RECEBIMENTO_DIVERGENTE");
                    return parcial(resposta, "O total confirmado pelo fornecedor difere do valor do booking ou do recebimento. "
                                             + "Consulte e concilie a compra antes de repetir.");
                }
                log.info("Rodoviario valores conciliados requestId={} codgRecebimento={} "
                         + "totalMkp={} totalRecebido={} totalFornecedorNet={}",
                        MDC.get(RodoviarioRequestLoggingFilter.REQUEST_ID),
                        recebimentoCriado.getCodgRecebimento(), totalFinal, totalRecebido, totalFornecedor);
                payload.setRecebimentoIds(List.of(recebimentoCriado.getCodgRecebimento()));
                efeito = true;
                var salvo = manager.salvarCompleta(payload, usuario);
                exigir(salvo != null && salvo.getReserva() != null && salvo.getReserva().getCodgReservaRodoviario() != null,
                        502, "Manager nao confirmou o identificador salvo.");
                resposta.setReserva(manager.detalhe(salvo.getReserva().getCodgReservaRodoviario(), usuario));
            } else {
                exigir(Objects.equals(existente.getReserva().getCodgAgencia(), req.getDadosManager().getReserva().getCodgAgencia()),
                        409, "Compra ja vinculada a outra agencia.");
                resposta.setReserva(existente);
            }
            sincronizar(resposta, usuario, "Cancelamento confirmado pelo fornecedor.");
            if (!temRecebimentoVinculado(resposta.getReserva())) {
                return parcial(resposta, "Compra registrada, mas o recebimento não consta como vinculado "
                                         + "no Manager. Concilie antes de confirmar ou enviar o comprovante.");
            }
            // A criacao foi concluida quando a compra identificada no fornecedor
            // foi gravada no Manager com o recebimento vinculado.
            // O status do fornecedor nao impede a abertura pelo localizador.
            resposta.setSuccess(true);
            resposta.setRequerConferencia(false);
            resposta.setCodigo(null);
            resposta.setMensagem("Reserva rodoviaria registrada no Manager.");
            return resposta;
        } catch (RuntimeException ex) {
            if (solicitandoReserva && ex instanceof RodoviarioIntegracaoException) {
                var erro = (RodoviarioIntegracaoException) ex;
                if (erro.getStatus() >= 400 && erro.getStatus() < 500
                    && !erro.getResposta().path("requerConferencia").asBoolean()) {
                    if (recebimentoCriado != null) {
                        Recebimento estorno = recebimentoService.cancelarRecebimento(recebimentoCriado);
                        if (estorno == null || !Integer.valueOf(0).equals(estorno.getStatus())) {
                            registrarErro(resposta, ex);
                            return parcial(resposta, "O fornecedor recusou o booking, mas o estorno da Rede "
                                                     + "não foi confirmado. Concilie o recebimento antes de repetir.");
                        }
                    }
                    registrarErro(resposta, ex);
                    resposta.setCodigo("BOOKING_RECUSADO");
                    resposta.setRequerConferencia(false);
                    resposta.setMensagem("Nao foi possivel realizar a reserva no Hub.");
                    return resposta;
                }
            }
            if (!efeito && resposta.getFornecedor() == null) throw ex;
            registrarErro(resposta, ex);
            if (acessandoManager && "AUTENTICACAO_MANAGER_REJEITADA".equals(resposta.getCodigo()))
                return parcial(resposta, "Compra retornada pelo Hub, mas o Manager recusou a autenticacao da aplicacao. Confira a integracao e recupere a compra sem repetir reserva ou emissao.");
            if (ex instanceof RodoviarioIntegracaoException && ("CONFERENCIA_NECESSARIA".equals(resposta.getCodigo())
                                                                || "VINCULOS_DESTINO_INVALIDOS".equals(resposta.getCodigo()))) {
                String motivo = ((RodoviarioIntegracaoException) ex).getDiagnosticoLocal();
                if (motivo != null)
                    return parcial(resposta, "Compra retornada pelo Hub, mas os dados para gravacao divergem. " + motivo
                                             + " Confira dadosManager no payload de recuperacao e sincronize sem nova reserva ou emissao.");
            }
            return parcial(resposta, acessandoManager
                    ? "Compra retornada pelo Hub, mas nao foi possivel concluir o registro no Manager. Consulte a compra antes de repetir."
                    : "Nao foi possivel confirmar a reserva/emissao no Hub. Consulte a compra antes de repetir.");
        }
    }

    private JsonNode operacaoBooking(String operacao, ObjectNode emissao, boolean efeito) {
        ObjectNode request = json.createObjectNode()
                .put("sistema", emissao.path("sistema").asText("QueroPassagem"))
                .put("codigoReserva", emissao.path("codigoReserva").asText());
        return http.trocar(RodoviarioHttpClient.Servico.HUB, "api/rodoviario/" + operacao,
                HttpMethod.POST, request, null, null, JsonNode.class, efeito);
    }

    private BigDecimal totalNetBooking(JsonNode booking) {
        JsonNode opcoes = booking.path("formasPagamento");
        if (!opcoes.isArray()) throw new IllegalArgumentException("Booking sem opcoes de pagamento.");
        BigDecimal total = null;
        for (JsonNode opcao : opcoes) {
            if (!"preApproved".equalsIgnoreCase(opcao.path("metodo").asText())) continue;
            JsonNode parcelas = opcao.path("parcelas");
            if (!parcelas.isNull() && !parcelas.isMissingNode()
                && (!parcelas.isIntegralNumber() || parcelas.asInt() != 1)) continue;
            JsonNode valor = opcao.path("total");
            if (!valor.isNumber()) throw new IllegalArgumentException("Booking sem total numerico.");
            BigDecimal candidato = valor.decimalValue();
            if (candidato.signum() <= 0 || candidato.stripTrailingZeros().scale() > 2
                || candidato.compareTo(new BigDecimal("9999999999999.99")) > 0)
                throw new IllegalArgumentException("Total invalido no booking.");
            if (total != null && total.compareTo(candidato) != 0)
                throw new IllegalArgumentException("Booking com totais ambiguos.");
            total = candidato.setScale(2);
        }
        if (total == null) throw new IllegalArgumentException("Booking sem opcao preApproved.");
        return total;
    }

    private RodoviarioReservaOperacaoResponse recusarAntesDoRecebimento(
            RodoviarioReservaOperacaoResponse resposta, ObjectNode emissao,
            boolean bookingCriado, String codigo, String mensagem) {
        if (bookingCriado) {
            try {
                JsonNode retorno = operacaoBooking("cancelarreserva", emissao, true);
                if (!retorno.path("dados").isBoolean() || !retorno.path("dados").asBoolean())
                    throw new IllegalStateException("Liberacao do booking nao confirmada.");
                resposta.setCodigoReservaTemporaria(null);
            } catch (RuntimeException ex) {
                registrarErro(resposta, ex);
                resposta.setCodigo("BOOKING_LIBERACAO_PENDENTE");
                return parcial(resposta, mensagem
                                         + " A liberacao das poltronas do booking temporario nao foi confirmada. Confira o booking antes de repetir.");
            }
        }
        resposta.setSuccess(false);
        resposta.setRequerConferencia(false);
        resposta.setCodigo(codigo);
        resposta.setMensagem(mensagem);
        return resposta;
    }

    private String moeda(BigDecimal valor) {
        return java.text.NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(valor);
    }

    private boolean temRecebimentoVinculado(ReservaRodoviarioDetalheDTO detalhe) {
        JsonNode lista = json.valueToTree(detalhe).path("recebimentos");
        if (!lista.isArray()) return false;
        for (JsonNode item : lista) {
            if (item.path("codgRecebimento").asInt(0) > 0) return true;
        }
        return false;
    }

    private boolean dataEmissaoRegistrada(ReservaRodoviarioDetalheDTO detalhe) {
        JsonNode data = json.valueToTree(detalhe.getReserva()).path("dataEmissao");
        return !data.isMissingNode() && !data.isNull() && !data.asText("").isBlank();
    }

    private JsonNode recuperar(ObjectNode consulta) {
        try {
            return hub.post(consultar, consulta);
        } catch (RodoviarioIntegracaoException ex) {
            if (ex.getStatus() == 404) return null;
            throw ex;
        }
    }

    private ReservaRodoviarioDetalheDTO porLocalizador(String localizador, Integer sistema, String usuario) {
        var filtros = new LinkedMultiValueMap<String, String>();
        filtros.add("codgSistema", sistema.toString());
        try {
            return manager.trocar("reservaRodoviario/findByLocalizador/" + localizador, HttpMethod.GET,
                    null, filtros, usuario, ReservaRodoviarioDetalheDTO.class, true);
        } catch (RodoviarioIntegracaoException ex) {
            if (ex.getStatus() == 404) return null;
            throw ex;
        }
    }

    public RodoviarioReservaOperacaoResponse consultarReserva(RodoviarioConsultaRequest req, String usuario) {
        var resposta = preparar(req, usuario);
        try {
            resposta.setFornecedor(hub.post(consultar, consultaSalva(req, resposta.getReserva())));
            conferirCompra(resposta);
            sincronizar(resposta, usuario, "Cancelamento confirmado na consulta do fornecedor.");
            resposta.setSuccess(true);
            resposta.setMensagem("Reserva consultada no Manager e no fornecedor.");
            return resposta;
        } catch (RuntimeException ex) {
            registrarErro(resposta, ex);
            return parcial(resposta, "Reserva carregada do Manager; nao foi possivel concluir a sincronizacao com o fornecedor.");
        }
    }

    public RodoviarioReservaOperacaoResponse cancelarReserva(RodoviarioConsultaRequest req, String usuario, boolean bilhete) {
        var resposta = preparar(req, usuario);
        exigir(req.getMotivo() != null && !req.getMotivo().isBlank() && req.getMotivo().length() <= 255, 400, "Informe motivo de cancelamento com ate 255 caracteres.");
        ObjectNode consulta = consultaSalva(req, resposta.getReserva());
        // Consultar primeiro garante que o codigo enviado pertence a reserva autorizada no Manager.
        resposta.setFornecedor(hub.post(consultar, consulta));
        conferirCompra(resposta);
        if (bilhete) {
            exigir(req.getConsulta().path("codigoPassageiro").isIntegralNumber(), 400, "Informe codigoPassageiro do fornecedor.");
            long codigo = req.getConsulta().path("codigoPassageiro").asLong();
            boolean existe = false;
            for (var t : resposta.getFornecedor().path("dados").path("trechos"))
                for (var p : t.path("passageiros")) existe |= p.path("codigoFornecedor").asLong(-1) == codigo;
            exigir(existe, 400, "Passageiro nao pertence a compra.");
        }
        try {
            if (mapper.status(resposta.getFornecedor().path("dados")) != 2) {
                ObjectNode cancelar = consulta.deepCopy();
                if (bilhete) cancelar.put("codigoPassageiro", req.getConsulta().path("codigoPassageiro").asLong());
                resposta.setFornecedor(hub.post(bilhete ? cancelarbilhete : cancelarcompra, cancelar));
                conferirCompra(resposta);
            }
            sincronizar(resposta, usuario, req.getMotivo());
            if (mapper.status(resposta.getFornecedor().path("dados")) != 2) {
                return parcial(resposta, "Cancelamento parcial ou pendente no fornecedor. "
                                         + "A reserva e o recebimento exigem conferencia.");
            }
            if (!Integer.valueOf(2).equals(resposta.getReserva().getReserva().getStatus())
                || !recebimentosCancelados(resposta.getReserva())) {
                return parcial(resposta, "O fornecedor confirmou o cancelamento, mas o Manager "
                                         + "não confirmou o cancelamento integral do recebimento.");
            }
            resposta.setSuccess(true);
            resposta.setMensagem("Cancelamento confirmado no fornecedor, na reserva e no recebimento.");
            return resposta;
        } catch (RuntimeException ex) {
            registrarErro(resposta, ex);
            return parcial(resposta, "Cancelamento nao confirmado. Consulte a compra antes de repetir.");
        }
    }

    private RodoviarioReservaOperacaoResponse preparar(RodoviarioConsultaRequest req, String usuario) {
        exigir(req != null && req.getCodgReservaRodoviario() != null && req.getCodgReservaRodoviario() > 0
               && req.getConsulta() != null && req.getConsulta().isObject(), 400, "Informe codgReservaRodoviario e consulta.");
        exigir(!req.getConsulta().path("email").asText().isBlank(), 400, "Informe email da compra.");
        var resposta = new RodoviarioReservaOperacaoResponse();
        resposta.setReserva(manager.detalhe(req.getCodgReservaRodoviario(), usuario));
        exigir(resposta.getReserva() != null && resposta.getReserva().getReserva() != null, 502, "Detalhe do Manager invalido.");
        return resposta;
    }

    private ObjectNode consultaSalva(RodoviarioConsultaRequest req, ReservaRodoviarioDetalheDTO detalhe) {
        String localizador = detalhe.getReserva().getLocalizador();
        exigir(localizador != null && localizador.matches("[1-9][0-9]{0,18}"), 409, "Reserva sem codigo de compra valido. Confira a importacao.");
        String informado = req.getConsulta().path("codigoCompra").asText(localizador);
        exigir(localizador.equals(informado), 409, "Codigo de compra diverge da reserva do Manager.");
        var consulta = json.createObjectNode().put("codigoCompra", Long.parseLong(localizador))
                .put("email", req.getConsulta().path("email").asText());
        consulta.put("sistema", req.getConsulta().path("sistema").asText("QueroPassagem"));
        return consulta;
    }

    private void conferirCompra(RodoviarioReservaOperacaoResponse r) {
        exigir(mapper.codigoCompra(r.getFornecedor().path("dados")).equals(r.getReserva().getReserva().getLocalizador()),
                409, "Fornecedor retornou outra compra.");
    }

    private boolean recebimentosCancelados(ReservaRodoviarioDetalheDTO detalhe) {
        JsonNode lista = json.valueToTree(detalhe).path("recebimentos");
        if (!lista.isArray() || lista.isEmpty()) return false;
        for (JsonNode item : lista) {
            if (item.path("codgRecebimento").asInt(0) <= 0
                || item.path("status").asInt(-1) != 0) return false;
        }
        return true;
    }

    private void sincronizar(RodoviarioReservaOperacaoResponse r, String usuario, String motivo) {
        conferirCompra(r);
        var detalhe = r.getReserva();
        var compra = r.getFornecedor().path("dados");
        int status = mapper.status(compra);
        Integer atual = detalhe.getReserva().getStatus();
        boolean jaEmitida = Integer.valueOf(1).equals(atual)
                            || (Integer.valueOf(0).equals(atual) && dataEmissaoRegistrada(detalhe));
        if (status == 0) {
            if (!Integer.valueOf(0).equals(atual)) r.setRequerConferencia(true);
            return;
        }
        if (status == 1 && !jaEmitida) atualizarBilhetes(detalhe, compra, usuario);
        if (status == 2 || (status == 1 && !jaEmitida)) {
            exigir(!Integer.valueOf(2).equals(atual) || status == 2, 409, "Reserva cancelada no Manager diverge do fornecedor.");
            var request = new StatusRodoviarioRequest();
            request.setStatus(status);
            request.setMotivo(motivo);
            manager.trocar("reservaRodoviario/" + detalhe.getReserva().getCodgReservaRodoviario() + "/statusFornecedor",
                    HttpMethod.PUT, request, null, usuario, JsonNode.class, true);
            r.setReserva(manager.detalhe(detalhe.getReserva().getCodgReservaRodoviario(), usuario));
            r.setStatusAtualizado(true);
        }
        r.setMergeExecutado(true);
    }

    private void atualizarBilhetes(ReservaRodoviarioDetalheDTO detalhe, JsonNode compra, String usuario) {
        var ids = new HashSet<Integer>();
        detalhe.getTrechos().forEach(t -> {
            ids.add(t.getCodgCidadeOrigem());
            ids.add(t.getCodgCidadeDestino());
        });
        var destinos = destinos(detalhe.getReserva().getCodgSistema(), ids, usuario);
        var atualizacoes = new ArrayList<RodoviarioBilheteDTO>();
        for (var t : detalhe.getTrechos()) {
            JsonNode vivo = null;
            for (var f : compra.path("trechos"))
                if (Objects.equals(destinos.get(t.getCodgCidadeOrigem()), f.path("origem").path("codigoFornecedor").asText())
                    && Objects.equals(destinos.get(t.getCodgCidadeDestino()), f.path("destino").path("codigoFornecedor").asText())) {
                    exigir(vivo == null, 409, "Trecho ambiguo na consulta do fornecedor.");
                    vivo = f;
                }
            exigir(vivo != null, 409, "Trecho ausente na consulta do fornecedor.");
            for (var p : detalhe.getPassageiros())
                if (Objects.equals(t.getCodgRodoviarioTrecho(), p.getCodgRodoviarioTrecho())) {
                    var dto = mapper.bilhete(mapper.passageiro(p, vivo.path("passageiros")));
                    exigir(Integer.valueOf(1).equals(dto.getStatus()), 409, "Passageiro ainda sem bilhete emitido.");
                    var existentes = detalhe.getBilhetes().stream().filter(b -> Objects.equals(b.getCodgRodoviarioPassageiro(), p.getCodgPassageiro())).toList();
                    exigir(existentes.size() <= 1, 409, "Multiplos bilhetes exigem conferencia.");
                    dto.setCodgRodoviarioBilhete(existentes.isEmpty() ? null : existentes.get(0).getCodgRodoviarioBilhete());
                    dto.setCodgRodoviarioTrecho(t.getCodgRodoviarioTrecho());
                    dto.setCodgRodoviarioPassageiro(p.getCodgPassageiro());
                    atualizacoes.add(dto);
                }
        }
        for (var b : atualizacoes) {
            Integer id = b.getCodgRodoviarioBilhete();
            manager.trocar("rodoviarioBilhete" + (id == null ? "" : "/" + id), id == null ? HttpMethod.POST : HttpMethod.PUT,
                    b, null, usuario, JsonNode.class, true);
        }
    }

    private List<RodoviarioDestinoDTO> destinosPorCidade(Integer sistema, Set<Integer> cidades, String usuario) {
        var destinos = new ArrayList<RodoviarioDestinoDTO>();
        for (Integer cidade : new TreeSet<>(cidades)) {
            int inicio = destinos.size();
            boolean concluido = false;
            for (int pagina = 0; pagina < 1000; pagina++) {
                var filtros = new LinkedMultiValueMap<String, String>();
                filtros.add("codgSistema", sistema.toString()); filtros.add("codgCidade", cidade.toString());
                filtros.add("page", String.valueOf(pagina)); filtros.add("size", "100");
                JsonNode result = manager.trocar("rodoviario/destinos", HttpMethod.GET, null, filtros, usuario, JsonNode.class, false);
                exigir(result.path("content").isArray(), 502, "Catalogo de destinos do Manager invalido.");
                for (var d : result.get("content")) {
                    var id = d.path("codgVinculoCidadeSistema");
                    if (d.path("codgCidade").isIntegralNumber() && d.path("codgCidade").asInt(-1) == cidade
                        && id.isIntegralNumber() && id.canConvertToInt() && id.asInt() > 0
                        && d.path("codgDestinoSistema").asText().matches("(?:CIT|ROD)_[1-9][0-9]{0,18}"))
                        destinos.add(new RodoviarioDestinoDTO(id.asInt(), cidade, d.get("codgDestinoSistema").asText()));
                }
                if (result.path("last").asBoolean() || result.path("content").size() < 100) { concluido = true; break; }
            }
            exigir(concluido, 502, "Catalogo de destinos do Manager excedeu o limite de paginas.");
            if (destinos.size() == inicio)
                throw RodoviarioIntegracaoException.falha(400, "CONFAPI", "VINCULOS_DESTINO_INVALIDOS",
                        "Nenhum destino ativo para codgCidade " + cidade + " no sistema " + sistema
                        + ". Informe o ID da tabela cidade, o mesmo usado na pesquisa.", true);
        }
        return destinos;
    }

    private Map<Integer, String> destinos(Integer sistema, Set<Integer> ids, String usuario) {
        var encontrados = new HashMap<Integer, String>();
        for (int pagina = 0; pagina < 1000; pagina++) {
            var filtros = new LinkedMultiValueMap<String, String>();
            filtros.add("codgSistema", sistema.toString());
            filtros.add("page", String.valueOf(pagina));
            filtros.add("size", "100");
            JsonNode result = manager.trocar("rodoviario/destinos", HttpMethod.GET, null, filtros, usuario, JsonNode.class, false);
            exigir(result.path("content").isArray(), 502, "Catalogo de destinos do Manager invalido.");
            for (var d : result.get("content")) {
                int id = d.path("codgVinculoCidadeSistema").asInt(-1);
                if (ids.contains(id) && d.path("codgDestinoSistema").isTextual())
                    encontrados.put(id, d.get("codgDestinoSistema").asText());
            }
            if (encontrados.keySet().containsAll(ids)) return encontrados;
            if (result.path("last").asBoolean() || result.path("content").size() < 100) break;
        }
        var ausentes = new TreeSet<>(ids);
        ausentes.removeAll(encontrados.keySet());
        throw RodoviarioIntegracaoException.falha(400, "CONFAPI", "VINCULOS_DESTINO_INVALIDOS",
                "Vinculos nao encontrados entre os destinos ativos do sistema " + sistema + ": " + ausentes
                + ". Confira os vinculos da reserva ja salva e o cadastro dos terminais no Manager.", true);
    }

    private Set<Integer> ids(SalvarReservaRodoviarioRequest req) {
        var ids = new HashSet<Integer>();
        req.getTrechos().forEach(t -> {
            ids.add(t.getTrecho().getCodgCidadeOrigem());
            ids.add(t.getTrecho().getCodgCidadeDestino());
        });
        return ids;
    }

    private String referencia(SalvarReservaRodoviarioRequest req) {
        try {
            String base = req.getReserva().getCodgAgencia() + ":" + req.getReserva().getCodgSistema() + ":" + req.getChaveIdempotencia();
            return "ROD-" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(base.getBytes(StandardCharsets.UTF_8))).substring(0, 20);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void registrarErro(RodoviarioReservaOperacaoResponse r, RuntimeException ex) {
        RodoviarioErroLog.registrar("resultado-parcial", ex);
        r.setCodigo(ex instanceof RodoviarioIntegracaoException ? ((RodoviarioIntegracaoException) ex).getResposta().path("codigo").asText("ERRO_INTEGRACAO") : "ERRO_INTEGRACAO");
    }

    private RodoviarioReservaOperacaoResponse parcial(RodoviarioReservaOperacaoResponse r, String mensagem) {
        r.setSuccess(false);
        r.setRequerConferencia(true);
        r.setMensagem(mensagem);
        return r;
    }

    private void exigir(boolean ok, int status, String motivo) {
        if (!ok) throw RodoviarioIntegracaoException.falha(status, "CONFAPI", "REQUEST_INVALIDO", motivo, false);
    }
}
