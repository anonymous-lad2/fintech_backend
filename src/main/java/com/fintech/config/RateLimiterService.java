package com.fintech.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiterService {

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> apiBuckets = new ConcurrentHashMap<>();

    public Bucket resolveLoginBucket(String ipAddress) {
        return loginBuckets.computeIfAbsent(ipAddress, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(10)
                                .refillGreedy(10, Duration.ofMinutes(1))
                                .build())
                        .build());
    }

    public Bucket resolveApiBucket(String ipAddress) {
        return apiBuckets.computeIfAbsent(ipAddress, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(200)
                                .refillGreedy(200, Duration.ofMinutes(1))
                                .build())
                        .build());
    }
}
