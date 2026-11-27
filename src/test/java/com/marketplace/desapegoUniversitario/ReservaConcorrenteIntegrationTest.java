package com.marketplace.desapegoUniversitario;

import com.marketplace.desapegoUniversitario.domain.Anuncio;
import com.marketplace.desapegoUniversitario.domain.Categoria;
import com.marketplace.desapegoUniversitario.domain.Condicao;
import com.marketplace.desapegoUniversitario.domain.Role;
import com.marketplace.desapegoUniversitario.domain.Reserva;
import com.marketplace.desapegoUniversitario.domain.StatusAnuncio;
import com.marketplace.desapegoUniversitario.domain.TipoAnuncio;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.exception.ConflitoException;
import com.marketplace.desapegoUniversitario.repository.AnuncioRepository;
import com.marketplace.desapegoUniversitario.repository.ReservaRepository;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import com.marketplace.desapegoUniversitario.service.NegociacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class ReservaConcorrenteIntegrationTest {

    @Autowired
    private NegociacaoService negociacaoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AnuncioRepository anuncioRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Test
    void duasReservasSimultaneasNaoDevemCriarDuasReservasAtivas() throws Exception {
        Usuario vendedor = salvarUsuario("vendedor");
        Usuario compradorUm = salvarUsuario("comprador-um");
        Usuario compradorDois = salvarUsuario("comprador-dois");
        Anuncio anuncio = salvarAnuncio(vendedor);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            List<Future<String>> tentativas = List.of(
                    executor.submit(() -> {
                        inicio.await();
                        negociacaoService.reservar(anuncio.getId(), compradorUm.getEmail());
                        return "reservado";
                    }),
                    executor.submit(() -> {
                        inicio.await();
                        negociacaoService.reservar(anuncio.getId(), compradorDois.getEmail());
                        return "reservado";
                    }));

            inicio.countDown();

            int sucessos = 0;
            int conflitos = 0;
            for (Future<String> tentativa : tentativas) {
                try {
                    tentativa.get(30, TimeUnit.SECONDS);
                    sucessos++;
                } catch (ExecutionException e) {
                    assertInstanceOf(ConflitoException.class, e.getCause());
                    conflitos++;
                }
            }

            assertEquals(1, sucessos);
            assertEquals(1, conflitos);
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, reservaRepository.findAll().stream()
                .filter(reserva -> anuncio.getId().equals(reserva.getAnuncio().getId()))
                .filter(reserva -> reserva.getCanceladaEm() == null
                        && reserva.getConcluidaEm() == null
                        && reserva.getExpiradaEm() == null)
                .count());
        assertEquals(StatusAnuncio.RESERVADO,
                anuncioRepository.findById(anuncio.getId()).orElseThrow().getStatus());
    }

    @Test
    void restricaoDoBancoImpedeSegundaReservaAtivaMesmoForaDoServico() {
        Usuario vendedor = salvarUsuario("vendedor");
        Usuario comprador = salvarUsuario("comprador");
        Anuncio anuncio = salvarAnuncio(vendedor);

        reservaRepository.saveAndFlush(new Reserva(
                null, anuncio, comprador, Instant.now(), null, null, null));

        assertThrows(DataIntegrityViolationException.class, () -> reservaRepository.saveAndFlush(
                new Reserva(null, anuncio, comprador, Instant.now(), null, null, null)));
    }

    private Usuario salvarUsuario(String prefixo) {
        return usuarioRepository.save(new Usuario(
                null,
                "Usuario " + prefixo,
                prefixo + "-" + UUID.randomUUID() + "@universidade.edu.br",
                "hash",
                Role.USUARIO,
                true,
                Instant.now()));
    }

    private Anuncio salvarAnuncio(Usuario vendedor) {
        Instant agora = Instant.now();
        return anuncioRepository.save(new Anuncio(
                null,
                "Bicicleta",
                BigDecimal.TEN,
                "Descrição",
                null,
                Categoria.LIVROS,
                "contato",
                vendedor,
                true,
                TipoAnuncio.VENDA,
                Condicao.USADO,
                StatusAnuncio.ATIVO,
                "Campus",
                "Biblioteca",
                agora,
                agora,
                null));
    }
}
