package edu.usd.jbuchanan.oauth.cache.comparison.client;

import lombok.Data;

@Data
class BenchmarkResult {
    private double oauthRequestTime;
    private double resourceRequestTime; // Changed to double for nanosecond precision
    private int requestNumber;
    private String cacheType;
    private boolean success;
    private String errorMessage;
    private String token;
}
