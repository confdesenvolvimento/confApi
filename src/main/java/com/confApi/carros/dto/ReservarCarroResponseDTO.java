package com.confApi.carros.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReservarCarroResponseDTO {
    private String mensagem;
    private CarroBookingHub reservaCarro;
    private RegrasCancelamento regrasCancelamento;
    private List<String> formsOfReceipts;
    private Boolean carWithFranchise;
    private Boolean carTypeWithoutProtection;
    private String tariffType;
    private Double maximumDiscountPercentage;
    private DebitoHub debit;
    private String restrictionOfTermsAndConditions;
    private String localPaymentAlert;
    private List<PolicyHub> policies;
    private byte[] termsAndConditionsInBytes;
    private String descriptionOfTheCommissionPlan;
    private Double agentDiscountAmount;
    private String voucherType;
    private String urlTravelFlow;
    private String tokenSession;
    private String tokenSessionExpiresIn;
    private List<ErrorResponse> error;
    private Object returnMessage;
    private Boolean success;
}
