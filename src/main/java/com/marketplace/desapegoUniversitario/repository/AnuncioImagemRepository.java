package com.marketplace.desapegoUniversitario.repository;

import com.marketplace.desapegoUniversitario.domain.AnuncioImagem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnuncioImagemRepository extends JpaRepository<AnuncioImagem, UUID> {
    List<AnuncioImagem> findByAnuncioIdOrderByOrdemAscCriadoEmAscIdAsc(UUID anuncioId);

    List<AnuncioImagem> findByAnuncioIdInOrderByAnuncioIdAscOrdemAscCriadoEmAscIdAsc(
            Collection<UUID> anuncioIds);

    Optional<AnuncioImagem> findByIdAndAnuncioId(UUID id, UUID anuncioId);

    long countByAnuncioId(UUID anuncioId);
}
