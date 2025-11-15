package com.servicio.reserva.gateway.application.dto;

import lombok.Data;

@Data
public class TokenResponse {
    private String access_token;
    private String refresh_token;
}
