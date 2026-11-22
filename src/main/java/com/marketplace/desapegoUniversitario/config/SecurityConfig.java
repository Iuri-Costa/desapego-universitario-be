package com.marketplace.desapegoUniversitario.config;

import com.marketplace.desapegoUniversitario.security.JwtAuthenticationFilter;
import com.marketplace.desapegoUniversitario.security.RestAccessDeniedHandler;
import com.marketplace.desapegoUniversitario.security.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler,
                                                   CorsConfigurationSource corsConfigurationSource,
                                                   @Value("${app.security.hsts-enabled:false}") boolean hstsEnabled)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .headers(headers -> {
                    headers.frameOptions(frame -> frame.deny());
                    headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                            "default-src 'none'; frame-ancestors 'none'; object-src 'none'; base-uri 'none'"));
                    headers.referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER));
                    headers.permissionsPolicy(policy -> policy.policy(
                            "camera=(), microphone=(), geolocation=(), payment=()"));
                    if (!hstsEnabled) {
                        headers.httpStrictTransportSecurity(hsts -> hsts.disable());
                    }
                })
                .exceptionHandling(excecoes -> excecoes
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(regras -> regras
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/usuarios/cadastro", "/api/v1/usuarios/login",
                                "/api/v1/usuarios/verificar-email", "/api/v1/usuarios/reenviar-verificacao",
                                "/api/v1/usuarios/recuperar-senha", "/api/v1/usuarios/redefinir-senha",
                                "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/anuncios", "/api/v1/anuncios/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
}
