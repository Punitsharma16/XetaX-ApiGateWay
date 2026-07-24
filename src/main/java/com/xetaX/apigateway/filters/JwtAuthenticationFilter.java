package com.xetaX.apigateway.filters;

import com.xetaX.apigateway.security.JwtService;
import com.xetaX.apigateway.utils.ResponseUtil;
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
        if (token.isBlank()) {
            log.warn("Invalid JWT token");

            return ResponseUtil.writeErrorResponse(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "Invalid JWT Token."
            );
        }

        if (token.isBlank()) {
            log.warn("Refresh token used instead of access token.");
            return ResponseUtil.writeErrorResponse(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "Access token required."
            );
        }
             Claims claims;
        try {
            /*
             * Signature, issuer and expiration validation.
             */
            claims = jwtService.getClaims(token);

        } catch (Exception exception) {

            log.warn(
                    "JWT validation failed: path={}, reason={}",
                    path,
                    exception.getMessage()
            );

            return ResponseUtil.writeErrorResponse(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "Invalid or expired access token."
            );
        }

        String tokenType = claims.get("typ", String.class);
        if(!"access".equalsIgnoreCase(tokenType)){
         return ResponseUtil.writeErrorResponse( exchange,
                 HttpStatus.UNAUTHORIZED,
                 "Access token is required.");
        }
        String userId=claims.getSubject();
        String email = claims.get("user", String.class);
        String role = claims.get("role", String.class);

        if (userId == null || userId.isBlank()) {

            return ResponseUtil.writeErrorResponse(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "User ID is missing from access token."
            );
        }


        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(request -> request.headers(headers -> {

                    /*
                     * UI se aaye fake identity headers remove karo.
                     */
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Email");
                    headers.remove("X-User-Role");

                    /*
                     * Verified JWT claims se headers add karo.
                     */
                    headers.set("X-User-Id", userId);

                    if (email != null && !email.isBlank()) {
                        headers.set("X-User-Email", email);
                    }

                    if (role != null && !role.isBlank()) {
                        headers.set("X-User-Role", role);
                    }
                }))
                .build();

        log.debug("JWT verified successfully for user {}", claims.getSubject());

        return chain.filter(mutatedExchange);
    }
}