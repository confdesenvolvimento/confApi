package com.confApi.endPoints.clube.cartaoUsuarioCia;

import com.confApi.db.clube.cartaoUsuarioCia.CartaoUsuarioCia;
import com.confApi.db.clube.cartaoUsuarioCia.CartaoUsuarioCiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clube/cartaoUsuarioCia")
public class CartaoUsuarioCiaController {

    @Autowired
    private CartaoUsuarioCiaService cartaoUsuarioCiaService;

    @GetMapping("/AllByCartaoUsuario/{id}")
    public ResponseEntity<List<CartaoUsuarioCia>> getAllByUsuario(@PathVariable int id) {
        System.out.println("cartaoUsuarioCia : "+id);
        return cartaoUsuarioCiaService.findAllByUsuario(id);
    }

    @GetMapping("/cartaoUsuario/AllById/{id}")
    public CartaoUsuarioCia getAllById(@PathVariable int id) {
        System.out.println("------AllById------ : "+id);
        System.out.println("cartaoUsuarioCia : "+id);
        System.out.println("cartaoUsuarioCia : "+cartaoUsuarioCiaService.getAllById(id));
        return cartaoUsuarioCiaService.getAllById(id);
    }
}
