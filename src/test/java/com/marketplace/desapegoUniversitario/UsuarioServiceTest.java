package com.marketplace.desapegoUniversitario;

import com.marketplace.desapegoUniversitario.config.DominiosInstitucionais;
import com.marketplace.desapegoUniversitario.domain.Role;
import com.marketplace.desapegoUniversitario.domain.TipoToken;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.exception.RecursoNaoEncontradoException;
import com.marketplace.desapegoUniversitario.repository.RefreshTokenRepository;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import com.marketplace.desapegoUniversitario.service.EmailSender;
import com.marketplace.desapegoUniversitario.service.UsuarioService;
import com.marketplace.desapegoUniversitario.service.UsuarioTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailSender emailSender;
    @Mock
    private UsuarioTokenService usuarioTokenService;

    private UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(usuarioRepository, refreshTokenRepository, passwordEncoder,
                emailSender, new DominiosInstitucionais("edu.br"), usuarioTokenService,
                "https://app.example.com");
    }

    private Usuario usuario(String email, boolean verificado) {
        return new Usuario(UUID.randomUUID(), "Aluno", email, "hash", Role.USUARIO,
                verificado, Instant.now());
    }

    @Test
    void deveListarTodosOsUsuarios() {
        Usuario usuario = usuario("aluno@universidade.edu.br", true);
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario));

        var respostas = service.listarTodos();

        assertEquals(1, respostas.size());
        assertEquals(usuario.getEmail(), respostas.get(0).email());
        assertEquals("USUARIO", respostas.get(0).role());
    }

    @Test
    void deveDeletarUsuarioExistente() {
        UUID id = UUID.randomUUID();
        when(usuarioRepository.existsById(id)).thenReturn(true);

        service.deletarPorID(id);

        verify(usuarioRepository).deleteById(id);
    }

    @Test
    void deveRejeitarExclusaoDeUsuarioInexistente() {
        UUID id = UUID.randomUUID();
        when(usuarioRepository.existsById(id)).thenReturn(false);

        assertThrows(RecursoNaoEncontradoException.class, () -> service.deletarPorID(id));
        verify(usuarioRepository, never()).deleteById(any());
    }

    @Test
    void deveReenviarVerificacaoApenasParaUsuarioNaoVerificado() {
        Usuario naoVerificado = usuario("aluno@universidade.edu.br", false);
        when(usuarioRepository.findByEmail(naoVerificado.getEmail()))
                .thenReturn(Optional.of(naoVerificado));
        when(usuarioTokenService.criar(naoVerificado, TipoToken.VERIFICACAO_EMAIL)).thenReturn("token");

        service.reenviarVerificacao(naoVerificado.getEmail());

        verify(emailSender).enviar(eq(naoVerificado.getEmail()), eq("Verificação de e-mail"),
                any());
    }

    @Test
    void naoDeveReenviarVerificacaoParaUsuarioJaVerificado() {
        Usuario verificado = usuario("aluno@universidade.edu.br", true);
        when(usuarioRepository.findByEmail(verificado.getEmail())).thenReturn(Optional.of(verificado));

        service.reenviarVerificacao(verificado.getEmail());

        verify(usuarioTokenService, never()).criar(any(), any());
        verify(emailSender, never()).enviar(any(), any(), any());
    }

    @Test
    void deveEnviarLinkDeRecuperacaoSemRevelarInexistencia() {
        Usuario usuario = usuario("aluno@universidade.edu.br", true);
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(usuarioTokenService.criar(usuario, TipoToken.REDEFINICAO_SENHA)).thenReturn("token-recuperacao");

        service.recuperarSenha(usuario.getEmail());

        verify(emailSender).enviar(eq(usuario.getEmail()), eq("Recuperação de senha"), any());
    }

    @Test
    void deveNormalizarEmail() {
        assertEquals("aluno@universidade.edu.br",
                UsuarioService.normalizar("  Aluno@Universidade.edu.br "));
        assertTrue(UsuarioService.normalizar(null) == null);
    }
}
