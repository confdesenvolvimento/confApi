package com.confApi.seguros;

import com.confApi.db.confManager.passageiro.Passageiro;
import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.db.confManager.seguro.apolice.SeguroApolice;
import com.confApi.db.confManager.seguro.reserva.DTO.CancelamentoRequestDTO;
import com.confApi.db.confManager.seguro.reserva.SeguroReserva;
import com.confApi.db.confManager.seguro.segurado.SeguroSegurado;
import com.confApi.hub.seguro.HubSeguroClient;
import com.confApi.model.RecebimentoModel;
import com.confApi.recebimento.PagamentoRedeEmConferencia;
import com.confApi.recebimento.RecebimentoService;
import com.confApi.seguros.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/seguro")
public class  SegurosController {

    private final SegurosService service;

    @Autowired
    private HubSeguroClient hubSeguroClient;
    @Autowired
    private RecebimentoService recebimentoService;

    public SegurosController(SegurosService service) {
        this.service = service;
    }

    @PostMapping("/pesquisar")
    public List<PlanoSeguroDTO> pesquisar(@RequestBody SeguroViagemPesquisaDTO req) {
//        List<PlanoSeguroDTO> resultado = mockPlanosSeguro();
        List<PlanoSeguroDTO> resultado = hubSeguroClient.pesquisarDisponibilidade(req);
        for (PlanoSeguroDTO plano : resultado) {
            plano.setUrlLogo("https://portaldoagente.com.br/4/Recursos/Imagens/NovoLayout/logo_seguro/sistema_26.png");
            for (CoberturaSeguroDTO cs : plano.getCoberturasDetalhes()){
                cs.setIcone(cs.gerarIconePorNome(cs.getNome()));

            }
        }
        return resultado;
    }

    @PostMapping("/comprar")
    public SeguroReserva comprar(@RequestBody SeguroCompraModel req) {
        validarIdentificacao(req);
        Recebimento recebimento = recebimentoService.criarRecebimento(req);
        if (recebimento != null && Integer.valueOf(3).equals(recebimento.getStatus())) {
            throw PagamentoRedeEmConferencia.doRecebimento(recebimento);
        }
        if(recebimento == null || recebimento.getCodgRecebimento() == null || !Integer.valueOf(1).equals(recebimento.getStatus())){
            return new SeguroReserva("ERRO: O pagamento não foi confirmado.");
        }
        req.setRecebimento(new RecebimentoModel(recebimento.getCodgRecebimento()));
        SeguroReserva seguroReserva;
        try {
            seguroReserva = service.comprar(req);
        } catch (RuntimeException erro) {
            throw PagamentoRedeEmConferencia.doRecebimento(recebimento);
        }
        if(seguroReserva == null || seguroReserva.getCodgReservaSeguro() == null){
            // A emissão ainda não foi enviada ao fornecedor. Preserve a compensação deste caso.
            recebimentoService.cancelarRecebimento(recebimento);
            throw PagamentoRedeEmConferencia.doRecebimento(recebimento);
        }
        List<SeguroReservaDTO> efetuarCompra;
        try {
            efetuarCompra = hubSeguroClient.efetuarReserva(req);
        } catch (RuntimeException erro) {
            throw PagamentoRedeEmConferencia.doRecebimento(recebimento);
        }

        if(efetuarCompra == null || efetuarCompra.isEmpty() || efetuarCompra.get(0) == null){
            throw PagamentoRedeEmConferencia.doRecebimento(recebimento);
        }

        SeguroReservaDTO retorno = efetuarCompra.get(0);
        seguroReserva.setLocalizador(retorno.getLocalizador());
        if (seguroReserva.getLocalizador() == null || seguroReserva.getLocalizador().isBlank()) {
            throw PagamentoRedeEmConferencia.doRecebimento(recebimento);
        }
        var divergencias = seguroReserva.getDivergenciasOperacao();
        com.confApi.operacao.DivergenciaOperacao.comparar(divergencias, "EMISSAO", "DATA_INICIO_COBERTURA",
                com.confApi.operacao.DivergenciaOperacao.dataUtc(seguroReserva.getDataInicioCobertura()),
                com.confApi.operacao.DivergenciaOperacao.dataUtc(retorno.getDataInicioCobertura()));
        com.confApi.operacao.DivergenciaOperacao.comparar(divergencias, "EMISSAO", "DATA_FINAL_COBERTURA",
                com.confApi.operacao.DivergenciaOperacao.dataUtc(seguroReserva.getDataFinalCobertura()),
                com.confApi.operacao.DivergenciaOperacao.dataUtc(retorno.getDataFinalCobertura()));
        com.confApi.operacao.DivergenciaOperacao.comparar(divergencias, "EMISSAO", "VALOR_TOTAL_NET",
                com.confApi.operacao.DivergenciaOperacao.valor(seguroReserva.getValorTotalReservaNet()),
                com.confApi.operacao.DivergenciaOperacao.valor(retorno.getValorTotalReservaNet()));
        if (retorno.getDataInicioCobertura() != null && retorno.getDataFinalCobertura() != null
                && !retorno.getDataInicioCobertura().after(retorno.getDataFinalCobertura())) {
            seguroReserva.setDataInicioCobertura(retorno.getDataInicioCobertura());
            seguroReserva.setDataFinalCobertura(retorno.getDataFinalCobertura());
        }
        List<SeguradoDTO> seguradosRetornados = retorno.getSegurados() == null ? List.of() : retorno.getSegurados();

        for (SeguroSegurado segurado : seguroReserva.getSeguradosList()) {
            SeguradoDTO seguradoDTO = seguradosRetornados
                    .stream()
                    .filter(java.util.Objects::nonNull)
                    .filter(x -> normalizarCpf(x.getCpf()).equals(normalizarCpf(segurado.getCpf())))
                    .findFirst()
                    .orElse(null);

            SeguradoDTO seguradoDTORequest = req.getSegurados()
                    .stream()
                    .filter(x -> normalizarCpf(x.getCpf()).equals(normalizarCpf(segurado.getCpf())))
                    .findFirst()
                    .orElse(null);

            if (seguradoDTO == null) {
                divergencias.add(com.confApi.operacao.DivergenciaOperacao.registrar(
                        "EMISSAO", "SEGURADOS", "Apólice esperada", "Segurado sem apólice correspondente no retorno"));
                continue;
            }

            SeguroApolice seguroApolice = new SeguroApolice(seguradoDTO);
            segurado.getApoliceList().add(seguroApolice);
            if(seguradoDTORequest != null && seguradoDTORequest.getCodgPassageiro() != null) {
                segurado.setCodgPassageiro(new Passageiro(seguradoDTORequest.getCodgPassageiro()));
            }
        }

        seguroReserva.setDivergenciasOperacao(divergencias.stream().distinct().toList());
        SeguroReserva atualizada = service.atualizarReserva(seguroReserva);
        if (!gravada(seguroReserva, atualizada)) atualizada = service.atualizarReserva(seguroReserva);
        if (!gravada(seguroReserva, atualizada)) {
            String localizador = seguroReserva.getLocalizador();
            String referencia = localizador.matches("[A-Za-z0-9_-]{1,85}") ? " " + localizador : "";
            return new SeguroReserva("ERRO: O fornecedor confirmou a apólice" + referencia
                    + ", mas a gravação no Manager está pendente. Confira a reserva original.");
        }
        seguroReserva.setDivergenciasOperacao(List.of());
        return seguroReserva;
//        return null;
    }
    private static boolean gravada(SeguroReserva pedido, SeguroReserva resposta) {
        return resposta != null && pedido.getCodgReservaSeguro().equals(resposta.getCodgReservaSeguro())
                && pedido.getLocalizador().equals(resposta.getLocalizador());
    }

