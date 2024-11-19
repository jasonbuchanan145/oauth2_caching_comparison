package edu.usd.jbuchanan.oauth.cache.comparison.client;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/benchmark")
class BenchmarkController {
    private final BenchmarkService benchmarkService;

    public BenchmarkController(BenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
    }

    @PostMapping("/run")
    public Mono<BenchmarkSummary> runBenchmark(@RequestBody BenchmarkRequest request) {
        return benchmarkService.runBenchmark(request.getNumberOfRequests(), request.getCacheType());
    }
}