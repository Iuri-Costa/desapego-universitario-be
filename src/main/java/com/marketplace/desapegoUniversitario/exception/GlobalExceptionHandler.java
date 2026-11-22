package com.marketplace.desapegoUniversitario.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException ex,
                                                        HttpServletRequest request) {
        List<ErroCampo> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroCampo(erro.getField(), erro.getDefaultMessage()))
                .sorted(Comparator.comparing(ErroCampo::campo))
                .toList();

        return resposta(HttpStatus.BAD_REQUEST, "VALIDACAO", "Erro de validação", request, erros);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarCorpoInvalido(HttpMessageNotReadableException ex,
                                                           HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA",
                "Corpo da requisição inválido ou mal formatado", request, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tratarParametroInvalido(MethodArgumentTypeMismatchException ex,
                                                                HttpServletRequest request) {
        String mensagem = "O parâmetro '%s' possui um valor inválido".formatted(ex.getName());
        return resposta(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO", mensagem, request, List.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResponse> tratarParametroAusente(MissingServletRequestParameterException ex,
                                                               HttpServletRequest request) {
        String mensagem = "O parâmetro '%s' é obrigatório".formatted(ex.getParameterName());
        return resposta(HttpStatus.BAD_REQUEST, "PARAMETRO_OBRIGATORIO", mensagem, request, List.of());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratarNaoEncontrado(RecursoNaoEncontradoException ex,
                                                            HttpServletRequest request) {
        return resposta(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> tratarRotaNaoEncontrada(NoResourceFoundException ex,
                                                                HttpServletRequest request) {
        return resposta(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Recurso não encontrado", request, List.of());
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResponse> tratarCredenciaisInvalidas(CredenciaisInvalidasException ex,
                                                                   HttpServletRequest request) {
        return resposta(HttpStatus.UNAUTHORIZED, "CREDENCIAIS_INVALIDAS", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(TokenInvalidoException.class)
    public ResponseEntity<ErroResponse> tratarTokenInvalido(TokenInvalidoException ex,
                                                            HttpServletRequest request) {
        return resposta(HttpStatus.UNAUTHORIZED, "TOKEN_INVALIDO", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResponse> tratarNaoAutenticado(AuthenticationException ex,
                                                             HttpServletRequest request) {
        return resposta(HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO",
                "Autenticação necessária para acessar este recurso", request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> tratarAcessoNegado(AccessDeniedException ex,
                                                           HttpServletRequest request) {
        return resposta(HttpStatus.FORBIDDEN, "ACESSO_NEGADO",
                "Você não tem permissão para acessar este recurso", request, List.of());
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroResponse> tratarConflito(ConflitoException ex,
                                                       HttpServletRequest request) {
        return resposta(HttpStatus.CONFLICT, "CONFLITO", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> tratarIntegridade(DataIntegrityViolationException ex,
                                                          HttpServletRequest request) {
        log.warn("Violação de integridade ao processar {}", request.getRequestURI());
        return resposta(HttpStatus.CONFLICT, "CONFLITO_DE_DADOS",
                "Não foi possível concluir a operação por conflito de dados", request, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> tratarMetodoNaoSuportado(HttpRequestMethodNotSupportedException ex,
                                                                 HttpServletRequest request) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "METODO_NAO_SUPORTADO",
                "Método HTTP não suportado para este recurso", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarErroInesperado(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado (tipo={}) ao processar {}", ex.getClass().getSimpleName(),
                request.getRequestURI());
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO",
                "Erro interno no servidor", request, List.of());
    }

    private ResponseEntity<ErroResponse> resposta(HttpStatus status, String codigo, String mensagem,
                                                  HttpServletRequest request, List<ErroCampo> erros) {
        ErroResponse corpo = new ErroResponse(
                OffsetDateTime.now(),
                status.value(),
                codigo,
                mensagem,
                request.getRequestURI(),
                erros
        );
        return ResponseEntity.status(status).body(corpo);
    }
}