    private void validarIdentificacao(SeguroCompraModel req) {
        if (req == null || req.getIdentificacaoAgenciaModel() == null
                || req.getIdentificacaoAgenciaModel().getCodgAgencia() == null
                || req.getIdentificacaoAgenciaModel().getCodgAgencia() <= 0) {
            throw new IdentificacaoSeguroInvalida("Informe uma agência válida antes de comprar o seguro.");
        }
        if (req.getIdentificacaoAgenciaModel().getCodgUsuario() == null
                || req.getIdentificacaoAgenciaModel().getCodgUsuario() <= 0) {
            throw new IdentificacaoSeguroInvalida("Informe um usuário válido antes de comprar o seguro.");
        }
    }

    @ExceptionHandler(IdentificacaoSeguroInvalida.class)
    public ResponseEntity<Map<String, String>> identificacaoInvalida(IdentificacaoSeguroInvalida error) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("codigo", "IDENTIFICACAO_SEGURO_INVALIDA", "mensagem", error.getMessage()));
    }

    private static class IdentificacaoSeguroInvalida extends RuntimeException {
        IdentificacaoSeguroInvalida(String mensagem) { super(mensagem); }
    }

    @PostMapping("/carregarReserva")
    public SeguroReservaDTO carregarReserva(@RequestBody SeguroCarregarReservaDTO req) {
        if(req.getOperacao() == null){
            req.setOperacao("CONFIANCA");
        }
        SeguroReservaDTO  response =  service.carregarReserva(req.getLocalizador());

       // System.out.println("response:: " + response);
        return response;
    }

    @PostMapping("/carregarReservas")
    public List<SeguroReserva> carregarReservas(@RequestBody FiltroReservaSeguro req) {
        List<SeguroReserva> response =  service.carregarReservas(req);
        return response;
    }

    @PostMapping("/cancelarReserva")
    public SeguroReservaDTO cancelarReserva(@RequestBody CancelamentoRequestDTO req) {
        List<SeguroReservaDTO> resultado;
        try {
            resultado = hubSeguroClient.cancelarReserva(req);
        } catch (RuntimeException erro) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "O fornecedor não confirmou o cancelamento do seguro. Consulte a reserva original.");
        }
        if (resultado == null || resultado.isEmpty() || resultado.stream().anyMatch(item ->
                item == null || item.getLocalizador() == null || item.getLocalizador().isBlank())) {
            return new SeguroReservaDTO();
        }
        try {
            service.cancelarReserva(req);
            SeguroReservaDTO confirmada = service.carregarReserva(req.getOperacao());
            if (confirmada != null && Integer.valueOf(2).equals(confirmada.getStatus())) return confirmada;
        } catch (RuntimeException erro) {
            // A tentativa do fornecedor já foi confirmada, portanto a baixa não deve ser repetida.
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "O fornecedor confirmou o cancelamento, mas o Manager não confirmou a baixa. Consulte a reserva original.");
    }



    private static CoberturaSeguroDTO cobertura(
            int order,
            String nome,
            String descricao,
            Double valor,
            String moeda,
            String icone,
            String obs) {

        CoberturaSeguroDTO c = new CoberturaSeguroDTO();
        c.setOrderDisplay(order);
        c.setNome(nome);
        c.setDescricao(descricao);
        c.setValor(valor);
        c.setMoeda(moeda);
        c.setIcone(icone);
        c.setObs(obs);
        return c;
    }

    private String normalizarCpf(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }


}
