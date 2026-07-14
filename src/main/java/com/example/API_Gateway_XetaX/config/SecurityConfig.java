package com.example.API_Gateway_XetaX.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrfSpec -> csrfSpec.disable())
                .cors(Customizer.withDefaults())
                .httpBasic(httpBasicSpec -> httpBasicSpec.disable())
                .logout(logoutSpec -> logoutSpec.disable())
                .authorizeExchange(exchangeSpec -> exchangeSpec
                        .pathMatchers("/auth/v1**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .anyExchange().permitAll()
                )
                .build();
    }
}
