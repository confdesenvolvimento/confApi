package com.confApi.chatconfianca.contexto;

import com.confApi.chatconfianca.dto.model.Mensagem;
import com.confApi.chatconfianca.dto.request.PerguntarConfiaRequest;
import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.chatconfianca.financeiro.ChatDocumentoFinanceiroService;
import com.confApi.chatconfianca.reservas.ChatReservaConsultaClient;
import com.confApi.chatconfianca.reservas.ChatReservaConsultaParser;
import com.confApi.chatconfianca.reservas.ChatReservaConsultaTexto;
import com.confApi.chatconfianca.reservas.ReservaConsultaFiltros;
import com.confApi.chatconfianca.service.ChatContextualAcessoService;
import com.confApi.chatconfianca.service.ChatPedidoOperacional;
import com.confApi.chatgpt.dto.ChatActionDTO;
import com.confApi.chatgpt.dto.ChatActionDestinoDTO;
import com.confApi.chatgpt.dto.ChatMessageDTO;
import com.confApi.chatgpt.dto.ChatResponseDTO;
import com.confApi.chatgpt.util.LocalizadorAereo;
import com.confApi.db.confManager.faturas.dto.FaturaSicaRS;
import com.confApi.exception.RegraDeNegocioException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Copiloto factual: contexto e uma referencia nao confiavel, nunca dados prontos ou permissao. */
@Service
public class ChatAjudaContextualService {
    private final ChatContextualAcessoService acesso;
    private final ChatReservaConsultaClient reservas;
    private final ChatDocumentoFinanceiroService documentos;
    private final ObjectMapper mapper;
    @Value("${chat-confianca.ajuda-contextual.enabled:true}")
    private boolean enabled=true;
    private ChatReservaCopilotoService copiloto;
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    public void setCopiloto(ChatReservaCopilotoService copiloto) { this.copiloto=copiloto; }

    public ChatAjudaContextualService(ChatContextualAcessoService acesso,ChatReservaConsultaClient reservas,
            ChatDocumentoFinanceiroService documentos,ObjectMapper mapper) {
        this.acesso=acesso;this.reservas=reservas;this.documentos=documentos;this.mapper=mapper;
    }

    public boolean podeAtender(PerguntarConfiaRequest req,SessaoChatResponse sessao,List<Mensagem> historico) {
        return enabled&&escolher(req,sessao,historico)!=null;
    }

    public ChatResponseDTO tentar(PerguntarConfiaRequest req,SessaoChatResponse sessao,Long conversa,List<Mensagem> historico) {
        if(!enabled)return null;
        Escolha escolha=escolher(req,sessao,historico);if(escolha==null)return null;
        if(escolha.recurso()==null)return resposta(req,sessao,conversa,null,"ORIENTACAO_FORNECIDA",
                "Na Central de Faturas você pode filtrar os documentos e abrir o detalhe de uma fatura. "
                +"Selecione uma fatura e use ‘Ajuda com esta fatura’, ou informe seu número para consultar os dados e documentos disponíveis. "
                +"PDF da fatura, boleto e conciliação são documentos diferentes; esta orientação não gerou boleto nem realizou pagamento.",acoesCentral(req));
        try {
            acesso.validarSessao(sessao,req.getCodgUsuario(),req.getCodgAgenciaSessao());
            if("RESERVA_AEREA".equals(escolha.recurso().getTipo()))return reserva(req,sessao,conversa,escolha.recurso());
            return fatura(req,sessao,conversa,escolha.recurso());
        }catch(RegraDeNegocioException ex) {
            if(ex.getStatus()>=500)return resposta(req,sessao,conversa,null,"ERRO_INTEGRACAO",
                    "Não consegui concluir a consulta desse registro na origem. Isso não significa que ele não exista. Tente novamente mais tarde; nenhum documento foi gerado.",List.of());
            if(ex.getStatus()==404)return resposta(req,sessao,conversa,null,"SEM_RESULTADO",
                    "A consulta não encontrou esse registro no escopo autorizado da sessão. Confira a seleção ou o número informado; não ampliei a busca para outra agência.",List.of());
            // Nao divulgar corpo de fornecedor nem identificar recursos de outra agencia.
            return resposta(req,sessao,conversa,null,"CONSULTA_BLOQUEADA",
                    "Não foi possível validar essa referência com segurança na sua sessão. "
                    +"Confira a agência selecionada e abra novamente o registro pelo sistema. Nenhum documento foi gerado.",List.of());
        }catch(Exception ex) {
            java.util.logging.Logger.getLogger(getClass().getName()).warning("AJUDA_CONTEXTUAL_ERRO tipo="+ex.getClass().getSimpleName());
            return resposta(req,sessao,conversa,null,"ERRO_INTEGRACAO",
                    "Não consegui concluir a consulta desse registro agora. Isso não significa que ele não exista. "
                    +"Tente novamente pela tela de origem; nenhum documento ou operação comercial foi executado.",List.of());
        }
    }

