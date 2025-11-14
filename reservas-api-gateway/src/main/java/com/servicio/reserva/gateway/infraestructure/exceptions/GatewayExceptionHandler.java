package com.servicio.reserva.gateway.infraestructure.exceptions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
public class GatewayExceptionHandler {
    private final ObjectMapper objectMapper;

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<GatewayErrorResponse> handleWebClientResponseException(WebClientResponseException ex, ServerHttpRequest request) {
        Map<String, Object> responseBody;
        String errorBody = ex.getResponseBodyAsString();

        try {
            responseBody = objectMapper.readValue(errorBody, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            responseBody = new HashMap<>();
            responseBody.put("message", errorBody);
        }

        GatewayErrorResponse errorResponse = new GatewayErrorResponse(
                ex.getStatusCode().value(),
                request.getPath().toString(),
                ex.getStatusText(),
                responseBody
        );

        return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
    }
}
