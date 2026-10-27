package com.confApi.chatconfianca.contexto;

import com.confApi.aereo.AereoClient;
import com.confApi.aereo.AereoRegrasReservaService;
import com.confApi.aereo.dto.ConsultarLocalizadorRequest;
import com.confApi.aereo.dto.ConsultarLocalizadorResponse;
import com.confApi.aereo.dto.Reserva;
import com.confApi.aereo.dto.regrasAereas.RegrasAereasReservaResponse;
import com.confApi.chatconfianca.dto.request.PerguntarConfiaRequest;
import com.confApi.chatconfianca.dto.reserva.ReservaAereaRecenteItem;
import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.chatconfianca.reservas.ChatReservaConsultaClient;
import com.confApi.chatconfianca.reservas.ReservaConsultaFiltros;
import com.confApi.chatconfianca.service.ChatContextualAcessoService;
import com.confApi.chatgpt.dto.ChatActionDTO;
import com.confApi.chatgpt.dto.ChatMessageDTO;
import com.confApi.chatgpt.dto.ChatResponseDTO;
import com.confApi.chatgpt.util.LocalizadorAereo;
import com.confApi.db.confManager.regraAereaAlteracao.dto.RegraAereaAlteracaoConsultaResponse;
import com.confApi.db.confManager.regraAereaReembolso.dto.RegraAereaReembolsoConsultaResponse;
import com.confApi.exception.RegraDeNegocioException;
import com.confApi.model.IdentificacaoAgenciaModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/** Regras de um ID selecionado: leitura privada e factual, sem decisao/execucao por LLM. */
@Service
public class ChatCopilotoRegrasService {
    private final ChatContextualAcessoService acesso;
    private final ChatReservaConsultaClient consulta;
    private final AereoClient aereo;
    private final AereoRegrasReservaService regras;
    private final ChatReservaCopilotoService copiloto;
    private final ObjectMapper mapper;

    public ChatCopilotoRegrasService(ChatContextualAcessoService acesso, ChatReservaConsultaClient consulta,
            AereoClient aereo, AereoRegrasReservaService regras, ChatReservaCopilotoService copiloto, ObjectMapper mapper) {
        this.acesso=acesso;this.consulta=consulta;this.aereo=aereo;this.regras=regras;this.copiloto=copiloto;this.mapper=mapper;
    }

    /** Filtro sem IO, usado antes do planejador. Nao constitui autorizacao. */
    public boolean podeAtender(PerguntarConfiaRequest req) {
        if (req == null || !ChatContextoClienteContrato.suportaCopiloto(req.getContextoCliente())) return false;
        var r=req.getContextoCliente().getRecurso();
        if (r == null || !"RESERVA_AEREA".equals(r.getTipo()) || id(r.getId()) == null) return false;
        String texto=normalizar(req.getMensagem());
        if (texto.matches(".*\\bnao\\b.{0,35}\\b(?:consulte|consultar|carregue|carregar|mostrar|mostre|ver)\\b.{0,20}\\b(?:regras?|multas?)\\b.*")) return false;
        return perguntaRegras(texto) || texto.matches(".*\\bbagagem\\b.*");
    }

    static boolean perguntaRegras(String texto) {
        String t = normalizar(texto);
        return t.matches(".*\\b(?:regras?|multas?|penalidades?|no[ -]?show)\\b.*")
                || t.matches(".*\\b(?:tarifa|bilhete)\\b.*\\b(?:permite|permitem)\\b.*\\b(?:remarcacao|reembolso|alteracao)\\b.*");
    }

