package com.marketplace.desapegoUniversitario.repository;

import com.marketplace.desapegoUniversitario.domain.Reserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

    Optional<Reserva> findByAnuncioIdAndCanceladaEmIsNullAndConcluidaEmIsNullAndExpiradaEmIsNull(
            UUID anuncioId);

    Optional<Reserva> findByAnuncioIdAndCompradorIdAndConcluidaEmIsNotNull(
            UUID anuncioId, UUID compradorId);

    boolean existsByAnuncioIdAndCompradorIdAndCanceladaEmIsNullAndExpiradaEmIsNull(
            UUID anuncioId, UUID compradorId);

    Optional<Reserva> findTopByAnuncioIdOrderByCriadoEmDesc(UUID anuncioId);

    List<Reserva> findByCanceladaEmIsNullAndConcluidaEmIsNullAndExpiradaEmIsNullAndCriadoEmBefore(
            Instant limite);

    @Query("""
            select r from Reserva r
            where r.comprador.id = :usuarioId or r.anuncio.usuario.id = :usuarioId
            order by r.criadoEm desc
            """)
    Page<Reserva> findHistorico(@Param("usuarioId") UUID usuarioId, Pageable pageable);
}
