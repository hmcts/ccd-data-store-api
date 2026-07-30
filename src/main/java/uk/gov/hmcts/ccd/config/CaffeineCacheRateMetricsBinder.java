package uk.gov.hmcts.ccd.config;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Component;

@Component
public class CaffeineCacheRateMetricsBinder implements MeterBinder {

    public static final String CACHE = "cache";
    private final CacheManager cacheManager;

    public CaffeineCacheRateMetricsBinder(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache caffeineCache) {
                com.github.benmanes.caffeine.cache.Cache<?, ?> nativeCache = caffeineCache.getNativeCache();

                FunctionCounter.builder("cache.hits", nativeCache, c -> c.stats().hitCount())
                    .description("The number of cache Hit Count")
                    .tag(CACHE, cacheName)
                    .register(registry);

                FunctionCounter.builder("cache.hits.rate", nativeCache, c -> c.stats().hitRate())
                    .description("The number of cache Hit Rate")
                    .tag(CACHE, cacheName)
                    .register(registry);

                FunctionCounter.builder("cache.misses", nativeCache, c -> c.stats().missCount())
                    .description("The number of cache Miss Count")
                    .tag(CACHE, cacheName)
                    .register(registry);

                FunctionCounter.builder("cache.misses.rate", nativeCache, c -> c.stats().missRate())
                    .description("The number of cache miss rate")
                    .tag(CACHE, cacheName)
                    .register(registry);

                FunctionCounter.builder("cache.load.count", nativeCache, c -> c.stats().loadCount())
                    .description("The number of cache load count")
                    .tag(CACHE, cacheName)
                    .register(registry);

                FunctionCounter.builder("cache.average.load.penalty", nativeCache, c -> c.stats().averageLoadPenalty())
                    .description("The number of cache average load penalty")
                    .tag(CACHE, cacheName)
                    .register(registry);

                FunctionCounter.builder("cache.eviction.count", nativeCache, c -> c.stats().evictionCount())
                    .description("The number of cache eviction count")
                    .tag(CACHE, cacheName)
                    .register(registry);

                FunctionCounter.builder("cache.eviction.weight", nativeCache, c -> c.stats().evictionWeight())
                    .description("The number of cache eviction weight")
                    .tag(CACHE, cacheName)
                    .register(registry);

                FunctionCounter.builder("cache.request.count", nativeCache, c -> c.stats().requestCount())
                    .description("The number of cache request count")
                    .tag(CACHE, cacheName)
                    .register(registry);

                FunctionCounter.builder("cache.puts", nativeCache, c -> c.stats().loadSuccessCount())
                    .description("The number of cache puts (load success count)")
                    .tag(CACHE, cacheName)
                    .register(registry);

            }
        });
    }
}
