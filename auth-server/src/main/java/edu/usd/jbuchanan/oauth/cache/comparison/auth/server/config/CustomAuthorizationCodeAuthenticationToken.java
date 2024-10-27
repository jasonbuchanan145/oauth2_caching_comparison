package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config;

import lombok.Getter;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;

import java.util.Map;

@Getter
public class CustomAuthorizationCodeAuthenticationToken extends OAuth2AuthorizationCodeAuthenticationToken {

    private final String version;

    public CustomAuthorizationCodeAuthenticationToken(
            String code,
            OAuth2ClientAuthenticationToken clientPrincipal,
            String redirectUri,
            String version,
            Map<String, Object> additionalParameters) {
        super(code, clientPrincipal, redirectUri, additionalParameters);
        this.version = version;
    }

}

