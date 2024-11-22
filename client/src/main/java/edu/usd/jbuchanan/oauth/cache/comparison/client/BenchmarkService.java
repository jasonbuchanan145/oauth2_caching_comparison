package edu.usd.jbuchanan.oauth.cache.comparison.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
class BenchmarkService {
        private final WebClient webClient;
        private final String tokenEndpoint = "http://oauth-server-service:9999/oauth2/token";
        private final String resourceEndpoint = "http://resource-server-service:8081/sample/user-info";
        private final String clientId = "ThisIsMyClientId";
        private final String clientSecret = "myClientSecret";

        public BenchmarkService(WebClient webClient) {
            this.webClient = webClient;
        }

        private String createAuthHeader() {
            String auth = clientId + ":" + clientSecret;
            return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
        }


    private Mono<BenchmarkResult> getOAuthToken(int requestNumber, String cacheType) {
        return Mono.defer(() -> {
            long startTimeNano = System.nanoTime();
            return webClient.post()
                    .uri(tokenEndpoint)
                    .header("Authorization", createAuthHeader())
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("cache-type", cacheType)
                    .bodyValue("grant_type=authorization_code&client_id=" + clientId + "&client_secret=" + clientSecret)
                    .retrieve()
                    .bodyToMono(TokenResponse.class)
                    .map(response -> {
                        BenchmarkResult result = new BenchmarkResult();
                        // Convert nanoseconds to milliseconds with decimal precision
                        result.setOauthRequestTime((System.nanoTime() - startTimeNano) / 1_000_000.0);
                        result.setRequestNumber(requestNumber);
                        result.setCacheType(cacheType);
                        result.setSuccess(true);
                        result.setToken(response.getAccessToken());
                        log.debug("OAuth token received for request {}: {}", requestNumber, response.getAccessToken());
                        return result;
                    })
                    .onErrorResume(e -> {
                        log.error("Error getting OAuth token for request {}: {}", requestNumber, e.getMessage());
                        BenchmarkResult result = new BenchmarkResult();
                        result.setRequestNumber(requestNumber);
                        result.setCacheType(cacheType);
                        result.setSuccess(false);
                        result.setErrorMessage(e.getMessage());
                        return Mono.just(result);
                    });
        });
    }
    private Mono<BenchmarkResult> callResourceServer(BenchmarkResult result) {
        if (!result.isSuccess() || result.getToken() == null) {
            log.debug("Skipping resource server call for request {} due to missing token or failed OAuth request",
                    result.getRequestNumber());
            return Mono.just(result);
        }

        return Mono.defer(() -> {
            long startTimeNano = System.nanoTime();
            return webClient.get()
                    .uri(resourceEndpoint)
                    .header("Authorization", "Bearer " + result.getToken())
                    .retrieve()
                    .onStatus(status -> !status.equals(HttpStatus.OK),
                            clientResponse -> Mono.error(new RuntimeException("Unexpected status code: " + clientResponse.statusCode())))
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .map(response -> {
                        // Calculate time in milliseconds with nanosecond precision
                        result.setResourceRequestTime((System.nanoTime() - startTimeNano) / 1_000_000.0);

                        // Validate response content
                        String subject = (String) response.get("subject");
                        if (!clientId.equals(subject)) {
                            result.setSuccess(false);
                            result.setErrorMessage("Invalid subject in response. Expected: " + clientId + ", Got: " + subject);
                        }

                        log.debug("Resource server call completed for request {}: {}ms, subject: {}",
                                result.getRequestNumber(), result.getResourceRequestTime(), subject);
                        return result;
                    })
                    .onErrorResume(e -> {
                        result.setResourceRequestTime((System.nanoTime() - startTimeNano) / 1_000_000.0);
                        result.setSuccess(false);
                        result.setErrorMessage(e.getMessage());
                        log.error("Error calling resource server for request {}: {}, time taken: {}ms",
                                result.getRequestNumber(), e.getMessage(), result.getResourceRequestTime());
                        return Mono.just(result);
                    });
        });
    }

    public Mono<BenchmarkSummary> runBenchmark(int numberOfRequests, String cacheType) {
        return Flux.range(1, numberOfRequests)
                .flatMap(i ->
                                getOAuthToken(i, cacheType)
                                        .flatMap(this::callResourceServer)
                        , 20) // Concurrency limit
                .collectList()
                .map(results -> {
                    // Sort the results by requestNumber
                    results.sort(Comparator.comparing(BenchmarkResult::getRequestNumber));
                    return calculateSummary(results, cacheType);
                });
    }

    private BenchmarkSummary calculateSummary(List<BenchmarkResult> results, String cacheType) {
        BenchmarkSummary summary = new BenchmarkSummary();
        summary.setCacheType(cacheType);
        summary.setResults(results);
        summary.setTotalRequests(results.size());

        List<BenchmarkResult> successfulResults = results.stream()
                .filter(BenchmarkResult::isSuccess)
                .toList();

        summary.setSuccessfulRequests(successfulResults.size());
        summary.setFailedRequests(results.size() - successfulResults.size());

        if (!successfulResults.isEmpty()) {
            // Calculate OAuth metrics
            DoubleSummaryStatistics oauthStats = successfulResults.stream()
                    .mapToDouble(BenchmarkResult::getOauthRequestTime)
                    .summaryStatistics();

            summary.setAvgOAuthTime(oauthStats.getAverage());
            summary.setMaxOAuthTime(oauthStats.getMax());
            summary.setMinOAuthTime(oauthStats.getMin());

            // Calculate Resource Server metrics
            DoubleSummaryStatistics resourceStats = successfulResults.stream()
                    .mapToDouble(BenchmarkResult::getResourceRequestTime)
                    .summaryStatistics();

            summary.setAvgResourceTime(resourceStats.getAverage());
            summary.setMaxResourceTime(resourceStats.getMax());
            summary.setMinResourceTime(resourceStats.getMin());

            // Calculate 95th percentile for OAuth times
            List<Double> oauthTimes = successfulResults.stream()
                    .map(BenchmarkResult::getOauthRequestTime)
                    .sorted()
                    .toList();

            List<Double> resourceTimes = successfulResults.stream()
                    .map(BenchmarkResult::getResourceRequestTime)
                    .sorted()
                    .toList();

            int p95Index = (int) Math.ceil(0.95 * successfulResults.size()) - 1;
            if (p95Index >= 0) {
                summary.setP95OAuthTime(oauthTimes.get(p95Index));
                summary.setP95ResourceTime(resourceTimes.get(p95Index));
            }

            // Calculate throughput (requests per second)
            double totalDuration = successfulResults.stream()
                    .mapToDouble(r -> r.getOauthRequestTime() + r.getResourceRequestTime())
                    .sum() / 1000.0; // Convert to seconds
            summary.setThroughput(successfulResults.size() / totalDuration);

            // Calculate error rate
            summary.setErrorRate((double) summary.getFailedRequests() / summary.getTotalRequests() * 100);
        }

        return summary;
    }

}