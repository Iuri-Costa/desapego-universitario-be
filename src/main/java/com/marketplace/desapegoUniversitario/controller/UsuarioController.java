package com.marketplace.desapegoUniversitario.controller;

import com.marketplace.desapegoUniversitario.dto.CadastroDTO;
import com.marketplace.desapegoUniversitario.dto.LoginDTO;
import com.marketplace.desapegoUniversitario.dto.RedefinirSenhaDTO;
import com.marketplace.desapegoUniversitario.dto.RecuperarSenhaDTO;
import com.marketplace.desapegoUniversitario.dto.TokenEmailDTO;
import com.marketplace.desapegoUniversitario.dto.TokenResponseDTO;
import com.marketplace.desapegoUniversitario.dto.UsuarioResponseDTO;
import com.marketplace.desapegoUniversitario.service.AutenticacaoService;
import com.marketplace.desapegoUniversitario.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AutenticacaoService autenticacaoService;

    public UsuarioController(UsuarioService usuarioService, AutenticacaoService autenticacaoService) {
        this.usuarioService = usuarioService;
        this.autenticacaoService = autenticacaoService;
    }

    @PostMapping("/cadastro")
    public UsuarioResponseDTO cadastrar(@Valid @RequestBody CadastroDTO dto) {
        return usuarioService.cadastrar(dto);
    }

    @PostMapping("/login")
    public TokenResponseDTO login(@Valid @RequestBody LoginDTO dto) {
        return autenticacaoService.login(dto);
    }

    @PostMapping("/verificar-email")
    public ResponseEntity<Void> verificarEmail(@Valid @RequestBody TokenEmailDTO dto) {
        usuarioService.verificarEmail(dto.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reenviar-verificacao")
    public ResponseEntity<Void> reenviarVerificacao(@Valid @RequestBody RecuperarSenhaDTO dto) {
        usuarioService.reenviarVerificacao(dto.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/recuperar-senha")
    public ResponseEntity<Void> recuperarSenha(@Valid @RequestBody RecuperarSenhaDTO dto) {
        usuarioService.recuperarSenha(dto.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<Void> redefinirSenha(@Valid @RequestBody RedefinirSenhaDTO dto) {
        usuarioService.redefinirSenha(dto);
        return ResponseEntity.noContent().build();
    }
}
