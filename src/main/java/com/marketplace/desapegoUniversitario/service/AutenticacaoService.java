package com.marketplace.desapegoUniversitario.service;

import com.marketplace.desapegoUniversitario.domain.RefreshToken;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.dto.LoginDTO;
import com.marketplace.desapegoUniversitario.dto.TokenResponseDTO;
import com.marketplace.desapegoUniversitario.exception.CredenciaisInvalidasException;
import com.marketplace.desapegoUniversitario.exception.TokenInvalidoException;
import com.marketplace.desapegoUniversitario.repository.RefreshTokenRepository;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import com.marketplace.desapegoUniversitario.security.JwtService;
import com.marketplace.desapegoUniversitario.security.LimitadorRequisicoes;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AutenticacaoService {

    private static final String MENSAGEM_TOKEN_INVALIDO = "Refresh token inválido ou expirado";

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final LimitadorRequisicoes limitador;

    public AutenticacaoService(AuthenticationManager authenticationManager,
                               UsuarioRepository usuarioRepository,
                               RefreshTokenRepository refreshTokenRepository,
                               JwtService jwtService,
                               LimitadorRequisicoes limitador) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.limitador = limitador;
    }

    @Transactional
    public TokenResponseDTO login(LoginDTO dto) {
        String email = UsuarioService.normalizar(dto.email());
        limitador.verificarEmail(LimitadorRequisicoes.Tipo.LOGIN, email);
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, dto.senha()));
        } catch (AuthenticationException e) {
            throw new CredenciaisInvalidasException("E-mail ou senha inválidos");
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
                .filter(Usuario::isEmailVerificado)
                .orElseThrow(() -> new CredenciaisInvalidasException("E-mail ou senha inválidos"));
        return emitirTokens(usuario);
    }

    @Transactional(noRollbackFor = TokenInvalidoException.class)
    public TokenResponseDTO renovar(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.validar(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new TokenInvalidoException(MENSAGEM_TOKEN_INVALIDO);
        }
        if (!jwtService.isRefreshToken(claims)) {
            throw new TokenInvalidoException(MENSAGEM_TOKEN_INVALIDO);
        }

        String hashAtual = jwtService.hash(refreshToken);
        RefreshToken atual = refreshTokenRepository.findByHashForUpdate(hashAtual)
                .orElseThrow(() -> new TokenInvalidoException(MENSAGEM_TOKEN_INVALIDO));

        if (atual.getRevogadoEm() != null) {
            revogarTodos(atual.getUsuario());
            throw new TokenInvalidoException(MENSAGEM_TOKEN_INVALIDO);
        }
        if (atual.getExpiraEm().isBefore(Instant.now())) {
            throw new TokenInvalidoException(MENSAGEM_TOKEN_INVALIDO);
        }

        Usuario usuario = atual.getUsuario();
        String novoRefreshToken = jwtService.gerarRefreshToken(usuario.getEmail());
        Instant agora = Instant.now();
        atual.setRevogadoEm(agora);
        atual.setSubstituidoPor(jwtService.hash(novoRefreshToken));

        RefreshToken novo = new RefreshToken(
                UUID.randomUUID(), usuario, jwtService.hash(novoRefreshToken),
                agora.plus(jwtService.getRefreshTtl()), null, null, agora);
        refreshTokenRepository.save(atual);
        refreshTokenRepository.save(novo);

        return new TokenResponseDTO(
                jwtService.gerarAccessToken(usuario.getEmail()), novoRefreshToken, "Bearer",
                jwtService.getAccessTtl().toSeconds());
    }

    @Transactional
    public void logout(String email) {
        usuarioRepository.findByEmail(email).ifPresent(this::revogarTodos);
    }

    private TokenResponseDTO emitirTokens(Usuario usuario) {
        Instant agora = Instant.now();
        String refreshToken = jwtService.gerarRefreshToken(usuario.getEmail());
        refreshTokenRepository.save(new RefreshToken(
                UUID.randomUUID(), usuario, jwtService.hash(refreshToken),
                agora.plus(jwtService.getRefreshTtl()), null, null, agora));

        return new TokenResponseDTO(
                jwtService.gerarAccessToken(usuario.getEmail()), refreshToken, "Bearer",
                jwtService.getAccessTtl().toSeconds());
    }

    private void revogarTodos(Usuario usuario) {
        Instant agora = Instant.now();
        List<RefreshToken> tokens = refreshTokenRepository.findByUsuarioAndRevogadoEmIsNull(usuario);
        tokens.forEach(token -> token.setRevogadoEm(agora));
        refreshTokenRepository.saveAll(tokens);
    }
}
