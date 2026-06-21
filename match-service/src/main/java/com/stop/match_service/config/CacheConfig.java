package com.stop.match_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.time.Duration;

@Configuration
public class CacheConfig {

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
}
