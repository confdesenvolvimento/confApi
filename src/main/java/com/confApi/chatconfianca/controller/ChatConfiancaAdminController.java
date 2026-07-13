package com.confApi.chatconfianca.controller;

import com.confApi.chatconfianca.dto.model.AtendenteStatus;
import com.confApi.chatconfianca.dto.model.ChatPerfil;
import com.confApi.chatconfianca.dto.model.ChatUsuarioPerfil;
import com.confApi.chatconfianca.dto.model.Departamento;
import com.confApi.chatconfianca.dto.model.DepartamentoAtendente;
import com.confApi.chatconfianca.dto.model.DepartamentoUnidade;
import com.confApi.chatconfianca.dto.model.ParametroSistema;
import com.confApi.chatconfianca.dto.model.RefAgencia;
import com.confApi.chatconfianca.dto.model.RefUnidade;
import com.confApi.chatconfianca.dto.model.RefUsuario;
import com.confApi.chatconfianca.dto.model.RespostaRapida;
import com.confApi.chatconfianca.dto.model.SlaPolitica;
import com.confApi.chatconfianca.service.ChatConfiancaConfigService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/chat-confianca/admin")
public class ChatConfiancaAdminController {
    private final ChatConfiancaConfigService service;

    public ChatConfiancaAdminController(ChatConfiancaConfigService service) {
        this.service = service;
    }

    @GetMapping("/departamentos")
    public List<Departamento> listarDepartamentos() {
        return service.listarDepartamentos();
    }

    @GetMapping("/departamentos/{id}")
    public Departamento buscarDepartamento(@PathVariable Long id) {
        return service.buscarDepartamento(id);
    }

    @PostMapping("/departamentos")
    public Departamento salvarDepartamento(@RequestBody Departamento departamento) {
        return service.salvarDepartamento(departamento);
    }

    @DeleteMapping("/departamentos/{id}")
    public void excluirDepartamento(@PathVariable Long id) {
        service.excluirDepartamento(id);
    }

    @GetMapping("/departamento-unidades")
    public List<DepartamentoUnidade> listarDepartamentoUnidades(@RequestParam(required = false) Long departamentoId,
                                                                @RequestParam(required = false) Integer codgUnidade) {
        if (departamentoId != null) {
            return service.listarDepartamentoUnidadesPorDepartamento(departamentoId);
        }
        if (codgUnidade != null) {
            return service.listarDepartamentoUnidadesPorUnidade(codgUnidade);
        }
        return service.listarDepartamentoUnidades();
    }

    @GetMapping("/departamento-unidades/{id}")
    public DepartamentoUnidade buscarDepartamentoUnidade(@PathVariable Long id) {
        return service.buscarDepartamentoUnidade(id);
    }

    @PostMapping("/departamento-unidades")
    public DepartamentoUnidade salvarDepartamentoUnidade(@RequestBody DepartamentoUnidade entity) {
        return service.salvarDepartamentoUnidade(entity);
    }

    @DeleteMapping("/departamento-unidades/{id}")
    public void excluirDepartamentoUnidade(@PathVariable Long id) {
        service.excluirDepartamentoUnidade(id);
    }

    @GetMapping("/departamento-atendentes")
    public List<DepartamentoAtendente> listarDepartamentoAtendentes(@RequestParam(required = false) Long departamentoUnidadeId,
                                                                    @RequestParam(required = false) Integer codgUsuario) {
        if (departamentoUnidadeId != null) {
            return service.listarAtendentesDepartamento(departamentoUnidadeId);
        }
        if (codgUsuario != null) {
            return service.listarDepartamentosAtendente(codgUsuario);
        }
        return service.listarDepartamentoAtendentes();
    }

    @GetMapping("/departamento-atendentes/{id}")
    public DepartamentoAtendente buscarDepartamentoAtendente(@PathVariable Long id) {
        return service.buscarDepartamentoAtendente(id);
    }

    @PostMapping("/departamento-atendentes")
    public DepartamentoAtendente salvarDepartamentoAtendente(@RequestBody DepartamentoAtendente entity) {
        return service.salvarDepartamentoAtendente(entity);
    }

    @DeleteMapping("/departamento-atendentes/{id}")
    public void excluirDepartamentoAtendente(@PathVariable Long id) {
        service.excluirDepartamentoAtendente(id);
    }

    @GetMapping("/perfis")
    public List<ChatPerfil> listarPerfis() {
        return service.listarPerfis();
    }

    @GetMapping("/perfis/{id}")
    public ChatPerfil buscarPerfil(@PathVariable Long id) {
        return service.buscarPerfil(id);
    }

