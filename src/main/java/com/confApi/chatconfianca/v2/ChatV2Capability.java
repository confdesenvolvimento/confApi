package com.confApi.chatconfianca.v2;

import java.util.Arrays;
import java.util.List;

/** Closed execution catalog. Database keywords cannot create arbitrary tools or writes. */
public enum ChatV2Capability {
    LIMITES("financeiro.limites", "limites", null, "Consultar credito da agencia autenticada"),
    FATURAS("financeiro.faturas", "faturas", null, "Consultar faturas; orientar acesso/PDF conforme dados"),
    BOLETOS("financeiro.boletos", "boletos", null, "Consultar documentos de cobranca"),
    PAGAMENTO("financeiro.forma_pagamento", null, null, "Orientar formas de pagamento com fonte publicada"),
    BSP("financeiro.calendario_bsp", null, null, "Consultar vencimentos por data de emissao e produto"),
    RESERVAS("aereo.reservas_recentes", "ultimas_reservas_aereas", null, "Listar ultimas reservas/vendas, nao total anual de vendas"),
    RESERVA("aereo.reserva_detalhes", "reserva_aerea_detalhes", null, "Consultar reserva pelo localizador"),
    REGRAS("aereo.regra_tarifaria", "reserva_aerea_regras", null, "Consultar regras, multas, cancelamento e reembolso de reserva"),
    REMARCACAO("aereo.simular_remarcacao", "selecionar_reserva_remarcacao", null, "Abrir simulador; nunca concluir remarcacao ou cobrar"),
    ACOES_RESERVA("aereo.acoes_reserva", "reserva_aerea_detalhes", null, "Preparar emissao/cancelamento/reembolso/alteracao, assentos, bilhetes ou voucher pelos botoes existentes; nunca concluir"),
    CHECKIN("aereo.checkin", "checkin", null, "Consultar embarques/check-ins proximos"),
    ALERTAS("aereo.alertas_tarifa", "alertas", null, "Consultar alertas do usuario autenticado"),
    FAMILIAS("aereo.familias_tarifarias", "familias", null, "Consultar bagagem e beneficios por companhia"),
    VOOS("aereo.busca_voos", null, "search_flights", "Abrir pesquisa convencional no portal com datas exatas"),
    TARIFA_IDA("aereo.melhor_tarifa_ida", null, "search_cheapest_airfares", "Consultar cache de melhores tarifas somente ida"),
    TARIFA_VOLTA("aereo.melhor_tarifa_ida_volta", null, "search_cheapest_roundtrip_airfares", "Consultar cache ida e volta; periodo/mes sao aceitos"),
    HOTEL("hotel.busca_hospedagem", null, "search_hotels", "Abrir pesquisa de hotel no portal"),
    PACOTE("pacote.melhor_oferta", null, "search_cheapest_packages", "Consultar cache de pacotes com aereo e hotel"),
    CONTATOS("institucional.contatos_departamentos", null, null, "Consultar contatos, inclusive TI e Grupos, pela fonte publicada"),
    HORARIO("institucional.horario_atendimento", null, null, "Consultar horario regular, distinguindo plantao"),
    EMERGENCIA("institucional.atendimento_emergencial", null, null, "Consultar plantao e canais emergenciais"),
    TI("institucional.suporte_ti", null, null, "Orientar problema de acesso/app/portal"),
    AJUDA("orientacao_geral", null, null, "Navegar ajuda; esclarecer assunto ou capacidade nao disponivel"),
    SAUDACAO("conversa.saudacao", null, null, "Saudar sem departamento"),
    HUMANO("conversa.atendimento_humano", null, null, "Sugerir handoff; departamento exige escolha/confirmacao na interface");

    public final String code;
    public final String action;
    public final String tool;
    public final String description;
    ChatV2Capability(String code, String action, String tool, String description) {
        this.code=code; this.action=action; this.tool=tool; this.description=description;
    }
    public static ChatV2Capability from(String code) {
        return Arrays.stream(values()).filter(c -> c.code.equals(code)).findFirst().orElse(null);
    }
    public static List<String> codes() { return Arrays.stream(values()).map(c -> c.code).toList(); }
    public boolean usaLocalizador() {
        return this == RESERVA || this == REGRAS || this == REMARCACAO || this == ACOES_RESERVA;
    }
    public boolean exigeConfirmacao() {
        return this == REMARCACAO || this == ACOES_RESERVA || this == HUMANO;
    }
    public String memoryCode() {
        return this == REMARCACAO || this == ACOES_RESERVA ? REGRAS.code : code;
    }
}
