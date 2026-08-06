package com.marketplace.desapegoUniversitario.service;

import com.marketplace.desapegoUniversitario.model.Usuario;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository){
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario criar(Usuario usuario){
        return usuarioRepository.save(usuario);
    }

    public List<Usuario> listarTodos(){
        return usuarioRepository.findAll();
    }

    public void deletarPorID(UUID id){
        usuarioRepository.deleteById(id);
    }
}