package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    @Override
    public void customize(JwtEncodingContext context) {
        Authentication authentication = context.getPrincipal();
        if (authentication instanceof OAuth2AuthorizationCodeAuthenticationToken authToken) {
            context.getClaims().claims(claims ->
                    claims.putAll(authToken.getAdditionalParameters()));
        }
    }
}
