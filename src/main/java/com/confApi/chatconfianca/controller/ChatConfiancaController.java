package com.confApi.chatconfianca.controller;

import com.confApi.chatconfianca.dto.enums.PrioridadeConversa;
import com.confApi.chatconfianca.dto.enums.StatusConversa;
import com.confApi.chatconfianca.dto.model.AtendimentoAvaliacao;
import com.confApi.chatconfianca.dto.model.Conversa;
import com.confApi.chatconfianca.dto.model.ConversaEvento;
import com.confApi.chatconfianca.dto.request.TransferirConversaRequest;
import com.confApi.chatconfianca.dto.request.AdicionarTagConversaRequest;
import com.confApi.chatconfianca.dto.model.Tag;
import com.confApi.chatconfianca.dto.model.SlaConversaResumo;
import com.confApi.chatconfianca.dto.model.DashboardAtendimentoResumo;
import com.confApi.chatconfianca.dto.model.DepartamentoUnidade;
import com.confApi.chatconfianca.dto.model.Mensagem;
import com.confApi.chatconfianca.dto.model.VwConversaResumo;
import com.confApi.chatconfianca.dto.model.VwFilaAtendimento;
import com.confApi.chatconfianca.dto.request.AbrirConversaRequest;
import com.confApi.chatconfianca.dto.request.AssumirAtendimentoRequest;
import com.confApi.chatconfianca.dto.request.AvaliarAtendimentoRequest;
import com.confApi.chatconfianca.dto.request.EncerrarConversaRequest;
import com.confApi.chatconfianca.dto.request.EnviarAnexoRequest;
import com.confApi.chatconfianca.dto.request.EnviarMensagemRequest;
import com.confApi.chatconfianca.dto.request.PerguntarConfiaRequest;
import com.confApi.chatconfianca.dto.request.RegistrarLeituraRequest;
import com.confApi.chatconfianca.dto.response.AnexoDownloadResponse;
import com.confApi.chatconfianca.dto.response.ChatConfiancaIaResponse;
import com.confApi.chatconfianca.dto.response.ChatNotificacaoResumoResponse;
import com.confApi.chatconfianca.dto.response.DepartamentoAtendimentoOpcao;
import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.chatconfianca.service.ChatConfiancaIaService;
import com.confApi.chatconfianca.service.ChatConfiancaService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/chat-confianca")
public class ChatConfiancaController {
    private final ChatConfiancaService service;
    private final ChatConfiancaIaService iaService;

    public ChatConfiancaController(ChatConfiancaService service, ChatConfiancaIaService iaService) {
        this.service = service;
        this.iaService = iaService;
    }

    @GetMapping("/sessao/{codgUsuario}")
    public SessaoChatResponse montarSessao(@PathVariable Integer codgUsuario,
                                           @RequestParam(required = false) Integer codgAgenciaSessao) {
        return service.montarSessao(codgUsuario, codgAgenciaSessao);
    }

    @GetMapping("/departamentos/agencia/{codgAgencia}")
    public List<DepartamentoUnidade> listarDepartamentosDisponiveis(@PathVariable Integer codgAgencia) {
        return service.listarDepartamentosDisponiveis(codgAgencia);
    }

    @GetMapping("/departamentos/usuario/{codgUsuario}")
    public List<DepartamentoUnidade> listarDepartamentosDisponiveisPorUsuario(@PathVariable Integer codgUsuario,
                                                                              @RequestParam(required = false) Integer codgAgenciaSessao) {
        return service.listarDepartamentosDisponiveisPorUsuario(codgUsuario, codgAgenciaSessao);
    }

    @GetMapping("/departamentos-opcoes/usuario/{codgUsuario}")
    public List<DepartamentoAtendimentoOpcao> listarOpcoesAtendimentoUsuario(@PathVariable Integer codgUsuario,
                                                                              @RequestParam(required = false) Integer codgAgenciaSessao) {
        return service.listarOpcoesAtendimentoUsuario(codgUsuario, codgAgenciaSessao);
    }

    @PostMapping("/conversas")
    public Conversa abrirConversa(@RequestBody AbrirConversaRequest request) {
        return service.abrirConversa(request);
    }

