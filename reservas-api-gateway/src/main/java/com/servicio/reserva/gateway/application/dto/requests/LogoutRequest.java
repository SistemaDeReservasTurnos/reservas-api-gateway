package com.servicio.reserva.gateway.application.dto.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogoutRequest {
    @NotBlank(message = "Token is required.")
    private String token;
}
