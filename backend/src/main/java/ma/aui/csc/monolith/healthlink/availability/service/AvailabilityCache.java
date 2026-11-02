package ma.aui.csc.monolith.healthlink.availability.service;

import java.util.Objects;
import java.util.function.Supplier;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cache-aside for public map searches.
 *
 * Values are stored as JSON text, never as serialized Java objects: whatever is read back from
 * Redis is only parsed as data, so a tampered cache entry cannot trigger Java deserialization attacks.
 * With Redis enabled the cache is shared by every application instance; clearing it once clears it for all.
 */
@Component
public class AvailabilityCache {
    public static final String NAME = "availability";

    private final Cache cache;
    private final JsonMapper json;

    public AvailabilityCache(CacheManager cacheManager, JsonMapper json) {
        this.cache = Objects.requireNonNull(cacheManager.getCache(NAME), "Availability cache is not configured");
        this.json = json;
    }

    /** Returns the cached value for the key, or computes, stores and returns it. */
    public <T> T get(String key, Class<T> type, Supplier<T> loader) {
        String cached = cache.get(key, () -> json.writeValueAsString(loader.get()));
        return json.readValue(cached, type);
    }

    public void clear() {
        cache.clear();
    }
}
