package com.servicio.reserva.gateway.application.dto;

import lombok.Data;

@Data
public class RefreshTokenRequest {
    private String refresh_token;
}
