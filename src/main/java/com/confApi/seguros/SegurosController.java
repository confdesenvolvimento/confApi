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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

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
            plano.setUrlLogo("https://portaldoagente.com.br/4/Recursos/Imagens/NovoLayout/logo_seguro/sistema_26.png");
            for (CoberturaSeguroDTO cs : plano.getCoberturasDetalhes()){
                cs.setIcone(cs.gerarIconePorNome(cs.getNome()));

            }
        }
        return resultado;
    }

    @PostMapping("/comprar")
    public SeguroReserva comprar(@RequestBody SeguroCompraModel req) {
        Recebimento recebimento = recebimentoService.criarRecebimento(req);


        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
try {


        String jsonRecebimento = mapper.writeValueAsString(recebimento);

        System.out.println(jsonRecebimento);
}catch (Exception e){
    e.printStackTrace();
}
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
      // return null;
    }
    @PostMapping("/carregarReserva")
    public SeguroReservaDTO carregarReserva(@RequestBody SeguroCarregarReservaDTO req) {
        if(req.getOperacao() == null){
            req.setOperacao("CONFIANCA");
        }
        List<SeguroReservaDTO> resultado = hubSeguroClient.carregarReserva(req);
        SeguroReservaDTO  response =  service.carregarReserva(req.getLocalizador());
        return response;
    }

    @PostMapping("/carregarReservas")
    public List<SeguroReserva> carregarReservas(@RequestBody FiltroReservaSeguro req) {
        List<SeguroReserva> response =  service.carregarReservas(req);
        return response;
    }

    @PostMapping("/cancelarReserva")
    public SeguroReservaDTO cancelarReserva(@RequestBody CancelamentoRequestDTO req) {
        List<SeguroReservaDTO> resultado = hubSeguroClient.cancelarReserva(req);
        if(resultado != null && !resultado.isEmpty()){
            service.cancelarReserva(req);
        }
        return new SeguroReservaDTO();
    }


    public static List<PlanoSeguroDTO> mockPlanosSeguro() {
        List<PlanoSeguroDTO> planos = new ArrayList<>();

        // =========================
        // PLANO 1 - HERO 30 BASIC
        // =========================
        PlanoSeguroDTO planoBasic = new PlanoSeguroDTO();
        planoBasic.setIdPlano("HERO30");
        planoBasic.setCodgFornecedor("6");
        planoBasic.setFornecedor("HERO");
        planoBasic.setNomePlano("HERO 30 BASIC");
        planoBasic.setPrecoBaseBRL(189.90);
        planoBasic.setPrecoFinalBRL(219.90);
        planoBasic.setValorCobertura(30000.00);
        planoBasic.setScore(70);
        planoBasic.setDataInicioCobertura("12/01/2026");
        planoBasic.setDataFinalCombertura("20/01/2026");

        planoBasic.getCoberturasDetalhes().add(
                cobertura(1, "Despesas Médicas e Hospitalares",
                        "Cobertura para atendimento médico e hospitalar",
                        30000.0, "USD", "pi pi-heart", "DMH até USD 30.000")
        );

        planoBasic.getCoberturasDetalhes().add(
                cobertura(2, "Invalidez por Acidente",
                        "Indenização em caso de invalidez permanente",
                        20000.0, "USD", "pi pi-user", null)
        );

        planoBasic.getCoberturasDetalhes().add(
                cobertura(3, "Extravio de Bagagem",
                        "Cobertura para bagagem extraviada",
                        1000.0, "USD", "pi pi-briefcase", null)
        );

        planos.add(planoBasic);

        // =========================
        // PLANO 2 - HERO 60 PLUS
        // =========================
        PlanoSeguroDTO planoPlus = new PlanoSeguroDTO();
        planoPlus.setIdPlano("HERO60");
        planoPlus.setCodgFornecedor("6");
        planoPlus.setFornecedor("HERO");
        planoPlus.setNomePlano("HERO 60 PLUS");
        planoPlus.setPrecoBaseBRL(289.90);
        planoPlus.setPrecoFinalBRL(329.90);
        planoPlus.setValorCobertura(60000.00);
        planoPlus.setScore(85);
        planoPlus.setDataInicioCobertura("12/01/2026");
        planoPlus.setDataFinalCombertura("20/01/2026");
        planoPlus.getCoberturasDetalhes().add(
                cobertura(1, "Despesas Médicas e Hospitalares",
                        "Cobertura médica completa",
                        60000.0, "USD", "pi pi-heart-fill", "Alta cobertura")
        );

        planoPlus.getCoberturasDetalhes().add(
                cobertura(2, "Despesas Odontológicas",
                        "Atendimento odontológico emergencial",
                        2000.0, "USD", "pi pi-smile", null)
        );

        planoPlus.getCoberturasDetalhes().add(
                cobertura(3, "Extravio de Bagagem",
                        "Indenização em caso de extravio",
                        1500.0, "USD", "pi pi-briefcase", null)
        );

        planoPlus.getCoberturasDetalhes().add(
                cobertura(4, "Cancelamento de Viagem",
                        "Reembolso por cancelamento",
                        3000.0, "USD", "pi pi-calendar-times", null)
        );

        planos.add(planoPlus);

        // =========================
        // PLANO 3 - HERO 100 PREMIUM
        // =========================
        PlanoSeguroDTO planoPremium = new PlanoSeguroDTO();
        planoPremium.setIdPlano("HERO100");
        planoPremium.setCodgFornecedor("6");
        planoPremium.setFornecedor("HERO");
        planoPremium.setNomePlano("HERO 100 PREMIUM");
        planoPremium.setPrecoBaseBRL(429.90);
        planoPremium.setPrecoFinalBRL(489.90);
        planoPremium.setValorCobertura(100000.00);
        planoPremium.setScore(95);
        planoPremium.setDataInicioCobertura("12/01/2026");
        planoPremium.setDataFinalCombertura("20/01/2026");

        planoPremium.getCoberturasDetalhes().add(
                cobertura(1, "Despesas Médicas e Hospitalares",
                        "Cobertura médica premium",
                        100000.0, "USD", "pi pi-heart-fill", "Top cobertura")
        );

        planoPremium.getCoberturasDetalhes().add(
                cobertura(2, "Covid-19",
                        "Cobertura para despesas relacionadas à Covid",
                        15000.0, "USD", "pi pi-shield", null)
        );

        planoPremium.getCoberturasDetalhes().add(
                cobertura(3, "Extravio de Bagagem",
                        "Cobertura ampliada de bagagem",
                        2500.0, "USD", "pi pi-briefcase", null)
        );

        planoPremium.getCoberturasDetalhes().add(
                cobertura(4, "Regresso Sanitário",
                        "Retorno ao país de origem",
                        50000.0, "USD", "pi pi-home", null)
        );

        planoPremium.getCoberturasDetalhes().add(
                cobertura(5, "Prática de Esportes",
                        "Cobertura para esportes amadores",
                        10000.0, "USD", "pi pi-bolt", "Opcional incluso")
        );

        planos.add(planoPremium);

        return planos;
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
