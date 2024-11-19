package edu.usd.jbuchanan.oauth.cache.comparison.auth.server;

import edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config.CachedClientRepository;
import edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config.CustomAuthorizationCodeAuthenticationProvider;
import edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config.DynamicCacheResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
public class CustomAuthorizationCodeAuthenticationProviderTests {

    @Mock
    private OAuth2AuthorizationService authorizationService;

    @Mock
    private OAuth2TokenGenerator<OAuth2Token> tokenGenerator;

    @Mock
    private DynamicCacheResolver cacheResolver;

    @Mock
    private CachedClientRepository clientRepository;

    @Mock
    private OAuth2AuthorizationCodeAuthenticationToken authenticationToken;

    @Mock
    private OAuth2ClientAuthenticationToken clientPrincipal;

    @Mock
    private RegisteredClient registeredClient;

    @Mock
    private AuthorizationServerContext authorizationServerContext;

    @InjectMocks
    private CustomAuthorizationCodeAuthenticationProvider provider;

    @AfterEach
    void tearDown() {
        AuthorizationServerContextHolder.resetContext();
    }

    @Test
    void authenticate_WithValidToken_ShouldReturnAccessToken() {
        // Arrange
        Map<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.put("cache", "redis");
        additionalParameters.put("version", 1);

        when(authenticationToken.getAdditionalParameters()).thenReturn(additionalParameters);
        when(authenticationToken.getCode()).thenReturn("client-id");
        when(authenticationToken.getPrincipal()).thenReturn(clientPrincipal);
        when(clientRepository.findByClientId(anyString())).thenReturn(registeredClient);


        AuthorizationServerContextHolder.setContext(authorizationServerContext);

        // Create tokens
        Instant now = Instant.now();
        Set<String> scopes = new HashSet<>();
        scopes.add("read");

        // Create JWT for access token
        Map<String, Object> claims = new HashMap<>();
        claims.put("scope", scopes);

        Jwt jwt = Jwt.withTokenValue("access-token-value")
                .header("alg", "RS256")
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .claims(claimsBuilder -> claimsBuilder.putAll(claims))
                .build();

        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
                "refresh-token-value",
                now,
                now.plus(24, ChronoUnit.HOURS)
        );

        // Mock token generator
        doAnswer(invocation -> {
            OAuth2TokenContext context = invocation.getArgument(0);
            if (context.getTokenType().equals(OAuth2TokenType.ACCESS_TOKEN)) {
                return jwt;
            } else if (context.getTokenType().equals(OAuth2TokenType.REFRESH_TOKEN)) {
                return refreshToken;
            }
            return null;
        }).when(tokenGenerator).generate(any());

        // Act
        Authentication result = provider.authenticate(authenticationToken);

        // Assert
        assertNotNull(result);
        verify(authorizationService).save(any(OAuth2Authorization.class));
        verify(tokenGenerator, times(2)).generate(any());
    }

    @Test
    void supports_WithCorrectClass_ShouldReturnTrue() {
        assertTrue(provider.supports(OAuth2AuthorizationCodeAuthenticationToken.class));
    }

    @Test
    void supports_WithIncorrectClass_ShouldReturnFalse() {
        assertFalse(provider.supports(OAuth2ClientAuthenticationToken.class));
    }

    @Test
    void authenticate_WithNullClient_ShouldThrowException() {
        // Arrange
        when(authenticationToken.getCode()).thenReturn("client-id");
        when(clientRepository.findByClientId(anyString())).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> provider.authenticate(authenticationToken));
    }
}