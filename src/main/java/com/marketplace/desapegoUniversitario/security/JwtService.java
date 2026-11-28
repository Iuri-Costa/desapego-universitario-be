package com.marketplace.desapegoUniversitario.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class JwtService {

    public static final String TIPO_ACCESS = "access";
    public static final String TIPO_REFRESH = "refresh";
    public static final String CLAIM_VERSAO = "ver";

    private final SecretKey chave;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtService(@Value("${app.jwt.secret}") String segredo,
                      @Value("${app.jwt.access-token-ttl}") Duration accessTtl,
                      @Value("${app.jwt.refresh-token-ttl}") Duration refreshTtl) {
        byte[] bytes = segredo == null ? new byte[0] : segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET deve ter pelo menos 32 bytes (HS256)");
        }
        this.chave = Keys.hmacShaKeyFor(bytes);
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    public String gerarAccessToken(String email) {
        return gerarAccessToken(email, 0);
    }

    public String gerarAccessToken(String email, int versao) {
        return gerar(email, TIPO_ACCESS, accessTtl, versao);
    }

    public String gerarRefreshToken(String email) {
        return gerarRefreshToken(email, 0);
    }

    public String gerarRefreshToken(String email, int versao) {
        return gerar(email, TIPO_REFRESH, refreshTtl, versao);
    }

    public Duration getRefreshTtl() {
        return refreshTtl;
    }

    public Duration getAccessTtl() {
        return accessTtl;
    }

    public Claims validar(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isAccessToken(Claims claims) {
        return TIPO_ACCESS.equals(claims.get("tipo", String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return TIPO_REFRESH.equals(claims.get("tipo", String.class));
    }

    /** Tokens sem a claim (emitidos antes da V21) pertencem a versao 0. */
    public boolean versaoValida(Claims claims, int versaoAtual) {
        Integer versaoToken = claims.get(CLAIM_VERSAO, Integer.class);
        return (versaoToken == null ? 0 : versaoToken) == versaoAtual;
    }

    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível", e);
        }
    }

    private String gerar(String email, String tipo, Duration ttl, int versao) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(email)
                .id(UUID.randomUUID().toString())
                .claim("tipo", tipo)
                .claim(CLAIM_VERSAO, versao)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(ttl)))
                .signWith(chave)
                .compact();
    }
}
