package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.Filter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Configuration
public class SecurityConfiguration {



    @Bean
    public AuthenticationProvider authenticationProvider(CustomTokenCustomizer customTokenCustomizer) {
        return new CustomAuthorizationCodeAuthenticationProvider(authorizationService(), tokenGenerator(customTokenCustomizer));
    }
    @Bean
    public Filter cacheTypeFilter() {
        return new CacheTypeFilter();
    }

    @Bean
    public OAuth2AuthorizationService authorizationService() {
        return new InMemoryOAuth2AuthorizationService();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("client-id")
                .clientSecret("{noop}client-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/login/oauth2/code/your-client")
                .scope("read")
                .scope("write")
                .build();

        return new InMemoryRegisteredClientRepository(registeredClient);
    }
    @Bean
    public OAuth2TokenGenerator<?> tokenGenerator(CustomTokenCustomizer customTokenCustomizer) {
        JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder());
        jwtGenerator.setJwtCustomizer(customTokenCustomizer);

        return new DelegatingOAuth2TokenGenerator(
                jwtGenerator,
                new OAuth2AccessTokenGenerator(),
                new OAuth2RefreshTokenGenerator()
        );
    }
    // Define the JwtEncoder bean
    @Bean
    public JwtEncoder jwtEncoder() {
        SecretKey secretKey = new SecretKeySpec("secret-key-which-is-at-least-32-bytes-long".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        OctetSequenceKey octetSequenceKey = new OctetSequenceKey.Builder(secretKey)
                .keyID("key-id")
                .build();

        JWKSet jwkSet = new JWKSet(octetSequenceKey);
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);

        return new NimbusJwtEncoder(jwkSource);
    }
}