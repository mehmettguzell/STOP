package com.stop.communication_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.lang.NonNull;

import java.time.Duration;

@Slf4j
@Configuration
public class CacheConfig implements CachingConfigurer {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()
                        )
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    /**
     * Redis timeout veya bağlantı hatası olursa exception fırlatmak yerine
     * cache miss olarak devam eder — gateway timeout ve circuit breaker tetiklenmez.
     */
    @Override
    @NonNull
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(@NonNull RuntimeException e, @NonNull Cache cache, @NonNull Object key) {
                log.warn("Cache GET error — cache={} key={} : {}", cache.getName(), key, e.getMessage());
            }
            @Override
            public void handleCachePutError(@NonNull RuntimeException e, @NonNull Cache cache, @NonNull Object key, Object value) {
                log.warn("Cache PUT error — cache={} key={} : {}", cache.getName(), key, e.getMessage());
            }
            @Override
            public void handleCacheEvictError(@NonNull RuntimeException e, @NonNull Cache cache, @NonNull Object key) {
                log.warn("Cache EVICT error — cache={} key={} : {}", cache.getName(), key, e.getMessage());
            }
            @Override
            public void handleCacheClearError(@NonNull RuntimeException e, @NonNull Cache cache) {
                log.warn("Cache CLEAR error — cache={} : {}", cache.getName(), e.getMessage());
            }
        };
    }
}
