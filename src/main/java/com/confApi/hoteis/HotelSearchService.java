package com.confApi.hoteis;

import com.confApi.db.confManager.hotel.model.HotelResponse;
import com.confApi.db.confManager.markup.MarkupService;
import com.confApi.hoteis.model.pesquisa.HotelPesquisaModelFront;
import com.confApi.hoteis.model.reserva.CancelarReservaRequestHotelFront;
import com.confApi.hoteis.model.reserva.HotelCarregaModelFront;
import com.confApi.hoteis.model.reserva.ReservarRequestFront;
import com.confApi.hub.hotel.dto.HotelReserva;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HotelSearchService {

    private final HotelClient hubClient;
    private final MarkupService markupService;
  //  private final HotelPricingService pricingService;

    public HotelSearchService(
            HotelClient hubClient,
            MarkupService markupService
    ) {
        this.hubClient = hubClient;
        this.markupService = markupService;
    }

    public List<HotelResponse> pesquisar(HotelPesquisaModelFront req) {

        // 1) chamar HUB (client)
        List<HotelResponse> resultado = hubClient.pesquisar(req);

        // 2) buscar markup por unidade/produto (Manager)
        Double mkp = markupService.findByCodProdutoValue(req.getIdentificacaoAgenciaModel().getCodgProduto());
        System.out.println("Olha o produto ai: "+req.getIdentificacaoAgenciaModel().getCodgProduto()+ " "+mkp);

        // 3) aplicar comissao extra + markup + prazos
       // pricingService.aplicarRegras(resultado, req, mkp);

        // 4) devolver já pronto pra UI
        return resultado;
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




}