    @GetMapping("/conversas/{conversaId}")
    public Conversa buscarConversa(@PathVariable Long conversaId,
                                   @RequestParam Integer codgUsuario,
                                   @RequestParam(defaultValue = "false") boolean gestor) {
        return service.buscarConversa(conversaId, codgUsuario, gestor);
    }

    @GetMapping("/conversas/{conversaId}/mensagens")
    public List<Mensagem> listarMensagens(@PathVariable Long conversaId,
                                          @RequestParam Integer codgUsuario,
                                          @RequestParam(defaultValue = "false") boolean podeVerInternas,
                                          @RequestParam(defaultValue = "false") boolean gestor) {
        return service.listarMensagens(conversaId, codgUsuario, podeVerInternas, gestor);
    }

    @GetMapping("/conversas/{conversaId}/eventos")
    public List<ConversaEvento> listarEventos(@PathVariable Long conversaId,
                                              @RequestParam Integer codgUsuario,
                                              @RequestParam(defaultValue = "false") boolean gestor) {
        return service.listarEventos(conversaId, codgUsuario, gestor);
    }


    @GetMapping("/tags")
    public List<Tag> listarTagsAtivas(@RequestParam Integer codgUsuario) {
        return service.listarTagsAtivas(codgUsuario);
    }

    @GetMapping("/conversas/{conversaId}/tags")
    public List<Tag> listarTagsConversa(@PathVariable Long conversaId,
                                        @RequestParam Integer codgUsuario,
                                        @RequestParam(defaultValue = "false") boolean gestor) {
        return service.listarTagsConversa(conversaId, codgUsuario, gestor);
    }

    @PostMapping("/conversas/tags")
    public Tag adicionarTagConversa(@RequestBody AdicionarTagConversaRequest request) {
        return service.adicionarTagConversa(request);
    }

    @PostMapping("/conversas/tags/remover")
    public void removerTagConversa(@RequestBody AdicionarTagConversaRequest request) {
        service.removerTagConversa(request);
    }

    @PostMapping("/conversas/transferir")
    public Conversa transferirConversa(@RequestBody TransferirConversaRequest request) {
        return service.transferirConversa(request);
    }

    @GetMapping("/conversas/{conversaId}/sla")
    public SlaConversaResumo calcularSlaConversa(@PathVariable Long conversaId,
                                                 @RequestParam Integer codgUsuario,
                                                 @RequestParam(defaultValue = "false") boolean gestor) {
        return service.calcularSlaConversa(conversaId, codgUsuario, gestor);
    }

    @GetMapping("/dashboard/unidade/{codgUnidade}")
    public DashboardAtendimentoResumo dashboardUnidade(@PathVariable Integer codgUnidade,
                                                       @RequestParam Integer codgUsuario) {
        return service.dashboardUnidade(codgUnidade, codgUsuario);
    }

    @PostMapping("/mensagens")
    public Mensagem enviarMensagem(@RequestBody EnviarMensagemRequest request) {
        return service.enviarMensagem(request);
    }

    @PostMapping("/ia/perguntar")
    public ChatConfiancaIaResponse perguntarConfia(@RequestBody PerguntarConfiaRequest request) {
        return iaService.perguntar(request);
    }

    @PostMapping("/ia/encaminhar-atendente")
    public ChatConfiancaIaResponse encaminharConfiaParaAtendente(@RequestBody PerguntarConfiaRequest request) {
        return iaService.encaminharAtendente(request);
    }

    @PostMapping("/anexos")
    public Mensagem enviarAnexo(@RequestBody EnviarAnexoRequest request) {
        return service.enviarAnexo(request);
    }

