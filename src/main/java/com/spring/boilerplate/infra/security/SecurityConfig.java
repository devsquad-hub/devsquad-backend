package com.spring.boilerplate.infra.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Configuração central de segurança — Supabase Resource Server (OAuth2).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(RateLimitFilter rateLimitFilter) {
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // --- Sessão Stateless ---
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // --- CSRF desabilitado (API-only, stateless) ---
                .csrf(AbstractHttpConfigurer::disable)

                // --- CORS (restritivo por padrão) ---
                .cors(AbstractHttpConfigurer::disable)

                // --- Regras de Autorização ---
                .authorizeHttpRequests(auth -> auth
                        // Endpoints Públicos
                        .requestMatchers("/api/v1/health").permitAll()
                        // Actuator — Requer role 'admin' (mapeada do Supabase)
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // Todo o resto requer autenticação
                        .anyRequest().authenticated()
                )

                // --- Servidor de Recursos OAuth2 ---
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )

                // --- Headers de Segurança Reforçados ---
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .contentTypeOptions(contentType -> {})
                        .xssProtection(xss ->
                                xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                        .contentSecurityPolicy(csp ->
                                csp.policyDirectives("default-src 'self'; frame-ancestors 'none';"))
                )

                // --- Filtro de Rate Limiting ---
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new SupabaseRoleConverter());
        return converter;
    }

    /**
     * Extrai as roles do JWT do Supabase.
     * Verifica 'app_metadata' ou 'user_metadata' por uma lista de roles,
     * ou usa como padrão a claim 'role' (ex: 'authenticated', 'service_role').
     */
    static class SupabaseRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            // 1. Tenta pegar roles customizadas de app_metadata (se configurado)
            Map<String, Object> appMetadata = jwt.getClaim("app_metadata");
            if (appMetadata != null && appMetadata.containsKey("roles")) {
                Object rolesObj = appMetadata.get("roles");
                if (rolesObj instanceof List<?>) {
                    return ((List<?>) rolesObj).stream()
                            .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toString().toUpperCase()))
                            .collect(Collectors.toList());
                }
            }

            // 2. Fallback: Mapeia a claim 'role' (Supabase)
            // 'service_role' -> ROLE_ADMIN
            // 'authenticated' -> ROLE_USER
            String supabaseRole = jwt.getClaim("role");
            if ("service_role".equals(supabaseRole)) {
                return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }
            
            // Padrão para usuários autenticados
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
    }
}
