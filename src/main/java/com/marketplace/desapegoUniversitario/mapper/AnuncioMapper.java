package com.marketplace.desapegoUniversitario.mapper;

import com.marketplace.desapegoUniversitario.domain.Anuncio;
import com.marketplace.desapegoUniversitario.domain.Categoria;
import com.marketplace.desapegoUniversitario.domain.Condicao;
import com.marketplace.desapegoUniversitario.domain.StatusAnuncio;
import com.marketplace.desapegoUniversitario.domain.TipoAnuncio;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.dto.AnuncioPatchDTO;
import com.marketplace.desapegoUniversitario.dto.AnuncioRequestDTO;
import com.marketplace.desapegoUniversitario.dto.AnuncioResponseDTO;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

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
        anuncio.setCampus(textoOuPadrao(dto.campus(), CAMPUS_PADRAO));
        anuncio.setLocalRetirada(textoOuPadrao(dto.localRetirada(), LOCAL_PADRAO));
    }

    public static void aplicarCampos(Anuncio anuncio, AnuncioPatchDTO dto) {
        aplicarSePresente(dto.titulo(), anuncio::setTitulo);
        aplicarSePresente(dto.preco(), anuncio::setPreco);
        aplicarSePresente(dto.descricao(), anuncio::setDescricao);
        aplicarSePresente(dto.imagem(), anuncio::setImagem);
        aplicarSePresente(dto.categoria(), anuncio::setCategoria);
        aplicarSePresente(dto.contato(), anuncio::setContato);
        aplicarSePresente(dto.tipo(), anuncio::setTipo);
        aplicarSePresente(dto.condicao(), anuncio::setCondicao);
        aplicarSePresente(dto.campus(), valor -> anuncio.setCampus(textoOuPadrao(valor, CAMPUS_PADRAO)));
        aplicarSePresente(dto.localRetirada(),
                valor -> anuncio.setLocalRetirada(textoOuPadrao(valor, LOCAL_PADRAO)));
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

    private static <T> void aplicarSePresente(Optional<T> valor, Consumer<T> consumidor) {
        if (valor != null && valor.isPresent()) {
            consumidor.accept(valor.get());
        }
    }

    private static String textoOuPadrao(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : valor.trim();
    }
}
