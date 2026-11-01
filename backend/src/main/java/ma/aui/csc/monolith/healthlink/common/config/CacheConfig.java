package ma.aui.csc.monolith.healthlink.common.config;

import java.time.Duration;
import java.util.Set;

import ma.aui.csc.monolith.healthlink.availability.service.AvailabilityCache;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Chooses where cached map searches live:
 * - Redis (shared by all instances, entries expire after the TTL) when healthlink.redis.enabled=true;
 * - this instance's memory when Redis is disabled (single-instance development);
 * - nowhere when healthlink.cache.enabled=false (tests that need fresh reads every time).
 */
@Configuration
public class CacheConfig {

    @Bean
    CacheManager cacheManager(@Value("${healthlink.cache.enabled:true}") boolean cacheEnabled,
                              @Value("${healthlink.redis.enabled:false}") boolean redisEnabled,
                              @Value("${healthlink.cache.availability-ttl:PT60S}") Duration ttl,
                              ObjectProvider<RedisConnectionFactory> redisConnections) {
        if (!cacheEnabled) {
            return new NoOpCacheManager();
        }
        if (redisEnabled) {
            RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(ttl)
                    .prefixCacheNameWith("healthlink:")
                    .disableCachingNullValues()
                    .serializeValuesWith(SerializationPair.fromSerializer(RedisSerializer.string()));
            return RedisCacheManager.builder(redisConnections.getObject())
                    .cacheDefaults(defaults)
                    .initialCacheNames(Set.of(AvailabilityCache.NAME))
                    .build();
        }
        return new ConcurrentMapCacheManager(AvailabilityCache.NAME);
    }
}