    @GetMapping("/anexos/{anexoId}/download")
    public ResponseEntity<byte[]> baixarAnexo(@PathVariable Long anexoId,
                                              @RequestParam Integer codgUsuario,
                                              @RequestParam(defaultValue = "false") boolean gestor) {
        AnexoDownloadResponse arquivo = service.baixarAnexo(anexoId, codgUsuario, gestor);
        String mimeType = arquivo.getMimeType() == null || arquivo.getMimeType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : arquivo.getMimeType();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nomeDownloadSeguro(arquivo.getNomeArquivo()) + "\"")
                .body(arquivo.getConteudo());
    }

    @GetMapping("/fila/atendente/{codgUsuario}")
    public List<VwFilaAtendimento> listarFilaParaAtendente(@PathVariable Integer codgUsuario,
                                                           @RequestParam(defaultValue = "false") boolean gestor) {
        return service.listarFilaParaAtendente(codgUsuario, gestor);
    }

    @PostMapping("/fila/assumir")
    public Conversa assumirAtendimento(@RequestBody AssumirAtendimentoRequest request) {
        return service.assumirAtendimento(request);
    }

    @PostMapping("/fila/redistribuir")
    public Conversa redistribuirFila(@RequestParam Long filaId,
                                     @RequestParam Integer codgUsuario) {
        return service.redistribuirFila(filaId, codgUsuario);
    }

    @PostMapping("/conversas/encerrar")
    public Conversa encerrarConversa(@RequestBody EncerrarConversaRequest request) {
        return service.encerrarConversa(request);
    }

    @PostMapping("/avaliacoes")
    public AtendimentoAvaliacao avaliarAtendimento(@RequestBody AvaliarAtendimentoRequest request) {
        return service.avaliarAtendimento(request);
    }

    @GetMapping("/avaliacoes/conversa/{conversaId}")
    public AtendimentoAvaliacao buscarAvaliacaoAtendimento(@PathVariable Long conversaId,
                                                            @RequestParam Integer codgUsuario,
                                                            @RequestParam(defaultValue = "false") boolean gestor) {
        return service.buscarAvaliacaoAtendimento(conversaId, codgUsuario, gestor);
    }

    @PostMapping("/leituras")
    public int registrarLeitura(@RequestBody RegistrarLeituraRequest request) {
        return service.registrarLeitura(request);
    }

    @GetMapping("/historico/solicitante/{codgUsuario}")
    public List<VwConversaResumo> listarHistoricoSolicitante(@PathVariable Integer codgUsuario) {
        return service.listarHistoricoSolicitante(codgUsuario);
    }

    @GetMapping("/historico/atendente/{codgUsuario}")
    public List<VwConversaResumo> listarHistoricoAtendente(@PathVariable Integer codgUsuario) {
        return service.listarHistoricoAtendente(codgUsuario);
    }

    @GetMapping("/notificacoes/resumo/{codgUsuario}")
    public ChatNotificacaoResumoResponse resumirNotificacoes(@PathVariable Integer codgUsuario) {
        return service.resumirNotificacoes(codgUsuario);
    }

    @GetMapping("/historico/unidade/{codgUnidade}")
    public List<VwConversaResumo> listarHistoricoUnidade(@PathVariable Integer codgUnidade,
                                                         @RequestParam Integer codgUsuario) {
        return service.listarHistoricoUnidade(codgUnidade, codgUsuario);
    }

    @GetMapping("/historico/buscar")
    public List<VwConversaResumo> buscarHistorico(@RequestParam Integer codgUsuario,
                                                   @RequestParam(defaultValue = "false") boolean gestor,
                                                   @RequestParam(required = false) String termo,
                                                   @RequestParam(required = false) StatusConversa status,
                                                   @RequestParam(required = false) PrioridadeConversa prioridade,
                                                   @RequestParam(required = false) Integer codgUnidade,
                                                   @RequestParam(required = false) Integer codgAgencia,
                                                   @RequestParam(required = false) Integer codgSolicitante,
                                                   @RequestParam(required = false) Integer codgAtendente,
                                                   @RequestParam(required = false) Long departamentoUnidadeId,
                                                   @RequestParam(required = false)
                                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                   LocalDateTime dataInicio,
                                                   @RequestParam(required = false)
                                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                   LocalDateTime dataFim,
                                                   @RequestParam(required = false) Integer limite) {
        return service.buscarHistorico(codgUsuario, gestor, termo, status, prioridade, codgUnidade,
                codgAgencia, codgSolicitante, codgAtendente, departamentoUnidadeId,
                dataInicio, dataFim, limite);
    }

    private String nomeDownloadSeguro(String nomeArquivo) {
        if (nomeArquivo == null || nomeArquivo.isBlank()) {
            return "anexo";
        }
        return nomeArquivo.replace("\\", "_").replace("/", "_").replace("\"", "_");
    }
}
