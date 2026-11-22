package com.marketplace.desapegoUniversitario;

import com.marketplace.desapegoUniversitario.domain.Reserva;
import com.marketplace.desapegoUniversitario.repository.ReservaRepository;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NegociacaoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Test
    void reservaConclusaoEAvaliacaoDevemFuncionar() throws Exception {
        String emailVendedor = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, emailVendedor);
        String vendedor = TestAutenticacao.login(mockMvc, emailVendedor);
        UUID vendedorId = usuarioRepository.findByEmail(emailVendedor).orElseThrow().getId();
        UUID anuncioId = TestAutenticacao.criarAnuncio(mockMvc, vendedor);

        String emailComprador = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, emailComprador);
        String comprador = TestAutenticacao.login(mockMvc, emailComprador);
        UUID compradorId = usuarioRepository.findByEmail(emailComprador).orElseThrow().getId();

        mockMvc.perform(post("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVADO"));

        mockMvc.perform(post("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk());

        String outroEmail = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, outroEmail);
        String outro = TestAutenticacao.login(mockMvc, outroEmail);
        mockMvc.perform(post("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + outro))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO"));

        mockMvc.perform(post("/api/v1/anuncios/{id}/conclusao", anuncioId)
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/anuncios/{id}/conclusao", anuncioId)
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VENDIDO"));

        mockMvc.perform(post("/api/v1/anuncios/{id}/avaliacoes", anuncioId)
                        .header("Authorization", "Bearer " + comprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\":5,\"comentario\":\"Ótimo vendedor\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nota").value(5))
                .andExpect(jsonPath("$.vendedorId").value(vendedorId.toString()));

        mockMvc.perform(post("/api/v1/anuncios/{id}/avaliacoes", anuncioId)
                        .header("Authorization", "Bearer " + comprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nota").value(2));

        mockMvc.perform(get("/api/v1/usuarios/{id}/avaliacoes", vendedorId)
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.media").value(2.0));

        mockMvc.perform(post("/api/v1/anuncios/{id}/avaliacoes", anuncioId)
                        .header("Authorization", "Bearer " + outro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\":5}"))
                .andExpect(status().isConflict());

        org.junit.jupiter.api.Assertions.assertNotNull(compradorId);
    }

    @Test
    void cancelamentoDeReservaDeveVoltarAnuncioParaAtivo() throws Exception {
        String vendedor = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID anuncioId = TestAutenticacao.criarAnuncio(mockMvc, vendedor);
        String comprador = TestAutenticacao.cadastrarELogar(mockMvc);

        mockMvc.perform(post("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk());

        String terceiro = TestAutenticacao.cadastrarELogar(mockMvc);
        mockMvc.perform(delete("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + terceiro))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"));

        mockMvc.perform(delete("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isConflict());
    }

    @Test
    void doacaoConcluidaDeveVirarDoadoEValidarDados() throws Exception {
        String emailVendedor = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, emailVendedor);
        String vendedor = TestAutenticacao.login(mockMvc, emailVendedor);
        String emailComprador = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, emailComprador);
        String comprador = TestAutenticacao.login(mockMvc, emailComprador);

        String corpo = mockMvc.perform(post("/api/v1/anuncios")
                        .header("Authorization", "Bearer " + vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Mochila\",\"preco\":0,\"categoria\":\"OUTROS\",\"tipo\":\"DOACAO\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        UUID anuncioId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(corpo, "$.id"));

        mockMvc.perform(post("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));

        mockMvc.perform(post("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/anuncios/{id}/conclusao", anuncioId)
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DOADO"));

        mockMvc.perform(post("/api/v1/anuncios/{id}/avaliacoes", anuncioId)
                        .header("Authorization", "Bearer " + comprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\":6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    @Test
    void negociacaoDeveExigirAutenticacaoEAnuncioExistente() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/anuncios/{id}/reserva", id))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/usuarios/{id}/avaliacoes", id))
                .andExpect(status().isUnauthorized());

        String token = TestAutenticacao.cadastrarELogar(mockMvc);
        mockMvc.perform(get("/api/v1/usuarios/{id}/avaliacoes", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/anuncios/{id}/reserva", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    void consultaDeReservaDeveSerRestritaAosParticipantes() throws Exception {
        String vendedor = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID anuncioId = TestAutenticacao.criarAnuncio(mockMvc, vendedor);
        String comprador = TestAutenticacao.cadastrarELogar(mockMvc);
        String terceiro = TestAutenticacao.cadastrarELogar(mockMvc);

        mockMvc.perform(get("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));

        mockMvc.perform(post("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anuncioId").value(anuncioId.toString()))
                .andExpect(jsonPath("$.status").value("ATIVA"))
                .andExpect(jsonPath("$.anuncioStatus").value("RESERVADO"));

        mockMvc.perform(get("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVA"));

        mockMvc.perform(get("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + terceiro))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    void historicoDeReservasDeveListarCanceladasEConcluidas() throws Exception {
        String vendedor = TestAutenticacao.cadastrarELogar(mockMvc);
        String compradorCancelou = TestAutenticacao.cadastrarELogar(mockMvc);
        String compradorConcluiu = TestAutenticacao.cadastrarELogar(mockMvc);
        String terceiro = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID anuncioCancelado = TestAutenticacao.criarAnuncio(mockMvc, vendedor, "Reserva cancelada");
        UUID anuncioConcluido = TestAutenticacao.criarAnuncio(mockMvc, vendedor, "Reserva concluída");

        mockMvc.perform(post("/api/v1/anuncios/{id}/reserva", anuncioCancelado)
                        .header("Authorization", "Bearer " + compradorCancelou))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/anuncios/{id}/reserva", anuncioCancelado)
                        .header("Authorization", "Bearer " + compradorCancelou))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/anuncios/{id}/reserva", anuncioConcluido)
                        .header("Authorization", "Bearer " + compradorConcluiu))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/anuncios/{id}/conclusao", anuncioConcluido)
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/usuarios/me/reservas")
                        .header("Authorization", "Bearer " + vendedor)
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.status == 'CANCELADA')]").exists())
                .andExpect(jsonPath("$.content[?(@.status == 'CONCLUIDA')]").exists());

        mockMvc.perform(get("/api/v1/usuarios/me/reservas")
                        .header("Authorization", "Bearer " + compradorCancelou))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("CANCELADA"));

        mockMvc.perform(get("/api/v1/usuarios/me/reservas")
                        .header("Authorization", "Bearer " + compradorConcluiu))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("CONCLUIDA"));

        mockMvc.perform(get("/api/v1/usuarios/me/reservas")
                        .header("Authorization", "Bearer " + terceiro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/v1/usuarios/me/reservas")
                        .header("Authorization", "Bearer " + vendedor)
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
    }

    @Test
    void reservaVencidaDeveLiberarAnuncioESerMarcadaComoExpirada() throws Exception {
        String vendedor = TestAutenticacao.cadastrarELogar(mockMvc);
        String primeiroComprador = TestAutenticacao.cadastrarELogar(mockMvc);
        String segundoComprador = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID anuncioId = TestAutenticacao.criarAnuncio(mockMvc, vendedor, "Reserva vencida");

        mockMvc.perform(post("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + primeiroComprador))
                .andExpect(status().isOk());

        Reserva reserva = reservaRepository
                .findByAnuncioIdAndCanceladaEmIsNullAndConcluidaEmIsNullAndExpiradaEmIsNull(anuncioId)
                .orElseThrow();
        reserva.setCriadoEm(Instant.now().minus(Duration.ofHours(25)));
        reservaRepository.save(reserva);

        mockMvc.perform(get("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRADA"))
                .andExpect(jsonPath("$.anuncioStatus").value("ATIVO"))
                .andExpect(jsonPath("$.expiradaEm").exists());

        mockMvc.perform(post("/api/v1/anuncios/{id}/mensagens", anuncioId)
                        .header("Authorization", "Bearer " + primeiroComprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conteudo\":\"Ainda está disponível?\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + segundoComprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVADO"));

        mockMvc.perform(get("/api/v1/usuarios/me/reservas")
                        .header("Authorization", "Bearer " + primeiroComprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.status == 'EXPIRADA')]").exists());
    }
}
