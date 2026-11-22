package com.marketplace.desapegoUniversitario.repository;

import com.marketplace.desapegoUniversitario.domain.TipoToken;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.domain.UsuarioToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioTokenRepository extends JpaRepository<UsuarioToken, UUID> {

    Optional<UsuarioToken> findByTokenHashAndTipoAndUsadoEmIsNull(String tokenHash, TipoToken tipo);

    List<UsuarioToken> findByUsuarioAndTipoAndUsadoEmIsNull(Usuario usuario, TipoToken tipo);

    void deleteByUsuario(Usuario usuario);
}
