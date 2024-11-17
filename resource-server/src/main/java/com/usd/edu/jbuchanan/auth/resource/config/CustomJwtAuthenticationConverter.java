package com.usd.edu.jbuchanan.auth.resource.config;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomJwtAuthenticationConverter extends JwtAuthenticationConverter {

    public void validate(Jwt jwt) {
        // The JwtDecoder will have already validated the signature before this point
        validateIssuer(jwt);
        validateAudience(jwt);
        validateScope(jwt);
        validateCustomClaims(jwt);
    }

    private void validateIssuer(Jwt jwt) {
        String issuer = jwt.getClaimAsString("iss");
        if (!"your-expected-issuer".equals(issuer)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_token", "Invalid token issuer", null)
            );
        }
    }

    private void validateAudience(Jwt jwt) {
        List<String> audience = jwt.getClaimAsStringList("aud");
        if (audience == null || !audience.contains("your-expected-audience")) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_token", "Invalid token audience", null)
            );
        }
    }

    private void validateScope(Jwt jwt) {
        String scope = jwt.getClaimAsString("scope");
        if (scope == null || !scope.contains("required-scope")) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_token", "Required scope not present", null)
            );
        }
    }

    private void validateCustomClaims(Jwt jwt) {
        String customClaim = jwt.getClaimAsString("custom-claim");
        if (customClaim == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_token", "Required custom claim not present", null)
            );
        }
    }
}
