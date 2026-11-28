package com.marketplace.desapegoUniversitario.security;

import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return usuarioRepository.findByEmail(email)
                .map(usuario -> new UsuarioPrincipal(
                        usuario.getEmail(),
                        usuario.getSenha(),
                        List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name())),
                        usuario.getTokenVersion()))
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }
}
