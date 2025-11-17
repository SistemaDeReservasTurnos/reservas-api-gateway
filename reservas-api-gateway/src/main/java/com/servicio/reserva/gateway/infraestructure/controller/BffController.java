package com.servicio.reserva.gateway.infraestructure.controller;

import com.servicio.reserva.gateway.application.dto.requests.LoginRequest;
import com.servicio.reserva.gateway.application.dto.requests.LogoutRequest;
import com.servicio.reserva.gateway.application.dto.requests.RefreshTokenRequest;
import com.servicio.reserva.gateway.application.dto.responses.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class BffController {
    private final WebClient webClient;

    public BffController(WebClient.Builder webClientBuilder,
                         @Value("${bff.client-id}") String clientId,
                         @Value("${bff.client-secret}") String clientSecret) {
        this.webClient = webClientBuilder
                .baseUrl("lb://reservas-auth-service")
                .filter(ExchangeFilterFunctions.basicAuthentication(clientId, clientSecret))
                .build();
    }

    @PostMapping("/auth/login")
    public Mono<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        BodyInserters.FormInserter<String> formData = BodyInserters
                .fromFormData("grant_type", "password")
                .with("username", loginRequest.getEmail())
                .with("password", loginRequest.getPassword())
                .with("scope", "openid read write");

        return this.webClient.post()
                .uri("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .bodyToMono(TokenResponse.class);
    }

    @PostMapping("/auth/refresh")
    public Mono<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        BodyInserters.FormInserter<String> formData = BodyInserters
                .fromFormData("grant_type", "refresh_token")
                .with("refresh_token", refreshTokenRequest.getRefresh_token());

        return this.webClient.post()
                .uri("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .bodyToMono(TokenResponse.class);
    }

    @PostMapping("/auth/logout")
    public Mono<Void> logout(@RequestBody LogoutRequest request) {
        BodyInserters.FormInserter<String> formData = BodyInserters
                .fromFormData("token", request.getToken())
                .with("token_type_hint", "access_token");

        return this.webClient.post()
                .uri("/oauth2/revoke") // Endpoint estándar de revocación
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .toBodilessEntity()
                .then();
    }
}
