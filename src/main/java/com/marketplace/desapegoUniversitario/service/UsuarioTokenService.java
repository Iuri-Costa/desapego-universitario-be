package com.marketplace.desapegoUniversitario.service;

import com.marketplace.desapegoUniversitario.domain.TipoToken;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.domain.UsuarioToken;
import com.marketplace.desapegoUniversitario.exception.TokenInvalidoException;
import com.marketplace.desapegoUniversitario.repository.UsuarioTokenRepository;
import com.marketplace.desapegoUniversitario.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class UsuarioTokenService {

    private final UsuarioTokenRepository usuarioTokenRepository;
    private final JwtService jwtService;
    private final Duration verificacaoTtl;
    private final Duration redefinicaoTtl;

    public UsuarioTokenService(UsuarioTokenRepository usuarioTokenRepository,
                               JwtService jwtService,
                               @Value("${app.token.verificacao-ttl}") Duration verificacaoTtl,
                               @Value("${app.token.redefinicao-ttl}") Duration redefinicaoTtl) {
        this.usuarioTokenRepository = usuarioTokenRepository;
        this.jwtService = jwtService;
        this.verificacaoTtl = verificacaoTtl;
        this.redefinicaoTtl = redefinicaoTtl;
    }

    @Transactional
    public String criar(Usuario usuario, TipoToken tipo) {
        revogarPendentes(usuario, tipo);
        String token = UUID.randomUUID() + UUID.randomUUID().toString();
        Instant agora = Instant.now();
        Duration ttl = tipo == TipoToken.VERIFICACAO_EMAIL ? verificacaoTtl : redefinicaoTtl;
        usuarioTokenRepository.save(new UsuarioToken(
                UUID.randomUUID(), usuario, tipo, jwtService.hash(token),
                agora.plus(ttl), null, agora));
        return token;
    }

    @Transactional
    public Usuario consumir(String token, TipoToken tipo) {
        UsuarioToken registro = usuarioTokenRepository
                .findByTokenHashAndTipoAndUsadoEmIsNull(jwtService.hash(token), tipo)
                .orElseThrow(() -> new TokenInvalidoException("Token inválido ou expirado"));
        if (registro.getExpiraEm().isBefore(Instant.now())) {
            throw new TokenInvalidoException("Token inválido ou expirado");
        }
        registro.setUsadoEm(Instant.now());
        usuarioTokenRepository.save(registro);
        return registro.getUsuario();
    }

    @Transactional
    public void revogarPendentes(Usuario usuario, TipoToken tipo) {
        Instant agora = Instant.now();
        List<UsuarioToken> pendentes =
                usuarioTokenRepository.findByUsuarioAndTipoAndUsadoEmIsNull(usuario, tipo);
        pendentes.forEach(token -> token.setUsadoEm(agora));
        usuarioTokenRepository.saveAll(pendentes);
    }
}
