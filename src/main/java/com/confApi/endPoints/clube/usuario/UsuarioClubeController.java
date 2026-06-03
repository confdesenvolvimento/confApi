package com.confApi.endPoints.clube.usuario;

import com.confApi.db.clube.usuario.Usuario;
import com.confApi.db.clube.usuario.UsuarioClubeService;
import com.confApi.db.clube.usuario.dto.AgenciaDTO;
import com.confApi.db.clube.usuario.dto.UnidadeDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clube/usuario")
public class UsuarioClubeController {

    @Autowired
    private UsuarioClubeService usuarioClubeService;

    @Autowired
    private UsuarioClubeApi usuarioClubeApi;

    /*ta pronto*/
    @GetMapping("/findAllPage")
    public Page<Usuario> findAllPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String nome) {

        System.out.println("aki " + page + " " + size + " " + nome);
        return usuarioClubeService.findAllPage(page, size, nome);
    }
    /*ta pronto*/
    @PostMapping()
    public ResponseEntity<?> create(@RequestBody Usuario usuario) {
        Usuario criado = usuarioClubeApi.create(usuario);
        if (criado != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(criado);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
    /*ta pronto*/
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody Usuario usuario) {
        System.out.println("atualizar aki " + id + " " + usuario);
        Usuario atualizado = usuarioClubeApi.update(id, usuario);
        System.out.println("atualizado : "+atualizado);
        if (atualizado != null) {
            return ResponseEntity.ok(atualizado);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /*ta pronto*/
    @GetMapping()
    public ResponseEntity<List<Usuario>> getAll() {
        return ResponseEntity.ok(usuarioClubeApi.getAll());
    }

    /*ta pronto*/
    @GetMapping("/consultaUsuarioExiste/{loginUsuario}")
    public ResponseEntity<?> consultaUsuarioExiste(@PathVariable String loginUsuario) {
        System.out.println("aki "+loginUsuario);
        Usuario usuario = usuarioClubeApi.consultaUsuarioExiste(loginUsuario);
        System.out.println("encontrou "+usuario);
        if (usuario != null) {
            return ResponseEntity.ok(usuario);
        }
        return ResponseEntity.notFound().build();
    }
    /*ta pronto*/
    @GetMapping("/consultaUsuarioIDExiste/{idUsuarioManger}")
    public ResponseEntity<?> consultaUsuarioIDExiste(@PathVariable int idUsuarioManger) {
        Usuario usuario = usuarioClubeApi.consultaUsuarioIDExiste(idUsuarioManger);
        System.out.println("encontrou "+usuario);
        if (usuario != null) {
            return ResponseEntity.ok(usuario);
        }
        return ResponseEntity.notFound().build();
    }
    /*ta pronto*/
    @PostMapping("/listaParams")
    public List<Usuario> listaParams(@RequestBody Usuario usuario) {
        System.out.println(usuario);
        return usuarioClubeService.listaParams(usuario);
    }

    /*ta pronto*/
    @GetMapping("/distinctUnidades")
    public List<UnidadeDTO> getDistinctUnidades() {
        System.out.println(usuarioClubeApi.getDistinctUnidades());
        return usuarioClubeApi.getDistinctUnidades();
    }

    /*ta pronto*/
    @GetMapping("/distinctAgencias/{idWoobaUnidade}")
    public List<AgenciaDTO> getDistinctAgencias(@PathVariable Integer idWoobaUnidade) {
        System.out.println("aki "+idWoobaUnidade);
        return usuarioClubeApi.getDistinctAgencias(idWoobaUnidade);
    }

}
