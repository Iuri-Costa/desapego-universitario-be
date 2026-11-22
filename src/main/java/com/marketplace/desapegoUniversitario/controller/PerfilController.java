package com.marketplace.desapegoUniversitario.controller;

import com.marketplace.desapegoUniversitario.domain.StatusAnuncio;
import com.marketplace.desapegoUniversitario.dto.AnuncioResponseDTO;
import com.marketplace.desapegoUniversitario.dto.AtualizarPerfilDTO;
import com.marketplace.desapegoUniversitario.dto.PaginadoDTO;
import com.marketplace.desapegoUniversitario.dto.PerfilResponseDTO;
import com.marketplace.desapegoUniversitario.service.PerfilService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios/me")
@Tag(name = "Perfil", description = "Dados do usuário autenticado e histórico")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping
    public PerfilResponseDTO buscar(Authentication authentication) {
        return perfilService.buscar(authentication.getName());
    }

    @PatchMapping
    public PerfilResponseDTO atualizar(@Valid @RequestBody AtualizarPerfilDTO dto,
                                       Authentication authentication) {
        return perfilService.atualizar(authentication.getName(), dto);
    }

    @GetMapping("/anuncios")
    public PaginadoDTO<AnuncioResponseDTO> historico(
            @RequestParam(required = false) StatusAnuncio status,
            @RequestParam(defaultValue = "false") boolean incluirRemovidos,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return perfilService.historico(authentication.getName(), status, incluirRemovidos, page, size);
    }
}
