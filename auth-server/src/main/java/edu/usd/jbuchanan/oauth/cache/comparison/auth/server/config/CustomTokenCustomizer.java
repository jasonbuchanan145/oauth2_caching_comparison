package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config;

import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

import java.util.Objects;


@Component
public class CustomTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    @Override
    public void customize(JwtEncodingContext context) {
        if (context.getTokenType().getValue().equals("access_token")) {
            String deviceId = Objects.requireNonNull(context.getAuthorization()).getAttribute("device_id");
            if (deviceId != null) {
                context.getClaims().claim("device_id", deviceId);
            }
        }
    }
}