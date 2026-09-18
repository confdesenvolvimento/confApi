package com.confApi.cacheHotel.hotel;

import com.confApi.cacheHotel.cidade.CacheHotelCidade;
import com.confApi.cacheHotel.cidade.CacheHotelCidadeAPI;
import com.confApi.cacheHotel.hotel.DTO.CacheHotelDTO;
import com.confApi.db.confManager.hotel.model.HotelAcomodacao;
import com.confApi.db.confManager.hotel.model.HotelResponse;
import com.confApi.db.confManager.hotel.model.QuartoPesquisa;
import com.confApi.db.confManager.markup.MarkupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CacheHotelService {


    @Autowired
    private CacheHotelAPI cacheHotelAPI;

    @Autowired
    private MarkupService markupService;

    @Autowired
    private CacheHotelCidadeAPI cacheHotelCidadeAPI;

    public List<CacheHotelDTO> salvarCacheHotel(List<HotelResponse> hotelResponses) {

        // verificar se está nulo ou vazio
        if (hotelResponses == null || hotelResponses.isEmpty()) {
            return Collections.emptyList();
        }

        // converte para CacheHotelDTO e devolve em lista
        List<CacheHotelDTO> lista = hotelResponses.stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());

        //metodo aplicar mkp chamar findByCodProdutoValue
        //lista = aplicarMKPTotalTarifa(lista);

        //metodo para ajustar o nome da cidade, estado e país
        lista = ajusteNomeCidadeEstadoPais(lista);

        // enviar o resultado para o metodo gravarBusca()
        return cacheHotelAPI.gravarBusca(lista);
    }

    public CacheHotelDTO converterParaDTO(HotelResponse hotelResponse) {

        CacheHotelDTO dto = new CacheHotelDTO();

        dto.setNomeHotel(hotelResponse.getNome());
        dto.setCategoria(hotelResponse.getCategoria());
        dto.setEndereco(hotelResponse.getEndereco());

        dto.setCodgCidade(hotelResponse.getCodigoCidade());
        dto.setNomeCidade(hotelResponse.getCidade());
        dto.setNomeEstado(hotelResponse.getEstado());
        dto.setNomePais(hotelResponse.getPais());

        dto.setCheckin(hotelResponse.getDataEntrada());
        dto.setCheckout(hotelResponse.getDataSaida());

        List<HotelAcomodacao> acomodacoes = selecionarMelhoresAcomodacoes(hotelResponse);
        HotelAcomodacao acomodacao = acomodacoes.stream().findFirst().orElse(null);

        if (acomodacao != null) {
            dto.setNomeQuarto(acomodacao.getNomeQuarto());
            dto.setRegime(acomodacao.getRegime());
        }

        double totalVenda = acomodacoes.stream()
                .map(HotelAcomodacao::getTarifaHotel)
                .mapToDouble(tarifa -> tarifa.getValorTotalEstadiaComMarkupBrl())
                .sum();
        double taxas = acomodacoes.stream()
                .map(HotelAcomodacao::getTarifaHotel)
                .mapToDouble(tarifa -> valor(tarifa.getValorTaxaServico())
                        + valor(tarifa.getValorTaxaIss()))
                .sum();
        int noites = quantidadeNoites(hotelResponse);

        if (totalVenda <= 0 && hotelResponse.getTarifasHotel() != null) {
            totalVenda = primeiroPositivo(
                    hotelResponse.getTarifasHotel().getValorTotalEstadiaComMarkupBrl(),
                    hotelResponse.getTarifasHotel().getValorTotalEstadiaComMarkup(),
                    hotelResponse.getTarifasHotel().getValorTotalEstadiaNet()
            );
            taxas = valor(hotelResponse.getTarifasHotel().getValorTaxaServico())
                    + valor(hotelResponse.getTarifasHotel().getValorTaxaIss());
        }

        dto.setTotalDiarias(totalVenda);
        dto.setTaxas(taxas);
        dto.setDiariaMedia(noites > 0 && totalVenda > 0 ? totalVenda / noites : totalVenda);

        dto.setLatitude(String.valueOf(hotelResponse.getLatitude()));
        dto.setLongitude(String.valueOf(hotelResponse.getLongitude()));

        dto.setNomeSistema(hotelResponse.getNomeSistema());

        dto.setQtdHospedes(
                (hotelResponse.getQuantidadeAdultos() != null ? hotelResponse.getQuantidadeAdultos() : 0) +
                        (hotelResponse.getQuantidadeCriancas() != null ? hotelResponse.getQuantidadeCriancas() : 0));

        return dto;
    }

    private List<HotelAcomodacao> selecionarMelhoresAcomodacoes(HotelResponse hotel) {
        if (hotel == null || hotel.getQuartoPesquisa() == null) {
            return Collections.emptyList();
        }

        List<HotelAcomodacao> selecionadas = new ArrayList<>();
        for (QuartoPesquisa quarto : hotel.getQuartoPesquisa()) {
            if (quarto == null || quarto.getAcomodacoes() == null) {
                continue;
            }
            quarto.getAcomodacoes().stream()
                    .filter(this::possuiValorVendaValido)
                    .min(Comparator.comparing(
                            acomodacao -> acomodacao.getTarifaHotel().getValorTotalEstadiaComMarkupBrl()
                    ))
                    .ifPresent(selecionadas::add);
        }
        return selecionadas;
    }

    private boolean possuiValorVendaValido(HotelAcomodacao acomodacao) {
        return acomodacao != null
                && acomodacao.getTarifaHotel() != null
                && acomodacao.getTarifaHotel().getValorTotalEstadiaComMarkupBrl() != null
                && acomodacao.getTarifaHotel().getValorTotalEstadiaComMarkupBrl() > 0;
    }

    private int quantidadeNoites(HotelResponse hotel) {
        if (hotel.getQuantidadeNoites() != null && hotel.getQuantidadeNoites() > 0) {
            return hotel.getQuantidadeNoites();
        }
        if (hotel.getDataEntrada() == null || hotel.getDataSaida() == null) {
            return 1;
        }
        long noites = ChronoUnit.DAYS.between(
                hotel.getDataEntrada().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                hotel.getDataSaida().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        );
        return noites > 0 ? (int) noites : 1;
    }

    private double primeiroPositivo(Double... valores) {
        if (valores != null) {
            for (Double valor : valores) {
                if (valor != null && !valor.isNaN() && !valor.isInfinite() && valor > 0) {
                    return valor;
                }
            }
        }
        return 0.0;
    }

    private double valor(Double valor) {
        return valor == null || valor.isNaN() || valor.isInfinite() ? 0.0 : valor;
    }

    public List<CacheHotelDTO> aplicarMKPTotalTarifa(List<CacheHotelDTO> lista) {

        Double mkp = markupService.findByCodProdutoValue(2);

        if (mkp == null || mkp == 0) {
            throw new IllegalArgumentException("MKP inválido");
        }

        BigDecimal mkpBD = BigDecimal.valueOf(mkp);

        for (CacheHotelDTO hotel  : lista) {

            if (hotel .getTotalDiarias() != null && hotel.getDiariaMedia() != null) {

                BigDecimal totalDiariasNet = BigDecimal.valueOf(hotel.getTotalDiarias());
                BigDecimal totalTarifaMKP = totalDiariasNet
                        .divide(mkpBD,2, RoundingMode.HALF_UP);

                BigDecimal diariaMediaNet = BigDecimal.valueOf(hotel.getDiariaMedia());
                BigDecimal diariaMediaMKP = diariaMediaNet
                        .divide(mkpBD, 2, RoundingMode.HALF_UP);

                hotel.setTotalDiarias(totalTarifaMKP.doubleValue());
                hotel.setDiariaMedia(diariaMediaMKP.doubleValue());
            }
        }
        return lista;
    }

    public List<CacheHotelDTO> ajusteNomeCidadeEstadoPais(List<CacheHotelDTO> lista) {

        if (lista == null || lista.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, CacheHotelCidade> cache = new HashMap<>();

        for (CacheHotelDTO hotel : lista) {

            String codCidade = hotel.getCodgCidade();

            if (codCidade == null || codCidade.isEmpty()) {
                throw new IllegalArgumentException("Não possui CodgCidade");
            }

            CacheHotelCidade cacheHotelCidade = cache.get(codCidade);

            if (cacheHotelCidade == null) {
                cacheHotelCidade = cacheHotelCidadeAPI.findFirstByCodeCidade(codCidade);
                cache.put(codCidade, cacheHotelCidade);
            }

            if (cacheHotelCidade == null) {
                // pode trocar por log se não quiser quebrar tudo
                throw new IllegalArgumentException("Não retornou cidade para código: " + codCidade);
            }

            hotel.setNomeCidade(cacheHotelCidade.getNomeCidade());
            hotel.setNomeEstado(cacheHotelCidade.getNomeEstado());
            hotel.setNomePais(cacheHotelCidade.getNomePais());
        }

        return lista;
    }
}
