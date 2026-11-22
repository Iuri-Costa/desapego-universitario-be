package com.marketplace.desapegoUniversitario.mapper;

import com.marketplace.desapegoUniversitario.domain.Anuncio;
import com.marketplace.desapegoUniversitario.domain.Condicao;
import com.marketplace.desapegoUniversitario.domain.StatusAnuncio;
import com.marketplace.desapegoUniversitario.domain.TipoAnuncio;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.dto.AnuncioRequestDTO;
import com.marketplace.desapegoUniversitario.dto.AnuncioResponseDTO;

import java.time.Instant;
import java.util.List;

public final class AnuncioMapper {

    public static final String CAMPUS_PADRAO = "PRINCIPAL";
    public static final String LOCAL_PADRAO = "A combinar";

    private AnuncioMapper() {
    }

    public static Anuncio toEntity(AnuncioRequestDTO dto, Usuario usuario) {
        Anuncio anuncio = new Anuncio();
        aplicarCampos(anuncio, dto);
        anuncio.setUsuario(usuario);
        anuncio.setStatus(StatusAnuncio.ATIVO);
        Instant agora = Instant.now();
        anuncio.setCriadoEm(agora);
        anuncio.setAtualizadoEm(agora);
        return anuncio;
    }

    public static void aplicarCampos(Anuncio anuncio, AnuncioRequestDTO dto) {
        anuncio.setTitulo(dto.titulo());
        anuncio.setPreco(dto.preco());
        anuncio.setDescricao(dto.descricao());
        anuncio.setImagem(dto.imagem());
        anuncio.setCategoria(dto.categoria());
        anuncio.setContato(dto.contato());
        anuncio.setTipo(dto.tipo() == null ? TipoAnuncio.VENDA : dto.tipo());
        anuncio.setCondicao(dto.condicao() == null ? Condicao.USADO : dto.condicao());
        anuncio.setCampus(dto.campus() == null || dto.campus().isBlank() ? CAMPUS_PADRAO : dto.campus().trim());
        anuncio.setLocalRetirada(dto.localRetirada() == null || dto.localRetirada().isBlank()
                ? LOCAL_PADRAO : dto.localRetirada().trim());
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
                anuncio.isModerado(),
                anuncio.getTipo(),
                anuncio.getCondicao(),
                anuncio.getStatus(),
                anuncio.getCampus(),
                anuncio.getLocalRetirada(),
                anuncio.getCriadoEm(),
                anuncio.getAtualizadoEm()
        );
    }

    public static List<AnuncioResponseDTO> toResponseList(List<Anuncio> anuncios) {
        return anuncios.stream()
                .map(AnuncioMapper::toResponse)
                .toList();
    }
}
