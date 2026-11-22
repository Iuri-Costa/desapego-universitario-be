package com.marketplace.desapegoUniversitario.service;

import com.marketplace.desapegoUniversitario.domain.DispositivoFCM;
import com.marketplace.desapegoUniversitario.domain.Notificacao;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.dto.NotificacaoResponseDTO;
import com.marketplace.desapegoUniversitario.dto.PaginadoDTO;
import com.marketplace.desapegoUniversitario.exception.RecursoNaoEncontradoException;
import com.marketplace.desapegoUniversitario.repository.DispositivoFCMRepository;
import com.marketplace.desapegoUniversitario.repository.NotificacaoRepository;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import com.marketplace.desapegoUniversitario.util.Paginas;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final DispositivoFCMRepository dispositivoRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificadorPush notificadorPush;
    private final Duration deviceTtl;
    private final int cleanupLote;

    public NotificacaoService(NotificacaoRepository notificacaoRepository,
                              DispositivoFCMRepository dispositivoRepository,
                              UsuarioRepository usuarioRepository,
                              NotificadorPush notificadorPush,
                              @Value("${app.fcm.device-ttl:P90D}") Duration deviceTtl,
                              @Value("${app.fcm.cleanup-lote:500}") int cleanupLote) {
        this.notificacaoRepository = notificacaoRepository;
        this.dispositivoRepository = dispositivoRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificadorPush = notificadorPush;
        this.deviceTtl = deviceTtl;
        this.cleanupLote = cleanupLote;
    }

    @Transactional
    public void notificar(UUID destinatarioId, String titulo, String mensagem,
                          String tipo, UUID referenciaId) {
        Usuario destinatario = usuarioRepository.findById(destinatarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
        notificacaoRepository.save(new Notificacao(
                null, destinatario, titulo, mensagem, tipo, referenciaId, null, Instant.now()));
        limparDispositivosAntigos();
        for (DispositivoFCM dispositivo : dispositivoRepository.findByUsuarioId(destinatarioId)) {
            if (!notificadorPush.enviar(dispositivo.getToken(), titulo, mensagem)) {
                dispositivoRepository.delete(dispositivo);
            }
        }
    }

    @Transactional(readOnly = true)
    public PaginadoDTO<NotificacaoResponseDTO> listar(String emailUsuario, int page, int size) {
        Usuario usuario = buscarUsuario(emailUsuario);
        Page<Notificacao> pagina = notificacaoRepository
                .findByDestinatarioIdOrderByCriadoEmDesc(usuario.getId(), Paginas.de(page, size));
        return PaginadoDTO.de(pagina, this::paraResponse);
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas(String emailUsuario) {
        return notificacaoRepository.countByDestinatarioIdAndLidaEmIsNull(
                buscarUsuario(emailUsuario).getId());
    }

    @Transactional
    public NotificacaoResponseDTO marcarComoLida(UUID id, String emailUsuario) {
        Usuario usuario = buscarUsuario(emailUsuario);
        Notificacao notificacao = notificacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notificação não encontrada"));
        if (!notificacao.getDestinatario().getId().equals(usuario.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Esta notificação não pertence ao usuário");
        }
        if (notificacao.getLidaEm() == null) {
            notificacao.setLidaEm(Instant.now());
            notificacaoRepository.save(notificacao);
        }
        return paraResponse(notificacao);
    }

    @Transactional
    public void registrarDispositivo(String token, String emailUsuario) {
        Usuario usuario = buscarUsuario(emailUsuario);
        limparDispositivosAntigos();
        String tokenNormalizado = token.trim();
        DispositivoFCM dispositivo = dispositivoRepository.findByToken(tokenNormalizado)
                .orElseGet(() -> new DispositivoFCM(
                        null, usuario, tokenNormalizado, Instant.now()));
        dispositivo.setUsuario(usuario);
        dispositivoRepository.save(dispositivo);
    }

    @Transactional
    public void removerDispositivo(String token, String emailUsuario) {
        Usuario usuario = buscarUsuario(emailUsuario);
        dispositivoRepository.findByToken(token.trim())
                .filter(dispositivo -> dispositivo.getUsuario().getId().equals(usuario.getId()))
                .ifPresent(dispositivoRepository::delete);
    }

    private void limparDispositivosAntigos() {
        Instant limite = Instant.now().minus(deviceTtl);
        dispositivoRepository.findByCriadoEmBefore(limite, PageRequest.of(0, cleanupLote))
                .forEach(dispositivoRepository::delete);
    }

    private NotificacaoResponseDTO paraResponse(Notificacao notificacao) {
        return new NotificacaoResponseDTO(
                notificacao.getId(),
                notificacao.getTitulo(),
                notificacao.getMensagem(),
                notificacao.getTipo(),
                notificacao.getReferenciaId(),
                notificacao.getCriadoEm(),
                notificacao.getLidaEm()
        );
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }
}
