package com.servicio.reserva.gateway.infraestructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange(exchange ->
                    exchange
                            .pathMatchers("/api/auth/register").permitAll()
                            .pathMatchers("/api/auth/login").permitAll()
                            .pathMatchers("/api/auth/refresh").permitAll()
                            .pathMatchers("/api/auth/logout").permitAll()
                            .anyExchange().authenticated()
                )
                .oauth2ResourceServer(server -> server.jwt(Customizer.withDefaults()))
                .csrf(ServerHttpSecurity.CsrfSpec::disable);

        return http.build();
    }
}
