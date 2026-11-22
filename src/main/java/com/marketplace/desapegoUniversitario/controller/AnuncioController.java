package com.marketplace.desapegoUniversitario.controller;

import com.marketplace.desapegoUniversitario.domain.Categoria;
import com.marketplace.desapegoUniversitario.domain.Condicao;
import com.marketplace.desapegoUniversitario.domain.StatusAnuncio;
import com.marketplace.desapegoUniversitario.domain.TipoAnuncio;
import com.marketplace.desapegoUniversitario.dto.AnuncioPatchDTO;
import com.marketplace.desapegoUniversitario.dto.AnuncioRequestDTO;
import com.marketplace.desapegoUniversitario.dto.AnuncioResponseDTO;
import com.marketplace.desapegoUniversitario.dto.AnuncioFiltroDTO;
import com.marketplace.desapegoUniversitario.dto.ImagemResponseDTO;
import com.marketplace.desapegoUniversitario.dto.PaginadoDTO;
import com.marketplace.desapegoUniversitario.service.AnuncioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/anuncios")
@Tag(name = "Anúncios", description = "CRUD, busca paginada e imagens dos anúncios")
public class AnuncioController {

    private final AnuncioService anuncioService;

    public AnuncioController(AnuncioService anuncioService) {
        this.anuncioService = anuncioService;
    }

    @PostMapping
    @Operation(summary = "Cria um anúncio", description = "Requer autenticação. IDs são UUID e status inicial é ATIVO.")
    public AnuncioResponseDTO criar(@Valid @RequestBody AnuncioRequestDTO dto, Authentication authentication) {
        return anuncioService.criar(dto, authentication.getName());
    }

    @GetMapping
    @Operation(summary = "Busca anúncios com filtros combináveis",
            description = "Parâmetros: q, categoria, tipo, precoMin, precoMax, condicao, campus, status, "
                    + "page (base 0), size (1-100) e sort (ex.: `preco,asc;criadoEm,desc`).")
    public PaginadoDTO<AnuncioResponseDTO> buscar(
            @RequestParam(name = "q", required = false) String texto,
            @RequestParam(name = "categoria", required = false) Categoria categoria,
            @RequestParam(name = "tipo", required = false) TipoAnuncio tipo,
            @RequestParam(name = "precoMin", required = false) BigDecimal precoMin,
            @RequestParam(name = "precoMax", required = false) BigDecimal precoMax,
            @RequestParam(name = "condicao", required = false) Condicao condicao,
            @RequestParam(name = "campus", required = false) String campus,
            @RequestParam(name = "status", required = false) StatusAnuncio status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "criadoEm,desc") String sort) {
        return anuncioService.buscar(new AnuncioFiltroDTO(
                texto, categoria, tipo, precoMin, precoMax, condicao, campus, status, page, size, sort));
    }

    @GetMapping("/{id}")
    public AnuncioResponseDTO buscarPorId(@PathVariable UUID id) {
        return anuncioService.buscarPorId(id);
    }

    @GetMapping("/categoria/{categoria}")
    public List<AnuncioResponseDTO> listarPorCategoria(@PathVariable Categoria categoria) {
        return anuncioService.listarPorCategoria(categoria);
    }

    @PutMapping("/{id}")
    public AnuncioResponseDTO atualizar(@PathVariable UUID id,
                                        @Valid @RequestBody AnuncioRequestDTO dto,
                                        Authentication authentication) {
        return anuncioService.atualizar(id, dto, authentication.getName());
    }

    @PatchMapping("/{id}")
    public AnuncioResponseDTO atualizarParcial(@PathVariable UUID id,
                                                @Valid @RequestBody AnuncioPatchDTO dto,
                                                Authentication authentication) {
        return anuncioService.atualizarParcial(id, dto, authentication.getName());
    }

    @PostMapping(value = "/{id}/imagens", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Envia imagens (multipart/form-data)",
            description = "Campo `imagens`, até 5 arquivos de 2MB. Tipos aceitos: JPEG, PNG e WebP.")
    public List<ImagemResponseDTO> adicionarImagens(@PathVariable UUID id,
                                                     @RequestPart("imagens") List<MultipartFile> imagens,
                                                     Authentication authentication) {
        return anuncioService.adicionarImagens(id, imagens, authentication.getName());
    }

    @GetMapping("/{id}/imagens")
    public List<ImagemResponseDTO> listarImagens(@PathVariable UUID id, Authentication authentication) {
        return anuncioService.listarImagens(id, authentication.getName());
    }

    @DeleteMapping("/{id}/imagens/{imagemId}")
    public ResponseEntity<Void> removerImagem(@PathVariable UUID id,
                                               @PathVariable UUID imagemId,
                                               Authentication authentication) {
        anuncioService.removerImagem(id, imagemId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarPorId(@PathVariable UUID id, Authentication authentication) {
        anuncioService.deletarPorID(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
