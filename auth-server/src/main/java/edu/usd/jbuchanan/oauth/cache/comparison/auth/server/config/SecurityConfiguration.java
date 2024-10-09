package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Configuration
public class SecurityConfiguration {
    @Autowired
    private OAuth2AuthorizationService authorizationService;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        // Retrieve and customize the OAuth2AuthorizationServerConfigurer
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                http.getConfigurer(OAuth2AuthorizationServerConfigurer.class);

        // Customize the token endpoint
        authorizationServerConfigurer
                .tokenEndpoint(tokenEndpoint ->
                        tokenEndpoint
                                .accessTokenRequestConverter(new CustomAuthorizationCodeAuthenticationConverter())
                );

        // Register the custom authentication provider
        authorizationServerConfigurer.authorizationService(authorizationService);

        http.authorizeHttpRequests(auths ->
            auths
                    .requestMatchers("/oauth/redis/**").permitAll()
                    .requestMatchers("/oauth/memcached/**").permitAll()
                    .requestMatchers("/oauth/hazelcast/**").permitAll()
                    .anyRequest().authenticated()
        )
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(cacheTypeFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    @Autowired
    OAuth2TokenGenerator<?> oAuth2TokenGenerator;
    @Bean
    public AuthenticationProvider customAuthorizationCodeAuthenticationProvider() {
        return new CustomAuthorizationCodeAuthenticationProvider(authorizationService, oAuth2TokenGenerator);
    }
    @Bean
    public Filter cacheTypeFilter() {
        return new CacheTypeFilter();
    }
    @Bean
    public OAuth2AuthorizationService oAuth2AuthorizationService() {
        return new InMemoryOAuth2AuthorizationService();
    }

    // Define the RegisteredClientRepository bean
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        // Define your registered clients here
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

    // Define the JwtEncoder bean
    @Bean
    public JwtEncoder jwtEncoder() {
        // For simplicity, we'll use a symmetric key here.
        SecretKey secretKey = new SecretKeySpec("secret-key-which-is-at-least-32-bytes-long".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        OctetSequenceKey octetSequenceKey = new OctetSequenceKey.Builder(secretKey)
                .keyID("key-id")
                .build();

        JWKSet jwkSet = new JWKSet(octetSequenceKey);
        JWKSource jwkSource = new ImmutableJWKSet<>(jwkSet);

        return new NimbusJwtEncoder(jwkSource);
    }
}