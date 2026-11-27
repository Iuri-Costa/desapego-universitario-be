package com.marketplace.desapegoUniversitario.service;

import com.marketplace.desapegoUniversitario.domain.Anuncio;
import com.marketplace.desapegoUniversitario.domain.Avaliacao;
import com.marketplace.desapegoUniversitario.domain.Reserva;
import com.marketplace.desapegoUniversitario.domain.StatusAnuncio;
import com.marketplace.desapegoUniversitario.domain.StatusReserva;
import com.marketplace.desapegoUniversitario.domain.TipoAnuncio;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.dto.AnuncioResponseDTO;
import com.marketplace.desapegoUniversitario.dto.AvaliacaoRequestDTO;
import com.marketplace.desapegoUniversitario.dto.AvaliacaoResponseDTO;
import com.marketplace.desapegoUniversitario.dto.AvaliacoesResponseDTO;
import com.marketplace.desapegoUniversitario.dto.PaginadoDTO;
import com.marketplace.desapegoUniversitario.dto.ReservaResponseDTO;
import com.marketplace.desapegoUniversitario.exception.ConflitoException;
import com.marketplace.desapegoUniversitario.exception.ParametroInvalidoException;
import com.marketplace.desapegoUniversitario.exception.RecursoNaoEncontradoException;
import com.marketplace.desapegoUniversitario.repository.AnuncioRepository;
import com.marketplace.desapegoUniversitario.repository.AvaliacaoRepository;
import com.marketplace.desapegoUniversitario.repository.ReservaRepository;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import com.marketplace.desapegoUniversitario.util.Paginas;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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
    private final Duration reservaTtl;

    public NegociacaoService(ReservaRepository reservaRepository,
                             AvaliacaoRepository avaliacaoRepository,
                             AnuncioRepository anuncioRepository,
                             UsuarioRepository usuarioRepository,
                             AnuncioService anuncioService,
                             NotificacaoService notificacaoService,
                             @Value("${app.reserva.ttl}") Duration reservaTtl) {
        this.reservaRepository = reservaRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.anuncioRepository = anuncioRepository;
        this.usuarioRepository = usuarioRepository;
        this.anuncioService = anuncioService;
        this.notificacaoService = notificacaoService;
        this.reservaTtl = reservaTtl;
    }

    @Transactional
    public AnuncioResponseDTO reservar(UUID anuncioId, String emailUsuario) {
        expirarVencidas();
        Anuncio anuncio = buscarAnuncioParaReserva(anuncioId);
        Usuario comprador = buscarUsuario(emailUsuario);
        Usuario vendedor = exigirVendedor(anuncio);

        if (vendedor.getId().equals(comprador.getId())) {
            throw new ParametroInvalidoException("O vendedor não pode reservar o próprio anúncio");
        }
        var existente = reservaRepository
                .findByAnuncioIdAndCanceladaEmIsNullAndConcluidaEmIsNullAndExpiradaEmIsNull(anuncioId);
        if (existente.isPresent()) {
            if (existente.get().getComprador().getId().equals(comprador.getId())) {
                return anuncioService.paraResponse(anuncio);
            }
            throw new ConflitoException("Este anúncio já está reservado");
        }
        if (anuncio.getStatus() != StatusAnuncio.ATIVO) {
            throw new ConflitoException("Este anúncio não está disponível para reserva");
        }

        try {
            reservaRepository.saveAndFlush(new Reserva(
                    null, anuncio, comprador, Instant.now(), null, null, null));
        } catch (DataIntegrityViolationException e) {
            // A unique index da coluna gerada é a última linha de defesa contra duas
            // reservas ativas simultaneamente; sem ela a checagem acima sofre TOCTOU.
            throw new ConflitoException("Este anúncio já está reservado");
        }
        AnuncioResponseDTO resposta = anuncioService.alterarStatus(anuncio, StatusAnuncio.RESERVADO);
        notificacaoService.notificar(vendedor.getId(), "Anúncio reservado",
                "Seu anúncio '" + anuncio.getTitulo() + "' foi reservado", "RESERVA", anuncioId);
        return resposta;
    }

    @Transactional
    public AnuncioResponseDTO cancelarReserva(UUID anuncioId, String emailUsuario) {
        expirarVencidas();
        Anuncio anuncio = buscarAnuncio(anuncioId);
        Usuario usuario = buscarUsuario(emailUsuario);
        Reserva reserva = reservaRepository
                .findByAnuncioIdAndCanceladaEmIsNullAndConcluidaEmIsNullAndExpiradaEmIsNull(anuncioId)
                .orElseThrow(() -> new ConflitoException("Não há reserva ativa neste anúncio"));

        exigirParticipante(anuncio, reserva, usuario);
        reserva.setCanceladaEm(Instant.now());
        reservaRepository.save(reserva);
        return anuncioService.alterarStatus(anuncio, StatusAnuncio.ATIVO);
    }

    @Transactional
    public AnuncioResponseDTO concluir(UUID anuncioId, String emailUsuario) {
        expirarVencidas();
        Anuncio anuncio = buscarAnuncio(anuncioId);
        Usuario usuario = buscarUsuario(emailUsuario);
        Usuario vendedor = exigirVendedor(anuncio);
        if (!vendedor.getId().equals(usuario.getId())) {
            throw new AccessDeniedException("Somente o vendedor pode concluir a negociação");
        }

        Reserva reserva = reservaRepository
                .findByAnuncioIdAndCanceladaEmIsNullAndConcluidaEmIsNullAndExpiradaEmIsNull(anuncioId)
                .orElseThrow(() -> new ConflitoException("Não há reserva ativa para concluir"));

        reserva.setConcluidaEm(Instant.now());
        reservaRepository.save(reserva);
        StatusAnuncio statusFinal = anuncio.getTipo() == TipoAnuncio.DOACAO
                ? StatusAnuncio.DOADO : StatusAnuncio.VENDIDO;
        AnuncioResponseDTO resposta = anuncioService.alterarStatus(anuncio, statusFinal);
        notificacaoService.notificar(reserva.getComprador().getId(), "Negociação concluída",
                "A negociação do anúncio '" + anuncio.getTitulo() + "' foi concluída",
                "CONCLUSAO", anuncioId);
        return resposta;
    }

    @Transactional
    public ReservaResponseDTO consultarReserva(UUID anuncioId, String emailUsuario) {
        expirarVencidas();
        Anuncio anuncio = buscarAnuncio(anuncioId);
        Usuario usuario = buscarUsuario(emailUsuario);
        Reserva reserva = reservaRepository.findTopByAnuncioIdOrderByCriadoEmDesc(anuncioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Este anúncio não possui reserva"));
        exigirParticipante(anuncio, reserva, usuario);
        return paraResponse(reserva);
    }

    @Transactional
    public PaginadoDTO<ReservaResponseDTO> listarReservas(String emailUsuario, int page, int size) {
        expirarVencidas();
        Usuario usuario = buscarUsuario(emailUsuario);
        Pageable paginacao = Paginas.de(page, size);
        Page<Reserva> reservas = reservaRepository.findHistorico(usuario.getId(), paginacao);
        return PaginadoDTO.de(reservas, this::paraResponse);
    }

    @Transactional
    public void expirarVencidas() {
        Instant agora = Instant.now();
        Instant limite = agora.minus(reservaTtl);
        for (Reserva reserva : reservaRepository
                .findByCanceladaEmIsNullAndConcluidaEmIsNullAndExpiradaEmIsNullAndCriadoEmBefore(limite)) {
            reserva.setExpiradaEm(agora);
            reservaRepository.save(reserva);
            Anuncio anuncio = reserva.getAnuncio();
            if (anuncio.getStatus() == StatusAnuncio.RESERVADO) {
                anuncioService.alterarStatus(anuncio, StatusAnuncio.ATIVO);
            }
        }
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

    private void exigirParticipante(Anuncio anuncio, Reserva reserva, Usuario usuario) {
        boolean participante = reserva.getComprador().getId().equals(usuario.getId())
                || (anuncio.getUsuario() != null && anuncio.getUsuario().getId().equals(usuario.getId()));
        if (!participante) {
            throw new AccessDeniedException("Somente o comprador ou o vendedor desta reserva podem consultá-la");
        }
    }

    private ReservaResponseDTO paraResponse(Reserva reserva) {
        Anuncio anuncio = reserva.getAnuncio();
        Usuario vendedor = anuncio.getUsuario();
        return new ReservaResponseDTO(
                reserva.getId(),
                anuncio.getId(),
                anuncio.getTitulo(),
                anuncio.getStatus(),
                reserva.getComprador().getId(),
                reserva.getComprador().getNome(),
                vendedor == null ? null : vendedor.getId(),
                statusDaReserva(reserva),
                reserva.getCriadoEm(),
                reserva.getCanceladaEm(),
                reserva.getConcluidaEm(),
                reserva.getExpiradaEm());
    }

    private StatusReserva statusDaReserva(Reserva reserva) {
        if (reserva.getExpiradaEm() != null) {
            return StatusReserva.EXPIRADA;
        }
        if (reserva.getConcluidaEm() != null) {
            return StatusReserva.CONCLUIDA;
        }
        if (reserva.getCanceladaEm() != null) {
            return StatusReserva.CANCELADA;
        }
        return StatusReserva.ATIVA;
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

    private Anuncio buscarAnuncio(UUID id) {
        return anuncioRepository.findByIdAndRemovidoEmIsNull(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Anúncio não encontrado"));
    }

    private Anuncio buscarAnuncioParaReserva(UUID id) {
        return anuncioRepository.findByIdForUpdate(id)
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
