package com.marketplace.desapegoUniversitario.controller;

import com.marketplace.desapegoUniversitario.dto.CadastroDTO;
import com.marketplace.desapegoUniversitario.dto.LoginDTO;
import com.marketplace.desapegoUniversitario.dto.MensagemDTO;
import com.marketplace.desapegoUniversitario.dto.RedefinirSenhaDTO;
import com.marketplace.desapegoUniversitario.dto.RecuperarSenhaDTO;
import com.marketplace.desapegoUniversitario.dto.TokenEmailDTO;
import com.marketplace.desapegoUniversitario.dto.TokenResponseDTO;
import com.marketplace.desapegoUniversitario.service.AutenticacaoService;
import com.marketplace.desapegoUniversitario.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuários", description = "Cadastro, login legado, verificação e recuperação de senha")
public class UsuarioController {

    private static final String SUNSET_LOGIN_LEGADO = "Wed, 31 Mar 2027 00:00:00 GMT";

    private final UsuarioService usuarioService;
    private final AutenticacaoService autenticacaoService;

    public UsuarioController(UsuarioService usuarioService, AutenticacaoService autenticacaoService) {
        this.usuarioService = usuarioService;
        this.autenticacaoService = autenticacaoService;
    }

    @PostMapping("/cadastro")
    public MensagemDTO cadastrar(@Valid @RequestBody CadastroDTO dto) {
        usuarioService.cadastrar(dto);
        return new MensagemDTO("Se o e-mail for elegível, enviamos instruções para concluir o cadastro");
    }

    @PostMapping("/login")
    @Operation(deprecated = true, summary = "Login legado — use POST /api/v1/auth/login",
            description = "Alias deprecated de /auth/login; emite o mesmo par access/refresh token.")
    public ResponseEntity<TokenResponseDTO> login(@Valid @RequestBody LoginDTO dto) {
        return ResponseEntity.ok()
                .header("Deprecation", "true")
                .header("Sunset", SUNSET_LOGIN_LEGADO)
                .header(HttpHeaders.LINK, "</api/v1/auth/login>; rel=\"successor-version\"")
                .body(autenticacaoService.login(dto));
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
