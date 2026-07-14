package com.example.API_Gateway_XetaX.filters;

import com.example.API_Gateway_XetaX.security.JwtService;
import com.example.API_Gateway_XetaX.utils.ResponseUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter {

    private final JwtService jwtService;
    private final RouteValidator routeValidator;
    @PostConstruct
    public void init() {
        System.out.println("JwtAuthenticationFilter Loaded");
    }
    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {

        System.out.println("========== Gateway ==========");
        System.out.println(exchange.getRequest().getMethod());
        System.out.println(exchange.getRequest().getURI());

        String path = exchange.getRequest().getPath().value();

        // Skip public APIs
        if (!routeValidator.isSecured(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {

            log.warn("Authorization header missing for {}", path);

            return ResponseUtil.writeErrorResponse(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "Authorization header is missing."
            );
        }

        String token = authHeader.substring(7);

        if (!jwtService.validateToken(token)) {

            log.warn("Invalid JWT token");

            return ResponseUtil.writeErrorResponse(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "Invalid JWT Token."
            );
        }

        if (!jwtService.validateToken(token)) {

            log.warn("Refresh token used instead of access token.");

            return ResponseUtil.writeErrorResponse(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "Access token required."
            );
        }

        Claims claims = jwtService.getClaims(token);

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header("X-User-Id", claims.getSubject())
                        .header("X-User-Email", claims.get("user", String.class))
                        // Uncomment after Auth Service starts sending roles
                        //.header("X-User-Role", String.join(",", jwtService.getRoles(token)))
                        .build())
                .build();

        log.debug("JWT verified successfully for user {}", claims.getSubject());

        return chain.filter(mutatedExchange);
    }
}