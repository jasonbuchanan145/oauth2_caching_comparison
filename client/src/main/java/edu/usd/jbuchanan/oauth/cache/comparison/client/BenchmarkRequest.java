package edu.usd.jbuchanan.oauth.cache.comparison.client;

import lombok.Data;

@Data
class BenchmarkRequest {
    private int numberOfRequests;
    private String cacheType; // "hazelcast" or "redis"
}