package com.marketplace.desapegoUniversitario.controller;

import com.marketplace.desapegoUniversitario.domain.Categoria;
import com.marketplace.desapegoUniversitario.dto.AnuncioRequestDTO;
import com.marketplace.desapegoUniversitario.dto.AnuncioResponseDTO;
import com.marketplace.desapegoUniversitario.service.AnuncioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    public AnuncioResponseDTO criar(@Valid @RequestBody AnuncioRequestDTO dto) {
        return anuncioService.criar(dto);
    }

    @GetMapping
    public List<AnuncioResponseDTO> listarTodos() {
        return anuncioService.listarTodos();
    }

    @GetMapping("/categoria/{categoria}")
    public List<AnuncioResponseDTO> listarPorCategoria(@PathVariable Categoria categoria) {
        return anuncioService.listarPorCategoria(categoria);
    }

    @DeleteMapping("/{id}")
    public void deletarPorId(@PathVariable UUID id) {
        anuncioService.deletarPorID(id);
    }
}
