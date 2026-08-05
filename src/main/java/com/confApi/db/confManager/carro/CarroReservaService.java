package com.confApi.db.confManager.carro;

import com.confApi.carros.dto.*;
import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.carro.dto.CancelamentoCarroRequestDTO;
import com.confApi.db.confManager.carro.dto.SalvarReservaCarroRequestDTO;
import com.confApi.db.confManager.carro.dto.SalvarReservaCarroResponseDTO;
import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.db.confManager.sistema.Sistema;
import com.confApi.hub.carro.HubCarroClient;
import com.confApi.model.RecebimentoModel;
import com.confApi.recebimento.RecebimentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class CarroReservaService {

    private final RestTemplate restTemplate;

    @Autowired
    private ConfAppService confAppService;

    @Autowired
    private RecebimentoService recebimentoService;

    @Autowired
    private HubCarroClient hubCarroClient;

    public CarroReservaService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<CarroReserva> findAllReservas() {
        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<CarroReserva>> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_MANAGER + "carroReserva/findAll",
                            HttpMethod.GET,
                            entity,
                            new ParameterizedTypeReference<List<CarroReserva>>() {
                            }
                    );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            return Collections.emptyList();

        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public CarroReservaOperacaoResponseDTO reservar(CarroCompraModel req) {
        Recebimento recebimento = null;

        try {
            if (req == null || req.getReservaCarro() == null) {
                return CarroReservaOperacaoResponseDTO.erro(
                        "ERRO: Dados da reserva de carro não informados."
                );
            }

            if (req.getRecebimento() == null) {
                return CarroReservaOperacaoResponseDTO.erro(
                        "ERRO: Dados de recebimento não informados."
                );
            }

            System.out.println("Criando recebimento::");

            /*
             * 1. Cria recebimento.
             */
            recebimento = recebimentoService.criarRecebimento(req);

            if (recebimento == null
                    || recebimento.getCodgRecebimento() == null
                    || !Integer.valueOf(1).equals(recebimento.getStatus())) {

                return CarroReservaOperacaoResponseDTO.erro(
                        "ERRO: Não foi possível efetuar o pagamento, tente novamente mais tarde."
                );
            }

            /*
             * Mantém o ID do recebimento sem perder os valores calculados no front.
             * O recebimento do front já contém o total da reserva com extras.
             */
            RecebimentoModel recebimentoRequest = req.getRecebimento();

            if (recebimentoRequest == null) {
                recebimentoRequest = new RecebimentoModel();
            }

            recebimentoRequest.setCodgRecebimento(recebimento.getCodgRecebimento());

            if (recebimentoRequest.getValorPagamento() == null
                    || recebimentoRequest.getValorPagamento() <= 0) {

                recebimentoRequest.setValorPagamento(recebimento.getValrRecebimento());
            }

            if (recebimentoRequest.getValorEntrada() == null
                    || recebimentoRequest.getValorEntrada() <= 0) {

                recebimentoRequest.setValorEntrada(recebimento.getValrEntrada());
            }

            req.setRecebimento(recebimentoRequest);

            System.out.println("Reserva no fornecedor::");

            /*
             * 2. Reserva/emite no fornecedor.
             * HubCarroClient continua retornando o DTO do fornecedor internamente.
             */
            List<EmitirCarroResponseDTO> emissoes =
                    hubCarroClient.reservar(req.getReservaCarro());

            if (emissoes == null || emissoes.isEmpty() || temErroEmissao(emissoes)) {
                recebimentoService.cancelarRecebimento(recebimento);

                return CarroReservaOperacaoResponseDTO.erro(
                        "ERRO: Pagamento cancelado. Não foi possível concluir a reserva de carro no fornecedor."
                );
            }

            EmitirCarroResponseDTO emissao = obterPrimeiraEmissaoValida(emissoes);

            if (emissao == null) {
                recebimentoService.cancelarRecebimento(recebimento);

                return CarroReservaOperacaoResponseDTO.erro(
                        "ERRO: Pagamento cancelado. A emissão não retornou uma resposta válida."
                );
            }

            /*
             * 3. Consulta a reserva emitida no fornecedor para recuperar dados completos.
             */
            ReservarCarroResponseDTO reservaConsultada =
                    consultarReservaEmitida(emissao, req);

            if (reservaConsultada == null || reservaConsultada.getReservaCarro() == null) {
                recebimentoService.cancelarRecebimento(recebimento);

                return CarroReservaOperacaoResponseDTO.erro(
                        "ERRO: Pagamento cancelado. Não foi possível consultar a reserva emitida para salvar no banco."
                );
            }

            System.out.println("Salva no Manager::");

            /*
             * 4. Salva no Manager.
             */
            SalvarReservaCarroResponseDTO reservaSalva =
                    salvarReservaCompletaNoManager(reservaConsultada, req);

            if (reservaSalva == null || reservaSalva.getCodgReservaCarro() == null) {
                recebimentoService.cancelarRecebimento(recebimento);

                return CarroReservaOperacaoResponseDTO.erro(
                        "ERRO: Pagamento cancelado. Não foi possível salvar a reserva de carro no banco."
                );
            }

            /*
             * 5. Busca no Manager a reserva salva, que vira o contrato do front.
             */
            CarroReservaDetalheDTO detalhe =
                    buscarReservaDetalheNoManager(reservaSalva.getCodgReservaCarro());

            if (detalhe == null || detalhe.getCarroReserva() == null) {
                return CarroReservaOperacaoResponseDTO.erro(
                        "ERRO: Reserva salva, mas não foi possível carregar os dados salvos."
                );
            }

            return CarroReservaOperacaoResponseDTO.sucesso(detalhe);

        } catch (Exception e) {
            if (recebimento != null && recebimento.getCodgRecebimento() != null) {
                recebimentoService.cancelarRecebimento(recebimento);
            }

            return CarroReservaOperacaoResponseDTO.erro(
                    "ERRO: Não foi possível concluir a reserva de carro. " + e.getMessage()
            );
        }
    }

    private CarroReservaDetalheDTO buscarReservaDetalheNoManager(Integer codgReservaCarro) {
        if (codgReservaCarro == null) {
            return null;
        }

        ConfAppResp token = confAppService.token();

        HttpHeaders headers = defaultHeaders(token.getToken());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<CarroReservaDetalheDTO> response =
                restTemplate.exchange(
                        UrlConfig.URL_CONFIANCA_MANAGER
                                + "carroReserva/detalhe/"
                                + codgReservaCarro,
                        HttpMethod.GET,
                        entity,
                        CarroReservaDetalheDTO.class
                );

        return response.getBody();
    }

    private EmitirCarroResponseDTO obterPrimeiraEmissaoValida(List<EmitirCarroResponseDTO> emissoes) {
        if (emissoes == null || emissoes.isEmpty()) {
            return null;
        }

        for (EmitirCarroResponseDTO emissao : emissoes) {
            if (emissao == null) {
                continue;
            }

            if (Boolean.FALSE.equals(emissao.getSuccess())) {
                continue;
            }

            if (emissao.getError() != null && !emissao.getError().isEmpty()) {
                continue;
            }

            if (!isBlank(emissao.getLocalizador())) {
                return emissao;
            }
        }

        return null;
    }

    public List<CancelarReservaCarroResponseDTO> cancelarReserva(CancelarCarroModel req) {
        try {
            if (req == null || req.getCancelarReservaCarroRequestDTO() == null) {
                return erroCancelamento("ERRO: Dados de cancelamento não informados.");
            }

            List<CancelarReservaCarroResponseDTO> resultadoHub =
                    hubCarroClient.cancelarReserva(req.getCancelarReservaCarroRequestDTO());

            if (resultadoHub == null || resultadoHub.isEmpty()) {
                return erroCancelamento(
                        "ERRO: Não foi possível cancelar a reserva no fornecedor."
                );
            }

            cancelarReservaNoManager(req);

            return resultadoHub;

        } catch (Exception e) {
            return erroCancelamento(
                    "ERRO: Não foi possível cancelar a reserva de carro. "
                            + e.getMessage()
            );
        }
    }

    private void cancelarReservaNoManager(CancelarCarroModel req) {
        CancelamentoCarroRequestDTO dto = new CancelamentoCarroRequestDTO();

        dto.setLocalizador(req.getCancelarReservaCarroRequestDTO() != null ? req.getCancelarReservaCarroRequestDTO().getLocalizador() : null);
        dto.setDescricaoMotivoCancelamento(req.getDescricaoMotivoCancelamento());

        if (req.getUsuario() != null) {
            dto.setCodgUsuarioCancelamento(req.getUsuario().getCodgUsuario());
        }

        ConfAppResp token = confAppService.token();

        HttpHeaders headers = defaultHeaders(token.getToken());

        HttpEntity<CancelamentoCarroRequestDTO> entity =
                new HttpEntity<>(dto, headers);

        restTemplate.exchange(
                UrlConfig.URL_CONFIANCA_MANAGER + "carroReserva/cancelar",
                HttpMethod.PUT,
                entity,
                Void.class
        );
    }

    private List<CancelarReservaCarroResponseDTO> erroCancelamento(String mensagem) {
        CancelarReservaCarroResponseDTO response =
                new CancelarReservaCarroResponseDTO();

        response.setSuccess(false);
        response.setMessage(mensagem);
        response.setReturnMessage(mensagem);

        return Collections.singletonList(response);
    }

    private ReservarCarroResponseDTO consultarReservaEmitida(EmitirCarroResponseDTO emissao, CarroCompraModel req) {
        if (emissao == null || isBlank(emissao.getLocalizador())) {
            return null;
        }

        String token = !isBlank(emissao.getTokenSession())
                ? emissao.getTokenSession()
                : req.getReservaCarro().getToken();

        ConsultarReservaCarroRequestDTO consultarRequest =
                new ConsultarReservaCarroRequestDTO();

        consultarRequest.setLocalizador(emissao.getLocalizador());
        consultarRequest.setToken(token);
        consultarRequest.setSistema(req.getReservaCarro().getSistema());

        List<ReservarCarroResponseDTO> consultas =
                hubCarroClient.consultarReserva(consultarRequest);

        if (consultas == null || consultas.isEmpty()) {
            return null;
        }

        for (ReservarCarroResponseDTO consulta : consultas) {
            if (consulta != null && consulta.getReservaCarro() != null) {
                return consulta;
            }
        }

        return null;
    }

    public CarroReservaOperacaoResponseDTO consultarReserva(ConsultarReservaCarroRequestDTO req) {
        try {
            if (req == null || isBlank(req.getLocalizador())) {
                return CarroReservaOperacaoResponseDTO.erro(
                        "ERRO: Localizador da reserva de carro não informado."
                );
            }

            /*
             * 1. Busca reserva salva no Manager.
             */
            CarroReservaDetalheDTO detalheBanco =
                    buscarReservaDetalhePorLocalizadorNoManager(req.getLocalizador());

            if (detalheBanco == null || detalheBanco.getCarroReserva() == null) {
                return CarroReservaOperacaoResponseDTO.erro(
                        "ERRO: Reserva de carro não encontrada no banco."
                );
            }

            /*
             * 2. Consulta fornecedor.
             * Atenção: se não tiver token, não dá para garantir consulta no fornecedor.
             */
            ReservarCarroResponseDTO reservaFornecedor =
                    consultarReservaFornecedor(req);

            if (reservaFornecedor == null || reservaFornecedor.getReservaCarro() == null) {
                return CarroReservaOperacaoResponseDTO.sucesso(
                        detalheBanco,
                        null,
                        false,
                        false
                );
            }

            /*
             * 3. Monta dados vivos do fornecedor.
             */
            CarroFornecedorDadosDTO fornecedorDTO =
                    montarDadosFornecedor(reservaFornecedor);

            /*
             * 4. Compara status fornecedor x banco.
             */
            boolean statusDiferente =
                    statusDiferenteBancoFornecedor(detalheBanco, fornecedorDTO);

            /*
             * 5. Se diferente, atualiza Manager e recarrega detalhe salvo.
             */
            if (statusDiferente) {
                atualizarStatusReservaNoManager(
                        detalheBanco.getCodgReservaCarro(),
                        fornecedorDTO
                );

                detalheBanco =
                        buscarReservaDetalheNoManager(detalheBanco.getCodgReservaCarro());
            }

            /*
             * 6. Retorna banco + dados vivos do fornecedor.
             */
            return CarroReservaOperacaoResponseDTO.sucesso(
                    detalheBanco,
                    fornecedorDTO,
                    true,
                    statusDiferente
            );

        } catch (Exception e) {
            return CarroReservaOperacaoResponseDTO.erro(
                    "ERRO: Não foi possível consultar a reserva de carro. "
                            + e.getMessage()
            );
        }
    }

    private ReservarCarroResponseDTO consultarReservaFornecedor(ConsultarReservaCarroRequestDTO req) {
        if (req == null || isBlank(req.getLocalizador())) {
            return null;
        }

        /*
         * Se o token for obrigatório no HUB, este ponto precisa estar preenchido.
         * Para reservas abertas pela tela geral, considere salvar tokenSession no banco.
         */
//        if (isBlank(req.getToken())) {
//            return null;
//        }

        List<ReservarCarroResponseDTO> responseFornecedor =
                hubCarroClient.consultarReserva(req);

        System.out.println("responseFornecedor:: " + responseFornecedor);

        if (responseFornecedor == null || responseFornecedor.isEmpty()) {
            return null;
        }

        for (ReservarCarroResponseDTO response : responseFornecedor) {
            if (response == null) {
                continue;
            }

            if (Boolean.FALSE.equals(response.getSuccess())) {
                continue;
            }

            if (response.getError() != null && !response.getError().isEmpty()) {
                continue;
            }

            if (response.getReservaCarro() != null) {
                return response;
            }
        }

        return null;
    }

    private CarroFornecedorDadosDTO montarDadosFornecedor(ReservarCarroResponseDTO fornecedor) {
        CarroFornecedorDadosDTO dto = new CarroFornecedorDadosDTO();

        if (fornecedor == null) {
            return dto;
        }

        CarroBookingHub reservaFornecedor = fornecedor.getReservaCarro();

        if (reservaFornecedor != null) {
            dto.setBookingStatus(reservaFornecedor.getBookingStatus());
            dto.setPagamentoStatus(reservaFornecedor.getPagamentoStatus());

            dto.setStatusReservaFornecedor(
                    mapStatusReservaFornecedor(reservaFornecedor.getBookingStatus())
            );

            dto.setStatusPagamentoFornecedor(
                    mapStatusReservaFornecedor(reservaFornecedor.getPagamentoStatus())
            );

            dto.setTermosECondicoes(reservaFornecedor.getTermosECondicoes());
            dto.setUrlVoucherPT(reservaFornecedor.getUrlVoucherPT());
            dto.setUrlVoucherUS(reservaFornecedor.getUrlVoucherUS());

            dto.setRestricao(reservaFornecedor.getRestricao());
            dto.setRestricaoUS(reservaFornecedor.getRestricaoUS());

            CarroBookingLojaHub lojaRetirada =
                    buscarLojaFornecedor(reservaFornecedor, true);

            CarroBookingLojaHub lojaDevolucao =
                    buscarLojaFornecedor(reservaFornecedor, false);

            dto.setTelefoneLojaRetirada(
                    lojaRetirada != null ? lojaRetirada.getTelefone() : null
            );

            dto.setTelefoneLojaDevolucao(
                    lojaDevolucao != null ? lojaDevolucao.getTelefone() : null
            );


        }

        dto.setRegrasCancelamento(fornecedor.getRegrasCancelamento());
        dto.setUrlTravelFlow(fornecedor.getUrlTravelFlow());
        dto.setPolicies(montarPolicies(fornecedor.getPolicies()));

        return dto;
    }

    private CarroBookingLojaHub buscarLojaFornecedor(CarroBookingHub reservaFornecedor, boolean lojaRetirada) {

        if (reservaFornecedor == null
                || reservaFornecedor.getLojas() == null
                || reservaFornecedor.getLojas().isEmpty()) {
            return null;
        }

        for (CarroBookingLojaHub loja : reservaFornecedor.getLojas()) {
            if (loja == null) {
                continue;
            }

            if (Boolean.valueOf(lojaRetirada).equals(loja.getLojaRetirada())) {
                return loja;
            }
        }

        return null;
    }

    private List<PolicyHub> montarPolicies(List<PolicyHub> policies) {
        List<PolicyHub> result = new ArrayList<>();

        if (policies == null || policies.isEmpty()) {
            return result;
        }

        for (PolicyHub policy : policies) {
            if (policy == null) {
                continue;
            }

            PolicyHub dto = new PolicyHub();
            dto.setTitle(policy.getTitle());
            dto.setDescription(policy.getDescription());

            result.add(dto);
        }

        return result;
    }

    private Integer mapStatusReservaFornecedor(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }

        String statusTratado = status.trim();

        if ("Confirmed".equalsIgnoreCase(statusTratado)
                || "Confirmado".equalsIgnoreCase(statusTratado)) {
            return 1; // Emitido
        }

        if ("Cancelled".equalsIgnoreCase(statusTratado)
                || "Canceled".equalsIgnoreCase(statusTratado)
                || "Cancelado".equalsIgnoreCase(statusTratado)) {
            return 2; // Cancelado
        }

        /*
         * Pending não entra no padrão definido.
         * Se no futuro quiser controlar pendente, defina um código específico.
         */
        return null;
    }

    private boolean statusDiferenteBancoFornecedor(CarroReservaDetalheDTO detalheBanco, CarroFornecedorDadosDTO fornecedor) {
        if (detalheBanco == null
                || detalheBanco.getCarroReserva() == null
                || fornecedor == null) {
            return false;
        }

        boolean statusReservaDiferente = false;
        boolean statusPagamentoFornecedorDiferente = false;

        if (fornecedor.getStatusReservaFornecedor() != null) {
            statusReservaDiferente =
                    !fornecedor.getStatusReservaFornecedor()
                            .equals(detalheBanco.getCarroReserva().getStatusReserva());
        }

        if (fornecedor.getStatusPagamentoFornecedor() != null) {
            statusPagamentoFornecedorDiferente =
                    !fornecedor.getStatusPagamentoFornecedor()
                            .equals(detalheBanco.getCarroReserva().getStatusPagamentoFornecedor());
        }

        return statusReservaDiferente || statusPagamentoFornecedorDiferente;
    }

    private void atualizarStatusReservaNoManager(Integer codgReservaCarro, CarroFornecedorDadosDTO fornecedor) {
        if (codgReservaCarro == null || fornecedor == null) {
            return;
        }

        AtualizarStatusReservaCarroRequestDTO dto =
                new AtualizarStatusReservaCarroRequestDTO();

        dto.setCodgReservaCarro(codgReservaCarro);

        dto.setStatusReserva(fornecedor.getStatusReservaFornecedor());
        dto.setStatusPagamentoFornecedor(fornecedor.getStatusPagamentoFornecedor());

        /*
         * status_pagamento_cliente é interno.
         * Só altera para cancelado quando a reserva do fornecedor estiver cancelada.
         */
        if (Integer.valueOf(2).equals(fornecedor.getStatusReservaFornecedor())) {
            dto.setStatusPagamentoCliente(2);
        }

        dto.setBookingStatusFornecedor(fornecedor.getBookingStatus());
        dto.setPagamentoStatusFornecedor(fornecedor.getPagamentoStatus());

        ConfAppResp token = confAppService.token();

        HttpHeaders headers = defaultHeaders(token.getToken());

        HttpEntity<AtualizarStatusReservaCarroRequestDTO> entity =
                new HttpEntity<>(dto, headers);

        restTemplate.exchange(
                UrlConfig.URL_CONFIANCA_MANAGER + "carroReserva/atualizarStatusFornecedor",
                HttpMethod.PUT,
                entity,
                Void.class
        );
    }

    private CarroReservaDetalheDTO buscarReservaDetalhePorLocalizadorNoManager(String localizador) {
        if (isBlank(localizador)) {
            return null;
        }

        ConfAppResp token = confAppService.token();

        HttpHeaders headers = defaultHeaders(token.getToken());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<CarroReservaDetalheDTO> response =
                restTemplate.exchange(
                        UrlConfig.URL_CONFIANCA_MANAGER
                                + "carroReserva/detalheByLocalizador/"
                                + localizador.trim(),
                        HttpMethod.GET,
                        entity,
                        CarroReservaDetalheDTO.class
                );

        return response.getBody();
    }
    public SalvarReservaCarroResponseDTO salvarReservaCompletaNoManager(ReservarCarroResponseDTO reservaHub, CarroCompraModel carroCompraModel) {

        System.out.println("getValorTaxaExtraBrl:::: " + carroCompraModel.getValorTaxaExtraBrl());

        if (reservaHub == null || reservaHub.getReservaCarro() == null) {
            throw new IllegalArgumentException("Reserva do HUB inválida.");
        }
        if (carroCompraModel == null || carroCompraModel.getReservaCarro() == null) {
            throw new IllegalArgumentException("Request de reserva inválido.");
        }

        ReservarCarroRequestDTO reservarCarroRequestDTO =
                carroCompraModel.getReservaCarro();

        CarroBookingHub reservaCarroHub = reservaHub.getReservaCarro();

        CarroReserva carroReserva = new CarroReserva(reservaHub);
        carroReserva.setUsuario(carroCompraModel.getUsuario());
        carroReserva.setAgencia(carroCompraModel.getAgencia());
        carroReserva.setSistema(
                montarSistemaReferencia(reservarCarroRequestDTO.getSistema())
        );

        if (carroReserva.getStatusReserva() == null) {
            carroReserva.setStatusReserva(1);
        }

        carroReserva.setStatusPagamentoCliente(1);
        carroReserva.setFonte(normalizarFonte(carroCompraModel.getFonte()));

        adicionarRecebimentoNaReserva(carroReserva, carroCompraModel);

        Carro carro = new Carro(reservaHub, null);

        if (carro.getQtdPortas() == null
                && carroCompraModel.getQtdPortas() != null) {

            carro.setQtdPortas(carroCompraModel.getQtdPortas());
        }

        CarroValor carroValor = new CarroValor(reservaCarroHub, null);

        Double valorTotalCobradoBrl = null;

        if (carroCompraModel.getRecebimento() != null) {
            valorTotalCobradoBrl = carroCompraModel.getRecebimento().getValorPagamento();

            if (valorTotalCobradoBrl == null || valorTotalCobradoBrl <= 0) {
                valorTotalCobradoBrl = carroCompraModel.getRecebimento().getValorEntrada();
            }
        }

        if (valorTotalCobradoBrl != null && valorTotalCobradoBrl > 0) {
            carroReserva.setValorTotalReservaNet(valorTotalCobradoBrl);
            carroValor.setValorTotalReservaNetBrl(valorTotalCobradoBrl);
        }

        if (carroCompraModel.getValorTaxaExtraBrl() != null
                && carroCompraModel.getValorTaxaExtraBrl() > 0) {

            carroValor.setValorTaxaExtraBrl(carroCompraModel.getValorTaxaExtraBrl());
        }

        List<CarroCondutor> condutores = new ArrayList<>();

        if (reservaCarroHub.getCondutor() != null) {
            for (CarroBookingCondutorHub condutorHub : reservaCarroHub.getCondutor()) {
                condutores.add(new CarroCondutor(condutorHub, null));
            }
        }

        List<CarroItem> itens = new ArrayList<>();

        if (reservaCarroHub.getItems() != null) {
            for (CarroBookingItemHub itemHub : reservaCarroHub.getItems()) {
                itens.add(new CarroItem(itemHub, null));
            }
        }

        SalvarReservaCarroRequestDTO payload = new SalvarReservaCarroRequestDTO();
        payload.setCarroReserva(carroReserva);
        payload.setCarro(carro);
        payload.setCarroValor(carroValor);
        payload.setCondutores(condutores);
        payload.setItens(itens);


        ConfAppResp token = confAppService.token();

        HttpHeaders headers = defaultHeaders(token.getToken());

        HttpEntity<SalvarReservaCarroRequestDTO> entity =
                new HttpEntity<>(payload, headers);

        ResponseEntity<SalvarReservaCarroResponseDTO> response =
                restTemplate.exchange(
                        UrlConfig.URL_CONFIANCA_MANAGER + "carroReserva/salvarCompleta",
                        HttpMethod.POST,
                        entity,
                        SalvarReservaCarroResponseDTO.class
                );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException(
                    "Erro ao salvar reserva completa no Manager. Status: "
                            + response.getStatusCode()
            );
        }

        return response.getBody();
    }

    private String normalizarFonte(String fonte) {
        if (fonte == null || fonte.trim().isEmpty()) {
            return null;
        }

        return fonte.trim();
    }

    private void adicionarRecebimentoNaReserva(CarroReserva carroReserva, CarroCompraModel carroCompraModel) {
        if (carroReserva == null
                || carroCompraModel == null
                || carroCompraModel.getRecebimento() == null
                || carroCompraModel.getRecebimento().getCodgRecebimento() == null) {
            return;
        }

        Recebimento recebimento = new Recebimento();
        recebimento.setCodgRecebimento(carroCompraModel.getRecebimento().getCodgRecebimento());

        if (carroReserva.getRecebimentos() == null) {
            carroReserva.setRecebimentos(new ArrayList<>());
        }

        carroReserva.getRecebimentos().add(recebimento);
    }

    private boolean temErroEmissao(List<EmitirCarroResponseDTO> emissoes) {
        for (EmitirCarroResponseDTO emissao : emissoes) {
            if (emissao == null) {
                return true;
            }

            if (Boolean.FALSE.equals(emissao.getSuccess())) {
                return true;
            }

            if (emissao.getError() != null && !emissao.getError().isEmpty()) {
                return true;
            }

            if (isBlank(emissao.getLocalizador())) {
                return true;
            }
        }

        return false;
    }

    private Sistema montarSistemaReferencia(SistemaCarroHub sistemaHub) {
        if (sistemaHub == null) {
            return null;
        }

        Integer codgSistema = sistemaHub.getSistema() != null
                ? sistemaHub.getSistema()
                : sistemaHub.getCodigo();

        if (codgSistema == null) {
            return null;
        }

        Sistema sistema = new Sistema();
        sistema.setCodgSistema(codgSistema);

        return sistema;
    }

    private HttpHeaders defaultHeaders(String bearerToken) {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        if (!isBlank(bearerToken)) {
            headers.setBearerAuth(bearerToken);
        }

        return headers;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
