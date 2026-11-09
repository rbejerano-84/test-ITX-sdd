package com.example.prices.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfig {

  public static final String PRICE_QUERY_CACHE = "priceQueries";

  @Bean
  CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager(PRICE_QUERY_CACHE);
    cacheManager.setCaffeine(
        Caffeine.newBuilder().maximumSize(10_000).expireAfterWrite(Duration.ofMinutes(5)));
    return cacheManager;
  }
}
