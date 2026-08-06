package com.marketplace.desapegoUniversitario.service;

import com.marketplace.desapegoUniversitario.dto.CadastroDTO;
import com.marketplace.desapegoUniversitario.dto.LoginDTO;
import com.marketplace.desapegoUniversitario.dto.UsuarioResponseDTO;
import com.marketplace.desapegoUniversitario.model.Usuario;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository){
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public UsuarioResponseDTO cadastrar(CadastroDTO dto){
        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setSenha(passwordEncoder.encode(dto.senha()));

        Usuario salvo = usuarioRepository.save(usuario);

        return new UsuarioResponseDTO(salvo.getNome(), salvo.getEmail(), null);
    }

    public UsuarioResponseDTO login(LoginDTO dto){
        Usuario usuario = usuarioRepository.findByEmail(dto.email())
                .orElseThrow(() -> new RuntimeException("Usuario nao encontrado"));

        if (!passwordEncoder.matches(dto.senha(), usuario.getSenha())){
            throw new RuntimeException("Senha invalida");
        }

        return new UsuarioResponseDTO(usuario.getNome(), usuario.getEmail(), null);
    }

    public List<Usuario> listarTodos(){
        return usuarioRepository.findAll();
    }

    public void deletarPorID(UUID id){
        usuarioRepository.deleteById(id);
    }
}