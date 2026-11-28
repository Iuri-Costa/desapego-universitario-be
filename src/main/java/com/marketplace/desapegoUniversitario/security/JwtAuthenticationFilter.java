package com.marketplace.desapegoUniversitario.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            autenticar(request, authorization.substring("Bearer ".length()));
        }
        filterChain.doFilter(request, response);
    }

    private void autenticar(HttpServletRequest request, String token) {
        try {
            Claims claims = jwtService.validar(token);
            if (!jwtService.isAccessToken(claims)) {
                return;
            }
            UserDetails usuario = userDetailsService.loadUserByUsername(claims.getSubject());
            if (!(usuario instanceof UsuarioPrincipal principal)
                    || !jwtService.versaoValida(claims, principal.getTokenVersion())) {
                SecurityContextHolder.clearContext();
                return;
            }
            UsernamePasswordAuthenticationToken autenticacao =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(autenticacao);
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            SecurityContextHolder.clearContext();
        }
    }
}
