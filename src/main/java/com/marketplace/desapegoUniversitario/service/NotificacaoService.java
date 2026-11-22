package com.marketplace.desapegoUniversitario.service;

import com.marketplace.desapegoUniversitario.domain.DispositivoFCM;
import com.marketplace.desapegoUniversitario.domain.Notificacao;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.dto.NotificacaoResponseDTO;
import com.marketplace.desapegoUniversitario.dto.PaginadoDTO;
import com.marketplace.desapegoUniversitario.exception.ParametroInvalidoException;
import com.marketplace.desapegoUniversitario.exception.RecursoNaoEncontradoException;
import com.marketplace.desapegoUniversitario.repository.DispositivoFCMRepository;
import com.marketplace.desapegoUniversitario.repository.NotificacaoRepository;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class NotificacaoService {

    private static final int TAMANHO_MAXIMO_PAGINA = 100;

    private final NotificacaoRepository notificacaoRepository;
    private final DispositivoFCMRepository dispositivoRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificadorPush notificadorPush;

    public NotificacaoService(NotificacaoRepository notificacaoRepository,
                              DispositivoFCMRepository dispositivoRepository,
                              UsuarioRepository usuarioRepository,
                              NotificadorPush notificadorPush) {
        this.notificacaoRepository = notificacaoRepository;
        this.dispositivoRepository = dispositivoRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificadorPush = notificadorPush;
    }

    @Transactional
    public void notificar(UUID destinatarioId, String titulo, String mensagem,
                          String tipo, UUID referenciaId) {
        Usuario destinatario = usuarioRepository.findById(destinatarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
        notificacaoRepository.save(new Notificacao(
                null, destinatario, titulo, mensagem, tipo, referenciaId, null, Instant.now()));
        dispositivoRepository.findByUsuarioId(destinatarioId).stream()
                .map(DispositivoFCM::getToken)
                .forEach(token -> notificadorPush.enviar(token, titulo, mensagem));
    }

    @Transactional(readOnly = true)
    public PaginadoDTO<NotificacaoResponseDTO> listar(String emailUsuario, int page, int size) {
        if (page < 0 || size < 1 || size > TAMANHO_MAXIMO_PAGINA) {
            throw new ParametroInvalidoException(
                    "Use page >= 0 e size entre 1 e " + TAMANHO_MAXIMO_PAGINA);
        }
        Usuario usuario = buscarUsuario(emailUsuario);
        Page<Notificacao> pagina = notificacaoRepository
                .findByDestinatarioIdOrderByCriadoEmDesc(usuario.getId(), PageRequest.of(page, size));
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
        dispositivoRepository.findByToken(token)
                .filter(dispositivo -> dispositivo.getUsuario().getId().equals(usuario.getId()))
                .ifPresent(dispositivoRepository::delete);
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
