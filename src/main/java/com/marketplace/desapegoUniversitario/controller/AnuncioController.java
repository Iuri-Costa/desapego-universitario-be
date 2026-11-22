package com.marketplace.desapegoUniversitario.controller;

import com.marketplace.desapegoUniversitario.domain.Categoria;
import com.marketplace.desapegoUniversitario.dto.AnuncioPatchDTO;
import com.marketplace.desapegoUniversitario.dto.AnuncioRequestDTO;
import com.marketplace.desapegoUniversitario.dto.AnuncioResponseDTO;
import com.marketplace.desapegoUniversitario.service.AnuncioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/anuncios")
public class AnuncioController {

    private final AnuncioService anuncioService;

    public AnuncioController(AnuncioService anuncioService) {
        this.anuncioService = anuncioService;
    }

    @PostMapping
    public AnuncioResponseDTO criar(@Valid @RequestBody AnuncioRequestDTO dto, Authentication authentication) {
        return anuncioService.criar(dto, authentication.getName());
    }

    @GetMapping
    public List<AnuncioResponseDTO> listarTodos() {
        return anuncioService.listarTodos();
    }

    @GetMapping("/{id}")
    public AnuncioResponseDTO buscarPorId(@PathVariable UUID id) {
        return anuncioService.buscarPorId(id);
    }

    @GetMapping("/categoria/{categoria}")
    public List<AnuncioResponseDTO> listarPorCategoria(@PathVariable Categoria categoria) {
        return anuncioService.listarPorCategoria(categoria);
    }

    @PutMapping("/{id}")
    public AnuncioResponseDTO atualizar(@PathVariable UUID id,
                                        @Valid @RequestBody AnuncioRequestDTO dto,
                                        Authentication authentication) {
        return anuncioService.atualizar(id, dto, authentication.getName());
    }

    @PatchMapping("/{id}")
    public AnuncioResponseDTO atualizarParcial(@PathVariable UUID id,
                                                @Valid @RequestBody AnuncioPatchDTO dto,
                                                Authentication authentication) {
        return anuncioService.atualizarParcial(id, dto, authentication.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarPorId(@PathVariable UUID id, Authentication authentication) {
        anuncioService.deletarPorID(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
