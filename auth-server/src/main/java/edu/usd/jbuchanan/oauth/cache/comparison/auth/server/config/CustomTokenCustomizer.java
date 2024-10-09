package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config;

import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;


@Component
public class CustomTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    @Override
    public void customize(JwtEncodingContext context) {
        if (context.getTokenType().getValue().equals("access_token")) {
            // Retrieve device_id from additionalParameters
            String deviceId = (String) context.getAuthorizationGrant().getAdditionalParameters().get("device_id");
            if (deviceId != null) {
                context.getClaims().claim("device_id", deviceId);
            }
        }
    }
}