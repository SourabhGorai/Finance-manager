package com.finance.finance_service.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Simple cache configuration using Spring's default in-memory cache (ConcurrentMapCache)
 * No external dependencies required - just spring-boot-starter-cache
 */
@Configuration
@EnableCaching
public class CacheConfig {
    // That's it! Spring Boot auto-configures ConcurrentMapCacheManager
    // Cache names are defined in service methods using @Cacheable annotations
}