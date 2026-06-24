package com.campus.exhibition.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 本地缓存配置（Caffeine，预留 Redis 接口）
 */
@Configuration
public class CacheConfig {

    /** 大屏轮播缓存 TTL（分钟），0 表示禁用缓存 */
    @Value("${app.cache.screen-carousel-ttl-minutes:5}")
    private int screenCarouselTtlMinutes;

    @Bean
    public Cache<String, Object> screenCache() {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumSize(500)
                .recordStats();

        if (screenCarouselTtlMinutes > 0) {
            builder.expireAfterWrite(screenCarouselTtlMinutes, TimeUnit.MINUTES);
        }
        // TTL <= 0：不设置过期，缓存永久有效（需手动清除或重启）

        return builder.build();
    }
}
