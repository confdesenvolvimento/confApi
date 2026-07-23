package com.confApi.aereo.dto;

import com.confApi.aereo.eNums.TipoConsulta;
import com.confApi.aereo.eNums.TipoPesquisa;
import com.confApi.hub.aereo.PassageiroModel;
import com.confApi.hub.aereo.dto.Contato;
import com.confApi.hub.aereo.dto.Passageiro;
import com.confApi.hub.aereo.dto.Voo;
import com.confApi.model.IdentificacaoAgenciaModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReservarRequest {
    private IdentificacaoAgenciaModel identificacaoAgenciaModel;
    private Agencia agencia;
    private List<ClasseSelecionada> classesSelecionadas;
    private List<Contato> contatos;
    private String identificacaoDaViagem;
    private String identificacaoDaViagemVolta;
    private List<String> identificacaoViagemMultipla;
    private List<Passageiro> passageiros;
    private String sistema;
    private String promocode = "";

    public ReservarRequest() {
    }

    public ReservarRequest(IdentificacaoAgenciaModel identificacaoAgenciaModel,
                           Agencia agencia, List<ClasseSelecionada> classesSelecionadas,
                           List<Contato> contatos, String identificacaoDaViagem,
                           String identificacaoDaViagemVolta, List<String> identificacaoViagemMultipla,
                           List<Passageiro> passageiros, String sistema, String promocode) {
        this.identificacaoAgenciaModel = identificacaoAgenciaModel;
        this.agencia = agencia;
        this.classesSelecionadas = classesSelecionadas;
        this.contatos = contatos;
        this.identificacaoDaViagem = identificacaoDaViagem;
        this.identificacaoDaViagemVolta = identificacaoDaViagemVolta;
        this.identificacaoViagemMultipla = identificacaoViagemMultipla;
        this.passageiros = passageiros;
        this.sistema = sistema;
        this.promocode = promocode;
    }

    public ReservarRequest(PreReserva preReserva) {
        this.identificacaoAgenciaModel = null;

        this.agencia = preReserva.getUsuario() != null && preReserva.getUsuario().getAgencia() != null
                ? new Agencia(preReserva.getUsuario().getAgencia())
                : null;

        this.classesSelecionadas = new ArrayList<>();

        String identificacaoViagemInter = "";
        int countTrecho = 0;

        if (preReserva.getTrechos() != null) {
            for (Trecho trecho : preReserva.getTrechos()) {
                countTrecho++;

                if (trecho.getVoos() == null) {
                    continue;
                }

                for (Voo voo : trecho.getVoos()) {
                    if (preReserva.getTipoTrecho().equals(TipoConsulta.INTERNACIONAL.getCod())) {
                        if (trecho.getFamiliaSelecionada() == null || trecho.getFamiliaSelecionada().getFamiliaPrecoInterList() == null) {
                            continue;
                        }

                        identificacaoViagemInter = trecho.getFamiliaSelecionada().getIdentificacaoDaViagem();

                        for (FamiliaPrecoInter familiaPrecoInter : trecho.getFamiliaSelecionada().getFamiliaPrecoInterList()) {
                            if (familiaPrecoInter.getNumeroVoo().equalsIgnoreCase(voo.getNumeroVoo())) {
                                this.classesSelecionadas.add(new ClasseSelecionada(familiaPrecoInter, countTrecho, trecho));
                            }
                        }

                    } else {
                        this.classesSelecionadas.add(new ClasseSelecionada(voo, countTrecho, trecho));
                    }
                }
            }
        }

        if (preReserva.getTrechos() != null && !preReserva.getTrechos().isEmpty()) {
            if (preReserva.getTipoVooPesquisa().equals(TipoPesquisa.ONEWAY.getCod())) {
                this.identificacaoDaViagem = preReserva.getTrechos().get(0).getIdentificacaoDaViagem();
            } else if (preReserva.getTipoVooPesquisa().equals(TipoPesquisa.ROUNDTRIP.getCod())) {
                this.identificacaoDaViagem = preReserva.getTrechos().get(0).getIdentificacaoDaViagem();
                if (preReserva.getTipoTrecho().equals(TipoConsulta.NACIONAL.getCod()) && preReserva.getTrechos().size() > 1) {
                    this.identificacaoDaViagemVolta = preReserva.getTrechos().get(1).getIdentificacaoDaViagem();
                }
            } else if (preReserva.getTipoVooPesquisa().equals(TipoPesquisa.MULTIPLOSTRECHOS.getCod())) {
                this.identificacaoViagemMultipla = null;
            }

            if (preReserva.getTipoTrecho().equals(TipoConsulta.INTERNACIONAL.getCod())) {
                this.identificacaoDaViagem = identificacaoViagemInter;
            }
        }

        this.contatos = preReserva.getContatos() != null && !preReserva.getContatos().isEmpty()
                ? preReserva.getContatos() : List.of(new Contato(preReserva));

        this.passageiros = new ArrayList<>();

        if (preReserva.getPassageiros() != null) {
            for (PassageiroModel paxModel : preReserva.getPassageiros()) {
                this.passageiros.add(new Passageiro(paxModel, preReserva));
            }
        }

        this.sistema = preReserva.getSistema() != null && !preReserva.getSistema().isBlank()
                ? preReserva.getSistema()
                : "Wooba";

        this.promocode = "";
    }
}

