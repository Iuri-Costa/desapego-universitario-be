package com.marketplace.desapegoUniversitario;

import com.jayway.jsonpath.JsonPath;
import com.marketplace.desapegoUniversitario.domain.Role;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class TestAutenticacao {

    public static final String SENHA = "Senha@12345";

    private TestAutenticacao() {
    }

    public static String novoEmail() {
        return "aluno-" + UUID.randomUUID() + "@universidade.edu.br";
    }

    public static void cadastrar(MockMvc mockMvc, String email) throws Exception {
        mockMvc.perform(post("/api/v1/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Aluno Teste","email":"%s","senha":"%s"}
                                """.formatted(email, SENHA)))
                .andExpect(status().isOk());
    }

    public static String login(MockMvc mockMvc, String email) throws Exception {
        String corpo = mockMvc.perform(post("/api/v1/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(email, SENHA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.accessToken");
    }

    public static String refreshToken(MockMvc mockMvc, String email) throws Exception {
        String corpo = mockMvc.perform(post("/api/v1/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(email, SENHA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.refreshToken");
    }

    public static String cadastrarELogar(MockMvc mockMvc) throws Exception {
        String email = novoEmail();
        cadastrar(mockMvc, email);
        return login(mockMvc, email);
    }

    public static String cadastrarAdminELogar(MockMvc mockMvc, UsuarioRepository usuarioRepository) throws Exception {
        String email = novoEmail();
        cadastrar(mockMvc, email);
        var usuario = usuarioRepository.findByEmail(email).orElseThrow();
        usuario.setRole(Role.ADMIN);
        usuarioRepository.save(usuario);
        return login(mockMvc, email);
    }

    public static UUID criarAnuncio(MockMvc mockMvc, String token) throws Exception {
        String corpo = mockMvc.perform(post("/api/v1/anuncios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Livro","preco":10.00,"categoria":"LIVROS"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(corpo, "$.id"));
    }
}
