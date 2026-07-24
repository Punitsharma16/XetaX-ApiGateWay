package com.xetaX.apigateway.filters;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RouteValidator {

    public static final List<String> OPEN_API_ENDPOINT = List.of(
            "/auth/v1/login",
            "/auth/v1/register",
            "/auth/v1/refresh",
            "/actuator/**"
    );

    public boolean isSecured(String requestPath) {
        return OPEN_API_ENDPOINT.stream().noneMatch(uri -> requestPath.contains(uri));
    }
}
