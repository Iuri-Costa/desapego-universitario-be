package com.marketplace.desapegoUniversitario.repository;

import com.marketplace.desapegoUniversitario.domain.Anuncio;
import com.marketplace.desapegoUniversitario.dto.AnuncioFiltroDTO;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AnuncioSpecification {

    private AnuncioSpecification() {
    }

    public static Specification<Anuncio> de(AnuncioFiltroDTO filtro) {
        List<Specification<Anuncio>> especificacoes = new ArrayList<>();
        especificacoes.add((root, query, cb) -> cb.isNull(root.get("removidoEm")));

        if (filtro.texto() != null && !filtro.texto().isBlank()) {
            String padrao = padrao(filtro.texto());
            especificacoes.add((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("titulo")), padrao),
                    cb.like(cb.lower(root.get("descricao")), padrao)));
        }
        adicionarIgualdade(especificacoes, "categoria", filtro.categoria());
        adicionarIgualdade(especificacoes, "tipo", filtro.tipo());
        adicionarIgualdade(especificacoes, "condicao", filtro.condicao());
        adicionarIgualdade(especificacoes, "status", filtro.status());

        if (filtro.campus() != null && !filtro.campus().isBlank()) {
            String padrao = padrao(filtro.campus());
            especificacoes.add((root, query, cb) -> cb.like(cb.lower(root.get("campus")), padrao));
        }
        if (filtro.precoMin() != null) {
            especificacoes.add((root, query, cb) -> cb.ge(root.get("preco"), filtro.precoMin()));
        }
        if (filtro.precoMax() != null) {
            especificacoes.add((root, query, cb) -> cb.le(root.get("preco"), filtro.precoMax()));
        }
        return Specification.allOf(especificacoes);
    }

    private static String padrao(String valor) {
        return "%" + valor.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private static void adicionarIgualdade(List<Specification<Anuncio>> especificacoes,
                                           String campo, Object valor) {
        if (valor != null) {
            especificacoes.add((root, query, cb) -> cb.equal(root.get(campo), valor));
        }
    }
}
