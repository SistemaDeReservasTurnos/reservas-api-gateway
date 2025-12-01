package com.servicio.reserva.gateway.application.dto.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogoutRequest {
    @NotBlank(message = "Token is required.")
    private String token;
}
