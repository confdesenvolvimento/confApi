package com.confApi.chatconfianca.controller;

import com.confApi.chatconfianca.dto.model.AtendenteStatus;
import com.confApi.chatconfianca.dto.model.ChatPerfil;
import com.confApi.chatconfianca.dto.model.ChatUsuarioPerfil;
import com.confApi.chatconfianca.dto.model.Departamento;
import com.confApi.chatconfianca.dto.model.DepartamentoAtendente;
import com.confApi.chatconfianca.dto.model.DepartamentoUnidade;
import com.confApi.chatconfianca.dto.model.RefUnidade;
import com.confApi.chatconfianca.dto.model.RefUsuario;
import com.confApi.chatconfianca.dto.model.RespostaRapida;
import com.confApi.chatconfianca.dto.model.SlaPolitica;
import com.confApi.chatconfianca.dto.request.DepartamentoUnidadeSincronizacaoRequest;
import com.confApi.chatconfianca.dto.request.DepartamentoAtendenteSincronizacaoRequest;
import com.confApi.chatconfianca.dto.request.SlaPoliticaSincronizacaoRequest;
import com.confApi.chatconfianca.service.ChatConfiancaGestaoUnidadeService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/chat-confianca/gestao-unidade")
public class ChatConfiancaGestaoUnidadeController {

    private final ChatConfiancaGestaoUnidadeService service;

    public ChatConfiancaGestaoUnidadeController(ChatConfiancaGestaoUnidadeService service) {
        this.service = service;
    }

    @GetMapping("/departamentos")
    public List<Departamento> listarDepartamentos(@RequestParam Integer codgUsuario) {
        return service.listarDepartamentos(codgUsuario);
    }

    @PostMapping("/departamentos")
    public Departamento salvarDepartamento(@RequestParam Integer codgUsuario,
                                           @RequestBody Departamento departamento) {
        return service.salvarDepartamento(codgUsuario, departamento);
    }

    @DeleteMapping("/departamentos/{id}")
    public void excluirDepartamento(@RequestParam Integer codgUsuario, @PathVariable Long id) {
        service.excluirDepartamento(codgUsuario, id);
    }

    @GetMapping("/departamento-unidades")
    public List<DepartamentoUnidade> listarDepartamentoUnidades(@RequestParam Integer codgUsuario) {
        return service.listarDepartamentoUnidades(codgUsuario);
    }

    @GetMapping("/unidades")
    public List<RefUnidade> listarUnidades(@RequestParam Integer codgUsuario) {
        return service.listarUnidades(codgUsuario);
    }

    @PostMapping("/departamento-unidades/sincronizar")
    public List<DepartamentoUnidade> sincronizarDepartamentoUnidades(
            @RequestParam Integer codgUsuario,
            @RequestBody DepartamentoUnidadeSincronizacaoRequest request) {
        return service.sincronizarDepartamentoUnidades(codgUsuario, request);
    }

    @PostMapping("/departamento-unidades")
    public DepartamentoUnidade salvarDepartamentoUnidade(@RequestParam Integer codgUsuario,
                                                        @RequestBody DepartamentoUnidade entity) {
        return service.salvarDepartamentoUnidade(codgUsuario, entity);
    }

    @DeleteMapping("/departamento-unidades/{id}")
    public void excluirDepartamentoUnidade(@RequestParam Integer codgUsuario, @PathVariable Long id) {
        service.excluirDepartamentoUnidade(codgUsuario, id);
    }

    @GetMapping("/atendentes")
    public List<DepartamentoAtendente> listarAtendentes(@RequestParam Integer codgUsuario) {
        return service.listarAtendentes(codgUsuario);
    }

    @PostMapping("/atendentes")
    public DepartamentoAtendente salvarAtendente(@RequestParam Integer codgUsuario,
                                                 @RequestBody DepartamentoAtendente entity) {
        return service.salvarAtendente(codgUsuario, entity);
    }

    @PostMapping("/atendentes/sincronizar")
    public List<DepartamentoAtendente> sincronizarAtendente(
            @RequestParam Integer codgUsuario,
            @RequestBody DepartamentoAtendenteSincronizacaoRequest request) {
        return service.sincronizarAtendente(codgUsuario, request);
    }

