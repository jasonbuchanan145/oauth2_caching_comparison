package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config;

import edu.usd.jbuchanan.oauth.cache.comparison.cacheconfig.cache.HazelcastConfig;
import edu.usd.jbuchanan.oauth.cache.comparison.cacheconfig.cache.MemcachedConfig;
import edu.usd.jbuchanan.oauth.cache.comparison.cacheconfig.cache.MemcachedHealthIndicator;
import edu.usd.jbuchanan.oauth.cache.comparison.cacheconfig.cache.RedisCacheConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({HazelcastConfig.class, MemcachedConfig.class, MemcachedHealthIndicator.class, RedisCacheConfig.class})
public class CacheImportConfig {
}
