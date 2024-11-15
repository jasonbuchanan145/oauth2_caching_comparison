package com.usd.edu.jbuchanan.auth.resource.config;

import com.nimbusds.jwt.JWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class CustomTokenValidator implements OAuth2TokenValidator<Jwt> {

    @Autowired
    @Qualifier("redisCacheManager")
    private CacheManager redisCacheManager;

    @Autowired
    @Qualifier("memcachedCacheManager")
    private CacheManager memcachedCacheManager;

    @Autowired
    @Qualifier("hazelcastCacheManager")
    private CacheManager hazelcastCacheManager;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        List<OAuth2Error> errors = new ArrayList<>();

        // Validate issuer
        String issuer = jwt.getClaimAsString("iss");
        if (!"your-expected-issuer".equals(issuer)) {
            errors.add(new OAuth2Error("invalid_issuer",
                    "Invalid token issuer", null));
        }

        // Validate audience
        List<String> audience = jwt.getClaimAsStringList("aud");
        if (audience == null || !audience.contains("your-expected-audience")) {
            errors.add(new OAuth2Error("invalid_audience",
                    "Invalid token audience", null));
        }

        // Validate scope
        String scope = jwt.getClaimAsString("scope");
        if (scope == null || !scope.contains("required-scope")) {
            errors.add(new OAuth2Error("invalid_scope",
                    "Required scope not present", null));
        }

        //Extract the claim for which cache to check
        String cache = jwt.getClaimAsString("cache-type");
        CacheManager manager = switch (cache) {
            case "redis" -> redisCacheManager;
            case "memcached" -> memcachedCacheManager;
            case "hazelcast" -> hazelcastCacheManager;
            default -> throw new IllegalArgumentException("Unknown cache type: " + cache);
        };

        String version = jwt.getClaim("version");
        //check the cache to see if session is blacklisted
        Jwt retrived = manager.getCache("auth").get(jwt.toString(), Jwt.class);
        if(retrived != null) {
            if (Integer.parseInt(retrived.getClaim("version"))>=Integer.parseInt(version)){
                errors.add(new OAuth2Error("Invalid_token","The provided token has been issued after the user already logged out. " +
                        "This token is blacklisted",null));
            }
        }


        // Validate custom claims
        String customClaim = jwt.getClaimAsString("custom-claim");
        if (customClaim == null) {
            errors.add(new OAuth2Error("invalid_claim",
                    "Required custom claim not present", null));
        }

        // Return success if no errors, otherwise return errors
        return errors.isEmpty()
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(errors);
    }
}