    @DeleteMapping("/atendentes/{id}")
    public void excluirAtendente(@RequestParam Integer codgUsuario, @PathVariable Long id) {
        service.excluirAtendente(codgUsuario, id);
    }

    @GetMapping("/atendente-status")
    public List<AtendenteStatus> listarAtendenteStatus(@RequestParam Integer codgUsuario) {
        return service.listarAtendenteStatus(codgUsuario);
    }

    @PostMapping("/atendente-status")
    public AtendenteStatus salvarAtendenteStatus(@RequestParam Integer codgUsuario,
                                                 @RequestBody AtendenteStatus status) {
        return service.salvarAtendenteStatus(codgUsuario, status);
    }

    @GetMapping("/usuarios-internos")
    public List<RefUsuario> listarUsuariosInternos(@RequestParam Integer codgUsuario) {
        return service.listarUsuariosInternos(codgUsuario);
    }

    @GetMapping("/usuarios-internos/buscar")
    public RefUsuario buscarUsuarioInternoPorLogin(@RequestParam Integer codgUsuario,
                                                   @RequestParam String login) {
        return service.buscarUsuarioInternoPorLogin(codgUsuario, login);
    }

    @GetMapping("/usuarios-acesso/buscar")
    public RefUsuario buscarUsuarioParaAcessoPorLogin(@RequestParam Integer codgUsuario,
                                                      @RequestParam String login) {
        return service.buscarUsuarioParaAcessoPorLogin(codgUsuario, login);
    }

    @GetMapping("/perfis")
    public List<ChatPerfil> listarPerfis(@RequestParam Integer codgUsuario) {
        return service.listarPerfisGerenciaveis(codgUsuario);
    }

    @GetMapping("/usuario-perfis")
    public List<ChatUsuarioPerfil> listarUsuarioPerfis(@RequestParam Integer codgUsuario) {
        return service.listarUsuarioPerfis(codgUsuario);
    }

    @PostMapping("/usuario-perfis")
    public ChatUsuarioPerfil salvarUsuarioPerfil(@RequestParam Integer codgUsuario,
                                                 @RequestBody ChatUsuarioPerfil entity) {
        return service.salvarUsuarioPerfil(codgUsuario, entity);
    }

    @DeleteMapping("/usuario-perfis/{id}")
    public void excluirUsuarioPerfil(@RequestParam Integer codgUsuario, @PathVariable Long id) {
        service.excluirUsuarioPerfil(codgUsuario, id);
    }

    @GetMapping("/respostas-rapidas")
    public List<RespostaRapida> listarRespostasRapidas(@RequestParam Integer codgUsuario) {
        return service.listarRespostasRapidas(codgUsuario);
    }

    @PostMapping("/respostas-rapidas")
    public RespostaRapida salvarRespostaRapida(@RequestParam Integer codgUsuario,
                                               @RequestBody RespostaRapida entity) {
        return service.salvarRespostaRapida(codgUsuario, entity);
    }

    @DeleteMapping("/respostas-rapidas/{id}")
    public void excluirRespostaRapida(@RequestParam Integer codgUsuario, @PathVariable Long id) {
        service.excluirRespostaRapida(codgUsuario, id);
    }

    @GetMapping("/sla-politicas")
    public List<SlaPolitica> listarSlaPoliticas(@RequestParam Integer codgUsuario) {
        return service.listarSlaPoliticas(codgUsuario);
    }

    @PostMapping("/sla-politicas")
    public SlaPolitica salvarSlaPolitica(@RequestParam Integer codgUsuario,
                                         @RequestBody SlaPolitica politica) {
        return service.salvarSlaPolitica(codgUsuario, politica);
    }

    @PostMapping("/sla-politicas/sincronizar")
    public List<SlaPolitica> sincronizarSlaPoliticas(
            @RequestParam Integer codgUsuario,
            @RequestBody SlaPoliticaSincronizacaoRequest request) {
        return service.sincronizarSlaPoliticas(codgUsuario, request);
    }

    @DeleteMapping("/sla-politicas/{id}")
    public void excluirSlaPolitica(@RequestParam Integer codgUsuario, @PathVariable Long id) {
        service.excluirSlaPolitica(codgUsuario, id);
    }
}
