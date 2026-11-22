package com.marketplace.desapegoUniversitario;

import com.marketplace.desapegoUniversitario.controller.AdminController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class MetodosAdministrativosIntegrationTest {

    @Autowired
    private AdminController adminController;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void metodoAdministrativoDeveNegarUsuarioComumMesmoSemFiltroWeb() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "aluno@universidade.edu.br", "senha",
                List.of(new SimpleGrantedAuthority("ROLE_USUARIO"))));

        assertThrows(AccessDeniedException.class, () -> adminController.listarDenuncias(null, 0, 20));
    }
}
