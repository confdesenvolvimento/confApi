package com.confApi.rodoviario;

import com.confApi.db.confManager.rodoviario.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class RodoviarioReservaMapper {
    private final ObjectMapper json;

    public void validar(SalvarReservaRodoviarioRequest dados) {
        exigir(dados != null && dados.getReserva() != null && dados.getValores() != null, "Informe dadosManager.reserva e valores.");
        exigir(dados.getChaveIdempotencia() != null && dados.getChaveIdempotencia().matches("[a-zA-Z0-9._:-]{8,64}"), "Chave de idempotencia invalida.");
        validarValores(dados.getValores());
        exigir(dados.getValores().getCodgRodoviarioValor() == null && dados.getValores().getCodgReservaRodoviario() == null,
                "Valores devem ser de uma nova reserva.");
        var reserva = dados.getReserva();
        exigir(reserva.getCodgUsuarioCriacao() != null && reserva.getCodgUsuarioCriacao() > 0,
                "Informe dadosManager.reserva.codgUsuarioCriacao com o ID do usuario criador no Manager.");
        exigir(reserva.getCodgReservaRodoviario() == null && reserva.getCodgAgencia() != null && reserva.getCodgAgencia() > 0
                && reserva.getCodgSistema() != null && reserva.getCodgSistema() > 0, "Informe uma nova reserva com agencia e sistema do Manager.");
        exigir(dados.getTrechos() != null && !dados.getTrechos().isEmpty() && dados.getTrechos().size() <= 20, "Informe os metadados dos trechos.");
        for (var item : dados.getTrechos()) {
            exigir(item != null && item.getTrecho() != null, "Trecho invalido.");
            var t = item.getTrecho();
            exigir(t.getCodgRodoviarioTrecho() == null && t.getCodgReservaRodoviario() == null
                    && t.getCodgCidadeOrigem() != null && t.getCodgCidadeOrigem() > 0
                    && t.getCodgCidadeDestino() != null && t.getCodgCidadeDestino() > 0
                    && !t.getCodgCidadeOrigem().equals(t.getCodgCidadeDestino()), "Informe codgCidade de origem e destino para um novo trecho.");
            exigir(item.getPassageiros() != null && !item.getPassageiros().isEmpty() && item.getPassageiros().size() <= 50, "Informe os passageiros do trecho.");
            var nomes = new HashSet<String>();
            for (var pessoa : item.getPassageiros()) {
                exigir(pessoa != null && pessoa.getPassageiro() != null, "Passageiro invalido.");
                var p = pessoa.getPassageiro();
                exigir(p.getCodgPassageiro() == null && p.getCodgRodoviarioTrecho() == null && p.getSexo() != null
                        && p.getNomePassageiro() != null && !p.getNomePassageiro().isBlank()
                        && p.getSobrenomePassageiro() != null && !p.getSobrenomePassageiro().isBlank(), "Informe nome, sobrenome e sexo de cada novo passageiro.");
                exigir(p.getNomePassageiro().length() <= 150 && p.getSobrenomePassageiro().length() <= 150
                        && (p.getCpf() == null || p.getCpf().matches("[0-9]{11}"))
                        && (p.getDataNascimento() == null || !p.getDataNascimento().isAfter(java.time.LocalDate.now())), "Cadastro do passageiro invalido.");
                exigir(nomes.add(nome(p)), "Passageiros homonimos exigem conferencia manual.");
            }
        }
    }

    public SalvarReservaRodoviarioRequest mapearCidades(SalvarReservaRodoviarioRequest metadados, JsonNode compra,
                                                       List<RodoviarioDestinoDTO> destinos) {
        var dados = json.convertValue(metadados, SalvarReservaRodoviarioRequest.class);
        var fornecedor = compra.path("trechos");
        exigirFornecedor(fornecedor.isArray() && fornecedor.size() == dados.getTrechos().size(), "Trechos divergentes do fornecedor.");
        var usados = new HashSet<Integer>();
        var codigos = new HashMap<Integer, String>();
        for (var item : dados.getTrechos()) {
            var trecho = item.getTrecho();
            int encontrado = -1;
            Integer origem = null, destino = null;
            for (int i = 0; i < fornecedor.size(); i++) {
                if (usados.contains(i)) continue;
                var viagem = fornecedor.get(i);
                var origens = vinculos(destinos, trecho.getCodgCidadeOrigem(), viagem.path("origem").path("codigoFornecedor").asText());
                var chegadas = vinculos(destinos, trecho.getCodgCidadeDestino(), viagem.path("destino").path("codigoFornecedor").asText());
                if (!origens.isEmpty() && !chegadas.isEmpty()) {
                    exigirFornecedor(encontrado == -1, "Trechos ambiguos para as cidades informadas.");
                    exigirFornecedor(origens.size() == 1 && chegadas.size() == 1, "Multiplos vinculos para a mesma cidade e codigo do fornecedor.");
                    encontrado = i; origem = origens.iterator().next(); destino = chegadas.iterator().next();
                }
            }
            exigirFornecedor(encontrado != -1, "Origem/destino do fornecedor divergem das cidades informadas. Cidade de origem "
                    + trecho.getCodgCidadeOrigem() + "; cidade de destino " + trecho.getCodgCidadeDestino()
                    + ". Confira codgCidade no catalogo do Manager para os terminais retornados pelo Hub.");
            usados.add(encontrado);
            var viagem = fornecedor.get(encontrado);
            codigos.put(origem, viagem.path("origem").path("codigoFornecedor").asText());
            codigos.put(destino, viagem.path("destino").path("codigoFornecedor").asText());
            trecho.setCodgCidadeOrigem(origem); trecho.setCodgCidadeDestino(destino);
        }
        return mapear(dados, compra, codigos);
    }

    private Set<Integer> vinculos(List<RodoviarioDestinoDTO> destinos, Integer cidade, String codigo) {
        var encontrados = new HashSet<Integer>();
        for (var destino : destinos)
            if (Objects.equals(cidade, destino.codgCidade()) && Objects.equals(codigo, destino.codgDestinoSistema())
                    && destino.codgVinculoCidadeSistema() != null && destino.codgVinculoCidadeSistema() > 0)
                encontrados.add(destino.codgVinculoCidadeSistema());
        return encontrados;
    }

    public SalvarReservaRodoviarioRequest mapear(SalvarReservaRodoviarioRequest metadados, JsonNode compra,
                                                 Map<Integer, String> destinos) {
        var dados = json.convertValue(metadados, SalvarReservaRodoviarioRequest.class);
        var fornecedor = compra.path("trechos");
        exigirFornecedor(fornecedor.isArray() && fornecedor.size() == dados.getTrechos().size(), "Trechos divergentes do fornecedor.");
        var usados = new HashSet<Integer>();
        for (var item : dados.getTrechos()) {
            var t = item.getTrecho();
            JsonNode viagem = null;
            int indice = -1;
            for (int i = 0; i < fornecedor.size(); i++) {
                var candidato = fornecedor.get(i);
                if (!usados.contains(i) && Objects.equals(destinos.get(t.getCodgCidadeOrigem()), candidato.path("origem").path("codigoFornecedor").asText())
                        && Objects.equals(destinos.get(t.getCodgCidadeDestino()), candidato.path("destino").path("codigoFornecedor").asText())) {
                    exigirFornecedor(viagem == null, "Trechos ambiguos para os vinculos do Manager.");
                    viagem = candidato; indice = i;
                }
            }
            exigirFornecedor(viagem != null, divergenciaDestinos(t, destinos, fornecedor));
            usados.add(indice);
            String empresa = texto(viagem.path("viacao").path("nome"));
            t.setNomeEmpresa(empresa == null ? null : empresa.substring(0, Math.min(45, empresa.length())));
            for (var logo : viagem.path("viacao").path("logos")) {
                if (logo.isTextual() && logo.asText().length() <= 155 && logo.asText().matches("https?://[^\\s]+")) {
                    t.setUrlLogo(logo.asText()); break;
                }
            }
            preencherDatas(t, viagem);
            var passageiros = viagem.path("passageiros");
            exigirFornecedor(passageiros.isArray() && passageiros.size() == item.getPassageiros().size(), "Quantidade de passageiros divergente.");
            var pessoasUsadas = new HashSet<String>();
            for (var pessoa : item.getPassageiros()) {
                var p = pessoa.getPassageiro();
                var vivo = passageiro(p, passageiros);
                exigirFornecedor(pessoasUsadas.add(normalizar(vivo.path("nome").asText())), "Passageiro ambiguo.");
                String documento = texto(vivo.path("documento"));
                if ("CPF".equalsIgnoreCase(vivo.path("tipoDocumento").asText())) {
                    exigirFornecedor(documento != null && documento.matches("[0-9]{11}")
                            && (p.getCpf() == null || p.getCpf().equals(documento)), "CPF divergente.");
                    p.setCpf(documento);
                }
                if (vivo.hasNonNull("dataNascimento")) {
                    var nascimento = java.time.LocalDate.parse(vivo.get("dataNascimento").asText());
                    exigirFornecedor(p.getDataNascimento() == null || p.getDataNascimento().equals(nascimento), "Nascimento divergente.");
                    p.setDataNascimento(nascimento);
                }
                String poltrona = texto(vivo.path("poltrona"));
                exigirFornecedor(poltrona != null, "Fornecedor nao retornou o assento.");
                var assento = new RodoviarioAssentoDTO(); assento.setNumeroAssento(poltrona); pessoa.setAssento(assento);
                pessoa.setBilhetes(new ArrayList<>(List.of(bilhete(vivo))));
            }
        }
        String codigo = codigoCompra(compra);
        dados.getReserva().setLocalizador(codigo);
        dados.getReserva().setStatus(status(compra) == 1 ? 1 : 0);
        var pagamento = compra.path("pagamento");
        exigirFornecedor(pagamento.path("total").isNumber(), "Fornecedor nao retornou o total da compra.");
        BigDecimal total = pagamento.get("total").decimalValue();
        exigirFornecedor(total.signum() >= 0 && total.stripTrailingZeros().scale() <= 2, "Total do fornecedor invalido.");
        dados.getValores().setValorTotalNet(total);
        dados.getValores().setValorTarifaNet(valor(pagamento, "bilhetes"));
        dados.getValores().setValorTaxaEmbarque(valor(pagamento, "taxas"));
        dados.getValores().setValorSeguro(valor(pagamento, "seguro"));
        dados.getValores().setDemaisTaxas(valor(pagamento, "pedagio"));
        if (dados.getValores().getValorTotalMkp() == null) dados.getValores().setValorTotalMkp(total);
        return dados;
    }

    public JsonNode passageiro(RodoviarioPassageiroDTO p, JsonNode passageiros) {
        JsonNode encontrado = null;
        for (var candidato : passageiros) {
            if (nome(p).equals(normalizar(candidato.path("nome").asText()))) {
                exigirFornecedor(encontrado == null, "Passageiro ambiguo no fornecedor.");
                encontrado = candidato;
            }
        }
        exigirFornecedor(encontrado != null, "Passageiro ausente no fornecedor.");
        if (p.getCpf() != null && "CPF".equalsIgnoreCase(encontrado.path("tipoDocumento").asText()))
            exigirFornecedor(p.getCpf().equals(encontrado.path("documento").asText()), "CPF divergente na consulta do fornecedor.");
        if (p.getDataNascimento() != null && encontrado.hasNonNull("dataNascimento"))
            exigirFornecedor(p.getDataNascimento().toString().equals(encontrado.get("dataNascimento").asText()), "Nascimento divergente na consulta do fornecedor.");
        return encontrado;
    }

    public RodoviarioBilheteDTO bilhete(JsonNode passageiro) {
        var ticket = passageiro.path("bilhete");
        var dto = new RodoviarioBilheteDTO();
        dto.setNumerBilhete(texto(ticket.path("numero")));
        String situacao = ticket.path("situacao").asText();
        dto.setStatus("emitted".equals(situacao) && dto.getNumerBilhete() != null ? 1 : "canceled".equals(situacao) ? 2 : 0);
        return dto;
    }

    public int status(JsonNode compra) {
        int quantidade = 0;
        boolean emitidos = true, cancelados = true;
        for (var trecho : compra.path("trechos")) {
            if (!trecho.path("passageiros").isArray() || trecho.path("passageiros").isEmpty()) return 0;
            for (var p : trecho.path("passageiros")) {
                quantidade++;
                int status = bilhete(p).getStatus();
                emitidos &= status == 1; cancelados &= status == 2;
            }
        }
        if (quantidade == 0) return 0;
        if ("paid".equals(compra.path("situacao").asText()) && emitidos) return 1;
        if ("canceled".equals(compra.path("situacao").asText()) && cancelados) return 2;
        return 0;
    }

    public String codigoCompra(JsonNode compra) {
        var codigo = compra.path("codigoFornecedor");
        exigirFornecedor(codigo.isIntegralNumber() && codigo.canConvertToLong() && codigo.longValue() > 0, "Codigo de compra invalido no fornecedor.");
        return codigo.asText();
    }
    private void validarValores(RodoviarioValorDTO v) {
        var valores = Arrays.asList(v.getValorTarifa(), v.getValorTarifaNet(), v.getValorTaxaEmbarque(), v.getValorMkp(),
                v.getDemaisTaxas(), v.getValorSeguro(), v.getTaxaConveniencia(), v.getValorFee(), v.getValorTotalNet(), v.getValorTotalMkp());
        for (var valor : valores) exigir(valor == null || (valor.signum() >= 0 && valor.stripTrailingZeros().scale() <= 2
                && valor.compareTo(new BigDecimal("9999999999999.99")) <= 0), "Valor monetario invalido nos metadados.");
        exigir(v.getPercMkp() == null || (v.getPercMkp().signum() >= 0 && v.getPercMkp().stripTrailingZeros().scale() <= 4
                && v.getPercMkp().compareTo(new BigDecimal("99999.9999")) <= 0), "Percentual de markup invalido.");
        exigir(v.getFlagSeguroIncluido() == null || v.getFlagSeguroIncluido() == 0 || v.getFlagSeguroIncluido() == 1, "Indicador de seguro invalido.");
    }
    private BigDecimal valor(JsonNode p, String campo) { return p.path(campo).isNumber() ? p.path(campo).decimalValue() : BigDecimal.ZERO; }
    private void preencherDatas(RodoviarioTrechoDTO t, JsonNode viagem) {
        if (viagem.hasNonNull("partida")) {
            var data = LocalDateTime.parse(viagem.get("partida").asText()); t.setDataPartida(data.toLocalDate()); t.setHoraPartida(data.toLocalTime().withSecond(0).withNano(0).toString());
        }
        if (viagem.hasNonNull("chegada")) {
            var data = LocalDateTime.parse(viagem.get("chegada").asText()); t.setDataChegada(data.toLocalDate()); t.setHoraChegada(data.toLocalTime().withSecond(0).withNano(0).toString());
        }
    }
    private String texto(JsonNode n) { return n.isTextual() && !n.asText().isBlank() ? n.asText() : null; }
    private String divergenciaDestinos(RodoviarioTrechoDTO trecho, Map<Integer, String> destinos, JsonNode fornecedor) {
        var rotas = new ArrayList<String>();
        for (var viagem : fornecedor)
            rotas.add(codigoDestinoSeguro(viagem.path("origem").path("codigoFornecedor").asText())
                    + " -> " + codigoDestinoSeguro(viagem.path("destino").path("codigoFornecedor").asText()));
        return "Origem/destino do fornecedor divergem dos vinculos do Manager. Vinculo de origem "
                + trecho.getCodgCidadeOrigem() + " corresponde a " + codigoDestinoSeguro(destinos.get(trecho.getCodgCidadeOrigem()))
                + "; vinculo de destino " + trecho.getCodgCidadeDestino() + " corresponde a "
                + codigoDestinoSeguro(destinos.get(trecho.getCodgCidadeDestino()))
                + ". Trechos do fornecedor: " + String.join(", ", rotas) + ".";
    }
    private String codigoDestinoSeguro(String codigo) {
        return codigo != null && codigo.matches("(?:CIT|ROD)_[1-9][0-9]{0,18}") ? codigo : "codigo nao reconhecido";
    }
    private String nome(RodoviarioPassageiroDTO p) { return normalizar(p.getNomePassageiro() + " " + p.getSobrenomePassageiro()); }
    private String normalizar(String s) { return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT); }
    private void exigir(boolean ok, String motivo) {
        if (!ok) throw RodoviarioIntegracaoException.falha(400, "CONFAPI", "REQUEST_INVALIDO", motivo, false);
    }
    private void exigirFornecedor(boolean ok, String motivo) {
        if (!ok) throw RodoviarioIntegracaoException.falha(409, "CONFAPI", "CONFERENCIA_NECESSARIA", motivo, true);
    }
}
