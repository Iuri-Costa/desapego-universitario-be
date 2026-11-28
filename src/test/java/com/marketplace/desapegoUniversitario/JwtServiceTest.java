package com.marketplace.desapegoUniversitario;

import com.marketplace.desapegoUniversitario.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SEGREDO_VALIDO = "segredo-de-teste-com-ao-menos-32-bytes";

    @Test
    void deveFalharNoStartupSeOSegredoEstiverAusenteOuCurto() {
        assertThrows(IllegalStateException.class,
                () -> new JwtService(null, Duration.ofMinutes(15), Duration.ofDays(7)));
        assertThrows(IllegalStateException.class,
                () -> new JwtService("curto", Duration.ofMinutes(15), Duration.ofDays(7)));
    }

    @Test
    void tokenDevePerderValidadeQuandoVersaoDoUsuarioMuda() {
        JwtService service = new JwtService(SEGREDO_VALIDO, Duration.ofMinutes(15), Duration.ofDays(7));
        String email = "aluno@universidade.edu.br";

        Claims claims = service.validar(service.gerarAccessToken(email, 3));
        assertEquals(3, claims.get(JwtService.CLAIM_VERSAO, Integer.class));
        assertTrue(service.versaoValida(claims, 3));
        assertFalse(service.versaoValida(claims, 4));
        assertTrue(service.versaoValida(service.validar(service.gerarAccessToken(email)), 0));
    }

    @Test
    void deveGerarEValidarTokenComClaimDeTipo() {
        JwtService service = new JwtService(SEGREDO_VALIDO, Duration.ofMinutes(15), Duration.ofDays(7));

        String token = service.gerarAccessToken("aluno@universidade.edu.br");
        Claims claims = service.validar(token);

        assertEquals("aluno@universidade.edu.br", claims.getSubject());
        assertTrue(service.isAccessToken(claims));
        assertFalse(service.isRefreshToken(claims));
        assertThrows(JwtException.class, () -> service.validar(token + "x"));
    }
}
