package com.marketplace.desapegoUniversitario.repository;

import com.marketplace.desapegoUniversitario.domain.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, UUID> {

    List<Avaliacao> findByVendedorIdOrderByCriadoEmDesc(UUID vendedorId);

    Optional<Avaliacao> findByAnuncioIdAndAvaliadorId(UUID anuncioId, UUID avaliadorId);

    @Query("select coalesce(avg(a.nota), 0) from Avaliacao a where a.vendedor.id = :vendedorId")
    double mediaPorVendedor(@Param("vendedorId") UUID vendedorId);

    long countByVendedorId(UUID vendedorId);
}
