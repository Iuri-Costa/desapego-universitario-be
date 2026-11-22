package com.marketplace.desapegoUniversitario.repository;

import com.marketplace.desapegoUniversitario.domain.Mensagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MensagemRepository extends JpaRepository<Mensagem, UUID> {

    @Query("""
            select m from Mensagem m
            where m.anuncio.id = :anuncioId
              and ((m.remetente.id = :usuarioId and m.destinatario.id = :outroId)
                or (m.remetente.id = :outroId and m.destinatario.id = :usuarioId))
            order by m.criadoEm asc
            """)
    List<Mensagem> findThread(@Param("anuncioId") UUID anuncioId,
                              @Param("usuarioId") UUID usuarioId,
                              @Param("outroId") UUID outroId);

    @Query("""
            select m from Mensagem m
            where m.anuncio.id = :anuncioId
              and (m.remetente.id = :usuarioId or m.destinatario.id = :usuarioId)
            order by m.criadoEm asc
            """)
    List<Mensagem> findEnvolvendo(@Param("anuncioId") UUID anuncioId,
                                  @Param("usuarioId") UUID usuarioId);

    @Query("""
            select m from Mensagem m
            where m.remetente.id = :usuarioId or m.destinatario.id = :usuarioId
            order by m.criadoEm asc
            """)
    List<Mensagem> findEnvolvendoUsuario(@Param("usuarioId") UUID usuarioId);
}
