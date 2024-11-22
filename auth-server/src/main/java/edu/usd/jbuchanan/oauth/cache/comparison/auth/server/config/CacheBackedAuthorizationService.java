package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config;

import com.nimbusds.jwt.SignedJWT;
import edu.usd.jbuchanan.oauth.cache.comparison.cacheconfig.cache.FindMaxVerisionUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.stereotype.Component;

import java.text.ParseException;

@Component
@Slf4j
public class CacheBackedAuthorizationService implements OAuth2AuthorizationService {
    private final DynamicCacheResolver cacheManager;
    private final FindMaxVerisionUtility findMaxVerisionUtility;
    public CacheBackedAuthorizationService(DynamicCacheResolver cacheManager, FindMaxVerisionUtility findMaxVerisionUtility) {
        this.cacheManager = cacheManager;
        this.findMaxVerisionUtility = findMaxVerisionUtility;
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
        String cache;
        Integer version;
        String subject;
        try {
            SignedJWT signedJWT = SignedJWT.parse(authorization.getAccessToken().getToken().getTokenValue());
            cache = signedJWT.getJWTClaimsSet().getStringClaim("cache");
            version = signedJWT.getJWTClaimsSet().getIntegerClaim("version");
            subject = signedJWT.getJWTClaimsSet().getSubject();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        /*TODO Shortcut
        cacheManager.getCacheManager(cache).getCache("auth").evictIfPresent(authorization.getId());
        cacheManager.getCacheManager(cache).getCache("tokenToAuthorizationId");
        */
        log.warn("Removing authorization {} from cache", authorization);
        findMaxVerisionUtility.logout(subject, FindMaxVerisionUtility.CacheType.valueOf(cache.toUpperCase()),version);

    }

    @Override
    public OAuth2Authorization findById(String id) {
        return (OAuth2Authorization) cacheManager.getCacheManager()
                .getCache("auth").get(id);
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        String cache;
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            cache = signedJWT.getJWTClaimsSet().getStringClaim("cache");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        Cache tokenCache = cacheManager.getCacheManager(cache).getCache("tokenToAuthorizationId");
        String authorizationId = tokenCache.get(token, String.class);

        if (authorizationId != null) {
            Cache authCache = cacheManager.getCacheManager(cache).getCache("auth");
            return authCache.get(authorizationId, OAuth2Authorization.class);
        }
        log.warn("Authorization {} not found", token);
        return null;
    }
}
