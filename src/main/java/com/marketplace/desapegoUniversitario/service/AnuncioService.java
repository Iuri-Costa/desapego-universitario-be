package com.marketplace.desapegoUniversitario.service;

import com.marketplace.desapegoUniversitario.domain.Anuncio;
import com.marketplace.desapegoUniversitario.domain.Categoria;
import com.marketplace.desapegoUniversitario.domain.StatusAnuncio;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.dto.AnuncioFiltroDTO;
import com.marketplace.desapegoUniversitario.dto.AnuncioPatchDTO;
import com.marketplace.desapegoUniversitario.dto.AnuncioRequestDTO;
import com.marketplace.desapegoUniversitario.dto.AnuncioResponseDTO;
import com.marketplace.desapegoUniversitario.dto.PaginadoDTO;
import com.marketplace.desapegoUniversitario.exception.ParametroInvalidoException;
import com.marketplace.desapegoUniversitario.exception.RecursoNaoEncontradoException;
import com.marketplace.desapegoUniversitario.mapper.AnuncioMapper;
import com.marketplace.desapegoUniversitario.repository.AnuncioRepository;
import com.marketplace.desapegoUniversitario.repository.AnuncioSpecification;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AnuncioService {

    private static final Map<String, String> ORDENACAO_PERMITIDA = Map.of(
            "titulo", "titulo",
            "preco", "preco",
            "categoria", "categoria",
            "condicao", "condicao",
            "tipo", "tipo",
            "status", "status",
            "campus", "campus",
            "criadoEm", "criadoEm",
            "atualizadoEm", "atualizadoEm"
    );
    private static final int TAMANHO_MAXIMO_PAGINA = 100;

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
        return AnuncioMapper.toResponse(salvar(anuncio));
    }

    @Transactional
    public AnuncioResponseDTO atualizar(UUID id, AnuncioRequestDTO dto, String emailUsuario) {
        Anuncio anuncio = buscarAnuncio(id);
        garantirDono(anuncio, buscarUsuario(emailUsuario));

        AnuncioMapper.aplicarCampos(anuncio, dto);
        return AnuncioMapper.toResponse(salvar(anuncio));
    }

    @Transactional
    public AnuncioResponseDTO atualizarParcial(UUID id, AnuncioPatchDTO dto, String emailUsuario) {
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
    public PaginadoDTO<AnuncioResponseDTO> buscar(AnuncioFiltroDTO filtro) {
        Page<Anuncio> pagina = anuncioRepository.findAll(AnuncioSpecification.de(filtro), paginacao(filtro));
        return PaginadoDTO.de(pagina, AnuncioMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<AnuncioResponseDTO> listarTodos() {
        return AnuncioMapper.toResponseList(anuncioRepository.findAllByRemovidoEmIsNull());
    }

    @Transactional
    public void deletarPorID(UUID id, String emailUsuario) {
        Anuncio anuncio = buscarAnuncio(id);
        garantirDono(anuncio, buscarUsuario(emailUsuario));
        removerLogicamente(anuncio);
    }

    @Transactional(readOnly = true)
    public List<AnuncioResponseDTO> listarPorCategoria(Categoria categoria) {
        return AnuncioMapper.toResponseList(
                anuncioRepository.findByCategoriaAndRemovidoEmIsNull(categoria));
    }

    @Transactional
    public AnuncioResponseDTO moderar(UUID id, boolean moderado) {
        Anuncio anuncio = buscarAnuncio(id);
        anuncio.setModerado(moderado);
        return AnuncioMapper.toResponse(salvar(anuncio));
    }

    @Transactional
    public void removerPorModeracao(UUID id) {
        removerLogicamente(buscarAnuncio(id));
    }

    private Pageable paginacao(AnuncioFiltroDTO filtro) {
        if (filtro.page() < 0 || filtro.size() < 1 || filtro.size() > TAMANHO_MAXIMO_PAGINA) {
            throw new ParametroInvalidoException(
                    "Use page >= 0 e size entre 1 e " + TAMANHO_MAXIMO_PAGINA);
        }
        BigDecimal min = filtro.precoMin();
        BigDecimal max = filtro.precoMax();
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new ParametroInvalidoException("O preço mínimo não pode ser maior que o preço máximo");
        }

        String ordenacao = filtro.sort() == null || filtro.sort().isBlank()
                ? "criadoEm,desc" : filtro.sort();
        List<Sort.Order> ordens = Arrays.stream(ordenacao.split(";"))
                .map(String::trim)
                .filter(parte -> !parte.isEmpty())
                .map(parte -> {
                    String[] pedacos = parte.split(",", -1);
                    String campo = ORDENACAO_PERMITIDA.get(pedacos[0].trim());
                    if (campo == null) {
                        throw new ParametroInvalidoException(
                                "Campo de ordenação inválido: " + pedacos[0]);
                    }
                    String direcao = pedacos.length > 1 ? pedacos[1].trim().toLowerCase(Locale.ROOT) : "asc";
                    if (!direcao.equals("asc") && !direcao.equals("desc")) {
                        throw new ParametroInvalidoException("Direção de ordenação inválida: " + direcao);
                    }
                    return direcao.equals("desc") ? Sort.Order.desc(campo) : Sort.Order.asc(campo);
                })
                .toList();
        if (ordens.isEmpty()) {
            throw new ParametroInvalidoException("Informe ao menos um campo de ordenação");
        }
        return PageRequest.of(filtro.page(), filtro.size(), Sort.by(ordens));
    }

    private void removerLogicamente(Anuncio anuncio) {
        anuncio.setRemovidoEm(Instant.now());
        anuncio.setStatus(StatusAnuncio.REMOVIDO);
        salvar(anuncio);
    }

    private Anuncio salvar(Anuncio anuncio) {
        anuncio.setAtualizadoEm(Instant.now());
        return anuncioRepository.save(anuncio);
    }

    private Anuncio buscarAnuncio(UUID id) {
        return anuncioRepository.findByIdAndRemovidoEmIsNull(id)
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
