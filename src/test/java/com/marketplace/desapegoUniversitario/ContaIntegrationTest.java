package com.marketplace.desapegoUniversitario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void limparEmails() {
        CapturingEmailSender.limpar();
    }

    @Test
    void cadastroDeveRejeitarSenhaFraca() throws Exception {
        String email = TestAutenticacao.novoEmail();
        mockMvc.perform(post("/api/v1/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Aluno","email":"%s","senha":"senha123"}
                                """.formatted(email)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.erros[*].campo").value("senha"));
    }

    @Test
    void loginSoDeveFuncionarAposVerificacaoDoEmail() throws Exception {
        String email = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrarSemVerificar(mockMvc, email);

        mockMvc.perform(post("/api/v1/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(email, TestAutenticacao.SENHA)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"));

        TestAutenticacao.verificarEmail(mockMvc, email);

        mockMvc.perform(post("/api/v1/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(email, TestAutenticacao.SENHA)))
                .andExpect(status().isOk());
    }

    @Test
    void tokenDeVerificacaoNaoDeveSerReutilizado() throws Exception {
        String email = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrarSemVerificar(mockMvc, email);
        String token = CapturingEmailSender.ultimoTokenPara(email);

        TestAutenticacao.verificarEmail(mockMvc, email);

        mockMvc.perform(post("/api/v1/usuarios/verificar-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s"}
                                """.formatted(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
    }

    @Test
    void recuperacaoDeSenhaDeveSerGenericaParaEmailInexistente() throws Exception {
        String email = TestAutenticacao.novoEmail();

        mockMvc.perform(post("/api/v1/usuarios/recuperar-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(status().isAccepted());
        org.junit.jupiter.api.Assertions.assertEquals(0, CapturingEmailSender.contarPara(email));

        String existente = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, existente);
        org.junit.jupiter.api.Assertions.assertEquals(1, CapturingEmailSender.contarPara(existente));

        mockMvc.perform(post("/api/v1/usuarios/recuperar-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(existente)))
                .andExpect(status().isAccepted());
        org.junit.jupiter.api.Assertions.assertEquals(2, CapturingEmailSender.contarPara(existente));
    }

    @Test
    void redefinicaoDeSenhaDeveAlterarSenhaERevogarSessoes() throws Exception {
        String email = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, email);
        String refresh = TestAutenticacao.refreshToken(mockMvc, email);
        String accessAntigo = TestAutenticacao.login(mockMvc, email);

        mockMvc.perform(post("/api/v1/usuarios/recuperar-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(status().isAccepted());
        String token = CapturingEmailSender.ultimoTokenPara(email);

        mockMvc.perform(post("/api/v1/usuarios/redefinir-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","novaSenha":"Nova@Senha123"}
                                """.formatted(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/usuarios/me")
                        .header("Authorization", "Bearer " + accessAntigo))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(email, TestAutenticacao.SENHA)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"Nova@Senha123"}
                                """.formatted(email)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void redefinicaoDeveRejeitarSenhaFracaETokenInvalido() throws Exception {
        String email = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, email);
        mockMvc.perform(post("/api/v1/usuarios/recuperar-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)));
        String token = CapturingEmailSender.ultimoTokenPara(email);

        mockMvc.perform(post("/api/v1/usuarios/redefinir-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","novaSenha":"12345678"}
                                """.formatted(token)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/usuarios/redefinir-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"token-inexistente","novaSenha":"Outra@Senha123"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
