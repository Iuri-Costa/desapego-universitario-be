package com.marketplace.desapegoUniversitario.service;

import com.marketplace.desapegoUniversitario.domain.Anuncio;
import com.marketplace.desapegoUniversitario.domain.Avaliacao;
import com.marketplace.desapegoUniversitario.domain.Reserva;
import com.marketplace.desapegoUniversitario.domain.StatusAnuncio;
import com.marketplace.desapegoUniversitario.domain.TipoAnuncio;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.dto.AnuncioResponseDTO;
import com.marketplace.desapegoUniversitario.dto.AvaliacaoRequestDTO;
import com.marketplace.desapegoUniversitario.dto.AvaliacaoResponseDTO;
import com.marketplace.desapegoUniversitario.dto.AvaliacoesResponseDTO;
import com.marketplace.desapegoUniversitario.exception.ConflitoException;
import com.marketplace.desapegoUniversitario.exception.ParametroInvalidoException;
import com.marketplace.desapegoUniversitario.exception.RecursoNaoEncontradoException;
import com.marketplace.desapegoUniversitario.repository.AnuncioRepository;
import com.marketplace.desapegoUniversitario.repository.AvaliacaoRepository;
import com.marketplace.desapegoUniversitario.repository.ReservaRepository;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NegociacaoService {

    private final ReservaRepository reservaRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final AnuncioRepository anuncioRepository;
    private final UsuarioRepository usuarioRepository;
    private final AnuncioService anuncioService;
    private final NotificacaoService notificacaoService;

    public NegociacaoService(ReservaRepository reservaRepository,
                             AvaliacaoRepository avaliacaoRepository,
                             AnuncioRepository anuncioRepository,
                             UsuarioRepository usuarioRepository,
                             AnuncioService anuncioService,
                             NotificacaoService notificacaoService) {
        this.reservaRepository = reservaRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.anuncioRepository = anuncioRepository;
        this.usuarioRepository = usuarioRepository;
        this.anuncioService = anuncioService;
        this.notificacaoService = notificacaoService;
    }

    @Transactional
    public AnuncioResponseDTO reservar(UUID anuncioId, String emailUsuario) {
        Anuncio anuncio = buscarAnuncio(anuncioId);
        Usuario comprador = buscarUsuario(emailUsuario);
        Usuario vendedor = exigirVendedor(anuncio);

        if (vendedor.getId().equals(comprador.getId())) {
            throw new ParametroInvalidoException("O vendedor não pode reservar o próprio anúncio");
        }
        var existente = reservaRepository
                .findByAnuncioIdAndCanceladaEmIsNullAndConcluidaEmIsNull(anuncioId);
        if (existente.isPresent()) {
            if (existente.get().getComprador().getId().equals(comprador.getId())) {
                return anuncioService.paraResponse(anuncio);
            }
            throw new ConflitoException("Este anúncio já está reservado");
        }
        if (anuncio.getStatus() != StatusAnuncio.ATIVO) {
            throw new ConflitoException("Este anúncio não está disponível para reserva");
        }

        reservaRepository.save(new Reserva(null, anuncio, comprador, Instant.now(), null, null));
        anuncio.setStatus(StatusAnuncio.RESERVADO);
        AnuncioResponseDTO resposta = anuncioService.paraResponse(salvar(anuncio));
        notificacaoService.notificar(vendedor.getId(), "Anúncio reservado",
                "Seu anúncio '" + anuncio.getTitulo() + "' foi reservado", "RESERVA", anuncioId);
        return resposta;
    }

    @Transactional
    public AnuncioResponseDTO cancelarReserva(UUID anuncioId, String emailUsuario) {
        Anuncio anuncio = buscarAnuncio(anuncioId);
        Usuario usuario = buscarUsuario(emailUsuario);
        Reserva reserva = reservaRepository
                .findByAnuncioIdAndCanceladaEmIsNullAndConcluidaEmIsNull(anuncioId)
                .orElseThrow(() -> new ConflitoException("Não há reserva ativa neste anúncio"));

        boolean participante = reserva.getComprador().getId().equals(usuario.getId())
                || (anuncio.getUsuario() != null && anuncio.getUsuario().getId().equals(usuario.getId()));
        if (!participante) {
            throw new AccessDeniedException("Somente o comprador ou o vendedor pode cancelar a reserva");
        }

        reserva.setCanceladaEm(Instant.now());
        reservaRepository.save(reserva);
        anuncio.setStatus(StatusAnuncio.ATIVO);
        return anuncioService.paraResponse(salvar(anuncio));
    }

    @Transactional
    public AnuncioResponseDTO concluir(UUID anuncioId, String emailUsuario) {
        Anuncio anuncio = buscarAnuncio(anuncioId);
        Usuario usuario = buscarUsuario(emailUsuario);
        Usuario vendedor = exigirVendedor(anuncio);
        if (!vendedor.getId().equals(usuario.getId())) {
            throw new AccessDeniedException("Somente o vendedor pode concluir a negociação");
        }

        Reserva reserva = reservaRepository
                .findByAnuncioIdAndCanceladaEmIsNullAndConcluidaEmIsNull(anuncioId)
                .orElseThrow(() -> new ConflitoException("Não há reserva ativa para concluir"));

        reserva.setConcluidaEm(Instant.now());
        reservaRepository.save(reserva);
        anuncio.setStatus(anuncio.getTipo() == TipoAnuncio.DOACAO
                ? StatusAnuncio.DOADO : StatusAnuncio.VENDIDO);
        AnuncioResponseDTO resposta = anuncioService.paraResponse(salvar(anuncio));
        notificacaoService.notificar(reserva.getComprador().getId(), "Negociação concluída",
                "A negociação do anúncio '" + anuncio.getTitulo() + "' foi concluída",
                "CONCLUSAO", anuncioId);
        return resposta;
    }

    @Transactional
    public AvaliacaoResponseDTO avaliar(UUID anuncioId, AvaliacaoRequestDTO dto, String emailUsuario) {
        Anuncio anuncio = buscarAnuncio(anuncioId);
        Usuario avaliador = buscarUsuario(emailUsuario);
        Usuario vendedor = exigirVendedor(anuncio);

        if (vendedor.getId().equals(avaliador.getId())) {
            throw new ParametroInvalidoException("O vendedor não pode avaliar a si mesmo");
        }
        reservaRepository.findByAnuncioIdAndCompradorIdAndConcluidaEmIsNotNull(
                        anuncioId, avaliador.getId())
                .orElseThrow(() -> new ConflitoException(
                        "Somente o comprador de uma negociação concluída pode avaliar o vendedor"));

        Avaliacao avaliacao = avaliacaoRepository.findByAnuncioIdAndAvaliadorId(anuncioId, avaliador.getId())
                .orElseGet(() -> new Avaliacao(null, anuncio, vendedor, avaliador,
                        dto.nota(), dto.comentario(), Instant.now()));
        avaliacao.setNota(dto.nota());
        avaliacao.setComentario(dto.comentario());
        AvaliacaoResponseDTO resposta = paraResponse(avaliacaoRepository.save(avaliacao));
        notificacaoService.notificar(vendedor.getId(), "Nova avaliação",
                "Você recebeu uma avaliação de " + dto.nota() + " estrelas", "AVALIACAO", anuncioId);
        return resposta;
    }

    @Transactional(readOnly = true)
    public AvaliacoesResponseDTO listarAvaliacoes(UUID vendedorId) {
        if (!usuarioRepository.existsById(vendedorId)) {
            throw new RecursoNaoEncontradoException("Usuário não encontrado");
        }
        List<AvaliacaoResponseDTO> avaliacoes = avaliacaoRepository
                .findByVendedorIdOrderByCriadoEmDesc(vendedorId).stream()
                .map(this::paraResponse)
                .toList();
        return new AvaliacoesResponseDTO(
                avaliacaoRepository.mediaPorVendedor(vendedorId),
                avaliacaoRepository.countByVendedorId(vendedorId),
                avaliacoes);
    }

    private AvaliacaoResponseDTO paraResponse(Avaliacao avaliacao) {
        return new AvaliacaoResponseDTO(
                avaliacao.getId(),
                avaliacao.getAnuncio().getId(),
                avaliacao.getVendedor().getId(),
                avaliacao.getAvaliador().getId(),
                avaliacao.getNota(),
                avaliacao.getComentario(),
                avaliacao.getCriadoEm()
        );
    }

    private Anuncio salvar(Anuncio anuncio) {
        return anuncioService.salvar(anuncio);
    }

    private Anuncio buscarAnuncio(UUID id) {
        return anuncioRepository.findByIdAndRemovidoEmIsNull(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Anúncio não encontrado"));
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }

    private Usuario exigirVendedor(Anuncio anuncio) {
        if (anuncio.getUsuario() == null) {
            throw new ParametroInvalidoException("Este anúncio não possui vendedor");
        }
        return anuncio.getUsuario();
    }
}
