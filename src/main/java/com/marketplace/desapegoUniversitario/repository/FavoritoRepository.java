package com.marketplace.desapegoUniversitario.repository;

import com.marketplace.desapegoUniversitario.domain.Favorito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FavoritoRepository extends JpaRepository<Favorito, UUID> {

    Page<Favorito> findByUsuarioIdAndAnuncioRemovidoEmIsNullOrderByCriadoEmDesc(
            UUID usuarioId, Pageable pageable);

    Optional<Favorito> findByUsuarioIdAndAnuncioId(UUID usuarioId, UUID anuncioId);

    boolean existsByUsuarioIdAndAnuncioId(UUID usuarioId, UUID anuncioId);

    long countByAnuncioId(UUID anuncioId);
}
