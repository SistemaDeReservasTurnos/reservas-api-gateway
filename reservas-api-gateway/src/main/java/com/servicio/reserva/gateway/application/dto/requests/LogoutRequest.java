package com.servicio.reserva.gateway.application.dto.requests;

import lombok.Data;

@Data
public class LogoutRequest {
    private String token;
}
