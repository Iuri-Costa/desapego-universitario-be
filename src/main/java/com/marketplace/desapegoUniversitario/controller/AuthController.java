package com.marketplace.desapegoUniversitario.controller;

import com.marketplace.desapegoUniversitario.dto.LoginDTO;
import com.marketplace.desapegoUniversitario.dto.RefreshRequestDTO;
import com.marketplace.desapegoUniversitario.dto.TokenResponseDTO;
import com.marketplace.desapegoUniversitario.exception.ErroResponse;
import com.marketplace.desapegoUniversitario.service.AutenticacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Login, renovação e logout de access tokens JWT")
public class AuthController {

    private final AutenticacaoService autenticacaoService;

    public AuthController(AutenticacaoService autenticacaoService) {
        this.autenticacaoService = autenticacaoService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica e emite tokens",
            description = "Retorna access token (15 min) e refresh token (7 dias).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens emitidos",
                    content = @Content(schema = @Schema(implementation = TokenResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "429", description = "Limite de tentativas excedido",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    public TokenResponseDTO login(@Valid @RequestBody LoginDTO dto) {
        return autenticacaoService.login(dto);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renova o access token",
            description = "Usa o refresh token uma vez; a rotação revoga o token anterior.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Novos tokens emitidos",
                    content = @Content(schema = @Schema(implementation = TokenResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Refresh token inválido ou reutilizado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    public TokenResponseDTO refresh(@Valid @RequestBody RefreshRequestDTO dto) {
        return autenticacaoService.renovar(dto.refreshToken());
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoga a família de refresh tokens do usuário")
    @ApiResponse(responseCode = "204", description = "Logout realizado")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails usuario) {
        autenticacaoService.logout(usuario.getUsername());
        return ResponseEntity.noContent().build();
    }
}
