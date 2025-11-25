package com.confApi.db.confManager.hotel.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class HotelRepository {

    private List<HotelResponse> hoteis;

    public HotelRepository() {
        this.hoteis = new ArrayList<>();
        popularHoteis();
    }

    private void popularHoteis() {
        for (int i = 1; i <= 10; i++) {
            HotelResponse hotel1 = new HotelResponse();
            hotel1.setCodigoHotelSistema("HOT001");
            hotel1.setCategoria(generateRandomInt());
            hotel1.setDescricaoCategoria("Luxo");
            hotel1.setClasseSistema("Premium");
            hotel1.setNomeSistema("Sistema A "+i);
            hotel1.setCodigoSistema("SYS001");
            hotel1.setNome("Hotel Grand Palace" +i);
            hotel1.setCodigoIataCidade("GRD");
            hotel1.setDescricao("Hotel de luxo com excelente localização.");
            hotel1.setDescricaoExibicao("Hotel Grand Palace - Luxo e conforto.");
            hotel1.setDescricaoLocalizacao("Localizado no centro da cidade.");
            hotel1.setDescricaoQuartos("Quartos espaçosos e bem equipados.");
            hotel1.setDescricaoAreasLazer("Piscina, spa e academia.");
            hotel1.setDescricaoRefeicoes("Restaurante gourmet e bar.");
            hotel1.setDescricaoInstalacoes("Instalações modernas e confortáveis.");
            hotel1.setDescricaoFornecedor("Fornecedor de alta qualidade.");
            hotel1.setInformacoesEspeciais(List.of("Wi-Fi grátis", "Estacionamento gratuito"));
            hotel1.setEndereco("Rua Exemplo, 123");
            hotel1.setEstado("SP");
            hotel1.setBairro("Quilombo "+i);
            hotel1.setDescricaoCategoriaCurta("Luxo");
            hotel1.setCidade("São Paulo");
            hotel1.setCodigoCidade("SP123");
            hotel1.setPais("Brasil");
            hotel1.setLocalizacao("Latitude: -23.5505, Longitude: -46.6333");
            hotel1.setUrlImagem("https://media.staticontent.com/media/pictures/c7af7a04-352e-48ac-b4c0-ad462519cc30/316x311");
            hotel1.setTelefone("+55 11 1234-5678");
            hotel1.setFax("+55 11 8765-4321");
            hotel1.setEmail("contato@hotelgrandpalace.com.br");
            hotel1.setCep("01000-000");
            hotel1.setCnpj("12.345.678/0001-90");
            hotel1.setMoeda("BRL");
            hotel1.setValorCambio(5.25);
            hotel1.setQuantidadeNoites(3);
            hotel1.setVisivel(true);
            hotel1.setRecomendado(true);
            hotel1.setReservar(true);
            hotel1.setExibirMapa(true);
            hotel1.setExibirDetalhesHotel(true);
            hotel1.setExibirColunaQuartoDisponivel(true);
            hotel1.setCobrarTaxaIr(false);
            hotel1.setDataEntrada(new Date()); // Data atual para teste
            hotel1.setDataSaida(new Date(System.currentTimeMillis() + 86400000L * 3)); // Data 3 dias no futuro
            hotel1.setLatitude(-23.5505f);
            hotel1.setLongitude(-46.6333f);

            // Preenchendo tarifas de exemplo
            TarifaHotel tarifaHotel = new TarifaHotel();
            tarifaHotel.setValorMarkupAplicado(300.0);
            tarifaHotel.setPercentualMarkupAplicado(12.0);
            tarifaHotel.setValorTotalEstadiaComMarkup(3300.0);
            tarifaHotel.setValorTotalEstadiaComMarkupBrl(generateRandomDouble());
            tarifaHotel.setValorTotalEstadiaNet(3000.0);

            tarifaHotel.setPercentualTaxaIss(6.0);
            tarifaHotel.setPercentualTaxaServico(10.0);
            tarifaHotel.setPercentualTaxaExtra(4.0);
            tarifaHotel.setValorTaxaIss(180.0);
            tarifaHotel.setValorTaxaServico(300.0);
            tarifaHotel.setMoeda("BRL");
            hotel1.setTarifasHotel(tarifaHotel);
            hotel1.getAcomodacoes().addAll(gerarAcomodacoesTeste());
            hotel1.getTaxasPoliticas().addAll(gerarTaxasPoliticasTeste());
            hotel1.getServicos().addAll(gerarServicosHotelTeste());
            hotel1.getDetalhesHotel().addAll(gerarDetalhesHotelTeste());
            hotel1.getImagens().addAll(gerarImagensTeste());

            this.hoteis.add(hotel1);
        }
    }

    public List<HotelImagens> gerarImagensTeste() {
        List<HotelImagens> listaImagens = new ArrayList<>();

        HotelImagens imagem1 = new HotelImagens();
        imagem1.setUrlImg("https://media.staticontent.com/media/pictures/c7af7a04-352e-48ac-b4c0-ad462519cc30/316x311");
        imagem1.setNome("Vista do Quarto");
        imagem1.setDescricao("Imagem da vista do quarto com vista para o mar");
        listaImagens.add(imagem1);

        HotelImagens imagem2 = new HotelImagens();
        imagem2.setUrlImg("https://media.staticontent.com/media/pictures/c7af7a04-352e-48ac-b4c0-ad462519cc30/316x311");
        imagem2.setNome("Área da Piscina");
        imagem2.setDescricao("Imagem da piscina ao ar livre com espreguiçadeiras");
        listaImagens.add(imagem2);

        HotelImagens imagem3 = new HotelImagens();
        imagem3.setUrlImg("https://media.staticontent.com/media/pictures/c7af7a04-352e-48ac-b4c0-ad462519cc30/316x311");
        imagem3.setNome("Restaurante");
        imagem3.setDescricao("Imagem do restaurante gourmet do hotel");
        listaImagens.add(imagem3);

        return listaImagens;
    }

    public  double generateRandomDouble() {
        Random random = new Random();
        // Gera um número double entre 500 e 1000
        return 500 + (1000 - 500) * random.nextDouble();
    }



    public  int generateRandomInt() {
        Random random = new Random();
        // Gera um número inteiro entre 1 e 5 (inclusive)
        return random.nextInt(5) + 1;
    }

    public List<HotelDetalhesHotel> gerarDetalhesHotelTeste() {
        List<HotelDetalhesHotel> listaDetalhesHotel = new ArrayList<>();

        HotelDetalhesHotel detalhe1 = new HotelDetalhesHotel();
        detalhe1.setCodgReferencia("DT001");
        detalhe1.setNome("Piscina");
        detalhe1.setDescricao("Piscina externa aquecida com vista para o mar");
        listaDetalhesHotel.add(detalhe1);

        HotelDetalhesHotel detalhe2 = new HotelDetalhesHotel();
        detalhe2.setCodgReferencia("DT002");
        detalhe2.setNome("Academia");
        detalhe2.setDescricao("Academia de última geração aberta 24 horas");
        listaDetalhesHotel.add(detalhe2);

        HotelDetalhesHotel detalhe3 = new HotelDetalhesHotel();
        detalhe3.setCodgReferencia("DT003");
        detalhe3.setNome("Spa");
        detalhe3.setDescricao("Serviço completo de spa com massagens e tratamentos");
        listaDetalhesHotel.add(detalhe3);

        return listaDetalhesHotel;
    }

    public List<HotelServicoHotel> gerarServicosHotelTeste() {
        List<HotelServicoHotel> listaServicosHotel = new ArrayList<>();

        HotelServicoHotel servico1 = new HotelServicoHotel();
        servico1.setCodgReferencia("SV001");
        servico1.setNomeServico("Wi-Fi Premium");
        servico1.setDescricaoServico("Acesso à internet de alta velocidade");
        servico1.setValor(30.0);
        listaServicosHotel.add(servico1);

        HotelServicoHotel servico2 = new HotelServicoHotel();
        servico2.setCodgReferencia("SV002");
        servico2.setNomeServico("Transfer Aeroporto");
        servico2.setDescricaoServico("Serviço de transporte entre o hotel e o aeroporto");
        servico2.setValor(70.0);
        listaServicosHotel.add(servico2);

        HotelServicoHotel servico3 = new HotelServicoHotel();
        servico3.setCodgReferencia("SV003");
        servico3.setNomeServico("Café da Manhã");
        servico3.setDescricaoServico("Buffet completo de café da manhã");
        servico3.setValor(40.0);
        listaServicosHotel.add(servico3);

        return listaServicosHotel;
    }

    public List<HotelAcomodacao> gerarAcomodacoesTeste() {
        List<HotelAcomodacao> listaAcomodacoes = new ArrayList<>();

        HotelAcomodacao acomodacao1 = new HotelAcomodacao();
        /*acomodacao1.setSiglaTipoAcomodacao("STD");
        acomodacao1.setCodgAcomodacao("001");*/
        acomodacao1.setNomeQuarto("Quarto Standard");
        acomodacao1.setNomeQuartoExtenso("Quarto Standard com vista para o jardim");
        acomodacao1.setVagasDisponiveis(5);
        acomodacao1.setIsSelecionado(true);
        acomodacao1.setDescricaoTipoCama("Cama de casal");
        acomodacao1.setDescricaoTipoAcomodacao("Acomodação simples");
        acomodacao1.setVagasDisponiveis(3);
        acomodacao1.setIsPrePagamento(true);
        acomodacao1.setIsNaoReembolsavel(false);
        acomodacao1.setRegime("Café da manha");
        TarifaHotel tarifa1 = new TarifaHotel();
        tarifa1.setValorMarkupAplicado(200.0);
        tarifa1.setPercentualMarkupAplicado(10.0);
        tarifa1.setValorTotalEstadiaComMarkup(2200.0);
        tarifa1.setValorTotalEstadiaComMarkupBrl(11500.0);
        tarifa1.setValorTotalEstadiaNet(2000.0);
        tarifa1.setPercentualTaxaIss(5.0);
        tarifa1.setPercentualTaxaServico(8.0);
        tarifa1.setPercentualTaxaExtra(3.0);
        tarifa1.setValorTaxaIss(110.0);
        tarifa1.setValorTaxaServico(180.0);
        tarifa1.setMoeda("BRL");
        acomodacao1.setTarifaHotel(tarifa1);
        acomodacao1.getRegrasCancelamentoAcomodacao().addAll(gerarRegrasCancelamentoTeste());
        // Adicionar tarifaHotel e regrasCancelamentoAcomodacao conforme necessário
        listaAcomodacoes.add(acomodacao1);

        for (int i = 0; i < 10; i++) {

            HotelAcomodacao acomodacao2 = new HotelAcomodacao();
        /*acomodacao2.setSiglaTipoAcomodacao("DEL");
        acomodacao2.setCodgAcomodacao("002");*/
            acomodacao2.setNomeQuarto("Quarto Deluxe "+i);
            acomodacao2.setNomeQuartoExtenso("Quarto Deluxe com vista para o mar");
            acomodacao2.setVagasDisponiveis(3);
            acomodacao2.setIsSelecionado(false);
            acomodacao2.setDescricaoTipoCama("Cama king size");
            acomodacao2.setDescricaoTipoAcomodacao("Acomodação de luxo");
            acomodacao2.setVagasDisponiveis(2);
            acomodacao2.setIsPrePagamento(false);
            acomodacao2.setIsNaoReembolsavel(true);
            acomodacao2.setRegime("Café da manha");
            // Adicionar tarifaHotel e regrasCancelamentoAcomodacao conforme necessário
            acomodacao2.setTarifaHotel(tarifa1);
            acomodacao2.getRegrasCancelamentoAcomodacao().addAll(gerarRegrasCancelamentoTeste());
            listaAcomodacoes.add(acomodacao2);
        }

        // Adicionar mais acomodações conforme necessário para o teste
        return listaAcomodacoes;
    }

    public List<HotelRegrasCancelamento> gerarRegrasCancelamentoTeste() {
        List<HotelRegrasCancelamento> listaRegrasCancelamento = new ArrayList<>();

        HotelRegrasCancelamento regra1 = new HotelRegrasCancelamento();
        regra1.setCodgReferencia("RC001");
        regra1.setNomeRegra("Cancelamento Grátis");
        regra1.setDescricaoRegra("Cancelamento permitido até 24 horas antes da chegada sem custos.");
        regra1.setIsReembolsavel(true);
        regra1.setValorMulta(0.0);
        listaRegrasCancelamento.add(regra1);

        HotelRegrasCancelamento regra2 = new HotelRegrasCancelamento();
        regra2.setCodgReferencia("RC002");
        regra2.setNomeRegra("Cancelamento com Multa");
        regra2.setDescricaoRegra("Cancelamento com multa de 50% se realizado até 48 horas antes do check-in.");
        regra2.setIsReembolsavel(false);
        regra2.setValorMulta(500.0);
        listaRegrasCancelamento.add(regra2);

        HotelRegrasCancelamento regra3 = new HotelRegrasCancelamento();
        regra3.setCodgReferencia("RC003");
        regra3.setNomeRegra("Cancelamento Não Reembolsável");
        regra3.setDescricaoRegra("Cancelamento não permitido após a reserva, não reembolsável.");
        regra3.setIsReembolsavel(false);
        regra3.setValorMulta(1000.0);
        listaRegrasCancelamento.add(regra3);

        return listaRegrasCancelamento;
    }



    public List<HotelTaxasPoliticas> gerarTaxasPoliticasTeste() {
        List<HotelTaxasPoliticas> listaTaxasPoliticas = new ArrayList<>();

        HotelTaxasPoliticas taxa1 = new HotelTaxasPoliticas();
        taxa1.setCodgReferencia("TX001");
        taxa1.setNome("Taxa de Serviço");
        taxa1.setDescricao("Taxa adicional de 10% sobre o valor total");
        taxa1.setValor(100.0);
        listaTaxasPoliticas.add(taxa1);

        HotelTaxasPoliticas taxa2 = new HotelTaxasPoliticas();
        taxa2.setCodgReferencia("TX002");
        taxa2.setNome("Taxa de Limpeza");
        taxa2.setDescricao("Cobrança única de limpeza do quarto");
        taxa2.setValor(50.0);
        listaTaxasPoliticas.add(taxa2);

        HotelTaxasPoliticas taxa3 = new HotelTaxasPoliticas();
        taxa3.setCodgReferencia("TX003");
        taxa3.setNome("Taxa de Turismo");
        taxa3.setDescricao("Cobrança obrigatória para o fundo de turismo local");
        taxa3.setValor(25.0);
        listaTaxasPoliticas.add(taxa3);

        return listaTaxasPoliticas;
    }

    public List<HotelResponse> getHoteis() {
        return hoteis;
    }
}

