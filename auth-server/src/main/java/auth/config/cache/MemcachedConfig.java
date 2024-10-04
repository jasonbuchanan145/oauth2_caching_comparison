package auth.config.cache;

import lombok.extern.slf4j.Slf4j;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.utils.AddrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.Callable;

@Configuration
public class MemcachedConfig {

    @Value("${memcached.servers}")
    private String memcachedServers;

    @Bean(name = "memcachedCacheManager")
    public CacheManager memcachedCacheManager() throws IOException {
        MemcachedClient memcachedClient = memcachedClient();

        // Create a MemcachedCache instance for your cache names
        MemcachedCache blacklistedTokensCache = new MemcachedCache("blacklistedTokens", memcachedClient, 3600);

        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(blacklistedTokensCache));
        return cacheManager;
    }

    @Bean
    public MemcachedClient memcachedClient() throws IOException {
        XMemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil.getAddresses(memcachedServers));
        // Additional configurations can be set here if needed
        return builder.build();
    }
    //what a mess, for my usecase I don't want it to take over as the default cache so I can't use most of the clients
    //so have to manually roll it as a cache
    @Slf4j
    public static class MemcachedCache implements Cache {

        private final String name;
        private final MemcachedClient memcachedClient;
        private final int expiration; // in seconds

        public MemcachedCache(String name, MemcachedClient memcachedClient, int expiration) {
            this.name = name;
            this.memcachedClient = memcachedClient;
            this.expiration = expiration;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public Object getNativeCache() {
            return this.memcachedClient;
        }

        @Override
        public ValueWrapper get(Object key) {
            try {
                Object value = memcachedClient.get(generateKey(key));
                return (value != null ? new SimpleValueWrapper(value) : null);
            } catch (TimeoutException | InterruptedException | MemcachedException e) {
                log.error(e.getMessage(), e);
                return null;
            }
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            ValueWrapper wrapper = get(key);
            return (wrapper != null ? (T) wrapper.get() : null);
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            ValueWrapper wrapper = get(key);
            if (wrapper != null) {
                return (T) wrapper.get();
            } else {
                try {
                    T value = valueLoader.call();
                    put(key, value);
                    return value;
                } catch (Exception e) {
                    throw new ValueRetrievalException(key, valueLoader, e);
                }
            }
        }

        @Override
        public void put(Object key, Object value) {
            try {
                memcachedClient.set(generateKey(key), expiration, value);
            } catch (TimeoutException | InterruptedException | MemcachedException e) {
               log.error(e.getMessage(), e);
            }
        }

        @Override
        public ValueWrapper putIfAbsent(Object key, Object value) {
            try {
                boolean success = memcachedClient.add(generateKey(key), expiration, value);
                if (success) {
                    return null;
                } else {
                    Object existingValue = memcachedClient.get(generateKey(key));
                    return new SimpleValueWrapper(existingValue);
                }
            } catch (TimeoutException | InterruptedException | MemcachedException e) {
               log.error(e.getMessage(), e);
                return null;
            }
        }

        @Override
        public void evict(Object key) {
            try {
                memcachedClient.delete(generateKey(key));
            } catch (TimeoutException | InterruptedException | MemcachedException e) {
                log.error(e.getMessage(), e);
            }
        }

        @Override
        public void clear() {
            try {
                memcachedClient.flushAll();
            } catch (TimeoutException | InterruptedException | MemcachedException e) {
               log.error(e.getMessage(), e);
            }
        }

        private String generateKey(Object key) {
            return this.name + ":" + key.toString();
        }
    }
}
