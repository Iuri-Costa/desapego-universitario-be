package com.marketplace.desapegoUniversitario.repository;

import com.marketplace.desapegoUniversitario.domain.RefreshToken;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshToken t where t.tokenHash = :hash")
    Optional<RefreshToken> findByHashForUpdate(@Param("hash") String hash);

    Optional<RefreshToken> findByTokenHash(String hash);

    List<RefreshToken> findByUsuarioAndRevogadoEmIsNull(Usuario usuario);

    void deleteByUsuario(Usuario usuario);
}
