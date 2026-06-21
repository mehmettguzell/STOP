package com.stop.notification_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
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
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.lang.NonNull;

import java.time.Duration;

@Slf4j
@Configuration
public class CacheConfig implements CachingConfigurer {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .activateDefaultTyping(
                        BasicPolymorphicTypeValidator.builder()
                                .allowIfBaseType(Object.class)
                                .build(),
                        ObjectMapper.DefaultTyping.EVERYTHING
                );

        RedisSerializer<Object> jsonSerializer = new RedisSerializer<>() {
            @Override
            public byte[] serialize(Object value) throws SerializationException {
                if (value == null) return null;
                try { return objectMapper.writeValueAsBytes(value); }
                catch (Exception e) { throw new SerializationException("Serialize error", e); }
            }

            @Override
            public Object deserialize(byte[] bytes) throws SerializationException {
                if (bytes == null) return null;
                try { return objectMapper.readValue(bytes, Object.class); }
                catch (Exception e) { throw new SerializationException("Deserialize error", e); }
            }
        };

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    /**
     * Redis düşse bile servis çalışmaya devam eder — cache miss olarak işlem görür,
     * 500 hatası fırlatmaz ve circuit breaker'ı tetiklemez.
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
