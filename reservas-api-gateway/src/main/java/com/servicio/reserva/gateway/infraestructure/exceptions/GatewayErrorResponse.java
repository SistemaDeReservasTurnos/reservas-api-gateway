package com.servicio.reserva.gateway.infraestructure.exceptions;

import lombok.Data;

import java.time.Instant;

@Data
public class GatewayErrorResponse {
    private String timestamp;
    private int status;
    private String path;
    private String message;
    private Object errors;


    public GatewayErrorResponse(int status, String path, String message, Object errors) {
        this.timestamp = Instant.now().toString();
        this.status = status;
        this.path = path;
        this.message = message;
        this.errors = errors;
    }
}