    public ChatResponseDTO tentar(PerguntarConfiaRequest req, SessaoChatResponse sessao, Long conversaId) {
        if (!podeAtender(req)) return null;
        ReservaAereaRecenteItem item=null;
        try {
            if (!copiloto.habilitado()) throw new Bloqueio();
            if (conversaId == null || conversaId <= 0) throw new Bloqueio();
            acesso.validarSessao(sessao,req.getCodgUsuario(),req.getCodgAgenciaSessao());
            if (sessao == null || sessao.getAgencia() == null || sessao.getUsuario() == null
                    || !java.util.Objects.equals(sessao.getUsuario().getCodgUsuario(),req.getCodgUsuario())) throw new Bloqueio();
            Integer agencia=sessao.getAgencia().getCodgAgencia();
            if (agencia == null || agencia <= 0) throw new Bloqueio();
            ReservaConsultaFiltros filtro=new ReservaConsultaFiltros();
            filtro.setReservaId(id(req.getContextoCliente().getRecurso().getId()));filtro.setSize(1);
            var pagina=consulta.consultar(agencia,filtro);
            if (pagina == null || !java.util.Objects.equals(agencia,pagina.getCodgAgencia()) || pagina.getReservas() == null) throw new Integracao();
            if (pagina.getTotalReservas() == 0 && pagina.getReservas().isEmpty()) return resposta(req,sessao,conversaId,null,"SEM_RESULTADO",
                    "Nao encontrei a reserva selecionada no escopo autorizado desta agencia. Selecione novamente o registro correto; nao consultei regras de outra reserva.",List.of());
            if (pagina.getTotalReservas() != 1 || pagina.getReservas().size() != 1) throw new Ambigua();
            var candidato=pagina.getReservas().get(0);
            if (candidato == null || !java.util.Objects.equals(filtro.getReservaId(),candidato.getReservaId())
                    || candidato.getLocalizador() == null || !candidato.getLocalizador().trim().matches("[A-Za-z0-9]{5,30}")) throw new Integracao();
            item=candidato;
            String localizador=item.getLocalizador().trim().toUpperCase(Locale.ROOT);
            validarLocalizadores(req.getMensagem(),localizador);
            String companhia=companhia(item.getCompanhiaIata());
            if (companhia.isEmpty()) throw new Bloqueio();
            ConsultarLocalizadorResponse resposta=aereo.carregarReservaEstrita(pedido(sessao,req,localizador));
            Reserva unica=unica(resposta,localizador,companhia);
            if (normalizar(req.getMensagem()).matches(".*\\bbagagem\\b.*") && !perguntaRegras(req.getMensagem()))
                return resposta(req,sessao,conversaId,item,"DADOS_CONSULTADOS",bagagem(unica,localizador),copiloto.acoes(item,req.getContextoCliente()));
            ConsultarLocalizadorResponse selecionada=new ConsultarLocalizadorResponse();
            selecionada.setReservas(new ArrayList<>(List.of(unica)));
            // Nao consulta regras de outras correspondencias retornadas pelo provedor.
            Reserva enriquecida=unica(regras.enriquecer(selecionada),localizador,companhia);
            RegrasAereasReservaResponse dados=enriquecida.getRegrasAereas();
            if (dados == null || falha(dados)) throw new Integracao();
            List<String> linhas=formatar(dados);
            if (linhas.isEmpty()) return resposta(req,sessao,conversaId,item,"SEM_REGRAS",
                    "Nao localizei regras aplicaveis com os dados disponiveis para esta reserva. Isso nao significa isencao de multa ou permissao para alterar/reembolsar. Consulte o atendimento antes de decidir.",List.of());
            String texto="Regras consultadas para a reserva "+localizador+":\n\n"+String.join("\n",linhas)
                    +"\n\nSao condicoes cadastradas para os dados retornados. Nao representam autorizacao final nem garantem disponibilidade ou valor total da operacao. Nenhum cancelamento, reembolso, remarcacao ou cobranca foi executado.";
            return resposta(req,sessao,conversaId,item,"DADOS_CONSULTADOS",texto,copiloto.acoes(item,req.getContextoCliente()));
        } catch (Ambigua ex) {
            return resposta(req,sessao,conversaId,item,"REFERENCIA_AMBIGUA","Nao consegui confirmar uma unica reserva correspondente a selecao e ao localizador informado. Abra novamente o registro correto ou consulte o atendimento; nao usei regras de outra reserva.",List.of());
        } catch (Bloqueio ex) {
            return resposta(req,sessao,conversaId,null,"CONSULTA_BLOQUEADA","Nao foi possivel confirmar o acesso e a identificacao desta reserva. Atualize sua sessao e selecione novamente o registro.",List.of());
        } catch (RegraDeNegocioException ex) {
            String status=ex.getStatus()==403?"CONSULTA_BLOQUEADA":"ERRO_INTEGRACAO";
            return resposta(req,sessao,conversaId,null,status,"Nao foi possivel confirmar a consulta das regras desta reserva agora. Isso nao significa ausencia de regras ou isencao de multa. Tente novamente ou procure o atendimento.",List.of());
        } catch (RuntimeException ex) {
            return resposta(req,sessao,conversaId,item,"ERRO_INTEGRACAO","Nao foi possivel concluir a consulta das regras desta reserva agora. Isso nao significa ausencia de regras ou isencao de multa. Tente novamente ou procure o atendimento.",List.of());
        }
    }

