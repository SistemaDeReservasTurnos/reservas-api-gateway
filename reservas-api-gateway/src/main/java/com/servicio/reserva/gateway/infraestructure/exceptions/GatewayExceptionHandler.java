package com.servicio.reserva.gateway.infraestructure.exceptions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
@RequiredArgsConstructor
public class GatewayExceptionHandler {
    private final ObjectMapper objectMapper;

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<GatewayErrorResponse> handleWebClientResponseException(WebClientResponseException ex, ServerHttpRequest request) {
        int statusCode = ex.getStatusCode().value();
        String statusText = ex.getStatusText().trim();
        Map<String, Object> responseBody;
        String errorBody = ex.getResponseBodyAsString();

        try {
            responseBody = objectMapper.readValue(errorBody, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            responseBody = new HashMap<>();
            responseBody.put("error", errorBody);
        }

        String oauth2ErrorCode =  String.valueOf(responseBody.get("error"));

        if (Objects.equals(oauth2ErrorCode, "temporarily_unavailable")) {
            statusCode = HttpStatus.SERVICE_UNAVAILABLE.value();
            statusText = HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase();
        }

        GatewayErrorResponse errorResponse = new GatewayErrorResponse(
                statusCode,
                request.getPath().toString(),
                statusText,
                responseBody
        );

        return ResponseEntity.status(statusCode).body(errorResponse);
    }
}
