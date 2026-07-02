package com.confApi.db.clube.cartaoUsuarioCia;

import com.confApi.db.clube.campanha.Campanha;
import com.confApi.endPoints.clube.cartaoUsuarioCia.CartaoUsuarioCiaApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartaoUsuarioCiaService {

    @Autowired
    private CartaoUsuarioCiaApi cartaoUsuarioCiaApi;


    public ResponseEntity<List<CartaoUsuarioCia>> findAllByUsuario(Integer id) {
        return ResponseEntity.ok(cartaoUsuarioCiaApi.getAllByUsuario(id));
    }

    public CartaoUsuarioCia getAllById(int id) {
        return cartaoUsuarioCiaApi.getAllById(id);
    }
}