    private static String bagagem(Reserva reserva, String localizador) {
        List<String> linhas = new ArrayList<>();
        for (var trecho : reserva.getViagens()) if (trecho != null && trecho.getVoos() != null)
            for (var voo : trecho.getVoos()) if (voo != null) {
                String numero = voo.getNumeroVoo();
                numero = numero != null && numero.matches("[A-Za-z0-9]{1,10}") ? numero : "não informado";
                String franquia = "franquia não informada pela origem";
                if (Boolean.FALSE.equals(voo.getBagagemInclusa())) franquia = "bagagem despachada não incluída";
                else if (Boolean.TRUE.equals(voo.getBagagemInclusa())) {
                    franquia = "bagagem incluída";
                    if (voo.getBagagemQuantidade() != null && voo.getBagagemQuantidade() > 0)
                        franquia += ": " + voo.getBagagemQuantidade() + " peça(s)";
                    Double peso = voo.getBagagemPeso(); String unidade = voo.getBagagemUnidadeDeMedida();
                    if (peso != null && Double.isFinite(peso) && peso > 0 && unidade != null
                            && Set.of("KG","K","LB","LBS").contains(unidade.toUpperCase(Locale.ROOT)))
                        franquia += " de " + java.text.NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).format(peso)
                                + " " + unidade.toLowerCase(Locale.ROOT);
                }
                linhas.add("Voo " + numero + ": " + franquia + ".");
            }
        return "Bagagem consultada para a reserva " + localizador + ":\n\n"
                + (linhas.isEmpty() ? "A origem não informou a franquia por voo." : String.join("\n", linhas))
                + "\n\nNenhuma alteração ou contratação de bagagem foi executada.";
    }

    private static Reserva unica(ConsultarLocalizadorResponse resposta,String pnr,String cia) {
        if (resposta == null || resposta.getException() != null || resposta.getReservas() == null || resposta.getReservas().isEmpty()) throw new Integracao();
        List<Reserva> exatas=resposta.getReservas().stream().filter(java.util.Objects::nonNull)
                .filter(r->r.getLocalizador()!=null && pnr.equalsIgnoreCase(r.getLocalizador().trim()))
                .filter(r->r.getViagens()!=null && r.getViagens().stream().filter(java.util.Objects::nonNull)
                    .filter(t->t.getCompanhia()!=null).anyMatch(t->cia.equals(companhia(t.getCompanhia().getCodigoIata())))).toList();
        if (exatas.size()!=1) throw new Ambigua();
        return exatas.get(0);
    }

    private static ConsultarLocalizadorRequest pedido(SessaoChatResponse sessao,PerguntarConfiaRequest req,String pnr) {
        ConsultarLocalizadorRequest p=new ConsultarLocalizadorRequest();p.setSistema("Wooba");p.setLocalizador(pnr);
        var agencia=new com.confApi.aereo.dto.Agencia();
        agencia.setCodgAgencia(Integer.toString(sessao.getAgencia().getCodgAgencia()));
        agencia.setCodgSistemaBackoffice(sessao.getAgencia().getCodgSistemaBackoffice());
        agencia.setNome(sessao.getAgencia().getNomeAgencia());agencia.setUnidade(sessao.getUnidade()==null?null:sessao.getUnidade().getNomeUnidade());p.setAgencia(agencia);
        String erp=sessao.getAgencia().getCodgSistemaBackoffice();
        if (erp == null || !erp.matches("\\d{1,10}")) throw new Bloqueio();
        Integer erpId=id(erp.replaceFirst("^0+(?!$)",""));if (erpId==null) throw new Bloqueio();
        IdentificacaoAgenciaModel identificacao=new IdentificacaoAgenciaModel();
        identificacao.setCodgAgencia(sessao.getAgencia().getCodgAgencia());identificacao.setCodgUsuario(req.getCodgUsuario());
        identificacao.setCodgUnidade(sessao.getAgencia().getCodgUnidade());identificacao.setCodgErp(erpId);p.setIdentificacaoAgenciaModel(identificacao);return p;
    }

    private static void validarLocalizadores(String texto,String atual) {
        String extraido=LocalizadorAereo.extrair(texto);
        if (extraido != null && !Set.of("SELECIONADA","SELECIONADO","CARREGADA","CONSULTADA","DESSA","DESTA","ATUAL").contains(extraido)
                && !atual.equalsIgnoreCase(extraido)) throw new Ambigua();
        var codigos=Pattern.compile("\\b[A-Za-z0-9]{5,30}\\b").matcher(texto==null?"":texto);
        while(codigos.find()) {
            String codigo=codigos.group();
            if(codigo.matches("(?i)(?=.*[a-z])(?=.*[0-9]).*") && !atual.equalsIgnoreCase(codigo))throw new Ambigua();
        }
    }

    private static boolean falha(RegrasAereasReservaResponse d) {
        if (erro(d.getStatus())) return true;
        if (d.getReembolso()!=null && erro(d.getReembolso().getStatus())) return true;
        if (d.getAlteracao()!=null && erro(d.getAlteracao().getStatus())) return true;
        if (d.getReembolsos()!=null && d.getReembolsos().stream().filter(java.util.Objects::nonNull).anyMatch(r->erro(r.getStatus()))) return true;
        return d.getAlteracoes()!=null && d.getAlteracoes().stream().filter(java.util.Objects::nonNull).anyMatch(r->erro(r.getStatus()));
    }
    private static boolean erro(String status){return status!=null && Set.of("ERRO_CONSULTA","ERRO","ERROR","FALHA","ERRO_INTEGRACAO").contains(chave(status));}

    private static List<String> formatar(RegrasAereasReservaResponse d) {
        List<String> linhas=new ArrayList<>();
        List<RegraAereaReembolsoConsultaResponse> reembolsos=d.getReembolsos()==null||d.getReembolsos().isEmpty()
                ?d.getReembolso()==null?List.of():List.of(d.getReembolso()):d.getReembolsos();
        for(var r:reembolsos.stream().filter(java.util.Objects::nonNull).limit(3).toList())if(reembolsoValido(r.getStatus())&&r.getRegra()!=null) {
            var regra=r.getRegra();String titulo="Reembolso"+momento(regra.getMomento());
            boolean manual="VALIDACAO_MANUAL".equals(chave(r.getStatus()))||Boolean.TRUE.equals(regra.getRequerValidacaoManual());
            linhas.add(titulo+": "+(manual?"a aplicabilidade exige validacao manual; nao ha permissao confirmada.":permissaoReembolso(r.getStatus()))
                    +multa(aplicaMultaConsistente(r.getStatus(),regra.getAplicaMulta()),regra.getTipoMulta(),regra.getMoedaMulta(),regra.getValorMultaFixo(),regra.getPercentualMulta(),regra.getPorPassageiro(),regra.getPorTrecho(),false)
                    +(Boolean.TRUE.equals(regra.getRequerSimulacao())?" Exige simulacao.":""));
        }
        List<RegraAereaAlteracaoConsultaResponse> alteracoes=d.getAlteracoes()==null||d.getAlteracoes().isEmpty()
                ?d.getAlteracao()==null?List.of():List.of(d.getAlteracao()):d.getAlteracoes();
        for(var r:alteracoes.stream().filter(java.util.Objects::nonNull).limit(3).toList())if(alteracaoValida(r.getStatus())&&r.getRegra()!=null) {
            var regra=r.getRegra();linhas.add("Alteracao/remarcacao"+momento(regra.getMomento())+": "+("NAO_PERMITIDA".equals(chave(r.getStatus()))?"a regra cadastrada nao permite.":"a regra cadastrada permite, sujeita as condicoes aplicaveis.")
                    +multa(aplicaMultaConsistente(r.getStatus(),regra.getAplicaMulta()),regra.getTipoMulta(),regra.getMoedaMulta(),regra.getValorMultaFixo(),regra.getPercentualMulta(),regra.getPorPassageiro(),regra.getPorTrecho(),regra.getValorMultaFixoEuropa()!=null||regra.getValorMultaFixoDemaisInternacionais()!=null)
                    +(Boolean.TRUE.equals(regra.getCobraDiferencaTarifaria())?" Pode haver diferenca tarifaria.":"")
                    +(Boolean.TRUE.equals(regra.getExigeRemarcacaoConjunta())?" A regra exige remarcacao conjunta dos trechos aplicaveis.":""));
        }
        return linhas;
    }
    private static String permissaoReembolso(String status){return switch(chave(status)){
        case "REEMBOLSO_COM_MULTA","REEMBOLSO_PERMITIDO"->"a regra cadastrada permite, sujeita as condicoes aplicaveis.";
        case "SOMENTE_TAXAS_REEMBOLSAVEIS"->"somente as taxas de embarque sao reembolsaveis nas condicoes da regra; isso nao indica reembolso da tarifa.";
        default->"a regra cadastrada nao permite reembolso da tarifa.";};}
    private static boolean reembolsoValido(String status){return Set.of("VALIDACAO_MANUAL","REEMBOLSO_COM_MULTA","REEMBOLSO_PERMITIDO","SOMENTE_TAXAS_REEMBOLSAVEIS","NAO_REEMBOLSAVEL").contains(chave(status));}
    private static boolean alteracaoValida(String status){return Set.of("NAO_PERMITIDA","PERMITIDA_COM_MULTA","PERMITIDA_SEM_MULTA").contains(chave(status));}
    private static Boolean aplicaMultaConsistente(String status,Boolean aplica){
        String s=chave(status);
        if(Set.of("REEMBOLSO_COM_MULTA","PERMITIDA_COM_MULTA").contains(s)&&!Boolean.TRUE.equals(aplica))return null;
        if(Set.of("REEMBOLSO_PERMITIDO","PERMITIDA_SEM_MULTA").contains(s)&&Boolean.TRUE.equals(aplica))return null;
        return aplica;
    }
    private static String chave(String valor){return valor==null?"":valor.trim().toUpperCase(Locale.ROOT);}
    private static String momento(String momento){return switch(momento==null?"":momento){case "ANTES_EMBARQUE"->" antes do embarque";case "APOS_EMBARQUE"->" apos o embarque";case "NO_SHOW"->" em caso de no-show";default->"";};}
    private static String multa(Boolean aplica,String tipo,String moeda,BigDecimal fixa,BigDecimal percentual,Boolean passageiro,Boolean trecho,boolean valoresRegionais) {
        if(aplica==null)return " Multa nao informada nesta regra.";
        if(!aplica)return " A regra informa que nao aplica multa; isso nao garante ausencia de outros encargos.";
        String por=(Boolean.TRUE.equals(passageiro)?" por passageiro":"")+(Boolean.TRUE.equals(trecho)?" por trecho":"");
        boolean fixaValida=fixa!=null&&fixa.signum()>=0&&moeda!=null&&moeda.matches("[A-Z]{3}");
        boolean percentualValido=percentual!=null&&percentual.signum()>=0&&percentual.compareTo(BigDecimal.valueOf(100))<=0;
        String tipoNormalizado=chave(tipo);
        if("PERCENTUAL".equals(tipoNormalizado)&&percentualValido)return " Multa percentual cadastrada: "+percentual.toPlainString()+"% sobre a base prevista na regra"+por+"; confirme o calculo na simulacao/atendimento.";
        if(!valoresRegionais&&Set.of("FIXO","FIXA","VALOR_FIXO").contains(tipoNormalizado)&&fixaValida)return " Multa fixa cadastrada: "+moeda+" "+fixa.toPlainString()+por+".";
        if(!valoresRegionais&&Set.of("MENOR_ENTRE_FIXO_E_PERCENTUAL","MAIOR_ENTRE_FIXO_E_PERCENTUAL").contains(tipoNormalizado)&&fixaValida&&percentualValido)
            return " A multa cadastrada considera o "+(tipoNormalizado.startsWith("MENOR")?"menor":"maior")+" entre "+moeda+" "+fixa.toPlainString()+" e "+percentual.toPlainString()+"% sobre a base prevista na regra"+por+"; o resultado precisa de simulacao/atendimento.";
        return " A regra preve multa; o valor precisa ser confirmado em simulacao/atendimento.";
    }

    private ChatResponseDTO resposta(PerguntarConfiaRequest req,SessaoChatResponse sessao,Long conversaId,ReservaAereaRecenteItem item,String status,String texto,List<ChatActionDTO> acoes) {
        Map<String,Object> payload=new LinkedHashMap<>();payload.put("schema","chat.copiloto-regras.v1");payload.put("statusConsulta",status);payload.put("resultadoTarefa",status);
        payload.put("conversa",conversaId);payload.put("usuario",req.getCodgUsuario());payload.put("agencia",sessao==null||sessao.getAgencia()==null?null:sessao.getAgencia().getCodgAgencia());payload.put("atualizadoEm",System.currentTimeMillis());
        if(item!=null&&item.getReservaId()!=null&&item.getReservaId()>0&&item.getLocalizador()!=null&&item.getLocalizador().trim().matches("[A-Za-z0-9]{5,30}")){payload.put("recurso",Map.of("tipo","RESERVA_AEREA","id",Integer.toString(item.getReservaId())));payload.put("localizador",item.getLocalizador());}
        String json;try{json=mapper.writeValueAsString(payload);}catch(Exception ex){json="{\"schema\":\"chat.copiloto-regras.v1\",\"statusConsulta\":\"ERRO_INTEGRACAO\",\"resultadoTarefa\":\"ERRO_INTEGRACAO\"}";texto="Nao foi possivel preparar a consulta das regras. Tente novamente.";acoes=List.of();}
        return new ChatResponseDTO(null,texto,List.of(),null,List.of("reserva_aerea_regras"),List.of(new ChatMessageDTO("system",json)),acoes);
    }
    private static Integer id(String v){try{return v!=null&&v.matches("[1-9][0-9]{0,9}")&&Long.parseLong(v)<=Integer.MAX_VALUE?Integer.valueOf(v):null;}catch(NumberFormatException ex){return null;}}
    private static String companhia(String v){String s=v==null?"":v.trim().toUpperCase(Locale.ROOT);return Set.of("LA","JJ").contains(s)?"LATAM":s;}
    private static String normalizar(String v){return v==null?"":Normalizer.normalize(v,Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);}
    private static class Ambigua extends RuntimeException { }
    private static class Bloqueio extends RuntimeException { }
    private static class Integracao extends RuntimeException { }
}
