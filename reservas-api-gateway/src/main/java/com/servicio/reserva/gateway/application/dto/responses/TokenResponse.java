package com.servicio.reserva.gateway.application.dto.responses;

import lombok.Data;

@Data
public class TokenResponse {
    private String access_token;
    private String refresh_token;
}
