package com.marketplace.desapegoUniversitario.service;

import com.marketplace.desapegoUniversitario.domain.Anuncio;
import com.marketplace.desapegoUniversitario.domain.StatusAnuncio;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.dto.AnuncioResponseDTO;
import com.marketplace.desapegoUniversitario.dto.AtualizarPerfilDTO;
import com.marketplace.desapegoUniversitario.dto.PaginadoDTO;
import com.marketplace.desapegoUniversitario.dto.PerfilResponseDTO;
import com.marketplace.desapegoUniversitario.exception.RecursoNaoEncontradoException;
import com.marketplace.desapegoUniversitario.repository.AnuncioRepository;
import com.marketplace.desapegoUniversitario.repository.FavoritoRepository;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import com.marketplace.desapegoUniversitario.util.Paginas;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PerfilService {

    private final UsuarioRepository usuarioRepository;
    private final AnuncioRepository anuncioRepository;
    private final FavoritoRepository favoritoRepository;
    private final AnuncioService anuncioService;

    public PerfilService(UsuarioRepository usuarioRepository,
                         AnuncioRepository anuncioRepository,
                         FavoritoRepository favoritoRepository,
                         AnuncioService anuncioService) {
        this.usuarioRepository = usuarioRepository;
        this.anuncioRepository = anuncioRepository;
        this.favoritoRepository = favoritoRepository;
        this.anuncioService = anuncioService;
    }

    @Transactional(readOnly = true)
    public PerfilResponseDTO buscar(String emailUsuario) {
        Usuario usuario = buscarUsuario(emailUsuario);
        return new PerfilResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getRole().name(),
                usuario.isEmailVerificado(),
                usuario.getCriadoEm(),
                anuncioRepository.countByUsuarioIdAndRemovidoEmIsNull(usuario.getId()),
                anuncioRepository.countByUsuarioIdAndStatusAndRemovidoEmIsNull(
                        usuario.getId(), StatusAnuncio.ATIVO),
                favoritoRepository.countByUsuarioId(usuario.getId())
        );
    }

    @Transactional
    public PerfilResponseDTO atualizar(String emailUsuario, AtualizarPerfilDTO dto) {
        Usuario usuario = buscarUsuario(emailUsuario);
        usuario.setNome(dto.nome().trim());
        usuarioRepository.save(usuario);
        return buscar(emailUsuario);
    }

    @Transactional(readOnly = true)
    public PaginadoDTO<AnuncioResponseDTO> historico(String emailUsuario, StatusAnuncio status,
                                                      boolean incluirRemovidos, int page, int size) {
        Usuario usuario = buscarUsuario(emailUsuario);
        PageRequest paginacao = Paginas.de(page, size, Sort.by(Sort.Direction.DESC, "criadoEm"));
        Page<Anuncio> anuncios = consultarHistorico(usuario.getId(), status, incluirRemovidos, paginacao);
        return PaginadoDTO.de(anuncios, anuncioService::paraResponse);
    }

    private Page<Anuncio> consultarHistorico(UUID usuarioId, StatusAnuncio status,
                                             boolean incluirRemovidos, PageRequest paginacao) {
        if (status != null) {
            return incluirRemovidos
                    ? anuncioRepository.findByUsuarioIdAndStatus(usuarioId, status, paginacao)
                    : anuncioRepository.findByUsuarioIdAndStatusAndRemovidoEmIsNull(
                            usuarioId, status, paginacao);
        }
        return incluirRemovidos
                ? anuncioRepository.findByUsuarioId(usuarioId, paginacao)
                : anuncioRepository.findByUsuarioIdAndRemovidoEmIsNull(usuarioId, paginacao);
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }
}
