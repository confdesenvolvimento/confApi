package com.confApi.seguros.mapper;

import com.confApi.db.confManager.formaPagamento.FormaPagamento;
import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.model.FormaPagamentoModel;
import com.confApi.model.RecebimentoModel;
import com.confApi.model.CartaoModel;

import java.util.ArrayList;
import java.util.List;

public class RecebimentoSeguroMapper {

    private RecebimentoSeguroMapper() {
        // Evita instanciar classe utilitária
    }

    public static List<RecebimentoModel> toDTOList(List<Recebimento> recebimentos) {
        List<RecebimentoModel> recebimentosDTO = new ArrayList<>();

        if (recebimentos == null || recebimentos.isEmpty()) {
            return recebimentosDTO;
        }

        for (Recebimento recebimento : recebimentos) {
            if (recebimento == null) {
                continue;
            }

            recebimentosDTO.add(toDTO(recebimento));
        }

        return recebimentosDTO;
    }

    public static RecebimentoModel toDTO(Recebimento recebimento) {
        RecebimentoModel dto = new RecebimentoModel();

        if (recebimento == null) {
            return dto;
        }

        dto.setCodgRecebimento(recebimento.getCodgRecebimento());
        dto.setValorEntrada(valorOuZero(recebimento.getValrEntrada()));
        dto.setValorPagamento(valorOuZero(recebimento.getValrRecebimento()));
        dto.setStatusRecebimento(recebimento.getStatus());
        dto.setDataRecebimento(recebimento.getDataRecebimento());
        dto.setAssinatura(recebimento.getAssinaturaEletronica());
        dto.setLink(recebimento.getLink());

        FormaPagamento formaPagamento = recebimento.getCodgFormaPagto();

        if (formaPagamento != null) {
            dto.setCodgFormaPagamento(formaPagamento.getCodgFormaPagto());
            dto.setNomeFormaPagamento(formaPagamento.getNomeFormaPagto());

            dto.setFormaDePagamento(toFormaPagamentoModel(formaPagamento));
        }

        if (possuiDadosCartao(recebimento)) {
            dto.setCartaoSelecionado(toCartaoModel(recebimento));
        }

        return dto;
    }

    private static FormaPagamentoModel toFormaPagamentoModel(FormaPagamento formaPagamento) {
        FormaPagamentoModel model = new FormaPagamentoModel();

        if (formaPagamento == null) {
            return model;
        }

        model.setCodgFormaPagto(formaPagamento.getCodgFormaPagto());
        model.setNomeFormaPagto(formaPagamento.getNomeFormaPagto());

        return model;
    }

    private static CartaoModel toCartaoModel(Recebimento recebimento) {
        CartaoModel cartao = new CartaoModel();

        if (recebimento == null) {
            return cartao;
        }

        cartao.setNumeroCartao(recebimento.getNumrCartao());
        cartao.setValidadeCartao(recebimento.getValidadeCartao());

        cartao.setTitularBandeira(recebimento.getTitularCartao());

        if (recebimento.getCodgBandeira() != null) {
            cartao.setCodgBandeira(recebimento.getCodgBandeira().getCodgBandeira().toString());
            cartao.setNomeBandeira(recebimento.getCodgBandeira().getNomeBandeira());
        }

        return cartao;
    }

    private static boolean possuiDadosCartao(Recebimento recebimento) {
        return recebimento.getNumrCartao() != null
                || recebimento.getValidadeCartao() != null
                || recebimento.getCodgSegCartao() != null
                || recebimento.getTitularCartao() != null
                || recebimento.getCodgBandeira() != null;
    }

    private static Double valorOuZero(Double valor) {
        return valor != null ? valor : 0.0;
    }
}
