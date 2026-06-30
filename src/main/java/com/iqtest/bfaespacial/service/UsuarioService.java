package com.iqtest.bfaespacial.service;

import com.iqtest.bfaespacial.model.Usuario;
import com.iqtest.bfaespacial.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario u = repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        if (!u.isActivo()) {
            throw new UsernameNotFoundException("Usuario inactivo: " + username);
        }
        return new User(
                u.getUsername(),
                u.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(u.getRol()))
        );
    }

    public List<Usuario> listar() {
        return repo.findAll();
    }

    @Transactional
    public Usuario crear(String username, String password, String rol) {
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setPassword(encoder.encode(password));
        u.setRol(rol.startsWith("ROLE_") ? rol : "ROLE_" + rol.toUpperCase());
        u.setActivo(true);
        return repo.save(u);
    }

    @Transactional
    public void cambiarActivo(Long id) {
        Usuario u = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no existe: " + id));
        u.setActivo(!u.isActivo());
        repo.save(u);
    }
}
