package com.example.API_Gateway_XetaX.filters;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RouteValidator {

    public static final List<String> OPEN_API_ENDPOINT = List.of(
            "/api/v1/auth/**",
            "/actuator/**"
    );

    public boolean isSecured(String requestPath) {
        return OPEN_API_ENDPOINT.stream().noneMatch(uri -> requestPath.contains(uri));
    }
}