    @PostMapping("/perfis")
    public ChatPerfil salvarPerfil(@RequestBody ChatPerfil perfil) {
        return service.salvarPerfil(perfil);
    }

    @DeleteMapping("/perfis/{id}")
    public void excluirPerfil(@PathVariable Long id) {
        service.excluirPerfil(id);
    }

    @GetMapping("/usuario-perfis")
    public List<ChatUsuarioPerfil> listarUsuarioPerfis(@RequestParam(required = false) Integer codgUsuario) {
        if (codgUsuario != null) {
            return service.listarPerfisUsuario(codgUsuario);
        }
        return service.listarUsuarioPerfis();
    }

    @GetMapping("/usuario-perfis/{id}")
    public ChatUsuarioPerfil buscarUsuarioPerfil(@PathVariable Long id) {
        return service.buscarUsuarioPerfil(id);
    }

    @PostMapping("/usuario-perfis")
    public ChatUsuarioPerfil salvarUsuarioPerfil(@RequestBody ChatUsuarioPerfil entity) {
        return service.salvarUsuarioPerfil(entity);
    }

    @DeleteMapping("/usuario-perfis/{id}")
    public void excluirUsuarioPerfil(@PathVariable Long id) {
        service.excluirUsuarioPerfil(id);
    }

    @GetMapping("/respostas-rapidas")
    public List<RespostaRapida> listarRespostasRapidas(@RequestParam(required = false) Long departamentoId,
                                                       @RequestParam(required = false) Integer codgUnidade,
                                                       @RequestParam(defaultValue = "false") Boolean somenteAtivas) {
        return service.listarRespostasRapidas(departamentoId, codgUnidade, somenteAtivas);
    }

    @GetMapping("/respostas-rapidas/{id}")
    public RespostaRapida buscarRespostaRapida(@PathVariable Long id) {
        return service.buscarRespostaRapida(id);
    }

    @PostMapping("/respostas-rapidas")
    public RespostaRapida salvarRespostaRapida(@RequestBody RespostaRapida entity) {
        return service.salvarRespostaRapida(entity);
    }

    @DeleteMapping("/respostas-rapidas/{id}")
    public void excluirRespostaRapida(@PathVariable Long id) {
        service.excluirRespostaRapida(id);
    }

    @GetMapping("/atendente-status")
    public List<AtendenteStatus> listarAtendenteStatus() {
        return service.listarAtendenteStatus();
    }

    @GetMapping("/atendente-status/{codgUsuario}")
    public AtendenteStatus buscarAtendenteStatus(@PathVariable Integer codgUsuario) {
        return service.buscarAtendenteStatus(codgUsuario);
    }

    @PostMapping("/atendente-status")
    public AtendenteStatus salvarAtendenteStatus(@RequestBody AtendenteStatus status) {
        return service.salvarAtendenteStatus(status);
    }

    @DeleteMapping("/atendente-status/{codgUsuario}")
    public void excluirAtendenteStatus(@PathVariable Integer codgUsuario) {
        service.excluirAtendenteStatus(codgUsuario);
    }

    @GetMapping("/parametros")
    public List<ParametroSistema> listarParametros() {
        return service.listarParametros();
    }

    @PostMapping("/parametros")
    public ParametroSistema salvarParametro(@RequestBody ParametroSistema parametro) {
        return service.salvarParametro(parametro);
    }

    @DeleteMapping("/parametros/{chave}")
    public void excluirParametro(@PathVariable String chave) {
        service.excluirParametro(chave);
    }

    @GetMapping("/sla-politicas")
    public List<SlaPolitica> listarSlaPoliticas() {
        return service.listarSlaPoliticas();
    }

    @PostMapping("/sla-politicas")
    public SlaPolitica salvarSlaPolitica(@RequestBody SlaPolitica politica) {
        return service.salvarSlaPolitica(politica);
    }

    @DeleteMapping("/sla-politicas/{id}")
    public void excluirSlaPolitica(@PathVariable Long id) {
        service.excluirSlaPolitica(id);
    }

    @GetMapping("/referencias/usuarios")
    public List<RefUsuario> listarUsuariosReferencia() {
        return service.listarUsuariosReferencia();
    }

    @GetMapping("/referencias/usuarios/buscar")
    public RefUsuario buscarUsuarioReferenciaPorLogin(@RequestParam String login) {
        return service.buscarUsuarioReferenciaPorLogin(login);
    }

    @GetMapping("/referencias/agencias")
    public List<RefAgencia> listarAgenciasReferencia() {
        return service.listarAgenciasReferencia();
    }

    @GetMapping("/referencias/unidades")
    public List<RefUnidade> listarUnidadesReferencia() {
        return service.listarUnidadesReferencia();
    }
}