package com.confApi.db.clube.cartaoUsuarioCia;

import com.confApi.db.clube.beneficioCartao.BeneficioCartao;
import com.confApi.db.clube.beneficioCartaoUsuario.BeneficioCartaoUsuarioService;
import com.confApi.db.clube.beneficioTransacaoCartao.BeneficioTransacaoCartao;
import com.confApi.db.clube.campanha.Campanha;
import com.confApi.db.clube.usuario.UsuarioClube;
import com.confApi.endPoints.clube.beneficioTransacaoCartao.BeneficioTransacaoCartaoApi;
import com.confApi.endPoints.clube.cartaoUsuarioCia.CartaoUsuarioCiaApi;
import com.confApi.endPoints.clube.usuario.UsuarioClubeApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CartaoUsuarioCiaService {

    @Autowired
    private CartaoUsuarioCiaApi cartaoUsuarioCiaApi;

    @Autowired
    private BeneficioCartaoUsuarioService beneficioCartaoUsuarioService;

    @Autowired
    private UsuarioClubeApi usuarioClubeApi;

    @Autowired
    private BeneficioTransacaoCartaoApi beneficioTransacaoCartaoApi;


    public ResponseEntity<List<CartaoUsuarioCia>> findAllByUsuario(Integer id) {
        return ResponseEntity.ok(cartaoUsuarioCiaApi.getAllByUsuario(id));
    }

    public List<CartaoUsuarioCia> lista(int idUsuarioManger) {
        try {
            System.out.println("lista---");
            System.out.println("idUsuarioManger --- "+ idUsuarioManger);
            // 1. Busca usuario do clube pelo ID do Manager
            UsuarioClube usuarioClube = usuarioClubeApi.consultaUsuarioIDExiste(idUsuarioManger);
            System.out.println("usuarioClube: " + usuarioClube);

            if (usuarioClube == null || usuarioClube.getCodgUsuario() == null) {
                System.out.println("usuarioClube nulo ou sem codgUsuario");
                return new ArrayList<>();
            }

            // 2. Busca cartões do usuario
            List<CartaoUsuarioCia> cartoes = cartaoUsuarioCiaApi.getAllByUsuario(usuarioClube.getCodgUsuario());
            System.out.println("cartoes size: " + (cartoes != null ? cartoes.size() : "null"));

            if (cartoes == null || cartoes.isEmpty()) {
                System.out.println("cartoes vazio ou nulo");
                return new ArrayList<>();
            }

            // 3. Para cada cartão verifica benefício utilizado
            for (CartaoUsuarioCia cartao : cartoes) {
                System.out.println("cartao: " + cartao.getCodgCartaoUsuarioCia());
                if (cartao.getCartaoCia() != null && cartao.getCartaoCia().getBeneficios() != null) {
                    for (BeneficioCartao beneficio : cartao.getCartaoCia().getBeneficios()) {
                        boolean utilizado = beneficioCartaoUsuarioService.verificaBeneficio(
                                cartao.getCodgCartaoUsuarioCia(),
                                beneficio.getCodgBeneficioCartao()
                        );
                        System.out.println("beneficio: " + beneficio.getCodgBeneficioCartao() + " utilizado: " + utilizado);
                        beneficio.setUtilizado(utilizado);
                    }
                }
            }

            return cartoes;

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public CartaoUsuarioCia getAllById(int id) {
        return cartaoUsuarioCiaApi.getAllById(id);
    }

    public CartaoUsuarioCiaDetalheDTO cartaoUsuarioById(int id) {
        try {
            // 1. Busca cartão por ID
            CartaoUsuarioCia cartao = cartaoUsuarioCiaApi.getAllById(id);
            if (cartao == null || cartao.getCodgCartaoUsuarioCia() == null) {
                return new CartaoUsuarioCiaDetalheDTO();
            }

            // 2. Busca extrato
            List<BeneficioTransacaoCartao> transacoes = beneficioTransacaoCartaoApi
                    .getAllByTransacaoUsuarioExtrato(cartao.getCodgCartaoUsuarioCia());

            // 3. Verifica benefícios utilizados e remove os não utilizados
            if (cartao.getCartaoCia() != null && cartao.getCartaoCia().getBeneficios() != null) {
                List<BeneficioCartao> beneficios = cartao.getCartaoCia().getBeneficios();
                for (BeneficioCartao beneficio : beneficios) {
                    boolean utilizado = beneficioCartaoUsuarioService.verificaBeneficio(
                            cartao.getCodgCartaoUsuarioCia(),
                            beneficio.getCodgBeneficioCartao()
                    );
                    beneficio.setUtilizado(utilizado);
                }
                beneficios.removeIf(b -> !b.getUtilizado());
            }

            return new CartaoUsuarioCiaDetalheDTO(cartao, transacoes);

        } catch (Exception e) {
            e.printStackTrace();
            return new CartaoUsuarioCiaDetalheDTO();
        }
    }
}
