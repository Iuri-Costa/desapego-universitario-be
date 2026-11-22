package com.marketplace.desapegoUniversitario.controller;

import com.marketplace.desapegoUniversitario.domain.StatusAnuncio;
import com.marketplace.desapegoUniversitario.dto.AnuncioResponseDTO;
import com.marketplace.desapegoUniversitario.dto.AvaliacaoRequestDTO;
import com.marketplace.desapegoUniversitario.dto.AvaliacaoResponseDTO;
import com.marketplace.desapegoUniversitario.dto.AvaliacoesResponseDTO;
import com.marketplace.desapegoUniversitario.service.NegociacaoService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class NegociacaoController {

    private final NegociacaoService negociacaoService;

    public NegociacaoController(NegociacaoService negociacaoService) {
        this.negociacaoService = negociacaoService;
    }

    @PostMapping("/anuncios/{id}/reserva")
    public AnuncioResponseDTO reservar(@PathVariable UUID id, Authentication authentication) {
        return negociacaoService.reservar(id, authentication.getName());
    }

    @DeleteMapping("/anuncios/{id}/reserva")
    public AnuncioResponseDTO cancelarReserva(@PathVariable UUID id, Authentication authentication) {
        return negociacaoService.cancelarReserva(id, authentication.getName());
    }

    @PostMapping("/anuncios/{id}/conclusao")
    public AnuncioResponseDTO concluir(@PathVariable UUID id, Authentication authentication) {
        return negociacaoService.concluir(id, authentication.getName());
    }

    @PostMapping("/anuncios/{id}/avaliacoes")
    public AvaliacaoResponseDTO avaliar(@PathVariable UUID id,
                                        @Valid @RequestBody AvaliacaoRequestDTO dto,
                                        Authentication authentication) {
        return negociacaoService.avaliar(id, dto, authentication.getName());
    }

    @GetMapping("/usuarios/{id}/avaliacoes")
    public AvaliacoesResponseDTO listarAvaliacoes(@PathVariable UUID id) {
        return negociacaoService.listarAvaliacoes(id);
    }
}
