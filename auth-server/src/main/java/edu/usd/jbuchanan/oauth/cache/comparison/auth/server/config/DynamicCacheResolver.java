package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;
import java.util.Collections;
@Component
public class DynamicCacheResolver implements CacheResolver {

    @Autowired
    @Qualifier("redisCacheManager")
    private CacheManager redisCacheManager;

    @Autowired
    @Qualifier("memcachedCacheManager")
    private CacheManager memcachedCacheManager;

    @Autowired
    @Qualifier("hazelcastCacheManager")
    private CacheManager hazelcastCacheManager;

    @Override
    public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        if (requestAttributes instanceof ServletRequestAttributes) {
            HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
            String cacheType = (String) request.getAttribute("CACHE_TYPE");

            CacheManager cacheManager = switch (cacheType) {
                case "redis" -> redisCacheManager;
                case "memcached" -> memcachedCacheManager;
                case "hazelcast" -> hazelcastCacheManager;
                default -> throw new IllegalArgumentException("Unknown cache type: " + cacheType);
            };

            String cacheName = "blacklistedTokens"; // Use your actual cache name
            Cache cache = cacheManager.getCache(cacheName);

            return Collections.singletonList(cache);
        } else {
            throw new IllegalStateException("No request attributes found. Is this called outside of an HTTP request?");
        }
    }
}
