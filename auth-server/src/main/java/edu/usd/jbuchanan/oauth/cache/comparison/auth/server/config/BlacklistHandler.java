package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2TokenRevocationAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2TokenRevocationAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BlacklistHandler implements AuthenticationProvider {
    private final CacheBackedAuthorizationService authorizationService;
    private final OAuth2TokenRevocationAuthenticationProvider defaultProvider;

    public BlacklistHandler(
            CacheBackedAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
        this.defaultProvider = new OAuth2TokenRevocationAuthenticationProvider(authorizationService);
    }

    @Override
    public Authentication authenticate(Authentication authentication){
        log.warn("revokeAllUserTokens fetching authorization");
        OAuth2TokenRevocationAuthenticationToken revocationAuthentication =
                (OAuth2TokenRevocationAuthenticationToken) authentication;
        log.warn("revokeAllUserTokens called with authentication {}", revocationAuthentication);
        Authentication result = defaultProvider.authenticate(revocationAuthentication);


        String token = revocationAuthentication.getToken();
        OAuth2Authorization authorization = authorizationService.findByToken(
                token,
                OAuth2TokenType.ACCESS_TOKEN
        );

        if (authorization != null) {
            revokeAllUserTokens(authorization);

            // Clear any custom session data
            clearCustomSessionData(authorization);
        }

        return result;
    }

    private void revokeAllUserTokens(OAuth2Authorization authorization) {
        String principalName = authorization.getPrincipalName();
        authorizationService.remove(authorization);
    }

    private void clearCustomSessionData(OAuth2Authorization authorization) {

    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OAuth2TokenRevocationAuthenticationToken.class.isAssignableFrom(authentication);
    }
}