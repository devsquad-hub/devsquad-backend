package com.devsquad.identity.adapter.in.security;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration(proxyBeanMethods = false)
class ClerkJwtConfiguration {

    @Bean
    @Lazy
    JwtDecoder clerkJwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
            @Value("${app.security.authorized-parties}") String authorizedPartiesValue) {
        var decoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuer);
        Set<String> authorizedParties = Arrays.stream(authorizedPartiesValue.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        var validator = new DelegatingOAuth2TokenValidator<Jwt>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new AuthorizedPartyValidator(authorizedParties));
        decoder.setJwtValidator(validator);
        return decoder;
    }
}
