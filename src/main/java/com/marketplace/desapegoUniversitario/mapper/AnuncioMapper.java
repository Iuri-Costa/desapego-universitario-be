package com.marketplace.desapegoUniversitario.mapper;

import com.marketplace.desapegoUniversitario.domain.Anuncio;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.dto.AnuncioRequestDTO;
import com.marketplace.desapegoUniversitario.dto.AnuncioResponseDTO;

import java.util.List;

public final class AnuncioMapper {

    private AnuncioMapper() {
    }

    public static Anuncio toEntity(AnuncioRequestDTO dto, Usuario usuario) {
        Anuncio anuncio = new Anuncio();
        anuncio.setTitulo(dto.titulo());
        anuncio.setPreco(dto.preco());
        anuncio.setDescricao(dto.descricao());
        anuncio.setImagem(dto.imagem());
        anuncio.setCategoria(dto.categoria());
        anuncio.setContato(dto.contato());
        anuncio.setUsuario(usuario);
        anuncio.setModerado(false);
        return anuncio;
    }

    public static AnuncioResponseDTO toResponse(Anuncio anuncio) {
        return new AnuncioResponseDTO(
                anuncio.getId(),
                anuncio.getTitulo(),
                anuncio.getPreco(),
                anuncio.getDescricao(),
                anuncio.getImagem(),
                anuncio.getCategoria(),
                anuncio.getContato(),
                anuncio.isModerado()
        );
    }

    public static List<AnuncioResponseDTO> toResponseList(List<Anuncio> anuncios) {
        return anuncios.stream()
                .map(AnuncioMapper::toResponse)
                .toList();
    }
}
