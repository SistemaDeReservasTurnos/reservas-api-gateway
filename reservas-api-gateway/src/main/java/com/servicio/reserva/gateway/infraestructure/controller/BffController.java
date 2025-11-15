package com.servicio.reserva.gateway.infraestructure.controller;

import com.servicio.reserva.gateway.application.dto.LoginRequest;
import com.servicio.reserva.gateway.application.dto.RefreshTokenRequest;
import com.servicio.reserva.gateway.application.dto.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;

@RestController
@RequestMapping("/api")
public class BffController {
    private final WebClient webClient;

    @Value("${bff.client-id}")
    private String clientId;
    @Value("${bff.client-secret}")
    private String clientSecret;

    public BffController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("lb://reservas-auth-service").build();
    }

    @PostMapping("/auth/login")
    public Mono<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        String authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes());

        BodyInserters.FormInserter<String> formData = BodyInserters
                .fromFormData("grant_type", "password")
                .with("username", loginRequest.getEmail())
                .with("password", loginRequest.getPassword())
                .with("scope", "openid read write");

        return this.webClient.post()
                .uri("/oauth2/token")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .bodyToMono(TokenResponse.class);
    }

    @PostMapping("/auth/refresh")
    public Mono<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        String authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes());

        BodyInserters.FormInserter<String> formData = BodyInserters
                .fromFormData("grant_type", "refresh_token")
                .with("refresh_token", refreshTokenRequest.getRefresh_token());

        return this.webClient.post()
                .uri("/oauth2/token")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .bodyToMono(TokenResponse.class);
    }
}
