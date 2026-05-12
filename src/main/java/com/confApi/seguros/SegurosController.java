package com.confApi.seguros;

import com.confApi.db.confManager.passageiro.Passageiro;
import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.db.confManager.seguro.apolice.SeguroApolice;
import com.confApi.db.confManager.seguro.reserva.DTO.CancelamentoRequestDTO;
import com.confApi.db.confManager.seguro.reserva.SeguroReserva;
import com.confApi.db.confManager.seguro.segurado.SeguroSegurado;
import com.confApi.hub.seguro.HubSeguroClient;
import com.confApi.model.RecebimentoModel;
import com.confApi.recebimento.RecebimentoService;
import com.confApi.seguros.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

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
            plano.setUrlLogo("");
        }
        return resultado;
    }

    @PostMapping("/comprar")
    public SeguroReserva comprar(@RequestBody SeguroCompraModel req) {
        Recebimento recebimento = recebimentoService.criarRecebimento(req);
        if(recebimento.getCodgRecebimento() == null || recebimento.getStatus() != 1){
            return new SeguroReserva("ERRO: Não foi possivel efetuar o pagamento, tente novamente mais tarde.");
        }
        req.setRecebimento(new RecebimentoModel(recebimento.getCodgRecebimento()));
        SeguroReserva seguroReserva = service.comprar(req);
        if(seguroReserva.getCodgReservaSeguro() == null){
            Recebimento recebimentoErro = recebimentoService.cancelarRecebimento(recebimento);
            return new SeguroReserva("ERRO: Não foi possivel gravar o seguro no banco de dados, tente novamente mais tarde.");
        }
        List<SeguroReservaDTO> efetuarCompra = hubSeguroClient.efetuarReserva(req);

        if(efetuarCompra == null || efetuarCompra.isEmpty()){
            Recebimento recebimentoErro = recebimentoService.cancelarRecebimento(recebimento);
            service.cancelarReserva(new CancelamentoRequestDTO(seguroReserva));
            return new SeguroReserva("ERRO: Não foi possivel gravar o seguro no banco de dados, tente novamente mais tarde.");
        }

        seguroReserva.setLocalizador(efetuarCompra.get(0).getLocalizador());

        for (SeguroSegurado segurado : seguroReserva.getSeguradosList()) {
            SeguradoDTO seguradoDTO = efetuarCompra.get(0)
                    .getSegurados()
                    .stream()
                    .filter(x -> normalizarCpf(x.getCpf()).equals(normalizarCpf(segurado.getCpf())))
                    .findFirst()
                    .orElse(null);

            SeguradoDTO seguradoDTORequest = req.getSegurados()
                    .stream()
                    .filter(x -> normalizarCpf(x.getCpf()).equals(normalizarCpf(segurado.getCpf())))
                    .findFirst()
                    .orElse(null);

            if (seguradoDTO == null) {
                throw new RuntimeException("Segurado não encontrado no retorno da reserva para CPF: " + segurado.getCpf());
            }

            SeguroApolice seguroApolice = new SeguroApolice(seguradoDTO);
            segurado.getApoliceList().add(seguroApolice);
            if(seguradoDTORequest != null && seguradoDTORequest.getCodgPassageiro() != null) {
                segurado.setCodgPassageiro(new Passageiro(seguradoDTORequest.getCodgPassageiro()));
            }
        }

        service.atualizarReserva(seguroReserva);
        return seguroReserva;
//        return null;
    }
    @PostMapping("/carregarReserva")
    public SeguroReservaDTO carregarReserva(@RequestBody SeguroCarregarReservaDTO req) {
        if(req.getOperacao() == null){
            req.setOperacao("CONFIANCA");
        }
        SeguroReservaDTO  response =  service.carregarReserva(req.getLocalizador());

        System.out.println("response:: " + response);
        return response;
    }

    @PostMapping("/carregarReservas")
    public List<SeguroReserva> carregarReservas(@RequestBody FiltroReservaSeguro req) {
        List<SeguroReserva> response =  service.carregarReservas(req);
        return response;
    }

    @PostMapping("/cancelarReserva")
    public SeguroReservaDTO cancelarReserva(@RequestBody CancelamentoRequestDTO req) {

        System.out.println("req:: " + req);
        List<SeguroReservaDTO> resultado = hubSeguroClient.cancelarReserva(req);
        System.out.println("resultado:: " + resultado);
        if(resultado != null && !resultado.isEmpty()){
            service.cancelarReserva(req);
            return service.carregarReserva(req.getOperacao());
        }
        return new SeguroReservaDTO();
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
