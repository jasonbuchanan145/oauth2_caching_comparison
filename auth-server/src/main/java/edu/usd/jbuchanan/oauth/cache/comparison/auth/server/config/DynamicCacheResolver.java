package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
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
import java.util.List;
@Slf4j
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

        CacheManager cacheManager = getCacheManager();

        String cacheName = "blacklistedTokens"; // Use your actual cache name
        Cache cache = cacheManager.getCache(cacheName);

        return Collections.singletonList(cache);
    }
    List<CacheManager> getAll(){
        return List.of(redisCacheManager,memcachedCacheManager,hazelcastCacheManager);
    }

    public CacheManager getCacheManager(String cache){
        return switch (cache) {
            case "redis" -> redisCacheManager;
            case "memcached" -> memcachedCacheManager;
            case "hazelcast" -> hazelcastCacheManager;
            default -> throw new IllegalArgumentException("Unknown cache type: " + cache);
        };
    }
    public CacheManager getCacheManager() {
        String cacheType = CacheTypeContext.getCacheType();
        if(cacheType==null){
            log.warn("cache type is null");
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            String type = request.getHeader("cache-type");
            if(type==null){
                throw new NullPointerException("cache type is null again - big sad");
            }
            else{
                cacheType = type;
            }
        }
        return getCacheManager(cacheType);
    }
}
