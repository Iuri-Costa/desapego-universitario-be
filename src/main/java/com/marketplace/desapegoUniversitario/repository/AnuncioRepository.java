package com.marketplace.desapegoUniversitario.repository;

import com.marketplace.desapegoUniversitario.model.Anuncio;
import com.marketplace.desapegoUniversitario.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnuncioRepository extends JpaRepository<Anuncio, UUID> {
    List<Anuncio> findByCategoria(Categoria categoria);
}