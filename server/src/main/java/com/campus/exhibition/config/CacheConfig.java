package com.campus.exhibition.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 本地缓存配置（Caffeine，预留 Redis 接口）
 */
@Configuration
public class CacheConfig {

    @Bean
    public Cache<String, Object> screenCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats()
                .build();
    }
}
