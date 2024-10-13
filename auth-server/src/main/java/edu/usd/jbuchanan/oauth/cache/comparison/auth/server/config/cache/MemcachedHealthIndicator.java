package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config.cache;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import net.rubyeye.xmemcached.MemcachedClient;

@Component
public class MemcachedHealthIndicator implements HealthIndicator {

    private final MemcachedClient memcachedClient;

    public MemcachedHealthIndicator(MemcachedClient memcachedClient) {
        this.memcachedClient = memcachedClient;
    }

    @Override
    public Health health() {
        try {
            String stats = memcachedClient.getStats().toString();
            return Health.up().withDetail("stats", stats).build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
