package com.marketplace.desapegoUniversitario.service;

import com.marketplace.desapegoUniversitario.domain.Anuncio;
import com.marketplace.desapegoUniversitario.domain.Mensagem;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.dto.MensagemRequestDTO;
import com.marketplace.desapegoUniversitario.dto.MensagemResponseDTO;
import com.marketplace.desapegoUniversitario.exception.ParametroInvalidoException;
import com.marketplace.desapegoUniversitario.exception.RecursoNaoEncontradoException;
import com.marketplace.desapegoUniversitario.repository.AnuncioRepository;
import com.marketplace.desapegoUniversitario.repository.MensagemRepository;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MensagemService {

    private final MensagemRepository mensagemRepository;
    private final AnuncioRepository anuncioRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacaoService notificacaoService;

    public MensagemService(MensagemRepository mensagemRepository,
                           AnuncioRepository anuncioRepository,
                           UsuarioRepository usuarioRepository,
                           NotificacaoService notificacaoService) {
        this.mensagemRepository = mensagemRepository;
        this.anuncioRepository = anuncioRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacaoService = notificacaoService;
    }

    @Transactional
    public MensagemResponseDTO enviar(UUID anuncioId, MensagemRequestDTO dto, String emailUsuario) {
        Anuncio anuncio = buscarAnuncio(anuncioId);
        Usuario remetente = buscarUsuario(emailUsuario);
        Usuario vendedor = anuncio.getUsuario();
        if (vendedor == null) {
            throw new ParametroInvalidoException("Este anúncio não possui vendedor");
        }

        Usuario destinatario;
        if (vendedor.getId().equals(remetente.getId())) {
            if (dto.destinatarioId() == null) {
                throw new ParametroInvalidoException("Informe o destinatário da mensagem");
            }
            destinatario = buscarUsuarioPorId(dto.destinatarioId());
        } else {
            if (dto.destinatarioId() != null && !dto.destinatarioId().equals(vendedor.getId())) {
                throw new ParametroInvalidoException("Em anúncios de terceiros, a mensagem vai para o vendedor");
            }
            destinatario = vendedor;
        }
        if (destinatario.getId().equals(remetente.getId())) {
            throw new ParametroInvalidoException("Não é possível enviar uma mensagem para si mesmo");
        }

        Mensagem mensagem = mensagemRepository.save(new Mensagem(
                null, anuncio, remetente, destinatario, dto.conteudo().trim(), Instant.now(), null));
        notificacaoService.notificar(destinatario.getId(), "Nova mensagem",
                "Você recebeu uma mensagem sobre o anúncio '" + anuncio.getTitulo() + "'",
                "MENSAGEM", anuncioId);
        return paraResponse(mensagem);
    }

    @Transactional(readOnly = true)
    public List<MensagemResponseDTO> listar(UUID anuncioId, UUID compradorId, String emailUsuario) {
        Anuncio anuncio = buscarAnuncio(anuncioId);
        Usuario usuario = buscarUsuario(emailUsuario);
        UUID vendedorId = anuncio.getUsuario() == null ? null : anuncio.getUsuario().getId();

        List<Mensagem> mensagens;
        if (compradorId == null) {
            mensagens = mensagemRepository.findEnvolvendo(anuncioId, usuario.getId());
        } else {
            boolean usuarioEhVendedor = usuario.getId().equals(vendedorId);
            if (!usuarioEhVendedor
                    && !compradorId.equals(usuario.getId())
                    && !compradorId.equals(vendedorId)) {
                throw new AccessDeniedException("Você não participa desta conversa");
            }
            mensagens = mensagemRepository.findThread(anuncioId, usuario.getId(), compradorId);
        }
        return mensagens.stream().map(this::paraResponse).toList();
    }

    @Transactional
    public MensagemResponseDTO marcarComoLida(UUID mensagemId, String emailUsuario) {
        Mensagem mensagem = mensagemRepository.findById(mensagemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Mensagem não encontrada"));
        Usuario usuario = buscarUsuario(emailUsuario);
        if (!mensagem.getDestinatario().getId().equals(usuario.getId())) {
            throw new AccessDeniedException("Somente o destinatário pode marcar a mensagem como lida");
        }
        if (mensagem.getLidaEm() == null) {
            mensagem.setLidaEm(Instant.now());
            mensagemRepository.save(mensagem);
        }
        return paraResponse(mensagem);
    }

    private MensagemResponseDTO paraResponse(Mensagem mensagem) {
        return new MensagemResponseDTO(
                mensagem.getId(),
                mensagem.getAnuncio().getId(),
                mensagem.getRemetente().getId(),
                mensagem.getRemetente().getNome(),
                mensagem.getDestinatario().getId(),
                mensagem.getConteudo(),
                mensagem.getCriadoEm(),
                mensagem.getLidaEm()
        );
    }

    private Anuncio buscarAnuncio(UUID id) {
        return anuncioRepository.findByIdAndRemovidoEmIsNull(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Anúncio não encontrado"));
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }

    private Usuario buscarUsuarioPorId(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }
}
