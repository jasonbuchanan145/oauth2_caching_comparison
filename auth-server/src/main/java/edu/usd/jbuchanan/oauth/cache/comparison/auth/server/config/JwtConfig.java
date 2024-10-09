package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
@Configuration
public class JwtConfig {

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private CustomJwtCustomizer customJwtCustomizer;

    @Bean
    public JwtGenerator jwtGenerator() {
        JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
        jwtGenerator.setJwtCustomizer(customJwtCustomizer);
        return jwtGenerator;
    }
}
