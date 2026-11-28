package com.marketplace.desapegoUniversitario.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean emailVerificado;

    @Column(nullable = false)
    private Instant criadoEm;

    @Column(length = 255)
    private String contato;

    @Column(length = 1000)
    private String avatar;

    @Column(nullable = false)
    private int tokenVersion;

    public Usuario(UUID id, String nome, String email, String senha, Role role,
                   boolean emailVerificado, Instant criadoEm) {
        this(id, nome, email, senha, role, emailVerificado, criadoEm, null, null, 0);
    }
}
