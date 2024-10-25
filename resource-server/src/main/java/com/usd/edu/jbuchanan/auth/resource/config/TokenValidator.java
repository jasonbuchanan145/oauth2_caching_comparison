package com.usd.edu.jbuchanan.auth.resource.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class TokenValidator {

    @Autowired
    private StringRedisTemplate redisTemplate;

    public boolean isTokenValid(String jti, String subject, int tokenVersion) {
        // Build the Redis key for the user's token version
        String redisKey = "user:" + subject + ":token_version";

        // Get the latest token version from Redis
        String latestVersionStr = redisTemplate.opsForValue().get(redisKey);
        int latestVersion = latestVersionStr != null ? Integer.parseInt(latestVersionStr) : 0;

        // Compare token versions
        return tokenVersion >= latestVersion;
    }

    public boolean isTokenBlacklisted(String jti) {
        // Check if the token ID is in the blacklist
        String redisKey = "blacklist:" + jti;
        return redisTemplate.hasKey(redisKey);
    }
}