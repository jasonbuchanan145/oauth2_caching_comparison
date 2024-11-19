package edu.usd.jbuchanan.oauth.cache.comparison.auth.server;

import edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config.CustomAuthorizationCodeAuthenticationConverter;
import edu.usd.jbuchanan.oauth.cache.comparison.cacheconfig.cache.FindMaxVerisionUtility;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CustomAuthorizationCodeAuthenticationConverterTests {

    @Mock
    private HttpServletRequest request;

    @Mock
    private FindMaxVerisionUtility findMaxVerisionUtility;

    @Mock
    private OAuth2ClientAuthenticationToken clientAuthenticationToken;

    @Mock
    private RegisteredClient registeredClient;

    @InjectMocks
    private CustomAuthorizationCodeAuthenticationConverter converter;

    private Map<String, String[]> parameterMap;

    @BeforeEach
    void setUp() {
        parameterMap = new HashMap<>();
        parameterMap.put("code", new String[]{"test-code"});
        parameterMap.put("redirect_uri", new String[]{"http://127.0.0.1:8080/login/oauth2/code/client"});
    }
    @Test
    void convert_WithValidRequest_ShouldReturnAuthenticationToken() {
        // Arrange
        when(request.getParameterMap()).thenReturn(parameterMap);
        when(request.getUserPrincipal()).thenReturn(clientAuthenticationToken);
        when(request.getHeader("cache-type")).thenReturn("redis");
        when(clientAuthenticationToken.getName()).thenReturn("client-name");
        when(findMaxVerisionUtility.incrementVersion(anyString(), any()))
                .thenReturn(1);

        // Act
        Authentication auth = converter.convert(request);

        // Assert
        assertNotNull(auth);
        assertInstanceOf(OAuth2AuthorizationCodeAuthenticationToken.class, auth);

        OAuth2AuthorizationCodeAuthenticationToken token = (OAuth2AuthorizationCodeAuthenticationToken) auth;
        Map<String, Object> additionalParameters = token.getAdditionalParameters();

        assertNotNull(additionalParameters);
        assertTrue(additionalParameters.containsKey("cache"));
        assertTrue(additionalParameters.containsKey("version"));
        assertEquals("redis", additionalParameters.get("cache"));
        assertEquals(1, additionalParameters.get("version"));
        assertEquals("test-code", token.getCode());
        assertEquals("http://127.0.0.1:8080/login/oauth2/code/client", token.getRedirectUri());
    }

    @Test
    void convert_WithMissingCacheType_ShouldThrowException() {
        // Arrange
        when(request.getParameterMap()).thenReturn(parameterMap);
        when(request.getUserPrincipal()).thenReturn(clientAuthenticationToken);
        when(request.getHeader("cache-type")).thenReturn(null);

        // Act & Assert
        assertThrows(NullPointerException.class, () -> converter.convert(request));
    }

    @Test
    void convert_WithMissingCode_ShouldUseClientId() {
        // Arrange
        parameterMap.remove("code");
        parameterMap.put("client_id", new String[]{"test-client-id"});

        when(request.getParameterMap()).thenReturn(parameterMap);
        when(request.getUserPrincipal()).thenReturn(clientAuthenticationToken);
        when(request.getHeader("cache-type")).thenReturn("redis");
        when(clientAuthenticationToken.getName()).thenReturn("client-name");
        when(findMaxVerisionUtility.incrementVersion(anyString(), any()))
                .thenReturn(1);

        // Act
        Authentication auth = converter.convert(request);

        // Assert
        assertNotNull(auth);
        assertInstanceOf(OAuth2AuthorizationCodeAuthenticationToken.class, auth);

        OAuth2AuthorizationCodeAuthenticationToken token = (OAuth2AuthorizationCodeAuthenticationToken) auth;
        assertEquals("test-client-id", token.getCode());
    }
}
