package com.servicio.reserva.gateway.infraestructure.exceptions;

import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class GatewayErrorResponse {
    private String timestamp;
    private int status;
    private String path;
    private String error;
    private Map<String, Object> message;

    public GatewayErrorResponse(int status, String path, String error, Map<String, Object> message) {
        this.timestamp = Instant.now().toString();
        this.status = status;
        this.path = path;
        this.error = error;
        this.message = message;
    }
}
