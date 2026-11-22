package com.marketplace.desapegoUniversitario.service;

import com.marketplace.desapegoUniversitario.domain.Anuncio;
import com.marketplace.desapegoUniversitario.domain.Denuncia;
import com.marketplace.desapegoUniversitario.domain.StatusDenuncia;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.dto.DenunciaRequestDTO;
import com.marketplace.desapegoUniversitario.dto.DenunciaResponseDTO;
import com.marketplace.desapegoUniversitario.dto.PaginadoDTO;
import com.marketplace.desapegoUniversitario.dto.ResolucaoDenunciaDTO;
import com.marketplace.desapegoUniversitario.exception.ConflitoException;
import com.marketplace.desapegoUniversitario.exception.ParametroInvalidoException;
import com.marketplace.desapegoUniversitario.exception.RecursoNaoEncontradoException;
import com.marketplace.desapegoUniversitario.repository.AnuncioRepository;
import com.marketplace.desapegoUniversitario.repository.DenunciaRepository;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import com.marketplace.desapegoUniversitario.util.Paginas;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class DenunciaService {

    private final DenunciaRepository denunciaRepository;
    private final AnuncioRepository anuncioRepository;
    private final UsuarioRepository usuarioRepository;
    private final AnuncioService anuncioService;
    private final NotificacaoService notificacaoService;

    public DenunciaService(DenunciaRepository denunciaRepository,
                           AnuncioRepository anuncioRepository,
                           UsuarioRepository usuarioRepository,
                           AnuncioService anuncioService,
                           NotificacaoService notificacaoService) {
        this.denunciaRepository = denunciaRepository;
        this.anuncioRepository = anuncioRepository;
        this.usuarioRepository = usuarioRepository;
        this.anuncioService = anuncioService;
        this.notificacaoService = notificacaoService;
    }

    @Transactional
    public DenunciaResponseDTO criar(UUID anuncioId, DenunciaRequestDTO dto, String emailUsuario) {
        Anuncio anuncio = anuncioRepository.findByIdAndRemovidoEmIsNull(anuncioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Anúncio não encontrado"));
        Usuario denunciante = buscarUsuario(emailUsuario);
        if (anuncio.getUsuario() != null && anuncio.getUsuario().getId().equals(denunciante.getId())) {
            throw new ParametroInvalidoException("Você não pode denunciar seu próprio anúncio");
        }
        if (denunciaRepository.findByAnuncioIdAndDenuncianteIdAndStatus(
                anuncioId, denunciante.getId(), StatusDenuncia.PENDENTE).isPresent()) {
            throw new ConflitoException("Você já denunciou este anúncio");
        }

        Denuncia denuncia = denunciaRepository.save(new Denuncia(
                null, anuncio, denunciante, dto.motivo(),
                dto.descricao() == null ? null : dto.descricao().trim(),
                StatusDenuncia.PENDENTE, null, null, Instant.now()));
        return paraResponse(denuncia);
    }

    @Transactional(readOnly = true)
    public PaginadoDTO<DenunciaResponseDTO> listar(StatusDenuncia status, int page, int size) {
        PageRequest paginacao = Paginas.de(page, size);
        Page<Denuncia> denuncias = status == null
                ? denunciaRepository.findAllByOrderByCriadoEmAsc(paginacao)
                : denunciaRepository.findByStatusOrderByCriadoEmAsc(status, paginacao);
        return PaginadoDTO.de(denuncias, this::paraResponse);
    }

    @Transactional(readOnly = true)
    public PaginadoDTO<DenunciaResponseDTO> listarMinhas(String emailUsuario, int page, int size) {
        Usuario denunciante = buscarUsuario(emailUsuario);
        Page<Denuncia> denuncias = denunciaRepository
                .findByDenuncianteIdOrderByCriadoEmDesc(denunciante.getId(), Paginas.de(page, size));
        return PaginadoDTO.de(denuncias, this::paraResponse);
    }

    @Transactional(readOnly = true)
    public DenunciaResponseDTO buscarMinha(UUID id, String emailUsuario) {
        Usuario denunciante = buscarUsuario(emailUsuario);
        Denuncia denuncia = denunciaRepository.findByIdAndDenuncianteId(id, denunciante.getId())
                .orElseThrow(() -> new AccessDeniedException("Esta denúncia não pertence ao usuário"));
        return paraResponse(denuncia);
    }

    @Transactional
    public DenunciaResponseDTO resolver(UUID id, ResolucaoDenunciaDTO dto, String emailUsuario) {
        Denuncia denuncia = denunciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Denúncia não encontrada"));
        if (denuncia.getStatus() != StatusDenuncia.PENDENTE) {
            throw new ConflitoException("Esta denúncia já foi resolvida");
        }
        if (dto.status() == StatusDenuncia.PENDENTE) {
            throw new ParametroInvalidoException("Informe RESOLVIDA ou REJEITADA");
        }

        if (dto.deveRemoverAnuncio()) {
            anuncioService.removerPorModeracao(denuncia.getAnuncio().getId());
        }
        denuncia.setStatus(dto.status());
        denuncia.setResolvidaEm(Instant.now());
        denuncia.setResolvidaPor(buscarUsuario(emailUsuario));
        DenunciaResponseDTO resposta = paraResponse(denunciaRepository.save(denuncia));
        notificacaoService.notificar(denuncia.getDenunciante().getId(), "Denúncia analisada",
                "Sua denúncia foi analisada: " + dto.status(), "DENUNCIA", denuncia.getAnuncio().getId());
        return resposta;
    }

    private DenunciaResponseDTO paraResponse(Denuncia denuncia) {
        return new DenunciaResponseDTO(
                denuncia.getId(),
                denuncia.getAnuncio().getId(),
                denuncia.getDenunciante().getId(),
                denuncia.getMotivo(),
                denuncia.getDescricao(),
                denuncia.getStatus().name(),
                denuncia.getResolvidaEm(),
                denuncia.getCriadoEm()
        );
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }
}
