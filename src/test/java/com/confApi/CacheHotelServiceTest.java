package com.confApi;

import com.confApi.cacheHotel.hotel.CacheHotelService;
import com.confApi.cacheHotel.hotel.DTO.CacheHotelDTO;
import com.confApi.db.confManager.hotel.model.HotelAcomodacao;
import com.confApi.db.confManager.hotel.model.HotelResponse;
import com.confApi.db.confManager.hotel.model.QuartoPesquisa;
import com.confApi.db.confManager.hotel.model.TarifaHotel;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CacheHotelServiceTest {

    @Test
    void gravaMenorTarifaFinalDeVendaComDiariaCoerente() {
        HotelResponse hotel = new HotelResponse();
        hotel.setNome("Hotel Teste");
        hotel.setCodigoCidade("CGB");
        hotel.setQuantidadeNoites(2);
        hotel.setQuartoPesquisa(List.of(quarto(
                acomodacao("Luxo", "Cafe da manha", 300.0, 30.0, 6.0),
                acomodacao("Standard", "Sem refeicao", 220.0, 20.0, 5.0)
        )));

        CacheHotelDTO dto = new CacheHotelService().converterParaDTO(hotel);

        assertEquals("Standard", dto.getNomeQuarto());
        assertEquals("Sem refeicao", dto.getRegime());
        assertEquals(220.0, dto.getTotalDiarias());
        assertEquals(110.0, dto.getDiariaMedia());
        assertEquals(25.0, dto.getTaxas());
    }

    private static QuartoPesquisa quarto(HotelAcomodacao... acomodacoes) {
        QuartoPesquisa quarto = new QuartoPesquisa();
        quarto.setAcomodacoes(List.of(acomodacoes));
        return quarto;
    }

    private static HotelAcomodacao acomodacao(
            String nome,
            String regime,
            double totalVenda,
            double taxaServico,
            double taxaIss
    ) {
        TarifaHotel tarifa = new TarifaHotel();
        tarifa.setValorTotalEstadiaComMarkupBrl(totalVenda);
        tarifa.setValorTaxaServico(taxaServico);
        tarifa.setValorTaxaIss(taxaIss);

        HotelAcomodacao acomodacao = new HotelAcomodacao();
        acomodacao.setNomeQuarto(nome);
        acomodacao.setRegime(regime);
        acomodacao.setTarifaHotel(tarifa);
        return acomodacao;
    }
}
