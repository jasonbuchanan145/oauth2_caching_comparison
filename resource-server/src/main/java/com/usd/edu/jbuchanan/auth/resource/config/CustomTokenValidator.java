package com.usd.edu.jbuchanan.auth.resource.config;

import edu.usd.jbuchanan.oauth.cache.comparison.cacheconfig.cache.FindMaxVerisionUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CustomTokenValidator implements OAuth2TokenValidator<Jwt> {

    @Autowired
    private FindMaxVerisionUtility findMaxVerisionUtility;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        List<OAuth2Error> errors = new ArrayList<>();

        String user = jwt.getSubject();
        // Validate issuer
        String issuer = jwt.getClaimAsString("iss");
        if (!issuer.equals("http://oauth-server-service:9999")) {
            errors.add(new OAuth2Error("invalid_issuer",
                    "Invalid token issuer", null));
        }

        // Validate audience
        List<String> audience = jwt.getClaimAsStringList("aud");
        if (audience == null || !audience.contains("ThisIsMyClientId")) {
            errors.add(new OAuth2Error("invalid_audience",
                    "Invalid token audience", null));
        }

        // Validate scope
        String scope = jwt.getClaimAsString("scope");
        if (scope == null || !scope.contains("read")) {
            errors.add(new OAuth2Error("invalid_scope",
                    "Required scope not present", null));
        }

        //Extract the claim for which cache to check
        String cache = jwt.getClaimAsString("cache");

        int version = Math.toIntExact(jwt.getClaim("version"));
        //check the cache to see if session is blacklisted

        findMaxVerisionUtility.isLoggedOut(user, FindMaxVerisionUtility.CacheType.valueOf(cache.toUpperCase()))
                .filter(loggedOutVersion-> loggedOutVersion >= version)
                .ifPresent(loggedOutVersion -> errors.add(new OAuth2Error("Invalid_token","The token has been invalidated due to logout",null)));

        // Return success if no errors, otherwise return errors
        return errors.isEmpty()
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(errors);
    }
}