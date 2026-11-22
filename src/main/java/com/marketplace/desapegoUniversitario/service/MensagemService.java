package com.marketplace.desapegoUniversitario.service;

import com.marketplace.desapegoUniversitario.domain.Anuncio;
import com.marketplace.desapegoUniversitario.domain.Mensagem;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.dto.ConversaResponseDTO;
import com.marketplace.desapegoUniversitario.dto.MensagemRequestDTO;
import com.marketplace.desapegoUniversitario.dto.MensagemResponseDTO;
import com.marketplace.desapegoUniversitario.exception.ParametroInvalidoException;
import com.marketplace.desapegoUniversitario.exception.RecursoNaoEncontradoException;
import com.marketplace.desapegoUniversitario.repository.AnuncioRepository;
import com.marketplace.desapegoUniversitario.repository.MensagemRepository;
import com.marketplace.desapegoUniversitario.repository.ReservaRepository;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MensagemService {

    private final MensagemRepository mensagemRepository;
    private final AnuncioRepository anuncioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReservaRepository reservaRepository;
    private final NotificacaoService notificacaoService;

    public MensagemService(MensagemRepository mensagemRepository,
                           AnuncioRepository anuncioRepository,
                           UsuarioRepository usuarioRepository,
                           ReservaRepository reservaRepository,
                           NotificacaoService notificacaoService) {
        this.mensagemRepository = mensagemRepository;
        this.anuncioRepository = anuncioRepository;
        this.usuarioRepository = usuarioRepository;
        this.reservaRepository = reservaRepository;
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

        UUID compradorId = remetente.getId().equals(vendedor.getId())
                ? destinatario.getId() : remetente.getId();
        exigirCompradorDaReserva(anuncioId, compradorId);

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
            if (usuarioEhVendedor) {
                exigirCompradorDaReserva(anuncioId, compradorId);
            } else if (!compradorId.equals(usuario.getId())) {
                throw new AccessDeniedException("Você não participa desta conversa");
            }
            mensagens = mensagemRepository.findThread(anuncioId, usuario.getId(), compradorId);
        }
        return mensagens.stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ConversaResponseDTO> listarConversas(String emailUsuario) {
        Usuario usuario = buscarUsuario(emailUsuario);
        Map<String, ConversaAcumulada> conversas = new LinkedHashMap<>();

        for (Mensagem mensagem : mensagemRepository.findEnvolvendoUsuario(usuario.getId())) {
            boolean usuarioEhRemetente = mensagem.getRemetente().getId().equals(usuario.getId());
            Usuario outro = usuarioEhRemetente ? mensagem.getDestinatario() : mensagem.getRemetente();
            String chave = mensagem.getAnuncio().getId() + "|" + outro.getId();

            ConversaAcumulada conversa = conversas.computeIfAbsent(chave,
                    ignorado -> new ConversaAcumulada(mensagem.getAnuncio(), outro));
            conversa.ultimaMensagem = mensagem;
            if (!usuarioEhRemetente && mensagem.getLidaEm() == null) {
                conversa.naoLidas++;
            }
        }

        return conversas.values().stream()
                .map(conversa -> paraResponse(conversa, usuario))
                .sorted(Comparator.comparing(ConversaResponseDTO::ultimaMensagemEm,
                        Comparator.nullsLast(Comparator.<Instant>reverseOrder())))
                .toList();
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

    private void exigirCompradorDaReserva(UUID anuncioId, UUID compradorId) {
        if (!reservaRepository.existsByAnuncioIdAndCompradorIdAndCanceladaEmIsNull(
                anuncioId, compradorId)) {
            throw new AccessDeniedException(
                    "Somente o comprador e o vendedor deste anúncio podem trocar mensagens");
        }
    }

    private ConversaResponseDTO paraResponse(ConversaAcumulada conversa, Usuario usuario) {
        Anuncio anuncio = conversa.anuncio;
        UUID vendedorId = anuncio.getUsuario() == null ? null : anuncio.getUsuario().getId();
        UUID compradorId = vendedorId != null && vendedorId.equals(usuario.getId())
                ? conversa.outro.getId() : usuario.getId();

        return new ConversaResponseDTO(
                anuncio.getId(),
                anuncio.getTitulo(),
                anuncio.getStatus(),
                compradorId,
                vendedorId,
                conversa.outro.getId(),
                conversa.outro.getNome(),
                conversa.ultimaMensagem.getConteudo(),
                conversa.ultimaMensagem.getCriadoEm(),
                conversa.naoLidas);
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

    private static final class ConversaAcumulada {
        private final Anuncio anuncio;
        private final Usuario outro;
        private Mensagem ultimaMensagem;
        private long naoLidas;

        private ConversaAcumulada(Anuncio anuncio, Usuario outro) {
            this.anuncio = anuncio;
            this.outro = outro;
        }
    }
}
