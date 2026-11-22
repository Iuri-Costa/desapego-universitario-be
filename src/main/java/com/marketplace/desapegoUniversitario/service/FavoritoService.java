package com.marketplace.desapegoUniversitario.service;

import com.marketplace.desapegoUniversitario.domain.Anuncio;
import com.marketplace.desapegoUniversitario.domain.Favorito;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.dto.AnuncioResponseDTO;
import com.marketplace.desapegoUniversitario.dto.FavoritoResponseDTO;
import com.marketplace.desapegoUniversitario.dto.PaginadoDTO;
import com.marketplace.desapegoUniversitario.exception.RecursoNaoEncontradoException;
import com.marketplace.desapegoUniversitario.repository.AnuncioRepository;
import com.marketplace.desapegoUniversitario.repository.FavoritoRepository;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import com.marketplace.desapegoUniversitario.util.Paginas;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class FavoritoService {

    private final FavoritoRepository favoritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AnuncioRepository anuncioRepository;
    private final AnuncioService anuncioService;

    public FavoritoService(FavoritoRepository favoritoRepository,
                           UsuarioRepository usuarioRepository,
                           AnuncioRepository anuncioRepository,
                           AnuncioService anuncioService) {
        this.favoritoRepository = favoritoRepository;
        this.usuarioRepository = usuarioRepository;
        this.anuncioRepository = anuncioRepository;
        this.anuncioService = anuncioService;
    }

    @Transactional
    public FavoritoResponseDTO adicionar(UUID anuncioId, String emailUsuario) {
        Usuario usuario = buscarUsuario(emailUsuario);
        Anuncio anuncio = anuncioRepository.findByIdAndRemovidoEmIsNull(anuncioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Anúncio não encontrado"));

        if (!favoritoRepository.existsByUsuarioIdAndAnuncioId(usuario.getId(), anuncioId)) {
            favoritoRepository.save(new Favorito(null, usuario, anuncio, Instant.now()));
        }
        return new FavoritoResponseDTO(anuncioId, true);
    }

    @Transactional
    public void remover(UUID anuncioId, String emailUsuario) {
        Usuario usuario = buscarUsuario(emailUsuario);
        favoritoRepository.findByUsuarioIdAndAnuncioId(usuario.getId(), anuncioId)
                .ifPresent(favoritoRepository::delete);
    }

    @Transactional(readOnly = true)
    public PaginadoDTO<AnuncioResponseDTO> listar(String emailUsuario, int page, int size) {
        Usuario usuario = buscarUsuario(emailUsuario);
        Page<Favorito> favoritos = favoritoRepository
                .findByUsuarioIdAndAnuncioRemovidoEmIsNullOrderByCriadoEmDesc(
                        usuario.getId(), Paginas.de(page, size, Sort.by(Sort.Direction.DESC, "criadoEm")));
        return PaginadoDTO.de(favoritos,
                favorito -> anuncioService.buscarPorId(favorito.getAnuncio().getId()));
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }
}
