package com.confApi.aereo;

import com.confApi.aereo.dto.*;
import com.confApi.aereo.eNums.FlagTipoPassageiro;
import com.confApi.aereo.eNums.StatusBilhete;
import com.confApi.db.confManager.aeroporto.Aeroporto;
import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.db.confManager.bilhete.BilheteAereo;
import com.confApi.db.confManager.companhiaAerea.CompanhiaAerea;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.db.confManager.reservaValor.ReservaValor;
import com.confApi.db.confManager.sistema.Sistema;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.hub.aereo.*;
import com.confApi.hub.aereo.dto.*;
import com.confApi.hub.aereo.dto.Bilhete;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;

@Service
public class AereoService {

    public ReservaAereoModel convertToReservaAereoModel(
            ConsultarLocalizadorResponse reservasApiModel,
            ReservaAereoModel reservaAerea,
            Boolean isVendaWooba
    ) {
        if (reservasApiModel == null
                || reservasApiModel.getReservas() == null
                || reservasApiModel.getReservas().isEmpty()) {
            return reservaAerea;
        }

        Reserva reservaApi = reservasApiModel.getReservas().get(0);

        reservaAerea.setLocalizador(reservaApi.getLocalizador());
        reservaAerea.setSistema(reservaApi.getSistema());
        reservaAerea.setDataCriacao(reservaApi.getDataCriacao());
        reservaAerea.setDataEmissao(reservaApi.getDataEmissao());
        reservaAerea.setStatusReserva(reservaApi.getStatus());

        if (reservaApi.getViagens() != null && !reservaApi.getViagens().isEmpty()) {
            reservaAerea.setCompanhiaAerea(
                    reservaApi.getViagens().get(0).getCompanhia().getDescricao()
            );
        }

        if (reservaAerea.getContatos() == null) {
            reservaAerea.setContatos(new ArrayList<>());
        }

        if (reservaApi.getContatos() != null) {
            for (Contato contato : reservaApi.getContatos()) {
                ContatoModel contatoModel = new ContatoModel();
                contatoModel.setNome(contato.getNome());
                contatoModel.setEmail(contato.getEmail());
                contatoModel.setNumeroDDD(contato.getNumeroDDD());
                contatoModel.setNumeroDDI(contato.getNumeroDDI());
                contatoModel.setNumeroTelefone(contato.getNumeroTelefone());

                reservaAerea.getContatos().add(contatoModel);
            }
        }

        if (reservaAerea.getPassageiros() == null) {
            reservaAerea.setPassageiros(new ArrayList<>());
        }

        if (reservaApi.getPassageiros() != null) {
            for (Passageiro paxApi : reservaApi.getPassageiros()) {
                PassageiroModel pax = new PassageiroModel();

                pax.setNome(paxApi.getNome());
                pax.setSobrenome(paxApi.getSobrenome());
                pax.setFaixaEtaria(paxApi.getFaixaEtaria());

                if (paxApi.getDocumento() != null) {
                    pax.setCpf(paxApi.getDocumento().getNumero());
                    pax.setDocumento(new DocumentoPassageiro(
                            paxApi.getDocumento().getNacionalidade(),
                            paxApi.getDocumento().getNumero(),
                            paxApi.getDocumento().getPaisEmissor(),
                            paxApi.getDocumento().getTipo()
                    ));
                }

                if (paxApi.getNascimento() != null) {
                    pax.setNascimento(paxApi.getNascimento());
                }

                if (paxApi.getBilhetes() != null) {
                    pax.setBilhetes(new ArrayList<>());

                    for (Bilhete bilheteApi : paxApi.getBilhetes()) {
                        BilheteModel b = new BilheteModel();

                        b.setDataCancelamento(null);
                        b.setDataEmissao(bilheteApi.getDataDeEmissao());
                        b.setNumeroBilhete(bilheteApi.getNumero());

                        if ("Ativa".equalsIgnoreCase(bilheteApi.getStatus())) {
                            b.setStatus(StatusBilhete.Ativo.statusBilhete);
                        } else if ("Cancelado".equalsIgnoreCase(bilheteApi.getStatus())) {
                            b.setStatus(StatusBilhete.Cancelado.statusBilhete);
                        } else {
                            b.setStatus(StatusBilhete.Indefinido.statusBilhete);
                        }

                        pax.getBilhetes().add(b);
                    }
                }

                reservaAerea.getPassageiros().add(pax);
            }
        }

        if (reservaApi.getPrazoEmissao() != null) {
            reservaAerea.setPrazoReserva(converterPrazoEmissao(reservaApi.getPrazoEmissao()));
        }

        if (reservaAerea.getTrechos() == null) {
            reservaAerea.setTrechos(new ArrayList<>());
        }

        if (reservaApi.getViagens() != null) {
            reservaAerea.getTrechos().addAll(reservaApi.getViagens());
        }

        if (Boolean.TRUE.equals(isVendaWooba)) {
            popularValoresReserva(reservaAerea, reservaApi);
        }

        return reservaAerea;
    }

