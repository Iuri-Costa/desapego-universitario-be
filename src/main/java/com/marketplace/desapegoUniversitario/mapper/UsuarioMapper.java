package com.marketplace.desapegoUniversitario.mapper;

import com.marketplace.desapegoUniversitario.domain.Role;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.dto.CadastroDTO;
import com.marketplace.desapegoUniversitario.dto.UsuarioResponseDTO;

public final class UsuarioMapper {

    private UsuarioMapper() {
    }

    public static Usuario toEntity(CadastroDTO dto, String senhaHash) {
        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setSenha(senhaHash);
        usuario.setRole(Role.USUARIO);
        usuario.setEmailVerificado(false);
        return usuario;
    }

    public static UsuarioResponseDTO toResponse(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getNome(), usuario.getEmail(), usuario.getRole().name(), usuario.isEmailVerificado());
    }
}
