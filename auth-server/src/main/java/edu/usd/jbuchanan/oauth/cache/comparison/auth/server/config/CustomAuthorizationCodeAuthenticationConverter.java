package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CustomAuthorizationCodeAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        MultiValueMap<String, String> parameters = getParameters(request);

        // Extract the authorization code
        String code = Optional.ofNullable(parameters.getFirst(OAuth2ParameterNames.CODE)).orElse(parameters.getFirst(OAuth2ParameterNames.CLIENT_ID));
     /*   if (!StringUtils.hasText(code)) {
            throwError(OAuth2ErrorCodes.INVALID_REQUEST, OAuth2ParameterNames.CODE);
        }
*/
        String redirectUri = Optional.ofNullable(
                parameters.getFirst(OAuth2ParameterNames.REDIRECT_URI)).orElse("http://127.0.0.1:8080/login/oauth2/code/client");

        // Extract additional parameters
        Map<String, Object> additionalParameters = new HashMap<>();
        parameters.forEach((key, values) -> {
            if (!key.equals(OAuth2ParameterNames.GRANT_TYPE) &&
                    !key.equals(OAuth2ParameterNames.CODE) &&
                    !key.equals(OAuth2ParameterNames.REDIRECT_URI)) {
                additionalParameters.put(key, values.get(0));
            }
        });

        // Extract version
        String deviceId = parameters.getFirst("version");
        if (StringUtils.hasText(deviceId)) {
            additionalParameters.put("version", deviceId);
        }

        // Get client authentication
        Authentication clientPrincipal = (Authentication) request.getUserPrincipal();

        if (!(clientPrincipal instanceof OAuth2ClientAuthenticationToken)) {
            throwError(OAuth2ErrorCodes.INVALID_CLIENT, "No OAuth2ClientAuthenticationToken");
        }

        return new OAuth2AuthorizationCodeAuthenticationToken(
                code, clientPrincipal, redirectUri, additionalParameters);
    }

    // Utility methods
    private MultiValueMap<String, String> getParameters(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        MultiValueMap<String, String> parameters = new org.springframework.util.LinkedMultiValueMap<>(parameterMap.size());
        parameterMap.forEach((key, values) -> parameters.put(key, java.util.Arrays.asList(values)));
        return parameters;
    }

    private void throwError(String errorCode, String parameterName) {
        throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException(
                new org.springframework.security.oauth2.core.OAuth2Error(errorCode, "Invalid parameter: " + parameterName, null)
        );
    }
}

