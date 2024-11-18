package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    @Override
    public void customize(JwtEncodingContext context) {
        Authentication authentication = context.getPrincipal();
        if (authentication instanceof OAuth2AuthorizationCodeAuthenticationToken authToken) {
            context.getClaims().claims(claims ->
                    claims.putAll(authToken.getAdditionalParameters().entrySet().stream().filter(entry -> !entry.getKey().startsWith("client_secret")).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));
        }
    }
}
