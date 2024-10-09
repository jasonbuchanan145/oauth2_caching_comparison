package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config;

import jakarta.servlet.Filter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfiguration {


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Apply the default OAuth2 Authorization Server configuration
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        // Retrieve the OAuth2AuthorizationServerConfigurer to customize it
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                http.getConfigurer(OAuth2AuthorizationServerConfigurer.class);

        // Customize the token endpoint to use the custom authentication converter
        authorizationServerConfigurer
                .tokenEndpoint(tokenEndpoint ->
                        tokenEndpoint
                                .accessTokenRequestConverter(new CustomAuthorizationCodeAuthenticationConverter())
                );
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

    @Bean
    public Filter cacheTypeFilter() {
        return new CacheTypeFilter();
    }
}