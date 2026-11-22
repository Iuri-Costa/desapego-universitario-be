package com.marketplace.desapegoUniversitario.repository;

import com.marketplace.desapegoUniversitario.domain.DispositivoFCM;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DispositivoFCMRepository extends JpaRepository<DispositivoFCM, UUID> {

    List<DispositivoFCM> findByUsuarioId(UUID usuarioId);

    Optional<DispositivoFCM> findByToken(String token);
}
