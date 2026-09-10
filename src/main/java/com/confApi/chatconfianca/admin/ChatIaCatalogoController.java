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
            item.put("disponivel",true);
            item.put("exigeConfirmacao",c.exigeConfirmacao());
            item.put("permissao","Autenticacao e validacoes do executor atual; agencia/unidade/usuario sempre obtidos da sessao. O cadastro nao concede permissoes.");
            var ferramenta=ChatV2Arguments.tool(c);
            item.put("parametros",ferramenta==null?Map.of():ferramenta.jsonSchema());
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
            case TARIFA_IDA -> new String[]{"MelhoresTarifasAereasService","consultar"};
            case TARIFA_VOLTA -> new String[]{"MelhoresTarifasAereasIdaVoltaService","consultar"};
            case PACOTE -> new String[]{"PacoteMelhorOfertaService","consultar"};
            case AJUDA, SAUDACAO, HUMANO -> new String[]{"ChatV2Executor","executar"};
            default -> new String[]{"ChatV2Executor","conhecimento"};
        };
    }
}
