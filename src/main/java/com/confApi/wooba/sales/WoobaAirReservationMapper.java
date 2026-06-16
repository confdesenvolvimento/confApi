package com.confApi.wooba.sales;

import com.confApi.db.confManager.aeroporto.Aeroporto;
import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.db.confManager.bilhete.BilheteAereo;
import com.confApi.db.confManager.companhiaAerea.CompanhiaAerea;
import com.confApi.db.confManager.formaPagamento.FormaPagamento;
import com.confApi.db.confManager.passageiro.Passageiro;
import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.db.confManager.reservaValor.ReservaValor;
import com.confApi.db.confManager.sistema.Sistema;
import com.confApi.db.confManager.trecho.Trecho;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.db.confManager.voo.Voo;
import com.confApi.wooba.sales.dto.WoobaSalesDetailsResponse;
import com.confApi.wooba.webhook.WoobaWebhookRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

@Component
public class WoobaAirReservationMapper {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Sao_Paulo");

    private static final List<DateTimeFormatter> DATE_TIME_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
    );

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    );

    public ReservaAereo toReservaAereo(WoobaSalesDetailsResponse details, WoobaWebhookRequest webhook) {
        if (details == null || details.getTransaction() == null || details.getTransaction().isNull()) {
            throw new IllegalArgumentException("Details da Wooba sem Transaction.");
        }

        JsonNode transaction = details.getTransaction();
        JsonNode header = transaction.path("Header");
        JsonNode context = transaction.path("Context");
        JsonNode productDetail = transaction.path("ProductDetail");
        JsonNode airDetail = resolveAirDetail(productDetail);
        Integer woobaStatus = intValue(header, "TransactionState");
        String statusDescription = text(header, "TransactionStateDescription");
        Date lastUpdate = parseDate(text(header, "LastUpdate"));
        Date issueDate = parseDate(text(airDetail, "IssueDate"));

        ReservaAereo reserva = new ReservaAereo();
        reserva.setLocalizador(firstNonBlank(text(header, "Locator"), webhook != null ? webhook.getLocator() : null));
        reserva.setStatus(mapManagerStatus(woobaStatus, statusDescription));
        reserva.setDataCriacao(firstDate(
                parseDate(text(productDetail, "InsertDate")),
                lastUpdate,
                webhook != null ? parseDate(webhook.getLastUpdate()) : null
        ));
        reserva.setDataLimiteEmissao(firstDate(
                parseDate(text(airDetail, "Deadline")),
                issueDate,
                lastUpdate,
                reserva.getDataCriacao()
        ));

        if (isIssued(woobaStatus, statusDescription) || (isCanceled(woobaStatus, statusDescription) && issueDate != null)) {
            reserva.setDataEmissao(firstDate(issueDate, lastUpdate));
        }
        if (isCanceled(woobaStatus, statusDescription)) {
            reserva.setDataCancelamento(lastUpdate);
            reserva.setDescMotivoCancelamento(statusDescription);
        }

        reserva.setRegraReserva(buildRegraReserva(header, airDetail, transaction.path("Links")));
        reserva.setFonte(2);
        reserva.setCodgSistema(new Sistema(2));
        CompanhiaAerea companhiaReserva = mapCompanhiaAerea(airDetail);
        reserva.setCodgCompanhiaAerea(companhiaReserva);
        reserva.setCodgAgencia(mapAgencia(context.path("Agency")));
        reserva.setCodgUsuarioCriacao(mapUsuario(transaction.path("User")));
        reserva.setPassageiros(mapPassageiros(airDetail, header, issueDate, lastUpdate, woobaStatus, statusDescription));
        reserva.setTrechos(mapTrechos(airDetail.path("Flights"), woobaStatus, statusDescription, companhiaReserva));
        reserva.setRecebimentos(mapRecebimentos(transaction.path("Payments"), isCanceled(woobaStatus, statusDescription)));
        reserva.setValorTotalReserva(resolveValorTotal(airDetail.path("MonetaryDetail")));

        return reserva;
    }

    private JsonNode resolveAirDetail(JsonNode productDetail) {
        JsonNode airReservationDetail = productDetail.path("AirReservationDetail");
        if (airReservationDetail != null && airReservationDetail.isObject()) {
            return airReservationDetail;
        }

        JsonNode airTicketDetail = productDetail.path("AirTicketDetail");
        if (airTicketDetail != null && airTicketDetail.isObject()) {
            return airTicketDetail;
        }

        return airReservationDetail;
    }

    private CompanhiaAerea mapCompanhiaAerea(JsonNode airDetail) {
        String iata = WoobaAirlineCodeNormalizer.canonicalIata(text(airDetail.path("Airline"), "Code"));
        String nome = firstNonBlank(
                text(airDetail.path("Airline"), "Name"),
                text(airDetail.path("Airline"), "Description")
        );

        if (isBlank(iata) && airDetail.path("Flights").isArray() && airDetail.path("Flights").size() > 0) {
            JsonNode firstFlight = airDetail.path("Flights").get(0);
            iata = WoobaAirlineCodeNormalizer.canonicalIata(text(firstFlight.path("MarketingAirline"), "Code"));
        }

        CompanhiaAerea cia = new CompanhiaAerea();
        cia.setIataCia(iata);
        cia.setNomeCia(nome);
        return isBlank(iata) && isBlank(nome) ? null : cia;
    }

    private Agencia mapAgencia(JsonNode agencyNode) {
        if (agencyNode == null || agencyNode.isMissingNode() || agencyNode.isNull()) {
            return null;
        }

        Agencia agencia = new Agencia();
        agencia.setIdWoobaAgencia(intValue(agencyNode, "Id"));
        agencia.setNomeAgencia(text(agencyNode, "Name"));
        agencia.setCnpj(text(agencyNode.path("Document"), "Number"));
        agencia.setCodgSistemaBackOffice(text(agencyNode.path("BackofficeDetail"), "Code"));
        return agencia;
    }

    private Usuario mapUsuario(JsonNode userNode) {
        if (userNode == null || userNode.isMissingNode() || userNode.isNull()) {
            return null;
        }

        Usuario usuario = new Usuario();
        usuario.setCodigoWooba(intValue(userNode, "Id"));
        usuario.setNomeCompleto(text(userNode, "Name"));
        usuario.setLoginUsuario(text(userNode, "Username"));
        usuario.setEmail(text(userNode, "Email"));
        usuario.setCpf(text(userNode.path("Document"), "Number"));
        return usuario;
    }

    private List<Recebimento> mapRecebimentos(JsonNode paymentsNode, boolean transacaoCancelada) {
        List<Recebimento> recebimentos = new ArrayList<>();
        if (paymentsNode == null || !paymentsNode.isArray()) {
            return recebimentos;
        }

        for (JsonNode paymentNode : paymentsNode) {
            if (!isCustomerPayment(paymentNode)) {
                continue;
            }

            Recebimento recebimento = new Recebimento();
            Double valor = firstDouble(
                    doubleValue(paymentNode, "TransactionAmount"),
                    doubleValue(paymentNode, "Amount")
            );
            Integer parcelas = defaultPaymentInstallments(paymentNode);
            JsonNode creditCardNode = paymentNode.path("CreditCard");
            boolean pagamentoCancelado = isPagamentoCancelado(paymentNode, transacaoCancelada);

            recebimento.setValrRecebimento(valor);
            recebimento.setStatus(mapStatusRecebimento(
                    intValue(paymentNode, "PaymentState"),
                    text(paymentNode, "PaymentStateDescription"),
                    pagamentoCancelado
            ));
            recebimento.setDataRecebimento(parseDate(text(paymentNode, "Date")));
            recebimento.setCodgFormaPagto(mapFormaPagamento(paymentNode));
            recebimento.setQtdeParcela(parcelas);
            recebimento.setValrEntrada(0.0);
            recebimento.setValrPrimeiraParcela(firstDouble(
                    doubleValue(creditCardNode, "FirstInstallmentAmount"),
                    parcelValue(valor, parcelas)
            ));
            recebimento.setValrDemaisParcela(firstDouble(
                    doubleValue(creditCardNode, "OtherInstallmentsAmount"),
                    parcelValue(valor, parcelas)
            ));
            recebimento.setCodgTransacao(firstNonBlank(text(paymentNode, "MerchantTransactionId"), text(paymentNode, "ProviderTransactionId")));
            recebimento.setOrderGatewayCartao(validPaymentId(text(paymentNode, "Id")));
            recebimento.setValrCancelado(pagamentoCancelado ? valor : 0.0);
            recebimento.setMensagem(buildPagamentoMensagem(paymentNode));
            mapCreditCard(creditCardNode, recebimento);

            recebimentos.add(recebimento);
        }

        return recebimentos;
    }

    private String validPaymentId(String paymentId) {
        if (isBlank(paymentId) || "0".equals(paymentId.trim())) {
            return null;
        }
        return paymentId.trim();
    }

    private boolean isCustomerPayment(JsonNode paymentNode) {
        Integer paymentType = intValue(paymentNode, "PaymentType");
        String description = text(paymentNode, "PaymentTypeDescription");
        return Integer.valueOf(1).equals(paymentType) || "Customer".equalsIgnoreCase(defaultString(description));
    }

    private FormaPagamento mapFormaPagamento(JsonNode paymentNode) {
        FormaPagamento formaPagamento = new FormaPagamento();
        formaPagamento.setNomeFormaPagto(mapFormaPagamentoNome(text(paymentNode, "PaymentFormDescription")));
        return formaPagamento;
    }

    private String mapFormaPagamentoNome(String woobaPaymentFormDescription) {
        if (isBlank(woobaPaymentFormDescription)) {
            return null;
        }

        String normalized = woobaPaymentFormDescription.trim();
        if ("Invoice".equalsIgnoreCase(normalized) || "InvoicedPayment".equalsIgnoreCase(normalized)) {
            return "FATURADO";
        }
        if ("CreditCard".equalsIgnoreCase(normalized) || "Credit Card".equalsIgnoreCase(normalized)) {
            return "CARTAO";
        }
        if ("Pix".equalsIgnoreCase(normalized)) {
            return "PIX";
        }
        return normalized;
    }

    private Integer defaultPaymentInstallments(JsonNode paymentNode) {
        Integer installments = intValue(paymentNode, "Installments");
        if (installments != null && installments > 0) {
            return installments;
        }

        JsonNode creditCard = paymentNode.path("CreditCard");
        installments = intValue(creditCard, "Installments");
        return installments != null && installments > 0 ? installments : 1;
    }

    private Double parcelValue(Double value, Integer installments) {
        if (value == null) {
            return null;
        }
        if (installments == null || installments <= 1) {
            return value;
        }
        return value / installments;
    }

    private Integer mapStatusRecebimento(Integer paymentState, String paymentStateDescription, boolean pagamentoCancelado) {
        if (pagamentoCancelado) {
            return 0;
        }
        if (Integer.valueOf(4).equals(paymentState) || containsIgnoreCase(paymentStateDescription, "Confirmed")) {
            return 1;
        }
        if (isPaymentCanceled(paymentStateDescription) || Integer.valueOf(5).equals(paymentState)) {
            return 0;
        }
        return 2;
    }

    private boolean isPagamentoCancelado(JsonNode paymentNode, boolean transacaoCancelada) {
        return transacaoCancelada
                || isPaymentCanceled(text(paymentNode, "PaymentStateDescription"))
                || Integer.valueOf(5).equals(intValue(paymentNode, "PaymentState"))
                || hasCanceledPaymentLink(paymentNode.path("Links"));
    }

    private boolean isPaymentCanceled(String paymentStateDescription) {
        return containsIgnoreCase(paymentStateDescription, "Canceled")
                || containsIgnoreCase(paymentStateDescription, "Cancelled");
    }

    private boolean hasCanceledPaymentLink(JsonNode links) {
        if (links == null || !links.isArray()) {
            return false;
        }
        for (JsonNode link : links) {
            if (Integer.valueOf(5).equals(intValue(link, "TransactionState"))
                    || isPaymentCanceled(text(link, "TransactionStateDescription"))) {
                return true;
            }
        }
        return false;
    }

    private void mapCreditCard(JsonNode creditCardNode, Recebimento recebimento) {
        if (creditCardNode == null || creditCardNode.isMissingNode() || creditCardNode.isNull()) {
            return;
        }

        recebimento.setNumrCartao(firstNonBlank(
                text(creditCardNode, "Number"),
                text(creditCardNode, "MaskedNumber"),
                text(creditCardNode, "CardNumber")
        ));
        recebimento.setValidadeCartao(firstNonBlank(
                text(creditCardNode, "ExpirationDate"),
                text(creditCardNode, "Expiration"),
                text(creditCardNode, "Validity")
        ));
        recebimento.setTitularCartao(firstNonBlank(
                text(creditCardNode, "HolderName"),
                text(creditCardNode, "Holder"),
                text(creditCardNode, "Name")
        ));
        recebimento.setCodgAutCartao(firstNonBlank(
                text(creditCardNode, "AuthorizationCode"),
                text(creditCardNode, "AuthorizationNumber")
        ));
    }

    private String buildPagamentoMensagem(JsonNode paymentNode) {
        StringBuilder mensagem = new StringBuilder();
        JsonNode creditCardNode = paymentNode.path("CreditCard");
        JsonNode posNode = creditCardNode.path("POS");
        appendRegra(mensagem, "WoobaPaymentId", text(paymentNode, "Id"));
        appendRegra(mensagem, "WoobaPaymentType", text(paymentNode, "PaymentTypeDescription"));
        appendRegra(mensagem, "WoobaPaymentForm", text(paymentNode, "PaymentFormDescription"));
        appendRegra(mensagem, "WoobaPaymentState", text(paymentNode, "PaymentStateDescription"));
        appendRegra(mensagem, "WoobaPaymentCurrency", text(paymentNode, "CurrencyCode"));
        appendRegra(mensagem, "WoobaMerchantTransactionId", text(paymentNode, "MerchantTransactionId"));
        appendRegra(mensagem, "WoobaProviderTransactionId", text(paymentNode, "ProviderTransactionId"));
        appendRegra(mensagem, "WoobaPaymentTicket", firstPaymentLinkTicket(paymentNode.path("Links")));
        appendRegra(mensagem, "WoobaPaymentUniqueId", firstPaymentLinkUniqueId(paymentNode.path("Links")));
        appendRegra(mensagem, "WoobaRegisterCode", text(paymentNode.path("Register").path("BackofficeDetail"), "Code"));
        appendRegra(mensagem, "WoobaRegisterName", text(paymentNode.path("Register"), "Name"));
        appendRegra(mensagem, "WoobaCardBrandCode", text(creditCardNode, "BrandCode"));
        appendRegra(mensagem, "WoobaCardBrand", text(creditCardNode, "BrandDescription"));
        appendRegra(mensagem, "WoobaCardProofOfSale", text(creditCardNode, "ProofOfSale"));
        appendRegra(mensagem, "WoobaCardPaymentAccountReference", text(creditCardNode, "PaymentAccountReference"));
        appendRegra(mensagem, "WoobaCardPosId", text(posNode, "Id"));
        appendRegra(mensagem, "WoobaCardPosName", text(posNode, "Name"));
        appendRegra(mensagem, "WoobaCardPosMerchantId", text(posNode, "MerchantId"));
        return mensagem.length() == 0 ? null : mensagem.toString();
    }

    private String firstPaymentLinkTicket(JsonNode links) {
        JsonNode firstLink = firstArrayItem(links);
        return firstLink == null ? null : text(firstLink, "Ticket");
    }

    private String firstPaymentLinkUniqueId(JsonNode links) {
        JsonNode firstLink = firstArrayItem(links);
        return firstLink == null ? null : text(firstLink, "UniqueId");
    }

    private JsonNode firstArrayItem(JsonNode node) {
        if (node == null || !node.isArray() || node.size() == 0) {
            return null;
        }
        return node.get(0);
    }

    private List<Passageiro> mapPassageiros(JsonNode airDetail,
                                            JsonNode header,
                                            Date issueDate,
                                            Date lastUpdate,
                                            Integer woobaStatus,
                                            String statusDescription) {
        List<Passageiro> passageiros = new ArrayList<>();
        JsonNode passengersNode = airDetail.path("Passengers");
        JsonNode reservationMonetaryDetail = airDetail.path("MonetaryDetail");
        if (passengersNode == null || !passengersNode.isArray()) {
            return passageiros;
        }

        int index = 0;
        for (JsonNode passengerNode : passengersNode) {
            Passageiro passageiro = new Passageiro();
            JsonNode personNode = passengerNode.path("Person");

            passageiro.setNomePassageiro(text(personNode, "FirstName"));
            passageiro.setMeioNomePassageiro(text(personNode, "MiddleName"));
            passageiro.setSobrenomePassageiro(text(personNode, "Surname"));
            passageiro.setSexo(mapSexo(personNode));
            passageiro.setTipoPassageiro(mapTipoPassageiro(text(passengerNode, "TypeCode")));
            passageiro.setEmail(text(passengerNode, "Email"));
            passageiro.setCpf(text(personNode.path("Document"), "Number"));
            passageiro.setNumrDocumento(firstNonBlank(
                    text(personNode.path("Document"), "Number"),
                    text(personNode.path("IdentityDocument"), "Number")
            ));
            passageiro.setIdPassageiroCia(text(passengerNode, "ProviderId"));
            passageiro.setTelefone(mapTelefone(passengerNode.path("Phone")));
            passageiro.setCelular(passageiro.getTelefone());
            passageiro.setDataNascimento(firstDate(
                    parseDate(text(personNode, "BirthDate")),
                    parseDate(text(personNode, "Birthday")),
                    parseDate(text(personNode, "DateOfBirth"))
            ));
            passageiro.setBilhetes(mapBilhetes(passengerNode, airDetail, header, issueDate, lastUpdate, woobaStatus, statusDescription, index));

            JsonNode passengerMonetaryDetail = passengerNode.path("MonetaryDetail");
            ReservaValor reservaValor = mapReservaValor(
                    hasMonetaryInfo(passengerMonetaryDetail) ? passengerMonetaryDetail : (index == 0 ? reservationMonetaryDetail : null)
            );
            if (reservaValor != null) {
                List<ReservaValor> valores = new ArrayList<>();
                valores.add(reservaValor);
                passageiro.setReservaValores(valores);
            }

            passageiros.add(passageiro);
            index++;
        }

        return passageiros;
    }

    private List<BilheteAereo> mapBilhetes(JsonNode passengerNode,
                                           JsonNode airDetail,
                                           JsonNode header,
                                           Date issueDate,
                                           Date lastUpdate,
                                           Integer woobaStatus,
                                           String statusDescription,
                                           int passengerIndex) {
        List<String> tickets = new ArrayList<>();
        addTicket(tickets, text(passengerNode, "Ticket"));
        if (tickets.isEmpty()) {
            addTicket(tickets, text(header, "Ticket"));
        }

        if (passengerIndex == 0 && airDetail.path("ConjunctiveTicket").isArray()) {
            for (JsonNode conjunctiveTicket : airDetail.path("ConjunctiveTicket")) {
                addTicket(tickets, conjunctiveTicket.asText());
            }
        }

        if (tickets.isEmpty()) {
            return null;
        }

        List<BilheteAereo> bilhetes = new ArrayList<>();
        for (String ticket : tickets) {
            BilheteAereo bilhete = new BilheteAereo();
            bilhete.setNumrBilhete(ticket);
            bilhete.setStatus(mapStatusBilhete(woobaStatus, statusDescription));
            bilhete.setDataEmissao(firstDate(issueDate, isIssued(woobaStatus, statusDescription) ? lastUpdate : null));
            bilhete.setDataCancelamento(isCanceled(woobaStatus, statusDescription) ? lastUpdate : null);
            bilhetes.add(bilhete);
        }

        return bilhetes;
    }

    private void addTicket(List<String> tickets, String ticket) {
        if (isBlank(ticket)) {
            return;
        }

        String normalized = ticket.trim();
        if (!tickets.contains(normalized)) {
            tickets.add(normalized);
        }
    }

    private List<Trecho> mapTrechos(JsonNode flightsNode,
                                    Integer woobaStatus,
                                    String statusDescription,
                                    CompanhiaAerea companhiaReserva) {
        List<Trecho> trechos = new ArrayList<>();
        if (flightsNode == null || !flightsNode.isArray()) {
            return trechos;
        }

        for (JsonNode flightNode : flightsNode) {
            CompanhiaAerea cia = firstCompanhia(
                    mapCompanhiaAereaFromFlight(flightNode.path("MarketingAirline")),
                    companhiaReserva
            );
            CompanhiaAerea ciaOperada = firstCompanhia(
                    mapCompanhiaAereaFromFlight(flightNode.path("OperationAirline")),
                    cia
            );
            Aeroporto origem = new Aeroporto(text(flightNode, "DepartureAirportCode"));
            Aeroporto destino = new Aeroporto(text(flightNode, "ArrivalAirportCode"));
            Date dataPartida = parseFlightDateTime(text(flightNode, "DepartureDate"), text(flightNode, "DepartureTime"));
            Date dataChegada = parseFlightDateTime(text(flightNode, "ArrivalDate"), text(flightNode, "ArrivalTime"));

            Voo voo = new Voo();
            voo.setCodgCompanhiaAerea(cia);
            voo.setCodgCompanhiaAereaOperada(ciaOperada);
            voo.setNumeroVoo(text(flightNode, "FlightNumber"));
            voo.setCodgAeroportoOrigem(origem);
            voo.setCodgAeroportoDestino(destino);
            voo.setBaseTarifa(text(flightNode, "FareBasisCode"));
            voo.setClasseTarifa(text(flightNode, "ClassOfService"));
            voo.setAeronave(text(flightNode, "EquipmentType"));
            voo.setFamilia(text(flightNode.path("Brand"), "Name"));
            voo.setCodgFamilia(text(flightNode.path("Brand"), "Code"));
            voo.setDataHoraPartida(toTimestamp(dataPartida));
            voo.setDataHoraChegada(toTimestamp(dataChegada));
            voo.setQtdBagagem(defaultInt(intValue(flightNode.path("Baggage"), "Quantity")));
            voo.setPesoBagagem(defaultDouble(doubleValue(flightNode.path("Baggage"), "MaximumWeight")));
            voo.setLocalizadorCia(text(flightNode, "AirlineLocator"));
            voo.setStatusVoo(mapStatusVoo(woobaStatus, statusDescription));
            voo.setFlagCodeShare(Boolean.TRUE.equals(booleanValue(flightNode, "CodeShare")) ? 1 : 0);

            Trecho trecho = new Trecho();
            trecho.setCodgCompanhiaAerea(cia);
            trecho.setCodgAeroportoOrigem(origem);
            trecho.setCodgAeroportoDestino(destino);
            trecho.setDataPartida(dataPartida);
            trecho.setDataChegada(dataChegada);
            trecho.setVoos(List.of(voo));

            trechos.add(trecho);
        }

        return trechos;
    }

    private CompanhiaAerea firstCompanhia(CompanhiaAerea primary, CompanhiaAerea fallback) {
        if (primary != null && !isBlank(primary.getIataCia())) {
            return primary;
        }
        return fallback;
    }

    private CompanhiaAerea mapCompanhiaAereaFromFlight(JsonNode airlineNode) {
        if (airlineNode == null || airlineNode.isMissingNode() || airlineNode.isNull()) {
            return null;
        }

        String code = WoobaAirlineCodeNormalizer.canonicalIata(text(airlineNode, "Code"));
        String name = firstNonBlank(text(airlineNode, "Name"), text(airlineNode, "Description"));
        if (isBlank(code) && isBlank(name)) {
            return null;
        }

        CompanhiaAerea companhiaAerea = new CompanhiaAerea();
        companhiaAerea.setIataCia(code);
        companhiaAerea.setNomeCia(name);
        return companhiaAerea;
    }

    private ReservaValor mapReservaValor(JsonNode monetaryDetails) {
        if (!hasMonetaryInfo(monetaryDetails)) {
            return null;
        }

        JsonNode saleDetail = monetaryDetail(monetaryDetails, "Sale");
        JsonNode netDetail = monetaryDetail(monetaryDetails, "Net");

        ReservaValor valor = new ReservaValor();
        valor.setMoeda(resolveCurrency(saleDetail));
        valor.setValorTarifa(monetaryAmount(saleDetail, "FarePrice"));
        valor.setValorTarifaNet(firstDouble(monetaryAmount(netDetail, "FarePrice"), valor.getValorTarifa()));
        valor.setValorTaxaEmbarque(monetaryAmount(saleDetail, "Tax"));
        valor.setValorRav(firstDouble(
                monetaryAmount(saleDetail, "Rav"),
                monetaryAmountByType(saleDetail, 5)
        ));
        valor.setValorDu(firstDouble(
                monetaryAmountByCode(saleDetail, "DU"),
                monetaryAmount(saleDetail, "Du"),
                monetaryAmountByType(saleDetail, 4)
        ));
        valor.setValorRc(firstDouble(
                monetaryAmountByType(saleDetail, 6),
                monetaryAmount(saleDetail, "Fee")
        ));
        valor.setValorMkp(sumDouble(
                monetaryAmount(saleDetail, "MkpFarePrice"),
                monetaryAmount(saleDetail, "MkpDu"),
                monetaryAmount(saleDetail, "Markup")
        ));
        valor.setValorTaxaCombustivel(0.0);
        valor.setValorAssento(0.0);
        return valor;
    }

    private Double resolveValorTotal(JsonNode monetaryDetails) {
        JsonNode saleDetail = monetaryDetail(monetaryDetails, "Sale");
        return defaultDouble(monetaryAmount(saleDetail, "Total"));
    }

    private JsonNode monetaryDetail(JsonNode monetaryDetails, String detailDescription) {
        if (monetaryDetails == null || !monetaryDetails.isArray()) {
            return null;
        }

        for (JsonNode detail : monetaryDetails) {
            String description = text(detail, "MonetaryDetailTypeDescription");
            if (detailDescription.equalsIgnoreCase(defaultString(description))) {
                return detail;
            }
        }

        return null;
    }

    private Double monetaryAmount(JsonNode monetaryDetail, String infoDescription) {
        JsonNode info = monetaryInfo(monetaryDetail, infoDescription, "BRL");
        if (info == null) {
            info = monetaryInfo(monetaryDetail, infoDescription, null);
        }
        return info == null ? null : doubleValue(info, "Amount");
    }

    private Double monetaryAmountByCode(JsonNode monetaryDetail, String code) {
        JsonNode info = monetaryInfoByCode(monetaryDetail, code, "BRL");
        if (info == null) {
            info = monetaryInfoByCode(monetaryDetail, code, null);
        }
        return info == null ? null : doubleValue(info, "Amount");
    }

    private Double monetaryAmountByType(JsonNode monetaryDetail, int type) {
        JsonNode info = monetaryInfoByType(monetaryDetail, type, "BRL");
        if (info == null) {
            info = monetaryInfoByType(monetaryDetail, type, null);
        }
        return info == null ? null : doubleValue(info, "Amount");
    }

    private String resolveCurrency(JsonNode monetaryDetail) {
        JsonNode info = monetaryInfo(monetaryDetail, "Total", "BRL");
        if (info == null) {
            info = firstMonetaryInfo(monetaryDetail);
        }
        return info == null ? null : text(info, "CurrencyCode");
    }

    private JsonNode monetaryInfo(JsonNode monetaryDetail, String infoDescription, String currency) {
        if (monetaryDetail == null || !monetaryDetail.path("MonetaryInfo").isArray()) {
            return null;
        }

        for (JsonNode info : monetaryDetail.path("MonetaryInfo")) {
            String description = text(info, "MonetaryInfoTypeDescription");
            String currencyCode = text(info, "CurrencyCode");
            boolean sameDescription = infoDescription.equalsIgnoreCase(defaultString(description));
            boolean sameCurrency = currency == null || currency.equalsIgnoreCase(defaultString(currencyCode));

            if (sameDescription && sameCurrency) {
                return info;
            }
        }

        return null;
    }

    private JsonNode monetaryInfoByCode(JsonNode monetaryDetail, String code, String currency) {
        if (monetaryDetail == null || !monetaryDetail.path("MonetaryInfo").isArray()) {
            return null;
        }

        for (JsonNode info : monetaryDetail.path("MonetaryInfo")) {
            String infoCode = text(info, "Code");
            String description = text(info, "Description");
            String currencyCode = text(info, "CurrencyCode");
            boolean sameCode = code.equalsIgnoreCase(defaultString(infoCode))
                    || code.equalsIgnoreCase(defaultString(description));
            boolean sameCurrency = currency == null || currency.equalsIgnoreCase(defaultString(currencyCode));

            if (sameCode && sameCurrency) {
                return info;
            }
        }

        return null;
    }

    private JsonNode monetaryInfoByType(JsonNode monetaryDetail, int type, String currency) {
        if (monetaryDetail == null || !monetaryDetail.path("MonetaryInfo").isArray()) {
            return null;
        }

        for (JsonNode info : monetaryDetail.path("MonetaryInfo")) {
            Integer monetaryType = intValue(info, "MonetaryInfoType");
            String currencyCode = text(info, "CurrencyCode");
            boolean sameType = Integer.valueOf(type).equals(monetaryType);
            boolean sameCurrency = currency == null || currency.equalsIgnoreCase(defaultString(currencyCode));

            if (sameType && sameCurrency) {
                return info;
            }
        }

        return null;
    }

    private JsonNode firstMonetaryInfo(JsonNode monetaryDetail) {
        if (monetaryDetail == null || !monetaryDetail.path("MonetaryInfo").isArray()) {
            return null;
        }

        Iterator<JsonNode> iterator = monetaryDetail.path("MonetaryInfo").elements();
        return iterator.hasNext() ? iterator.next() : null;
    }

    private boolean hasMonetaryInfo(JsonNode monetaryDetails) {
        if (monetaryDetails == null || !monetaryDetails.isArray()) {
            return false;
        }

        for (JsonNode detail : monetaryDetails) {
            if (detail.path("MonetaryInfo").isArray() && detail.path("MonetaryInfo").size() > 0) {
                return true;
            }
        }

        return false;
    }

    private String buildRegraReserva(JsonNode header, JsonNode airDetail, JsonNode links) {
        StringBuilder regra = new StringBuilder();
        appendRegra(regra, "WoobaUniqueId", text(header, "UniqueId"));
        appendRegra(regra, "WoobaAirUniqueId", relatedAirUniqueId(links));
        appendRegra(regra, "WoobaTransactionType", text(header, "TransactionTypeDescription"));
        appendRegra(regra, "WoobaTransactionState", text(header, "TransactionStateDescription"));
        appendRegra(regra, "WoobaTicket", text(header, "Ticket"));
        appendRegra(regra, "Route", text(airDetail, "RouteDescription"));
        return regra.length() == 0 ? null : regra.toString();
    }

    private String relatedAirUniqueId(JsonNode links) {
        if (links == null || !links.isArray()) {
            return null;
        }

        for (JsonNode link : links) {
            String transactionType = text(link, "TransactionTypeDescription");
            if ("AirReservation".equalsIgnoreCase(defaultString(transactionType))) {
                return text(link, "UniqueId");
            }
        }

        return null;
    }

    private void appendRegra(StringBuilder regra, String key, String value) {
        if (isBlank(value)) {
            return;
        }

        if (regra.length() > 0) {
            regra.append("; ");
        }
        regra.append(key).append("=").append(value);
    }

    private Integer mapSexo(JsonNode personNode) {
        Integer gender = intValue(personNode, "Gender");
        if (gender != null) {
            return gender;
        }

        String description = text(personNode, "GenderDescription");
        if ("Male".equalsIgnoreCase(description) || "M".equalsIgnoreCase(description)) {
            return 1;
        }
        if ("Female".equalsIgnoreCase(description) || "F".equalsIgnoreCase(description)) {
            return 2;
        }
        return 0;
    }

    private int mapTipoPassageiro(String typeCode) {
        if (typeCode == null) {
            return 0;
        }

        String normalized = typeCode.trim().toUpperCase(Locale.ROOT);
        if ("ADT".equals(normalized) || "ADULT".equals(normalized)) {
            return 1;
        }
        if ("CHD".equals(normalized) || "CNN".equals(normalized) || "CHILD".equals(normalized)) {
            return 2;
        }
        if ("INF".equals(normalized) || "INFANT".equals(normalized)) {
            return 3;
        }
        return 0;
    }

    private String mapTelefone(JsonNode phoneNode) {
        if (phoneNode == null || phoneNode.isMissingNode() || phoneNode.isNull()) {
            return null;
        }

        String fullNumber = firstNonBlank(
                text(phoneNode, "Number"),
                text(phoneNode, "PhoneNumber"),
                text(phoneNode, "CompleteNumber")
        );
        if (!isBlank(fullNumber)) {
            return fullNumber;
        }

        String ddi = text(phoneNode, "InternationalCode");
        String ddd = text(phoneNode, "AreaCode");
        String number = text(phoneNode, "LocalNumber");
        return joinNonBlank(ddi, ddd, number);
    }

    private Date parseFlightDateTime(String date, String time) {
        Date parsedDate = parseDate(date);
        if (parsedDate == null || isBlank(time)) {
            return parsedDate;
        }

        try {
            LocalDate localDate = parsedDate.toInstant().atZone(DEFAULT_ZONE).toLocalDate();
            LocalTime localTime = LocalTime.parse(time.trim(), DateTimeFormatter.ofPattern("HH:mm"));
            return Date.from(LocalDateTime.of(localDate, localTime).atZone(DEFAULT_ZONE).toInstant());
        } catch (DateTimeParseException e) {
            return parsedDate;
        }
    }

    private Date parseDate(String value) {
        if (isBlank(value)) {
            return null;
        }

        String normalized = value.trim();

        for (DateTimeFormatter formatter : DATE_TIME_FORMATS) {
            try {
                LocalDateTime parsed = LocalDateTime.parse(normalized, formatter);
                return Date.from(parsed.atZone(DEFAULT_ZONE).toInstant());
            } catch (DateTimeParseException ignored) {
                // Tenta o proximo formato.
            }
        }

        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                LocalDate parsed = LocalDate.parse(normalized, formatter);
                return Date.from(parsed.atStartOfDay(DEFAULT_ZONE).toInstant());
            } catch (DateTimeParseException ignored) {
                // Tenta o proximo formato.
            }
        }

        return null;
    }

    private Timestamp toTimestamp(Date value) {
        return value == null ? null : new Timestamp(value.getTime());
    }

    private Date firstDate(Date... dates) {
        if (dates == null) {
            return null;
        }

        for (Date date : dates) {
            if (date != null) {
                return date;
            }
        }
        return null;
    }

    private Double firstDouble(Double... values) {
        if (values == null) {
            return null;
        }

        for (Double value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Double sumDouble(Double... values) {
        if (values == null) {
            return null;
        }

        double total = 0.0;
        boolean hasValue = false;
        for (Double value : values) {
            if (value == null) {
                continue;
            }
            total += value;
            hasValue = true;
        }
        return hasValue ? total : null;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }

        return value.asText();
    }

    private Integer intValue(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        JsonNode value = node.path(field);
        return value.isNumber() ? value.asInt() : null;
    }

    private Double doubleValue(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        JsonNode value = node.path(field);
        return value.isNumber() ? value.asDouble() : null;
    }

    private Boolean booleanValue(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        JsonNode value = node.path(field);
        return value.isBoolean() ? value.asBoolean() : null;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double defaultDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private boolean isIssued(Integer status, String statusDescription) {
        return Integer.valueOf(4).equals(status) || containsIgnoreCase(statusDescription, "Issued");
    }

    private boolean isCanceled(Integer status, String statusDescription) {
        return Integer.valueOf(5).equals(status) || containsIgnoreCase(statusDescription, "Canceled");
    }

    private Integer mapManagerStatus(Integer woobaStatus, String statusDescription) {
        if (isIssued(woobaStatus, statusDescription)) {
            return 3;
        }
        if (isCanceled(woobaStatus, statusDescription)) {
            return 2;
        }
        if (Integer.valueOf(1).equals(woobaStatus)
                || Integer.valueOf(2).equals(woobaStatus)
                || Integer.valueOf(3).equals(woobaStatus)
                || Integer.valueOf(6).equals(woobaStatus)) {
            return 1;
        }
        return woobaStatus;
    }

    private Integer mapStatusBilhete(Integer woobaStatus, String statusDescription) {
        if (isCanceled(woobaStatus, statusDescription)) {
            return 0;
        }
        return 1;
    }

    private String mapStatusVoo(Integer woobaStatus, String statusDescription) {
        if (isCanceled(woobaStatus, statusDescription)) {
            return "HX";
        }
        return "HK";
    }

    private boolean containsIgnoreCase(String value, String expected) {
        return value != null && expected != null
                && value.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String joinNonBlank(String... values) {
        StringBuilder builder = new StringBuilder();
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (isBlank(value)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(value.trim());
        }

        return builder.length() == 0 ? null : builder.toString();
    }
}
