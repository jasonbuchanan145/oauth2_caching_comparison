package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class CreateTokenConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        Map<String, String> parameters = OAuth2EndpointUtils.getParameters(request);

        // Extract the authorization code
        String code = parameters.get(OAuth2ParameterNames.CODE);
        if (StringUtils.hasText(code)) {
            OAuth2EndpointUtils.throwError(
                    OAuth2ErrorCodes.INVALID_REQUEST,
                    OAuth2ParameterNames.CODE,
                    OAuth2EndpointUtils.ACCESS_TOKEN_REQUEST_ERROR_URI);
        }

        // Extract the redirect_uri (optional)
        String redirectUri = parameters.get(OAuth2ParameterNames.REDIRECT_URI);


        // Extract device_id
        String deviceId = parameters.get("device_id");

        // Get client authentication
        OAuth2ClientAuthenticationToken clientPrincipal = OAuth2EndpointUtils.getAuthenticatedClient(request);

        // Create a custom authentication token
        return new CustomAuthorizationCodeAuthenticationToken(
                code, clientPrincipal, redirectUri, deviceId, additionalParameters);
    }

}
