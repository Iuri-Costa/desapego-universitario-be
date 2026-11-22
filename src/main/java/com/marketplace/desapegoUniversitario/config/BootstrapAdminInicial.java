package com.marketplace.desapegoUniversitario.config;

import com.marketplace.desapegoUniversitario.domain.Role;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdminInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInicial.class);

    private final UsuarioRepository usuarioRepository;
    private final String email;

    public BootstrapAdminInicial(UsuarioRepository usuarioRepository,
                                 @Value("${app.admin.bootstrap.email:}") String email) {
        this.usuarioRepository = usuarioRepository;
        this.email = email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isEmpty()) {
            return;
        }
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        if (usuario == null) {
            log.warn("ADMIN_BOOTSTRAP_EMAIL não corresponde a uma conta cadastrada; nada foi promovido");
            return;
        }
        if (!usuario.isEmailVerificado()) {
            log.warn("ADMIN_BOOTSTRAP_EMAIL corresponde a uma conta não verificada; nada foi promovido");
            return;
        }
        if (usuario.getRole() == Role.ADMIN) {
            return;
        }
        usuario.setRole(Role.ADMIN);
        usuarioRepository.save(usuario);
        log.info("Conta indicada por ADMIN_BOOTSTRAP_EMAIL promovida a ADMIN na inicialização");
    }
}
