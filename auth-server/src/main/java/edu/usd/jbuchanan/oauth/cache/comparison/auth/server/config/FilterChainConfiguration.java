package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config;

import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
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


    public FilterChainConfiguration(
            @Autowired CustomAuthorizationCodeAuthenticationProvider customAuthorizationCodeAuthenticationProvider,
            @Autowired CacheBackedAuthorizationService authorizationService,
            @Autowired Filter cacheTypeFilter,
            @Autowired OAuth2TokenGenerator<?> tokenGenerator) {
        this.authProvider = customAuthorizationCodeAuthenticationProvider;
        this.cacheTypeFilter = cacheTypeFilter;
        this.tokenGenerator = tokenGenerator;
        this.authorizationService = authorizationService;
    }
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        RequestMatcher endpointsMatcher = new OrRequestMatcher(
                authorizationServerConfigurer.getEndpointsMatcher(),
                new AntPathRequestMatcher("/oauth/**")
        );
        http
                .securityMatcher(endpointsMatcher)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/oauth/redis/**", "/oauth/memcached/**", "/oauth/hazelcast/**").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers(endpointsMatcher))
                .with(authorizationServerConfigurer, configurer -> configurer
                        .tokenGenerator(tokenGenerator)
                        .authorizationService(authorizationService)
                        .tokenEndpoint(tokenEndpoint -> tokenEndpoint
                                .accessTokenRequestConverter(
                                        new CustomAuthorizationCodeAuthenticationConverter())
                                .authenticationProvider(authProvider)))
                .addFilterBefore(cacheTypeFilter, UsernamePasswordAuthenticationFilter.class);  // Add your filter here

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
}

