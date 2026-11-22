package com.marketplace.desapegoUniversitario.repository;

import com.marketplace.desapegoUniversitario.domain.Denuncia;
import com.marketplace.desapegoUniversitario.domain.StatusDenuncia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DenunciaRepository extends JpaRepository<Denuncia, UUID> {

    Page<Denuncia> findByStatusOrderByCriadoEmAsc(StatusDenuncia status, Pageable pageable);

    Page<Denuncia> findAllByOrderByCriadoEmAsc(Pageable pageable);

    Optional<Denuncia> findByAnuncioIdAndDenuncianteIdAndStatus(
            UUID anuncioId, UUID denuncianteId, StatusDenuncia status);

    Page<Denuncia> findByDenuncianteIdOrderByCriadoEmDesc(UUID denuncianteId, Pageable pageable);

    Optional<Denuncia> findByIdAndDenuncianteId(UUID id, UUID denuncianteId);
}
