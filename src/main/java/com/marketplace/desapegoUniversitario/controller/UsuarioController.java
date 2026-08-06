package com.marketplace.desapegoUniversitario.controller;

import com.marketplace.desapegoUniversitario.dto.CadastroDTO;
import com.marketplace.desapegoUniversitario.dto.LoginDTO;
import com.marketplace.desapegoUniversitario.dto.UsuarioResponseDTO;
import com.marketplace.desapegoUniversitario.service.UsuarioService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/cadastro")
    public UsuarioResponseDTO cadastrar(@RequestBody CadastroDTO dto) {
        return usuarioService.cadastrar(dto);
    }

    @PostMapping("/login")
    public UsuarioResponseDTO login(@RequestBody LoginDTO dto) {
        return usuarioService.login(dto);
    }
}