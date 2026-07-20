package com.xetaX.apigateway.utils;

import com.xetaX.apigateway.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public final class ResponseUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ResponseUtil() {
    }

    public static Mono<Void> writeErrorResponse(
            ServerWebExchange exchange,
            HttpStatus status,
            String message) {

        try {

            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(status.value())
                    .error(status.getReasonPhrase())
                    .message(message)
                    .path(exchange.getRequest().getPath().value())
                    .timestamp(LocalDateTime.now())
                    .build();

            byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(errorResponse);

            exchange.getResponse().setStatusCode(status);
            exchange.getResponse().getHeaders()
                    .setContentType(MediaType.APPLICATION_JSON);

            DataBuffer buffer = exchange.getResponse()
                    .bufferFactory()
                    .wrap(bytes);

            return exchange.getResponse()
                    .writeWith(Mono.just(buffer));

        } catch (Exception e) {

            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);

            return exchange.getResponse().setComplete();

        }
    }

}