    public void populaReservaFromDB(ReservaAereo reservaDB, ReservaAereoModel reservaAerea) {
        if (reservaDB == null || reservaAerea == null) {
            return;
        }

        reservaAerea.setUsuarioCriacao(reservaDB.getCodgUsuarioCriacao().getLoginUsuario());
        reservaAerea.setUsuarioCriacao2(reservaDB.getCodgUsuarioCriacao());
        reservaAerea.setCodgReservaAereoDB(Long.valueOf(reservaDB.getCodgReservaAereo()));
        reservaAerea.setDataCriacao(reservaDB.getDataCriacao());

        if (reservaAerea.getLocalizador() == null) {
            reservaAerea.setLocalizador(reservaDB.getLocalizador());
        }

        if (reservaDB.getDataEmissao() != null) {
            reservaAerea.setDataEmissao(reservaDB.getDataEmissao());
        }

        reservaAerea.setAgencia(reservaDB.getCodgAgencia());
        reservaAerea.setCodgCompanhiaAerea(reservaDB.getCodgCompanhiaAerea());

        if (reservaDB.getCodgReservaPacote() != null) {
            reservaAerea.setIsPacote(true);
            reservaAerea.setReservaPacote(reservaDB.getCodgReservaPacote());
        } else {
            reservaAerea.setIsPacote(false);
        }

        if (reservaDB.getDataCancelamento() != null) {
            reservaAerea.setDataCancelamento(reservaDB.getDataCancelamento());
        }

        if (reservaDB.getFonte() != null) {
            if (reservaDB.getFonte() == 1) {
                reservaAerea.setFonte("CONF_HUB");
            } else if (reservaDB.getFonte() == 0) {
                reservaAerea.setFonte("CONF_APP");
            } else if (reservaDB.getFonte() == 2) {
                reservaAerea.setFonte("PORTAL");
            }
        }

        popularPassageirosFromDB(reservaDB, reservaAerea);
        popularTrechosFromDB(reservaDB, reservaAerea);
        popularRecebimentosFromDB(reservaDB, reservaAerea);
    }

    public PreReserva montarPreReservaTarifada(
            PreReserva preReserva,
            TarifarResponse tarifaResponse) {

        if (preReserva == null || tarifaResponse == null) {
            return null;
        }

        if (tarifaResponse.getException() != null) {
            return null;
        }

        PreReserva preRes1 = new PreReserva();

        preRes1.setTrechos(new ArrayList<>());
        preRes1.setPassageiros(preReserva.getPassageiros());

        preRes1.setQtdAdt(preReserva.getQtdAdt());
        preRes1.setQtdChd(preReserva.getQtdChd());
        preRes1.setQtdInf(preReserva.getQtdInf());

        preRes1.setTipoTrecho(preReserva.getTipoTrecho());
        preRes1.setTipoVooPesquisa(preReserva.getTipoVooPesquisa());

        preRes1.setValorTotalTarifa(0.00);
        preRes1.setValorTotalTaxaDu(0.00);
        preRes1.setValorTotalTaxaEmbarque(0.00);
        preRes1.setValorTotalTaxas(0.00);
        preRes1.setValorTotalGeral(0.00);
        preRes1.setValorTotalMkp(0.00);

        if (tarifaResponse.getTrecho1() != null) {
            preRes1.getTrechos().addAll(tarifaResponse.getTrecho1());
        }

        if (tarifaResponse.getTrecho2() != null) {
            preRes1.getTrechos().addAll(tarifaResponse.getTrecho2());
        }

        // preencher valores ADT/CHD/INF aqui

        return preRes1;
    }

    public void populaReservaToReservaDB(Reserva reservaDB, PreReserva preReserva) {
        if (reservaDB == null || preReserva == null) {
            return;
        }

        popularVoos(reservaDB, preReserva);
        popularPassageiros(reservaDB, preReserva);
    }

    private void popularVoos(Reserva reservaDB, PreReserva preReserva) {
        if (preReserva.getTrechos() == null || reservaDB.getViagens() == null) {
            return;
        }

        for (Trecho trechoReserva : preReserva.getTrechos()) {
            if (trechoReserva == null || trechoReserva.getVoos() == null) {
                continue;
            }

            for (TrechoReserva trechoDB : reservaDB.getViagens()) {
                if (trechoDB == null || trechoDB.getVoos() == null) {
                    continue;
                }

                for (Voo vooReserva : trechoReserva.getVoos()) {
                    if (vooReserva == null || vooReserva.getNumeroVoo() == null) {
                        continue;
                    }

                    for (Voo vooDB : trechoDB.getVoos()) {
                        if (vooDB == null || vooDB.getNumeroVoo() == null) {
                            continue;
                        }

                        if (vooReserva.getNumeroVoo().equalsIgnoreCase(vooDB.getNumeroVoo())) {
                            if (trechoReserva.getFamiliaSelecionada() != null) {
                                vooDB.setFamilia(trechoReserva.getFamiliaSelecionada().getBaseTarifaria());

                                if (trechoReserva.getFamiliaSelecionada().getFamilia() != null) {
                                    vooDB.setFamiliaCodigo(
                                            trechoReserva.getFamiliaSelecionada()
                                                    .getFamilia()
                                                    .getCodgFamilia()
                                    );
                                }
                            }

                            vooDB.setEquipamento(vooReserva.getEquipamento());
                        }
                    }
                }
            }
        }
    }

