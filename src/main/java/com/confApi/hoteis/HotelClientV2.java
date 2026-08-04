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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class HotelClientV2 {

    private final HotelServiceV2 hotelService;
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

        List<HotelResponse> hotelResponse = hotelService.pesquisar(req);
        Double mkp = markupService.findVlrMarkup(req.getIdentificacaoAgenciaModel());

        List<Cambio> cambioList = cambioService.findUltimoCambio();
        aplicarCambioHoteis(hotelResponse, cambioList);      // 4) devolver já pronto pra UI
        aplicarMarkupHoteis(hotelResponse, mkp);
        aplicarPrazosHoteis(hotelResponse);
        //4) Salvar no cache -  processo realizado em thred para não parar o processo, mesmo com ou sem erro.
        CompletableFuture.runAsync(() -> {
            try {
                cacheHotelService.salvarCacheHotel(hotelResponse);
            } catch (Exception e) {
                // loga o erro mas não interrompe o fluxo
                System.err.println("Erro ao salvar cache: " + e.getMessage());
            }
        });
        return hotelResponse;
    }

    public ReservaHotel efetuarReserva(HotelPreReserva hotelPreReserva) {

        ReservarRequestFront reqBanco = new ReservarRequestFront(hotelPreReserva);
        HotelReserva reserva = hotelService.efetuarReserva(reqBanco);
        ReservaHotel rhBanco = convertToDB(reserva, hotelPreReserva);
        ReservaHotel reservaHotel = hotelService.criarReservaDB(rhBanco);

        return reservaHotel;
    }

    public HotelReserva carregarReserva(HotelCarregaModelFront req) {

        // 1) chamar HUB (client)
        HotelReserva reserva = hotelService.carregarReserva(req);
        return reserva;
    }

    public String cancelarReserva(ReservaHotelModel req) {
        CancelarReservaRequestHotelFront cancelarReservaRequestHotelFront = new CancelarReservaRequestHotelFront(req);
        Boolean result = hotelService.cancelarReserva(cancelarReservaRequestHotelFront);
        List<CancelarResponse> cancelarResponseList = new ArrayList<>();
        for (RecebimentoModel recebimento : req.getRecebimentos()) {
            if (recebimento.getStatusRecebimento() != 0) {
                List<CancelarResponse> cancelarResponses = recebimentoApi.cancelar(
                        recebimento.getCodgRecebimento(),
                        new Recebimento(recebimento));
                cancelarResponseList.addAll(cancelarResponses);
            }
        }

        if(result) {
            hotelService.cancelarReservaDB(req);
        }

        if (result) {
            return "Cancelado com sucesso!";
        } else {
            return "Erro ao cancelar a reserva";
        }
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
        //hotelDB.setCodgCidade(preReserva.getHotel().getCidade());
//        hotelDB.setCodgCidade(new Cidade(72));
//        hotelDB.setCodgHotel(Integer.parseInt(hotelApi.getCodgHotel()));
        hotelDB.setIdentificacaoHotelSitema(hotelApi.getCodgHotel());
        // hotelDB.setCodgSistema(new SistemaApi().findByCodgSistema(Integer.valueOf(preReserva.getHotel().getCodigoSistema())));
        if (preReserva.getHotel() != null) {
            hotelDB.setCodgSistema(new Sistema());
            if (preReserva.getHotel().getCodigoHotelSistema() != null) {
                Sistema sistemaApi = new Sistema(Integer.valueOf(preReserva.getHotel().getCodigoSistema()));
                hotelDB.setCodgSistema(sistemaApi);
                rhBanco.setCodgSistema(sistemaApi);
            }
            if (preReserva.getHotel().getNomeSistema() != null) {
                hotelDB.getCodgSistema().setNomeSistema(preReserva.getHotel().getNomeSistema());
                rhBanco.getCodgSistema().setNomeSistema(preReserva.getHotel().getNomeSistema());
            }
        }

        hotelDB.setMapa(null);

        hotelDB.setUrlImagemHotel(preReserva.getHotel().getUrlImagem());
        hotelDB.setTelefone(preReserva.getHotel().getTelefone());
        hotelDB.setNomeHotel(preReserva.getHotel().getNome());
        hotelDB.setLongitude(preReserva.getHotel().getLongitude().toString());
        hotelDB.setLatitude(preReserva.getHotel().getLatitude().toString());
        hotelDB.setIdentificacaoHotelSitema(preReserva.getHotel().getCodigoHotelSistema());
        hotelDB.setFax(preReserva.getHotel().getFax());
        hotelDB.setEstrela(preReserva.getHotel().getCategoria().doubleValue());
        hotelDB.setEndereco1(preReserva.getHotel().getEndereco());
        hotelDB.setEmail(preReserva.getHotel().getEmail());
        rhBanco.setCodgHotel(hotelDB);

        rhBanco.setCodgUsuarioCancelamento(null);
        rhBanco.setCodgUsuarioCriacao(new Usuario(preReserva.getUsuario()));
        rhBanco.setDataEmissao(null);
        /*Inicio Regra Cancelamento AJUSTAR*/
        if (preReserva.getHotel().getPrazoPagamentoFornecedor() == null) {
            rhBanco.setDataLimiteEmissao(new Timestamp(new Date().getTime()));
        } else {

            rhBanco.setDataLimiteEmissao(new Timestamp(preReserva.getHotel().getPrazoPagamentoFornecedor().getTime()));
        }
        /*Final Regra Cancelamento AJUSTAR*/

        rhBanco.setDescricaoMotivoCancelamento(null);
        rhBanco.setLocalizador(hotelApi.getReservasHotelRsList().get(0).getIdentificador());
        rhBanco.setStatus(StatusReservaHotel.Confirmada.getValor());

        rhBanco.setDataEntrada(hotelApi.getDataEntrada());
        rhBanco.setDataSaida(hotelApi.getDataSaida());
        Integer i = 0;

        for (HotelAcomodacao ac : preReserva.getAcomodacao()) {
            rhBanco.setDescricaoRegraCancelamento(ac.getDescricaoRegraCancelamento());
            if (ac.getPrazoPagamentoCliente() == null) {
                rhBanco.setPrazoPagamentoCliente(new java.sql.Timestamp(new Date().getTime()));
            } else {
                rhBanco.setPrazoPagamentoCliente(new java.sql.Timestamp(ac.getPrazoPagamentoCliente().getTime()));
            }
            if (ac.getPrazoPagamentoFornecedor() == null) {
                rhBanco.setPrazoPagamentoFornecedor(new java.sql.Timestamp(new Date().getTime()));
            } else {
                rhBanco.setPrazoPagamentoFornecedor(new java.sql.Timestamp(ac.getPrazoPagamentoFornecedor().getTime()));
            }
            HotelQuartoValor hotelQuartoValor = new HotelQuartoValor();
            if (ac.getCodgRoom().length() > 10) {
                hotelQuartoValor.setCodgFornecedorSistema(hotelApi.getReservasHotelRsList().get(0).getAcomodacoes().get(i).getCodgRoom());
            } else {
                hotelQuartoValor.setCodgFornecedorSistema(ac.getCodgRoom());
            }
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

            for (Hospedes hospedeAc : ac.getHospedes()) {
                HotelHospede hH = new HotelHospede();
                //  hH.setCodgHotelQuartoValor(hotelQuartoValor);
                if (hospedeAc.getTipo().equalsIgnoreCase("ADT")) {
                    hH.setTipoHospede("1");
                    hotelQuartoValor.setQtdAdt(hotelQuartoValor.getQtdAdt() + 1);
                } else if (hospedeAc.getTipo().equalsIgnoreCase("CHD")) {
                    hH.setTipoHospede("2");
                    hotelQuartoValor.setQtdChd(hotelQuartoValor.getQtdChd() + 1);
                } else if (hospedeAc.getTipo().equalsIgnoreCase("INF")) {
                    hotelQuartoValor.setQtdChd(hotelQuartoValor.getQtdChd() + +1);
                    hH.setTipoHospede("3");
                } else {
                    hH.setTipoHospede("1");
                }
                hH.setHospedeIdade(hospedeAc.getIdade());
                if (hospedeAc.getSexo().equalsIgnoreCase("M")) {
                    hH.setHospedeSexo(1);
                } else if (hospedeAc.getSexo().equalsIgnoreCase("F")) {
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
}
