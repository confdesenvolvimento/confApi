package com.confApi.hoteis;

import com.confApi.cacheHotel.hotel.CacheHotelService;
import com.confApi.db.confManager.cambio.Cambio;
import com.confApi.db.confManager.cambio.CambioService;
import com.confApi.db.confManager.hotel.model.HotelAcomodacao;
import com.confApi.db.confManager.hotel.model.HotelResponse;
import com.confApi.db.confManager.hotel.model.QuartoPesquisa;
import com.confApi.db.confManager.markup.MarkupService;
import com.confApi.hoteis.model.pesquisa.HotelPesquisaModelFront;
import com.confApi.hoteis.model.reserva.CancelarReservaRequestHotelFront;
import com.confApi.hoteis.model.reserva.HotelCarregaModelFront;
import com.confApi.hoteis.model.reserva.ReservarRequestFront;
import com.confApi.hub.hotel.dto.HotelReserva;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class HotelSearchService {

    private final HotelClient hubClient;
    private final MarkupService markupService;
    private final CambioService cambioService;
    private static final double CAMBIO_PADRAO = 8.0;

    @Autowired
    private CacheHotelService cacheHotelService;

    public HotelSearchService(
            HotelClient hubClient,
            MarkupService markupService, CambioService cambioService
    ) {
        this.hubClient = hubClient;
        this.markupService = markupService;
        this.cambioService = cambioService;
    }

    public List<HotelResponse> pesquisar(HotelPesquisaModelFront req) {

        // 1) chamar HUB (client)
        List<HotelResponse> hotelResponse = hubClient.pesquisar(req);

        // 2) buscar markup por unidade/produto (Manager)
        Double mkp = markupService.findByCodProdutoValue(req.getIdentificacaoAgenciaModel().getCodgProduto());
        System.out.println("Olha o produto ai: " + req.getIdentificacaoAgenciaModel().getCodgProduto() + " " + mkp);

        // 3) aplicar comissao extra + markup + prazos
        List<Cambio> cambioList = cambioService.findUltimoCambio();
        System.out.println("Olha o cambioList ai: " + cambioList.size());
        for (Cambio cambio : cambioList) {
        System.out.println("cambio: " + cambio.getMoeda().getSigla() + " - "+cambio.getValorCotacao());

        }
        aplicarCambioHoteis(hotelResponse, cambioList);      // 4) devolver já pronto pra UI

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

    public HotelReserva efetuarReserva(ReservarRequestFront req) {

        // 1) chamar HUB (client)
        HotelReserva reserva = hubClient.efetuarReserva(req);


        return reserva;
    }

    public HotelReserva carregarReserva(HotelCarregaModelFront req) {

        // 1) chamar HUB (client)
        HotelReserva reserva = hubClient.carregarReserva(req);
        return reserva;
    }

    public String cancelarReserva(CancelarReservaRequestHotelFront req) {

        // 1) chamar HUB (client)
        String result = hubClient.cancelarReserva(req);
        return result;
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

}
