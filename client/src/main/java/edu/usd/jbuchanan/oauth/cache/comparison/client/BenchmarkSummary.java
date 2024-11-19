package edu.usd.jbuchanan.oauth.cache.comparison.client;

import lombok.Data;

import java.util.List;

@Data
class BenchmarkSummary {
        private String cacheType;
        private double avgOAuthTime;
        private double avgResourceTime;
        private double maxOAuthTime;
        private double maxResourceTime;
        private double minOAuthTime;
        private double minResourceTime;
        private double p95OAuthTime;
        private double p95ResourceTime;
        private double throughput;
        private double errorRate;
        private int totalRequests;
        private int successfulRequests;
        private int failedRequests;
        private List<BenchmarkResult> results;
    }

