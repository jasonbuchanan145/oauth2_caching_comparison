package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class FilterChainConfiguration {
    private final CustomAuthorizationCodeAuthenticationProvider authProvider;
    private final CacheBackedAuthorizationService authorizationService;
    private final Filter cacheTypeFilter;
    private final OAuth2TokenGenerator<?> tokenGenerator;
    private final CustomAuthorizationCodeAuthenticationConverter authConverter;
    private final JWKSource<SecurityContext> jwkSource;
    private final BlacklistHandler blacklistHandler;

    public FilterChainConfiguration(
            @Autowired CustomAuthorizationCodeAuthenticationProvider customAuthorizationCodeAuthenticationProvider,
            @Autowired CacheBackedAuthorizationService authorizationService,
            @Autowired Filter cacheTypeFilter,
            @Autowired CustomAuthorizationCodeAuthenticationConverter authConverter,
            @Autowired OAuth2TokenGenerator<?> tokenGenerator,
            @Autowired JWKSource<SecurityContext> jwkSource,
                @Autowired BlacklistHandler blacklistHandler) {
        this.authProvider = customAuthorizationCodeAuthenticationProvider;
        this.cacheTypeFilter = cacheTypeFilter;
        this.tokenGenerator = tokenGenerator;
        this.authConverter = authConverter;
        this.authorizationService = authorizationService;
        this.jwkSource = jwkSource;  // Add this
        this.blacklistHandler = blacklistHandler;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {

        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        // Configure OIDC
        authorizationServerConfigurer
                .oidc(Customizer.withDefaults());
        authorizationServerConfigurer
                .tokenRevocationEndpoint(revocation -> revocation
                        .authenticationProvider(blacklistHandler));

        RequestMatcher endpointsMatcher = new OrRequestMatcher(
                authorizationServerConfigurer.getEndpointsMatcher(),
                new AntPathRequestMatcher("/oauth/**"),
                new AntPathRequestMatcher("/.well-known/**")
        );

        http
                .securityMatcher(endpointsMatcher)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/oauth/redis/**", "/oauth/memcached/**", "/oauth/hazelcast/**").permitAll()
                        .requestMatchers("/.well-known/**").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers(endpointsMatcher))
                .with(authorizationServerConfigurer, configurer -> configurer
                        .tokenGenerator(tokenGenerator)
                        .authorizationService(authorizationService)
                        .tokenEndpoint(tokenEndpoint -> tokenEndpoint
                                .accessTokenRequestConverter(authConverter)
                                .authenticationProvider(authProvider)))
                .addFilterBefore(cacheTypeFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(AntPathRequestMatcher.antMatcher("/actuator/**"))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://oauth-server-service:9999")  // Change this to match your setup
                .jwkSetEndpoint("/.well-known/jwks.json")
                .oidcClientRegistrationEndpoint("/connect/register")
                .oidcUserInfoEndpoint("/userinfo")
                .tokenEndpoint("/oauth2/token")
                .tokenIntrospectionEndpoint("/oauth2/introspect")
                .tokenRevocationEndpoint("/oauth2/revoke")
                .authorizationEndpoint("/oauth2/authorize")
                .build();
    }
}


