package com.marketplace.desapegoUniversitario;

import com.marketplace.desapegoUniversitario.domain.DispositivoFCM;
import com.marketplace.desapegoUniversitario.domain.Role;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.repository.DispositivoFCMRepository;
import com.marketplace.desapegoUniversitario.repository.NotificacaoRepository;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import com.marketplace.desapegoUniversitario.service.NotificacaoService;
import com.marketplace.desapegoUniversitario.service.NotificadorPush;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacaoServiceTest {

    private static final String EMAIL = "aluno@universidade.edu.br";

    private static final PlatformTransactionManager TRANSACAO_SIMULADA = new PlatformTransactionManager() {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    };

    @Mock
    private NotificacaoRepository notificacaoRepository;
    @Mock
    private DispositivoFCMRepository dispositivoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private NotificadorPush notificadorPush;

    private NotificacaoService service;
    private Usuario destinatario;
    private UUID destinatarioId;

    @BeforeEach
    void setUp() {
        service = new NotificacaoService(notificacaoRepository, dispositivoRepository,
                usuarioRepository, notificadorPush, TRANSACAO_SIMULADA, Duration.ofDays(90), 500);
        destinatarioId = UUID.randomUUID();
        destinatario = new Usuario(destinatarioId, "Aluno", EMAIL, "hash",
                Role.USUARIO, true, Instant.now());
    }

    private DispositivoFCM dispositivo(String token, Instant criadoEm) {
        return new DispositivoFCM(UUID.randomUUID(), destinatario, token, criadoEm);
    }

    @Test
    void deveSalvarNotificacaoEEnviarPushParaCadaDispositivo() {
        DispositivoFCM primeiro = dispositivo("token-1", Instant.now());
        DispositivoFCM segundo = dispositivo("token-2", Instant.now());
        when(usuarioRepository.findById(destinatarioId)).thenReturn(Optional.of(destinatario));
        when(dispositivoRepository.findByUsuarioId(destinatarioId)).thenReturn(List.of(primeiro, segundo));
        when(notificadorPush.enviar(anyString(), anyString(), anyString())).thenReturn(true);

        service.notificar(destinatarioId, "Titulo", "Mensagem", "MENSAGEM", null);

        verify(notificacaoRepository).save(any());
        verify(notificadorPush).enviar("token-1", "Titulo", "Mensagem");
        verify(notificadorPush).enviar("token-2", "Titulo", "Mensagem");
        verify(dispositivoRepository, never()).delete(any());
    }

    @Test
    void deveRemoverDispositivoComTokenInvalidoSemPerderNotificacao() {
        DispositivoFCM invalido = dispositivo("token-invalido", Instant.now());
        DispositivoFCM valido = dispositivo("token-valido", Instant.now());
        when(usuarioRepository.findById(destinatarioId)).thenReturn(Optional.of(destinatario));
        when(dispositivoRepository.findByUsuarioId(destinatarioId))
                .thenReturn(List.of(invalido, valido));
        when(notificadorPush.enviar("token-invalido", "Titulo", "Mensagem")).thenReturn(false);
        when(notificadorPush.enviar("token-valido", "Titulo", "Mensagem")).thenReturn(true);

        service.notificar(destinatarioId, "Titulo", "Mensagem", "MENSAGEM", null);

        verify(notificacaoRepository).save(any());
        verify(dispositivoRepository).delete(invalido);
        verify(dispositivoRepository, never()).delete(valido);
    }

    @Test
    void pushDeveSerEnviadoSomenteDepoisDoCommit() {
        DispositivoFCM dispositivo = dispositivo("token-1", Instant.now());
        when(usuarioRepository.findById(destinatarioId)).thenReturn(Optional.of(destinatario));
        when(dispositivoRepository.findByUsuarioId(destinatarioId)).thenReturn(List.of(dispositivo));
        when(notificadorPush.enviar("token-1", "Titulo", "Mensagem")).thenReturn(true);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.notificar(destinatarioId, "Titulo", "Mensagem", "MENSAGEM", null);

            verify(notificadorPush, never()).enviar(anyString(), anyString(), anyString());
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            verify(notificadorPush).enviar("token-1", "Titulo", "Mensagem");
            verify(notificacaoRepository).save(any());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void falhaNoPushNaoDevePropagarParaAOperacaoQueNotificou() {
        when(usuarioRepository.findById(destinatarioId)).thenReturn(Optional.of(destinatario));
        when(dispositivoRepository.findByUsuarioId(destinatarioId))
                .thenReturn(List.of(dispositivo("token-1", Instant.now())));
        when(notificadorPush.enviar(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("FCM indisponivel"));

        assertDoesNotThrow(() -> service.notificar(destinatarioId, "Titulo", "Mensagem", "MENSAGEM", null));
        verify(notificacaoRepository).save(any());
    }

    @Test
    void semDispositivosNaoEnviaPush() {
        when(usuarioRepository.findById(destinatarioId)).thenReturn(Optional.of(destinatario));
        when(dispositivoRepository.findByUsuarioId(destinatarioId)).thenReturn(List.of());

        service.notificar(destinatarioId, "Titulo", "Mensagem", "MENSAGEM", null);

        verify(notificacaoRepository).save(any());
        verify(notificadorPush, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void deveRemoverDispositivosAntigosEmLoteLimitado() {
        DispositivoFCM antigo = dispositivo("token-antigo", Instant.now().minus(Duration.ofDays(120)));
        when(usuarioRepository.findById(destinatarioId)).thenReturn(Optional.of(destinatario));
        when(dispositivoRepository.findByCriadoEmBefore(any(Instant.class), any()))
                .thenReturn(List.of(antigo));
        when(dispositivoRepository.findByUsuarioId(destinatarioId)).thenReturn(List.of());

        service.notificar(destinatarioId, "Titulo", "Mensagem", "MENSAGEM", null);

        verify(dispositivoRepository).delete(antigo);
    }

    @Test
    void registrarDispositivoDeveNormalizarTokenELimparAntigos() {
        DispositivoFCM antigo = dispositivo("token-antigo", Instant.now().minus(Duration.ofDays(120)));
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(destinatario));
        when(dispositivoRepository.findByCriadoEmBefore(any(Instant.class), any()))
                .thenReturn(List.of(antigo));
        when(dispositivoRepository.findByToken("token-novo")).thenReturn(Optional.empty());

        service.registrarDispositivo("  token-novo  ", EMAIL);

        verify(dispositivoRepository).delete(antigo);
        verify(dispositivoRepository).save(any());
    }

    @Test
    void removerDispositivoDeveNormalizarToken() {
        DispositivoFCM dispositivo = dispositivo("token-fcm", Instant.now());
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(destinatario));
        when(dispositivoRepository.findByToken("token-fcm")).thenReturn(Optional.of(dispositivo));

        service.removerDispositivo("  token-fcm  ", EMAIL);

        verify(dispositivoRepository).delete(dispositivo);
    }
}
