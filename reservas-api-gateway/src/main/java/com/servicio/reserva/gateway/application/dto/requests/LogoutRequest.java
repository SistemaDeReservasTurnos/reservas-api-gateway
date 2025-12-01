package com.servicio.reserva.gateway.application.dto.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class LogoutRequest {
    @NotBlank(message = "Token is required.")
    private String token;
}
