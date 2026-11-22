package com.marketplace.desapegoUniversitario.controller;

import com.marketplace.desapegoUniversitario.dto.MensagemRequestDTO;
import com.marketplace.desapegoUniversitario.dto.MensagemResponseDTO;
import com.marketplace.desapegoUniversitario.service.MensagemService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Mensagens", description = "Conversas por anúncio entre comprador e vendedor")
public class MensagemController {

    private final MensagemService mensagemService;

    public MensagemController(MensagemService mensagemService) {
        this.mensagemService = mensagemService;
    }

    @PostMapping("/anuncios/{id}/mensagens")
    @ResponseStatus(HttpStatus.CREATED)
    public MensagemResponseDTO enviar(@PathVariable UUID id,
                                      @Valid @RequestBody MensagemRequestDTO dto,
                                      Authentication authentication) {
        return mensagemService.enviar(id, dto, authentication.getName());
    }

    @GetMapping("/anuncios/{id}/mensagens")
    public List<MensagemResponseDTO> listar(@PathVariable UUID id,
                                            @RequestParam(required = false) UUID compradorId,
                                            Authentication authentication) {
        return mensagemService.listar(id, compradorId, authentication.getName());
    }

    @PatchMapping("/mensagens/{id}/lida")
    public MensagemResponseDTO marcarComoLida(@PathVariable UUID id, Authentication authentication) {
        return mensagemService.marcarComoLida(id, authentication.getName());
    }
}
