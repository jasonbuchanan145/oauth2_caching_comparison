package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@Slf4j
public class CustomAuthorizationCodeAuthenticationProvider implements AuthenticationProvider {
    private final OAuth2TokenGenerator<?> tokenGenerator;
    private final OAuth2AuthorizationService authorizationService;
    private final DynamicCacheResolver cacheManager;
    private final CachedClientRepository clientRepository;


    public CustomAuthorizationCodeAuthenticationProvider(
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<?> tokenGenerator,
            DynamicCacheResolver cacheManager,
            CachedClientRepository registeredClientRepository) {
        this.authorizationService = authorizationService;
        this.tokenGenerator = tokenGenerator;
        this.cacheManager = cacheManager;
        this.clientRepository = registeredClientRepository;
    }
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        OAuth2AuthorizationCodeAuthenticationToken authorizationCodeAuthentication =
                (OAuth2AuthorizationCodeAuthenticationToken) authentication;

        Map<String, Object> parameters = authorizationCodeAuthentication.getAdditionalParameters();
        String clientId = ((OAuth2AuthorizationCodeAuthenticationToken) authentication).getCode();
        Set<String> scopes;
        if(authentication.getPrincipal() instanceof OAuth2ClientAuthenticationToken clientAuthentication){
            scopes= Optional.ofNullable(clientAuthentication.getRegisteredClient()).map(RegisteredClient::getScopes).orElse(new HashSet<>());
        }else {
            String scopeParameter = (String) parameters.get(OAuth2ParameterNames.SCOPE);
            if (StringUtils.hasText(scopeParameter)) {
                scopes = new HashSet<>(Arrays.asList(scopeParameter.split(" ")));
            } else {
                scopes = new HashSet<>();
            }
        }
        // Find the registered client using the client ID
        RegisteredClient registeredClient = clientRepository.findByClientId(clientId);

        // Build access token context
        DefaultOAuth2TokenContext.Builder tokenContextBuilder = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(authorizationCodeAuthentication)
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .authorizedScopes(scopes)
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);

        OAuth2TokenContext tokenContext = tokenContextBuilder.build();
        Jwt jwt = (Jwt) tokenGenerator.generate(tokenContext);
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                jwt.getTokenValue(),
                jwt.getIssuedAt(),
                jwt.getExpiresAt(),
                new HashSet<>(jwt.getClaim("scope"))
        );
        OAuth2RefreshToken refreshToken = null;
       // if (registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)) {
        OAuth2TokenContext refreshTokenContext = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(authentication)  // Keep the original authentication
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .authorizedScopes(tokenContext.getAuthorizedScopes())
                .tokenType(OAuth2TokenType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrant(authentication)  // Set the authorization grant
                .build();

        OAuth2RefreshToken refresh = (OAuth2RefreshToken) tokenGenerator.generate(refreshTokenContext);
        refreshToken = new OAuth2RefreshToken(
                refresh.getTokenValue(),
                refresh.getIssuedAt(),
                refresh.getExpiresAt()
        );
       // }

        // Build the authorization
        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .principalName(authorizationCodeAuthentication.getPrincipal().toString())
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizedScopes(scopes);


        OAuth2Authorization authorization = authorizationBuilder.build();
        authorizationService.save(authorization);

        return new OAuth2AccessTokenAuthenticationToken(
                registeredClient,
                (Authentication) authorizationCodeAuthentication.getPrincipal(),
                accessToken,
                refreshToken);
    }



    @Override
    public boolean supports(Class<?> authentication) {
        return OAuth2AuthorizationCodeAuthenticationToken.class.isAssignableFrom(authentication);
    }
}


