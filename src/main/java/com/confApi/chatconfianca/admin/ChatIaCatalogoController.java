package com.confApi.chatconfianca.admin;

import com.confApi.chatconfianca.v2.ChatV2Arguments;
import com.confApi.chatconfianca.v2.ChatV2Capability;
import java.util.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only metadata: does not instantiate executors or perform customer operations. */
@RestController
@RequestMapping("/v1/chat-confianca/admin/ia")
public class ChatIaCatalogoController {
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    private com.confApi.chatconfianca.hotel.ChatIaHotelService hotel;
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    private com.confApi.chatconfianca.hotel.ChatIaHotelReservaService hotelReserva;
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    private com.confApi.chatconfianca.hotel.ChatIaHotelListaService hotelLista;
    @GetMapping("/catalogo")
    public Map<String,Object> catalogo() {
        List<Map<String,Object>> itens=new ArrayList<>();
        for(ChatV2Capability c:ChatV2Capability.values()) {
            Map<String,Object> item=new LinkedHashMap<>();
            item.put("codigo",c.code);
            item.put("nome",c.code);
            item.put("tema",c.code.contains(".")?c.code.substring(0,c.code.indexOf('.')):"geral");
            item.put("descricao",c.description);
            String[] destino=destino(c);
            item.put("executor",destino[0]);
            item.put("metodo",destino[1]);
            item.put("tipo",tipo(c));
            item.put("disponivel",true); // technical implementation; not a runtime permission
            if(c==ChatV2Capability.HOTEL_DADOS) {
                item.put("cadastroAplicavel",true);
                item.put("estadoCadastro",hotel==null?"IMPLEMENTADO_DESABILITADO":hotel.estadoCadastro());
                item.put("observacaoConfiguracao","Requer V2 e hotel-dados habilitados + cadeia ativa no Manager. Perfil GERAL/hotel. Aplica orientações/restrições, objetivo, pergunta, limite de resultados/sugestões e oferta por interações. SQL até 2s; HTTP até 8s; geração até 15s. Confiança mínima não se aplica ao planejador semântico desta entrega. Departamento somente no handoff existente. Não executa fluxos arbitrários.");
            }
            if(c==ChatV2Capability.HOTEL_RESERVA) {
                item.put("cadastroAplicavel",true);
                item.put("estadoCadastro",hotelReserva==null?"IMPLEMENTADO_DESABILITADO":hotelReserva.estadoCadastro());
                item.put("observacaoConfiguracao","Leitura privada de reservas do Manager, sempre da agência autenticada. Requer V2, flags hotel-reserva e cadeia ativa. Aplica pergunta, limites, timeout e oferta por interações. Resposta factual fixa: textos de estilo/objetivo/restrições e confiança mínima NÃO alteram a resposta nesta entrega. Não revalida fornecedor/status, não emite voucher, não cancela. Sem cache compartilhado. IDs só do servidor.");
            }
            if(c==ChatV2Capability.HOTEL_LISTA){
                item.put("cadastroAplicavel",true);item.put("estadoCadastro",hotelLista==null?"IMPLEMENTADO_DESABILITADO":hotelLista.estadoCadastro());
                item.put("observacaoConfiguracao","Lista privada do Manager por agência, hóspede/hotel/cidade e período CRIACAO/ENTRADA/SAIDA. Sem período: últimos 30 dias de criação. Até 5 opções, paginação por data/ID. Aplica cadeia ativa, limite/sugestões e timeout; formato factual fixo. Textos livres, confiança mínima e oferta por contagem de interações não controlam este executor. Abrir item exige hotel-reserva habilitado e configurado. Sem status, fornecedor, cache compartilhado ou escritas.");
            }
            item.put("exigeConfirmacao",c.exigeConfirmacao());
            item.put("permissao","Autenticacao e validacoes do executor atual; agencia/unidade/usuario sempre obtidos da sessao. O cadastro nao concede permissoes.");
            var ferramenta=ChatV2Arguments.tool(c);
            item.put("parametros",c==ChatV2Capability.HOTEL_LISTA?com.confApi.chatconfianca.hotel.ChatIaHotelListaService.schema():c==ChatV2Capability.HOTEL_RESERVA?com.confApi.chatconfianca.hotel.ChatIaHotelReservaService.schema():c==ChatV2Capability.HOTEL_DADOS?com.confApi.chatconfianca.hotel.ChatIaHotelService.schema():ferramenta==null?Map.of():ferramenta.jsonSchema());
            itens.add(item);
        }
        return Map.of("versao","catalogo-ia-v1","cadastrosAplicadosAoChat",false,"acoes",itens);
    }
    private String tipo(ChatV2Capability c) {
        if(c==ChatV2Capability.HUMANO)return "ATENDIMENTO_HUMANO";
        if(c==ChatV2Capability.REMARCACAO||c==ChatV2Capability.ACOES_RESERVA)return "PREPARACAO_COM_CONFIRMACAO";
        if(c.tool!=null)return "FERRAMENTA";
        if(c.action!=null)return "CONSULTA";
        if(c==ChatV2Capability.AJUDA||c==ChatV2Capability.SAUDACAO)return "ORIENTACAO";
        return "CONHECIMENTO";
    }
    private String[] destino(ChatV2Capability c) {
        return switch(c) {
            case LIMITES -> new String[]{"LimitesService","consultaLimiteApi"};
            case FATURAS -> new String[]{"ChatService","montarMensagemFaturas"};
            case BOLETOS -> new String[]{"ChatService","montarMensagemFaturasBoleto"};
            case RESERVAS -> new String[]{"ChatService","responderListagemReservasRecentes"};
            case RESERVA, ACOES_RESERVA -> new String[]{"ChatService","carregarDadosReservaAerea"};
            case REGRAS -> new String[]{"ChatService","carregarReservaAereaComRegras"};
            case REMARCACAO -> new String[]{"ChatService","actionApis"};
            case CHECKIN -> new String[]{"ChatService","buscarCheckinsProximos"};
            case ALERTAS -> new String[]{"AlertaTarifaService","listarPorUsuario"};
            case FAMILIAS -> new String[]{"ChatService","listarFamilias"};
            case VOOS, HOTEL -> new String[]{"ToolRouter","execute"};
            case HOTEL_LISTA -> new String[]{"ChatIaHotelListaService","listarReservas"};
            case HOTEL_RESERVA -> new String[]{"ChatIaHotelReservaService","consultarReserva"};
            case HOTEL_DADOS -> new String[]{"ChatIaHotelService","consultarHotel"};
            case TARIFA_IDA -> new String[]{"MelhoresTarifasAereasService","consultar"};
            case TARIFA_VOLTA -> new String[]{"MelhoresTarifasAereasIdaVoltaService","consultar"};
            case PACOTE -> new String[]{"PacoteMelhorOfertaService","consultar"};
            case AJUDA, SAUDACAO, HUMANO -> new String[]{"ChatV2Executor","executar"};
            default -> new String[]{"ChatV2Executor","conhecimento"};
        };
    }
}
