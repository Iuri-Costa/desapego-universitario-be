package com.marketplace.desapegoUniversitario.controller;

import com.marketplace.desapegoUniversitario.dto.AnuncioResponseDTO;
import com.marketplace.desapegoUniversitario.dto.FavoritoResponseDTO;
import com.marketplace.desapegoUniversitario.dto.PaginadoDTO;
import com.marketplace.desapegoUniversitario.service.FavoritoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Favoritos", description = "Favoritos do usuário autenticado")
public class FavoritoController {

    private final FavoritoService favoritoService;

    public FavoritoController(FavoritoService favoritoService) {
        this.favoritoService = favoritoService;
    }

    @PostMapping("/anuncios/{id}/favoritos")
    public FavoritoResponseDTO adicionar(@PathVariable UUID id, Authentication authentication) {
        return favoritoService.adicionar(id, authentication.getName());
    }

    @DeleteMapping("/anuncios/{id}/favoritos")
    public ResponseEntity<Void> remover(@PathVariable UUID id, Authentication authentication) {
        favoritoService.remover(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/usuarios/me/favoritos")
    public PaginadoDTO<AnuncioResponseDTO> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return favoritoService.listar(authentication.getName(), page, size);
    }
}
