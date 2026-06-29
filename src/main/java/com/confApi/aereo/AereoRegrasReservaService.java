package com.confApi.aereo;

import com.confApi.aereo.dto.ConsultarLocalizadorResponse;
import com.confApi.aereo.dto.Reserva;
import com.confApi.aereo.dto.ValorBase;
import com.confApi.aereo.dto.ValorReserva;
import com.confApi.aereo.dto.regrasAereas.RegrasAereasReservaResponse;
import com.confApi.db.confManager.regraAereaAlteracao.RegraAereaAlteracaoManagerService;
import com.confApi.db.confManager.regraAereaAlteracao.dto.RegraAereaAlteracaoConsultaRequest;
import com.confApi.db.confManager.regraAereaAlteracao.dto.RegraAereaAlteracaoConsultaResponse;
import com.confApi.db.confManager.regraAereaReembolso.RegraAereaReembolsoManagerService;
import com.confApi.db.confManager.regraAereaReembolso.dto.RegraAereaReembolsoConsultaRequest;
import com.confApi.db.confManager.regraAereaReembolso.dto.RegraAereaReembolsoConsultaResponse;
import com.confApi.db.confManager.familia.FamiliaService;
import com.confApi.db.confManager.familia.dto.FamiliaCompanhia;
import com.confApi.hub.aereo.dto.Companhia;
import com.confApi.hub.aereo.dto.TrechoReserva;
import com.confApi.hub.aereo.dto.Voo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AereoRegrasReservaService {

    private static final List<String> MOMENTOS_REEMBOLSO = Arrays.asList(
            "ANTES_EMBARQUE",
            "APOS_EMBARQUE",
            "NO_SHOW"
    );

    private static final List<String> MOMENTOS_ALTERACAO = Arrays.asList(
            "ANTES_EMBARQUE",
            "APOS_EMBARQUE",
            "NO_SHOW"
    );

    private static final Set<String> AEROPORTOS_BRASIL = new HashSet<>(Arrays.asList(
            "AJU", "BEL", "BPS", "BSB", "CGB", "CGR", "CNF", "CPV", "CWB", "CXJ",
            "FLN", "FOR", "GIG", "GRU", "GYN", "IGU", "IOS", "JDO", "JOI", "JPA",
            "LDB", "MAO", "MCZ", "MGF", "NAT", "NVT", "PMW", "POA", "PNZ", "PVH",
            "RAO", "RBR", "REC", "SDU", "SLZ", "SSA", "THE", "UDI", "VCP", "VIX",
            "AFL", "ATM", "BRA", "CAW", "CFB", "CMG", "FEN", "IMP", "IPN", "JTC",
            "MAB", "MCP", "OPS", "PET", "PPB", "RIA", "STM", "SJP", "TFF", "XAP"
    ));

    private final RegraAereaReembolsoManagerService reembolsoManagerService;
    private final RegraAereaAlteracaoManagerService alteracaoManagerService;
    private final FamiliaService familiaService;

    public AereoRegrasReservaService(RegraAereaReembolsoManagerService reembolsoManagerService,
                                     RegraAereaAlteracaoManagerService alteracaoManagerService,
                                     FamiliaService familiaService) {
        this.reembolsoManagerService = reembolsoManagerService;
        this.alteracaoManagerService = alteracaoManagerService;
        this.familiaService = familiaService;
    }

    public ConsultarLocalizadorResponse enriquecer(ConsultarLocalizadorResponse response) {
        if (response == null || response.getReservas() == null || response.getReservas().isEmpty()) {
            return response;
        }

        for (Reserva reserva : response.getReservas()) {
            reserva.setRegrasAereas(montarRegrasReserva(reserva));
        }

        return response;
    }

    private RegrasAereasReservaResponse montarRegrasReserva(Reserva reserva) {
        RegrasAereasReservaResponse regras = new RegrasAereasReservaResponse();
        RegraAereaReembolsoConsultaRequest baseRequest = montarRequestBase(reserva);

        if (baseRequest == null) {
            regras.setStatus("DADOS_INSUFICIENTES");
            regras.setMensagem("Nao foi possivel identificar companhia, familia, classe ou voo para consultar regra de reembolso.");
            return regras;
        }

        regras.setDadosReservaUtilizados(montarDadosReservaUtilizados(baseRequest));

        for (String momento : MOMENTOS_REEMBOLSO) {
            RegraAereaReembolsoConsultaRequest request = copiar(baseRequest);
            request.setMomento(momento);
            RegraAereaReembolsoConsultaResponse response = reembolsoManagerService.consultar(request);
            if (response != null) {
                regras.getReembolsos().add(response);
            }
        }

        RegraAereaAlteracaoConsultaRequest alteracaoBaseRequest = montarRequestAlteracao(baseRequest);
        for (String momento : MOMENTOS_ALTERACAO) {
            RegraAereaAlteracaoConsultaRequest request = copiar(alteracaoBaseRequest);
            request.setMomento(momento);
            RegraAereaAlteracaoConsultaResponse response = alteracaoManagerService.consultar(request);
            if (response != null) {
                regras.getAlteracoes().add(response);
            }
        }

        regras.setReembolso(escolherPrincipalReembolso(regras.getReembolsos()));
        regras.setAlteracao(escolherPrincipalAlteracao(regras.getAlteracoes()));
        regras.setStatus(resolverStatus(regras.getReembolsos(), regras.getAlteracoes()));
        regras.setMensagem(resolverMensagem(regras));
        return regras;
    }

    private RegraAereaReembolsoConsultaRequest montarRequestBase(Reserva reserva) {
        VooContexto contexto = primeiroVoo(reserva);
        if (contexto == null || contexto.getVoo() == null) {
            return null;
        }

        Voo voo = contexto.getVoo();
        String companhia = resolverCompanhia(voo, contexto.getTrecho());
        if (!temValor(companhia)) {
            return null;
        }

        RegraAereaReembolsoConsultaRequest request = new RegraAereaReembolsoConsultaRequest();
        request.setCompanhia(companhia);
        request.setMercado(resolverMercado(reserva));
        request.setCabine(primeiroValor(voo.getCabine()));
        request.setFamiliaTarifaria(primeiroValor(voo.getFamilia(), voo.getFamiliaCodigo()));
        request.setCodigoTarifario(primeiroValor(voo.getFamiliaCodigo(), voo.getBaseTarifaria()));
        request.setClasseReserva(primeiroValor(voo.getClasse(), primeiraLetra(voo.getBaseTarifaria())));
        aplicarDadosFamiliaCompanhia(request, resolverFamiliaCompanhia(request));
        request.setSistemaOrigem(reserva.getSistema());
        request.setQuantidadePassageiros(reserva.getPassageiros() == null ? 1 : Math.max(reserva.getPassageiros().size(), 1));
        request.setQuantidadeTrechos(Math.max(contarVoos(reserva), 1));
        preencherValores(request, reserva.getValorReserva());
        return request;
    }

    private void preencherValores(RegraAereaReembolsoConsultaRequest request, ValorReserva valorReserva) {
        if (valorReserva == null || valorReserva.getValorBase() == null) {
            return;
        }

        ValorBase valorBase = valorReserva.getValorBase();
        request.setValorTarifa(bigDecimal(valorBase.getTarifa()));
        request.setValorTaxaEmbarque(bigDecimal(valorBase.getTaxaEmbarque()));
        request.setValorTaxaDu(bigDecimal(valorBase.getTaxaDU()));
        request.setValorRav(bigDecimal(valorBase.getRAV()));
        request.setValorRc(bigDecimal(valorBase.getRC()));
        request.setValorTaxaAssento(bigDecimal(valorBase.getTaxaAssento()));
    }

    private RegraAereaAlteracaoConsultaRequest montarRequestAlteracao(RegraAereaReembolsoConsultaRequest origem) {
        RegraAereaAlteracaoConsultaRequest destino = new RegraAereaAlteracaoConsultaRequest();
        destino.setCompanhia(origem.getCompanhia());
        destino.setCodgFamiliaCompanhia(origem.getCodgFamiliaCompanhia());
        destino.setMercado(origem.getMercado());
        destino.setFamiliaTarifaria(origem.getFamiliaTarifaria());
        destino.setFamiliaTarifariaNormalizada(origem.getFamiliaTarifariaNormalizada());
        destino.setCodigoTarifario(origem.getCodigoTarifario());
        destino.setClasseReserva(origem.getClasseReserva());
        destino.setTipoEvento("REMARCACAO");
        aplicarCompanhiaDaFamilia(destino, origem);
        destino.setValorTarifa(origem.getValorTarifa());
        destino.setValorTotalReserva(somarValores(
                origem.getValorTarifa(),
                origem.getValorTaxaEmbarque(),
                origem.getValorTaxaDu(),
                origem.getValorRav(),
                origem.getValorRc(),
                origem.getValorTaxaAssento(),
                origem.getValorTaxaBagagem(),
                origem.getValorOutrasTaxas()
        ));
        return destino;
    }

    private void aplicarCompanhiaDaFamilia(RegraAereaAlteracaoConsultaRequest destino, RegraAereaReembolsoConsultaRequest origem) {
        FamiliaCompanhia familia = resolverFamiliaCompanhiaPorCodigo(origem.getCompanhia(), origem.getCodgFamiliaCompanhia());
        if (familia == null || familia.getCompanhiaAerea() == null) {
            return;
        }

        destino.setCodgCompanhiaAerea(familia.getCompanhiaAerea().getCodgCompanhiaAerea());
        destino.setCompanhia(primeiroValor(familia.getCompanhiaAerea().getIataCia(), destino.getCompanhia()));
    }

    private FamiliaCompanhia resolverFamiliaCompanhiaPorCodigo(String companhia, Integer codgFamiliaCompanhia) {
        if (codgFamiliaCompanhia == null) {
            return null;
        }

        for (FamiliaCompanhia familia : familiaService.findByNomeOuIataCia(companhia)) {
            if (familia != null && codgFamiliaCompanhia.equals(familia.getCodgFamiliaCompanhia())) {
                return familia;
            }
        }

        for (FamiliaCompanhia familia : familiaService.findByAll()) {
            if (familia != null && codgFamiliaCompanhia.equals(familia.getCodgFamiliaCompanhia())) {
                return familia;
            }
        }
        return null;
    }

    private VooContexto primeiroVoo(Reserva reserva) {
        if (reserva == null || reserva.getViagens() == null) {
            return null;
        }

        for (TrechoReserva trecho : reserva.getViagens()) {
            if (trecho == null || trecho.getVoos() == null || trecho.getVoos().isEmpty()) {
                continue;
            }
            for (Voo voo : trecho.getVoos()) {
                if (voo != null) {
                    return new VooContexto(trecho, voo);
                }
            }
        }
        return null;
    }

    private int contarVoos(Reserva reserva) {
        if (reserva == null || reserva.getViagens() == null) {
            return 0;
        }
        int total = 0;
        for (TrechoReserva trecho : reserva.getViagens()) {
            if (trecho != null && trecho.getVoos() != null) {
                total += trecho.getVoos().size();
            }
        }
        return total;
    }

    private String resolverCompanhia(Voo voo, TrechoReserva trecho) {
        String codigo = null;
        String descricao = null;

        Companhia companhia = voo.getCiaMandatoria();
        if (companhia == null && trecho != null) {
            companhia = trecho.getCompanhia();
        }
        if (companhia == null) {
            companhia = voo.getCiaOperadora();
        }
        if (companhia != null) {
            codigo = companhia.getCodigoIata();
            descricao = companhia.getDescricao();
        }

        String chave = primeiroValor(codigo, descricao);
        if (!temValor(chave)) {
            return null;
        }

        String normalizado = chave.trim().toUpperCase(Locale.ROOT);
        String descricaoNormalizada = descricao == null ? "" : descricao.toUpperCase(Locale.ROOT);
        if ("G3".equals(normalizado) || normalizado.contains("GOL") || descricaoNormalizada.contains("GOL")) {
            return "GOL";
        }
        if ("LA".equals(normalizado) || "JJ".equals(normalizado) || normalizado.contains("LATAM") || descricaoNormalizada.contains("LATAM")) {
            return "LATAM";
        }
        if ("AD".equals(normalizado) || normalizado.contains("AZUL") || descricaoNormalizada.contains("AZUL")) {
            return "AZUL";
        }
        return normalizado;
    }

    private String resolverMercado(Reserva reserva) {
        if (reserva == null || reserva.getViagens() == null || reserva.getViagens().isEmpty()) {
            return null;
        }

        boolean encontrouAeroporto = false;
        for (TrechoReserva trecho : reserva.getViagens()) {
            if (trecho == null) {
                continue;
            }
            String origem = trecho.getOrigem() == null ? null : trecho.getOrigem().getCodigoIata();
            String destino = trecho.getDestino() == null ? null : trecho.getDestino().getCodigoIata();
            if (temValor(origem)) {
                encontrouAeroporto = true;
                if (!AEROPORTOS_BRASIL.contains(origem.toUpperCase(Locale.ROOT))) {
                    return "INTERNACIONAL";
                }
            }
            if (temValor(destino)) {
                encontrouAeroporto = true;
                if (!AEROPORTOS_BRASIL.contains(destino.toUpperCase(Locale.ROOT))) {
                    return "INTERNACIONAL";
                }
            }
        }

        return encontrouAeroporto ? "NACIONAL" : null;
    }

    private RegraAereaReembolsoConsultaRequest copiar(RegraAereaReembolsoConsultaRequest origem) {
        RegraAereaReembolsoConsultaRequest destino = new RegraAereaReembolsoConsultaRequest();
        destino.setCompanhia(origem.getCompanhia());
        destino.setCodgFamiliaCompanhia(origem.getCodgFamiliaCompanhia());
        destino.setMercado(origem.getMercado());
        destino.setCabine(origem.getCabine());
        destino.setFamiliaTarifaria(origem.getFamiliaTarifaria());
        destino.setFamiliaTarifariaNormalizada(origem.getFamiliaTarifariaNormalizada());
        destino.setCodigoTarifario(origem.getCodigoTarifario());
        destino.setClasseReserva(origem.getClasseReserva());
        destino.setSistemaOrigem(origem.getSistemaOrigem());
        destino.setValorTarifa(origem.getValorTarifa());
        destino.setValorTaxaEmbarque(origem.getValorTaxaEmbarque());
        destino.setValorTaxaDu(origem.getValorTaxaDu());
        destino.setValorRav(origem.getValorRav());
        destino.setValorRc(origem.getValorRc());
        destino.setValorTaxaAssento(origem.getValorTaxaAssento());
        destino.setValorTaxaBagagem(origem.getValorTaxaBagagem());
        destino.setValorOutrasTaxas(origem.getValorOutrasTaxas());
        destino.setQuantidadePassageiros(origem.getQuantidadePassageiros());
        destino.setQuantidadeTrechos(origem.getQuantidadeTrechos());
        return destino;
    }

    private RegraAereaReembolsoConsultaResponse escolherPrincipalReembolso(List<RegraAereaReembolsoConsultaResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return null;
        }
        for (RegraAereaReembolsoConsultaResponse response : responses) {
            if (consultaValida(response)) {
                return response;
            }
        }
        return responses.get(0);
    }

    private RegraAereaAlteracaoConsultaResponse escolherPrincipalAlteracao(List<RegraAereaAlteracaoConsultaResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return null;
        }
        for (RegraAereaAlteracaoConsultaResponse response : responses) {
            if (consultaValida(response)) {
                return response;
            }
        }
        return responses.get(0);
    }

    private String resolverStatus(List<RegraAereaReembolsoConsultaResponse> reembolsos,
                                  List<RegraAereaAlteracaoConsultaResponse> alteracoes) {
        boolean semReembolso = reembolsos == null || reembolsos.isEmpty();
        boolean semAlteracao = alteracoes == null || alteracoes.isEmpty();
        if (semReembolso && semAlteracao) {
            return "NAO_CONSULTADO";
        }
        if (reembolsos != null) {
            for (RegraAereaReembolsoConsultaResponse response : reembolsos) {
                if (consultaValida(response)) {
                    return "CONSULTADO";
                }
            }
        }
        if (alteracoes != null) {
            for (RegraAereaAlteracaoConsultaResponse response : alteracoes) {
                if (consultaValida(response)) {
                    return "CONSULTADO";
                }
            }
        }
        if (temErroConsulta(reembolsos, alteracoes)) {
            return "ERRO_CONSULTA";
        }
        return "NAO_ENCONTRADO";
    }

    private boolean temErroConsulta(List<RegraAereaReembolsoConsultaResponse> reembolsos,
                                    List<RegraAereaAlteracaoConsultaResponse> alteracoes) {
        if (reembolsos != null) {
            for (RegraAereaReembolsoConsultaResponse response : reembolsos) {
                if (response != null && "ERRO_CONSULTA".equalsIgnoreCase(response.getStatus())) {
                    return true;
                }
            }
        }
        if (alteracoes != null) {
            for (RegraAereaAlteracaoConsultaResponse response : alteracoes) {
                if (response != null && "ERRO_CONSULTA".equalsIgnoreCase(response.getStatus())) {
                    return true;
                }
            }
        }
        return false;
    }

    private RegraAereaAlteracaoConsultaRequest copiar(RegraAereaAlteracaoConsultaRequest origem) {
        RegraAereaAlteracaoConsultaRequest destino = new RegraAereaAlteracaoConsultaRequest();
        destino.setCompanhia(origem.getCompanhia());
        destino.setCodgCompanhiaAerea(origem.getCodgCompanhiaAerea());
        destino.setCodgFamiliaCompanhia(origem.getCodgFamiliaCompanhia());
        destino.setMercado(origem.getMercado());
        destino.setFamiliaTarifaria(origem.getFamiliaTarifaria());
        destino.setFamiliaTarifariaNormalizada(origem.getFamiliaTarifariaNormalizada());
        destino.setCodigoTarifario(origem.getCodigoTarifario());
        destino.setClasseReserva(origem.getClasseReserva());
        destino.setTipoEvento(origem.getTipoEvento());
        destino.setMomento(origem.getMomento());
        destino.setValorTarifa(origem.getValorTarifa());
        destino.setValorNovaTarifa(origem.getValorNovaTarifa());
        destino.setValorTotalReserva(origem.getValorTotalReserva());
        return destino;
    }

    private boolean consultaValida(RegraAereaAlteracaoConsultaResponse response) {
        return response != null
                && response.getRegra() != null
                && response.getStatus() != null
                && !"NAO_ENCONTRADO".equalsIgnoreCase(response.getStatus())
                && !"ERRO_CONSULTA".equalsIgnoreCase(response.getStatus());
    }

    private String resolverMensagem(RegrasAereasReservaResponse regras) {
        if ("CONSULTADO".equals(regras.getStatus())) {
            return "Regras de reembolso e alteracao/remarcacao consultadas conforme os dados da reserva.";
        }
        if ("NAO_ENCONTRADO".equals(regras.getStatus())) {
            return "Nao encontrei regra compativel com a reserva carregada.";
        }
        if ("ERRO_CONSULTA".equals(regras.getStatus())) {
            return "A reserva foi carregada, mas houve falha ao consultar regras aereas.";
        }
        return "Regras aereas nao consultadas.";
    }

    private boolean consultaValida(RegraAereaReembolsoConsultaResponse response) {
        return response != null
                && response.getRegra() != null
                && response.getStatus() != null
                && !"NAO_ENCONTRADO".equalsIgnoreCase(response.getStatus())
                && !"ERRO_CONSULTA".equalsIgnoreCase(response.getStatus());
    }

    private Map<String, Object> montarDadosReservaUtilizados(RegraAereaReembolsoConsultaRequest request) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("companhia", request.getCompanhia());
        dados.put("codgFamiliaCompanhia", request.getCodgFamiliaCompanhia());
        dados.put("mercado", request.getMercado());
        dados.put("cabine", request.getCabine());
        dados.put("familiaTarifaria", request.getFamiliaTarifaria());
        dados.put("codigoTarifario", request.getCodigoTarifario());
        dados.put("classeReserva", request.getClasseReserva());
        dados.put("quantidadePassageiros", request.getQuantidadePassageiros());
        dados.put("quantidadeTrechos", request.getQuantidadeTrechos());
        dados.put("valorTarifa", request.getValorTarifa());
        dados.put("valorTaxaEmbarque", request.getValorTaxaEmbarque());
        dados.put("valorTotalReserva", somarValores(
                request.getValorTarifa(),
                request.getValorTaxaEmbarque(),
                request.getValorTaxaDu(),
                request.getValorRav(),
                request.getValorRc(),
                request.getValorTaxaAssento(),
                request.getValorTaxaBagagem(),
                request.getValorOutrasTaxas()
        ));
        return dados;
    }

    private FamiliaCompanhia resolverFamiliaCompanhia(RegraAereaReembolsoConsultaRequest request) {
        if (request == null || !temValor(request.getCompanhia())) {
            return null;
        }

        List<FamiliaCompanhia> familias = familiaService.findByNomeOuIataCia(request.getCompanhia());
        FamiliaCompanhia melhor = null;
        int melhorScore = -1;
        Integer tipoRota = tipoRotaMercado(request.getMercado());

        for (FamiliaCompanhia familia : familias) {
            int score = scoreFamiliaCompanhia(request, familia, tipoRota);
            if (score > melhorScore) {
                melhor = familia;
                melhorScore = score;
            }
        }

        return melhorScore > 0 ? melhor : null;
    }

    private void aplicarDadosFamiliaCompanhia(RegraAereaReembolsoConsultaRequest request, FamiliaCompanhia familia) {
        if (request == null || familia == null) {
            return;
        }

        request.setCodgFamiliaCompanhia(familia.getCodgFamiliaCompanhia());
        request.setCompanhia(primeiroValor(companhiaIata(familia), request.getCompanhia()));
        request.setMercado(primeiroValor(mercadoTipoRota(familia.getTipoRota()), request.getMercado()));
        request.setFamiliaTarifaria(primeiroValor(familia.getNomeFamiliaCompanhia(), request.getFamiliaTarifaria()));
        request.setFamiliaTarifariaNormalizada(normalizarChave(primeiroValor(
                familia.getNomeFamiliaCompanhia(),
                familia.getNomeFamiliaCompanhiaDescricao(),
                familia.getCodSigla(),
                request.getFamiliaTarifaria()
        )));
        request.setCodigoTarifario(primeiroValor(familia.getCodSigla(), request.getCodigoTarifario()));
    }

    private int scoreFamiliaCompanhia(RegraAereaReembolsoConsultaRequest request, FamiliaCompanhia familia, Integer tipoRota) {
        if (familia == null || !matchTipoRota(tipoRota, familia.getTipoRota())) {
            return -1;
        }

        int score = 0;
        if (temValor(request.getCodigoTarifario()) && mesmoTextoFlex(request.getCodigoTarifario(), familia.getCodSigla())) {
            score += 80;
        }
        if (temValor(request.getFamiliaTarifaria())
                && (mesmoTextoFlex(request.getFamiliaTarifaria(), familia.getNomeFamiliaCompanhia())
                || mesmoTextoFlex(request.getFamiliaTarifaria(), familia.getNomeFamiliaCompanhiaDescricao()))) {
            score += 40;
        }
        if (tipoRota != null && tipoRota.equals(familia.getTipoRota())) {
            score += 10;
        }
        return score;
    }

    private boolean matchTipoRota(Integer tipoRotaRequest, Integer tipoRotaFamilia) {
        return tipoRotaRequest == null || tipoRotaFamilia == null || tipoRotaFamilia == 0 || tipoRotaRequest.equals(tipoRotaFamilia);
    }

    private Integer tipoRotaMercado(String mercado) {
        String normalizado = normalizar(mercado);
        if ("AMBOS".equals(normalizado) || "AMBAS".equals(normalizado)) return 0;
        if (normalizado.contains("NACION")) return 1;
        if (normalizado.contains("INTERNACION")) return 2;
        return null;
    }

    private String mercadoTipoRota(Integer tipoRota) {
        if (tipoRota == null) return null;
        if (tipoRota == 0) return "AMBOS";
        if (tipoRota == 1) return "NACIONAL";
        if (tipoRota == 2) return "INTERNACIONAL";
        return null;
    }

    private String companhiaIata(FamiliaCompanhia familia) {
        if (familia == null || familia.getCompanhiaAerea() == null) {
            return null;
        }
        return primeiroValor(familia.getCompanhiaAerea().getIataCia(), familia.getCompanhiaAerea().getNomeCia());
    }

    private boolean mesmoTextoFlex(String a, String b) {
        if (!temValor(a) || !temValor(b)) return false;
        String na = normalizar(a);
        String nb = normalizar(b);
        return na.equals(nb) || na.contains(nb) || nb.contains(na);
    }

    private String normalizar(String value) {
        if (value == null) return "";
        String semAcento = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizarChave(String value) {
        String normalizado = normalizar(value);
        return normalizado.isEmpty() ? null : normalizado.replaceAll("[^A-Z0-9]+", "_").replaceAll("^_|_$", "");
    }

    private BigDecimal somarValores(BigDecimal... valores) {
        BigDecimal total = BigDecimal.ZERO;
        boolean temValor = false;
        if (valores != null) {
            for (BigDecimal valor : valores) {
                if (valor != null) {
                    total = total.add(valor);
                    temValor = true;
                }
            }
        }
        return temValor ? total : null;
    }

    private BigDecimal bigDecimal(Double valor) {
        if (valor == null || valor.isNaN() || valor.isInfinite()) {
            return null;
        }
        return BigDecimal.valueOf(valor);
    }

    private String primeiroValor(String... valores) {
        if (valores == null) {
            return null;
        }
        for (String valor : valores) {
            if (temValor(valor)) {
                return valor.trim();
            }
        }
        return null;
    }

    private String primeiraLetra(String valor) {
        if (!temValor(valor)) {
            return null;
        }
        return valor.trim().substring(0, 1);
    }

    private boolean temValor(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }

    private static class VooContexto {
        private final TrechoReserva trecho;
        private final Voo voo;

        private VooContexto(TrechoReserva trecho, Voo voo) {
            this.trecho = trecho;
            this.voo = voo;
        }

        private TrechoReserva getTrecho() {
            return trecho;
        }

        private Voo getVoo() {
            return voo;
        }
    }
}
