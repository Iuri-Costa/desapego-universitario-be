package com.marketplace.desapegoUniversitario.repository;

import com.marketplace.desapegoUniversitario.domain.Anuncio;
import com.marketplace.desapegoUniversitario.domain.Categoria;
import com.marketplace.desapegoUniversitario.domain.StatusAnuncio;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnuncioRepository extends JpaRepository<Anuncio, UUID>, JpaSpecificationExecutor<Anuncio> {
    List<Anuncio> findAllByRemovidoEmIsNull();

    List<Anuncio> findByCategoriaAndRemovidoEmIsNull(Categoria categoria);

    Optional<Anuncio> findByIdAndRemovidoEmIsNull(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Anuncio a where a.id = :id and a.removidoEm is null")
    Optional<Anuncio> findByIdForUpdate(@Param("id") UUID id);

    Page<Anuncio> findByUsuarioIdAndRemovidoEmIsNull(UUID usuarioId, Pageable pageable);

    Page<Anuncio> findByUsuarioId(UUID usuarioId, Pageable pageable);

    Page<Anuncio> findByUsuarioIdAndStatusAndRemovidoEmIsNull(
            UUID usuarioId, StatusAnuncio status, Pageable pageable);

    Page<Anuncio> findByUsuarioIdAndStatus(UUID usuarioId, StatusAnuncio status, Pageable pageable);

    long countByUsuarioIdAndRemovidoEmIsNull(UUID usuarioId);

    long countByUsuarioIdAndStatusAndRemovidoEmIsNull(UUID usuarioId, StatusAnuncio status);
}
