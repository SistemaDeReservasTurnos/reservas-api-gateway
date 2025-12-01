package com.servicio.reserva.gateway.application.dto.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh token is required")
    private String refresh_token;
}
