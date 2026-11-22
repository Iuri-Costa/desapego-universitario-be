package com.marketplace.desapegoUniversitario.controller;

import com.marketplace.desapegoUniversitario.dto.DispositivoFCMRequestDTO;
import com.marketplace.desapegoUniversitario.dto.NotificacaoResponseDTO;
import com.marketplace.desapegoUniversitario.dto.PaginadoDTO;
import com.marketplace.desapegoUniversitario.service.NotificacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notificacoes")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @GetMapping
    public PaginadoDTO<NotificacaoResponseDTO> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return notificacaoService.listar(authentication.getName(), page, size);
    }

    @GetMapping("/nao-lidas")
    public Map<String, Long> contarNaoLidas(Authentication authentication) {
        return Map.of("total", notificacaoService.contarNaoLidas(authentication.getName()));
    }

    @PatchMapping("/{id}/lida")
    public NotificacaoResponseDTO marcarComoLida(@PathVariable UUID id, Authentication authentication) {
        return notificacaoService.marcarComoLida(id, authentication.getName());
    }

    @PostMapping("/dispositivo")
    @ResponseStatus(HttpStatus.CREATED)
    public void registrarDispositivo(@Valid @RequestBody DispositivoFCMRequestDTO dto,
                                     Authentication authentication) {
        notificacaoService.registrarDispositivo(dto.token(), authentication.getName());
    }

    @DeleteMapping("/dispositivo")
    public ResponseEntity<Void> removerDispositivo(@Valid @RequestBody DispositivoFCMRequestDTO dto,
                                                   Authentication authentication) {
        notificacaoService.removerDispositivo(dto.token(), authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
