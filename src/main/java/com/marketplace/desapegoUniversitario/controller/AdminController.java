package com.marketplace.desapegoUniversitario.controller;

import com.marketplace.desapegoUniversitario.domain.StatusDenuncia;
import com.marketplace.desapegoUniversitario.dto.AnuncioResponseDTO;
import com.marketplace.desapegoUniversitario.dto.DenunciaResponseDTO;
import com.marketplace.desapegoUniversitario.dto.ModeracaoDTO;
import com.marketplace.desapegoUniversitario.dto.PaginadoDTO;
import com.marketplace.desapegoUniversitario.dto.ResolucaoDenunciaDTO;
import com.marketplace.desapegoUniversitario.service.AnuncioService;
import com.marketplace.desapegoUniversitario.service.DenunciaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "Administração", description = "Moderação de anúncios e denúncias (somente ADMIN)")
public class AdminController {

    private final AnuncioService anuncioService;
    private final DenunciaService denunciaService;

    public AdminController(AnuncioService anuncioService, DenunciaService denunciaService) {
        this.anuncioService = anuncioService;
        this.denunciaService = denunciaService;
    }

    @PatchMapping("/api/v1/admin/anuncios/{id}/moderacao")
    public AnuncioResponseDTO moderar(@PathVariable UUID id, @Valid @RequestBody ModeracaoDTO dto) {
        return anuncioService.moderar(id, dto.moderado());
    }

    @DeleteMapping("/api/v1/admin/anuncios/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        anuncioService.removerPorModeracao(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/admin/denuncias")
    public PaginadoDTO<DenunciaResponseDTO> listarDenuncias(
            @RequestParam(required = false) StatusDenuncia status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return denunciaService.listar(status, page, size);
    }

    @PostMapping("/api/v1/admin/denuncias/{id}/resolucao")
    public DenunciaResponseDTO resolver(@PathVariable UUID id,
                                        @Valid @RequestBody ResolucaoDenunciaDTO dto,
                                        Authentication authentication) {
        return denunciaService.resolver(id, dto, authentication.getName());
    }
}
