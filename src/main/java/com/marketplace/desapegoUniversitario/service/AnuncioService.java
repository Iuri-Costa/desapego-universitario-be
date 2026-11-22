package com.marketplace.desapegoUniversitario.service;

import com.marketplace.desapegoUniversitario.domain.Anuncio;
import com.marketplace.desapegoUniversitario.domain.AnuncioImagem;
import com.marketplace.desapegoUniversitario.domain.Categoria;
import com.marketplace.desapegoUniversitario.domain.StatusAnuncio;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.dto.AnuncioFiltroDTO;
import com.marketplace.desapegoUniversitario.dto.AnuncioPatchDTO;
import com.marketplace.desapegoUniversitario.dto.AnuncioRequestDTO;
import com.marketplace.desapegoUniversitario.dto.AnuncioResponseDTO;
import com.marketplace.desapegoUniversitario.dto.ImagemResponseDTO;
import com.marketplace.desapegoUniversitario.dto.PaginadoDTO;
import com.marketplace.desapegoUniversitario.exception.ImagemInvalidaException;
import com.marketplace.desapegoUniversitario.exception.ParametroInvalidoException;
import com.marketplace.desapegoUniversitario.exception.RecursoNaoEncontradoException;
import com.marketplace.desapegoUniversitario.mapper.AnuncioMapper;
import com.marketplace.desapegoUniversitario.repository.AnuncioImagemRepository;
import com.marketplace.desapegoUniversitario.repository.AnuncioRepository;
import com.marketplace.desapegoUniversitario.repository.AnuncioSpecification;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
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
    private static final Map<String, String> EXTENSOES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final AnuncioRepository anuncioRepository;
    private final UsuarioRepository usuarioRepository;
    private final AnuncioImagemRepository imagemRepository;
    private final ArmazenamentoImagens armazenamento;
    private final int maxImagens;
    private final DataSize maxTamanho;
    private final String prefixo;

    public AnuncioService(AnuncioRepository anuncioRepository,
                          UsuarioRepository usuarioRepository,
                          AnuncioImagemRepository imagemRepository,
                          ArmazenamentoImagens armazenamento,
                          @Value("${app.storage.max-imagens}") int maxImagens,
                          @Value("${app.storage.max-tamanho}") DataSize maxTamanho,
                          @Value("${app.storage.s3.prefix}") String prefixo) {
        this.anuncioRepository = anuncioRepository;
        this.usuarioRepository = usuarioRepository;
        this.imagemRepository = imagemRepository;
        this.armazenamento = armazenamento;
        this.maxImagens = maxImagens;
        this.maxTamanho = maxTamanho;
        this.prefixo = prefixo;
    }

    @Transactional
    public AnuncioResponseDTO criar(AnuncioRequestDTO dto, String emailUsuario) {
        Usuario usuario = buscarUsuario(emailUsuario);
        Anuncio anuncio = AnuncioMapper.toEntity(dto, usuario);
        return paraResponse(salvar(anuncio));
    }

    @Transactional
    public AnuncioResponseDTO atualizar(UUID id, AnuncioRequestDTO dto, String emailUsuario) {
        Anuncio anuncio = buscarAnuncio(id);
        garantirDono(anuncio, buscarUsuario(emailUsuario));

        AnuncioMapper.aplicarCampos(anuncio, dto);
        return paraResponse(salvar(anuncio));
    }

    @Transactional
    public AnuncioResponseDTO atualizarParcial(UUID id, AnuncioPatchDTO dto, String emailUsuario) {
        Anuncio anuncio = buscarAnuncio(id);
        garantirDono(anuncio, buscarUsuario(emailUsuario));

        AnuncioMapper.aplicarCampos(anuncio, dto);
        return paraResponse(salvar(anuncio));
    }

    @Transactional(readOnly = true)
    public AnuncioResponseDTO buscarPorId(UUID id) {
        return paraResponse(buscarAnuncio(id));
    }

    @Transactional(readOnly = true)
    public PaginadoDTO<AnuncioResponseDTO> buscar(AnuncioFiltroDTO filtro) {
        Page<Anuncio> pagina = anuncioRepository.findAll(AnuncioSpecification.de(filtro), paginacao(filtro));
        return PaginadoDTO.de(pagina, this::paraResponse);
    }

    @Transactional(readOnly = true)
    public List<AnuncioResponseDTO> listarTodos() {
        return anuncioRepository.findAllByRemovidoEmIsNull().stream()
                .map(this::paraResponse)
                .toList();
    }

    @Transactional
    public void deletarPorID(UUID id, String emailUsuario) {
        Anuncio anuncio = buscarAnuncio(id);
        garantirDono(anuncio, buscarUsuario(emailUsuario));
        removerLogicamente(anuncio);
    }

    @Transactional(readOnly = true)
    public List<AnuncioResponseDTO> listarPorCategoria(Categoria categoria) {
        return anuncioRepository.findByCategoriaAndRemovidoEmIsNull(categoria).stream()
                .map(this::paraResponse)
                .toList();
    }

    @Transactional
    public AnuncioResponseDTO moderar(UUID id, boolean moderado) {
        Anuncio anuncio = buscarAnuncio(id);
        anuncio.setModerado(moderado);
        return paraResponse(salvar(anuncio));
    }

    @Transactional
    public void removerPorModeracao(UUID id) {
        removerLogicamente(buscarAnuncio(id));
    }

    @Transactional(readOnly = true)
    public List<ImagemResponseDTO> listarImagens(UUID id, String emailUsuario) {
        Anuncio anuncio = buscarAnuncio(id);
        garantirDono(anuncio, buscarUsuario(emailUsuario));
        return imagensDe(id);
    }

    @Transactional
    public List<ImagemResponseDTO> adicionarImagens(UUID id, List<MultipartFile> arquivos, String emailUsuario) {
        Anuncio anuncio = buscarAnuncio(id);
        garantirDono(anuncio, buscarUsuario(emailUsuario));

        if (arquivos == null || arquivos.isEmpty()) {
            throw new ImagemInvalidaException("Envie ao menos uma imagem");
        }
        if (arquivos.size() > maxImagens
                || imagemRepository.countByAnuncioId(id) + arquivos.size() > maxImagens) {
            throw new ImagemInvalidaException("Este anúncio aceita no máximo " + maxImagens + " imagens");
        }

        List<byte[]> conteudos = new ArrayList<>();
        List<String> tipos = new ArrayList<>();
        for (MultipartFile arquivo : arquivos) {
            String tipo = tipoConteudo(arquivo.getContentType());
            byte[] conteudo = conteudo(arquivo);
            validar(tipo, conteudo);
            tipos.add(tipo);
            conteudos.add(conteudo);
        }

        List<String> chavesSalvas = new ArrayList<>();
        try {
            List<AnuncioImagem> imagens = new ArrayList<>();
            for (int i = 0; i < arquivos.size(); i++) {
                String chave = prefixo.trim().isEmpty()
                        ? UUID.randomUUID() + "." + EXTENSOES.get(tipos.get(i))
                        : prefixo.trim() + "/" + UUID.randomUUID() + "." + EXTENSOES.get(tipos.get(i));
                String url = armazenamento.salvar(chave, conteudos.get(i), tipos.get(i));
                chavesSalvas.add(chave);
                AnuncioImagem imagem = new AnuncioImagem(null, anuncio, chave, url,
                        tipos.get(i), Instant.now());
                imagens.add(imagemRepository.save(imagem));
            }
            return imagens.stream()
                    .map(imagem -> new ImagemResponseDTO(imagem.getId(), imagem.getUrl()))
                    .toList();
        } catch (RuntimeException e) {
            chavesSalvas.forEach(armazenamento::remover);
            throw e;
        }
    }

    @Transactional
    public void removerImagem(UUID anuncioId, UUID imagemId, String emailUsuario) {
        Anuncio anuncio = buscarAnuncio(anuncioId);
        garantirDono(anuncio, buscarUsuario(emailUsuario));
        AnuncioImagem imagem = imagemRepository.findByIdAndAnuncioId(imagemId, anuncioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Imagem não encontrada"));
        imagemRepository.delete(imagem);
        armazenamento.remover(imagem.getChave());
    }

    private List<ImagemResponseDTO> imagensDe(UUID anuncioId) {
        return imagemRepository.findByAnuncioIdOrderByCriadoEmAsc(anuncioId).stream()
                .map(imagem -> new ImagemResponseDTO(imagem.getId(), imagem.getUrl()))
                .toList();
    }

    public AnuncioResponseDTO paraResponse(Anuncio anuncio) {
        return AnuncioMapper.toResponse(anuncio, imagensDe(anuncio.getId()));
    }

    private String tipoConteudo(String informado) {
        if (informado == null) {
            throw new ImagemInvalidaException("Tipo de conteúdo não informado");
        }
        String tipo = informado.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (!EXTENSOES.containsKey(tipo)) {
            throw new ImagemInvalidaException("Use imagens JPEG, PNG ou WebP");
        }
        return tipo;
    }

    private byte[] conteudo(MultipartFile arquivo) {
        if (arquivo.isEmpty()) {
            throw new ImagemInvalidaException("Arquivo de imagem vazio");
        }
        if (arquivo.getSize() > maxTamanho.toBytes()) {
            throw new ImagemInvalidaException("Cada imagem pode ter no máximo " + maxTamanho);
        }
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            throw new ImagemInvalidaException("Não foi possível ler a imagem");
        }
    }

    private void validar(String tipo, byte[] conteudo) {
        boolean valido = switch (tipo) {
            case "image/jpeg" -> conteudo.length >= 3
                    && (conteudo[0] & 0xFF) == 0xFF
                    && (conteudo[1] & 0xFF) == 0xD8
                    && (conteudo[2] & 0xFF) == 0xFF;
            case "image/png" -> conteudo.length >= 8
                    && new String(conteudo, 0, 8, StandardCharsets.ISO_8859_1)
                    .equals("\u0089PNG\r\n\u001a\n");
            case "image/webp" -> conteudo.length >= 12
                    && new String(conteudo, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                    && new String(conteudo, 8, 4, StandardCharsets.US_ASCII).equals("WEBP");
            default -> false;
        };
        if (!valido) {
            throw new ImagemInvalidaException("O conteúdo do arquivo não é uma imagem válida");
        }
    }

    private void removerLogicamente(Anuncio anuncio) {
        anuncio.setRemovidoEm(Instant.now());
        anuncio.setStatus(StatusAnuncio.REMOVIDO);
        salvar(anuncio);
    }

    public Anuncio salvar(Anuncio anuncio) {
        anuncio.setAtualizadoEm(Instant.now());
        return anuncioRepository.save(anuncio);
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
