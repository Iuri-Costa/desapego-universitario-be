package com.marketplace.desapegoUniversitario.service;

import com.marketplace.desapegoUniversitario.domain.Categoria;
import com.marketplace.desapegoUniversitario.dto.AnuncioRequestDTO;
import com.marketplace.desapegoUniversitario.dto.AnuncioResponseDTO;
import com.marketplace.desapegoUniversitario.mapper.AnuncioMapper;
import com.marketplace.desapegoUniversitario.repository.AnuncioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AnuncioService {

    private final AnuncioRepository anuncioRepository;

    public AnuncioService(AnuncioRepository anuncioRepository) {
        this.anuncioRepository = anuncioRepository;
    }

    @Transactional
    public AnuncioResponseDTO criar(AnuncioRequestDTO dto) {
        return AnuncioMapper.toResponse(anuncioRepository.save(AnuncioMapper.toEntity(dto)));
    }

    @Transactional(readOnly = true)
    public List<AnuncioResponseDTO> listarTodos() {
        return AnuncioMapper.toResponseList(anuncioRepository.findAll());
    }

    @Transactional
    public void deletarPorID(UUID id) {
        anuncioRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<AnuncioResponseDTO> listarPorCategoria(Categoria categoria) {
        return AnuncioMapper.toResponseList(anuncioRepository.findByCategoria(categoria));
    }
}
