package com.marketplace.desapegoUniversitario.repository;

import com.marketplace.desapegoUniversitario.domain.Notificacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificacaoRepository extends JpaRepository<Notificacao, UUID> {

    Page<Notificacao> findByDestinatarioIdOrderByCriadoEmDesc(UUID destinatarioId, Pageable pageable);

    long countByDestinatarioIdAndLidaEmIsNull(UUID destinatarioId);
}
