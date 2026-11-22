package com.marketplace.desapegoUniversitario;

import com.marketplace.desapegoUniversitario.config.BootstrapAdminInicial;
import com.marketplace.desapegoUniversitario.domain.Role;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootstrapAdminInicialTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Test
    void devePromoverContaVerificadaNoBoot() {
        Usuario usuario = usuario(Role.USUARIO, true);
        when(usuarioRepository.findByEmail("admin@universidade.edu.br")).thenReturn(Optional.of(usuario));

        new BootstrapAdminInicial(usuarioRepository, " ADMIN@Universidade.edu.br ").run(null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals(Role.ADMIN, captor.getValue().getRole());
    }

    @Test
    void naoDevePromoverContaNaoVerificada() {
        Usuario usuario = usuario(Role.USUARIO, false);
        when(usuarioRepository.findByEmail("admin@universidade.edu.br")).thenReturn(Optional.of(usuario));

        new BootstrapAdminInicial(usuarioRepository, "admin@universidade.edu.br").run(null);

        verify(usuarioRepository, never()).save(any());
        assertEquals(Role.USUARIO, usuario.getRole());
    }

    @Test
    void naoDeveFazerNadaSemVariavel() {
        new BootstrapAdminInicial(usuarioRepository, "  ").run(null);

        verify(usuarioRepository, never()).findByEmail(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void naoDeveFazerNadaQuandoEmailNaoExiste() {
        when(usuarioRepository.findByEmail(any())).thenReturn(Optional.empty());

        new BootstrapAdminInicial(usuarioRepository, "admin@universidade.edu.br").run(null);

        verify(usuarioRepository, never()).save(any());
    }

    private static Usuario usuario(Role role, boolean verificado) {
        return new Usuario(null, "Admin", "admin@universidade.edu.br", "hash", role, verificado, null);
    }
}
