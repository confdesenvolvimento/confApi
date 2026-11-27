package com.confApi.hoteis;

import com.confApi.cacheHotel.hotel.CacheHotelService;
import com.confApi.db.confManager.cambio.Cambio;
import com.confApi.db.confManager.cambio.CambioService;
import com.confApi.db.confManager.hotel.dto.Hotel;
import com.confApi.db.confManager.hotel.model.*;
import com.confApi.db.confManager.hotelHospede.HotelHospede;
import com.confApi.db.confManager.hotelQuartoValor.HotelQuartoValor;
import com.confApi.db.confManager.markup.MarkupService;
import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.db.confManager.reservaHotel.dto.ReservaHotel;
import com.confApi.db.confManager.sistema.Sistema;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.endPoints.recebimento.CancelarResponse;
import com.confApi.endPoints.recebimento.RecebimentoApi;
import com.confApi.hoteis.eNums.StatusReservaHotel;
import com.confApi.hoteis.model.pesquisa.HotelPesquisaModelFront;
import com.confApi.hoteis.model.reserva.CancelarReservaRequestHotelFront;
import com.confApi.hoteis.model.reserva.HotelCarregaModelFront;
import com.confApi.hoteis.model.reserva.ReservarRequestFront;
import com.confApi.hub.aereo.RecebimentoModel;
import com.confApi.hub.hotel.dto.Hospedes;
import com.confApi.hub.hotel.dto.HotelReserva;
import com.confApi.hub.hotel.dto.ReservasHotelRs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class HotelClientV2 {

    private static final Logger LOG = LoggerFactory.getLogger(HotelClientV2.class);
    private static final String SISTEMA_OMNIBEES = Integer.toString(HotelServiceV2.CODIGO_SISTEMA_OMNIBEES);
    private final HotelServiceV2 hotelService;
    @Autowired private HotelSearchService pesquisaAtual;
    private final MarkupService markupService;
    private final CambioService cambioService;
    private static final double CAMBIO_PADRAO = 8.0;

    @Autowired
    private CacheHotelService cacheHotelService;

    @Autowired
    private RecebimentoApi recebimentoApi;

    public HotelClientV2(
            HotelServiceV2 hotelService,
            MarkupService markupService, CambioService cambioService
    ) {
        this.hotelService = hotelService;
        this.markupService = markupService;
        this.cambioService = cambioService;
    }

    public List<HotelResponse> pesquisar(HotelPesquisaModelFront req) {
        return pesquisaAtual.pesquisar(req);
    }

    public ReservaHotel efetuarReserva(HotelPreReserva hotelPreReserva) {

        ReservarRequestFront reqBanco = new ReservarRequestFront(hotelPreReserva);
        boolean omnibees = SISTEMA_OMNIBEES.equals(hotelPreReserva.getHotel().getCodigoSistema());
        HotelReserva reserva = reservarNoFornecedor(reqBanco, omnibees);
        if (reserva == null || reserva.getReservasHotelRsList() == null || reserva.getReservasHotelRsList().size() != 1
                || reserva.getReservasHotelRsList().get(0) == null
                || reserva.getReservasHotelRsList().get(0).getIdentificador() == null
                || reserva.getReservasHotelRsList().get(0).getIdentificador().isBlank()) {
            throw new IllegalStateException("O fornecedor não confirmou a reserva de hotel. Confira antes de tentar novamente.");
        }
        int status = StatusReservaHotelFornecedor.codigo(reserva.getReservasHotelRsList().get(0).getStatus());
        if (status != 1) throw new IllegalStateException("O fornecedor não confirmou a reserva de hotel.");
        ReservaHotel rhBanco = null;
        try {
            rhBanco = convertToDB(reserva, hotelPreReserva);
            rhBanco.setStatus(status);
            rhBanco.setDivergenciasOperacao(DivergenciasHotel.comparar(reqBanco, reserva));
            if (hotelPreReserva.getCodgPacote() != null && hotelPreReserva.getCodgPacote() > 0) {
                rhBanco.setCodgReservaPacote(new com.confApi.db.confManager.reservaPacote.ReservaPacote(hotelPreReserva.getCodgPacote()));
            }
            return hotelService.criarReservaDB(rhBanco);
        } catch (RuntimeException erro) {
            if (!omnibees) throw erro;
            LOG.warn("Reserva Omnibees confirmada no fornecedor com gravação pendente no Manager ({})",
                    erro.getClass().getSimpleName());
            var confirmada = reserva.getReservasHotelRsList().get(0);
            Date entrada = rhBanco == null ? dataReserva(reserva.getDataEntrada(), confirmada.getDataEntrada(), null)
                    : rhBanco.getDataEntrada();
            Date saida = rhBanco == null ? dataReserva(reserva.getDataSaida(), confirmada.getDataSaida(), null)
                    : rhBanco.getDataSaida();
            throw ReservaOmnibeesFalha.gravacaoNaoConfirmada(confirmada.getIdentificador(), entrada, saida,
                    reserva.getObservacaoOmnibees(), erro);
        }
    }

    private HotelReserva reservarNoFornecedor(ReservarRequestFront pedido, boolean omnibees) {
        try {
            return hotelService.efetuarReserva(pedido);
        } catch (ReservaOmnibeesFalha erro) {
            var resposta = erro.resposta();
            if (!omnibees || !"OMNIBEES_DETALHES_INDISPONIVEIS".equals(resposta.codigo()) || resposta.localizador() == null) {
                throw erro;
            }
            var confirmada = new ReservasHotelRs();
            confirmada.setIdentificador(resposta.localizador());
            confirmada.setStatus("Reserved");
            var reserva = new HotelReserva();
            reserva.setCodgHotel(pedido.getCodgHotel());
            reserva.setDataEntrada(pedido.getDtCheckIn());
            reserva.setDataSaida(pedido.getDtCheckOut());
            reserva.setReservasHotelRsList(List.of(confirmada));
            reserva.setObservacaoOmnibees(resposta.mensagem());
            return reserva;
        }
    }

    public HotelReserva carregarReserva(HotelCarregaModelFront req) {

        // 1) chamar HUB (client)
        HotelReserva reserva = hotelService.carregarReserva(req);
        return reserva;
    }

    public String cancelarReserva(ReservaHotelModel req) {
        ReservaHotel registro = hotelService.consultarReservaDB(req);
        if (registro.getCodgReservaPacote() != null)
            throw new IllegalArgumentException("Gerencie o cancelamento do hotel pelo pacote vinculado.");
        if (!Integer.valueOf(2).equals(registro.getStatus())) {
            if (!Boolean.TRUE.equals(hotelService.cancelarReserva(new CancelarReservaRequestHotelFront(req))))
                return "Erro ao cancelar a reserva";
            hotelService.cancelarReservaDB(req);
        }
        // Financial cancellation follows supplier and Manager confirmations, using persisted receipts.
        if (registro.getRecebimentos() != null) {
            for (Recebimento recebimento : registro.getRecebimentos()) {
                if (recebimento == null || Integer.valueOf(0).equals(recebimento.getStatus())) continue;
                List<CancelarResponse> respostas = recebimentoApi.cancelar(recebimento.getCodgRecebimento(), recebimento);
                boolean confirmado = respostas != null && respostas.stream().anyMatch(r -> r != null
                        && "Recebimento Banco".equalsIgnoreCase(r.getObjeto())
                        && ("Cancelamento integral confirmado.".equalsIgnoreCase(r.getMensagem())
                        || "Cancelamento integral já confirmado.".equalsIgnoreCase(r.getMensagem())));
                if (!confirmado) throw new IllegalStateException("Hotel cancelado; o recebimento precisa de conferência no Manager.");
            }
        }
        return "Cancelado com sucesso!";
    }

    public void aplicarCambioHoteis(List<HotelResponse> hotelResponse, List<Cambio> cambioList) {
        if (hotelResponse == null || hotelResponse.isEmpty()) return;

        Map<String, Double> cambioPorMoeda = buildCambioPorMoeda(cambioList);

        for (HotelResponse h : hotelResponse) {
            if (h == null || h.getQuartoPesquisa() == null) continue;

            for (QuartoPesquisa quartoPesquisa : h.getQuartoPesquisa()) {
                if (quartoPesquisa == null || quartoPesquisa.getAcomodacoes() == null) continue;

                for (HotelAcomodacao ha : quartoPesquisa.getAcomodacoes()) {
                    if (ha == null || ha.getTarifaHotel() == null) continue;

                    String moeda = ha.getTarifaHotel().getMoeda();

                    double cotacao = resolveCotacao(moeda, cambioPorMoeda);
                    ha.getTarifaHotel().setCambio(cotacao);
                }
            }
        }
    }
    private Map<String, Double> buildCambioPorMoeda(List<Cambio> cambioList) {
        if (cambioList == null || cambioList.isEmpty()) return Collections.emptyMap();

        Map<String, Double> map = new HashMap<>();
        for (Cambio c : cambioList) {
            if (c == null || c.getMoeda() == null) continue;
            String sigla = c.getMoeda().getSigla();
            if (sigla == null || sigla.isBlank()) continue;

            // Se vier repetido, mantém o primeiro (ou troque pra sobrescrever se preferir)
            map.putIfAbsent(sigla.trim().toUpperCase(Locale.ROOT), c.getValorCotacao());
        }
        return map;
    }

    private double resolveCotacao(String moeda, Map<String, Double> cambioPorMoeda) {
        // regra: se moeda for null/vazia -> 8.0
        if (moeda == null || moeda.isBlank()) return CAMBIO_PADRAO;

        Double cot = cambioPorMoeda.get(moeda.trim().toUpperCase(Locale.ROOT));
        // se não achou a moeda na lista, aplica padrão também (opcional, mas recomendo)
        return (cot != null ? cot : CAMBIO_PADRAO);
    }


    private void calcularValoresComMarkup(TarifaHotel tarifaHotel, Double markup, Integer qtdNoites) {
        if (tarifaHotel == null) {
            return;
        }

        if (tarifaHotel.getValorTotalEstadiaNet() == null) {
            return;
        }

        int noites = (qtdNoites != null && qtdNoites > 0) ? qtdNoites : 1;

        double percentualMarkup = markup != null ? markup : 0.0;
        tarifaHotel.setPercentualMarkupAplicado(percentualMarkup);

        // Se markup for zero, mantém os valores base
        if (percentualMarkup == 0.0) {
            tarifaHotel.setValorMarkupAplicado(0.0);
            tarifaHotel.setValorTotalEstadiaComMarkup(tarifaHotel.getValorTotalEstadiaNet());

            double taxaCambioZero = 1.0;
            if (tarifaHotel.getMoeda() != null
                    && !tarifaHotel.getMoeda().equalsIgnoreCase("BRL")
                    && tarifaHotel.getCambio() != null
                    && tarifaHotel.getCambio() > 0) {
                taxaCambioZero = tarifaHotel.getCambio();
            }

            tarifaHotel.setCambio(taxaCambioZero);
            tarifaHotel.setValorTotalEstadiaComMarkupBrl(tarifaHotel.getValorTotalEstadiaNet() * taxaCambioZero);

            if (tarifaHotel.getMediaDiaria() != null) {
                tarifaHotel.setMediaDiariaNet(tarifaHotel.getMediaDiaria());
                tarifaHotel.setMediaDiaria(tarifaHotel.getMediaDiariaNet());
                tarifaHotel.setTotalDiarias(tarifaHotel.getMediaDiaria() * noites);
            } else {
                double media = tarifaHotel.getValorTotalEstadiaNet() / noites;
                tarifaHotel.setMediaDiariaNet(media);
                tarifaHotel.setMediaDiaria(media);
                tarifaHotel.setTotalDiarias(media * noites);
            }

            tarifaHotel.setValorTaxaIss(0.0);
            tarifaHotel.setValorTaxaServico(0.0);
            tarifaHotel.setValorComissaoExtra(0.0);
            return;
        }

        // Evita divisão inválida
        if (percentualMarkup <= 0.0) {
            percentualMarkup = 1.0;
        }

        // Mantendo a fórmula original que já funciona no seu projeto
        double valorMarkupAplicado =
                (tarifaHotel.getValorTotalEstadiaNet() / percentualMarkup) - tarifaHotel.getValorTotalEstadiaNet();

        double valorTotalEstadiaComMarkup =
                tarifaHotel.getValorTotalEstadiaNet() + valorMarkupAplicado;

        double taxaCambio = 1.0;
        if (tarifaHotel.getMoeda() != null
                && !tarifaHotel.getMoeda().equalsIgnoreCase("BRL")
                && tarifaHotel.getCambio() != null
                && tarifaHotel.getCambio() > 0) {
            taxaCambio = tarifaHotel.getCambio();
        }

        tarifaHotel.setCambio(taxaCambio);

        double valorTotalEstadiaComMarkupBrl = valorTotalEstadiaComMarkup * taxaCambio;

        tarifaHotel.setValorMarkupAplicado(valorMarkupAplicado);

        if (tarifaHotel.getMediaDiaria() != null) {
            tarifaHotel.setMediaDiariaNet(tarifaHotel.getMediaDiaria());
        } else {
            tarifaHotel.setMediaDiariaNet(tarifaHotel.getValorTotalEstadiaNet() / noites);
        }

        tarifaHotel.setMediaDiaria(tarifaHotel.getMediaDiariaNet() + (valorMarkupAplicado / noites));
        tarifaHotel.setTotalDiarias(tarifaHotel.getMediaDiaria() * noites);

        // Taxas
        double valorTaxaIss = 0.0;
        if (tarifaHotel.getPercentualTaxaIss() != null) {
            valorTaxaIss =
                    (tarifaHotel.getValorTotalEstadiaNet() * (tarifaHotel.getPercentualTaxaIss() / 100.0)) * taxaCambio;
        }
        tarifaHotel.setValorTaxaIss(valorTaxaIss);

        double valorTaxaServico = 0.0;
        if (tarifaHotel.getPercentualTaxaServico() != null) {
            valorTaxaServico =
                    (tarifaHotel.getValorTotalEstadiaNet() * (tarifaHotel.getPercentualTaxaServico() / 100.0)) * taxaCambio;
        }
        tarifaHotel.setValorTaxaServico(valorTaxaServico);

        // Comissão extra não será calculada na API
        tarifaHotel.setValorComissaoExtra(0.0);

        // Total final sem comissão extra
        tarifaHotel.setValorTotalEstadiaComMarkup(
                valorTotalEstadiaComMarkup
                        + (tarifaHotel.getValorTaxaServico() / taxaCambio)
                        + (tarifaHotel.getValorTaxaIss() / taxaCambio)
        );

        tarifaHotel.setValorTotalEstadiaComMarkupBrl(
                valorTotalEstadiaComMarkupBrl
                        + tarifaHotel.getValorTaxaServico()
                        + tarifaHotel.getValorTaxaIss()
        );
    }


    private double obterTaxaCambio(TarifaHotel tarifaHotel) {
        if (tarifaHotel.getMoeda() == null || "BRL".equalsIgnoreCase(tarifaHotel.getMoeda())) {
            return 1.0;
        }

        if (tarifaHotel.getCambio() != null && tarifaHotel.getCambio() > 0) {
            return tarifaHotel.getCambio();
        }

        return 1.0;
    }

    private double nvl(Double valor) {
        return valor != null ? valor : 0.0;
    }
    private void aplicarMarkupHoteis(List<HotelResponse> hoteis, Double markup) {
        if (hoteis == null || hoteis.isEmpty()) {
            return;
        }

        for (HotelResponse hotel : hoteis) {
            if (hotel == null || hotel.getTarifasHotel() == null) {
                continue;
            }
            popularQuantidadeNoites(hotel);
            for (QuartoPesquisa quartoPesquisa : hotel.getQuartoPesquisa()) {
                for (HotelAcomodacao tarifa : quartoPesquisa.getAcomodacoes()) {


                if (tarifa == null) {
                    continue;
                }

                Integer qtdNoites = hotel.getQuantidadeNoites();


                calcularValoresComMarkup(tarifa.getTarifaHotel(), markup, qtdNoites);
            }
        }
        }
    }

    private void popularQuantidadeNoites(HotelResponse hotel) {
        if (hotel == null) {
            return;
        }

        // Se já estiver preenchido corretamente, mantém
        if (hotel.getQuantidadeNoites() != null && hotel.getQuantidadeNoites() > 0) {
            return;
        }

        try {
            // Prioridade para checkin/checkout string no formato dd/MM/yyyy
            if (hotel.getCheckin() != null && hotel.getCheckout() != null
                    && !hotel.getCheckin().trim().isEmpty()
                    && !hotel.getCheckout().trim().isEmpty()) {

                java.time.format.DateTimeFormatter formatter =
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

                java.time.LocalDate dataEntrada = java.time.LocalDate.parse(hotel.getCheckin().trim(), formatter);
                java.time.LocalDate dataSaida = java.time.LocalDate.parse(hotel.getCheckout().trim(), formatter);

                long noites = java.time.temporal.ChronoUnit.DAYS.between(dataEntrada, dataSaida);
                hotel.setQuantidadeNoites(noites > 0 ? (int) noites : 1);
                return;
            }

            // Fallback para Date
            if (hotel.getDataEntrada() != null && hotel.getDataSaida() != null) {
                java.time.LocalDate dataEntrada = hotel.getDataEntrada().toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();

                java.time.LocalDate dataSaida = hotel.getDataSaida().toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();

                long noites = java.time.temporal.ChronoUnit.DAYS.between(dataEntrada, dataSaida);
                hotel.setQuantidadeNoites(noites > 0 ? (int) noites : 1);
                return;
            }

            hotel.setQuantidadeNoites(1);

        } catch (Exception e) {
            hotel.setQuantidadeNoites(1);
            System.err.println("Erro ao calcular quantidade de noites do hotel: " + e.getMessage());
        }
    }

    private void ajustarPrazos(HotelAcomodacao ha) {
        if (ha == null) {
            return;
        }

        Date dataAtual = new Date();

        if (ha.getPoliticaCancelamento() != null && !ha.getPoliticaCancelamento().isEmpty()) {
            String dataLimiteStr = ha.getPoliticaCancelamento().get(0).getDataLimite();
            Date prazoPagamentoFornecedor;

            if (dataLimiteStr == null || dataLimiteStr.trim().isEmpty()) {
                prazoPagamentoFornecedor = dataAtual;
            } else {
                prazoPagamentoFornecedor = converterStringParaDate(dataLimiteStr);
                if (prazoPagamentoFornecedor == null) {
                    prazoPagamentoFornecedor = dataAtual;
                }
            }

            ha.setPrazoPagamentoFornecedor(prazoPagamentoFornecedor);

            // Subtrai 48 horas do prazo do fornecedor para obter o prazo do cliente
            Date prazoPagamentoCliente = subtrairHoras(prazoPagamentoFornecedor, 48);
            ha.setPrazoPagamentoCliente(prazoPagamentoCliente);

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            String prazoCancelamentoClienteFormatado = dateFormat.format(prazoPagamentoCliente);

            if (prazoPagamentoCliente.before(dataAtual)) {
                ha.setIsNaoReembolsavel(true);
                ha.setDescricaoRegraCancelamento("Cancelamento com multa. O prazo de cancelamento sem multa já expirou.");
            } else {
                ha.setIsNaoReembolsavel(false);
                ha.setDescricaoRegraCancelamento("Cancelamento sem multa até " + prazoCancelamentoClienteFormatado);
            }

        } else {
            ha.setPrazoPagamentoCliente(dataAtual);
            ha.setPrazoPagamentoFornecedor(dataAtual);
            ha.setIsNaoReembolsavel(false);

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            String prazoCancelamentoClienteFormatado = dateFormat.format(dataAtual);
            ha.setDescricaoRegraCancelamento("Cancelamento sem multa até " + prazoCancelamentoClienteFormatado);
        }
    }
    private Date converterStringParaDate(String dataStr) {
        if (dataStr == null || dataStr.trim().isEmpty()) {
            return null;
        }

        List<String> formatos = Arrays.asList(
                "dd/MM/yyyy",
                "dd/MM/yyyy HH:mm:ss",
                "yyyy-MM-dd",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ssXXX"
        );

        for (String formato : formatos) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(formato);
                sdf.setLenient(false);
                return sdf.parse(dataStr.trim());
            } catch (Exception e) {
                // tenta próximo formato
            }
        }

        return null;
    }
    public  Date subtrairHoras(Date data, int horas) {
        if (data == null) {
            throw new IllegalArgumentException("A data não pode ser nula.");
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(data);
        calendar.add(Calendar.HOUR_OF_DAY, -horas); // Subtrai as horas
        return calendar.getTime();
    }
    private void aplicarPrazosHoteis(List<HotelResponse> hoteis) {
        if (hoteis == null || hoteis.isEmpty()) {
            return;
        }

        for (HotelResponse hotel : hoteis) {
            if (hotel == null || hotel.getQuartoPesquisa() == null || hotel.getQuartoPesquisa().isEmpty()) {
                continue;
            }

            for (QuartoPesquisa quartoPesquisa : hotel.getQuartoPesquisa()) {
                if (quartoPesquisa == null || quartoPesquisa.getAcomodacoes() == null || quartoPesquisa.getAcomodacoes().isEmpty()) {
                    continue;
                }

                for (HotelAcomodacao acomodacao : quartoPesquisa.getAcomodacoes()) {
                    ajustarPrazos(acomodacao);
                }
            }
        }
    }

    public ReservaHotel convertToDB(HotelReserva hotelApi, HotelPreReserva preReserva) {
        ReservaHotel rhBanco = new ReservaHotel();
        rhBanco.setCodgAgencia(preReserva.getUsuario().getAgencia());
        Hotel hotelDB = new Hotel();
        var hotel = preReserva.getHotel();
        boolean omnibees = SISTEMA_OMNIBEES.equals(hotel.getCodigoSistema());
        Sistema sistema = new Sistema();
        sistema.setNomeSistema(hotel.getNomeSistema());
        if (hotel.getCodigoSistema() != null && !hotel.getCodigoSistema().isBlank()) {
            sistema.setCodgSistema(Integer.valueOf(hotel.getCodigoSistema()));
            rhBanco.setCodgSistema(sistema);
        }
        hotelDB.setCodgSistema(sistema);
        hotelDB.setIdentificacaoHotelSitema(hotel.getCodigoHotelSistema() == null
                ? hotelApi.getCodgHotel() : hotel.getCodigoHotelSistema());

        hotelDB.setUrlImagemHotel(hotel.getUrlImagem());
        hotelDB.setTelefone(hotel.getTelefone());
        hotelDB.setNomeHotel(hotel.getNome());
        hotelDB.setLongitude(Objects.toString(hotel.getLongitude(), null));
        hotelDB.setLatitude(Objects.toString(hotel.getLatitude(), null));
        hotelDB.setFax(hotel.getFax());
        hotelDB.setEstrela(hotel.getCategoria() == null ? null : hotel.getCategoria().doubleValue());
        hotelDB.setEndereco1(hotel.getEndereco());
        hotelDB.setEmail(hotel.getEmail());
        rhBanco.setCodgHotel(hotelDB);

        rhBanco.setCodgUsuarioCancelamento(null);
        rhBanco.setCodgUsuarioCriacao(new Usuario(preReserva.getUsuario()));
        rhBanco.setDataEmissao(null);
        aplicarPrazosReserva(rhBanco, preReserva);

        rhBanco.setDescricaoMotivoCancelamento(null);
        var retorno = hotelApi.getReservasHotelRsList().get(0);
        rhBanco.setLocalizador(retorno.getIdentificador());
        rhBanco.setStatus(StatusReservaHotel.Confirmada.getValor());
        rhBanco.setDataEntrada(omnibees ? dataReserva(hotelApi.getDataEntrada(), retorno.getDataEntrada(), hotel.getDataEntrada())
                : hotelApi.getDataEntrada());
        rhBanco.setDataSaida(omnibees ? dataReserva(hotelApi.getDataSaida(), retorno.getDataSaida(), hotel.getDataSaida())
                : hotelApi.getDataSaida());
        if (omnibees) rhBanco.setObservacaoOmnibees(hotelApi.getObservacaoOmnibees());
        int i = 0;

        for (HotelAcomodacao ac : preReserva.getAcomodacao()) {
            rhBanco.setDescricaoRegraCancelamento(ac.getDescricaoRegraCancelamento());
            HotelQuartoValor hotelQuartoValor = new HotelQuartoValor();
            String codigoQuarto = ac.getCodgRoom();
            if (!omnibees && codigoQuarto != null && codigoQuarto.length() > 10) {
                codigoQuarto = retorno.getAcomodacoes().get(i).getCodgRoom();
            }
            hotelQuartoValor.setCodgFornecedorSistema(codigoQuarto);
            hotelQuartoValor.setSiglaTipoQuarto(null);
            hotelQuartoValor.setNomeTipoQuarto(null);
            hotelQuartoValor.setNomeQuarto(ac.getNomeQuarto());
            hotelQuartoValor.setDescricaoRegime(ac.getRegime());
            hotelQuartoValor.setValorDiariaNet(ac.getTarifaHotel().getMediaDiariaNet());
            hotelQuartoValor.setValorDiariaMarkup(ac.getTarifaHotel().getMediaDiaria()/*/ac.getTarifaHotel().getCambio()*/);
            hotelQuartoValor.setValorTotalEstadiaMarkup(ac.getTarifaHotel().getValorTotalEstadiaComMarkup());
            hotelQuartoValor.setValorDiariaTotalNet(0.0);
            hotelQuartoValor.setValorTotalEstadiaNet(ac.getTarifaHotel().getValorTotalEstadiaNet());

            hotelQuartoValor.setPercTaxaIss(ac.getTarifaHotel().getPercentualTaxaIss());
            hotelQuartoValor.setValorTaxaIss(ac.getTarifaHotel().getValorTaxaIss() / ac.getTarifaHotel().getCambio());
            hotelQuartoValor.setPercTaxaServico(ac.getTarifaHotel().getPercentualTaxaServico());
            hotelQuartoValor.setValorTaxaServico(ac.getTarifaHotel().getValorTaxaServico() / ac.getTarifaHotel().getCambio());
            hotelQuartoValor.setPercMkpAplicado(ac.getTarifaHotel().getPercentualMarkupAplicado());
            hotelQuartoValor.setValorMkpAplicado(ac.getTarifaHotel().getValorMarkupAplicado());
            hotelQuartoValor.setValorTaxasExtras(0.0);
            hotelQuartoValor.setValorDiariaTotalMarkup(ac.getTarifaHotel().getTotalDiarias()/*/ac.getTarifaHotel().getCambio()*/);
            hotelQuartoValor.setPercTaxaExtra(ac.getTarifaHotel().getPercentualComissaoExtra());
            hotelQuartoValor.setValorTaxaExtra(ac.getTarifaHotel().getValorComissaoExtra() / ac.getTarifaHotel().getCambio());
            hotelQuartoValor.setMoeda(ac.getTarifaHotel().getMoeda());
            hotelQuartoValor.setVlrCambio(ac.getTarifaHotel().getCambio());
            if (hotelQuartoValor.getHotelHospedeList() == null) {
                hotelQuartoValor.setHotelHospedeList(new ArrayList<>());
            }

            for (Hospedes hospedeAc : ac.getHospedes() == null ? List.<Hospedes>of() : ac.getHospedes()) {
                HotelHospede hH = new HotelHospede();
                //  hH.setCodgHotelQuartoValor(hotelQuartoValor);
                if ("ADT".equalsIgnoreCase(hospedeAc.getTipo())) {
                    hH.setTipoHospede("1");
                    hotelQuartoValor.setQtdAdt(hotelQuartoValor.getQtdAdt() + 1);
                } else if ("CHD".equalsIgnoreCase(hospedeAc.getTipo())) {
                    hH.setTipoHospede("2");
                    hotelQuartoValor.setQtdChd(hotelQuartoValor.getQtdChd() + 1);
                } else if ("INF".equalsIgnoreCase(hospedeAc.getTipo())) {
                    hotelQuartoValor.setQtdChd(hotelQuartoValor.getQtdChd() + +1);
                    hH.setTipoHospede("3");
                } else {
                    hH.setTipoHospede("1");
                }
                hH.setHospedeIdade(hospedeAc.getIdade());
                if ("M".equalsIgnoreCase(hospedeAc.getSexo())) {
                    hH.setHospedeSexo(1);
                } else if ("F".equalsIgnoreCase(hospedeAc.getSexo())) {
                    hH.setHospedeSexo(0);
                } else {
                    hH.setHospedeSexo(1);
                }
                hH.setHospedeSobrenome(hospedeAc.getSobrenomePassageiro());
                hH.setHospedeNome(hospedeAc.getNomePassageiro());
                hotelQuartoValor.getHotelHospedeList().add(hH);

            }

            if (rhBanco.getHotelQuartoValorList() == null) {
                rhBanco.setHotelQuartoValorList(new ArrayList<>());
            }
            rhBanco.getHotelQuartoValorList().add(hotelQuartoValor);
        }

        return rhBanco;
    }

    private static Date dataReserva(Date geral, Date reserva, Date solicitada) {
        return geral != null ? geral : reserva != null ? reserva : solicitada;
    }

    /** Conserva o limite mais restritivo das acomodações selecionadas e do hotel. */
    private static void aplicarPrazosReserva(ReservaHotel reserva, HotelPreReserva origem) {
        Date clienteGeral = origem.getHotel().getPrazoPagamentoCliente();
        Date fornecedorGeral = origem.getHotel().getPrazoPagamentoFornecedor();
        Date cliente = clienteGeral;
        Date fornecedor = fornecedorGeral;
        boolean clienteConferido = true;
        boolean fornecedorConferido = true;
        for (HotelAcomodacao quarto : origem.getAcomodacao()) {
            Date prazoCliente = menorPrazo(clienteGeral, quarto.getPrazoPagamentoCliente());
            Date prazoFornecedor = menorPrazo(fornecedorGeral, quarto.getPrazoPagamentoFornecedor());
            clienteConferido &= prazoCliente != null;
            fornecedorConferido &= prazoFornecedor != null;
            cliente = menorPrazo(cliente, prazoCliente);
            fornecedor = menorPrazo(fornecedor, prazoFornecedor);
        }
        reserva.setPrazoPagamentoCliente(clienteConferido ? timestamp(cliente) : null);
        Date limiteFornecedor = fornecedorConferido ? fornecedor : null;
        reserva.setPrazoPagamentoFornecedor(timestamp(limiteFornecedor));
        reserva.setDataLimiteEmissao(timestamp(limiteFornecedor));
    }

    private static Date menorPrazo(Date primeiro, Date segundo) {
        return segundo == null ? primeiro : primeiro == null || segundo.before(primeiro) ? segundo : primeiro;
    }

    private static Timestamp timestamp(Date data) {
        return data == null ? null : new Timestamp(data.getTime());
    }
}
