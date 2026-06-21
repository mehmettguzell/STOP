package com.stop.api_gateway.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
public class RedisRateLimitConfig {

    private final GatewayProperties gatewayProperties;

    @Bean
    public LoadingCache<String, Bucket> rateLimitCache() {
        return Caffeine.newBuilder()
                .expireAfterAccess(1, TimeUnit.HOURS)
                .build(key -> newBucket());
    }

    private Bucket newBucket() {
        long capacity = gatewayProperties.getRateLimit().getCapacity();
        var refillPeriod = gatewayProperties.getRateLimit().getRefillPeriod();

        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillGreedy(capacity, refillPeriod)
                        .build())
                .build();
    }
}
