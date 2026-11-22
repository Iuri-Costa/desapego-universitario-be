package com.marketplace.desapegoUniversitario.repository;

import com.marketplace.desapegoUniversitario.domain.Anuncio;
import com.marketplace.desapegoUniversitario.domain.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnuncioRepository extends JpaRepository<Anuncio, UUID>, JpaSpecificationExecutor<Anuncio> {
    List<Anuncio> findByCategoria(Categoria categoria);

    List<Anuncio> findAllByRemovidoEmIsNull();

    List<Anuncio> findByCategoriaAndRemovidoEmIsNull(Categoria categoria);

    Optional<Anuncio> findByIdAndRemovidoEmIsNull(UUID id);
}
