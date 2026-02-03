package com.confApi.seguros.mapper;

import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.db.confManager.seguro.categoria.SeguroCategoria;
import com.confApi.db.confManager.seguro.cobertura.SeguroCobertura;
import com.confApi.db.confManager.seguro.coberturaDetalhada.SeguroCoberturaDetalhada;
import com.confApi.db.confManager.seguro.reserva.SeguroReserva;
import com.confApi.db.confManager.seguro.segurado.SeguroSegurado;
import com.confApi.db.confManager.sistema.Sistema;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.seguros.dto.CoberturaSeguroDTO;
import com.confApi.seguros.dto.SeguradoDTO;
import com.confApi.seguros.dto.SeguroCompraModel;
import com.confApi.seguros.dto.SeguroReservaDTO;
import com.confApi.seguros.util.DateUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class SeguroCompraToManagerMapper {

    private SeguroCompraToManagerMapper() {
    }


    public static SeguroReserva toReserva(SeguroCompraModel req, List<SeguroReservaDTO> seguroReservaDTOList) {
        SeguroReserva r = new SeguroReserva();

        // IDs básicos (vêm do "identificacaoAgenciaModel")
        if (req.getIdentificacaoAgenciaModel() != null) {

            // Usuario
            if (req.getIdentificacaoAgenciaModel().getCodgUsuario() != null) {
                Usuario u = new Usuario();
                u.setCodgUsuario(req.getIdentificacaoAgenciaModel().getCodgUsuario());
                r.setUsuario(u);
            }

            // Agencia
            if (req.getIdentificacaoAgenciaModel().getCodgAgencia() != null) {
                Agencia a = new Agencia();
                a.setCodgAgencia(req.getIdentificacaoAgenciaModel().getCodgAgencia());
                r.setAgencia(a);
            }

            if (req.getIdentificacaoAgenciaModel().getCodgErp() != null) {
                Sistema s = new Sistema();
                s.setCodgSistema(Integer.valueOf(req.getPlano().getCodgFornecedor() == null ? "6" : req.getPlano().getCodgFornecedor())); // <<< ajuste se o campo for outro
                r.setSistema(s);
            }
        }

        // Localizador: se ainda não existe (HERO normalmente retorna depois), gera um provisório
        r.setLocalizador(seguroReservaDTOList.get(0).getLocalizador());

        // Datas
        r.setDataCriacao(LocalDateTime.now());

        // Status (ajuste conforme sua regra)
        // Ex.: 0 = CRIADA, 1 = EMITIDA, 2 = CANCELADA...
        r.setStatus(seguroReservaDTOList.get(0).getStatus());
        r.setStatusPagamentoCliente(0);

        // Valores: usando plano do JSON
        if (req.getPlano() != null) {
            Double base = safeDouble(req.getPlano().getPrecoBaseBRL());
            Double fin = safeDouble(req.getPlano().getPrecoFinalBRL());

            r.setValorTotalReservaNet(base);
            r.setValorTotalReservaMarkup(round2(fin));
        }

        r.setContatoEmail(req.getContatoEmergencia().getEmail());
        r.setContatoTelefone(req.getContatoEmergencia().getTelefone());
        r.setContatoNome(req.getContatoEmergencia().getNome());
        r.setDataInicioCobertura(DateUtil.extractDataStringToDate(req.getPlano().getDataInicioCobertura()));
        r.setDataFinalCobertura(DateUtil.extractDataStringToDate(req.getPlano().getDataFinalCombertura()));

        // Observações (opcional): salvar um resumo do plano/forma pagto
        String obs = buildObs(req);
        r.setObservacaoInterna(obs);

        return r;
    }

    public static SeguroCobertura toCobertura(SeguroCompraModel req, SeguroReserva reservaSalva) {
        SeguroCobertura c = new SeguroCobertura();

        c.setSeguroReserva(reservaSalva);

        if (req.getPlano() != null) {
            // idCobertura/nomeCobertura: aqui você pode mapear do plano
            c.setIdCobertura(req.getPlano().getIdPlano());     // ex: "HERO60"
            c.setNomeCobertura(req.getPlano().getNomePlano()); // ex: "HERO 60 PLUS"

            // moeda / valorCobertura (se o plano tiver moedaCobertura e/ou um “valor principal”)
            c.setMoeda(req.getPlano().getMoedaCobertura());    // ex: "USD"

            // valorCobertura: se você quiser usar a principal DMH como "valorCobertura"
            c.setValorCobertura(extrairValorPrincipalCobertura(req)); // ex: 60000
            // valores de preço
            c.setValorSeguroNet(req.getPlano().getPrecoBaseBRL());
            c.setValorSeguroNetBrl(req.getPlano().getPrecoBaseBRL());

            // se precoFinalBRL = net + mkp
            Double net = nvl(req.getPlano().getPrecoBaseBRL());
            Double fin = nvl(req.getPlano().getPrecoFinalBRL());
            Double mkp = round2(fin - net);

            c.setValorSeguroMkp(mkp);
            c.setValorSeguroMkpBrl(mkp);

            // Se você quiser calcular % markup
            if (net > 0) {
                c.setPercMkp(round2((mkp / net) * 100.0));
            } else {
                c.setPercMkp(0.0);
            }

            c.setValorMkpAplicado(mkp);
            c.setValorMkpAplicadoBrl(mkp);

            // idGrupo: se existir no plano (no seu JSON não veio)
            // c.setIdGrupo(req.getPlano().getIdGrupo());
        }

        return c;
    }

    private static Double safeDouble(Double v) {
        return v == null ? 0.0 : v;
    }


    private static Date extractFimCobertura(SeguroCompraModel req) {
        // TODO: hoje não existe no seu JSON
        return null;
    }

    private static String buildObs(SeguroCompraModel req) {
        StringBuilder sb = new StringBuilder();

        if (req.getPlano() != null) {
            sb.append("Plano=").append(req.getPlano().getIdPlano())
                    .append(" / ").append(req.getPlano().getNomePlano())
                    .append(" / Fornecedor=").append(req.getPlano().getFornecedor())
                    .append(" / BRL Final=").append(req.getPlano().getPrecoFinalBRL());
        }

        if (req.getRecebimento() != null && req.getRecebimento().getFormaDePagamento() != null) {
            sb.append(" | Pagto=").append(req.getRecebimento().getFormaDePagamento().getNomeFormaPagto());
        }

        return sb.toString();
    }

    public static List<SeguroSegurado> toSegurados(SeguroCompraModel req, SeguroCobertura coberturaSalva, List<SeguroReservaDTO> seguroReservaDTOList) {
        List<SeguroSegurado> list = new ArrayList<>();
        if (req.getSegurados() == null) return list;

        for (SeguradoDTO s : req.getSegurados()) {

            String cpfBusca = s.getCpf() == null ? null : s.getCpf().replaceAll("\\D", "");
            SeguradoDTO seguradoDTOReserva = seguroReservaDTOList.get(0)
                    .getSegurados()
                    .stream()
                    .filter(x -> x.getCpf() != null
                            && x.getCpf().replaceAll("\\D", "").equals(cpfBusca))
                    .findFirst()
                    .orElse(null);

            SeguroSegurado ss = new SeguroSegurado();
            ss.setSeguroCobertura(coberturaSalva);

            // Nome
            String nome = nz(s.getNome());
            String sobrenome = nz(s.getSobrenome());
            ss.setPrimeiroNome(nome);
            ss.setUltimoNome(sobrenome);
            ss.setNomeCompleto((nome + " " + sobrenome).trim());

            // Sexo: seu JSON vem "F"/"M". Sua tabela é Integer.
            ss.setSexo(mapSexo(s.getSexo())); // ajuste conforme seu padrão

            // CPF, contato
            ss.setCpf(s.getCpf());
            ss.setTelefone(s.getTelefone());
            ss.setEmail(s.getEmail());

            // Data nascimento (você já tratou LocalDate -> Date; aqui assumo LocalDate no DTO)
            if (s.getNascimento() != null && !s.getNascimento().isEmpty()) {
                ss.setDataNascimento(DateUtil.extractDataStringToDate(s.getNascimento())); // helper abaixo
            }

            // Estado civil (não veio no JSON atual)
            ss.setEstadoCivil(null);

            // Endereço
            ss.setEnderecoCep(s.getCep());
            ss.setEnderecoEndereco(s.getEndereco());
            ss.setEnderecoNumero(s.getNumero());
            ss.setEnderecoComplemento(s.getComplemento());
            ss.setEnderecoBairro(s.getBairro());
            ss.setEnderecoCidade(s.getCidade());
            ss.setEnderecoEstado(s.getUf());
            ss.setLocalizador(seguradoDTOReserva == null ? null : seguradoDTOReserva.getLocalizador());

            list.add(ss);
        }

        return list;
    }

    /**
     * 3) Detalhes de cobertura (lista do plano.coberturasDetalhes)
     */
    public static List<SeguroCoberturaDetalhada> toCoberturaDetalhada(SeguroCompraModel req, SeguroCobertura coberturaSalva) {
        List<SeguroCoberturaDetalhada> list = new ArrayList<>();
        if (req.getPlano() == null || req.getPlano().getCoberturasDetalhes() == null) return list;

        for (CoberturaSeguroDTO cd : req.getPlano().getCoberturasDetalhes()) {
            SeguroCoberturaDetalhada d = new SeguroCoberturaDetalhada();
            d.setSeguroCobertura(coberturaSalva);

            // No seu DTO valor é Double (ex 60000.0) e na tabela é Integer.
            d.setValorCobertura(cd.getValor() == null ? null : cd.getValor().intValue());
            d.setMoedaCobertura(cd.getMoeda());

            // idCobertura: no seu JSON não existe um ID numérico da cobertura, só o "nome".
            // Sugestões:
            // 1) criar no DTO um "idCobertura" vindo do fornecedor
            // 2) se não tiver, salvar null ou gerar hash/ordem
            d.setIdCobertura(null);

            d.setTituloCobertura(cd.getNome());
            d.setDescricaoCobertura(cd.getDescricao());
            d.setIdOrdemExibicao(cd.getOrderDisplay());

            list.add(d);
        }

        return list;
    }

    /**
     * 4) Categoria (só se existir no seu JSON - não veio)
     */
    public static List<SeguroCategoria> toCategorias(SeguroCompraModel req, SeguroCobertura coberturaSalva) {
        List<SeguroCategoria> list = new ArrayList<>();

        // No JSON atual você não tem categoria/grupo estruturado.
        // Se futuramente vier (ex: cd.getGrupoId(), cd.getGrupoNome()) você popula aqui.

        return list;
    }

    // ===== Helpers =====

    private static Double nvl(Double v) {
        return v == null ? 0.0 : v;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static Double round2(Double v) {
        if (v == null) return 0.0;
        return Math.round(v * 100.0) / 100.0;
    }

    private static Double extrairValorPrincipalCobertura(SeguroCompraModel req) {
        // Regra: pega a cobertura "Despesas Médicas e Hospitalares" como principal
        if (req.getPlano() == null || req.getPlano().getCoberturasDetalhes() == null) return 0.0;

        return req.getPlano().getCoberturasDetalhes().stream()
                .filter(c -> c.getNome() != null && c.getNome().toLowerCase().contains("médic"))
                .map(CoberturaSeguroDTO::getValor)
                .filter(v -> v != null)
                .findFirst()
                .orElse(0.0);
    }

    private static Integer mapSexo(String sexo) {
        if (sexo == null) return null;
        // ajuste conforme seu padrão de banco:
        // 1 = M, 2 = F (exemplo)
        String s = sexo.trim().toUpperCase();
        if ("M".equals(s)) return 1;
        if ("F".equals(s)) return 2;
        return null;
    }
}
