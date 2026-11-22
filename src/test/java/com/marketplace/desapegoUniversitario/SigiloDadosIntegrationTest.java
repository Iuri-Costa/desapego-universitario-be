package com.marketplace.desapegoUniversitario;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.jayway.jsonpath.JsonPath;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SigiloDadosIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void capturarLogs() {
        logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        CapturingEmailSender.limpar();
    }

    @AfterEach
    void soltarAppender() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void respostasELogsNaoDevemExporSenhaHashOuTokens() throws Exception {
        String email = TestAutenticacao.novoEmail();
        String corpoCadastro = mockMvc.perform(post("/api/v1/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Aluno","email":"%s","senha":"%s"}
                                """.formatted(email, TestAutenticacao.SENHA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String tokenVerificacao = CapturingEmailSender.ultimoTokenPara(email);
        TestAutenticacao.verificarEmail(mockMvc, email);

        String corpoLogin = mockMvc.perform(post("/api/v1/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(email, TestAutenticacao.SENHA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(corpoLogin, "$.accessToken");
        String refreshToken = JsonPath.read(corpoLogin, "$.refreshToken");
        String hash = usuarioRepository.findByEmail(email).orElseThrow().getSenha();
        String tokenInvalido = accessToken.substring(0, accessToken.length() - 3) + "xxx";

        String corpoErro = mockMvc.perform(post("/api/v1/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"SenhaErrada@123"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/v1/usuarios/nao-existe")
                        .header("Authorization", "Bearer " + tokenInvalido))
                .andExpect(status().isUnauthorized());

        for (String resposta : new String[]{corpoCadastro, corpoLogin, corpoErro}) {
            org.junit.jupiter.api.Assertions.assertFalse(resposta.contains(TestAutenticacao.SENHA));
            org.junit.jupiter.api.Assertions.assertFalse(resposta.contains("SenhaErrada@123"));
            org.junit.jupiter.api.Assertions.assertFalse(resposta.contains(hash));
            org.junit.jupiter.api.Assertions.assertFalse(resposta.contains("$2a$"));
        }
        org.junit.jupiter.api.Assertions.assertFalse(corpoCadastro.contains("accessToken"));
        org.junit.jupiter.api.Assertions.assertFalse(corpoCadastro.contains("refreshToken"));
        org.junit.jupiter.api.Assertions.assertFalse(corpoCadastro.contains("senha"));

        for (ILoggingEvent evento : appender.list) {
            String mensagem = evento.getFormattedMessage();
            String lancavel = evento.getThrowableProxy() == null
                    ? "" : evento.getThrowableProxy().getMessage();
            for (String secreto : new String[]{
                    TestAutenticacao.SENHA, "SenhaErrada@123", tokenVerificacao,
                    accessToken, refreshToken, tokenInvalido, hash, email}) {
                org.junit.jupiter.api.Assertions.assertFalse(mensagem.contains(secreto),
                        "Log vazou dado sensível: " + evento.getLoggerName());
                org.junit.jupiter.api.Assertions.assertFalse(lancavel.contains(secreto),
                        "Stack trace vazou dado sensível: " + evento.getLoggerName());
            }
        }
    }
}
