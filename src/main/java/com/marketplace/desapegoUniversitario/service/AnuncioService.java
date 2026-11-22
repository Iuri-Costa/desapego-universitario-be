package com.marketplace.desapegoUniversitario.service;

import com.marketplace.desapegoUniversitario.domain.Anuncio;
import com.marketplace.desapegoUniversitario.domain.Categoria;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.dto.AnuncioRequestDTO;
import com.marketplace.desapegoUniversitario.dto.AnuncioResponseDTO;
import com.marketplace.desapegoUniversitario.exception.RecursoNaoEncontradoException;
import com.marketplace.desapegoUniversitario.mapper.AnuncioMapper;
import com.marketplace.desapegoUniversitario.repository.AnuncioRepository;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AnuncioService {

    private final AnuncioRepository anuncioRepository;
    private final UsuarioRepository usuarioRepository;

    public AnuncioService(AnuncioRepository anuncioRepository, UsuarioRepository usuarioRepository) {
        this.anuncioRepository = anuncioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public AnuncioResponseDTO criar(AnuncioRequestDTO dto, String emailUsuario) {
        Usuario usuario = buscarUsuario(emailUsuario);
        Anuncio anuncio = AnuncioMapper.toEntity(dto, usuario);
        return AnuncioMapper.toResponse(anuncioRepository.save(anuncio));
    }

    @Transactional
    public AnuncioResponseDTO atualizar(UUID id, AnuncioRequestDTO dto, String emailUsuario) {
        Anuncio anuncio = buscarAnuncio(id);
        garantirDono(anuncio, buscarUsuario(emailUsuario));

        AnuncioMapper.aplicarCampos(anuncio, dto);
        return AnuncioMapper.toResponse(salvar(anuncio));
    }

    @Transactional(readOnly = true)
    public AnuncioResponseDTO buscarPorId(UUID id) {
        return AnuncioMapper.toResponse(buscarAnuncio(id));
    }

    @Transactional(readOnly = true)
    public List<AnuncioResponseDTO> listarTodos() {
        return AnuncioMapper.toResponseList(anuncioRepository.findAll());
    }

    @Transactional
    public void deletarPorID(UUID id, String emailUsuario) {
        Anuncio anuncio = buscarAnuncio(id);
        garantirDono(anuncio, buscarUsuario(emailUsuario));
        anuncioRepository.delete(anuncio);
    }

    @Transactional(readOnly = true)
    public List<AnuncioResponseDTO> listarPorCategoria(Categoria categoria) {
        return AnuncioMapper.toResponseList(anuncioRepository.findByCategoria(categoria));
    }

    @Transactional
    public AnuncioResponseDTO moderar(UUID id, boolean moderado) {
        Anuncio anuncio = buscarAnuncio(id);
        anuncio.setModerado(moderado);
        return AnuncioMapper.toResponse(anuncioRepository.save(anuncio));
    }

    @Transactional
    public void removerPorModeracao(UUID id) {
        anuncioRepository.delete(buscarAnuncio(id));
    }

    private Anuncio salvar(Anuncio anuncio) {
        anuncio.setAtualizadoEm(Instant.now());
        return anuncioRepository.save(anuncio);
    }

    private Anuncio buscarAnuncio(UUID id) {
        return anuncioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Anúncio não encontrado"));
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }

    private void garantirDono(Anuncio anuncio, Usuario usuario) {
        if (anuncio.getUsuario() == null || !anuncio.getUsuario().getId().equals(usuario.getId())) {
            throw new AccessDeniedException("Somente o dono pode alterar este anúncio");
        }
    }
}
