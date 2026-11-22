package com.marketplace.desapegoUniversitario.controller;

import com.marketplace.desapegoUniversitario.dto.CadastroDTO;
import com.marketplace.desapegoUniversitario.dto.LoginDTO;
import com.marketplace.desapegoUniversitario.dto.UsuarioResponseDTO;
import com.marketplace.desapegoUniversitario.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/cadastro")
    public UsuarioResponseDTO cadastrar(@Valid @RequestBody CadastroDTO dto) {
        return usuarioService.cadastrar(dto);
    }

    @PostMapping("/login")
    public UsuarioResponseDTO login(@Valid @RequestBody LoginDTO dto) {
        return usuarioService.login(dto);
    }
}
