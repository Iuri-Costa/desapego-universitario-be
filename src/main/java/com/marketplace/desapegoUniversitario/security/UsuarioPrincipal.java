package com.marketplace.desapegoUniversitario.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/** Principal com a versao do token necessaria para validar access tokens revogados. */
public class UsuarioPrincipal implements UserDetails {

    private final String email;
    private final String senha;
    private final Collection<? extends GrantedAuthority> authorities;
    private final int tokenVersion;

    public UsuarioPrincipal(String email, String senha,
                            Collection<? extends GrantedAuthority> authorities, int tokenVersion) {
        this.email = email;
        this.senha = senha;
        this.authorities = authorities;
        this.tokenVersion = tokenVersion;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