    private ChatResponseDTO reserva(PerguntarConfiaRequest req,SessaoChatResponse sessao,Long conversa,ChatRecursoReferenciaDTO ref) {
        var filtro=new ReservaConsultaFiltros();filtro.setReservaId(Integer.valueOf(ref.getId()));filtro.setSize(1);
        var pagina=reservas.consultar(sessao.getAgencia().getCodgAgencia(),filtro);
        if(pagina==null||pagina.getReservas()==null
                ||!Objects.equals(pagina.getCodgAgencia(),sessao.getAgencia().getCodgAgencia()))
            throw new IllegalStateException("Escopo de reserva divergente.");
        if(pagina.getTotalReservas()==0)return resposta(req,sessao,conversa,null,"SEM_RESULTADO",
                "Não encontrei essa reserva entre os registros acessíveis na agência da sua sessão. Confira a seleção na tela de reservas.",List.of());
        if(pagina.getTotalReservas()!=1||pagina.getReservas().size()!=1
                ||!Objects.equals(pagina.getReservas().get(0).getReservaId(),filtro.getReservaId()))
            throw new IllegalStateException("Identidade de reserva divergente.");
        var item=pagina.getReservas().get(0);var real=referencia("RESERVA_AEREA",Integer.toString(item.getReservaId()));
        String texto="Estes são os dados cadastrados da reserva selecionada:\n\n"+ChatReservaConsultaTexto.renderizar(pagina,"DETALHES")
                +"\n\nPosso ajudar a entender os dados e abrir a reserva. Regras, disponibilidade e valores atuais precisam da consulta específica. "
                +"Não confirmei aptidão para emitir, nem efetuei emissão, alteração ou cobrança.";
        List<ChatActionDTO> actions=new ArrayList<>();
        String loc=item.getLocalizador()==null?null:item.getLocalizador().trim().toUpperCase(Locale.ROOT);
        if(!LocalizadorAereo.isValido(loc))loc=null;
        if(ChatContextoClienteContrato.suporta(req.getContextoCliente(),"ABRIR_RESERVA")&&loc!=null) {
            var destino=destino("RESERVA_AEREA",real,null);
            actions.add(new ChatActionDTO("abrir_reserva","Abrir reserva","Abrir o registro consultado no sistema.",loc,
                    item.getReservaId(),false,false,false,"Abra a reserva "+loc,destino));
        }
        if(copiloto!=null && copiloto.habilitado() && ChatContextoClienteContrato.suportaCopiloto(req.getContextoCliente())) {
            actions=copiloto.acoes(item,req.getContextoCliente());
            if(!actions.isEmpty())texto+="\n\nPróximos passos: escolha uma das opções abaixo. A simulação é apenas preparatória e terá os dados revalidados; nenhuma remarcação ou atendimento humano foi iniciado.";
        }
        return resposta(req,sessao,conversa,real,"DADOS_CONSULTADOS",texto,actions);
    }

