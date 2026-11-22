package com.marketplace.desapegoUniversitario.dto;

import com.marketplace.desapegoUniversitario.domain.StatusDenuncia;
import jakarta.validation.constraints.NotNull;

public record ResolucaoDenunciaDTO(
        @NotNull(message = "Informe a decisão") StatusDenuncia status,
        Boolean removerAnuncio
) {
    public boolean deveRemoverAnuncio() {
        return Boolean.TRUE.equals(removerAnuncio);
    }
}
