package com.marketplace.desapegoUniversitario;

import com.marketplace.desapegoUniversitario.domain.Anuncio;
import com.marketplace.desapegoUniversitario.domain.AnuncioImagem;
import com.marketplace.desapegoUniversitario.domain.Categoria;
import com.marketplace.desapegoUniversitario.domain.Condicao;
import com.marketplace.desapegoUniversitario.domain.Role;
import com.marketplace.desapegoUniversitario.domain.StatusAnuncio;
import com.marketplace.desapegoUniversitario.domain.TipoAnuncio;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.dto.AnuncioFiltroDTO;
import com.marketplace.desapegoUniversitario.dto.AnuncioPatchDTO;
import com.marketplace.desapegoUniversitario.dto.AnuncioRequestDTO;
import com.marketplace.desapegoUniversitario.exception.ConflitoException;
import com.marketplace.desapegoUniversitario.exception.ImagemInvalidaException;
import com.marketplace.desapegoUniversitario.exception.ParametroInvalidoException;
import com.marketplace.desapegoUniversitario.repository.AnuncioImagemRepository;
import com.marketplace.desapegoUniversitario.repository.AnuncioRepository;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import com.marketplace.desapegoUniversitario.service.ArmazenamentoImagens;
import com.marketplace.desapegoUniversitario.service.AnuncioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.unit.DataSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnuncioServiceTest {

    private static final String EMAIL = "aluno@universidade.edu.br";

    @Mock
    private AnuncioRepository anuncioRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AnuncioImagemRepository imagemRepository;
    @Mock
    private ArmazenamentoImagens armazenamento;

    private AnuncioService service;
    private UUID anuncioId;
    private Anuncio anuncio;

    @BeforeEach
    void setUp() {
        anuncioId = UUID.randomUUID();
        service = new AnuncioService(anuncioRepository, usuarioRepository, imagemRepository,
                armazenamento, 5, DataSize.ofMegabytes(2), "anuncios");

        Usuario dono = new Usuario(UUID.randomUUID(), "Aluno", EMAIL, "hash",
                Role.USUARIO, true, Instant.now());
        anuncio = new Anuncio();
        anuncio.setId(anuncioId);
        anuncio.setTitulo("Caderno");
        anuncio.setPreco(BigDecimal.TEN);
        anuncio.setCategoria(Categoria.LIVROS);
        anuncio.setTipo(TipoAnuncio.VENDA);
        anuncio.setCondicao(Condicao.USADO);
        anuncio.setStatus(StatusAnuncio.ATIVO);
        anuncio.setUsuario(dono);
        anuncio.setCriadoEm(Instant.now());
        anuncio.setAtualizadoEm(Instant.now());
    }

    private AnuncioFiltroDTO filtro(String sort, BigDecimal min, BigDecimal max) {
        return new AnuncioFiltroDTO(null, null, null, min, max, null, null, null,
                0, 20, sort);
    }

    private AnuncioImagem imagem(Anuncio alvo, String chave, boolean capa) {
        return new AnuncioImagem(UUID.randomUUID(), alvo, chave, "/uploads/" + chave,
                "image/png", Instant.now(), 0, capa);
    }

    @Test
    void listagemDeveBuscarImagensEmUmaUnicaConsulta() {
        Anuncio outro = new Anuncio();
        outro.setId(UUID.randomUUID());
        outro.setTitulo("Borracha");
        when(anuncioRepository.findAllByRemovidoEmIsNull()).thenReturn(List.of(anuncio, outro));
        when(imagemRepository.findByAnuncioIdInOrderByAnuncioIdAscOrdemAscCriadoEmAscIdAsc(
                anyCollection())).thenReturn(List.of(
                        imagem(anuncio, "caderno.png", true),
                        imagem(outro, "borracha.png", true)));

        var respostas = service.listarTodos();

        assertEquals(2, respostas.size());
        assertEquals("/uploads/caderno.png", respostas.get(0).imagem());
        assertEquals("/uploads/borracha.png", respostas.get(1).imagem());
        verify(imagemRepository, times(1))
                .findByAnuncioIdInOrderByAnuncioIdAscOrdemAscCriadoEmAscIdAsc(anyCollection());
        verify(imagemRepository, never()).findByAnuncioIdOrderByOrdemAscCriadoEmAscIdAsc(any());
    }

    @Test
    void deveRejeitarCampoDeOrdenacaoForaDaWhitelist() {
        assertThrows(ParametroInvalidoException.class,
                () -> service.buscar(filtro("senha,asc", null, null)));
    }

    @Test
    void deveRejeitarDirecaoDeOrdenacaoInvalida() {
        assertThrows(ParametroInvalidoException.class,
                () -> service.buscar(filtro("preco,lateral", null, null)));
    }

    @Test
    void deveRejeitarIntervaloDePrecoInvertido() {
        assertThrows(ParametroInvalidoException.class,
                () -> service.buscar(filtro("preco,asc", BigDecimal.TEN, BigDecimal.ONE)));
    }

    @Test
    void deveRejeitarPaginaInvalida() {
        var filtro = new AnuncioFiltroDTO(null, null, null, null, null, null, null, null,
                -1, 101, null);
        assertThrows(ParametroInvalidoException.class, () -> service.buscar(filtro));
    }

    @Test
    void deveImpedirQueNaoDonoRemovaAnuncio() {
        anuncio.getUsuario().setId(UUID.randomUUID());
        when(anuncioRepository.findByIdAndRemovidoEmIsNull(anuncioId)).thenReturn(Optional.of(anuncio));
        when(usuarioRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(new Usuario(UUID.randomUUID(), "Outro", EMAIL, "hash",
                        Role.USUARIO, true, Instant.now())));

        assertThrows(AccessDeniedException.class, () -> service.deletarPorID(anuncioId, EMAIL));
    }

    @Test
    void deveRejeitarEnvioSemImagens() {
        when(anuncioRepository.findByIdAndRemovidoEmIsNull(anuncioId)).thenReturn(Optional.of(anuncio));
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(anuncio.getUsuario()));

        assertThrows(ImagemInvalidaException.class,
                () -> service.adicionarImagens(anuncioId, List.of(), EMAIL));
    }

    @Test
    void deveRespeitarLimiteTotalDeImagens() {
        AnuncioService limitado = new AnuncioService(anuncioRepository, usuarioRepository,
                imagemRepository, armazenamento, 1, DataSize.ofMegabytes(2), "anuncios");
        when(anuncioRepository.findByIdAndRemovidoEmIsNull(anuncioId)).thenReturn(Optional.of(anuncio));
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(anuncio.getUsuario()));

        List<org.springframework.web.multipart.MultipartFile> arquivos = List.of(
                new MockMultipartFile("imagens", "a.png", "image/png", new byte[] {1}),
                new MockMultipartFile("imagens", "b.png", "image/png", new byte[] {1}));

        assertThrows(ImagemInvalidaException.class,
                () -> limitado.adicionarImagens(anuncioId, arquivos, EMAIL));
    }

    @Test
    void deveRejeitarImagemComMagicBytesInvalido() {
        when(anuncioRepository.findByIdAndRemovidoEmIsNull(anuncioId)).thenReturn(Optional.of(anuncio));
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(anuncio.getUsuario()));

        var arquivo = new MockMultipartFile("imagens", "falso.png", "image/png",
                "NAO-E-PNG-123".getBytes());

        assertThrows(ImagemInvalidaException.class,
                () -> service.adicionarImagens(anuncioId, List.of(arquivo), EMAIL));
    }

    @Test
    void deveRejeitarTipoDeConteudoNaoImagem() {
        when(anuncioRepository.findByIdAndRemovidoEmIsNull(anuncioId)).thenReturn(Optional.of(anuncio));
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(anuncio.getUsuario()));

        var arquivo = new MockMultipartFile("imagens", "texto.txt", "text/plain", new byte[] {1});

        assertThrows(ImagemInvalidaException.class,
                () -> service.adicionarImagens(anuncioId, List.of(arquivo), EMAIL));
    }

    @Test
    void deveRejeitarImagemMaiorQueOLimite() {
        AnuncioService limitado = new AnuncioService(anuncioRepository, usuarioRepository,
                imagemRepository, armazenamento, 5, DataSize.ofBytes(2), "anuncios");
        when(anuncioRepository.findByIdAndRemovidoEmIsNull(anuncioId)).thenReturn(Optional.of(anuncio));
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(anuncio.getUsuario()));

        var arquivo = new MockMultipartFile("imagens", "grande.png", "image/png", new byte[] {1, 2, 3});

        assertThrows(ImagemInvalidaException.class,
                () -> limitado.adicionarImagens(anuncioId, List.of(arquivo), EMAIL));
    }

    @Test
    void deveRejeitarEdicaoDeAnuncioNaoAtivo() {
        anuncio.setStatus(StatusAnuncio.RESERVADO);
        when(anuncioRepository.findByIdAndRemovidoEmIsNull(anuncioId)).thenReturn(Optional.of(anuncio));
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(anuncio.getUsuario()));

        var completo = new AnuncioRequestDTO("Caderno revisado", BigDecimal.TEN, null, null,
                Categoria.LIVROS, null, null, null, null, null);
        var parcial = new AnuncioPatchDTO(Optional.of("Caderno revisado"), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

        assertThrows(ConflitoException.class, () -> service.atualizar(anuncioId, completo, EMAIL));
        assertThrows(ConflitoException.class, () -> service.atualizarParcial(anuncioId, parcial, EMAIL));
    }
}