    private ChatResponseDTO fatura(PerguntarConfiaRequest req,SessaoChatResponse sessao,Long conversa,ChatRecursoReferenciaDTO ref) {
        FaturaSicaRS f=documentos.consultarFatura(sessao,Integer.parseInt(ref.getId()));
        String produto=produto(f.getInvoiceType());
        if(ref.getProduto()!=null&&!ref.getProduto().equals(produto))throw new RegraDeNegocioException(409,"Referencia financeira divergente.");
        ChatRecursoReferenciaDTO real=referencia("FATURA",Integer.toString(f.getNumfat()));real.setProduto(produto);real.setDataEmissao(dataIso(f.getDataFatura()));
        String situacao=f.isCancelado()?"Cancelada":normalizar(f.getSituacao()).contains("credito")?"Crédito":
                normalizar(f.getSituacao()).contains("a faturar")?"À faturar":normalizar(f.getPago()).equals("sim")?"Paga":"Em aberto";
        StringBuilder texto=new StringBuilder("Fatura ").append(f.getNumfat()).append(" — ").append(situacao)
                .append("\nProduto: ").append(produto==null?"Não informado":produto)
                .append("\nValor: ").append(NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(BigDecimal.valueOf(f.getValor())))
                .append("\nEmissão: ").append(dataTexto(f.getDataFatura())).append("\nVencimento: ").append(dataTexto(f.getDataVen()));
        List<ChatActionDTO> actions=new ArrayList<>();
        if(ChatContextoClienteContrato.suporta(req.getContextoCliente(),"ABRIR_FATURA"))
            actions.add(new ChatActionDTO("abrir_fatura","Abrir fatura","Abrir a Central de Faturas com o documento selecionado.",null,null,false,false,false,
                    "Abra a fatura "+f.getNumfat(),destino("FATURA_DETALHE",real,null)));
        List<String> tipos=documentos.tiposDisponiveis(f);
        if(ChatContextoClienteContrato.suporta(req.getContextoCliente(),"BAIXAR_DOCUMENTO"))for(String tipo:tipos) {
            String label=switch(tipo){case "FATURA_PDF"->"Baixar PDF da fatura";case "BOLETO_PDF"->"Obter boleto PDF";case "CONCILIACAO_CSV"->"Baixar conciliação CSV";default->null;};
            if(label!=null)actions.add(new ChatActionDTO("baixar_documento_financeiro",label,
                    "Consulta autenticada do documento após o clique; não efetua pagamento.",null,null,
                    "BOLETO_PDF".equals(tipo),false,true,null,destino("FATURA_DETALHE",real,tipo)));
        }
        texto.append("\n\nO PDF da fatura é o demonstrativo; o CSV é a conciliação. O boleto depende das condições e da disponibilidade na origem.");
        if(!tipos.contains("BOLETO_PDF"))texto.append(" Não disponibilizei boleto para a situação atual desta fatura.");
        if(actions.stream().noneMatch(a->"baixar_documento_financeiro".equals(a.code())))
            texto.append(" Para obter os documentos, abra o detalhe na área Financeiro com sua sessão autenticada.");
        else texto.append(" Use o botão do documento desejado para fazer a solicitação autenticada.");
        texto.append(" A linha digitável não está disponível como dado estruturado nesta integração. "
                +"Esta resposta não gerou boleto nem realizou pagamento, baixa ou cobrança.");
        return resposta(req,sessao,conversa,real,"DADOS_CONSULTADOS",texto.toString(),actions);
    }

