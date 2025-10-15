package com.confApi.service;

import com.confApi.entity.Usuario;
import com.confApi.repo.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioServiceImpl implements UserDetailsService {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Usuario user = usuarioRepository.findByLogin(username);
        //System.out.println("Usuario:UserDetails " + user.getLogin() + " - " + user.getRole());

        String[] roles = new String[] { user.getRole() };

        return User.builder().username(user.getLogin()).password(user.getSenha()).roles(roles).build();

    }
}
