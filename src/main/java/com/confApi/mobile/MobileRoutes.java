package com.confApi.mobile;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Catálogo fechado de famílias de operações consumidas pelo app; nunca recebe URL de destino. */
final class MobileRoutes {
    // Somente métodos/caminhos efetivamente usados pelos clientes mobile.
    private static final Map<String,List<String>> ROUTES=Map.of(
        "manager",List.of(
            "GET (?:unidade|familiaCompanhia|bandeira|formaPagamento/findByStatus|aeroporto/iataParametros|aeroporto/iata/[A-Z]{3}|api/cidade(?:/buscarPorNome)?|hotel/buscarByNome)",
            "GET (?:agencia/(?:unidade|findByNameAgenciaDTOid|agenciaId)|infoContato/unidade|parcelamentoBandeira/sigla)/[^/]+",
            "GET passageiroBase/search/[^/]+/[0-9]+/[0-9]+",
            "GET notificacao/(?:login/[^/]+(?:/lido/[01])?|[0-9]+)",
            "GET (?:token/[^/]+|tokenNumber/(?:number/)?[0-9]+)",
            "GET alertas/(?:ativos|usuariostatus/[0-9]+)","POST alertas","PUT alertas/desativar/[0-9]+",
            "GET reservaAereo/(?:[0-9]+|localizador/[^/]+|consultarReservas/localizador)",
            "PUT reservaAereo/(?:atualizarReserva/)?[0-9]+","POST reservaAereo/saveapp","PUT bilheteAereo/cancelar/[^/]+",
            "POST reservaHotel(?:/consultarReservasGeral)?","GET reservaHotel/localizador/[^/]+","PUT reservaHotel/cancelar/[0-9]+",
            "POST reservaTrecho/(?:checkinApp|countcheckin)","GET reservaTrecho/consultarstatusvooapp/[^/]+",
            "POST historicoReserva/gravar","POST recebimento/[^/]+","PUT recebimento/cancelarRecebimento/[0-9]+",
            "GET api/plantao/atendimento/solicitanteloginApp/[^/]+",
            "GET api/orcamento(?:/(?:[0-9]+|usuario/[0-9]+))?","POST api/orcamento(?:/filter|/status/[0-9]+/[0-9]+)?","PUT api/orcamento/[0-9]+"),
        "hub",List.of("POST api/(?:plantao/senha|token/generate)"),
        "confapi",List.of(
            "POST v[12]/aereo/(?:pesquisar|reservar|carregarReserva|mapaDeAssentos|marcarAssento|iniciarEmissao|tarifar|tarifarPesquisa|emitir|cancelarReserva|cancelarBilhete|obterParcelas)",
            "POST v2/aereo/(?:emitir/(?:true|false)|cancelarReservaRequest)",
            "GET v1/hoteis/(?:cidades|pesquisas/[^/]+)",
            "POST v1/hoteis/(?:pesquisar|pesquisas(?:/[^/]+/(?:cancelar|conteudo))?|efetuarReserva|carregarReserva|cancelaHotel)",
            "POST v1/carro/(?:pesquisar|selecionarCarro|reservar|consultarReserva|cancelarReserva)","GET v1/carro/findAllReservas",
            "POST v1/seguro/(?:pesquisar|comprar|carregarReservas|carregarReserva|cancelarReserva)",
            "GET (?:v1/bannerapp/ativos|v1/novidade|manager/textoBase)","POST api/geradorPDF/reservaAereoApp",
            "POST v1/(?:ai/(?:stt|sttrequest|conversation/chat)|ia/assistant/field)",
            "GET v1/chat-confianca/(?:sessao/[0-9]+|departamentos(?:-opcoes)?/usuario/[0-9]+|historico/solicitante/[0-9]+|conversas/[^/]+(?:/mensagens)?|remarcacoes/(?:reservas-emitidas|[0-9]+)|anexos/[^/]+/download|notificacoes/resumo/[0-9]+)",
            "POST v1/chat-confianca/(?:conversas(?:/encerrar)?|remarcacoes/iniciar|remarcacoes/[^/]+/(?:trecho|passageiros|pesquisar|simular|forma-pagamento|encaminhar|voltar)|mensagens|ia/(?:perguntar|encaminhar-atendente)|leituras|anexos|avaliacoes|conversas/[^/]+/faturas/[^/]+/documentos/[^/]+/download)"),
        "cache",List.of("POST CacheAereo/(?:buscarTarifas|disponibilidadewooba|aereo/melhores-ofertas(?:/ida-volta)?)","GET cacheHotel/oportunidades(?:/pacotes)?"),
        "cservice",List.of("POST api/token/(?:(?:mobile/)?gerartoken/[^/]+/[0-9]+|mobile/status)"),
        "email",List.of("GET sendSolicitacaoAcesso/[0-9]+"));
    static void require(String service,String method,String path,String query) {
        if(path.isBlank()||path.startsWith("/")||path.contains("\\")||path.contains(";")||path.contains("//"))throw new MobileFailure(404,"ROTA_INDISPONIVEL");
        String decoded;
        try{decoded=URLDecoder.decode(path,StandardCharsets.UTF_8);}catch(Exception e){throw new MobileFailure(400,"ROTA_INVALIDA");}
        if(decoded.contains("..")||decoded.contains("\\")||decoded.contains("%")||decoded.chars().anyMatch(Character::isISOControl)||!decoded.equals(path)&&decoded.chars().filter(c->c=='/').count()!=path.chars().filter(c->c=='/').count())throw new MobileFailure(400,"ROTA_INVALIDA");
        if(ROUTES.getOrDefault(service,List.of()).stream().noneMatch(pattern->(method+" "+path).matches(pattern)))throw new MobileFailure(404,"ROTA_INDISPONIVEL");
        if(query!=null&&(query.length()>8000||query.chars().anyMatch(Character::isISOControl)))throw new MobileFailure(400,"CONSULTA_INVALIDA");
    }
    static void identity(String service,String method,String path,JsonNode node,MobileSessions.Session session) {
        JsonNode actor=node;
        // O criador histórico de uma reserva não é a identidade de quem a está operando.
        // Os serviços de reservas continuam responsáveis pelas permissões de acesso à reserva/agência.
        if(node!=null&&node.isObject()) {
            var copy=(com.fasterxml.jackson.databind.node.ObjectNode)node.deepCopy();
            if("confapi".equals(service)&&"POST".equals(method)&&path.matches("v2/aereo/emitir/(?:true|false)")&&node.path("codgReservaAereoDB").asLong()>0)copy.remove("usuarioCriacao2");
            if("manager".equals(service)&&"PUT".equals(method)&&path.matches("reservaAereo/[0-9]+"))copy.remove("codgUsuarioCriacao");
            actor=copy;
        }
        identity(actor,session);
    }
    static void identity(JsonNode node,MobileSessions.Session session) {
        if(node==null)return;
        if(node.isObject()) {
            var fields=node.fields();while(fields.hasNext()) {
                var entry=fields.next();String key=entry.getKey();JsonNode value=entry.getValue();
                if(value.isValueNode()&&!value.isNull()) {
                    if(Set.of("gestor","podeVerInternas","interna").contains(key)&&value.asBoolean(false))throw new MobileFailure(403,"PERMISSAO_INSUFICIENTE");
                    if("usuarioCodg".equals(key)&&value.asLong(-1)!=session.profile.path("codigo").asLong(-2))throw new MobileFailure(403,"USUARIO_DIVERGENTE");
                    if(Set.of("loginUsuario","usuarioLoginAcesso","loginSolicitante","usuarioLogin").contains(key)&&!session.login.equalsIgnoreCase(value.asText()))throw new MobileFailure(403,"USUARIO_DIVERGENTE");
                    if(Set.of("userId","usuarioId","codgUsuario").contains(key)&&value.asLong(-1)!=session.profile.path("id").asLong(-2))throw new MobileFailure(403,"USUARIO_DIVERGENTE");
                }
                identity(value,session);
            }
        }else if(node.isArray())for(JsonNode value:node)identity(value,session);
    }
    static void ownPath(String service,String path,String query,MobileSessions.Session session) {
        for(String prefix:List.of("notificacao/login/","plantao/atendimento/solicitanteloginApp/","api/plantao/atendimento/solicitanteloginApp/"))
            if(path.startsWith(prefix)&&!session.login.equalsIgnoreCase(URLDecoder.decode(path.substring(prefix.length()).split("/")[0],StandardCharsets.UTF_8)))throw new MobileFailure(403,"USUARIO_DIVERGENTE");
        if("confapi".equals(service)&&path.matches("v1/chat-confianca/(?:sessao|departamentos(?:-opcoes)?/usuario|historico/solicitante|notificacoes/resumo)/[0-9]+")&&!path.endsWith("/"+session.profile.path("id").asText()))throw new MobileFailure(403,"USUARIO_DIVERGENTE");
        if("manager".equals(service)&&path.matches("passageiroBase/search/[^/]+/[0-9]+/[0-9]+")&&!path.split("/")[3].equals(session.profile.path("id").asText()))throw new MobileFailure(403,"USUARIO_DIVERGENTE");
        if("manager".equals(service)&&path.matches("(?:alertas/usuariostatus|api/orcamento/usuario)/[0-9]+")&&!path.endsWith("/"+session.profile.path("id").asText()))throw new MobileFailure(403,"USUARIO_DIVERGENTE");
        if("cservice".equals(service)&&path.contains("gerartoken/")) {
            String key=path.substring(path.indexOf("gerartoken/")+11).split("/")[0];
            if(!key.equals(session.profile.path("tokenAcesso").asText(session.profile.path("tokenIdentificacao").asText())))throw new MobileFailure(403,"TOKEN_DE_OUTRO_USUARIO");
        }
        if(query!=null)for(String item:query.split("&")) {
            String[] pair=item.split("=",2);if(pair.length!=2)continue;
            String key=URLDecoder.decode(pair[0],StandardCharsets.UTF_8),value=URLDecoder.decode(pair[1],StandardCharsets.UTF_8);
            if(Set.of("gestor","podeVerInternas").contains(key)&&!"false".equals(value))throw new MobileFailure(403,"PERMISSAO_INSUFICIENTE");
            if(Set.of("username","login","loginUsuario").contains(key)&&!session.login.equalsIgnoreCase(value))throw new MobileFailure(403,"USUARIO_DIVERGENTE");
            if(Set.of("codgUsuario","usuarioId","userId").contains(key)&&!session.profile.path("id").asText().equals(value))throw new MobileFailure(403,"USUARIO_DIVERGENTE");
        }
    }
}
