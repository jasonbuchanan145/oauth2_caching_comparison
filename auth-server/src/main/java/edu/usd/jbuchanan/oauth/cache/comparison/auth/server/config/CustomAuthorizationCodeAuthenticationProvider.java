package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

public class CustomAuthorizationCodeAuthenticationProvider implements AuthenticationProvider {

    private final OAuth2AuthorizationCodeAuthenticationProvider delegate;
    private final OAuth2AuthorizationService authorizationService;

    public CustomAuthorizationCodeAuthenticationProvider(
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<?> tokenGenerator) {
        this.authorizationService = authorizationService;
        this.delegate = new OAuth2AuthorizationCodeAuthenticationProvider(authorizationService,tokenGenerator);
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        OAuth2AuthorizationCodeAuthenticationToken authToken =
                (OAuth2AuthorizationCodeAuthenticationToken) authentication;

        // Perform the standard authentication
        OAuth2AccessTokenAuthenticationToken accessTokenAuthentication =
                (OAuth2AccessTokenAuthenticationToken) delegate.authenticate(authentication);

        // Retrieve the authorization
        OAuth2Authorization authorization = authorizationService.findByToken(
                authToken.getCode(), OAuth2TokenType.ACCESS_TOKEN);

        if (authorization != null) {
            // Retrieve device_id from additional parameters
            String deviceId = (String) authToken.getAdditionalParameters().get("device_id");

            // Store device_id in authorization attributes
            OAuth2Authorization updatedAuthorization = OAuth2Authorization.from(authorization)
                    .attribute("device_id", deviceId)
                    .build();

            // Update the authorization
            authorizationService.save(updatedAuthorization);
        }

        return accessTokenAuthentication;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return delegate.supports(authentication);
    }
}

