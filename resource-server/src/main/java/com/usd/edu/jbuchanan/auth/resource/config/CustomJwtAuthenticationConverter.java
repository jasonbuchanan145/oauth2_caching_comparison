package com.usd.edu.jbuchanan.auth.resource.config;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

public class CustomJwtAuthenticationConverter extends JwtAuthenticationConverter {
    public CustomJwtAuthenticationConverter(TokenValidator tokenValidator) {
    }
}
