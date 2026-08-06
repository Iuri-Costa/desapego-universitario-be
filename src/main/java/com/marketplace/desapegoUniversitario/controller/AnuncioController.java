package com.marketplace.desapegoUniversitario.controller;

import com.marketplace.desapegoUniversitario.model.Anuncio;
import com.marketplace.desapegoUniversitario.model.Categoria;
import com.marketplace.desapegoUniversitario.service.AnuncioService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/anuncios")
public class AnuncioController {
    private final AnuncioService anuncioService;

    public AnuncioController(AnuncioService anuncioService) {
        this.anuncioService = anuncioService;
    }

    @PostMapping
    public Anuncio criar(@RequestBody Anuncio anuncio) {
        return anuncioService.criar(anuncio);
    }

    @GetMapping
    public List<Anuncio> listarTodos(){
        return anuncioService.listarTodos();
    }

    @GetMapping("categoria/{categoria}")
    public List<Anuncio> listarPorCategoria(@PathVariable Categoria categoria){
        return anuncioService.listarPorCategoria(categoria);
    }

    @DeleteMapping("/{id}")
    public void deletarPorId(@PathVariable UUID id){
        anuncioService.deletarPorID(id);
    }
}
