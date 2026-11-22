package com.marketplace.desapegoUniversitario.controller;

import com.marketplace.desapegoUniversitario.dto.DenunciaRequestDTO;
import com.marketplace.desapegoUniversitario.dto.DenunciaResponseDTO;
import com.marketplace.desapegoUniversitario.dto.PaginadoDTO;
import com.marketplace.desapegoUniversitario.service.DenunciaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Denúncias", description = "Registro e acompanhamento de denúncias")
public class DenunciaController {

    private final DenunciaService denunciaService;

    public DenunciaController(DenunciaService denunciaService) {
        this.denunciaService = denunciaService;
    }

    @PostMapping("/anuncios/{id}/denuncias")
    @ResponseStatus(HttpStatus.CREATED)
    public DenunciaResponseDTO criar(@PathVariable UUID id,
                                     @Valid @RequestBody DenunciaRequestDTO dto,
                                     Authentication authentication) {
        return denunciaService.criar(id, dto, authentication.getName());
    }

    @GetMapping("/usuarios/me/denuncias")
    public PaginadoDTO<DenunciaResponseDTO> listarMinhas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return denunciaService.listarMinhas(authentication.getName(), page, size);
    }

    @GetMapping("/usuarios/me/denuncias/{id}")
    public DenunciaResponseDTO buscarMinha(@PathVariable UUID id, Authentication authentication) {
        return denunciaService.buscarMinha(id, authentication.getName());
    }
}
