package com.marketplace.desapegoUniversitario.repository;

import com.marketplace.desapegoUniversitario.domain.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

    Optional<Reserva> findByAnuncioIdAndCanceladaEmIsNullAndConcluidaEmIsNull(UUID anuncioId);

    Optional<Reserva> findByAnuncioIdAndCompradorIdAndConcluidaEmIsNotNull(
            UUID anuncioId, UUID compradorId);

    boolean existsByAnuncioIdAndCompradorIdAndCanceladaEmIsNull(UUID anuncioId, UUID compradorId);
}
