package com.devsquad.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain apiSecurity(
            HttpSecurity http,
            @Value("${app.security.enabled:true}") boolean securityEnabled) throws Exception {
        http.csrf(csrf -> csrf.disable());

        if (!securityEnabled) {
            return http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll()).build();
        }

        return http
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/actuator/health/**", "/api/v1/public/**", "/api/v1/webhooks/clerk").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(server -> server.jwt(Customizer.withDefaults()))
                .build();
    }
}
