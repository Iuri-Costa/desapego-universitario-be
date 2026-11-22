package com.marketplace.desapegoUniversitario.controller;

import com.marketplace.desapegoUniversitario.dto.AnuncioResponseDTO;
import com.marketplace.desapegoUniversitario.dto.ModeracaoDTO;
import com.marketplace.desapegoUniversitario.service.AnuncioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/anuncios")
public class AdminController {

    private final AnuncioService anuncioService;

    public AdminController(AnuncioService anuncioService) {
        this.anuncioService = anuncioService;
    }

    @PatchMapping("/{id}/moderacao")
    public AnuncioResponseDTO moderar(@PathVariable UUID id, @Valid @RequestBody ModeracaoDTO dto) {
        return anuncioService.moderar(id, dto.moderado());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        anuncioService.removerPorModeracao(id);
        return ResponseEntity.noContent().build();
    }
}
