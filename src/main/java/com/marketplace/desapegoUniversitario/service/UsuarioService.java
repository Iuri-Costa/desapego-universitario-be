package com.marketplace.desapegoUniversitario.service;

import com.marketplace.desapegoUniversitario.domain.TipoToken;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.dto.CadastroDTO;
import com.marketplace.desapegoUniversitario.dto.RedefinirSenhaDTO;
import com.marketplace.desapegoUniversitario.dto.UsuarioResponseDTO;
import com.marketplace.desapegoUniversitario.exception.ConflitoException;
import com.marketplace.desapegoUniversitario.exception.RecursoNaoEncontradoException;
import com.marketplace.desapegoUniversitario.mapper.UsuarioMapper;
import com.marketplace.desapegoUniversitario.repository.RefreshTokenRepository;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final UsuarioTokenService usuarioTokenService;
    private final String frontendUrl;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RefreshTokenRepository refreshTokenRepository,
                          PasswordEncoder passwordEncoder,
                          EmailSender emailSender,
                          UsuarioTokenService usuarioTokenService,
                          @Value("${app.frontend.url}") String frontendUrl) {
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.usuarioTokenService = usuarioTokenService;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public UsuarioResponseDTO cadastrar(CadastroDTO dto) {
        String email = normalizar(dto.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflitoException("Já existe um usuário cadastrado com este e-mail");
        }

        Usuario usuario = UsuarioMapper.toEntity(dto, passwordEncoder.encode(dto.senha()));
        usuario.setEmail(email);
        usuarioRepository.save(usuario);
        enviarVerificacao(usuario);
        return UsuarioMapper.toResponse(usuario);
    }

    @Transactional
    public void verificarEmail(String token) {
        Usuario usuario = usuarioTokenService.consumir(token, TipoToken.VERIFICACAO_EMAIL);
        usuario.setEmailVerificado(true);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void reenviarVerificacao(String email) {
        usuarioRepository.findByEmail(normalizar(email))
                .filter(usuario -> !usuario.isEmailVerificado())
                .ifPresent(this::enviarVerificacao);
    }

    @Transactional
    public void recuperarSenha(String email) {
        usuarioRepository.findByEmail(normalizar(email)).ifPresent(usuario -> {
            String token = usuarioTokenService.criar(usuario, TipoToken.REDEFINICAO_SENHA);
            String link = frontendUrl + "/redefinir-senha?token=" + token;
            emailSender.enviar(usuario.getEmail(), "Recuperação de senha",
                    "Use o link a seguir para redefinir sua senha: " + link);
        });
    }

    @Transactional
    public void redefinirSenha(RedefinirSenhaDTO dto) {
        Usuario usuario = usuarioTokenService.consumir(dto.token(), TipoToken.REDEFINICAO_SENHA);
        usuario.setSenha(passwordEncoder.encode(dto.novaSenha()));
        usuarioRepository.save(usuario);

        var sessoes = refreshTokenRepository.findByUsuarioAndRevogadoEmIsNull(usuario);
        sessoes.forEach(sessao -> sessao.setRevogadoEm(java.time.Instant.now()));
        refreshTokenRepository.saveAll(sessoes);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioMapper::toResponse)
                .toList();
    }

    @Transactional
    public void deletarPorID(UUID id) {
        if (!usuarioRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Usuário não encontrado");
        }
        usuarioRepository.deleteById(id);
    }

    private void enviarVerificacao(Usuario usuario) {
        String token = usuarioTokenService.criar(usuario, TipoToken.VERIFICACAO_EMAIL);
        String link = frontendUrl + "/verificar-email?token=" + token;
        emailSender.enviar(usuario.getEmail(), "Verificação de e-mail",
                "Use o link a seguir para verificar seu e-mail: " + link);
    }

    public static String normalizar(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
