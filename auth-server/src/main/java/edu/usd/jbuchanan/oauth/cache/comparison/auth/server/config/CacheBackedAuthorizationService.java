package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config;

import org.springframework.cache.Cache;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.stereotype.Component;

@Component
public class CacheBackedAuthorizationService implements OAuth2AuthorizationService {
    private final DynamicCacheResolver cacheManager;

    public CacheBackedAuthorizationService(DynamicCacheResolver cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        Cache authCache = cacheManager.getCacheManager().getCache("auth");
        Cache tokenCache = cacheManager.getCacheManager().getCache("tokenToAuthorizationId");

        authCache.put(authorization.getId(), authorization);

        if (authorization.getAccessToken() != null) {
            String accessTokenValue = authorization.getAccessToken().getToken().getTokenValue();
            tokenCache.put(accessTokenValue, authorization.getId());
        }

        if (authorization.getRefreshToken() != null) {
            String refreshTokenValue = authorization.getRefreshToken().getToken().getTokenValue();
            tokenCache.put(refreshTokenValue, authorization.getId());
        }

        OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode =
                authorization.getToken(OAuth2AuthorizationCode.class);
        if (authorizationCode != null) {
            String codeValue = authorizationCode.getToken().getTokenValue();
            tokenCache.put(codeValue, authorization.getId());
        }
    }

    @Override
    public void remove(OAuth2Authorization authorization) {

    }

    @Override
    public OAuth2Authorization findById(String id) {
        return (OAuth2Authorization) cacheManager.getCacheManager()
                .getCache("auth").get(id);
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        Cache tokenCache = cacheManager.getCacheManager().getCache("tokenToAuthorizationId");
        String authorizationId = tokenCache.get(token, String.class);

        if (authorizationId != null) {
            Cache authCache = cacheManager.getCacheManager().getCache("auth");
            return authCache.get(authorizationId, OAuth2Authorization.class);
        }

        return null;
    }
}
