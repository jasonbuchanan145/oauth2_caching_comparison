package edu.usd.jbuchanan.oauth.cache.comparison.cacheconfig.cache;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisCacheConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
    @Bean
    public RedisClusterConfiguration redisClusterConfiguration() {
        RedisClusterConfiguration config = new RedisClusterConfiguration();
        config.setUsername("default");
        config.setPassword("redis-password");
        config.addClusterNode(new RedisNode(
                "oauth-cache-comparison-redis-cluster-headless", 6379));
        return config;
    }

    @Bean
    public LettuceClientConfiguration lettuceClientConfiguration() {
        ClientOptions clientOptions = ClientOptions.builder()
                .protocolVersion(ProtocolVersion.RESP2)
                .build();

        return LettuceClientConfiguration.builder()
                .clientOptions(clientOptions)
                .build();
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(redisClusterConfiguration(), lettuceClientConfiguration());
    }

    @Bean(name = "redisCacheManager")
    public CacheManager redisCacheManager() {
        return RedisCacheManager.builder(redisConnectionFactory()).build();
    }
}