    private void popularPassageiros(Reserva reservaDB, PreReserva preReserva) {
        if (reservaDB.getPassageiros() == null || preReserva.getPassageiros() == null) {
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (Passageiro paxDB : reservaDB.getPassageiros()) {
            if (paxDB == null) {
                continue;
            }

            for (PassageiroModel passageiro : preReserva.getPassageiros()) {
                if (passageiro == null) {
                    continue;
                }

                if (!isMesmoPassageiro(paxDB, passageiro)) {
                    continue;
                }

                paxDB.setEmail(passageiro.getEmail());

                if (passageiro.getNascimento() != null && !passageiro.getNascimento().isBlank()) {
                    LocalDate localDate = LocalDate.parse(passageiro.getNascimento(), formatter);

                    Instant instant = localDate
                            .atTime(12, 0)
                            .atZone(ZoneId.systemDefault())
                            .toInstant();

                    paxDB.setDataNascimento(Date.from(instant));
                }
            }
        }
    }

    private void popularTrechosFromDB(ReservaAereo reservaDB, ReservaAereoModel reservaAerea) {
        if (reservaDB.getTrechos() == null || reservaAerea.getTrechos() == null) {
            return;
        }

        for (TrechoReserva trechoReserva : reservaAerea.getTrechos()) {
            if (trechoReserva == null || trechoReserva.getVoos() == null) {
                continue;
            }

            for (Voo vooModel : trechoReserva.getVoos()) {
                if (vooModel == null || vooModel.getNumeroVoo() == null) {
                    continue;
                }

                for (com.confApi.db.confManager.trecho.Trecho trechoDB : reservaDB.getTrechos()) {
                    if (trechoDB == null || trechoDB.getVoos() == null) {
                        continue;
                    }

                    for (com.confApi.db.confManager.voo.Voo vooDB : trechoDB.getVoos()) {
                        if (vooDB == null || vooDB.getNumeroVoo() == null) {
                            continue;
                        }

                        if (!vooModel.getNumeroVoo().equalsIgnoreCase(vooDB.getNumeroVoo())) {
                            continue;
                        }

                        vooModel.setBaseTarifaria(vooDB.getFamilia());

                        if (trechoReserva.getCompanhia() != null
                                && trechoReserva.getCompanhia().getCodigoIata() != null
                                && isCompanhiaComCss(trechoReserva.getCompanhia().getCodigoIata())) {

                            vooModel.setDescricaocss(
                                    "color_"
                                            + vooModel.getFamiliaCodigo()
                                            + "_"
                                            + trechoReserva.getCompanhia().getCodigoIata()
                            );
                        }
                    }
                }
            }
        }
    }

    private boolean isCompanhiaComCss(String codigoIata) {
        return "JJ".equalsIgnoreCase(codigoIata)
                || "LA".equalsIgnoreCase(codigoIata)
                || "G3".equalsIgnoreCase(codigoIata)
                || "AD".equalsIgnoreCase(codigoIata);
    }

    private void popularRecebimentosFromDB(ReservaAereo reservaDB, ReservaAereoModel reservaAerea) {
        if (reservaDB.getRecebimentos() == null) {
            return;
        }

        reservaAerea.setRecebimentos(new ArrayList<>());

        for (com.confApi.db.confManager.recebimento.Recebimento recebimentoDB : reservaDB.getRecebimentos()) {
            if (recebimentoDB == null) {
                continue;
            }

            RecebimentoModel recebimentoModel = new RecebimentoModel();

            if (recebimentoDB.getCodgFormaPagto() != null) {
                recebimentoModel.setCodgFormaPagamento(
                        recebimentoDB.getCodgFormaPagto().getCodgFormaPagto()
                );
                recebimentoModel.setNomeFormaPagamento(
                        recebimentoDB.getCodgFormaPagto().getNomeFormaPagto()
                );
            }

            recebimentoModel.setValorPagamento(recebimentoDB.getValrRecebimento());
            recebimentoModel.setValorEntrada(recebimentoDB.getValrEntrada());
            recebimentoModel.setStatusRecebimento(recebimentoDB.getStatus());
            recebimentoModel.setDataRecebimento(recebimentoDB.getDataRecebimento());
            recebimentoModel.setCodgRecebimento(recebimentoDB.getCodgRecebimento());
            recebimentoModel.setLink(recebimentoDB.getLink());

            if (recebimentoDB.getNumrCartao() != null) {
                CartaoModel cartaoModel = new CartaoModel();

                cartaoModel.setNumeroCartao(mascararNumeroCartao(recebimentoDB.getNumrCartao()));
                cartaoModel.setTitularBandeira(recebimentoDB.getTitularCartao());
                cartaoModel.setCodgAutorizacao(recebimentoDB.getCodgAutCartao());

                if (recebimentoDB.getQtdeParcela() != null) {
                    cartaoModel.setQuantidadeParcelas(recebimentoDB.getQtdeParcela().toString());
                }

                ParcelaCartaoModel parcela = new ParcelaCartaoModel();
                parcela.setValorPrimeiraParcela(recebimentoDB.getValrPrimeiraParcela());
                parcela.setValorDemaisParcelas(recebimentoDB.getValrDemaisParcela());

                cartaoModel.setParcelaSelecionada(parcela);
                recebimentoModel.setCartaoSelecionado(cartaoModel);
            }

            reservaAerea.getRecebimentos().add(recebimentoModel);
        }
    }

    private String mascararNumeroCartao(String numeroCartao) {
        if (numeroCartao == null || numeroCartao.length() <= 4) {
            return numeroCartao;
        }

        String ultimosDigitos = numeroCartao.substring(numeroCartao.length() - 4);

        return "**** **** **** " + ultimosDigitos;
    }

    private boolean isMesmoPassageiro(Passageiro paxDB, PassageiroModel passageiro) {
        return paxDB.getNome() != null
                && paxDB.getSobrenome() != null
                && passageiro.getNome() != null
                && passageiro.getSobrenome() != null
                && paxDB.getNome().equalsIgnoreCase(passageiro.getNome())
                && paxDB.getSobrenome().equalsIgnoreCase(passageiro.getSobrenome());
    }

    public ReservaAereo convertToReservaAereo(Reserva reservaResp, PreReserva preReserva) {
        ReservaAereo reservaAereo = new ReservaAereo();
        reservaAereo.setCodgAgencia(preReserva.getUsuario().getAgencia());
        reservaAereo.setCodgCompanhiaAerea(new CompanhiaAerea(reservaResp.getViagens().get(0).getCompanhia().getCodigoIata()));
        reservaAereo.setCodgSistema(new Sistema(1));
        reservaAereo.setCodgUsuarioCancelamento(null);
        reservaAereo.setCodgUsuarioCriacao(new Usuario(preReserva.getUsuario().getId()));
        reservaAereo.setCodgUsuarioEmissao(null);
        reservaAereo.setDataCancelamento(null);
        reservaAereo.setDataCriacao(new Date());
        reservaAereo.setDataEmissao(null);
        // reservaAereo.setCodgReservaPacote(new ReservaPacote(1));

        String prazoEmissao = reservaResp.getPrazoEmissao();

        String formattedDate = prazoEmissao.replaceAll(":(?=[0-9]{2}$)", "");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        LocalDateTime ldt = LocalDateTime.parse(formattedDate, formatter);
        ZonedDateTime zdt = ldt.atZone(ZoneId.of("America/Sao_Paulo"));
        Date prazo = Date.from(zdt.toInstant());

        // reservaAereo.setDataLimiteEmissao(convertStringToDate(reservaResp.getPrazoEmissao()));
        reservaAereo.setDataLimiteEmissao(prazo);
        reservaAereo.setDescMotivoCancelamento(null);
        reservaAereo.setLocalizador(reservaResp.getLocalizador());
        reservaAereo.setStatus(1);

        if (reservaAereo.getPassageiros() == null) {
            reservaAereo.setPassageiros(new ArrayList<>());
        }
        for (Passageiro passageiro : reservaResp.getPassageiros()) {
            com.confApi.db.confManager.passageiro.Passageiro paxDB = new com.confApi.db.confManager.passageiro.Passageiro();
            paxDB.setNomePassageiro(passageiro.getNome());
            paxDB.setMeioNomePassageiro(passageiro.getNomeDoMeio());
            paxDB.setSobrenomePassageiro(passageiro.getSobrenome());
            if (passageiro.getSexo() == null) {
                passageiro.setSexo("M");
            }
            if (passageiro.getSexo().equalsIgnoreCase("M")) {
                paxDB.setSexo(1);
            } else {
                paxDB.setSexo(0);
            }
            if (passageiro.getFaixaEtaria().equalsIgnoreCase("ADT")) {
                paxDB.setTipoPassageiro(FlagTipoPassageiro.ADT.getTipoPax());
            } else if (passageiro.getFaixaEtaria().equalsIgnoreCase("CHD")) {
                paxDB.setTipoPassageiro(FlagTipoPassageiro.CHD.getTipoPax());
            } else if (passageiro.getFaixaEtaria().equalsIgnoreCase("INF")) {
                paxDB.setTipoPassageiro(FlagTipoPassageiro.INF.getTipoPax());
            }
            paxDB.setCpf(passageiro.getCpf());
            paxDB.setTelefone(reservaResp.getContatos().get(0).getNumeroDDD() + reservaResp.getContatos().get(0).getNumeroTelefone());
            paxDB.setCelular(reservaResp.getContatos().get(0).getNumeroDDD() + reservaResp.getContatos().get(0).getNumeroTelefone());
            if (passageiro.getDocumento() != null && passageiro.getDocumento().getNumero() != null) {
                paxDB.setNumrDocumento(passageiro.getDocumento().getNumero());
            } else {
                paxDB.setNumrDocumento(passageiro.getCpf());
            }

            paxDB.setDataNascimento(passageiro.getDataNascimento());

            paxDB.setEmail(passageiro.getEmail());
            paxDB.setIdPassageiroCia(passageiro.getIdPassageiro());
            if (paxDB.getReservaValores() == null) {
                paxDB.setReservaValores(new ArrayList<>());
            }
            for (ValorPassageiro valorR : reservaResp.getValorReserva().getValorBase().getValorPassageiroList()) {
                if (valorR.getNomePassageiro().equalsIgnoreCase(paxDB.getNomePassageiro() + " " + paxDB.getSobrenomePassageiro())) {
                    if (paxDB.getTipoPassageiro() == FlagTipoPassageiro.ADT.getTipoPax()) {
                        for (ReservaValoresAereo reservaAereoValoresADT : preReserva.getValoresReservaAdt()) {
                            populaValorReservaPassageiroCiasDistintas(reservaAereoValoresADT, reservaResp);
                            populaPassageirosToDB(reservaAereoValoresADT, paxDB, reservaResp);
                        }
                    } else if (paxDB.getTipoPassageiro() == FlagTipoPassageiro.CHD.getTipoPax()) {
                        for (ReservaValoresAereo reservaAereoValoresChd : preReserva.getValoresReservaChd()) {
                            populaValorReservaPassageiroCiasDistintas(reservaAereoValoresChd, reservaResp);
                            populaPassageirosToDB(reservaAereoValoresChd, paxDB, reservaResp);
                        }
                    } else if (paxDB.getTipoPassageiro() == FlagTipoPassageiro.INF.getTipoPax()) {
                        for (ReservaValoresAereo reservaAereoValoresInf : preReserva.getValoresReservaInf()) {
                            populaValorReservaPassageiroCiasDistintas(reservaAereoValoresInf, reservaResp);
                            populaPassageirosToDB(reservaAereoValoresInf, paxDB, reservaResp);
                        }

                    }
                }
            }

            reservaAereo.getPassageiros().add(paxDB);
        }

        if (reservaAereo.getTrechos() == null) {
            reservaAereo.setTrechos(new ArrayList<>());
        }

        for (TrechoReserva trechoReserva : reservaResp.getViagens()) {
            com.confApi.db.confManager.trecho.Trecho trechoDB = new com.confApi.db.confManager.trecho.Trecho();
            trechoDB.setCodgAeroportoDestino(new Aeroporto(trechoReserva.getDestino().getCodigoIata()));
            trechoDB.setCodgAeroportoOrigem(new Aeroporto(trechoReserva.getOrigem().getCodigoIata()));
            trechoDB.setCodgCompanhiaAerea(new CompanhiaAerea(trechoReserva.getCompanhia().getCodigoIata()));

            for (Voo voo : trechoReserva.getVoos()) {
                com.confApi.db.confManager.voo.Voo vooDB = new com.confApi.db.confManager.voo.Voo();
                vooDB.setCodgAeroportoOrigem(new Aeroporto(voo.getOrigem().getCodigoIata()));
                vooDB.setCodgAeroportoDestino(new Aeroporto(voo.getDestino().getCodigoIata()));
                vooDB.setCodgCompanhiaAerea(new CompanhiaAerea(voo.getCiaMandatoria().getCodigoIata()));
                vooDB.setNumeroVoo(voo.getNumeroVoo());
                vooDB.setBaseTarifa(voo.getFamiliaCodigo());
                vooDB.setClasseTarifa(voo.getClasse());
                vooDB.setAeronave(voo.getEquipamento());
                vooDB.setQtdEscalas(voo.getQtdEscalas());
                vooDB.setFamilia(voo.getFamilia());
                vooDB.setCodgFamilia(voo.getFamiliaCodigo());
                vooDB.setDataHoraPartida(new Timestamp(voo.getDataPartida().getTime()));
                vooDB.setDataHoraChegada(new Timestamp(voo.getDataChegada().getTime()));
                vooDB.setTipoRota(voo.getTipoSegmento());
                vooDB.setQtdBagagem(voo.getBagagemQuantidade());
                vooDB.setPesoBagagem(voo.getBagagemPeso());
                if (voo.getCiaOperadora() != null) {
                    vooDB.setCodgCompanhiaAereaOperada(new CompanhiaAerea(voo.getCiaOperadora().getCodigoIata()));
                } else {
                    vooDB.setCodgCompanhiaAereaOperada(new CompanhiaAerea(voo.getCiaMandatoria().getCodigoIata()));
                }
                vooDB.setFlagCodeShare(0);
                vooDB.setStatusVoo(voo.getStatus());
                vooDB.setLocalizadorCia(voo.getLocalizadorCia());
                vooDB.setCabine(voo.getCabine());
                if (trechoDB.getVoos() == null) {
                    trechoDB.setVoos(new ArrayList<>());
                }
                trechoDB.getVoos().add(vooDB);
            }
            reservaAereo.getTrechos().add(trechoDB);

        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String agenciaJson = objectMapper.writeValueAsString(reservaAereo);
        } catch (JsonProcessingException ex) {
            ex.printStackTrace();
        }
        reservaAereo.setFonte(preReserva.getFonte());
        return reservaAereo;
    }

    public void populaValorReservaPassageiroCiasDistintas(ReservaValoresAereo reservaAereoValores, Reserva reservaResp) {
        for (ValorPassageiro valorPassageiro : reservaResp.getValorReserva().getValorBase().getValorPassageiroList()) {
            for (Passageiro passageiro : reservaResp.getPassageiros()) {
                if (valorPassageiro.getNomePassageiro().contains(passageiro.getNome()) && valorPassageiro.getNomePassageiro().contains(passageiro.getSobrenome())) {
                    reservaAereoValores.setValorTarifa(valorPassageiro.getTarifa());
                    reservaAereoValores.setValorTarifaNet(valorPassageiro.getTarifa());
                    reservaAereoValores.setValorTaxaEmbarque(valorPassageiro.getTaxaEmbarque());
                    reservaAereoValores.setTaxaDu(valorPassageiro.getTaxaDU());
                }
            }

        }

    }

    private void popularValoresReserva(ReservaAereoModel reservaAerea, Reserva reservaApi) {
        if (reservaApi.getValorReserva() == null
                || reservaApi.getValorReserva().getValorBase() == null) {
            reservaAerea.setMsg("Não é possível carregar a reserva após o embarque.");
            return;
        }

        if (reservaAerea.getPassageiros() != null) {
            for (PassageiroModel paxModel : reservaAerea.getPassageiros()) {
                paxModel.setValores(new ArrayList<>());
            }
        }

        if (reservaApi.getValorReserva().getValorBase().getValorPassageiroList() != null
                && reservaAerea.getPassageiros() != null) {

            for (ValorPassageiro vPax :
                    reservaApi.getValorReserva().getValorBase().getValorPassageiroList()) {

                for (PassageiroModel paxModel : reservaAerea.getPassageiros()) {
                    if (vPax.getNomePassageiro() != null
                            && paxModel.getNome() != null
                            && paxModel.getSobrenome() != null
                            && vPax.getNomePassageiro().contains(paxModel.getNome())
                            && vPax.getNomePassageiro().contains(paxModel.getSobrenome())) {

                        ReservaValoresAereo valor = new ReservaValoresAereo();
                        valor.setValorTarifa(vPax.getTarifa());
                        valor.setTaxaDu(vPax.getTaxaDU());
                        valor.setValorTaxaEmbarque(vPax.getTaxaEmbarque());
                        valor.setTotalGeral(vPax.getTotal());
                        valor.setTaxaRav(vPax.getRAV());
                        valor.setTaxaRc(vPax.getRC());

                        paxModel.getValores().add(valor);
                        break;
                    }
                }
            }
        }

        reservaAerea.setValorTotalReserva(
                reservaApi.getValorReserva().getValorBase().getTotal()
        );
        reservaAerea.setTarifaGeral(
                reservaApi.getValorReserva().getValorBase().getTarifa()
        );
        reservaAerea.setTaxaEmbarqueGeral(
                reservaApi.getValorReserva().getValorBase().getTaxaEmbarque()
        );
        reservaAerea.setTaxaDUGeral(
                reservaApi.getValorReserva().getValorBase().getTaxaDU()
        );
        reservaAerea.setTaxaAssento(
                reservaApi.getValorReserva().getValorBase().getTaxaAssento()
        );
    }

    public void populaPassageirosToDB(ReservaValoresAereo reservaAereoValores,
                                      com.confApi.db.confManager.passageiro.Passageiro paxDB,
                                      Reserva reservaResp) {
        ReservaValor reservaValorDB = new ReservaValor();

        reservaValorDB.setPercMkp(reservaAereoValores.getPercMkp());
        reservaValorDB.setValorDu(reservaAereoValores.getTaxaDu());
        reservaValorDB.setValorMkp(reservaAereoValores.getValorMkp());
        reservaValorDB.setValorRav(reservaAereoValores.getTaxaRav());
        reservaValorDB.setValorRc(reservaAereoValores.getTaxaRc());
        reservaValorDB.setValorTarifa(reservaAereoValores.getValorTarifa());
        reservaValorDB.setValorTarifaNet(reservaAereoValores.getValorTarifaNet());
        reservaValorDB.setValorTaxaCombustivel(reservaAereoValores.getValorTxCombustivel());
        reservaValorDB.setValorTaxaEmbarque(reservaAereoValores.getValorTaxaEmbarque());
        reservaValorDB.setMoeda(reservaAereoValores.getMoeda());
        paxDB.getReservaValores().add(reservaValorDB);
    }

    private void popularPassageirosFromDB(ReservaAereo reservaDB, ReservaAereoModel reservaAerea) {
        if (reservaDB.getPassageiros() == null || reservaAerea.getPassageiros() == null) {
            return;
        }

        reservaAerea.setTaxaRAVGeral(0.0);
        reservaAerea.setTaxaRCGeral(0.0);

        for (com.confApi.db.confManager.passageiro.Passageiro passageiroDB : reservaDB.getPassageiros()) {
            if (passageiroDB == null) {
                continue;
            }

            for (PassageiroModel passageiroModel : reservaAerea.getPassageiros()) {
                if (passageiroModel == null || !isMesmoPassageiroFromDB(passageiroDB, passageiroModel)) {
                    continue;
                }

                passageiroModel.setCodgPassageiroDb(passageiroDB.getCodgPassageiro());

                popularContatoPassageiroFromDB(passageiroDB, passageiroModel);
                popularDocumentoPassageiroFromDB(passageiroDB, passageiroModel);
                popularSexoPassageiroFromDB(passageiroDB, passageiroModel);
                popularValoresPassageiroFromDB(passageiroDB, passageiroModel, reservaAerea);
                popularBilhetesPassageiroFromDB(passageiroDB, passageiroModel);
            }
        }
    }

    private void popularContatoPassageiroFromDB(
            com.confApi.db.confManager.passageiro.Passageiro passageiroDB,
            PassageiroModel passageiroModel
    ) {
        if (passageiroModel.getTelefone() == null) {
            ContatoModel contato = new ContatoModel();
            contato.setEmail(passageiroDB.getEmail());
            contato.setNumeroTelefone(passageiroDB.getCelular());
            passageiroModel.setTelefone(contato);
            return;
        }

        passageiroModel.getTelefone().setEmail(passageiroDB.getEmail());
        passageiroModel.getTelefone().setNumeroTelefone(passageiroDB.getCelular());
    }

    private void popularDocumentoPassageiroFromDB(
            com.confApi.db.confManager.passageiro.Passageiro passageiroDB,
            PassageiroModel passageiroModel
    ) {
        if (passageiroModel.getDocumento() == null) {
            DocumentoPassageiro documento = new DocumentoPassageiro();
            documento.setNumero(passageiroDB.getCpf());
            passageiroModel.setDocumento(documento);
            return;
        }

        if (passageiroDB.getCpf() != null) {
            passageiroModel.getDocumento().setNumero(passageiroDB.getCpf());
        }
    }

    private void popularSexoPassageiroFromDB(
            com.confApi.db.confManager.passageiro.Passageiro passageiroDB,
            PassageiroModel passageiroModel
    ) {
        if (passageiroDB.getSexo() == null) {
            return;
        }

        if (passageiroDB.getSexo() == 1) {
            passageiroModel.setSexo("M");
        } else {
            passageiroModel.setSexo("F");
        }
    }

    private void popularValoresPassageiroFromDB(
            com.confApi.db.confManager.passageiro.Passageiro passageiroDB,
            PassageiroModel passageiroModel,
            ReservaAereoModel reservaAerea
    ) {
        if (passageiroDB.getReservaValores() == null) {
            return;
        }

        for (ReservaValor reservaValorDB : passageiroDB.getReservaValores()) {
            ReservaValoresAereo reservaValoresAereo = new ReservaValoresAereo();

            reservaValoresAereo.setPercMkp(reservaValorDB.getPercMkp());
            reservaValoresAereo.setTaxaDu(reservaValorDB.getValorDu());
            reservaValoresAereo.setValorMkp(reservaValorDB.getValorMkp());
            reservaValoresAereo.setValorTarifa(reservaValorDB.getValorTarifa());
            reservaValoresAereo.setValorTarifaNet(reservaValorDB.getValorTarifaNet());
            reservaValoresAereo.setValorTaxaEmbarque(reservaValorDB.getValorTaxaEmbarque());
            reservaValoresAereo.setValorTxCombustivel(reservaValorDB.getValorTaxaCombustivel());
            reservaValoresAereo.setTaxaAssento(reservaValorDB.getValorAssento());

            if (passageiroModel.getValores() != null && !passageiroModel.getValores().isEmpty()) {
                reservaValoresAereo.setTaxaRav(passageiroModel.getValores().get(0).getTaxaRav());
                reservaValoresAereo.setTaxaRc(passageiroModel.getValores().get(0).getTaxaRc());
            } else {
                reservaValoresAereo.setTaxaRav(reservaValorDB.getValorRav());
                reservaValoresAereo.setTaxaRc(reservaValorDB.getValorRc());
            }

            reservaAerea.setTaxaRAVGeral(
                    reservaAerea.getTaxaRAVGeral() + getDoubleValue(reservaValoresAereo.getTaxaRav())
            );

            reservaAerea.setTaxaRCGeral(
                    reservaAerea.getTaxaRCGeral() + getDoubleValue(reservaValoresAereo.getTaxaRc())
            );

            if (getDoubleValue(reservaValoresAereo.getValorTxCombustivel()) > 0.0) {
                reservaAerea.setIsExibirTxCombustivel(true);
            }

            if (reservaAerea.getTaxaRAVGeral() > 0.0) {
                reservaAerea.setIsExibirRav(true);
            }

            if (reservaAerea.getTaxaRCGeral() > 0.0) {
                reservaAerea.setIsExibirRC(true);
            }

            passageiroModel.setValores(new ArrayList<>());
            passageiroModel.getValores().add(reservaValoresAereo);
        }
    }

    private Double getDoubleValue(Double valor) {
        return valor == null ? 0.0 : valor;
    }

    private void popularBilhetesPassageiroFromDB(
            com.confApi.db.confManager.passageiro.Passageiro passageiroDB,
            PassageiroModel passageiroModel
    ) {
        if (passageiroDB.getBilhetes() == null) {
            return;
        }

        if (passageiroModel.getBilhetes() == null) {
            passageiroModel.setBilhetes(new ArrayList<>());
        }

        for (BilheteAereo bilheteDB : passageiroDB.getBilhetes()) {
            if (bilheteDB == null || bilheteDB.getNumrBilhete() == null) {
                continue;
            }

            boolean existeBilhete = false;

            for (BilheteModel bilheteModel : passageiroModel.getBilhetes()) {
                if (bilheteModel.getNumeroBilhete() != null
                        && bilheteModel.getNumeroBilhete().equalsIgnoreCase(bilheteDB.getNumrBilhete())) {
                    existeBilhete = true;
                    break;
                }
            }

            if (existeBilhete) {
                continue;
            }

            BilheteModel bilheteModel = new BilheteModel();
            bilheteModel.setDataCancelamento(bilheteDB.getDataCancelamento());
            bilheteModel.setDataEmissao(bilheteDB.getDataEmissao());
            bilheteModel.setNumeroBilhete(bilheteDB.getNumrBilhete());

            if (bilheteDB.getStatus() != null && bilheteDB.getStatus() == 1) {
                bilheteModel.setStatus(StatusBilhete.Ativo.statusBilhete);
            } else if (bilheteDB.getStatus() != null && bilheteDB.getStatus() == 0) {
                bilheteModel.setStatus(StatusBilhete.Cancelado.statusBilhete);
            } else {
                bilheteModel.setStatus(StatusBilhete.Indefinido.statusBilhete);
            }

            passageiroModel.getBilhetes().add(bilheteModel);
        }
    }

    private boolean isMesmoPassageiroFromDB(
            com.confApi.db.confManager.passageiro.Passageiro passageiroDB,
            PassageiroModel passageiroModel
    ) {
        return passageiroDB.getNomePassageiro() != null
                && passageiroDB.getSobrenomePassageiro() != null
                && passageiroModel.getNome() != null
                && passageiroModel.getSobrenome() != null
                && passageiroModel.getNome().equalsIgnoreCase(passageiroDB.getNomePassageiro())
                && passageiroModel.getSobrenome().equalsIgnoreCase(passageiroDB.getSobrenomePassageiro());
    }

    private Date converterPrazoEmissao(String prazoEmissao) {
        String formattedDate = prazoEmissao.replaceAll(":(?=[0-9]{2}$)", "");

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        LocalDateTime ldt = LocalDateTime.parse(formattedDate, formatter);
        ZonedDateTime zdt = ldt.atZone(ZoneId.of("America/Sao_Paulo"));

        return Date.from(zdt.toInstant());
    }

    private String convertDataApi(Date data) {
        if (data == null) {
            return null;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        return data.toInstant()
                .atZone(ZoneId.of("America/Sao_Paulo"))
                .toLocalDate()
                .format(formatter);
    }
}
