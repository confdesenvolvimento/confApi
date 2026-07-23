package com.confApi.aereo.dto;

import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.hub.aereo.dto.Voo;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Data
public class TarifarRequest {

    private String sistema;
    private Agencia agencia;
    private List<ClasseSelecionada> classes;
    private String identificacaoViagem;
    private String identificacaoDaViagemVolta;
    private List<PassageiroTipoQtd> passageiroTipoQtds;
    private String promocode;
    private List<String> viagensMultiplas = null;

    public TarifarRequest() {
        this.classes = new ArrayList<>();
        this.passageiroTipoQtds = new ArrayList<>();
    }

    public TarifarRequest(String sistema, Agencia agencia, List<ClasseSelecionada> classes,
                          String identificacaoViagem, String identificacaoDaViagemVolta,
                          List<PassageiroTipoQtd> passageiroTipoQtds, String promocode,
                          List<String> viagensMultiplas) {
        this.sistema = sistema;
        this.agencia = agencia;
        this.classes = classes;
        this.identificacaoViagem = identificacaoViagem;
        this.identificacaoDaViagemVolta = identificacaoDaViagemVolta;
        this.passageiroTipoQtds = passageiroTipoQtds;
        this.promocode = promocode;
        this.viagensMultiplas = viagensMultiplas;
    }

    public TarifarRequest(PreReserva preReserva) {
        this();
        if (preReserva == null || preReserva.getTrechos() == null || preReserva.getTrechos().isEmpty()) {
            return;
        }

        validarSistemas(preReserva);
        Trecho primeiroTrecho = preReserva.getTrechos().get(0);

        if (primeiroTrecho != null) {
            this.sistema = primeiroTrecho.getSistema();
        }

        this.agencia = null;
        Integer ordemTrecho = 0;

        for (Trecho trecho : preReserva.getTrechos()) {
            if (trecho == null) {
                continue;
            }

            ordemTrecho++;
            preencherIdentificacaoViagem(preReserva, trecho, ordemTrecho);
            preencherClasses(trecho, ordemTrecho);
        }
        preencherPassageiros(preReserva);
    }

    private void validarSistemas(PreReserva preReserva) {
        Set<String> sistemas = new HashSet<>();

        for (Trecho trecho : preReserva.getTrechos()) {
            if (trecho != null && trecho.getSistema() != null) {
                sistemas.add(trecho.getSistema());
            }
        }

        if (sistemas.size() > 1) {
            throw new IllegalArgumentException(
                    "Não foi possível tarifar. Os trechos selecionados pertencem a sistemas diferentes."
            );
        }
    }

    private void preencherIdentificacaoViagem(PreReserva preReserva, Trecho trecho, Integer ordemTrecho) {
        Integer tipoVooPesquisa = preReserva.getTipoVooPesquisa();
        int totalTrechos = preReserva.getTrechos().size();

        if (tipoVooPesquisa == null) {
            return;
        }

        if (tipoVooPesquisa == 1 && totalTrechos == 1) {
            this.identificacaoViagem = trecho.getIdentificacaoDaViagem();
            return;
        }

        if (tipoVooPesquisa == 0 && totalTrechos == 2) {
            if (ordemTrecho == 1) {
                this.identificacaoViagem = trecho.getIdentificacaoDaViagem();
            } else {
                this.identificacaoDaViagemVolta = trecho.getIdentificacaoDaViagem();
            }
        }
    }

    private void preencherClasses(Trecho trecho, Integer ordemTrecho) {
        if (trecho.getVoos() == null || trecho.getFamiliaSelecionada() == null) {
            return;
        }

        for (Voo voo : trecho.getVoos()) {
            if (voo == null) {
                continue;
            }

            this.classes.add(new ClasseSelecionada(
                    trecho.getFamiliaSelecionada().getBaseTarifaria(),
                    trecho.getFamiliaSelecionada().getClasse(),
                    trecho.getFamiliaSelecionada().getFamilia() != null
                            ? trecho.getFamiliaSelecionada().getFamilia().getCodgFamilia()
                            : null,
                    voo.getNumeroVoo(),
                    voo.getIdentificacaoDoVoo(),
                    ordemTrecho,
                    trecho.getSistema()
            ));
        }
    }

    private void preencherPassageiros(PreReserva preReserva) {
        if (preReserva.getQtdAdt() != null && preReserva.getQtdAdt() > 0) {
            this.passageiroTipoQtds.add(new PassageiroTipoQtd("ADT", preReserva.getQtdAdt()));
        }

        if (preReserva.getQtdInf() != null && preReserva.getQtdInf() > 0) {
            this.passageiroTipoQtds.add(new PassageiroTipoQtd("INF", preReserva.getQtdInf()));
        }

        if (preReserva.getQtdChd() != null && preReserva.getQtdChd() > 0) {
            this.passageiroTipoQtds.add(new PassageiroTipoQtd("CHD", preReserva.getQtdChd()));
        }

        if (this.passageiroTipoQtds.isEmpty()) {
            this.passageiroTipoQtds.add(new PassageiroTipoQtd("ADT", 1));
        }
    }
}
