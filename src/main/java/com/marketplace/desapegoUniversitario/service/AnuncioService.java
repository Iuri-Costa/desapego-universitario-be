package com.marketplace.desapegoUniversitario.service;
import com.marketplace.desapegoUniversitario.model.Anuncio;
import com.marketplace.desapegoUniversitario.model.Categoria;
import com.marketplace.desapegoUniversitario.repository.AnuncioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


@Service
public class AnuncioService {
    private final AnuncioRepository anuncioRepository;

    public AnuncioService(AnuncioRepository anuncioRepository){
        this.anuncioRepository = anuncioRepository;
    }

    public Anuncio criar(Anuncio anuncio){
        return anuncioRepository.save(anuncio);
    }

    public List<Anuncio> listarTodos(){
        return anuncioRepository.findAll();
    }

    public void deletarPorID(UUID id){
        anuncioRepository.deleteById(id);
    }

    public List<Anuncio> listarPorCategoria(Categoria categoria){
        return anuncioRepository.findByCategoria(categoria);
    }

}