    private record Escolha(ChatRecursoReferenciaDTO recurso) { }
    private Escolha escolher(PerguntarConfiaRequest req,SessaoChatResponse sessao,List<Mensagem> historico) {
        if(req==null||req.getMensagem()==null)return null;
        String t=normalizar(req.getMensagem());
        if(ChatPedidoOperacional.pedidoHumano(t)||t.matches("(?:quero |preciso |por favor )?(?:emitir|emita|cancelar|cancele|pagar|pague|quitar|remarcar|simular|simule|reembolsar)\\b.*"))return null;
        var cliente=ChatContextoClienteContrato.validar(req.getContextoCliente());
        ChatRecursoReferenciaDTO ref=cliente==null?null:cliente.getRecurso();
        boolean financeira=t.matches(".*\\b(?:fatura|boleto|pdf|csv|conciliacao|documento|arquivo|baixar|baixe|download|linha digitavel|codigo de barras)\\b.*");
        var numero=Pattern.compile("\\bfatura\\s*(?:n(?:umero)?[ºo.:]?\\s*)?[:#]?\\s*(\\d{1,10})\\b").matcher(t);
        if(numero.find()&&t.matches(".*\\b(?:explique|explicar|ajuda|dados|detalhes|pdf|csv|boleto|baixar|baixe|download|arquivo|documento)\\b.*")) {
            try {int n=Integer.parseInt(numero.group(1));if(n<=0)return null;return new Escolha(referencia("FATURA",Integer.toString(n)));}
            catch(NumberFormatException ex){return null;}
        }
        boolean relativa=t.matches(".*\\b(?:ess[ae]|est[ae]|dess[ae]|dest[ae]|del[ae]|aqui|selecionad[ao])\\b.*")
                ||t.matches("(?:e )?(?:quais (?:os )?)?(?:proximos passos|opcoes|acoes)[?.! ]*")
                ||t.matches("(?:qual (?:a )?(?:companhia|cia)|quais (?:os )?(?:passageiros|trechos)|(?:me )?(?:mostre|explique|resuma) (?:os )?(?:dados|detalhes))[?.! ]*")
                ||t.matches("(?:(?:me )?(?:mande|envie|baixe|baixar|mostre) (?:o |a )?)?(?:pdf|boleto|csv|documento|arquivo)(?: da fatura)?[?.! ]*");
        boolean ajuda=t.matches(".*\\b(?:explique|explicar|ajud[ae]|ajudar|resum[ao]|resumir|detalhes|dados|significa|falta|companhia|cia|passageiros|trechos|vencimento|valor|situacao|opcoes|acoes|passos)\\b.*")
                ||t.matches(".*\\bo que (?:posso|podemos) fazer com (?:esta|essa) reserva[?.! ]*");
        if(ref==null&&relativa)ref=ChatAjudaContextualHistorico.recuperar(historico,req.getConversaId(),
                sessao==null||sessao.getAgencia()==null?null:sessao.getAgencia().getCodgAgencia(),req.getCodgUsuario(),mapper);
        if(ref==null&&relativa&&ChatContextoClienteContrato.suportaCopiloto(cliente)
                &&ultimaRespostaEConsulta(historico)&&req.getConversaId()!=null&&req.getCodgUsuario()!=null&&req.getCodgUsuario()>0
                &&sessao!=null&&sessao.getAgencia()!=null&&sessao.getAgencia().getCodgAgencia()!=null) {
            var anterior=com.confApi.chatconfianca.reservas.ChatReservaConsultaContexto.recuperar(historico,req.getConversaId(),
                    sessao.getAgencia().getCodgAgencia().longValue(),req.getCodgUsuario().longValue(),mapper);
            if(anterior!=null&&anterior.getSelecionadaId()!=null)
                ref=referencia("RESERVA_AEREA",Integer.toString(anterior.getSelecionadaId()));
        }
        if(ref==null) {
            if(cliente!=null&&"FATURAS".equals(cliente.getTela())&&ajuda&&t.matches(".*\\b(?:tela|faturas|financeiro|aqui)\\b.*"))return new Escolha(null);
            return null;
        }
        if("RESERVA_AEREA".equals(ref.getTipo())) {
            if(t.matches(".*\\b(?:regras?|multas?|penalidades?|reembols\\w*|remarc\\w*|emissao|emitir|emita|cancel\\w*|pagar|pague|simul\\w*)\\b.*"))return null;
            if(financeira||!ajuda||!relativa||t.matches(".*\\b(?:hotel|hoteis|carro|seguro|pacote|pesquisar|pesquise|buscar|busque)\\b.*")
                    ||LocalizadorAereo.extrair(req.getMensagem())!=null)return null;
            return new Escolha(ref);
        }
        if(!"FATURA".equals(ref.getTipo())||(!ajuda&&!financeira)||!relativa
                ||t.matches(".*\\b(?:limite|credito disponivel|minhas|meus|ultimas|ultimos|reservas|vendas)\\b.*"))return null;
        return new Escolha(ref);
    }

