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

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

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
                favoritoRepository.countByUsuarioId(usuario.getId()),
                usuario.getContato(),
                usuario.getAvatar()
        );
    }

    @Transactional
    public PerfilResponseDTO atualizar(String emailUsuario, AtualizarPerfilDTO dto) {
        Usuario usuario = buscarUsuario(emailUsuario);
        aplicarSePresente(dto.nome(), valor -> usuario.setNome(valor.trim()));
        aplicarSePresente(dto.contato(), valor -> usuario.setContato(textoOuNulo(valor)));
        aplicarSePresente(dto.avatar(), valor -> usuario.setAvatar(textoOuNulo(valor)));
        usuarioRepository.save(usuario);
        return buscar(emailUsuario);
    }

    private static <T> void aplicarSePresente(Optional<T> valor, Consumer<T> consumidor) {
        if (valor != null && valor.isPresent()) {
            consumidor.accept(valor.get());
        }
    }

    private static String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    @Transactional(readOnly = true)
    public PaginadoDTO<AnuncioResponseDTO> historico(String emailUsuario, StatusAnuncio status,
                                                      boolean incluirRemovidos, int page, int size) {
        Usuario usuario = buscarUsuario(emailUsuario);
        PageRequest paginacao = Paginas.de(page, size, Sort.by(Sort.Direction.DESC, "criadoEm"));
        Page<Anuncio> anuncios = consultarHistorico(usuario.getId(), status, incluirRemovidos, paginacao);
        Map<UUID, AnuncioResponseDTO> respostas = anuncioService.paraResponseMap(anuncios.getContent());
        return PaginadoDTO.de(anuncios, anuncio -> respostas.get(anuncio.getId()));
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
