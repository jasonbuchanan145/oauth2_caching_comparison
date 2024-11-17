package edu.usd.jbuchanan.oauth.cache.comparison.cacheconfig.cache;

import com.hazelcast.map.IMap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@Slf4j
public class FindMaxVerisionUtility {

        private final static String cacheName="jwtversion";
        @Autowired
        @Qualifier("redisCacheManager")
        private CacheManager redisCacheManager;

        @Autowired
        @Qualifier("hazelcastCacheManager")
        private CacheManager hazelcastCacheManager;

        @Autowired
        private StringRedisTemplate stringRedisTemplate;

        public Optional<Integer> findMaxVersion(String userId, CacheType cacheType) {
            CacheManager cacheManager = (cacheType == CacheType.REDIS) ? redisCacheManager : hazelcastCacheManager;
            Cache cache = cacheManager.getCache(cacheName);

            if (cache == null) {
                return Optional.empty();
            }

            Collection<String> keys;
            if (cacheType == CacheType.REDIS) {
                org.springframework.data.redis.cache.RedisCache redisCache =
                        (org.springframework.data.redis.cache.RedisCache) cache;
                keys = getRedisKeys(redisCache, userId);
            } else {
                com.hazelcast.spring.cache.HazelcastCache hazelcastCache =
                        (com.hazelcast.spring.cache.HazelcastCache) cache;
                keys = getHazelcastKeys(hazelcastCache, userId);
            }

            return keys.stream()
                    .map(key -> {
                        String[] parts = key.split(":");
                        return parts.length > 0 ?
                                parseVersionSafely(parts[parts.length - 1]) : -1;
                    })
                    .filter(version -> version >= 0)
                    .max(Integer::compareTo);
        }

        private Collection<String> getRedisKeys(org.springframework.data.redis.cache.RedisCache redisCache, String userId) {
            String keyPattern = redisCache.getName() + "::*" + userId + ":*";
            return stringRedisTemplate.keys(keyPattern);
        }

    public int incrementVersion(String userId, CacheType cacheType) {
        if (cacheType == CacheType.REDIS) {
            return incrementRedisVersion(userId);
        } else {
            return incrementHazelcastVersion(userId);
        }
    }

    private int incrementRedisVersion(String userId) {
        String versionKey = cacheName + "::version:" + userId;
        Long newVersion = stringRedisTemplate.opsForValue().increment(versionKey);
        return newVersion != null ? newVersion.intValue() : 1;
    }
    public void removeAllVersions(String userId, CacheType cacheType) {
        if (cacheType == CacheType.REDIS) {
            removeRedisVersions(userId);
        } else {
            removeHazelcastVersions(userId);
        }
    }
    private void removeRedisVersions(String userId) {
        // Remove the version counter
        String versionKey = cacheName + "::version:" + userId;
        stringRedisTemplate.delete(versionKey);

        // Remove all keys containing this userId
        String keyPattern = cacheName + "::*" + userId + ":*";
        Set<String> keys = stringRedisTemplate.keys(keyPattern);
        int deletedCount = 0;

        if (keys != null && !keys.isEmpty()) {
            deletedCount = keys.size();
            stringRedisTemplate.delete(keys);
        }

        log.info("Redis: Removed {} keys for user {} in cache {}", deletedCount, userId, cacheName);
    }


    private int incrementHazelcastVersion(String userId) {
        com.hazelcast.spring.cache.HazelcastCache hazelcastCache =
                (com.hazelcast.spring.cache.HazelcastCache) hazelcastCacheManager.getCache(cacheName);
        if (hazelcastCache == null) {
            throw new IllegalStateException("Cache not found: " + cacheName);
        }

        IMap<Object, Object> map = hazelcastCache.getNativeCache();
        String versionKey = "version:" + userId;

        // Initialize with version 1 if key doesn't exist
        map.putIfAbsent(versionKey, "1");

        while (true) {
            Object currentValue = map.get(versionKey);
            if (currentValue == null) {
                // Should not happen due to putIfAbsent, but handle it anyway
                map.put(versionKey, "1");
                return 1;
            }

            int currentVersion = Integer.parseInt(currentValue.toString());
            int newVersion = currentVersion + 1;

            if (map.replace(versionKey, currentValue, String.valueOf(newVersion))) {
                return newVersion;
            }
        }
    }

    private void removeHazelcastVersions(String userId) {
        com.hazelcast.spring.cache.HazelcastCache hazelcastCache =
                (com.hazelcast.spring.cache.HazelcastCache) hazelcastCacheManager.getCache(cacheName);
        if (hazelcastCache == null) {
            throw new IllegalStateException("Cache not found: " + cacheName);
        }


        IMap<Object, Object> map = hazelcastCache.getNativeCache();

        // Remove the version counter
        String versionKey = "version:" + userId;
        map.remove(versionKey);

        // Count and remove all keys containing this userId
        AtomicInteger deletedCount = new AtomicInteger(0);
        map.keySet().stream()
                .map(Object::toString)
                .filter(key -> key.contains(userId + ":"))
                .forEach(key -> {
                    map.remove(key);
                    deletedCount.incrementAndGet();
                });

        log.info("Hazelcast: Removed {} keys for user {} in cache {}", deletedCount.get(), userId, cacheName);
    }

    private Collection<String> getHazelcastKeys(com.hazelcast.spring.cache.HazelcastCache hazelcastCache, String userId) {
        @SuppressWarnings("unchecked")
        IMap<Object, Object> map = hazelcastCache.getNativeCache();

        return map.keySet()
                .stream()
                .map(Object::toString)
                .filter(key -> key.contains(userId + ":"))
                .collect(Collectors.toSet());
    }

        private int parseVersionSafely(String versionStr) {
            try {
                return Integer.parseInt(versionStr);
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        public enum CacheType {
            REDIS,
            HAZELCAST
        }

    }