    private List<ChatActionDTO> acoesCentral(PerguntarConfiaRequest req) {
        if(!ChatContextoClienteContrato.suporta(req.getContextoCliente(),"ABRIR_FATURA"))return List.of();
        return List.of(new ChatActionDTO("abrir_fatura","Abrir Central de Faturas","Acessar o Financeiro no sistema.",null,null,false,false,false,
                "Abra a Central de Faturas",destino("FATURAS",null,null)));
    }
    private boolean ultimaRespostaEConsulta(List<Mensagem> historico) {
        if(historico==null)return false;
        for(int i=historico.size()-1;i>=0;i--) {
            Mensagem m=historico.get(i);if(m==null)continue;
            if(m.getRemetenteTipo()==com.confApi.chatconfianca.dto.enums.RemetenteTipo.BOT) {
                try {return m.getConteudoJson()!=null&&m.getConteudoJson().length()<=1_000_000
                        &&mapper.readTree(m.getConteudoJson()).has("consultaReservas");}
                catch(Exception ex){return false;}
            }
            if(m.getRemetenteTipo()!=com.confApi.chatconfianca.dto.enums.RemetenteTipo.USUARIO)return false;
        }
        return false;
    }
    private ChatResponseDTO resposta(PerguntarConfiaRequest req,SessaoChatResponse sessao,Long conversa,ChatRecursoReferenciaDTO recurso,
            String resultado,String texto,List<ChatActionDTO> acoes) {
        try {
            var j=mapper.createObjectNode();j.put("schema",ChatAjudaContextualHistorico.SCHEMA);j.put("resultadoTarefa",resultado);
            j.put("statusConsulta",resultado);j.put("conversa",conversa==null?0:conversa);j.put("usuario",req.getCodgUsuario());
            if(sessao!=null&&sessao.getAgencia()!=null)j.put("agencia",sessao.getAgencia().getCodgAgencia());
            j.put("atualizadoEm",System.currentTimeMillis());if(recurso!=null)j.set("recurso",mapper.valueToTree(recurso));
            j.put("intencao",recurso==null?("ORIENTACAO_FORNECIDA".equals(resultado)?"financeiro.navegacao":"ajuda_contextual.consulta_bloqueada"):"FATURA".equals(recurso.getTipo())?"financeiro.faturas":"aereo.reserva_detalhes");
            return new ChatResponseDTO(null,texto,List.of(),null,List.of("ajuda_contextual"),
                    List.of(new ChatMessageDTO("system",mapper.writeValueAsString(j))),acoes);
        }catch(Exception ex){throw new IllegalStateException("Falha ao preparar ajuda contextual.",ex);}
    }
    private static ChatRecursoReferenciaDTO referencia(String tipo,String id){var r=new ChatRecursoReferenciaDTO();r.setTipo(tipo);r.setId(id);return r;}
    private static ChatActionDestinoDTO destino(String tela,ChatRecursoReferenciaDTO recurso,String documento) {
        var d=new ChatActionDestinoDTO();d.setTela(tela);d.setRecurso(recurso);d.setDocumento(documento);return ChatContextoClienteContrato.validarDestino(d);
    }
    private static String normalizar(String s){return ChatReservaConsultaParser.normalizar(s);}
    private static String produto(String s){return switch(normalizar(s)){case "aereo","aerea"->"AEREO";case "terrestre"->"TERRESTRE";default->null;};}
    private static String dataIso(String s) {
        if(s==null||s.isBlank())return null;
        try {LocalDate d=LocalDate.parse(s.trim(),s.contains("/")?DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT):DateTimeFormatter.ISO_LOCAL_DATE);
            return d.getYear()<1900||d.getYear()>2100?null:d.toString();}catch(Exception ex){return null;}
    }
    private static String dataTexto(String s){String iso=dataIso(s);return iso==null?"Não informada":LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));}
}
