package com.taskboard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis configuration for caching.
 * Configures cache manager with different TTLs for various cache regions.
 */
@Configuration
@EnableCaching
@EnableRedisRepositories(basePackages = "com.taskboard.repository.redis")
public class RedisConfig {

    @Value("${taskboard.cache.default-ttl:60}")
    private long defaultTtlMinutes;

    @Value("${taskboard.cache.boards-ttl:30}")
    private long boardsTtlMinutes;

    @Value("${taskboard.cache.comments-ttl:10}")
    private long commentsTtlMinutes;

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JSON serializer for values (Spring Boot 4.x recommended approach)
        template.setValueSerializer(RedisSerializer.json());
        template.setHashValueSerializer(RedisSerializer.json());

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(defaultTtlMinutes))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(RedisSerializer.json()))
                .disableCachingNullValues();

        // Cache-specific configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Boards cache - covers full board DTOs (board + lists + cards)
        cacheConfigurations.put("boards", defaultConfig
                .entryTtl(Duration.ofMinutes(boardsTtlMinutes)));

        // Comments cache — keyed by cardId, shorter TTL because comments are
        // high-frequency real-time data. Evicted on every write to a card's thread.
        // Completely decoupled from the boards cache — board reads are never affected.
        cacheConfigurations.put("comments", defaultConfig
                .entryTtl(Duration.ofMinutes(commentsTtlMinutes)));


        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}

