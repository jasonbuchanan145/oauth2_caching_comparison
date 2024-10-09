package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenGenerator;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.OAuth2TokenType;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

public class CustomAuthorizationCodeAuthenticationProvider extends OAuth2AuthorizationCodeAuthenticationProvider {

    private final JwtEncoder jwtEncoder;

    public CustomAuthorizationCodeAuthenticationProvider(
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<?> tokenGenerator,
            JwtEncoder jwtEncoder) {
        super(authorizationService, tokenGenerator);
        this.jwtEncoder = jwtEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws OAuth2AuthenticationException {
        Assert.isInstanceOf(CustomAuthorizationCodeAuthenticationToken.class, authentication, "Authentication must be of type CustomAuthorizationCodeAuthenticationToken");

        CustomAuthorizationCodeAuthenticationToken customAuthentication =
                (CustomAuthorizationCodeAuthenticationToken) authentication;

        // Perform the standard authentication process
        OAuth2AccessTokenAuthenticationToken accessTokenAuthentication = (OAuth2AccessTokenAuthenticationToken) super.authenticate(customAuthentication);

        // Include deviceId in the token claims
        String deviceId = customAuthentication.getDeviceId();

        // Build the JWT claims with deviceId
        Map<String, Object> claims = new HashMap<>();
        claims.putAll(accessTokenAuthentication.getAdditionalParameters());
        claims.put("deviceId", deviceId);

        // Generate a new JWT with the additional claims
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .subject(accessTokenAuthentication.getPrincipal().getName())
                .issuedAt(Instant.now())
                .expiresAt(accessTokenAuthentication.getAccessToken().getExpiresAt())
                .claims(existingClaims -> existingClaims.putAll(claims));

        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(claimsBuilder.build()));

        // Return a new OAuth2AccessTokenAuthenticationToken with the updated JWT
        OAuth2AccessTokenAuthenticationToken result = new OAuth2AccessTokenAuthenticationToken(
                accessTokenAuthentication.getRegisteredClient(),
                accessTokenAuthentication.getPrincipal(),
                new OAuth2AccessToken(
                        accessTokenAuthentication.getAccessToken().getTokenType(),
                        jwt.getTokenValue(),
                        jwt.getIssuedAt(),
                        jwt.getExpiresAt(),
                        accessTokenAuthentication.getAccessToken().getScopes()
                ),
                accessTokenAuthentication.getRefreshToken(),
                accessTokenAuthentication.getAdditionalParameters()
        );

        return result;
    }
}
