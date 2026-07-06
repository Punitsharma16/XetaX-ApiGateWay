package com.example.API_Gateway_XetaX.filters;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        long startTime = System.currentTimeMillis();

        ServerHttpRequest request = exchange.getRequest();

        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        String  ip = request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress() : "UNKNOWN";

        log.info("Incoming Request => Method={}, Path={}, IP={}",
                method,
                path,
                ip);
        return chain.filter(exchange).doFinally(signalType -> {
            long duration = System.currentTimeMillis() - startTime;
            int status = exchange.getResponse().getStatusCode() != null ? exchange.getResponse().getStatusCode().value() : 0;
            String userId = Optional.ofNullable(
                    exchange.getRequest()
                            .getHeaders()
                            .getFirst("X-User-Id")
            ).orElse("ANONYMOUS");
            log.info(
                    "Completed Request => Method={}, Path={}, Status={}, Duration={} ms, UserId={}",
                    method,
                    path,
                    status,
                    duration,
                    userId
            );
        });
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
